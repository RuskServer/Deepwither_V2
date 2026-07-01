package com.ruskserver.deepwither_V2.modules.dungeon.instance;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinitionRegistry;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.BossSpawnService;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.DoorConnection;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.DungeonGenerator;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.DungeonGenerator.GenerationResult;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.DungeonLayout;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.LootService;
import com.ruskserver.deepwither_V2.modules.dungeon.generator.MobSpawnService;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * アクティブなダンジョンインスタンスを管理するサービス。
 * <p>
 * ダンジョンごとに void ワールドを自動生成し、入口ルームのみ即時配置。
 * 以降はプレイヤーが未接続ドアに近づくたびに逐次ルーム生成を行います。
 * 終了時にワールドを削除します。
 */
@Service
public class DungeonInstanceManager implements Startable, Stoppable, org.bukkit.event.Listener {

    /** プレイヤーが未接続ドアに近づいたとみなす距離（ブロック） */
    private static final double DOOR_PROXIMITY_RANGE = 4.0;

    private final JavaPlugin plugin;
    private final DungeonDefinitionRegistry definitionRegistry;
    private final DungeonGenerator generator;
    private final MobSpawnService mobSpawnService;
    private final BossSpawnService bossSpawnService;
    private final LootService lootService;
    private final Logger log;

    private final Map<String, DungeonInstance> activeInstances = new HashMap<>();
    private final Map<UUID, String> playerToInstance = new HashMap<>();
    /** プレイヤーごとのダンジョン参加前の位置 */
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final AtomicInteger instanceCounter = new AtomicInteger(0);

    private int tickTaskId = -1;

    @Inject
    public DungeonInstanceManager(
            JavaPlugin plugin,
            DungeonDefinitionRegistry definitionRegistry,
            DungeonGenerator generator,
            MobSpawnService mobSpawnService,
            BossSpawnService bossSpawnService,
            LootService lootService
    ) {
        this.plugin = plugin;
        this.definitionRegistry = definitionRegistry;
        this.generator = generator;
        this.mobSpawnService = mobSpawnService;
        this.bossSpawnService = bossSpawnService;
        this.lootService = lootService;
        this.log = plugin.getLogger();
    }

    @Override
    public void start() {
        ensureTemplateWorldExists();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        tickTaskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tick, 10L, 10L).getTaskId();
        log.info("[DungeonInstanceManager] タイムアウトチェック + 逐次生成タイマーを開始しました。");
    }

    @org.bukkit.event.EventHandler
    public void onWorldInit(org.bukkit.event.world.WorldInitEvent event) {
        if (event.getWorld().getName().startsWith("dungeon_")) {
            event.getWorld().setKeepSpawnInMemory(false);
            log.info("[DungeonInstanceManager] スポーンメモリ保持を無効化しました: " + event.getWorld().getName());
        }
    }

    private void ensureTemplateWorldExists() {
        File templateDir = new File(plugin.getDataFolder(), "dungeon_template");
        if (templateDir.exists() && templateDir.isDirectory() && new File(templateDir, "level.dat").exists()) {
            return;
        }

        log.info("[DungeonInstanceManager] テンプレートワールドが存在しないため、新規生成します...");
        templateDir.mkdirs();

        String tempWorldName = "dungeon_template_temp";
        org.bukkit.WorldCreator tempCreator = new org.bukkit.WorldCreator(tempWorldName)
                .environment(org.bukkit.World.Environment.NORMAL)
                .generator(new com.ruskserver.deepwither_V2.modules.dungeon.generator.VoidChunkGenerator());

        org.bukkit.World tempWorld = plugin.getServer().createWorld(tempCreator);
        if (tempWorld == null) {
            log.severe("[DungeonInstanceManager] テンプレートワールドの仮生成に失敗しました。");
            return;
        }

        tempWorld.setGameRuleValue("doDaylightCycle", "false");
        tempWorld.setGameRuleValue("doWeatherCycle", "false");
        tempWorld.setGameRuleValue("doMobSpawning", "false");
        tempWorld.setGameRuleValue("doFireTick", "false");
        tempWorld.setGameRuleValue("keepInventory", "true");
        tempWorld.setGameRuleValue("doImmediateRespawn", "true");
        tempWorld.setGameRuleValue("showDeathMessages", "false");
        tempWorld.setTime(6000);
        tempWorld.save();

        plugin.getServer().unloadWorld(tempWorld, true);

        File tempWorldFolder = new File(plugin.getServer().getWorldContainer(), tempWorldName);
        if (tempWorldFolder.exists()) {
            try {
                copyDirectory(tempWorldFolder, templateDir);
                new File(templateDir, "uid.dat").delete();
                new File(templateDir, "session.lock").delete();
            } catch (IOException e) {
                log.log(java.util.logging.Level.SEVERE, "[DungeonInstanceManager] テンプレートのコピー中にエラーが発生しました", e);
            }
            deleteRecursively(tempWorldFolder);
        }
        log.info("[DungeonInstanceManager] テンプレートワールドを生成・保存しました: " + templateDir.getAbsolutePath());
    }

    private void copyDirectory(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    if (file.equals("uid.dat") || file.equals("session.lock")) {
                        continue;
                    }
                    File srcFile = new File(source, file);
                    File destFile = new File(destination, file);
                    copyDirectory(srcFile, destFile);
                }
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void stop() {
        if (tickTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        for (DungeonInstance instance : new ArrayList<>(activeInstances.values())) {
            teleportParticipantsBack(instance);
            deleteWorld(instance.getWorldName());
        }
        activeInstances.clear();
        playerToInstance.clear();
        returnLocations.clear();
    }

    // ========================================================================
    // インスタンス作成（入口ルームのみ即時生成）
    // ========================================================================

    /**
     * ダンジョンを生成し、void ワールドを作成してインスタンスを返します。
     * <p>
     * 入口ルームのみを即時配置し、残りのルームはプレイヤーのドア接近時に逐次生成されます。
     *
     * @param definitionId ダンジョン定義ID
     * @param creator      ダンジョンワールドの WorldCreator
     * @return 作成されたインスタンス。失敗場合は null
     */
    @SuppressWarnings("deprecation")
    public DungeonInstance createInstance(String definitionId, WorldCreator creator) {
        DungeonDefinition definition = definitionRegistry.get(definitionId);
        if (definition == null) {
            log.warning("[DungeonInstanceManager] ダンジョン定義が見つかりません: " + definitionId);
            return null;
        }

        String instanceId = definitionId + "_" + instanceCounter.incrementAndGet();
        String worldName = "dungeon_" + instanceId;

        // テンプレートからワールドデータをコピー
        File templateDir = new File(plugin.getDataFolder(), "dungeon_template");
        File worldFolder = new File(plugin.getServer().getWorldContainer(), worldName);
        try {
            if (templateDir.exists() && templateDir.isDirectory()) {
                copyDirectory(templateDir, worldFolder);
            } else {
                log.warning("[DungeonInstanceManager] テンプレートワールドが見つからないため、新規生成します。");
            }
        } catch (IOException e) {
            log.log(java.util.logging.Level.SEVERE, "[DungeonInstanceManager] テンプレートのコピーに失敗しました", e);
        }

        creator.name(worldName);
        World dungeonWorld = plugin.getServer().createWorld(creator);
        if (dungeonWorld == null) {
            log.severe("[DungeonInstanceManager] ワールド生成失敗: " + worldName);
            return null;
        }

        // ゲームルールを設定
        dungeonWorld.setGameRuleValue("doDaylightCycle", "false");
        dungeonWorld.setGameRuleValue("doWeatherCycle", "false");
        dungeonWorld.setGameRuleValue("doMobSpawning", "false");
        dungeonWorld.setGameRuleValue("doFireTick", "false");
        dungeonWorld.setGameRuleValue("keepInventory", "true");
        dungeonWorld.setGameRuleValue("doImmediateRespawn", "true");
        dungeonWorld.setGameRuleValue("showDeathMessages", "false");
        dungeonWorld.setTime(6000);
        dungeonWorld.setDifficulty(org.bukkit.Difficulty.NORMAL);

        BlockVector3 origin = BlockVector3.at(0, 64, 0);

        log.info("[DungeonInstanceManager] ダンジョン入口生成開始: " + instanceId);

        // 入口ルームのみ生成
        DungeonGenerator.EntryResult entryResult = generator.generateEntry(definition, dungeonWorld, origin);
        DungeonLayout layout = entryResult.layout();

        if (layout.getPlacedRoomCount() == 0) {
            log.severe("[DungeonInstanceManager] 入口ルーム配置失敗: " + instanceId);
            plugin.getServer().unloadWorld(dungeonWorld, false);
            return null;
        }

        // 入口ルームのモブ・宝をスポーン
        mobSpawnService.spawnMobs(dungeonWorld, layout.getAllMobSpawnPositions(), definition.mobId());
        for (var bossPos : layout.getBossSpawnPositions()) {
            bossSpawnService.spawnBoss(dungeonWorld, bossPos);
        }
        lootService.placeChests(dungeonWorld, layout.getLootPositions(), definition.lootTableId());

        // インスタンス生成（逐次生成用の未接続ドアを持たせる）
        DungeonInstance instance = new DungeonInstance(
                instanceId, definition, layout, dungeonWorld, worldName, origin, definition.lives(),
                entryResult.pendingDoors()
        );
        activeInstances.put(instanceId, instance);

        log.info("[DungeonInstanceManager] ダンジョン入口生成完了: " + instanceId
                + " (" + pendingDoorSummary(instance) + ")");
        return instance;
    }

    // ========================================================================
    // プレイヤー参加・離脱
    // ========================================================================

    public boolean joinDungeon(UUID playerId, String instanceId) {
        DungeonInstance instance = activeInstances.get(instanceId);
        if (instance == null || !instance.isActive()) return false;

        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) return false;

        returnLocations.put(playerId, player.getLocation());
        instance.addParticipant(playerId);
        playerToInstance.put(playerId, instanceId);

        BlockVector3 origin = instance.getOrigin();
        Location tpLoc = new Location(instance.getWorld(), origin.x() + 0.5, origin.y() + 1, origin.z() + 0.5);
        player.teleport(tpLoc);
        return true;
    }

    public void leaveDungeon(UUID playerId) {
        String instanceId = playerToInstance.remove(playerId);
        if (instanceId != null) {
            DungeonInstance instance = activeInstances.get(instanceId);
            if (instance != null) {
                instance.removeParticipant(playerId);
            }
        }
        teleportBack(playerId);
    }

    public DungeonInstance getPlayerInstance(UUID playerId) {
        String instanceId = playerToInstance.get(playerId);
        return instanceId != null ? activeInstances.get(instanceId) : null;
    }

    public DungeonInstance getInstance(String instanceId) {
        return activeInstances.get(instanceId);
    }

    public List<DungeonInstance> getActiveInstances() {
        return Collections.unmodifiableList(new ArrayList<>(activeInstances.values()));
    }

    // ========================================================================
    // タイマー処理（タイムアウト + 逐次生成のトリガー）
    // ========================================================================

    private void tick() {
        for (DungeonInstance instance : new ArrayList<>(activeInstances.values())) {
            if (!instance.isActive()) continue;

            // タイムアウトチェック
            if (instance.isTimedOut()) {
                instance.timeout();
                onDungeonTimeout(instance);
                continue;
            }

            // 逐次生成: プレイヤーが未接続ドアに近づいたら次のルームを生成
            if (instance.hasPendingDoors()) {
                tryTriggerGeneration(instance);
            }
        }
    }

    /**
     * ダンジョン内のプレイヤーが未接続ドアに近づいている場合、次のルームを生成します。
     * <p>
     * 1 tick につき1ルームまで生成し、ラグを防止します。
     */
    private void tryTriggerGeneration(DungeonInstance instance) {
        World world = instance.getWorld();
        if (world == null) return;

        List<Player> playersInWorld = world.getPlayers();
        if (playersInWorld.isEmpty()) return;

        for (DoorConnection door : List.copyOf(instance.getPendingDoors())) {
            Location doorLoc = toLocation(world, door.worldPosition());

            boolean anyPlayerNear = playersInWorld.stream()
                    .anyMatch(p -> p.getWorld().equals(world)
                            && p.getLocation().distanceSquared(doorLoc) <= DOOR_PROXIMITY_RANGE * DOOR_PROXIMITY_RANGE);

            if (!anyPlayerNear) continue;

            if (instance.removePendingDoor(door)) {
                generateRoomAtDoor(instance, door);
                return;
            }
        }
    }

    /**
     * 指定ドアに次のルームを生成し、結果を反映します。
     */
    private void generateRoomAtDoor(DungeonInstance instance, DoorConnection door) {
        try {
            boolean branchingEnabled = instance.getDefinition().isBranchingEnabled();
            GenerationResult result = generator.generateNextRoom(
                    instance.getDefinition(),
                    instance.getWorld(),
                    instance.getLayout(),
                    door,
                    instance.getCurrentDepth(),
                    branchingEnabled
            );

            if (result == null) {
                log.warning("[DungeonInstanceManager] ルーム生成失敗 (door="
                        + door.worldPosition() + ")");
                return;
            }

            // ドア封鎖の場合は何も配置しない
            if (result.placedRoom() == null) {
                log.fine("[DungeonInstanceManager] ドア封鎖: " + door.worldPosition());
                return;
            }

            // 深度更新
            instance.incrementDepth();

            // 新しい未接続ドアを追加
            instance.addPendingDoors(result.newPendingDoors());

            // モブ・宝をスポーン
            mobSpawnService.spawnMobs(instance.getWorld(), result.placementResult().mobSpawnWorldPositions(), instance.getDefinition().mobId());
            if (result.placementResult().bossSpawnWorldPos() != null) {
                bossSpawnService.spawnBoss(instance.getWorld(), result.placementResult().bossSpawnWorldPos());
            }
            lootService.placeChests(instance.getWorld(), result.placementResult().lootWorldPositions(), instance.getDefinition().lootTableId());

            log.fine("[DungeonInstanceManager] ルーム生成: " + result.placedRoom().schematic().schematicId()
                    + " depth=" + instance.getCurrentDepth()
                    + " pending=" + instance.getPendingDoors().size());

        } catch (Exception e) {
            log.severe("[DungeonInstanceManager] ルーム生成例外: " + e.getMessage());
        }
    }

    // ========================================================================
    // ダンジョン終了処理
    // ========================================================================

    private void onDungeonTimeout(DungeonInstance instance) {
        log.info("[DungeonInstanceManager] ダンジョンタイムアウト: " + instance.getInstanceId());
        teleportParticipantsBack(instance);
        scheduleWorldDelete(instance);
    }

    public void endDungeon(DungeonInstance instance) {
        teleportParticipantsBack(instance);
        scheduleWorldDelete(instance);
    }

    private void teleportParticipantsBack(DungeonInstance instance) {
        for (UUID playerId : new ArrayList<>(instance.getParticipants())) {
            teleportBack(playerId);
            instance.removeParticipant(playerId);
            playerToInstance.remove(playerId);
        }
        activeInstances.remove(instance.getInstanceId());
    }

    private void teleportBack(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        Location returnLoc = returnLocations.remove(playerId);
        if (player != null && player.isOnline() && returnLoc != null) {
            player.teleport(returnLoc);
        }
    }

    private void scheduleWorldDelete(DungeonInstance instance) {
        String worldName = instance.getWorldName();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            deleteWorld(worldName);
        }, 60L);
    }

    private void deleteWorld(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return;

        World mainWorld = plugin.getServer().getWorlds().getFirst();
        for (Player p : world.getPlayers()) {
            p.teleport(mainWorld.getSpawnLocation());
        }

        plugin.getServer().unloadWorld(world, false);
        java.io.File worldFolder = new java.io.File(plugin.getServer().getWorldContainer(), worldName);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            deleteRecursively(worldFolder);
            log.info("[DungeonInstanceManager] ワールド削除完了 (非同期): " + worldName);
        });
    }

    private void deleteRecursively(java.io.File file) {
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    // ========================================================================
    // ユーティリティ
    // ========================================================================

    private static Location toLocation(World world, BlockVector3 pos) {
        return new Location(world, pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
    }

    private static String pendingDoorSummary(DungeonInstance instance) {
        return instance.getPendingDoors().size() + " pending doors";
    }
}

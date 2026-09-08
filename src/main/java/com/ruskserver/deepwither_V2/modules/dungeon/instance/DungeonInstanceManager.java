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
import com.ruskserver.deepwither_V2.modules.dungeon.modifier.DungeonModifierContext;
import com.ruskserver.deepwither_V2.modules.dungeon.portal.DungeonPortalVisualHelper;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Color;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
    private final com.ruskserver.deepwither_V2.modules.combat.CombatStateService combatState;

    private final Map<String, DungeonInstance> activeInstances = new HashMap<>();
    private final Map<UUID, String> playerToInstance = new HashMap<>();
    /** プレイヤーごとのダンジョン参加前の位置 */
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<UUID, Location> pendingRespawnLocations = new HashMap<>();
    /** ボスエンティティと、それが属するインスタンス・赤石マーカー座標の対応 */
    private final Map<UUID, BossContext> activeBosses = new HashMap<>();
    /** ボス部屋配置後にプレイヤー接近を待つ保留中スポーン情報 */
    private static final double BOSS_TRIGGER_RADIUS = 14.0; // たたきつけ攻撃範囲(6.0m) + 8.0m = 14.0m
    private static final double BOSS_TRIGGER_RADIUS_SQUARED = BOSS_TRIGGER_RADIUS * BOSS_TRIGGER_RADIUS; // 196.0

    private record PendingBossSpawn(String instanceId, BlockVector3 bossPosition, double triggerRadiusSquared) {}
    private final Map<String, PendingBossSpawn> pendingBossSpawns = new HashMap<>();

    private final AtomicInteger instanceCounter = new AtomicInteger(0);

    private int tickTaskId = -1;

    @Inject
    public DungeonInstanceManager(
            JavaPlugin plugin,
            DungeonDefinitionRegistry definitionRegistry,
            DungeonGenerator generator,
            MobSpawnService mobSpawnService,
            BossSpawnService bossSpawnService,
            LootService lootService,
            com.ruskserver.deepwither_V2.modules.combat.CombatStateService combatState
    ) {
        this.plugin = plugin;
        this.definitionRegistry = definitionRegistry;
        this.generator = generator;
        this.mobSpawnService = mobSpawnService;
        this.bossSpawnService = bossSpawnService;
        this.lootService = lootService;
        this.combatState = combatState;
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

        configureDungeonGameRules(tempWorld);
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
        pendingRespawnLocations.clear();
        activeBosses.clear();
        pendingBossSpawns.clear();
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
        return createInstance(definitionId, creator, DungeonModifierContext.none());
    }

    public DungeonInstance createInstance(String definitionId, WorldCreator creator, DungeonModifierContext modifierContext) {
        if (modifierContext == null) modifierContext = DungeonModifierContext.none();
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
            deleteRecursively(worldFolder);
            return null;
        }

        // ゲームルールを設定
        configureDungeonGameRules(dungeonWorld);
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
            deleteRecursively(worldFolder);
            return null;
        }

        // 入口ルームの通常モブ・宝をスポーン
        mobSpawnService.spawnMobs(dungeonWorld, layout.getAllMobSpawnPositions(), definition.mobId(), modifierContext);
        lootService.placeChests(dungeonWorld, layout.getLootPositions(), definition.lootTableId(), modifierContext);

        // モディファイアーによるライフ補正
        int effectiveLives = definition.lives() + modifierContext.combinedExtraLives();
        if (effectiveLives < 1) effectiveLives = 1;

        // インスタンス生成（逐次生成用の未接続ドアを持たせる）
        DungeonInstance instance = new DungeonInstance(
                instanceId, definition, layout, dungeonWorld, worldName, origin, effectiveLives,
                entryResult.pendingDoors(), modifierContext
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
        if (playerToInstance.containsKey(playerId)) {
            log.warning("[DungeonInstanceManager] プレイヤー " + playerId + " は既に別のダンジョンに参加しています");
            return false;
        }

        DungeonInstance instance = activeInstances.get(instanceId);
        if (instance == null || !instance.isActive()) return false;

        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) return false;

        returnLocations.put(playerId, player.getLocation());
        instance.addParticipant(playerId);
        playerToInstance.put(playerId, instanceId);

        player.teleport(getEntrySpawnLocation(instance));
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDungeonPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = getPlayerInstance(player.getUniqueId());
        if (instance == null) {
            return;
        }

        // Keep-inventory is enforced here as well as by the world rule.
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);

        if (instance.consumeLife() <= 0) {
            wipeDungeon(instance);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDungeonPlayerRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Location returnLocation = pendingRespawnLocations.remove(playerId);
        if (returnLocation != null && returnLocation.getWorld() != null) {
            event.setRespawnLocation(returnLocation);
            return;
        }

        DungeonInstance instance = getPlayerInstance(playerId);
        if (instance != null && instance.isActive()) {
            event.setRespawnLocation(getEntrySpawnLocation(instance));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDungeonBossDeath(EntityDeathEvent event) {
        BossContext bossContext = activeBosses.remove(event.getEntity().getUniqueId());
        if (bossContext == null) {
            return;
        }

        DungeonInstance instance = activeInstances.get(bossContext.instanceId());
        if (instance == null || !instance.isActive()) {
            return;
        }

        instance.clear(bossContext.portalPosition());
        Location portalBase = toPortalLocation(instance.getWorld(), bossContext.portalPosition());
        for (UUID participantId : instance.getParticipants()) {
            Player participant = plugin.getServer().getPlayer(participantId);
            if (participant == null || !participant.isOnline()) {
                continue;
            }
            participant.sendMessage("§6§lボスを撃破した！ §e出現したポータルから脱出できます。");
            participant.playSound(portalBase, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.15f);
        }
        log.info("[DungeonInstanceManager] ボス撃破・脱出ポータル有効化: "
                + instance.getInstanceId() + " at " + bossContext.portalPosition());
    }

    public void leaveDungeon(UUID playerId) {
        String instanceId = playerToInstance.remove(playerId);
        if (instanceId != null) {
            activeBosses.forEach((bossId, context) -> {
                if (context.instanceId().equals(instanceId)) combatState.leaveEncounter(bossId, playerId);
            });
            DungeonInstance instance = activeInstances.get(instanceId);
            if (instance != null) {
                instance.removeParticipant(playerId);
                if (instance.getParticipants().isEmpty()) {
                    activeInstances.remove(instanceId);
                    removeBossTracking(instanceId);
                    scheduleWorldDelete(instance);
                }
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
        activeBosses.forEach((bossId, context) -> {
            Entity boss = plugin.getServer().getEntity(bossId);
            DungeonInstance instance = activeInstances.get(context.instanceId());
            if (boss == null || !boss.isValid() || boss.isDead() || instance == null || !instance.isActive()) return;
            for (UUID playerId : instance.getParticipants()) {
                Player player = plugin.getServer().getPlayer(playerId);
                if (player != null) combatState.joinEncounter(boss, player);
            }
        });
        for (DungeonInstance instance : new ArrayList<>(activeInstances.values())) {
            if (instance.getState() == DungeonState.CLEARED) {
                tickExitPortal(instance);
                continue;
            }
            if (!instance.isActive()) {
                continue;
            }

            // タイムアウトチェック
            if (instance.isTimedOut()) {
                instance.timeout();
                onDungeonTimeout(instance);
                continue;
            }

            // 保留中ボスの接近検知チェック
            checkPendingBossSpawns(instance);

            // 逐次生成: プレイヤーが未接続ドアに近づいたら次のルームを生成
            if (instance.hasPendingDoors()) {
                tryTriggerGeneration(instance);
            }
        }
    }

    private void checkPendingBossSpawns(DungeonInstance instance) {
        PendingBossSpawn pending = pendingBossSpawns.get(instance.getInstanceId());
        if (pending == null) return;

        World world = instance.getWorld();
        if (world == null) return;

        BlockVector3 bossPos = pending.bossPosition();
        Location bossLoc = new Location(world, bossPos.x() + 0.5, bossPos.y(), bossPos.z() + 0.5);

        for (UUID participantId : instance.getParticipants()) {
            Player player = plugin.getServer().getPlayer(participantId);
            if (player == null || !player.isOnline() || !player.getWorld().equals(world)) {
                continue;
            }

            if (player.getLocation().distanceSquared(bossLoc) <= pending.triggerRadiusSquared()) {
                pendingBossSpawns.remove(instance.getInstanceId());
                log.info("[DungeonInstanceManager] プレイヤー接近検知 (" + player.getName()
                        + ") -> ボススポーン実行: " + instance.getInstanceId() + " at " + bossPos);
                spawnTrackedBoss(instance, bossPos);
                break;
            }
        }
    }

    private void tickExitPortal(DungeonInstance instance) {
        BlockVector3 portalPosition = instance.getExitPortalPosition();
        World world = instance.getWorld();
        if (portalPosition == null || world == null) {
            return;
        }

        Location portalBase = toPortalLocation(world, portalPosition);
        for (UUID participantId : new ArrayList<>(instance.getParticipants())) {
            Player player = plugin.getServer().getPlayer(participantId);
            if (player == null || !player.isOnline() || !player.getWorld().equals(world)) {
                continue;
            }

            if (player.getLocation().distanceSquared(portalBase) <= 24.0 * 24.0) {
                DungeonPortalVisualHelper.spawnPortal(
                        player,
                        portalBase,
                        Color.fromRGB(190, 110, 255),
                        Color.fromRGB(90, 220, 255)
                );
            }

            if (DungeonPortalVisualHelper.isInside(player.getLocation(), portalBase)) {
                exitThroughPortal(instance, player);
            }
        }
    }

    private void exitThroughPortal(DungeonInstance instance, Player player) {
        UUID playerId = player.getUniqueId();
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.8f, 1.2f);
        player.sendMessage("§bダンジョンを脱出しました。");

        instance.removeParticipant(playerId);
        playerToInstance.remove(playerId);
        teleportBack(playerId);

        if (instance.getParticipants().isEmpty()) {
            activeInstances.remove(instance.getInstanceId());
            removeBossTracking(instance.getInstanceId());
            scheduleWorldDelete(instance);
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
                    branchingEnabled,
                    instance.getRng(),
                    instance.getConsecutiveCorridors()
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
            mobSpawnService.spawnMobs(instance.getWorld(), result.placementResult().mobSpawnWorldPositions(), instance.getDefinition().mobId(), instance.getModifierContext());
            if (isBossRoom(instance, result)) {
                for (DoorConnection remainingDoor : instance.drainPendingDoors()) {
                    generator.sealDoor(instance.getWorld(), remainingDoor);
                }
                BlockVector3 bossPos = result.placementResult().bossSpawnWorldPos();
                if (bossPos != null) {
                    pendingBossSpawns.put(instance.getInstanceId(),
                            new PendingBossSpawn(instance.getInstanceId(), bossPos, BOSS_TRIGGER_RADIUS_SQUARED));
                    log.info("[DungeonInstanceManager] ボス部屋配置完了。接近トリガー待機中: " + instance.getInstanceId()
                            + " at " + bossPos + " (半径 " + BOSS_TRIGGER_RADIUS + "m)");
                } else {
                    log.severe("[DungeonInstanceManager] ボスルームにREDSTONE_BLOCKマーカーがありません: "
                            + instance.getDefinition().bossRoomSchematic());
                }
            }
            lootService.placeChests(instance.getWorld(), result.placementResult().lootWorldPositions(), instance.getDefinition().lootTableId(), instance.getModifierContext());

            // ペース管理: 配置したスロットの種別に応じて連続通路カウンタを更新
            if (result.roomSlot() != null) {
                if (DungeonDefinition.isCorridorType(result.roomSlot().type())) {
                    instance.incrementConsecutiveCorridors();
                } else {
                    instance.resetConsecutiveCorridors();
                }
            }

            log.fine("[DungeonInstanceManager] ルーム生成: " + result.placedRoom().schematic().schematicId()
                    + " depth=" + instance.getCurrentDepth()
                    + " pending=" + instance.getPendingDoors().size()
                    + " corridors=" + instance.getConsecutiveCorridors());

        } catch (Exception e) {
            log.severe("[DungeonInstanceManager] ルーム生成例外: " + e.getMessage());
        }
    }

    private boolean isBossRoom(DungeonInstance instance, GenerationResult result) {
        String bossSchematic = instance.getDefinition().bossRoomSchematic();
        return bossSchematic != null
                && !bossSchematic.isBlank()
                && result.placedRoom().schematic().schematicId()
                .equals(instance.getDefinition().id() + ":" + bossSchematic);
    }

    private void spawnTrackedBoss(DungeonInstance instance, BlockVector3 bossPosition) {
        if (bossPosition == null) {
            log.severe("[DungeonInstanceManager] ボスルームにREDSTONE_BLOCKマーカーがありません: "
                    + instance.getDefinition().bossRoomSchematic());
            return;
        }

        Entity boss = bossSpawnService.spawnBoss(
                instance.getWorld(),
                bossPosition,
                instance.getDefinition().bossMobId()
        );
        if (boss == null) {
            log.severe("[DungeonInstanceManager] ボスのスポーンに失敗: " + instance.getInstanceId());
            return;
        }

        activeBosses.put(
                boss.getUniqueId(),
                new BossContext(instance.getInstanceId(), bossPosition)
        );
        for (UUID playerId : instance.getParticipants()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) combatState.joinEncounter(boss, player);
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
        removeBossTracking(instance.getInstanceId());
    }

    private void wipeDungeon(DungeonInstance instance) {
        for (UUID participantId : new ArrayList<>(instance.getParticipants())) {
            teleportBack(participantId);
            instance.removeParticipant(participantId);
            playerToInstance.remove(participantId);
        }
        activeInstances.remove(instance.getInstanceId());
        removeBossTracking(instance.getInstanceId());
        scheduleWorldDelete(instance);
    }

    private void removeBossTracking(String instanceId) {
        activeBosses.forEach((bossId, context) -> {
            if (context.instanceId().equals(instanceId)) combatState.endEncounter(bossId);
        });
        activeBosses.entrySet().removeIf(entry -> entry.getValue().instanceId().equals(instanceId));
        pendingBossSpawns.remove(instanceId);
    }

    private void teleportBack(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        Location returnLoc = returnLocations.remove(playerId);
        if (player != null && player.isOnline() && returnLoc != null) {
            if (player.isDead()) {
                pendingRespawnLocations.put(playerId, returnLoc);
            } else {
                player.teleport(returnLoc);
            }
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

        if (plugin.getServer().isStopping()) {
            deleteRecursively(worldFolder);
            log.info("[DungeonInstanceManager] ワールド削除完了: " + worldName);
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                deleteRecursively(worldFolder);
                log.info("[DungeonInstanceManager] ワールド削除完了 (非同期): " + worldName);
            });
        }
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

    private static Location toPortalLocation(World world, BlockVector3 pos) {
        return new Location(world, pos.x() + 0.5, pos.y(), pos.z() + 0.5);
    }

    private static Location getEntrySpawnLocation(DungeonInstance instance) {
        DungeonLayout.PlacedRoom entryRoom = instance.getLayout().getPlacedRooms().getFirst();
        BlockVector3 localEntry = entryRoom.schematic().entryTeleport();
        BlockVector3 worldEntry = localEntry != null
                ? entryRoom.origin().add(DungeonGenerator.rotatePosition(localEntry, entryRoom.rotation()))
                : entryRoom.origin().add(0, 1, 0);
        return new Location(instance.getWorld(), worldEntry.x() + 0.5, worldEntry.y(), worldEntry.z() + 0.5);
    }

    private static void configureDungeonGameRules(World world) {
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
    }

    private static String pendingDoorSummary(DungeonInstance instance) {
        return instance.getPendingDoors().size() + " pending doors";
    }

    private record BossContext(String instanceId, BlockVector3 portalPosition) {
    }
}

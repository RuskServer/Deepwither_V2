package com.ruskserver.deepwither_V2.modules.quest.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.quest.api.Quest;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestCategory;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestState;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * クエスト受注・報告可能状態のNPC頭上にマーカー（！/？）を表示するサービス。
 * <p>
 * Paper API の TextDisplay とクライアント個別表示 (player.showEntity / hideEntity) を使用し、
 * 各プレイヤーのクエスト進行状態に応じたマーカーのみを表示します。
 */
@Service
public class QuestNpcMarkerService implements Startable, Stoppable, Listener {

    private static final double VISIBILITY_RANGE_SQUARED = 32.0 * 32.0;
    private static final double MARKER_HEIGHT_OFFSET = 0.35;

    private final JavaPlugin plugin;
    private final QuestService questService;
    private final Logger log;

    private int tickTaskId = -1;

    private record MarkerPair(TextDisplay availableMarker, TextDisplay completeMarker) {
        public void remove() {
            if (availableMarker != null && availableMarker.isValid()) {
                availableMarker.remove();
            }
            if (completeMarker != null && completeMarker.isValid()) {
                completeMarker.remove();
            }
        }
    }

    private final Map<Integer, MarkerPair> npcMarkers = new ConcurrentHashMap<>();

    private enum MarkerType {
        COMPLETE,  // 条件達成・報告待ち（？）
        AVAILABLE, // 未受注・受注可能（！）
        NONE       // 非表示
    }

    @Inject
    public QuestNpcMarkerService(JavaPlugin plugin, QuestService questService) {
        this.plugin = plugin;
        this.questService = questService;
        this.log = plugin.getLogger();
    }

    @Override
    public void start() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            log.info("[QuestNpcMarkerService] Citizens プラグインが無効なため、NPCマーカー機能をスキップします。");
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // 起動から少し遅延させて既存の全NPCにマーカーをセットアップ
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setupAllExistingNpcMarkers();
            tickTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L).getTaskId();
        }, 40L);

        log.info("[QuestNpcMarkerService] NPCクエストマーカーサービスを開始しました。");
    }

    @Override
    public void stop() {
        if (tickTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        for (MarkerPair pair : npcMarkers.values()) {
            pair.remove();
        }
        npcMarkers.clear();
    }

    private void setupAllExistingNpcMarkers() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) return;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.isSpawned() && isQuestNpc(npc.getName())) {
                createMarkersForNpc(npc);
            }
        }
    }

    private boolean isQuestNpc(String npcName) {
        if (npcName == null) return false;
        for (Quest quest : questService.getAllQuests()) {
            if (matchesNpc(quest, npcName)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesNpc(Quest quest, String npcName) {
        if (quest.getNpcName() == null || npcName == null) return false;
        if (quest.getNpcName().equalsIgnoreCase(npcName)) return true;
        if (quest.getNpcName().equals("村長") && (npcName.equalsIgnoreCase("VillageElder") || npcName.equals("村長"))) {
            return true;
        }
        if (quest.getNpcName().equals("鍛冶屋") && (npcName.equalsIgnoreCase("Blacksmith") || npcName.equals("鍛冶屋") || npcName.equals("合成屋"))) {
            return true;
        }
        return false;
    }

    private void createMarkersForNpc(NPC npc) {
        if (!npc.isSpawned()) return;
        Entity entity = npc.getEntity();
        if (entity == null || !entity.isValid()) return;

        // 既存のマーカーを破棄
        MarkerPair existing = npcMarkers.remove(npc.getId());
        if (existing != null) {
            existing.remove();
        }

        Location loc = getMarkerLocation(entity);

        // 受注可能マーカー（！）
        TextDisplay availableDisplay = entity.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.text(Component.text("§e!").decoration(TextDecoration.ITALIC, false));
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setVisibleByDefault(false);
            d.setPersistent(false);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(1.4f, 1.4f, 1.4f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            ));
        });

        // 完了可能マーカー（？）
        TextDisplay completeDisplay = entity.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.text(Component.text("§a?").decoration(TextDecoration.ITALIC, false));
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(true);
            d.setVisibleByDefault(false);
            d.setPersistent(false);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(1.4f, 1.4f, 1.4f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            ));
        });

        npcMarkers.put(npc.getId(), new MarkerPair(availableDisplay, completeDisplay));
    }

    private Location getMarkerLocation(Entity entity) {
        return entity.getLocation().clone().add(0, entity.getHeight() + MARKER_HEIGHT_OFFSET, 0);
    }

    private void tick() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) return;

        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (!npc.isSpawned()) {
                MarkerPair pair = npcMarkers.remove(npc.getId());
                if (pair != null) pair.remove();
                continue;
            }

            if (!isQuestNpc(npc.getName())) continue;

            Entity entity = npc.getEntity();
            if (entity == null || !entity.isValid()) continue;

            MarkerPair pair = npcMarkers.get(npc.getId());
            if (pair == null || !isMarkerValid(pair)) {
                createMarkersForNpc(npc);
                pair = npcMarkers.get(npc.getId());
            }

            if (pair == null) continue;

            // マーカーの位置をNPCの頭上に同期
            Location targetLoc = getMarkerLocation(entity);
            if (pair.availableMarker().isValid()) {
                pair.availableMarker().teleport(targetLoc);
            }
            if (pair.completeMarker().isValid()) {
                pair.completeMarker().teleport(targetLoc);
            }

            // 周囲のプレイヤーに対して可視性を同期
            updateVisibilityForNpc(npc, pair);
        }
    }

    private boolean isMarkerValid(MarkerPair pair) {
        return pair != null
                && pair.availableMarker() != null && pair.availableMarker().isValid()
                && pair.completeMarker() != null && pair.completeMarker().isValid();
    }

    private void updateVisibilityForNpc(NPC npc, MarkerPair pair) {
        Entity npcEntity = npc.getEntity();
        if (npcEntity == null) return;

        for (Player player : npcEntity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(npcEntity.getLocation()) > VISIBILITY_RANGE_SQUARED) {
                // 範囲外なら非表示
                player.hideEntity(plugin, pair.availableMarker());
                player.hideEntity(plugin, pair.completeMarker());
                continue;
            }

            MarkerType type = resolveMarkerType(player, npc.getName());
            switch (type) {
                case COMPLETE -> {
                    player.showEntity(plugin, pair.completeMarker());
                    player.hideEntity(plugin, pair.availableMarker());
                }
                case AVAILABLE -> {
                    player.showEntity(plugin, pair.availableMarker());
                    player.hideEntity(plugin, pair.completeMarker());
                }
                case NONE -> {
                    player.hideEntity(plugin, pair.availableMarker());
                    player.hideEntity(plugin, pair.completeMarker());
                }
            }
        }
    }

    public MarkerType resolveMarkerType(Player player, String npcName) {
        UUID playerId = player.getUniqueId();
        List<Quest> allQuests = questService.getAllQuests();

        boolean hasAvailable = false;

        for (Quest quest : allQuests) {
            if (!matchesNpc(quest, npcName)) continue;

            String questId = quest.getId();
            QuestState state = questService.getState(playerId, questId);

            // 1. 完了報告可能（最優先）
            if (state == QuestState.ACCEPTED) {
                if (questService.checkCompletion(player, questId)) {
                    return MarkerType.COMPLETE;
                }
            }

            // 2. 受注可能判定
            if (state != QuestState.ACCEPTED) {
                if (quest.getCategory() == QuestCategory.DAILY) {
                    if (questService.getRemainingDailyCompletions(playerId) > 0) {
                        hasAvailable = true;
                    }
                } else if (!quest.isRepeatable()) {
                    if (state != QuestState.COMPLETED && state != QuestState.TURNED_IN) {
                        hasAvailable = true;
                    }
                } else {
                    hasAvailable = true;
                }
            }
        }

        return hasAvailable ? MarkerType.AVAILABLE : MarkerType.NONE;
    }

    /**
     * プレイヤーのクエスト状態が変化した際に即座にマーカーの可視性を更新します。
     */
    public void refreshPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("Citizens")) return;

        for (Map.Entry<Integer, MarkerPair> entry : npcMarkers.entrySet()) {
            NPC npc = CitizensAPI.getNPCRegistry().getById(entry.getKey());
            if (npc == null || !npc.isSpawned()) continue;

            MarkerPair pair = entry.getValue();
            if (!isMarkerValid(pair)) continue;

            Entity entity = npc.getEntity();
            if (entity == null || !entity.getWorld().equals(player.getWorld())) {
                player.hideEntity(plugin, pair.availableMarker());
                player.hideEntity(plugin, pair.completeMarker());
                continue;
            }

            if (player.getLocation().distanceSquared(entity.getLocation()) > VISIBILITY_RANGE_SQUARED) {
                player.hideEntity(plugin, pair.availableMarker());
                player.hideEntity(plugin, pair.completeMarker());
                continue;
            }

            MarkerType type = resolveMarkerType(player, npc.getName());
            switch (type) {
                case COMPLETE -> {
                    player.showEntity(plugin, pair.completeMarker());
                    player.hideEntity(plugin, pair.availableMarker());
                }
                case AVAILABLE -> {
                    player.showEntity(plugin, pair.availableMarker());
                    player.hideEntity(plugin, pair.completeMarker());
                }
                case NONE -> {
                    player.hideEntity(plugin, pair.availableMarker());
                    player.hideEntity(plugin, pair.completeMarker());
                }
            }
        }
    }

    // ========================================================================
    // イベントリスナー
    // ========================================================================

    @EventHandler
    public void onNPCSpawn(NPCSpawnEvent event) {
        NPC npc = event.getNPC();
        if (isQuestNpc(npc.getName())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> createMarkersForNpc(npc), 5L);
        }
    }

    @EventHandler
    public void onNPCDespawn(NPCDespawnEvent event) {
        MarkerPair pair = npcMarkers.remove(event.getNPC().getId());
        if (pair != null) {
            pair.remove();
        }
    }

    @EventHandler
    public void onNPCRemove(NPCRemoveEvent event) {
        MarkerPair pair = npcMarkers.remove(event.getNPC().getId());
        if (pair != null) {
            pair.remove();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshPlayer(event.getPlayer()), 20L);
    }
}

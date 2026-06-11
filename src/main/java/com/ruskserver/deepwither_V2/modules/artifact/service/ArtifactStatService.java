package com.ruskserver.deepwither_V2.modules.artifact.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactData;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactPDCUtil;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactSaveData;
import com.ruskserver.deepwither_V2.modules.artifact.ArtifactSetType;
import com.ruskserver.deepwither_V2.modules.character.event.CharacterSelectEvent;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ArtifactStatService implements Listener, PlayerLifecycleTask {

    private final ArtifactEquipmentService equipmentService;
    private final StatManager statManager;
    private final ArtifactPDCUtil pdcUtil;

    @Inject
    public ArtifactStatService(ArtifactEquipmentService equipmentService, StatManager statManager, ArtifactPDCUtil pdcUtil) {
        this.equipmentService = equipmentService;
        this.statManager = statManager;
        this.pdcUtil = pdcUtil;
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.JOIN);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.STATS;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> context.player().ifPresent(this::applyArtifactStats));
    }

    @EventHandler
    public void onCharacterSelect(CharacterSelectEvent event) {
        applyArtifactStats(event.getPlayer());
    }

    public void applyArtifactStats(Player player) {
        UUID uuid = player.getUniqueId();

        // まず古いアーティファクト関連のモディファイアをすべて削除
        for (StatType type : StatType.values()) {
            statManager.removeModifier(uuid, type, "artifact_main");
            statManager.removeModifier(uuid, type, "artifact_sub");
            for (ArtifactSetType setType : ArtifactSetType.values()) {
                statManager.removeModifier(uuid, type, "artifact_set_2pc_" + setType.name());
                statManager.removeModifier(uuid, type, "artifact_set_3pc_" + setType.name());
            }
        }

        ArtifactSaveData artifactData = equipmentService.getEquippedArtifacts(player).orElse(null);
        if (artifactData == null) return;

        Map<StatType, Double> totalMainStats = new EnumMap<>(StatType.class);
        Map<StatType, Double> totalSubStats = new EnumMap<>(StatType.class);
        Map<ArtifactSetType, Integer> setCount = new EnumMap<>(ArtifactSetType.class);

        // 集計
        for (String base64 : artifactData.getEquippedArtifacts().values()) {
            if (base64 == null || base64.isEmpty()) continue;
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                ItemStack item = ItemStack.deserializeBytes(bytes);
                ArtifactData ad = pdcUtil.getArtifactData(item);

                if (ad != null) {
                    // メインステータス
                    totalMainStats.put(ad.getMainStat(), totalMainStats.getOrDefault(ad.getMainStat(), 0.0) + ad.getMainStatValue());

                    // サブステータス
                    for (Map.Entry<StatType, Double> sub : ad.getSubStats().entrySet()) {
                        totalSubStats.put(sub.getKey(), totalSubStats.getOrDefault(sub.getKey(), 0.0) + sub.getValue());
                    }

                    // セット効果カウント
                    setCount.put(ad.getSetType(), setCount.getOrDefault(ad.getSetType(), 0) + 1);
                }
            } catch (Exception ignored) {
            }
        }

        // StatManagerに登録 (加算として処理)
        for (Map.Entry<StatType, Double> entry : totalMainStats.entrySet()) {
            statManager.setModifier(uuid, entry.getKey(), "artifact_main", entry.getValue(), ModifierType.ADDITIVE);
        }
        for (Map.Entry<StatType, Double> entry : totalSubStats.entrySet()) {
            statManager.setModifier(uuid, entry.getKey(), "artifact_sub", entry.getValue(), ModifierType.ADDITIVE);
        }

        // セット効果適用
        applySetBonuses(uuid, setCount);
    }

    private void applySetBonuses(UUID uuid, Map<ArtifactSetType, Integer> setCount) {
        for (Map.Entry<ArtifactSetType, Integer> entry : setCount.entrySet()) {
            ArtifactSetType setType = entry.getKey();
            int count = entry.getValue();

            if (count >= 2) {
                String sourceId2pc = "artifact_set_2pc_" + setType.name();
                switch (setType) {
                    case ABYSS_PULSATION -> {
                        statManager.setModifier(uuid, StatType.HEALTH, sourceId2pc, 0.1, ModifierType.MULTIPLICATIVE);
                        statManager.setModifier(uuid, StatType.MAGIC_DAMAGE, sourceId2pc, 0.08, ModifierType.MULTIPLICATIVE);
                    }
                    case CELESTIAL_RESONANCE -> {
                        statManager.setModifier(uuid, StatType.MAX_MANA, sourceId2pc, 60.0, ModifierType.ADDITIVE);
                        statManager.setModifier(uuid, StatType.MAGIC_DAMAGE, sourceId2pc, 0.15, ModifierType.MULTIPLICATIVE);
                    }
                    case FAULT_LINE -> {
                        statManager.setModifier(uuid, StatType.ATTACK_DAMAGE, sourceId2pc, 12.0, ModifierType.ADDITIVE);
                        statManager.setModifier(uuid, StatType.CRITICAL_CHANCE, sourceId2pc, 0.02, ModifierType.MULTIPLICATIVE);
                    }
                    case ASTRAL_STEEL_GUARD -> {
                        statManager.setModifier(uuid, StatType.DEFENSE, sourceId2pc, 25.0, ModifierType.ADDITIVE);
                    }
                    case LUNAR_SKIRMISHER -> {
                        statManager.setModifier(uuid, StatType.SPEED, sourceId2pc, 0.02, ModifierType.MULTIPLICATIVE);
                        statManager.setModifier(uuid, StatType.CRITICAL_DAMAGE, sourceId2pc, 0.10, ModifierType.MULTIPLICATIVE);
                    }
                    case ETERNAL_HEARTS -> {
                        statManager.setModifier(uuid, StatType.HEALTH, sourceId2pc, 0.25, ModifierType.MULTIPLICATIVE);
                        statManager.setModifier(uuid, StatType.DEFENSE, sourceId2pc, 0.10, ModifierType.MULTIPLICATIVE);
                    }
                }
            }

            if (count >= 3) {
                String sourceId3pc = "artifact_set_3pc_" + setType.name();
                switch (setType) {
                    case ASTRAL_STEEL_GUARD -> {
                        // 物理ダメージ 15% 軽減
                        statManager.setModifier(uuid, StatType.PHYSICAL_DAMAGE_REDUCTION, sourceId3pc, 0.15, ModifierType.ADDITIVE);
                    }
                }
            }
        }
    }
}

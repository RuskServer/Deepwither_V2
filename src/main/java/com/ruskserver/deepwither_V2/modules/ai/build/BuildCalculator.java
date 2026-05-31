package com.ruskserver.deepwither_V2.modules.ai.build;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillRegistry;

import java.util.*;
import java.util.logging.Logger;

@Service
public class BuildCalculator {

    private static final double VIT_DEFENSE_MULTIPLIER = 0.005;

    private final ItemManager itemManager;
    private final Logger log;

    @Inject
    public BuildCalculator(ItemManager itemManager, Logger log) {
        this.itemManager = itemManager;
        this.log = log;
    }

    public List<BuildCandidate> preroll(BuildGoal goal) {
        List<CustomItem> allItems = new ArrayList<>();
        for (String id : itemManager.getRegisteredItemIds()) {
            CustomItem item = itemManager.getCustomItem(id);
            if (item != null) allItems.add(item);
        }

        List<CustomItem> chests = filterSlot(allItems, "chestplate");
        List<CustomItem> legs = filterSlot(allItems, "leggings");
        List<CustomItem> heads = filterSlot(allItems, "helmet", "hood", "headguard", "hood");
        List<CustomItem> boots = filterSlot(allItems, "boots", "footactuator");
        List<CustomItem> weapons = allItems.stream()
                .filter(i -> i.getWeaponType() != null)
                .toList();

        if (chests.isEmpty()) chests = allItems;
        if (legs.isEmpty()) legs = allItems;
        if (heads.isEmpty()) heads = allItems;
        if (boots.isEmpty()) boots = allItems;

        List<BuildCandidate> candidates = new ArrayList<>();

        for (CustomItem chest : chests) {
            for (CustomItem leg : legs) {
                for (CustomItem head : heads) {
                    for (CustomItem boot : boots) {
                        if (weapons.isEmpty()) {
                            candidates.add(compute(chest, leg, head, boot, null, goal));
                        } else {
                            for (CustomItem weapon : weapons) {
                                candidates.add(compute(chest, leg, head, boot, weapon, goal));
                            }
                        }
                    }
                }
            }
        }

        candidates.sort(null);

        int limit = Math.min(10, candidates.size());
        return candidates.subList(0, limit);
    }

    public BuildCandidate compute(CustomItem chest, CustomItem legs, CustomItem head,
                                   CustomItem boots, CustomItem weapon, BuildGoal goal) {
        BuildCandidate candidate = new BuildCandidate();
        candidate.setSlot("胴", chest != null ? chest.getDisplayName() : "なし");
        candidate.setSlot("脚", legs != null ? legs.getDisplayName() : "なし");
        candidate.setSlot("頭", head != null ? head.getDisplayName() : "なし");
        candidate.setSlot("足", boots != null ? boots.getDisplayName() : "なし");
        if (weapon != null) candidate.setSlot("武器", weapon.getDisplayName());

        Map<StatType, Double> additiveSum = new EnumMap<>(StatType.class);

        accumulateItemStats(chest, additiveSum);
        accumulateItemStats(legs, additiveSum);
        accumulateItemStats(head, additiveSum);
        accumulateItemStats(boots, additiveSum);
        accumulateItemStats(weapon, additiveSum);

        double vitDefMult = 1.0 + goal.getVit() * VIT_DEFENSE_MULTIPLIER;

        for (StatType type : StatType.values()) {
            double additive = additiveSum.getOrDefault(type, 0.0);
            double finalValue = additive;
            if (type == StatType.DEFENSE) {
                finalValue = additive * vitDefMult;
            }
            candidate.setFinalStat(type, finalValue);
        }

        double primary = candidate.getFinalStat(goal.getPrimaryStat());
        double score = primary;

        if (goal.getSecondaryStat() != null) {
            double secondary = candidate.getFinalStat(goal.getSecondaryStat());
            double maxP = Math.max(primary, 1.0);
            double maxS = Math.max(secondary, 1.0);
            double normP = primary / maxP * goal.getPrimaryWeight();
            double normS = secondary / maxS * (1.0 - goal.getPrimaryWeight());
            score = (normP + normS) * 100.0;
        }

        candidate.setScore(score);
        return candidate;
    }

    private void accumulateItemStats(CustomItem item, Map<StatType, Double> additiveSum) {
        if (item == null) return;
        for (Map.Entry<StatType, Double> entry : item.getBaseStats().entrySet()) {
            additiveSum.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
    }

    private List<CustomItem> filterSlot(List<CustomItem> items, String... keywords) {
        List<CustomItem> result = new ArrayList<>();
        for (CustomItem item : items) {
            String id = item.getId().toLowerCase();
            for (String kw : keywords) {
                if (id.contains(kw)) {
                    result.add(item);
                    break;
                }
            }
        }
        return result;
    }
}

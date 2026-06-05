package com.ruskserver.deepwither_V2.modules.item.modifier;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;

import java.util.*;

@Service
public class ModifierManager {

    private final Random random = new Random();

    public ModifierRollResult rollModifiers(CustomItem item) {
        ItemCategory category = item.getItemCategory();
        ItemRarity rarity = item.getRarity();
        Map<StatType, Double> baseStats = item.getBaseStats();

        Map<StatType, Double> baseModifiers = new EnumMap<>(StatType.class);
        Map<StatType, Double> addedStats = new EnumMap<>(StatType.class);
        List<SpecialEffectInstance> specialEffects = new ArrayList<>();

        int baseModCount = rollBaseModifierCount(rarity);
        int addedStatCount = rollAddedStatCount(rarity);

        // ベース修飾: ベースに含まれるStatTypeから重み付き選択
        if (!baseStats.isEmpty() && baseModCount > 0) {
            Set<StatType> baseKeys = baseStats.keySet();
            for (int i = 0; i < baseModCount; i++) {
                StatType selected = weightedSelect(baseKeys, category, baseModifiers.keySet());
                if (selected == null) break;
                double bonus = rollBonusValue(selected, baseStats.get(selected), rarity, false);
                baseModifiers.merge(selected, bonus, Double::sum);
            }
        }

        // 追加ステータス: ベースに含まれないStatTypeから重み付き選択
        if (addedStatCount > 0) {
            Set<StatType> allStats = new HashSet<>(Arrays.asList(StatType.values()));
            Set<StatType> blocked = new HashSet<>(baseStats.keySet());
            blocked.addAll(baseModifiers.keySet());
            // アイテム側で明示的に許可リストがある場合
            Set<StatType> allowed = item.getAllowedAddedStats();
            if (allowed != null) {
                allStats.retainAll(allowed);
            }
            allStats.removeAll(blocked);

            for (int i = 0; i < addedStatCount; i++) {
                StatType selected = weightedSelect(allStats, category, addedStats.keySet());
                if (selected == null) break;
                double baseForStat = getStatBaseValue(selected);
                double value = rollBonusValue(selected, baseForStat, rarity, true);
                addedStats.put(selected, value);
                allStats.remove(selected);
            }
        }

        // 特殊効果抽選
        if (rarity.ordinal() >= ItemRarity.UNCOMMON.ordinal()) {
            int effectCount = rollEffectCount(rarity);
            List<SpecialEffect> available = new ArrayList<>(Arrays.asList(SpecialEffect.values()));
            Collections.shuffle(available, random);
            for (int i = 0; i < effectCount && i < available.size(); i++) {
                if (random.nextDouble() < getEffectChance(rarity, i)) {
                    specialEffects.add(new SpecialEffectInstance(available.get(i), 1));
                }
            }
        }

        return new ModifierRollResult(baseModifiers, addedStats, specialEffects);
    }

    /**
     * 重み付きランダム選択。親和性マトリクスを使い、ベースに含まれるStatTypeは
     * 自動でPRIMARY扱いでブースト。
     */
    private StatType weightedSelect(Set<StatType> candidates, ItemCategory category, Set<StatType> exclude) {
        List<StatType> pool = new ArrayList<>(candidates);
        pool.removeAll(exclude);
        if (pool.isEmpty()) return null;

        double totalWeight = 0;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            StatType type = pool.get(i);
            StatAffinity affinity = StatAffinity.of(category, type);
            double w = affinity.getWeight();
            weights[i] = w;
            totalWeight += w;
        }

        if (totalWeight <= 0) return null;

        double roll = random.nextDouble() * totalWeight;
        for (int i = 0; i < pool.size(); i++) {
            roll -= weights[i];
            if (roll <= 0) return pool.get(i);
        }

        return pool.get(pool.size() - 1);
    }

    private int rollBaseModifierCount(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> random.nextInt(2);       // 0-1
            case UNCOMMON -> random.nextInt(3);     // 0-2
            case RARE -> 1 + random.nextInt(2);     // 1-2
            case EPIC -> 1 + random.nextInt(3);     // 1-3
            case LEGENDARY -> 2 + random.nextInt(3); // 2-4
        };
    }

    private int rollAddedStatCount(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> random.nextInt(2);     // 0-1
            case RARE -> random.nextInt(2);          // 0-1
            case EPIC -> 1 + random.nextInt(2);      // 1-2
            case LEGENDARY -> 1 + random.nextInt(3); // 1-3
        };
    }

    /**
     * レアリティに応じたボーナス値の倍率範囲を返す。
     */
    private double[] getBonusRange(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> new double[]{0.01, 0.05};
            case UNCOMMON -> new double[]{0.02, 0.08};
            case RARE -> new double[]{0.03, 0.12};
            case EPIC -> new double[]{0.05, 0.18};
            case LEGENDARY -> new double[]{0.08, 0.30};
        };
    }

    private double rollBonusValue(StatType stat, double baseValue, ItemRarity rarity, boolean isAddedStat) {
        double[] range = getBonusRange(rarity);
        double minPct = range[0];
        double maxPct = range[1];

        // 追加ステータスの場合、最低保証を少し上げる（新しく付ける価値を持たせる）
        if (isAddedStat) {
            minPct += 0.03;
            maxPct += 0.05;
        }

        double pct = minPct + (random.nextDouble() * (maxPct - minPct));
        double bonus = baseValue * pct;

        return Math.round(bonus * 10.0) / 10.0;
    }

    /**
     * 追加ステータス用の基準値（ArtifactGenerator方式を流用）。
     * ベース値が存在しないStatTypeに新しい値を付与する際の基準として使う。
     */
    private double getStatBaseValue(StatType stat) {
        return switch (stat) {
            case HEALTH -> 10.0;
            case MAX_MANA -> 20.0;
            case ATTACK_DAMAGE, MAGIC_DAMAGE -> 8.0;
            case DEFENSE, MAGIC_DEFENSE -> 8.0;
            case CRITICAL_CHANCE -> 4.0;
            case CRITICAL_DAMAGE -> 10.0;
            case ATTACK_SPEED -> 1.2;
            case SPEED -> 1.0;
            case COOLDOWN_REDUCTION -> 3.0;
            case FIRE_DAMAGE, ICE_DAMAGE, LIGHTNING_DAMAGE -> 4.0;
        };
    }

    private int rollEffectCount(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> random.nextDouble() < 0.15 ? 1 : 0;
            case RARE -> random.nextDouble() < 0.35 ? 1 : 0;
            case EPIC -> random.nextDouble() < 0.65 ? (random.nextDouble() < 0.3 ? 2 : 1) : 0;
            case LEGENDARY -> 1 + (random.nextDouble() < 0.4 ? 1 : 0);
        };
    }

    private double getEffectChance(ItemRarity rarity, int index) {
        return switch (rarity) {
            case LEGENDARY -> 1.0;
            case EPIC -> 0.8;
            case RARE -> 0.6;
            case UNCOMMON -> 0.4;
            default -> 0.0;
        };
    }
}

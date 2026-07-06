package com.ruskserver.deepwither_V2.modules.dungeon.modifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DungeonModifierContext {

    private static final DungeonModifierContext NONE = new DungeonModifierContext(Collections.emptySet());

    private final Set<DungeonModifier> modifiers;

    private final double combinedMobAtk;
    private final double combinedMobHp;
    private final double combinedMobCount;
    private final double combinedLootQty;
    private final double combinedLootQual;
    private final double combinedTimeLimit;
    private final int combinedExtraLives;

    public DungeonModifierContext(Set<DungeonModifier> modifiers) {
        this.modifiers = Collections.unmodifiableSet(new HashSet<>(modifiers));
        double atk = 1.0, hp = 1.0, count = 1.0, qty = 1.0, qual = 1.0, time = 1.0;
        int lives = 0;
        for (var m : this.modifiers) {
            atk *= m.mobAtkMultiplier();
            hp *= m.mobHpMultiplier();
            count *= m.mobCountMultiplier();
            qty *= m.lootQtyMultiplier();
            qual *= m.lootQualMultiplier();
            time *= m.timeLimitMultiplier();
            lives += m.extraLives();
        }
        this.combinedMobAtk = atk;
        this.combinedMobHp = hp;
        this.combinedMobCount = count;
        this.combinedLootQty = qty;
        this.combinedLootQual = qual;
        this.combinedTimeLimit = time;
        this.combinedExtraLives = lives;
    }

    public static DungeonModifierContext none() {
        return NONE;
    }

    public static DungeonModifierContext of(DungeonModifier... mods) {
        return new DungeonModifierContext(new HashSet<>(Arrays.asList(mods)));
    }

    public Set<DungeonModifier> modifiers() { return modifiers; }
    public double combinedMobAtk() { return combinedMobAtk; }
    public double combinedMobHp() { return combinedMobHp; }
    public double combinedMobCount() { return combinedMobCount; }
    public double combinedLootQty() { return combinedLootQty; }
    public double combinedLootQual() { return combinedLootQual; }
    public double combinedTimeLimit() { return combinedTimeLimit; }
    public int combinedExtraLives() { return combinedExtraLives; }
    public boolean isPresent() { return !modifiers.isEmpty(); }

    public String toIdString() {
        return modifiers.stream().map(DungeonModifier::id).reduce((a, b) -> a + "," + b).orElse("");
    }

    public static DungeonModifierContext fromIdString(String s) {
        if (s == null || s.isBlank()) return NONE;
        Set<DungeonModifier> set = new HashSet<>();
        for (String id : s.split(",")) {
            var m = DungeonModifier.byId(id.trim());
            if (m != null) set.add(m);
        }
        return set.isEmpty() ? NONE : new DungeonModifierContext(set);
    }
}

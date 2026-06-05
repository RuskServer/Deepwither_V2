package com.ruskserver.deepwither_V2.modules.item.modifier;

public class SpecialEffectInstance {

    private final SpecialEffect effect;
    private final int level;

    public SpecialEffectInstance(SpecialEffect effect, int level) {
        this.effect = effect;
        this.level = level;
    }

    public SpecialEffect getEffect() {
        return effect;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayString() {
        String levelStr = level > 1 ? " Lv." + level : "";
        return effect.getDisplayName() + levelStr;
    }
}

package com.ruskserver.deepwither_V2.modules.combat.damage;

import org.bukkit.entity.LivingEntity;

/**
 * ダメージ計算パイプライン内を流れるコンテキストデータ。
 */
public class DamageContext {

    private final LivingEntity attacker; // 環境ダメージなどの場合は null になる可能性があります
    private final LivingEntity defender;
    private final DamageType type;
    
    private double damage;
    private boolean isCritical = false;

    public DamageContext(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage) {
        this.attacker = attacker;
        this.defender = defender;
        this.type = type;
        this.damage = initialDamage;
    }

    public LivingEntity getAttacker() {
        return attacker;
    }

    public LivingEntity getDefender() {
        return defender;
    }

    public DamageType getType() {
        return type;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = Math.max(0, damage); // ダメージがマイナスにならないように保護
    }

    public void addDamage(double amount) {
        this.damage = Math.max(0, this.damage + amount);
    }

    public void multiplyDamage(double multiplier) {
        this.damage = Math.max(0, this.damage * multiplier);
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }
}

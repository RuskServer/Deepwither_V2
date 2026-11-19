package com.ruskserver.deepwither_V2.modules.combat.damage;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ダメージ計算パイプライン内を流れるマルチチャンネル・コンテキストデータ。
 * <p>
 * 内部では {@link DamageChannel} ごとのダメージマップを保持しつつ、
 * 既存の {@link #getDamage()} / {@link #setDamage(double)} / {@link #multiplyDamage(double)} API も
 * 全チャンネル合計および比例スケーリングとして後方互換を維持する。
 */
public class DamageContext {

    private final LivingEntity attacker;
    private final LivingEntity defender;
    private final DamageType type;
    private final Set<String> tags = new HashSet<>();
    private final EnumMap<DamageChannel, Double> channels = new EnumMap<>(DamageChannel.class);

    private double distanceMultiplier = 1.0;
    private double staggerMultiplier = 1.0;
    private boolean isCritical = false;
    private boolean evaded = false;
    private boolean blocked = false;
    private double magicShieldAbsorbed = 0.0;
    private Location impactLocation;

    public DamageContext(LivingEntity attacker, LivingEntity defender, DamageType type, double initialDamage) {
        this.attacker = attacker;
        this.defender = defender;
        this.type = type;
        if (initialDamage > 0.0) {
            this.channels.put(defaultChannelFor(type), initialDamage);
        }
    }

    public static DamageChannel defaultChannelFor(DamageType type) {
        if (type == null) return DamageChannel.PHYSICAL;
        return switch (type) {
            case PHYSICAL, RANGED -> DamageChannel.PHYSICAL;
            case MAGIC -> DamageChannel.FIRE; // タグがあれば Base フェーズで適切な属性へ振り直す
            case TRUE_DAMAGE, ENVIRONMENTAL -> DamageChannel.TRUE;
        };
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

    public EnumMap<DamageChannel, Double> getChannelMap() {
        return channels;
    }

    public Map<DamageChannel, Double> getChannelsView() {
        return Collections.unmodifiableMap(channels);
    }

    public double getChannelDamage(DamageChannel channel) {
        return channels.getOrDefault(channel, 0.0);
    }

    public void setChannelDamage(DamageChannel channel, double amount) {
        if (amount <= 0.0) {
            channels.remove(channel);
        } else {
            channels.put(channel, amount);
        }
    }

    public void addChannelDamage(DamageChannel channel, double amount) {
        if (amount == 0.0) return;
        double next = Math.max(0.0, channels.getOrDefault(channel, 0.0) + amount);
        setChannelDamage(channel, next);
    }

    public void multiplyChannelDamage(DamageChannel channel, double multiplier) {
        Double cur = channels.get(channel);
        if (cur == null) return;
        setChannelDamage(channel, cur * Math.max(0.0, multiplier));
    }

    /** 全チャンネル合計ダメージ (後方互換 API) */
    public double getDamage() {
        if (evaded) return 0.0;
        double sum = 0.0;
        for (double v : channels.values()) {
            if (v > 0.0) sum += v;
        }
        return sum;
    }

    /**
     * 合計ダメージを指定値へ比例スケールする (既存フェーズとの後方互換用)。
     */
    public void setDamage(double damage) {
        double safe = Math.max(0.0, damage);
        if (safe <= 0.0) {
            channels.clear();
            return;
        }
        double current = getDamage();
        if (current <= 0.0 || channels.isEmpty()) {
            channels.clear();
            channels.put(resolvePrimaryChannel(), safe);
            return;
        }
        double ratio = safe / current;
        for (DamageChannel ch : new ArrayListCopy<>(channels.keySet())) {
            channels.put(ch, channels.get(ch) * ratio);
        }
    }

    public void addDamage(double amount) {
        if (amount == 0.0) return;
        if (amount > 0.0) {
            addChannelDamage(resolvePrimaryChannel(), amount);
        } else {
            setDamage(Math.max(0.0, getDamage() + amount));
        }
    }

    public void multiplyDamage(double multiplier) {
        double m = Math.max(0.0, multiplier);
        if (m <= 0.0) {
            channels.clear();
            return;
        }
        for (DamageChannel ch : new ArrayListCopy<>(channels.keySet())) {
            channels.put(ch, channels.get(ch) * m);
        }
    }

    /** マジックシールドで吸収可能なダメージ合計 (物理・火・氷・雷・聖) */
    public double getShieldableDamage() {
        if (evaded) return 0.0;
        double sum = 0.0;
        for (Map.Entry<DamageChannel, Double> e : channels.entrySet()) {
            if (!e.getKey().bypassesMagicShield() && e.getValue() > 0.0) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    /** マジックシールドを貫通して HP を直接削るダメージ合計 (深淵・確定) */
    public double getMagicShieldBypassDamage() {
        if (evaded) return 0.0;
        double sum = 0.0;
        for (Map.Entry<DamageChannel, Double> e : channels.entrySet()) {
            if (e.getKey().bypassesMagicShield() && e.getValue() > 0.0) {
                sum += e.getValue();
            }
        }
        return sum;
    }

    /** タグと DamageType から主チャンネルを推定する */
    public DamageChannel resolvePrimaryChannel() {
        if (type == DamageType.TRUE_DAMAGE || type == DamageType.ENVIRONMENTAL) {
            return DamageChannel.TRUE;
        }
        if (hasTagIgnoreCase("abyss")) return DamageChannel.ABYSS;
        if (hasTagIgnoreCase("fire")) return DamageChannel.FIRE;
        if (hasTagIgnoreCase("ice")) return DamageChannel.ICE;
        if (hasTagIgnoreCase("lightning")) return DamageChannel.LIGHTNING;
        if (hasTagIgnoreCase("holy")) return DamageChannel.HOLY;
        if (type == DamageType.MAGIC) return DamageChannel.FIRE;
        return DamageChannel.PHYSICAL;
    }

    public boolean hasTagIgnoreCase(String tag) {
        if (tag == null) return false;
        if (tags.contains(tag)) return true;
        for (String t : tags) {
            if (t.equalsIgnoreCase(tag)) return true;
        }
        return false;
    }

    public double getDistanceMultiplier() {
        return distanceMultiplier;
    }

    public void setDistanceMultiplier(double distanceMultiplier) {
        this.distanceMultiplier = Math.max(0.01, distanceMultiplier);
    }

    public double getStaggerMultiplier() {
        return staggerMultiplier;
    }

    public void setStaggerMultiplier(double staggerMultiplier) {
        this.staggerMultiplier = Math.max(0.0, staggerMultiplier);
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    public boolean isEvaded() {
        return evaded;
    }

    public void setEvaded(boolean evaded) {
        this.evaded = evaded;
        if (evaded) {
            channels.clear();
        }
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public double getMagicShieldAbsorbed() {
        return magicShieldAbsorbed;
    }

    public void setMagicShieldAbsorbed(double magicShieldAbsorbed) {
        this.magicShieldAbsorbed = Math.max(0.0, magicShieldAbsorbed);
    }

    public Location getImpactLocation() {
        return impactLocation == null ? null : impactLocation.clone();
    }

    public void setImpactLocation(Location impactLocation) {
        this.impactLocation = impactLocation == null ? null : impactLocation.clone();
    }

    public void addTag(String tag) {
        if (tag != null) this.tags.add(tag);
    }

    public void addTags(Set<String> tags) {
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    private static final class ArrayListCopy<T> extends java.util.ArrayList<T> {
        ArrayListCopy(java.util.Collection<T> c) {
            super(c);
        }
    }
}

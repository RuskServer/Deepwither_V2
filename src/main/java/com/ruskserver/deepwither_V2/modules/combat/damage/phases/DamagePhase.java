package com.ruskserver.deepwither_V2.modules.combat.damage.phases;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageChannel;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageMath;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.modules.stat.StatProfile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PoE / MythicLib 式ダメージパイプラインの各フェーズ。
 * <p>
 * 実行順:
 * {@code Base (Flat+属性基礎) -> Conversion (一方向属性変換) -> Scaling (Increased×More + ポーション)
 * -> Avoidance (エントロピー回避 + ブロック) -> Critical -> Mitigation (チャンネル別アーマー/耐性/貫通)}
 */
public interface DamagePhase {
    void process(DamageContext context);

    /**
     * 1. 基礎ダメージ (Flat) をチャンネル別に初期化し、属性フラット加算もここで合流させる。
     */
    class Base implements DamagePhase {
        private final StatManager statManager;

        public Base(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            if (context.getAttacker() == null) return;

            // initialDamage が既に指定されている場合は、タグから推定した正しい主チャンネルへ移し替える
            double existing = context.getDamage();
            DamageChannel primary = context.resolvePrimaryChannel();
            if (existing > 0.0) {
                context.getChannelMap().clear();
                context.setChannelDamage(primary, existing);
            } else {
                double base = switch (context.getType()) {
                    case PHYSICAL -> statManager.getTotalStat(context.getAttacker(), StatType.ATTACK_DAMAGE);
                    case RANGED -> statManager.getTotalStat(context.getAttacker(), StatType.RANGED_DAMAGE);
                    case MAGIC -> statManager.getTotalStat(context.getAttacker(), StatType.MAGIC_DAMAGE);
                    case TRUE_DAMAGE, ENVIRONMENTAL -> 0.0;
                };
                if (base <= 0.0 && context.getType() != DamageType.TRUE_DAMAGE && context.getType() != DamageType.ENVIRONMENTAL) {
                    base = 1.0;
                }
                context.setChannelDamage(primary, base);
            }

            // 属性フラットダメージを対応チャンネルへ合流 (クリティカル・変換・耐性の対象になる)
            if (context.getType() != DamageType.TRUE_DAMAGE && context.getType() != DamageType.ENVIRONMENTAL) {
                addFlatElement(context, StatType.FIRE_DAMAGE, DamageChannel.FIRE, "fire");
                addFlatElement(context, StatType.ICE_DAMAGE, DamageChannel.ICE, "ice");
                addFlatElement(context, StatType.LIGHTNING_DAMAGE, DamageChannel.LIGHTNING, "lightning");
                addFlatElement(context, StatType.HOLY_DAMAGE, DamageChannel.HOLY, "holy");
                addFlatElement(context, StatType.ABYSS_DAMAGE, DamageChannel.ABYSS, "abyss");
            }

            if (context.getDistanceMultiplier() != 1.0) {
                context.multiplyDamage(context.getDistanceMultiplier());
            }
        }

        private void addFlatElement(DamageContext ctx, StatType stat, DamageChannel ch, String tag) {
            // 魔法は該当タグ持ちのとき、物理・射撃は武器エンチャント/装備由来のフラット属性として常時合流
            boolean applies = (ctx.getType() == DamageType.PHYSICAL || ctx.getType() == DamageType.RANGED)
                    || ctx.hasTagIgnoreCase(tag);
            if (!applies) return;
            double flat = statManager.getTotalStat(ctx.getAttacker(), stat);
            if (flat > 0.0) {
                ctx.addChannelDamage(ch, flat);
            }
        }
    }

    /**
     * 2. 一方向属性変換 (PHYSICAL -> LIGHTNING -> ICE -> FIRE -> ABYSS / ELEMENTAL -> ABYSS)。
     */
    class Conversion implements DamagePhase {
        private final StatManager statManager;

        public Conversion(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            if (context.getAttacker() == null) return;
            if (context.getType() == DamageType.TRUE_DAMAGE || context.getType() == DamageType.ENVIRONMENTAL) return;

            EnumMap<DamageChannel, Double> channels = context.getChannelMap();
            if (channels.getOrDefault(DamageChannel.PHYSICAL, 0.0) > 0.0) {
                Map<DamageChannel, Double> physConv = new EnumMap<>(DamageChannel.class);
                physConv.put(DamageChannel.LIGHTNING, statManager.getTotalStat(context.getAttacker(), StatType.PHYS_TO_LIGHTNING_CONVERSION));
                physConv.put(DamageChannel.ICE, statManager.getTotalStat(context.getAttacker(), StatType.PHYS_TO_ICE_CONVERSION));
                physConv.put(DamageChannel.FIRE, statManager.getTotalStat(context.getAttacker(), StatType.PHYS_TO_FIRE_CONVERSION));
                physConv.put(DamageChannel.ABYSS, statManager.getTotalStat(context.getAttacker(), StatType.PHYS_TO_ABYSS_CONVERSION));
                DamageMath.convertChannel(channels, DamageChannel.PHYSICAL, physConv);
            }

            double elemToAbyss = statManager.getTotalStat(context.getAttacker(), StatType.ELEMENTAL_TO_ABYSS_CONVERSION);
            if (elemToAbyss > 0.0) {
                Map<DamageChannel, Double> toAbyss = Map.of(DamageChannel.ABYSS, elemToAbyss);
                DamageMath.convertChannel(channels, DamageChannel.LIGHTNING, toAbyss);
                DamageMath.convertChannel(channels, DamageChannel.ICE, toAbyss);
                DamageMath.convertChannel(channels, DamageChannel.FIRE, toAbyss);
                DamageMath.convertChannel(channels, DamageChannel.HOLY, toAbyss);
            }
        }
    }

    /**
     * 3. PoE 式 Increased（加算%）× More（独立乗算%）スケーリング。
     */
    class Scaling implements DamagePhase {
        private final StatManager statManager;

        public Scaling(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            if (context.getAttacker() == null) return;
            if (context.getType() == DamageType.TRUE_DAMAGE || context.getType() == DamageType.ENVIRONMENTAL) return;

            StatProfile profile = statManager.getProfile(context.getAttacker().getUniqueId());
            double globalInc = profile.getAdditiveSum(StatType.INCREASED_DAMAGE) / 100.0;
            double globalMore = profile.getMoreMultiplier(StatType.INCREASED_DAMAGE);
            double elemInc = profile.getAdditiveSum(StatType.INCREASED_ELEMENTAL_DAMAGE) / 100.0;
            double elemMore = profile.getMoreMultiplier(StatType.INCREASED_ELEMENTAL_DAMAGE);

            for (DamageChannel ch : DamageChannel.values()) {
                double val = context.getChannelDamage(ch);
                if (val <= 0.0 || ch == DamageChannel.TRUE) continue;

                double inc = globalInc + (ch.isElemental() ? elemInc : 0.0);
                double more = globalMore * (ch.isElemental() ? elemMore : 1.0);
                double mult = Math.max(0.0, 1.0 + inc) * Math.max(0.0, more);
                if (mult != 1.0) {
                    context.setChannelDamage(ch, val * mult);
                }
            }
        }
    }

    class PotionEffects implements DamagePhase {
        @Override
        public void process(DamageContext context) {
            if (context.getAttacker() != null
                    && (context.getType() == DamageType.PHYSICAL || context.getType() == DamageType.RANGED)) {
                PotionEffect strength = context.getAttacker().getPotionEffect(PotionEffectType.STRENGTH);
                if (strength != null) {
                    context.multiplyDamage(1.0 + 0.20 * (strength.getAmplifier() + 1));
                }
            }

            if (context.getType() == DamageType.TRUE_DAMAGE
                    || context.getType() == DamageType.ENVIRONMENTAL) {
                return;
            }
            PotionEffect resistance = context.getDefender().getPotionEffect(PotionEffectType.RESISTANCE);
            if (resistance != null) {
                double reduction = Math.min(0.80, 0.20 * (resistance.getAmplifier() + 1));
                context.multiplyDamage(1.0 - reduction);
            }
        }
    }

    /**
     * 4. 回避（PoE エントロピー式）＆ ブロック判定。
     */
    class Avoidance implements DamagePhase {
        private final StatManager statManager;
        private final DamageMath.EvasionEntropyTracker entropyTracker;

        public Avoidance(StatManager statManager, DamageMath.EvasionEntropyTracker entropyTracker) {
            this.statManager = statManager;
            this.entropyTracker = entropyTracker;
        }

        @Override
        public void process(DamageContext context) {
            if (context.getDefender() == null) return;
            if (context.getType() == DamageType.TRUE_DAMAGE || context.getType() == DamageType.ENVIRONMENTAL) return;

            double evasion = statManager.getTotalStat(context.getDefender(), StatType.EVASION);
            if (evasion > 0.0 && entropyTracker.rollEvade(context.getDefender().getUniqueId(), evasion)) {
                context.setEvaded(true);
                return;
            }

            double blockChance = Math.min(75.0, statManager.getTotalStat(context.getDefender(), StatType.BLOCK_CHANCE));
            if (blockChance > 0.0 && ThreadLocalRandom.current().nextDouble(100.0) < blockChance) {
                context.setBlocked(true);
                context.multiplyDamage(DamageMath.BLOCK_DAMAGE_MULTIPLIER);
            }
        }
    }

    class Critical implements DamagePhase {
        private final StatManager statManager;

        public Critical(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            if (context.getAttacker() == null || context.isEvaded()) return;
            if (context.getType() == DamageType.TRUE_DAMAGE || context.getType() == DamageType.ENVIRONMENTAL) return;

            double critChance = statManager.getTotalStat(context.getAttacker(), StatType.CRITICAL_CHANCE);
            if (critChance > 0 && ThreadLocalRandom.current().nextDouble(100.0) < critChance) {
                context.setCritical(true);
                double critDamageBonus = statManager.getTotalStat(context.getAttacker(), StatType.CRITICAL_DAMAGE);
                double finalMultiplier = 1.5 + (critDamageBonus / 100.0);
                context.multiplyDamage(finalMultiplier);
            }
        }
    }

    /**
     * 5. チャンネル別の防御・耐性・貫通軽減 (PoE 式アーマー + 耐性上限75% + 貫通)。
     */
    class Defense implements DamagePhase {
        private final StatManager statManager;

        public Defense(StatManager statManager) {
            this.statManager = statManager;
        }

        @Override
        public void process(DamageContext context) {
            if (context.isEvaded() || context.getDefender() == null) return;
            if (context.getType() == DamageType.TRUE_DAMAGE || context.getType() == DamageType.ENVIRONMENTAL) return;

            double armour = statManager.getTotalStat(context.getDefender(), StatType.DEFENSE);
            double physReduction = statManager.getTotalStat(context.getDefender(), StatType.PHYSICAL_DAMAGE_REDUCTION);
            double magicDef = statManager.getTotalStat(context.getDefender(), StatType.MAGIC_DEFENSE);
            double maxResBonus = statManager.getTotalStat(context.getDefender(), StatType.MAX_ELEMENTAL_RESISTANCE);

            double armorPen = context.getAttacker() != null
                    ? statManager.getTotalStat(context.getAttacker(), StatType.ARMOR_PENETRATION) : 0.0;
            double elemPen = context.getAttacker() != null
                    ? statManager.getTotalStat(context.getAttacker(), StatType.ELEMENTAL_PENETRATION) : 0.0;
            double abyssPen = context.getAttacker() != null
                    ? statManager.getTotalStat(context.getAttacker(), StatType.ABYSS_PENETRATION) : 0.0;

            // 物理チャンネル: PoE アーマー式 + 物理ダメージ追加軽減
            double rawPhys = context.getChannelDamage(DamageChannel.PHYSICAL);
            if (rawPhys > 0.0) {
                double mult = DamageMath.armourMitigationMultiplier(armour, rawPhys, armorPen);
                if (physReduction > 0.0) {
                    mult *= (1.0 - Math.min(physReduction, 0.90));
                }
                context.setChannelDamage(DamageChannel.PHYSICAL, rawPhys * mult);
            }

            // 属性チャンネル: 属性耐性 (上限 75% + maxResBonus) - 貫通 + 既存 MAGIC_DEFENSE の緩やかな互換軽減
            double legacyMagicMult = magicDef > 0.0 ? (250.0 / (250.0 + magicDef)) : 1.0;
            applyElementalMitigation(context, DamageChannel.FIRE, StatType.FIRE_RESISTANCE, maxResBonus, elemPen, legacyMagicMult);
            applyElementalMitigation(context, DamageChannel.ICE, StatType.ICE_RESISTANCE, maxResBonus, elemPen, legacyMagicMult);
            applyElementalMitigation(context, DamageChannel.LIGHTNING, StatType.LIGHTNING_RESISTANCE, maxResBonus, elemPen, legacyMagicMult);
            applyElementalMitigation(context, DamageChannel.HOLY, StatType.HOLY_RESISTANCE, maxResBonus, elemPen, legacyMagicMult);

            // 深淵チャンネル: 深淵耐性 (上限 75%) - 深淵貫通 (MAGIC_DEFENSE は無効!)
            double rawAbyss = context.getChannelDamage(DamageChannel.ABYSS);
            if (rawAbyss > 0.0) {
                double abyssRes = statManager.getTotalStat(context.getDefender(), StatType.ABYSS_RESISTANCE);
                double mult = DamageMath.abyssResistanceMultiplier(abyssRes, abyssPen);
                context.setChannelDamage(DamageChannel.ABYSS, rawAbyss * mult);
            }
        }

        private void applyElementalMitigation(
                DamageContext context,
                DamageChannel channel,
                StatType resStat,
                double maxResBonus,
                double elemPen,
                double legacyMagicMult
        ) {
            double raw = context.getChannelDamage(channel);
            if (raw <= 0.0) return;
            double rawRes = statManager.getTotalStat(context.getDefender(), resStat);
            double resMult = DamageMath.elementalResistanceMultiplier(rawRes, maxResBonus, elemPen);
            context.setChannelDamage(channel, raw * resMult * legacyMagicMult);
        }
    }

    /** 後方互換用 (属性フラット加算は Base フェーズへ統合済み) */
    class ElementModifier implements DamagePhase {
        public ElementModifier(StatManager statManager) {
        }

        @Override
        public void process(DamageContext context) {
            // Base / Conversion / Defense フェーズへ統合済み
        }
    }
}

package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class FrostAmulet implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public FrostAmulet() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 50.0);
        this.baseStats.put(StatType.MAX_MANA, 200.0);
    }

    @Override
    public String getId() {
        return "frost_amulet";
    }

    @Override
    public Material getMaterial() {
        return Material.HEART_OF_THE_SEA;
    }

    @Override
    public String getDisplayName() {
        return "§bフロストアミュレット";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public String getFlavorText() {
        return "極寒の魔力が込められた護符。\n10%の確率で攻撃をブロックし、自身の放つ氷属性魔法の威力を1.5倍にする。";
    }

    // --- アイテム固有のアビリティ（パッシブ効果） ---

    @Override
    public void onAttack(DamageContext context) {
        // 自分が氷属性の攻撃を行った場合、ダメージを1.5倍にする
        if (context.hasTag("ICE")) {
            context.multiplyDamage(1.5);
            
            // 強化発動時のエフェクト
            if (context.getAttacker() instanceof Player player) {
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }

    @Override
    public void onDefend(DamageContext context) {
        // 固定ダメージや環境ダメージはブロックできない
        if (context.getType() == com.ruskserver.deepwither_V2.modules.combat.damage.DamageType.TRUE_DAMAGE ||
            context.getType() == com.ruskserver.deepwither_V2.modules.combat.damage.DamageType.ENVIRONMENTAL) {
            return;
        }

        // 10%の確率で攻撃を完全にブロックする
        if (ThreadLocalRandom.current().nextDouble(100.0) < 10.0) {
            context.setDamage(0.0);
            
            // ブロック時のエフェクトとメッセージ
            if (context.getDefender() instanceof Player player) {
                player.sendMessage("§b[FrostAmulet] 攻撃をブロックしました！");
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}

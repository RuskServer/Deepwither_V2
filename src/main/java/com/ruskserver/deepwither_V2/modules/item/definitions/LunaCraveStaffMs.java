package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.api.WandItem;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class LunaCraveStaffMs implements WandItem {

    private final Map<StatType, Double> baseStats;

    public LunaCraveStaffMs() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 38.0);
        this.baseStats.put(StatType.ICE_DAMAGE, 15.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 6.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 130.0);
        this.baseStats.put(StatType.MAX_MANA, 45.0);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 2.0);
    }

    @Override
    public String getId() {
        return "luna_crave_staff_ms";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§b§l月晶戦術導杖「ルナ・クレイヴ-MS」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public String getFlavorText() {
        return "Lunaris Atelierが戦術魔導部隊向けに再設計した“ルナ・クレイヴ”の軍用モデル。軍規格の“月光結晶強化筐体”を採用し、衝撃・過負荷・長時間運用への耐性が大幅に向上。民生モデルと比べて出力特性が鋭く、制圧用範囲魔法の初動展開を強力に補助する。";
    }

    @Override
    public int getCustomModelData() {
        return 6;
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.END_ROD;
    }

    @Override
    public double getManaCost() {
        return 24.0;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("ICE");
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}

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
public class LunaCraveStaff implements WandItem {

    private final Map<StatType, Double> baseStats;

    public LunaCraveStaff() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 30.0);
        this.baseStats.put(StatType.ICE_DAMAGE, 10.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 130.0);
        this.baseStats.put(StatType.MAX_MANA, 35.0);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 1.0);
    }

    @Override
    public String getId() {
        return "luna_crave_staff";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§b§l月晶導杖「ルナ・クレイヴ」";
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
        return "Lunaris Atelierが中級術者向けに設計した拡散術式補助杖。杖頭には“月光結晶”が装着されているが、これはエーテル結晶を独自の月環加工によって波動拡散特性を増幅した特製品であり、範囲系魔法の広がりと安定性に優れる。";
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
        return 18.0;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("ICE");
    }

    @Override
    public double getSellPrice() {
        return 4500.0;
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}

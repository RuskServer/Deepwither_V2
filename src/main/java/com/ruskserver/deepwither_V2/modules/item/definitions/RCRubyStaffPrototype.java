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
public class RCRubyStaffPrototype implements WandItem {

    private final Map<StatType, Double> baseStats;

    public RCRubyStaffPrototype() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 50.0);
        this.baseStats.put(StatType.FIRE_DAMAGE, 12.0);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 6.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 130.0);
    }

    @Override
    public String getId() {
        return "rc_ruby_staff_prototype";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§c§l紅晶制式魔導杖《ルビア・アーキタイプ》";
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
        return "Redline Constructが開発した制式採用型魔導杖。内部骨格にルビー系魔導結晶を用い、生体導路によって魔力の流量と分散を同時に制御する。血を代償とする機構は完全に排除されており、一部の魔法系派閥では衛兵部隊用装備として正式採用されている。";
    }

    @Override
    public int getCustomModelData() {
        return 10;
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.CRIMSON_SPORE;
    }

    @Override
    public double getManaCost() {
        return 30.0;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("FIRE");
    }

    @Override
    public double getSellPrice() {
        return 9200.0;
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}

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
public class RequiemBurstStaff implements WandItem {

    private final Map<StatType, Double> baseStats;

    public RequiemBurstStaff() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 35.0);
        this.baseStats.put(StatType.FIRE_DAMAGE, 8.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 7.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 180.0);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 3.0);
    }

    @Override
    public String getId() {
        return "requiem_burst_staff";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§5§l屍語りの骨杖・改《レクイエム・バースト》";
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
        return "灰機連盟が“屍語りの骨杖”を回収し、旧文明技術のエーテル増幅膜とナノ刻印を移植した改修モデル。儀式骨管内部の黒灰霧エーテルは安定化処理され、代わりに瞬間的な魔力圧縮と放出を可能にしている。";
    }

    @Override
    public int getCustomModelData() {
        return 4;
    }

    @Override
    public Particle getProjectileParticle() {
        return Particle.SOUL_FIRE_FLAME;
    }

    @Override
    public double getManaCost() {
        return 28.0;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("FIRE");
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}

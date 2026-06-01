package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.api.WandItem;
import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CorpseTalkBoneStaff implements WandItem {

    private final Map<StatType, Double> baseStats;

    public CorpseTalkBoneStaff() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 35.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 150.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 2.0);
    }

    @Override
    public String getId() {
        return "corpse_talk_bone_staff";
    }

    @Override
    public Material getMaterial() {
        return Material.STICK;
    }

    @Override
    public String getDisplayName() {
        return "§c§l屍語りの骨杖";
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
        return "「屍語りの骨杖」は、グール・シャーマンが死肉の塊と呪詛骨を編み合わせて作る儀式杖。杖の中心部には“死者の声を記録する”とされる中空の骨管が使われ、その内部には黒灰色の霧状エーテルが常に渦巻いている。";
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
        return 18.0;
    }

    @Override
    public double getSellPrice() {
        return 1200.0;
    }

    @Override
    public String getWeaponType() {
        return "杖";
    }
}

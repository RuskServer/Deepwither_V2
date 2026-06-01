package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class KobaltReverbMace implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public KobaltReverbMace() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 60.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.1);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 10.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 130.0);
    }

    @Override
    public String getId() {
        return "kobalt_reverb_mace";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§9§lLD-RW17「コバルト・リバーブメイス」";
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
        return "Lazward Dynamicsの試作振動兵器群の中でも、最も高い汎用性を持つと評価された軽量戦闘メイス。強化プラスチック外殻で驚くほど軽量だが、内部の“コバルト・レゾナンスコア”が高出力振動を発生させる。";
    }

    @Override
    public int getCustomModelData() {
        return 23;
    }

    @Override
    public double getSellPrice() {
        return 5200.0;
    }

    @Override
    public String getWeaponType() {
        return "メイス";
    }
}

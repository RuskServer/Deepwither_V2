package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RiftValtarLance implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RiftValtarLance() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 104.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 6.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 100.0);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 10.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.5);
        this.baseStats.put(StatType.MAX_MANA, 10.0);
    }

    @Override
    public String getId() {
        return "rift_valtar_lance";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§e§l裂界神槍「シャード・オブ・ヴァルター」";
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
        return "裂界神殿機構が旧世界の神の“断片（Shard）”を兵装化した大型神槍。刀身には位相ズレ金属が用いられ、常に黄緑色の偏光を放ちながら稲光が走る。";
    }

    @Override
    public int getCustomModelData() {
        return 6;
    }

    @Override
    public String getWeaponType() {
        return "槍";
    }
}

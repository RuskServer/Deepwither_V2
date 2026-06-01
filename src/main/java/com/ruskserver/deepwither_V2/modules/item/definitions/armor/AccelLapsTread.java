package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AccelLapsTread implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AccelLapsTread() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 12.0);
        this.baseStats.put(StatType.SPEED, 0.005);
    }

    @Override
    public String getId() {
        return "accel_laps_tread";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§d§l戦術機動ブーツ「アクセル・ラプス＝トレッド」";
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
    public double getSellPrice() {
        return 2800.0;
    }

    @Override
    public String getFlavorText() {
        return "ノア＝セクターの特殊工作部隊向けに限定生産された、高機動戦闘用フットウェア。足裏のエネルギー噴出機構により、空中でのわずかな軌道修正すら可能にする。";
    }
}

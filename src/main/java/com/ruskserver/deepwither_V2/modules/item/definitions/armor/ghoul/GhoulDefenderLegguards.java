package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;

@Component
public class GhoulDefenderLegguards extends AbstractGhoulArmor {
    public GhoulDefenderLegguards() {
        super("ghoul_defender_legguards", "§8§lグール・ディフェンダーのレッグガード", Material.IRON_LEGGINGS,
                "硬化したグールの皮膚を鋼板の間へ封じた脚鎧。衝撃を受けるたび内側で肉が蠢く。",
                "ghoul_defender", "netherite", 400.0,
                Map.of(StatType.DEFENSE, 23.0, StatType.HEALTH, 30.0, StatType.MAGIC_DEFENSE, 8.0));
    }
}

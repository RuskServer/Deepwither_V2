package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;

@Component
public class GhoulDefenderMask extends AbstractGhoulArmor {
    public GhoulDefenderMask() {
        super("ghoul_defender_mask", "§8§lグール・ディフェンダーのマスク", Material.IRON_HELMET,
                "グールの頭骨と内臓膜を重ねた仮面。痛みを遠い出来事のように感じさせる。",
                "ghoul_defender", "netherite", 240.0,
                Map.of(StatType.DEFENSE, 14.0, StatType.HEALTH, 15.0, StatType.MAGIC_DEFENSE, 4.0));
    }
}

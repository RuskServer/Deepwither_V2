package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;

@Component
public class GhoulDefenderSabatons extends AbstractGhoulArmor {
    public GhoulDefenderSabatons() {
        super("ghoul_defender_sabatons", "§8§lグール・ディフェンダーのサバトン", Material.IRON_BOOTS,
                "死体の重みを受け止めるために作られた重靴。踏みしめた地面へ腐気が染み出す。",
                "ghoul_defender", "netherite", 280.0,
                Map.of(StatType.DEFENSE, 17.0, StatType.HEALTH, 15.0, StatType.MAGIC_DEFENSE, 5.0));
    }
}

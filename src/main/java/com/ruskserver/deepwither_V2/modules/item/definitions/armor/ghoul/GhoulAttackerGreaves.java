package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;

@Component
public class GhoulAttackerGreaves extends AbstractGhoulArmor {
    public GhoulAttackerGreaves() {
        super("ghoul_attacker_greaves", "§2§lグール・アタッカーのグリーヴ", Material.LEATHER_BOOTS,
                "獲物へ飛びかかるグールの腱を移植した脚甲。足元から絶えず飢餓が這い上がる。",
                "ghoul_attacker", "copper", 220.0,
                Map.of(StatType.ATTACK_DAMAGE, 5.0, StatType.ATTACK_SPEED, 0.08,
                        StatType.SPEED, 5.0, StatType.DEFENSE, 7.0));
    }
}

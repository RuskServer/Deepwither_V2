package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;

@Component
public class GhoulAttackerHarness extends AbstractGhoulArmor {
    public GhoulAttackerHarness() {
        super("ghoul_attacker_harness", "§2§lグール・アタッカーのハーネス", Material.LEATHER_CHESTPLATE,
                "乾燥させたグールの筋繊維で補強された軽装。腐臭に反して身体を鋭く動かせる。",
                "ghoul_attacker", "copper", 360.0,
                Map.of(StatType.ATTACK_DAMAGE, 12.0, StatType.ATTACK_SPEED, 0.07, StatType.DEFENSE, 16.0));
    }
}

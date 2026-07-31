package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;

@Component
public class GhoulAttackerHood extends AbstractGhoulArmor {
    public GhoulAttackerHood() {
        super("ghoul_attacker_hood", "§2§lグール・アタッカーのフード", Material.LEATHER_HELMET,
                "グールの残滓を染み込ませた頭巾。飢えた感覚が獲物の急所を浮かび上がらせる。",
                "ghoul_attacker", "copper", 180.0,
                Map.of(StatType.ATTACK_DAMAGE, 5.0, StatType.CRITICAL_CHANCE, 2.0, StatType.DEFENSE, 7.0));
    }
}

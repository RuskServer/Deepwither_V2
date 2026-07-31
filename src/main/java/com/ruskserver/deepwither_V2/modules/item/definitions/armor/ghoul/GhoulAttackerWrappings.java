package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;

@Component
public class GhoulAttackerWrappings extends AbstractGhoulArmor {
    public GhoulAttackerWrappings() {
        super("ghoul_attacker_wrappings", "§2§lグール・アタッカーのラップ", Material.LEATHER_LEGGINGS,
                "残滓を練り込んだ包帯を幾重にも巻いた脚衣。血の気配を追うほど締め付けが弱まる。",
                "ghoul_attacker", "copper", 300.0,
                Map.of(StatType.ATTACK_DAMAGE, 8.0, StatType.CRITICAL_CHANCE, 3.0, StatType.DEFENSE, 12.0));
    }
}

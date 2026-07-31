package com.ruskserver.deepwither_V2.modules.item.definitions.armor.ghoul;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

import java.util.Map;

@Component
public class GhoulDefenderCarapace extends AbstractGhoulArmor {
    public GhoulDefenderCarapace() {
        super("ghoul_defender_carapace", "§8§lグール・ディフェンダーのカラペイス", Material.IRON_CHESTPLATE,
                "腐敗と再生を繰り返す肉膜を鉄殻へ定着させた鎧。傷口を塞ぐように装着者を包み込む。",
                "ghoul_defender", "netherite", 480.0,
                Map.of(StatType.DEFENSE, 30.0, StatType.HEALTH, 40.0, StatType.MAGIC_DEFENSE, 10.0));
    }
}

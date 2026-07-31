package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.crafting.api.SimpleCraftingRecipe;

import java.time.Duration;
import java.util.Map;

@Component
public class CrystalSwordMk2Recipe extends SimpleCraftingRecipe {

    public CrystalSwordMk2Recipe() {
        super(
                "crystal_sword_mk2",
                "crystal_sword_mk2",
                1,
                "合成屋",
                6,
                Duration.ofMinutes(2),
                100,
                Map.of(
                        "blue_crystal_sword", 1,
                        "auric_ingot", 12,
                        "cut_aether_diamond", 4
                )
        );
    }
}

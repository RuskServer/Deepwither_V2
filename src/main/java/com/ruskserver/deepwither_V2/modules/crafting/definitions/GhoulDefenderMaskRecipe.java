package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import java.util.Map;

@Component
public class GhoulDefenderMaskRecipe extends AbstractGhoulSetRecipe {
    public GhoulDefenderMaskRecipe() {
        super("ghoul_defender_mask", 3, 60, 40,
                Map.of("ghoul_viscera", 10, "ghoul_remnant", 6, "auric_ingot", 3));
    }
}

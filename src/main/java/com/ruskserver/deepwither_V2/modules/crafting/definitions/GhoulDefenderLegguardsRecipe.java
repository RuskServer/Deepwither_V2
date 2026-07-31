package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import java.util.Map;

@Component
public class GhoulDefenderLegguardsRecipe extends AbstractGhoulSetRecipe {
    public GhoulDefenderLegguardsRecipe() {
        super("ghoul_defender_legguards", 5, 100, 65,
                Map.of("ghoul_viscera", 16, "ghoul_remnant", 10, "ghoul_essence", 2, "auric_ingot", 6));
    }
}

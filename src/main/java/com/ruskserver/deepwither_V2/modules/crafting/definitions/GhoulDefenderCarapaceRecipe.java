package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import java.util.Map;

@Component
public class GhoulDefenderCarapaceRecipe extends AbstractGhoulSetRecipe {
    public GhoulDefenderCarapaceRecipe() {
        super("ghoul_defender_carapace", 6, 120, 80,
                Map.of("ghoul_viscera", 20, "ghoul_remnant", 12, "ghoul_essence", 3, "auric_ingot", 7));
    }
}

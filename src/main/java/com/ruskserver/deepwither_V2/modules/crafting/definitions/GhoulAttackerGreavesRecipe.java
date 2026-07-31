package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import java.util.Map;

@Component
public class GhoulAttackerGreavesRecipe extends AbstractGhoulSetRecipe {
    public GhoulAttackerGreavesRecipe() {
        super("ghoul_attacker_greaves", 3, 60, 40,
                Map.of("ghoul_viscera", 6, "ghoul_remnant", 8, "auric_ingot", 3));
    }
}

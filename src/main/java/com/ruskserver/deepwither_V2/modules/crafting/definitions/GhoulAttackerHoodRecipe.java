package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import java.util.Map;

@Component
public class GhoulAttackerHoodRecipe extends AbstractGhoulSetRecipe {
    public GhoulAttackerHoodRecipe() {
        super("ghoul_attacker_hood", 2, 45, 30,
                Map.of("ghoul_viscera", 6, "ghoul_remnant", 8, "auric_ingot", 2));
    }
}

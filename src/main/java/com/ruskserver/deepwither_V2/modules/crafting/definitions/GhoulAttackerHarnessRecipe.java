package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import java.util.Map;

@Component
public class GhoulAttackerHarnessRecipe extends AbstractGhoulSetRecipe {
    public GhoulAttackerHarnessRecipe() {
        super("ghoul_attacker_harness", 5, 90, 65,
                Map.of("ghoul_viscera", 12, "ghoul_remnant", 16, "ghoul_essence", 2, "auric_ingot", 5));
    }
}

package com.ruskserver.deepwither_V2.modules.crafting.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import java.util.Map;

@Component
public class GhoulAttackerWrappingsRecipe extends AbstractGhoulSetRecipe {
    public GhoulAttackerWrappingsRecipe() {
        super("ghoul_attacker_wrappings", 4, 75, 50,
                Map.of("ghoul_viscera", 10, "ghoul_remnant", 12, "ghoul_essence", 1, "auric_ingot", 4));
    }
}

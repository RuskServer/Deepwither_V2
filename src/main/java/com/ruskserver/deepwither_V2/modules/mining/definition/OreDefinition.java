package com.ruskserver.deepwither_V2.modules.mining.definition;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.time.Duration;
import java.util.List;

public record OreDefinition(
        Material material,
        int durability,
        Duration respawnTime,
        int professionExperience,
        int barSegments,
        NamedTextColor filledColor,
        List<OreDrop> drops) {

    public OreDefinition {
        if (material == null || !material.isBlock()) {
            throw new IllegalArgumentException("ore material must be a block");
        }
        durability = Math.max(1, durability);
        respawnTime = respawnTime == null || respawnTime.isNegative() ? Duration.ZERO : respawnTime;
        professionExperience = Math.max(0, professionExperience);
        barSegments = Math.max(1, barSegments);
        filledColor = filledColor == null ? NamedTextColor.WHITE : filledColor;
        drops = drops == null ? List.of() : List.copyOf(drops);
    }
}

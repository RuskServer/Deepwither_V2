package com.ruskserver.deepwither_V2.modules.mining.definition;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Service
public class OreRegistry {

    private final Map<Material, OreDefinition> definitions = new EnumMap<>(Material.class);

    public OreRegistry() {
        register(new OreDefinition(
                Material.GOLD_ORE,
                4,
                Duration.ofMinutes(5),
                10,
                4,
                NamedTextColor.GOLD,
                java.util.List.of(
                        OreDrop.custom("raw_gold_chunk", 1),
                        OreDrop.custom("raw_gold_chunk", 1, 1, 0.10D)
                )
        ));
        register(new OreDefinition(
                Material.DIAMOND_ORE,
                6,
                Duration.ofMinutes(8),
                20,
                6,
                NamedTextColor.AQUA,
                java.util.List.of(
                        OreDrop.custom("rough_diamond", 1),
                        OreDrop.custom("rough_diamond", 1, 1, 0.05D)
                )
        ));
    }

    public OreDefinition get(Material material) {
        return definitions.get(material);
    }

    public boolean contains(Material material) {
        return definitions.containsKey(material);
    }

    private void register(OreDefinition definition) {
        definitions.put(definition.material(), definition);
    }
}

package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinitionRegistry;
import com.ruskserver.deepwither_V2.modules.dungeon.modifier.DungeonModifier;
import com.ruskserver.deepwither_V2.modules.dungeon.modifier.DungeonModifierContext;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class DungeonMapItem implements CustomItem {

    private final DungeonDefinitionRegistry definitionRegistry;
    private final NamespacedKey dungeonIdKey;
    private final NamespacedKey portalXKey;
    private final NamespacedKey portalZKey;
    private final NamespacedKey dungeonModifiersKey;

    @Inject
    public DungeonMapItem(DungeonDefinitionRegistry definitionRegistry, JavaPlugin plugin) {
        this.definitionRegistry = definitionRegistry;
        this.dungeonIdKey = new NamespacedKey(plugin, "dungeon_map_dungeon_id");
        this.portalXKey = new NamespacedKey(plugin, "dungeon_map_portal_x");
        this.portalZKey = new NamespacedKey(plugin, "dungeon_map_portal_z");
        this.dungeonModifiersKey = new NamespacedKey(plugin, "dungeon_map_modifiers");
    }

    @Override
    public String getId() {
        return "dungeon_map";
    }

    @Override
    public Material getMaterial() {
        return Material.FILLED_MAP;
    }

    @Override
    public String getDisplayName() {
        return "§bダンジョン地図";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public String getFlavorText() {
        return "古代の遺跡へと続く道が記された不思議な地図。";
    }

    @Override
    public double getSellPrice() {
        return 0;
    }

    @Override
    public int getCustomModelData() {
        return 9001;
    }

    @Override
    public void appendCustomLore(ItemStack item, ItemMeta meta, List<net.kyori.adventure.text.Component> lore) {
        if (item == null || meta == null) return;

        var pdc = meta.getPersistentDataContainer();
        String dungeonId = pdc.get(dungeonIdKey, PersistentDataType.STRING);
        Double portalX = pdc.get(portalXKey, PersistentDataType.DOUBLE);
        Double portalZ = pdc.get(portalZKey, PersistentDataType.DOUBLE);
        String modString = pdc.get(dungeonModifiersKey, PersistentDataType.STRING);

        if (dungeonId != null) {
            String dungeonDisplay = getDungeonDisplayName(dungeonId);
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(net.kyori.adventure.text.Component.text("§7ダンジョン: §f" + dungeonDisplay)
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (portalX != null && portalZ != null) {
            lore.add(net.kyori.adventure.text.Component.text("§7座標: §eX=" + portalX.intValue() + " §eZ=" + portalZ.intValue())
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (modString != null && !modString.isBlank()) {
            DungeonModifierContext modCtx = DungeonModifierContext.fromIdString(modString);
            for (DungeonModifier mod : modCtx.modifiers()) {
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(net.kyori.adventure.text.Component.text("§7モディファイアー: ")
                        .append(net.kyori.adventure.text.Component.text(mod.displayName(), mod.color())
                                .decoration(TextDecoration.ITALIC, false)));
                lore.add(net.kyori.adventure.text.Component.text("  " + mod.description(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
    }

    private String getDungeonDisplayName(String dungeonId) {
        DungeonDefinition def = definitionRegistry.get(dungeonId);
        return def != null ? def.displayName() : dungeonId;
    }
}

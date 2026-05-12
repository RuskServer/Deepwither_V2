package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.player.gui.MainMenuGui;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MenuCompass implements CustomItem {

    private final MainMenuGui mainMenuGui;

    private final Map<StatType, Double> baseStats;

    @Inject
    public MenuCompass(MainMenuGui mainMenuGui) {
        this.mainMenuGui = mainMenuGui;
        this.baseStats = new EnumMap<>(StatType.class);
    }

    @Override
    public String getId() {
        return "menu_compass";
    }

    @Override
    public Material getMaterial() {
        return Material.COMPASS;
    }

    @Override
    public String getDisplayName() {
        return "§aメニューコンパス";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "右クリックでメインメニューを開きます。スキルツリー、ステータス、ステ振りなどが確認できます。";
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick()) {
            mainMenuGui.open(event.getPlayer());
        }
    }
}
package com.ruskserver.deepwither_V2.modules.item.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

@Component
public class ItemGlowListener implements Listener {

    private static final String TEAM_PREFIX = "dw_glow_";

    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;

    @Inject
    public ItemGlowListener(ItemManager itemManager, ItemPDCUtil pdcUtil) {
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        ItemStack itemStack = itemEntity.getItemStack();

        if (!itemStack.hasItemMeta()) return;
        ItemMeta meta = itemStack.getItemMeta();

        String itemId = pdcUtil.getItemId(itemStack);
        if (itemId == null) return;

        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (customItem == null) return;

        ItemRarity rarity = customItem.getRarity();
        if (!(rarity.getColor() instanceof NamedTextColor color)) return;

        itemEntity.setGlowing(true);
        String entryName = itemEntity.getUniqueId().toString();
        String teamName = TEAM_PREFIX + color.toString();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = player.getScoreboard();
            Team team = sb.getTeam(teamName);
            if (team == null) {
                team = sb.registerNewTeam(teamName);
                team.color(color);
            }
            if (!team.hasEntry(entryName)) {
                team.addEntry(entryName);
            }
        }

        Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team mainTeam = mainSb.getTeam(teamName);
        if (mainTeam == null) {
            mainTeam = mainSb.registerNewTeam(teamName);
            mainTeam.color(color);
        }
        if (!mainTeam.hasEntry(entryName)) {
            mainTeam.addEntry(entryName);
        }
    }
}

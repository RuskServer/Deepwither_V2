package com.ruskserver.deepwither_V2.modules.dungeon.portal;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dungeon.instance.DungeonInstance;
import com.ruskserver.deepwither_V2.modules.dungeon.instance.DungeonInstanceManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public class DungeonPortalListener implements Listener {

    private static final double ENTRY_RANGE = 2.0;

    private final JavaPlugin plugin;
    private final DungeonPortalManager portalManager;
    private final DungeonInstanceManager instanceManager;
    private final PortalLocationRepository portalRepo;

    @Inject
    public DungeonPortalListener(JavaPlugin plugin, DungeonPortalManager portalManager,
                                 DungeonInstanceManager instanceManager, PortalLocationRepository portalRepo) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.instanceManager = instanceManager;
        this.portalRepo = portalRepo;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (instanceManager.getPlayerInstance(player.getUniqueId()) != null) return;

        for (PortalLocation portal : portalRepo.getAll()) {
            World world = player.getServer().getWorld(portal.world());
            if (world == null || !player.getWorld().equals(world)) continue;

            double dx = player.getLocation().getX() - portal.x();
            double dz = player.getLocation().getZ() - portal.z();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > ENTRY_RANGE) continue;

            if (!hasValidMap(player, portal)) continue;

            enterDungeon(player, portal);
            return;
        }
    }

    private boolean hasValidMap(Player player, PortalLocation portal) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String dungeonId = portalManager.readDungeonId(item);
            if (dungeonId != null && dungeonId.equals(portal.dungeonId())) {
                Location loc = portalManager.readPortalLocation(item, player.getWorld());
                if (loc != null && (int) loc.getX() == (int) portal.x() && (int) loc.getZ() == (int) portal.z()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void enterDungeon(Player player, PortalLocation portal) {
        var creator = new org.bukkit.WorldCreator(
                "dungeon_" + portal.dungeonId() + "_" + System.currentTimeMillis())
                .environment(org.bukkit.World.Environment.NORMAL)
                .generator(new com.ruskserver.deepwither_V2.modules.dungeon.generator.VoidChunkGenerator());

        DungeonInstance instance = instanceManager.createInstance(portal.dungeonId(), creator);
        if (instance == null) {
            player.sendMessage(net.kyori.adventure.text.Component.text("ダンジョンの生成に失敗しました。", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        instanceManager.joinDungeon(player.getUniqueId(), instance.getInstanceId());
    }
}

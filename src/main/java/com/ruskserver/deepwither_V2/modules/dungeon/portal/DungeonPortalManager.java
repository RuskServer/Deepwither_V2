package com.ruskserver.deepwither_V2.modules.dungeon.portal;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.party.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class DungeonPortalManager implements Startable {

    private static final double PORTAL_ACTIVATION_RANGE = 6.0;
    private static final double PORTAL_PARTICLE_RANGE = 20.0;
    private static final long PARTICLE_TICK_INTERVAL = 10L;

    private final JavaPlugin plugin;
    private final Logger log;
    private final PortalLocationRepository portalRepo;
    private final ItemManager itemManager;
    private final PartyManager partyManager;
    private final NamespacedKey dungeonIdKey;
    private final NamespacedKey portalXKey;
    private final NamespacedKey portalZKey;

    private final Map<String, String> portalToInstance = new HashMap<>();

    @Inject
    public DungeonPortalManager(JavaPlugin plugin, PortalLocationRepository portalRepo,
                                ItemManager itemManager, PartyManager partyManager) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.portalRepo = portalRepo;
        this.itemManager = itemManager;
        this.partyManager = partyManager;
        this.dungeonIdKey = new NamespacedKey(plugin, "dungeon_map_dungeon_id");
        this.portalXKey = new NamespacedKey(plugin, "dungeon_map_portal_x");
        this.portalZKey = new NamespacedKey(plugin, "dungeon_map_portal_z");
    }

    @Override
    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 40L, PARTICLE_TICK_INTERVAL);
        log.info("[DungeonPortalManager] ポータルパーティクルタスクを開始しました。");
    }

    public ItemStack createMapItem(String dungeonId, PortalLocation portal) {
        ItemStack item = itemManager.generate("dungeon_map");
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        var pdc = meta.getPersistentDataContainer();
        pdc.set(dungeonIdKey, PersistentDataType.STRING, dungeonId);
        pdc.set(portalXKey, PersistentDataType.DOUBLE, portal.x());
        pdc.set(portalZKey, PersistentDataType.DOUBLE, portal.z());

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        String dungeonDisplay = getDungeonDisplayName(dungeonId);
        lore.add(Component.empty());
        lore.add(Component.text("§7ダンジョン: §f" + dungeonDisplay).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lore.add(Component.text("§7座標: §eX=" + (int) portal.x() + " §eZ=" + (int) portal.z())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public String readDungeonId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(dungeonIdKey, PersistentDataType.STRING);
    }

    public String getActiveInstanceForPortal(String portalId) {
        return portalToInstance.get(portalId);
    }

    public void registerPortalInstance(String portalId, String instanceId) {
        portalToInstance.put(portalId, instanceId);
    }

    public void unregisterPortalInstance(String portalId) {
        portalToInstance.remove(portalId);
    }

    public Location readPortalLocation(ItemStack item, World defaultWorld) {
        if (item == null || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        Double x = pdc.get(portalXKey, PersistentDataType.DOUBLE);
        Double z = pdc.get(portalZKey, PersistentDataType.DOUBLE);
        if (x == null || z == null) return null;
        return new Location(defaultWorld, x, 0, z);
    }

    private void tick() {
        var portals = portalRepo.getAll();
        if (portals.isEmpty()) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (PortalLocation portal : portals) {
                World world = player.getServer().getWorld(portal.world());
                if (world == null || !player.getWorld().equals(world)) continue;

                double dx = player.getLocation().getX() - portal.x();
                double dz = player.getLocation().getZ() - portal.z();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > PORTAL_PARTICLE_RANGE) continue;

                if (!canSeePortal(player, portal)) continue;

                if (dist <= PORTAL_ACTIVATION_RANGE) {
                    spawnVortex(player, portal);
                } else {
                    spawnHintParticle(player, portal);
                }
            }
        }
    }

    private boolean canSeePortal(Player player, PortalLocation portal) {
        if (hasMapForPortal(player, portal)) return true;
        var party = partyManager.getParty(player);
        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                Player member = plugin.getServer().getPlayer(memberId);
                if (member != null && member.isOnline() && hasMapForPortal(member, portal)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasMapForPortal(Player player, PortalLocation portal) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String dungeonId = readDungeonId(item);
            if (dungeonId != null && dungeonId.equals(portal.dungeonId())) {
                Location loc = readPortalLocation(item, player.getWorld());
                if (loc != null && (int) loc.getX() == (int) portal.x() && (int) loc.getZ() == (int) portal.z()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void spawnVortex(Player player, PortalLocation portal) {
        World world = player.getServer().getWorld(portal.world());
        if (world == null) return;
        Location center = new Location(world, portal.x(), portal.y(), portal.z());
        double time = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < 3; i++) {
            double angle = time * 2.0 + (i * Math.PI * 2 / 3);
            double radius = 1.2;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + 0.5 + i * 0.8;
            player.spawnParticle(Particle.END_ROD, x, y, z, 0, 0, 0, 0, 0.02);
            player.spawnParticle(Particle.PORTAL, x, y, z, 1, 0, 0, 0, 0.01);
        }
        player.spawnParticle(Particle.END_ROD, center.getX(), center.getY() + 0.5, center.getZ(), 0, 0, 0.5, 0, 0.05);
    }

    private void spawnHintParticle(Player player, PortalLocation portal) {
        World world = player.getServer().getWorld(portal.world());
        if (world == null) return;
        double time = System.currentTimeMillis() / 2000.0;
        double angle = time * Math.PI;
        double offsetX = Math.cos(angle) * 5.0;
        double offsetZ = Math.sin(angle) * 5.0;
        player.spawnParticle(Particle.END_ROD,
                portal.x() + offsetX, portal.y() + 1, portal.z() + offsetZ,
                0, 0, 0, 0, 0.01);
    }

    private String getDungeonDisplayName(String dungeonId) {
        return switch (dungeonId) {
            case "eternal_ice" -> "§b永久氷河";
            case "simple" -> "§a簡易遺跡";
            case "branch" -> "§6分岐迷宮";
            case "raid" -> "§c襲撃拠点";
            default -> dungeonId;
        };
    }
}

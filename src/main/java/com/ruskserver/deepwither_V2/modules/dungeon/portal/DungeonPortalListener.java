package com.ruskserver.deepwither_V2.modules.dungeon.portal;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.dungeon.instance.DungeonInstance;
import com.ruskserver.deepwither_V2.modules.dungeon.instance.DungeonInstanceManager;
import com.ruskserver.deepwither_V2.modules.dungeon.modifier.DungeonModifierContext;
import com.ruskserver.deepwither_V2.modules.party.Party;
import com.ruskserver.deepwither_V2.modules.party.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class DungeonPortalListener implements Listener {

    private static final double ENTRY_RANGE = 2.0;
    private static final long ENTRY_COOLDOWN_MS = 30_000L;

    private final JavaPlugin plugin;
    private final DungeonPortalManager portalManager;
    private final DungeonInstanceManager instanceManager;
    private final PortalLocationRepository portalRepo;
    private final PartyManager partyManager;

    private final Map<UUID, Long> entryCooldown = new HashMap<>();

    @Inject
    public DungeonPortalListener(JavaPlugin plugin, DungeonPortalManager portalManager,
                                 DungeonInstanceManager instanceManager, PortalLocationRepository portalRepo,
                                 PartyManager partyManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.instanceManager = instanceManager;
        this.portalRepo = portalRepo;
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        if (instanceManager.getPlayerInstance(player.getUniqueId()) != null) return;

        long now = System.currentTimeMillis();
        Long lastEntry = entryCooldown.get(player.getUniqueId());
        if (lastEntry != null && (now - lastEntry) < ENTRY_COOLDOWN_MS) return;

        for (PortalLocation portal : portalRepo.getAll()) {
            World world = player.getServer().getWorld(portal.world());
            if (world == null || !player.getWorld().equals(world)) continue;

            double dx = player.getLocation().getX() - portal.x();
            double dz = player.getLocation().getZ() - portal.z();
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > ENTRY_RANGE) continue;

            if (!canEnterPortal(player, portal)) continue;

            entryCooldown.put(player.getUniqueId(), now);
            enterDungeon(player, portal);
            return;
        }
    }

    private boolean canEnterPortal(Player player, PortalLocation portal) {
        if (hasValidMap(player, portal)) return true;

        String instanceId = portalManager.getActiveInstanceForPortal(portal.id());
        if (instanceId == null) return false;

        DungeonInstance inst = instanceManager.getInstance(instanceId);
        if (inst == null || !inst.isActive()) return false;

        Party party = partyManager.getParty(player);
        if (party == null) return false;

        for (UUID memberId : party.getMembers()) {
            if (inst.getParticipants().contains(memberId)) {
                return true;
            }
        }
        return false;
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
        String portalId = portal.id();

        String existingId = portalManager.getActiveInstanceForPortal(portalId);
        if (existingId != null && instanceManager.getInstance(existingId) != null) {
            if (instanceManager.joinDungeon(player.getUniqueId(), existingId)) {
                player.sendMessage(Component.text("§aパーティーのダンジョンに参加しました。"));
            } else {
                player.sendMessage(Component.text("§cダンジョンへの参加に失敗しました。", NamedTextColor.RED));
            }
            return;
        }

        String instanceId = createNewDungeon(player, portal);
        if (instanceId != null) {
            if (hasValidMap(player, portal)) consumeMap(player, portal);
            portalManager.registerPortalInstance(portalId, instanceId);
            notifyPartyMembers(player, portal, instanceId);
        }
    }

    private void consumeMap(Player player, PortalLocation portal) {
        var contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            String dungeonId = portalManager.readDungeonId(item);
            if (dungeonId == null || !dungeonId.equals(portal.dungeonId())) continue;
            Location loc = portalManager.readPortalLocation(item, player.getWorld());
            if (loc == null || (int) loc.getX() != (int) portal.x() || (int) loc.getZ() != (int) portal.z()) continue;
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) {
                player.getInventory().setItem(i, null);
            }
            return;
        }
    }

    private String createNewDungeon(Player player, PortalLocation portal) {
        WorldCreator creator = new WorldCreator(
                "dungeon_" + portal.dungeonId() + "_" + System.currentTimeMillis())
                .environment(World.Environment.NORMAL)
                .generator(new com.ruskserver.deepwither_V2.modules.dungeon.generator.VoidChunkGenerator());

        var modCtx = findModifiersOnPlayer(player, portal);
        DungeonInstance instance = instanceManager.createInstance(portal.dungeonId(), creator, modCtx);
        if (instance == null) {
            player.sendMessage(Component.text("ダンジョンの生成に失敗しました。", NamedTextColor.RED));
            return null;
        }

        if (!instanceManager.joinDungeon(player.getUniqueId(), instance.getInstanceId())) {
            player.sendMessage(Component.text("ダンジョンへの参加に失敗しました。", NamedTextColor.RED));
            return null;
        }

        if (modCtx.isPresent()) {
            var names = modCtx.modifiers().stream().map(m -> "§" + Integer.toHexString(m.color().value() & 0xF) + m.displayName()).toList();
            player.sendMessage(Component.text("§7[§6D§7] 適用モディファイアー: " + String.join(" §7+ ", names)));
        }

        return instance.getInstanceId();
    }

    private DungeonModifierContext findModifiersOnPlayer(Player player, PortalLocation portal) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String dungeonId = portalManager.readDungeonId(item);
            if (dungeonId == null || !dungeonId.equals(portal.dungeonId())) continue;
            Location loc = portalManager.readPortalLocation(item, player.getWorld());
            if (loc == null || (int) loc.getX() != (int) portal.x() || (int) loc.getZ() != (int) portal.z()) continue;
            var modCtx = portalManager.readModifiers(item);
            if (modCtx.isPresent()) return modCtx;
        }
        return DungeonModifierContext.none();
    }

    private void notifyPartyMembers(Player player, PortalLocation portal, String instanceId) {
        Party party = partyManager.getParty(player);
        if (party == null) return;

        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(player.getUniqueId())) continue;
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(Component.text("§b" + player.getName() + " §7がダンジョンに入りました。"));
                member.sendMessage(Component.text("§7ポータルに入ると合流できます。 §e/questportal tp " + portal.id()));
            }
        }
    }
}

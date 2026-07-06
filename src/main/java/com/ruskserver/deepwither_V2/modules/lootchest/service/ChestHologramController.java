package com.ruskserver.deepwither_V2.modules.lootchest.service;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootChestLocation;
import com.ruskserver.deepwither_V2.modules.lootchest.api.PlayerChestData;
import com.ruskserver.deepwither_V2.modules.lootchest.repository.PlayerChestRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChestHologramController implements Startable, Stoppable {

    private static final int HOLOGRAM_VIEW_RANGE = 48;
    private static final int TICK_INTERVAL = 20;

    private final JavaPlugin plugin;
    private final LootChestManager lootChestManager;
    private final PlayerChestRepository playerChestRepository;

    private final Map<UUID, ChestState> chestStates = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Integer>> hologramEntities = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, LocalDateTime>> cooldownCache = new ConcurrentHashMap<>();

    private BukkitRunnable tickTask;
    private int nextEntityId = 0x6F000000;

    @Inject
    public ChestHologramController(JavaPlugin plugin, LootChestManager lootChestManager, PlayerChestRepository playerChestRepository) {
        this.plugin = plugin;
        this.lootChestManager = lootChestManager;
        this.playerChestRepository = playerChestRepository;
    }

    @Override
    public void start() {
        for (LootChestLocation loc : lootChestManager.getActiveChests()) {
            chestStates.put(loc.getId(), new ChestState(loc.getId(), loc.getLocation(), loc.getLootTableId()));
        }
        startTickTask();
    }

    @Override
    public void stop() {
        if (tickTask != null) tickTask.cancel();
        for (Map<UUID, Integer> perChest : hologramEntities.values()) {
            for (int entityId : perChest.values()) {
                broadcastDestroy(entityId);
            }
        }
        hologramEntities.clear();
    }

    private void startTickTask() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        tickTask.runTaskTimer(plugin, TICK_INTERVAL, TICK_INTERVAL);
    }

    private void tick() {
        LocalDateTime now = LocalDateTime.now();

        for (ChestState chest : chestStates.values()) {
            if (!chest.location.isWorldLoaded()) continue;

            Location loc = chest.location;
            Map<UUID, Integer> playerEntities = hologramEntities.computeIfAbsent(chest.id, k -> new ConcurrentHashMap<>());

            Set<UUID> tracked = new HashSet<>(playerEntities.keySet());
            Set<UUID> nearby = new HashSet<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().equals(loc.getWorld())) continue;
                if (player.getLocation().distanceSquared(loc) > HOLOGRAM_VIEW_RANGE * HOLOGRAM_VIEW_RANGE) continue;

                nearby.add(player.getUniqueId());
                String text = getHologramText(player.getUniqueId(), chest.id, now);
                if (text == null) {
                    removePlayerHologram(player.getUniqueId(), chest.id, playerEntities);
                } else {
                    ensureHologram(player, chest, text, playerEntities);
                }
                tracked.remove(player.getUniqueId());
            }

            for (UUID gone : tracked) {
                removePlayerHologram(gone, chest.id, playerEntities);
            }
        }
    }

    public void notifyCooldownSet(UUID playerUuid, UUID chestId, LocalDateTime endTime) {
        cooldownCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).put(chestId, endTime);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        ChestState chest = chestStates.get(chestId);
        if (chest == null) return;

        Map<UUID, Integer> playerEntities = hologramEntities.computeIfAbsent(chestId, k -> new ConcurrentHashMap<>());
        ensureHologram(player, chest, formatTimeLeft(Duration.between(LocalDateTime.now(), endTime)), playerEntities);
    }

    private String getHologramText(UUID playerUuid, UUID chestId, LocalDateTime now) {
        Map<UUID, LocalDateTime> playerCache = cooldownCache.get(playerUuid);
        LocalDateTime endTime = playerCache != null ? playerCache.get(chestId) : null;

        if (endTime == null) {
            Optional<PlayerChestData> loaded = playerChestRepository.findByPlayerAndChest(playerUuid, chestId);
            if (loaded.isPresent()) {
                endTime = loaded.get().getNextOpenTime();
                cooldownCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).put(chestId, endTime);
            }
        }

        if (endTime == null) return null;

        if (now.isBefore(endTime)) {
            Duration remaining = Duration.between(now, endTime);
            return formatTimeLeft(remaining);
        }

        return null;
    }

    private static String formatTimeLeft(Duration d) {
        long total = d.getSeconds();
        if (total <= 0) return null;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0) return String.format("§c%d時間%02d分", h, m);
        if (m > 0) return String.format("§c%d分%02d秒", m, s);
        return String.format("§e%d秒", s);
    }

    private void ensureHologram(Player player, ChestState chest, String text, Map<UUID, Integer> playerEntities) {
        Integer existingId = playerEntities.get(player.getUniqueId());
        if (existingId != null) {
            updateHologram(player, existingId, text);
        } else {
            spawnHologram(player, chest, text, playerEntities);
        }
    }

    private void spawnHologram(Player player, ChestState chest, String text, Map<UUID, Integer> playerEntities) {
        int entityId = nextEntityId--;
        Location loc = chest.location.clone().add(0.5, 0.9, 0.5);

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.TEXT_DISPLAY,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                0f, 0f, 0f,
                0,
                Optional.empty()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);

        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(5, EntityDataTypes.BOOLEAN, true));
        metadata.add(new EntityData<>(14, EntityDataTypes.BYTE, (byte) 2));
        metadata.add(new EntityData<>(22, EntityDataTypes.ADV_COMPONENT, Component.text(text)));
        metadata.add(new EntityData<>(26, EntityDataTypes.BOOLEAN, true));

        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);

        playerEntities.put(player.getUniqueId(), entityId);
    }

    private void updateHologram(Player player, int entityId, String text) {
        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(22, EntityDataTypes.ADV_COMPONENT, Component.text(text)));

        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
    }

    private void removePlayerHologram(UUID playerUuid, UUID chestId, Map<UUID, Integer> playerEntities) {
        Integer entityId = playerEntities.remove(playerUuid);
        if (entityId == null) return;

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            broadcastDestroyTo(player, entityId);
        }
    }

    private void broadcastDestroy(int entityId) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }

    private void broadcastDestroyTo(Player player, int entityId) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private static class ChestState {
        final UUID id;
        final Location location;
        @SuppressWarnings("unused")
        final String lootTableId;

        ChestState(UUID id, Location location, String lootTableId) {
            this.id = id;
            this.location = location;
            this.lootTableId = lootTableId;
        }
    }
}

package com.ruskserver.deepwither_V2.modules.lootchest.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootChestLocation;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootItem;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootTableDefinition;
import com.ruskserver.deepwither_V2.modules.lootchest.repository.LootChestRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LootChestManager implements Startable, Stoppable {

    private static final NamespacedKey HOLOGRAM_KEY = new NamespacedKey("deepwither", "loot_chest_hologram");
    
    private final JavaPlugin plugin;
    private final LootChestRepository repository;
    private final LootRegistry registry;
    private final Map<UUID, LootChestLocation> activeChests = new HashMap<>();
    private final Map<UUID, ArmorStand> activeHolograms = new HashMap<>();
    private BukkitRunnable tickTask;

    @Inject
    public LootChestManager(JavaPlugin plugin, LootChestRepository repository, LootRegistry registry) {
        this.plugin = plugin;
        this.repository = repository;
        this.registry = registry;
    }

    @Override
    public void start() {
        // 全ワールドの古いホログラムを削除（ゾンビホログラム対策）
        cleanupAllHolograms();

        // データの読み込み
        List<LootChestLocation> loaded = repository.findAll();
        for (LootChestLocation loc : loaded) {
            activeChests.put(loc.getId(), loc);
            if (loc.isSpawned()) {
                spawnChestBlock(loc);
            }
        }

        // 管理タスクの開始
        startTickTask();
    }

    @Override
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (ArmorStand hologram : activeHolograms.values()) {
            hologram.remove();
        }
    }

    private void cleanupAllHolograms() {
        plugin.getServer().getWorlds().forEach(world -> {
            world.getEntitiesByClass(ArmorStand.class).forEach(as -> {
                if (as.getPersistentDataContainer().has(HOLOGRAM_KEY, PersistentDataType.BYTE)) {
                    as.remove();
                }
            });
        });
    }

    private void startTickTask() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                LocalDateTime now = LocalDateTime.now();
                for (LootChestLocation loc : activeChests.values()) {
                    if (!loc.isSpawned()) {
                        if (loc.getNextSpawnTime() != null && now.isAfter(loc.getNextSpawnTime())) {
                            spawnChest(loc);
                        } else {
                            updateHologram(loc, now);
                        }
                    } else {
                        checkChestEmptied(loc);
                    }
                }
            }
        };
        tickTask.runTaskTimer(plugin, 20L, 20L); // 1秒おきに実行
    }

    public void registerNewChest(Location location, String lootTableId) {
        UUID id = UUID.randomUUID();
        LootChestLocation loc = new LootChestLocation(id, location, lootTableId, LocalDateTime.now(), false);
        activeChests.put(id, loc);
        repository.save(loc);
        spawnChest(loc);
    }

    private void spawnChest(LootChestLocation loc) {
        loc.setSpawned(true);
        loc.setNextSpawnTime(null);
        removeHologram(loc.getId());
        spawnChestBlock(loc);
        repository.save(loc);
    }

    private void spawnChestBlock(LootChestLocation loc) {
        Block block = loc.getLocation().getBlock();
        block.setType(Material.CHEST);
        
        Chest chest = (Chest) block.getState();
        fillChest(chest, loc.getLootTableId());
    }

    private void fillChest(Chest chest, String lootTableId) {
        LootTableDefinition def = registry.getDefinition(lootTableId);
        if (def == null) return;

        Inventory inv = chest.getInventory();
        inv.clear();

        List<LootItem> items = def.getLootItems();
        if (items.isEmpty()) return;

        int totalWeight = items.stream().mapToInt(LootItem::weight).sum();
        Random random = new Random();

        for (int i = 0; i < def.getRolls(); i++) {
            int r = random.nextInt(totalWeight);
            int current = 0;
            for (LootItem item : items) {
                current += item.weight();
                if (r < current) {
                    int amount = ThreadLocalRandom.current().nextInt(item.minAmount(), item.maxAmount() + 1);
                    if (amount <= 0) break;
                    org.bukkit.inventory.ItemStack base = item.itemStack();
                    if (base == null) break;
                    org.bukkit.inventory.ItemStack is = base.clone();
                    is.setAmount(amount);

                    // ランダムな空きスロットに配置
                    List<Integer> emptySlots = new ArrayList<>();
                    for (int s = 0; s < inv.getSize(); s++) {
                        if (inv.getItem(s) == null || inv.getItem(s).getType() == Material.AIR) {
                            emptySlots.add(s);
                        }
                    }

                    if (!emptySlots.isEmpty()) {
                        int randomSlot = emptySlots.get(random.nextInt(emptySlots.size()));
                        inv.setItem(randomSlot, is);
                    }
                    break;
                }
            }
        }
    }

    private void checkChestEmptied(LootChestLocation loc) {
        Block block = loc.getLocation().getBlock();
        if (block.getType() != Material.CHEST) {
            // 破壊された場合もリスポーンタイマー開始
            startCooldown(loc);
            return;
        }

        Chest chest = (Chest) block.getState();
        if (isEmpty(chest.getInventory())) {
            block.setType(Material.AIR);
            startCooldown(loc);
        }
    }

    private boolean isEmpty(Inventory inv) {
        for (org.bukkit.inventory.ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) return false;
        }
        return true;
    }

    private void startCooldown(LootChestLocation loc) {
        loc.setSpawned(false);
        // 5分(300秒)から30分(1800秒)の間
        int seconds = ThreadLocalRandom.current().nextInt(300, 1801);
        loc.setNextSpawnTime(LocalDateTime.now().plusSeconds(seconds));
        repository.save(loc);
    }

    private void updateHologram(LootChestLocation loc, LocalDateTime now) {
        if (loc.getNextSpawnTime() == null) return;

        Duration duration = Duration.between(now, loc.getNextSpawnTime());
        if (duration.isNegative()) return;

        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        ArmorStand as = activeHolograms.get(loc.getId());
        if (as == null || as.isDead()) {
            if (as != null) activeHolograms.remove(loc.getId());
            as = createHologram(loc.getLocation());
            activeHolograms.put(loc.getId(), as);
        }
        as.customName(Component.text("§e§lリスポーンまで: §f" + timeStr));
    }

    private ArmorStand createHologram(Location loc) {
        Location spawnLoc = loc.clone().add(0.5, 0.5, 0.5);

        // 既存のホログラムが残っている場合は削除（チャンク再読み込み対策）
        for (Entity entity : spawnLoc.getWorld().getNearbyEntities(spawnLoc, 0.1, 0.1, 0.1)) {
            if (entity instanceof ArmorStand as
                    && as.getPersistentDataContainer().has(HOLOGRAM_KEY, PersistentDataType.BYTE)) {
                as.remove();
            }
        }

        ArmorStand as = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        as.setGravity(false);
        as.setCanPickupItems(false);
        as.setCustomNameVisible(true);
        as.setVisible(false);
        as.setSmall(true);
        as.getPersistentDataContainer().set(HOLOGRAM_KEY, PersistentDataType.BYTE, (byte) 1);
        return as;
    }

    private void removeHologram(UUID id) {
        ArmorStand as = activeHolograms.remove(id);
        if (as != null) {
            as.remove();
        }
    }
}

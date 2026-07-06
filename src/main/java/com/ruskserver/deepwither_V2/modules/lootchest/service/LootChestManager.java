package com.ruskserver.deepwither_V2.modules.lootchest.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootChestLocation;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootItem;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootTableDefinition;
import com.ruskserver.deepwither_V2.modules.lootchest.repository.LootChestRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LootChestManager implements Startable, Stoppable {

    private final JavaPlugin plugin;
    private final LootChestRepository repository;
    private final LootRegistry registry;
    private final Map<UUID, LootChestLocation> activeChests = new HashMap<>();

    @Inject
    public LootChestManager(JavaPlugin plugin, LootChestRepository repository, LootRegistry registry) {
        this.plugin = plugin;
        this.repository = repository;
        this.registry = registry;
    }

    @Override
    public void start() {
        List<LootChestLocation> loaded = repository.findAll();
        for (LootChestLocation loc : loaded) {
            activeChests.put(loc.getId(), loc);
            spawnChestBlock(loc);
        }
    }

    @Override
    public void stop() {
    }

    public Collection<LootChestLocation> getActiveChests() {
        return Collections.unmodifiableCollection(activeChests.values());
    }

    public LootChestLocation findByLocation(Location location) {
        for (LootChestLocation loc : activeChests.values()) {
            if (loc.getLocation().equals(location)) {
                return loc;
            }
        }
        return null;
    }

    public void registerNewChest(Location location, String lootTableId) {
        UUID id = UUID.randomUUID();
        LootChestLocation loc = new LootChestLocation(id, location, lootTableId, null, true);
        activeChests.put(id, loc);
        repository.save(loc);
        spawnChestBlock(loc);
    }

    public void placeOneShotChest(Location location, String lootTableId) {
        placeOneShotChest(location, lootTableId, 1.0);
    }

    public void placeOneShotChest(Location location, String lootTableId, double rollMultiplier) {
        Block block = location.getBlock();
        block.setType(Material.CHEST);

        Chest chest = (Chest) block.getState();
        fillChest(chest, lootTableId, rollMultiplier);
    }

    private void spawnChestBlock(LootChestLocation loc) {
        loc.getLocation().getBlock().setType(Material.CHEST);
    }

    private void fillChest(Chest chest, String lootTableId, double rollMultiplier) {
        LootTableDefinition def = registry.getDefinition(lootTableId);
        if (def == null) return;

        Inventory inv = chest.getInventory();
        inv.clear();

        List<LootItem> items = def.getLootItems();
        if (items.isEmpty()) return;

        int totalWeight = items.stream().mapToInt(LootItem::weight).sum();
        int rolls = (int) Math.ceil(def.getRolls() * Math.max(rollMultiplier, 0.0));
        Random random = new Random();

        for (int i = 0; i < rolls; i++) {
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
}

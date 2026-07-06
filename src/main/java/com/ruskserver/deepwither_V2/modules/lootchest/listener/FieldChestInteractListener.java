package com.ruskserver.deepwither_V2.modules.lootchest.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootChestLocation;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootItem;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootTableDefinition;
import com.ruskserver.deepwither_V2.modules.lootchest.api.PlayerChestData;
import com.ruskserver.deepwither_V2.modules.lootchest.repository.PlayerChestRepository;
import com.ruskserver.deepwither_V2.modules.lootchest.service.ChestHologramController;
import com.ruskserver.deepwither_V2.modules.lootchest.service.LootChestManager;
import com.ruskserver.deepwither_V2.modules.lootchest.service.LootRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class FieldChestInteractListener implements Listener {

    private static final String PERSONAL_CHEST_TITLE = "§8❖ §6ルートチェスト";
    private static final int PERSONAL_CHEST_SIZE = 27;

    private final LootChestManager lootChestManager;
    private final LootRegistry registry;
    private final PlayerChestRepository playerChestRepository;
    private final ChestHologramController hologramController;
    private final Map<UUID, UUID> openChests = new HashMap<>();

    @Inject
    public FieldChestInteractListener(LootChestManager lootChestManager, LootRegistry registry, PlayerChestRepository playerChestRepository, ChestHologramController hologramController) {
        this.lootChestManager = lootChestManager;
        this.registry = registry;
        this.playerChestRepository = playerChestRepository;
        this.hologramController = hologramController;
    }

    @EventHandler
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CHEST) return;

        LootChestLocation chestLoc = lootChestManager.findByLocation(event.getClickedBlock().getLocation());
        if (chestLoc == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        UUID chestId = chestLoc.getId();

        Optional<PlayerChestData> existing = playerChestRepository.findByPlayerAndChest(playerUuid, chestId);
        if (existing.isPresent()) {
            LocalDateTime nextOpen = existing.get().getNextOpenTime();
            if (LocalDateTime.now().isBefore(nextOpen)) {
                Duration remaining = Duration.between(LocalDateTime.now(), nextOpen);
                long mins = remaining.toMinutes();
                long secs = remaining.toSeconds() % 60;
                player.sendMessage("§cこのチェストはあと §e" + String.format("%02d:%02d", mins, secs) + " §cで開けられます。");
                return;
            }
        }

        Inventory inv = createPersonalInventory(chestLoc.getLootTableId());
        if (inv == null) return;

        openChests.put(playerUuid, chestId);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID playerUuid = player.getUniqueId();
        UUID chestId = openChests.remove(playerUuid);
        if (chestId == null) return;

        int seconds = ThreadLocalRandom.current().nextInt(300, 1801);
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(seconds);
        PlayerChestData data = new PlayerChestData(playerUuid, chestId, endTime);
        playerChestRepository.save(data);
        hologramController.notifyCooldownSet(playerUuid, chestId, endTime);
    }

    private Inventory createPersonalInventory(String lootTableId) {
        LootTableDefinition def = registry.getDefinition(lootTableId);
        if (def == null) return null;

        Inventory inv = Bukkit.createInventory(null, PERSONAL_CHEST_SIZE, Component.text(PERSONAL_CHEST_TITLE));
        List<LootItem> items = def.getLootItems();
        if (items.isEmpty()) return inv;

        int totalWeight = items.stream().mapToInt(LootItem::weight).sum();
        int rolls = def.getRolls();
        Random random = new Random();

        for (int i = 0; i < rolls; i++) {
            int r = random.nextInt(totalWeight);
            int current = 0;
            for (LootItem item : items) {
                current += item.weight();
                if (r < current) {
                    int amount = ThreadLocalRandom.current().nextInt(item.minAmount(), item.maxAmount() + 1);
                    if (amount <= 0) break;
                    ItemStack base = item.itemStack();
                    if (base == null) break;
                    ItemStack is = base.clone();
                    is.setAmount(amount);

                    List<Integer> emptySlots = new ArrayList<>();
                    for (int s = 0; s < inv.getSize(); s++) {
                        if (inv.getItem(s) == null || inv.getItem(s).getType() == Material.AIR) {
                            emptySlots.add(s);
                        }
                    }
                    if (!emptySlots.isEmpty()) {
                        inv.setItem(emptySlots.get(random.nextInt(emptySlots.size())), is);
                    }
                    break;
                }
            }
        }
        return inv;
    }
}

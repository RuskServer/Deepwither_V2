package com.ruskserver.deepwither_V2.modules.lootchest.commands;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.lootchest.service.LootRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Command(name = "lootchest")
public class CommandLootChest implements CommandExecutor, TabCompleter {

    private static final NamespacedKey LOOT_TABLE_KEY = new NamespacedKey("deepwither", "loot_table_id");
    private final LootRegistry registry;

    @Inject
    public CommandLootChest(LootRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            player.sendMessage("§c使用法: /lootchest give <loot_table_id>");
            return true;
        }

        String id = args[1];
        if (registry.getDefinition(id) == null) {
            player.sendMessage("§cエラー: ルートテーブル '" + id + "' は登録されていません。");
            return true;
        }

        ItemStack item = createSetupItem(id);
        player.getInventory().addItem(item);
        player.sendMessage("§aルートチェスト設置用アイテム (" + id + ") を付与しました。");
        return true;
    }

    private ItemStack createSetupItem(String lootTableId) {
        ItemStack item = new ItemStack(Material.CHEST_MINECART);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text("§b§lルートチェスト設置エッグ: §f" + lootTableId));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7ブロックを右クリックして設置します。"));
        lore.add(Component.text("§7ルートテーブル: §e" + lootTableId));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(LOOT_TABLE_KEY, PersistentDataType.STRING, lootTableId);
        item.setItemMeta(meta);
        
        return item;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return registry.getDefinitions().keySet().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

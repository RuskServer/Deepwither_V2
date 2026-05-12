package com.ruskserver.deepwither_V2.modules.player.gui;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.skilltree.gui.SkillTreeGui;
import com.ruskserver.deepwither_V2.modules.player.commands.CommandStatus;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class MainMenuGui implements Listener {

    public static final net.kyori.adventure.text.Component GUI_TITLE = net.kyori.adventure.text.Component.text("メインメニュー", NamedTextColor.GOLD);

    private final PlayerManager playerManager;
    private final SkillTreeGui skillTreeGui;
    private final AttributeGui attributeGui;

    @Inject
    public MainMenuGui(PlayerManager playerManager, SkillTreeGui skillTreeGui, AttributeGui attributeGui) {
        this.playerManager = playerManager;
        this.skillTreeGui = skillTreeGui;
        this.attributeGui = attributeGui;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);
        
        // スキルツリアイテム
        gui.setItem(0, createMenuIcon(
            Material.BOOK,
            "§aスキルツリー",
            "§7クリックしてスキルツリーを開く",
            List.of("§f・習得可能なスキルを確認", "§f・スキルポイントを消費して解放")
        ));
        
        // ステータス表示アイテム
        gui.setItem(1, createMenuIcon(
            Material.PAPER,
            "§eステータス",
            "§7クリックして詳細なステータスを確認",
            List.of("§f・現在のレベルと経験値", "§f・各種ステータス詳細")
        ));
        
        // ステ振りアイテム
        gui.setItem(2, createMenuIcon(
            Material.DIAMOND,
            "§bステ振り",
            "§7クリックしてステータスを振り分け",
            List.of("§f・STR・VIT・MND・INT・AGI", "§f・獲得したポイントを割り振り")
        ));
        
        // 装備確認アイテム
        gui.setItem(3, createMenuIcon(
            Material.IRON_CHESTPLATE,
            "§6装備確認",
            "§7クリックして現在の装備を確認",
            List.of("§f・装備中のアイテム詳細", "§f・セットボーナス確認")
        ));
        
        // クエストアイテム
        gui.setItem(4, createMenuIcon(
            Material.MAP,
            "§dクエスト",
            "§7クリックしてクエスト一覧を確認",
            List.of("§f・受注中のクエスト", "§f・達成状況と報酬")
        ));
        
        // 設定アイテム
        gui.setItem(7, createMenuIcon(
            Material.COMMAND_BLOCK,
            "§c設定",
            "§7クリックして各種設定を行う",
            List.of("§f・キー設定", "§f・表示設定", "§f・その他設定")
        ));
        
        // 閉じるアイテム
        gui.setItem(8, createMenuIcon(
            Material.BARRIER,
            "§c閉じる",
            "§7クリックしてメニューを閉じる",
            new ArrayList<>()
        ));
        
        player.openInventory(gui);
    }

    private ItemStack createMenuIcon(Material material, String name, String description, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        
        List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
        loreComponents.add(net.kyori.adventure.text.Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        loreComponents.add(net.kyori.adventure.text.Component.empty());
        
        for (String line : lore) {
            loreComponents.add(net.kyori.adventure.text.Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        
        meta.lore(loreComponents);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(GUI_TITLE)) return;

        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 9) return;

        switch (slot) {
            case 0: // スキルツリー
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                skillTreeGui.openSelectorOrFirst(player);
                break;
                
            case 1: // ステータス
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                player.performCommand("status");
                break;
                
            case 2: // ステ振り
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                attributeGui.open(player);
                break;
                
            case 3: // 装備確認
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                player.sendMessage("§e装備確認機能は準備中です...");
                break;
                
            case 4: // クエスト
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                player.sendMessage("§dクエスト機能は準備中です...");
                break;
                
            case 7: // 設定
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                player.sendMessage("§c設定機能は準備中です...");
                break;
                
            case 8: // 閉じる
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.closeInventory();
                break;
        }
    }
}
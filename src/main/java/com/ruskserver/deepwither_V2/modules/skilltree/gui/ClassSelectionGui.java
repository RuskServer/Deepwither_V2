package com.ruskserver.deepwither_V2.modules.skilltree.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterClass;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterClassProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.provider.CharacterSkillTreeProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class ClassSelectionGui implements Listener {

    private static final Component SELECTION_TITLE = Component.text("クラス選択", NamedTextColor.DARK_GREEN);
    private static final Component CONFIRMATION_TITLE = Component.text("クラス選択の確認", NamedTextColor.DARK_GREEN);
    private static final String ACTION_CONFIRM = "confirm";
    private static final String ACTION_BACK = "back";

    private final Deepwither_V2 plugin;
    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final SkillTreeService skillTreeService;
    private final SkillTreeGui skillTreeGui;
    private final NamespacedKey classKey;
    private final NamespacedKey actionKey;

    @Inject
    public ClassSelectionGui(Deepwither_V2 plugin, CharacterDataRepository characterDataRepository, CharacterService characterService, SkillTreeService skillTreeService, SkillTreeGui skillTreeGui) {
        this.plugin = plugin;
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.skillTreeService = skillTreeService;
        this.skillTreeGui = skillTreeGui;
        this.classKey = new NamespacedKey(plugin, "class_selection");
        this.actionKey = new NamespacedKey(plugin, "class_selection_action");
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, SELECTION_TITLE);

        inventory.setItem(1, createClassItem(CharacterClass.WARRIOR));
        inventory.setItem(3, createClassItem(CharacterClass.MAGE));
        inventory.setItem(5, createClassItem(CharacterClass.ARCHER));
        inventory.setItem(7, createClassItem(CharacterClass.HOLY));

        player.openInventory(inventory);
    }

    private ItemStack createClassItem(CharacterClass characterClass) {
        ItemStack item = new ItemStack(characterClass.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(characterClass.getDisplayName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                lore(characterClass.getDescription(), NamedTextColor.GRAY),
                Component.empty(),
                lore("クラスボーナス", NamedTextColor.YELLOW),
                lore(characterClass.getClassBonus(), NamedTextColor.WHITE),
                Component.empty(),
                lore("プレイスタイル", NamedTextColor.AQUA),
                lore(characterClass.getPlaystyle(), NamedTextColor.WHITE),
                Component.empty(),
                lore("クリックして詳細を確認", NamedTextColor.GREEN)
        ));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classKey, PersistentDataType.STRING, characterClass.name());

        item.setItemMeta(meta);
        return item;
    }

    private void openConfirmation(Player player, CharacterClass selectedClass) {
        Inventory inventory = Bukkit.createInventory(null, 27, CONFIRMATION_TITLE);
        inventory.setItem(13, createConfirmationItem(selectedClass));
        inventory.setItem(11, createActionItem(
                Material.LIME_CONCRETE,
                "このクラスに決定する",
                ACTION_CONFIRM,
                selectedClass,
                NamedTextColor.GREEN
        ));
        inventory.setItem(15, createActionItem(
                Material.RED_CONCRETE,
                "選び直す",
                ACTION_BACK,
                null,
                NamedTextColor.RED
        ));
        player.openInventory(inventory);
    }

    private ItemStack createConfirmationItem(CharacterClass characterClass) {
        ItemStack item = new ItemStack(characterClass.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(characterClass.getDisplayName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                lore(characterClass.getDescription(), NamedTextColor.GRAY),
                Component.empty(),
                lore("クラスボーナス", NamedTextColor.YELLOW),
                lore(characterClass.getClassBonus(), NamedTextColor.WHITE),
                Component.empty(),
                lore("プレイスタイル", NamedTextColor.AQUA),
                lore(characterClass.getPlaystyle(), NamedTextColor.WHITE),
                Component.empty(),
                lore("選択後は変更できません。", NamedTextColor.RED),
                lore("本当にこのクラスで始めますか？", NamedTextColor.YELLOW)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(
            Material material,
            String name,
            String action,
            CharacterClass selectedClass,
            NamedTextColor color
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actionKey, PersistentDataType.STRING, action);
        if (selectedClass != null) {
            pdc.set(classKey, PersistentDataType.STRING, selectedClass.name());
        }
        item.setItemMeta(meta);
        return item;
    }

    private CharacterClass readClass(PersistentDataContainer pdc) {
        String className = pdc.get(classKey, PersistentDataType.STRING);
        if (className == null) return null;
        try {
            CharacterClass characterClass = CharacterClass.valueOf(className);
            return characterClass != CharacterClass.UNKNOWN ? characterClass : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Component lore(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component title = event.getView().title();
        if (!title.equals(SELECTION_TITLE) && !title.equals(CONFIRMATION_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;

        PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
        if (title.equals(SELECTION_TITLE)) {
            CharacterClass selectedClass = readClass(pdc);
            if (selectedClass != null) {
                openConfirmation(player, selectedClass);
            }
            return;
        }

        String action = pdc.get(actionKey, PersistentDataType.STRING);
        if (ACTION_BACK.equals(action)) {
            open(player);
            return;
        }
        if (!ACTION_CONFIRM.equals(action)) return;

        CharacterClass selectedClass = readClass(pdc);
        if (selectedClass != null) {
            confirmSelection(player, selectedClass);
        }
    }

    private void confirmSelection(Player player, CharacterClass selectedClass) {
        characterService.getActiveCharacter(player.getUniqueId()).ifPresent(c -> {
            characterDataRepository.get(c.characterId()).ifPresent(data -> {
                CharacterClass currentClass = data.get(CharacterClassProvider.KEY);
                if (currentClass != null && currentClass != CharacterClass.UNKNOWN) {
                    player.closeInventory();
                    player.sendMessage(Component.text(
                            "クラスは既に " + currentClass.getDisplayName() + " に決定しています。",
                            NamedTextColor.RED
                    ));
                    return;
                }

                // クラスを保存
                data.set(CharacterClassProvider.KEY, selectedClass);

                // スタートノードを自動習得し、カメラ位置を設定
                CharacterSkillTreeProvider.SkillTreeData treeData = data.get(CharacterSkillTreeProvider.KEY);
                if (treeData != null) {
                    String startNodeId = selectedClass.name().toLowerCase() + "_start";
                    treeData.setNodeLevel(startNodeId, 1);
                    
                    // クラスごとに初期カメラ位置を合わせる
                    switch (selectedClass) {
                        case WARRIOR -> treeData.setCameraPosition("global", 0, -1);
                        case MAGE -> treeData.setCameraPosition("global", 1, 0);
                        case ARCHER -> treeData.setCameraPosition("global", 0, 1);
                        case HOLY -> treeData.setCameraPosition("global", -1, 0);
                    }
                    
                    data.markDirty(CharacterSkillTreeProvider.KEY);
                }

                characterDataRepository.save(c.characterId(), data);
                
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.sendMessage(Component.text(
                        selectedClass.getDisplayName() + " クラスを選択しました！",
                        NamedTextColor.GREEN
                ));

                // パッシブの再計算（初期バフ適用）
                skillTreeService.recalculatePassives(player);

                // 少し遅延させてスキルツリーを開く（インベントリが閉じるのを待つ）
                Bukkit.getScheduler().runTask(plugin, () -> {
                    skillTreeGui.openTree(player, "global");
                });
            });
        });
    }
}

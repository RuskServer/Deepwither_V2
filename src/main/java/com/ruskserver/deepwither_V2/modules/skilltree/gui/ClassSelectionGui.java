package com.ruskserver.deepwither_V2.modules.skilltree.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterClass;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.provider.CharacterClassProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.provider.CharacterSkillTreeProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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

import java.util.UUID;

@Component
public class ClassSelectionGui implements Listener {

    private static final net.kyori.adventure.text.Component TITLE = net.kyori.adventure.text.Component.text("クラス選択", NamedTextColor.DARK_GREEN);

    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final SkillTreeService skillTreeService;
    private final SkillTreeGui skillTreeGui;
    private final NamespacedKey classKey;

    @Inject
    public ClassSelectionGui(Deepwither_V2 plugin, CharacterDataRepository characterDataRepository, CharacterService characterService, SkillTreeService skillTreeService, SkillTreeGui skillTreeGui) {
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.skillTreeService = skillTreeService;
        this.skillTreeGui = skillTreeGui;
        this.classKey = new NamespacedKey(plugin, "class_selection");
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, TITLE);

        inventory.setItem(1, createClassItem(CharacterClass.WARRIOR));
        inventory.setItem(3, createClassItem(CharacterClass.MAGE));
        inventory.setItem(5, createClassItem(CharacterClass.ARCHER));
        inventory.setItem(7, createClassItem(CharacterClass.HOLY));

        player.openInventory(inventory);
    }

    private ItemStack createClassItem(CharacterClass characterClass) {
        ItemStack item = new ItemStack(characterClass.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(characterClass.getDisplayName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(classKey, PersistentDataType.STRING, characterClass.name());
        
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;

        PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(classKey, PersistentDataType.STRING)) return;

        String className = pdc.get(classKey, PersistentDataType.STRING);
        if (className == null) return;

        CharacterClass selectedClass = CharacterClass.valueOf(className);
        
        characterService.getActiveCharacter(player.getUniqueId()).ifPresent(c -> {
            characterDataRepository.get(c.characterId()).ifPresent(data -> {
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
                player.sendMessage(net.kyori.adventure.text.Component.text(selectedClass.getDisplayName() + " クラスを選択しました！", NamedTextColor.GREEN));

                // パッシブの再計算（初期バフ適用）
                skillTreeService.recalculatePassives(player);

                // 少し遅延させてスキルツリーを開く（インベントリが閉じるのを待つ）
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Deepwither_V2"), () -> {
                    skillTreeGui.openTree(player, "global");
                });
            });
        });
    }
}

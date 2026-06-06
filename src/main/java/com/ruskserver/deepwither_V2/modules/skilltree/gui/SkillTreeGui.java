package com.ruskserver.deepwither_V2.modules.skilltree.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillRegistry;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNodeType;
import com.ruskserver.deepwither_V2.modules.skilltree.api.UnlockResult;
import com.ruskserver.deepwither_V2.modules.skilltree.event.SkillTreeOpenEvent;
import com.ruskserver.deepwither_V2.modules.skilltree.provider.CharacterSkillTreeProvider;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeRegistry;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class SkillTreeGui implements Listener {

    private static final Component SELECTOR_TITLE = Component.text("スキルツリー選択", NamedTextColor.DARK_GREEN);
    private static final int GUI_ROWS = 6;
    private static final int VIEW_ROWS = 5;
    private static final int VIEW_COLUMNS = 9;

    private final Deepwither_V2 plugin;
    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final SkillTreeRegistry treeRegistry;
    private final SkillTreeService treeService;
    private final SkillRegistry skillRegistry;
    private final NamespacedKey treeKey;
    private final NamespacedKey nodeKey;
    private final NamespacedKey cameraXKey;
    private final NamespacedKey cameraYKey;
    private final NamespacedKey scrollKey;

    @Inject
    public SkillTreeGui(
            Deepwither_V2 plugin,
            CharacterDataRepository characterDataRepository,
            CharacterService characterService,
            SkillTreeRegistry treeRegistry,
            SkillTreeService treeService,
            SkillRegistry skillRegistry
    ) {
        this.plugin = plugin;
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.treeRegistry = treeRegistry;
        this.treeService = treeService;
        this.skillRegistry = skillRegistry;
        this.treeKey = new NamespacedKey(plugin, "skilltree_tree");
        this.nodeKey = new NamespacedKey(plugin, "skilltree_node");
        this.cameraXKey = new NamespacedKey(plugin, "skilltree_cam_x");
        this.cameraYKey = new NamespacedKey(plugin, "skilltree_cam_y");
        this.scrollKey = new NamespacedKey(plugin, "skilltree_scroll");
    }

    public void openSelectorOrFirst(Player player) {
        List<SkillTreeDefinition> trees = sortedTrees();
        if (trees.isEmpty()) {
            player.sendMessage(Component.text("スキルツリーが定義されていません。", NamedTextColor.RED));
            return;
        }
        if (trees.size() == 1) {
            openTree(player, trees.get(0).getId());
            return;
        }

        int rows = Math.max(1, (int) Math.ceil(trees.size() / 9.0));
        Inventory inventory = Bukkit.createInventory(null, rows * 9, SELECTOR_TITLE);
        for (int i = 0; i < trees.size(); i++) {
            SkillTreeDefinition tree = trees.get(i);
            ItemStack item = new ItemStack(tree.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(tree.getDisplayName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("ID: " + tree.getId(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("クリックして開く", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                    Component.text("tree:" + tree.getId(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    public void openTree(Player player, String treeId) {
        CharacterSkillTreeProvider.CameraPosition camera = treeService.getCamera(player, treeId);
        openTree(player, treeId, camera.x(), camera.y());
    }

    private void openTree(Player player, String treeId, int cameraX, int cameraY) {
        SkillTreeDefinition tree = treeRegistry.getTree(treeId);
        if (tree == null) {
            player.sendMessage(Component.text("スキルツリーが見つかりません。", NamedTextColor.RED));
            return;
        }

        Bukkit.getPluginManager().callEvent(new SkillTreeOpenEvent(player, treeId));
        characterService.getActiveCharacter(player.getUniqueId()).ifPresent(c -> {
            characterDataRepository.get(c.characterId()).ifPresent(data -> {
                CharacterSkillTreeProvider.SkillTreeData treeData = data.get(CharacterSkillTreeProvider.KEY);
                if (treeData == null) return;

                Inventory inventory = Bukkit.createInventory(null, GUI_ROWS * 9, Component.text("Skilltree: " + tree.getDisplayName(), NamedTextColor.DARK_AQUA));
                for (SkillTreeNode node : tree.getNodes()) {
                    int screenX = node.getX() - cameraX;
                    int screenY = node.getY() - cameraY;
                    if (screenX < 0 || screenX >= VIEW_COLUMNS || screenY < 0 || screenY >= VIEW_ROWS) continue;
                    int slot = screenY * 9 + screenX;
                    inventory.setItem(slot, buildNodeItem(player, treeData, node, treeId, cameraX, cameraY));
                }

                ItemStack separator = namedItem(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
                for (int i = 45; i < 54; i++) {
                    inventory.setItem(i, separator);
                }
                inventory.setItem(45, buildControl(Material.RED_STAINED_GLASS_PANE, "← 左へ", "LEFT", treeId, cameraX, cameraY));
                inventory.setItem(46, buildControl(Material.LIME_STAINED_GLASS_PANE, "↑ 上へ", "UP", treeId, cameraX, cameraY));
                inventory.setItem(49, buildControl(Material.COMPASS, "位置リセット (" + cameraX + ", " + cameraY + ")", "RESET", treeId, cameraX, cameraY));
                inventory.setItem(50, buildPointItem(treeData.getSkillPoints()));
                inventory.setItem(52, buildControl(Material.LIME_STAINED_GLASS_PANE, "↓ 下へ", "DOWN", treeId, cameraX, cameraY));
                inventory.setItem(53, buildControl(Material.RED_STAINED_GLASS_PANE, "右へ →", "RIGHT", treeId, cameraX, cameraY));

                player.openInventory(inventory);
            });
        });
    }

    private ItemStack buildNodeItem(Player player, CharacterSkillTreeProvider.SkillTreeData treeData, SkillTreeNode node, String treeId, int cameraX, int cameraY) {
        int level = treeData.getNodeLevel(node.getId());
        boolean learned = level > 0;
        boolean maxed = level >= node.getMaxLevel();
        boolean requirementsMet = areRequirementsMet(treeData, node);
        boolean conflicted = isConflicted(treeData, node);
        boolean available = !maxed && requirementsMet && !conflicted;

        Material material = node.getIcon();
        if (node.getType() == SkillTreeNodeType.SKILL && skillRegistry.get(node.getSkillId()) == null) {
            material = Material.BARRIER;
            available = false;
        } else if (!learned && !available) {
            material = conflicted ? Material.BARRIER : Material.RED_STAINED_GLASS_PANE;
        } else if (!learned) {
            material = Material.YELLOW_STAINED_GLASS_PANE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        NamedTextColor nameColor = learned ? NamedTextColor.GREEN : available ? NamedTextColor.YELLOW : NamedTextColor.RED;
        meta.displayName(Component.text(node.getDisplayName(), nameColor).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ID: " + node.getId(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("種別: " + node.getType(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (node.getSkillId() != null) {
            lore.add(Component.text("スキル: " + node.getSkillId(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        for (String line : node.getDescription()) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Lv: " + level + "/" + node.getMaxLevel(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("コスト: " + node.getCostPerLevel() + " SP", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        if (!node.getRequirements().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("前提ノード:", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            for (String requirement : node.getRequirements()) {
                SkillTreeNode requiredNode = treeRegistry.getNode(requirement);
                boolean met = requiredNode != null && treeData.getNodeLevel(requirement) >= requiredNode.getMaxLevel();
                lore.add(Component.text("- " + requirement, met ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            }
        }
        if (!node.getConflicts().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("競合:", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
            for (String conflict : node.getConflicts()) {
                lore.add(Component.text("- " + conflict, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            }
        }

        lore.add(Component.empty());
        if (maxed) {
            lore.add(Component.text("習得済み", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        } else if (available) {
            lore.add(Component.text("クリックで習得", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        } else if (conflicted) {
            lore.add(Component.text("競合により習得不可", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("前提不足", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        if (learned) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(treeKey, PersistentDataType.STRING, treeId);
        pdc.set(nodeKey, PersistentDataType.STRING, node.getId());
        pdc.set(cameraXKey, PersistentDataType.INTEGER, cameraX);
        pdc.set(cameraYKey, PersistentDataType.INTEGER, cameraY);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildControl(Material material, String name, String direction, String treeId, int cameraX, int cameraY) {
        ItemStack item = namedItem(material, Component.text(name, NamedTextColor.YELLOW));
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(treeKey, PersistentDataType.STRING, treeId);
        pdc.set(scrollKey, PersistentDataType.STRING, direction);
        pdc.set(cameraXKey, PersistentDataType.INTEGER, cameraX);
        pdc.set(cameraYKey, PersistentDataType.INTEGER, cameraY);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPointItem(int points) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("SP: " + points, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack namedItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Component title = event.getView().title();
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(title);
        if (!title.equals(SELECTOR_TITLE) && !plainTitle.startsWith("Skilltree: ")) return;

        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        if (title.equals(SELECTOR_TITLE)) {
            String treeId = findLoreToken(clicked, "tree:");
            if (treeId != null) {
                openTree(player, treeId);
            }
            return;
        }

        PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
        String treeId = pdc.get(treeKey, PersistentDataType.STRING);
        if (treeId == null) return;

        String scroll = pdc.get(scrollKey, PersistentDataType.STRING);
        int cameraX = pdc.getOrDefault(cameraXKey, PersistentDataType.INTEGER, 0);
        int cameraY = pdc.getOrDefault(cameraYKey, PersistentDataType.INTEGER, 0);
        if (scroll != null) {
            switch (scroll) {
                case "LEFT" -> cameraX--;
                case "RIGHT" -> cameraX++;
                case "UP" -> cameraY--;
                case "DOWN" -> cameraY++;
                case "RESET" -> {
                    cameraX = 0;
                    cameraY = 0;
                }
                default -> {
                }
            }
            treeService.saveCamera(player, treeId, cameraX, cameraY);
            openTree(player, treeId, cameraX, cameraY);
            return;
        }

        String nodeId = pdc.get(nodeKey, PersistentDataType.STRING);
        if (nodeId == null) return;

        UnlockResult result = treeService.unlock(player, treeId, nodeId);
        if (result.getMessage() != null) {
            player.sendMessage(result.getMessage());
        }
        player.playSound(player.getLocation(), result.isSuccess() ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.ENTITY_VILLAGER_NO, 1f, result.isSuccess() ? 2f : 1f);
        openTree(player, treeId, cameraX, cameraY);
    }

    private List<SkillTreeDefinition> sortedTrees() {
        List<SkillTreeDefinition> trees = new ArrayList<>(treeRegistry.getTrees());
        trees.sort(Comparator.comparing(SkillTreeDefinition::getId));
        return trees;
    }

    private boolean areRequirementsMet(CharacterSkillTreeProvider.SkillTreeData data, SkillTreeNode node) {
        for (String requirement : node.getRequirements()) {
            SkillTreeNode requiredNode = treeRegistry.getNode(requirement);
            if (requiredNode == null || data.getNodeLevel(requirement) < requiredNode.getMaxLevel()) {
                return false;
            }
        }
        return true;
    }

    private boolean isConflicted(CharacterSkillTreeProvider.SkillTreeData data, SkillTreeNode node) {
        for (String conflict : node.getConflicts()) {
            if (data.hasNode(conflict)) return true;
        }
        for (SkillTreeNode learnedNode : treeRegistry.getNodes()) {
            if (data.hasNode(learnedNode.getId()) && learnedNode.getConflicts().contains(node.getId())) {
                return true;
            }
        }
        return false;
    }

    private String findLoreToken(ItemStack item, String prefix) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) return null;
        for (Component line : meta.lore()) {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            if (plain.startsWith(prefix)) {
                return plain.substring(prefix.length()).trim();
            }
        }
        return null;
    }
}

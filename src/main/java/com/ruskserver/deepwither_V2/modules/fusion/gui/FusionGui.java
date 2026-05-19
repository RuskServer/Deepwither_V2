package com.ruskserver.deepwither_V2.modules.fusion.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.fusion.api.FusionRecipe;
import com.ruskserver.deepwither_V2.modules.fusion.service.FusionManager;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class FusionGui implements Listener {

    private final Deepwither_V2 plugin;
    private final FusionManager fusionManager;
    private final ItemManager itemManager;
    private final ItemPDCUtil itemPDCUtil;

    private final NamespacedKey guiTypeKey;
    private final NamespacedKey npcNameKey;
    private final NamespacedKey recipeIdKey;
    private final NamespacedKey slotTypeKey;

    private final Map<Player, String> openedFusionGuis = new HashMap<>();

    // GUIスロット定義
    private static final int[] INGREDIENT_SLOTS = {10, 11, 12, 19, 20, 21};
    private static final int RESULT_SLOT = 16;
    private static final int FUSION_BUTTON_SLOT = 25;

    @Inject
    public FusionGui(Deepwither_V2 plugin, FusionManager fusionManager, ItemManager itemManager, ItemPDCUtil itemPDCUtil) {
        this.plugin = plugin;
        this.fusionManager = fusionManager;
        this.itemManager = itemManager;
        this.itemPDCUtil = itemPDCUtil;

        this.guiTypeKey = new NamespacedKey(plugin, "fusion_gui_type");
        this.npcNameKey = new NamespacedKey(plugin, "fusion_npc_name");
        this.recipeIdKey = new NamespacedKey(plugin, "fusion_recipe_id");
        this.slotTypeKey = new NamespacedKey(plugin, "fusion_slot_type");
    }

    public void openFusionGui(Player player, String npcName) {
        Inventory gui = Bukkit.createInventory(null, 27, net.kyori.adventure.text.Component.text("§5§l" + npcName + "の合成屋"));

        // GUIの識別子を設定
        ItemMeta meta = gui.getItem(0) != null ? gui.getItem(0).getItemMeta() : Bukkit.getItemFactory().getItemMeta(Material.STONE);
        if (meta != null) {
            meta.getPersistentDataContainer().set(guiTypeKey, PersistentDataType.STRING, "fusion_gui");
            meta.getPersistentDataContainer().set(npcNameKey, PersistentDataType.STRING, npcName);
            // gui.setItem(0, new ItemStack(Material.AIR, 1) {{ setItemMeta(meta); }}); // どこかのスロットにPDCを保持させる
        }

        // 枠の設置
        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (!isIngredientSlot(i) && i != RESULT_SLOT && i != FUSION_BUTTON_SLOT) {
                gui.setItem(i, border);
            }
        }

        // 合成ボタン
        ItemStack fusionButton = createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a§l合成する");
        ItemMeta fusionButtonMeta = fusionButton.getItemMeta();
        if (fusionButtonMeta != null) {
            fusionButtonMeta.getPersistentDataContainer().set(slotTypeKey, PersistentDataType.STRING, "fusion_button");
            fusionButton.setItemMeta(fusionButtonMeta);
        }
        gui.setItem(FUSION_BUTTON_SLOT, fusionButton);

        player.openInventory(gui);
        openedFusionGuis.put(player, npcName);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null || !openedFusionGuis.containsKey(player)) return;

        // FusionGuiかどうかを判定
        // TODO: GUIの識別子をどこかのスロットに設定し、それをチェックする
        // 現在はopenedFusionGuisマップで判定しているが、より堅牢にする

        // プレイヤーのインベントリへのクリックは許可
        if (clickedInventory.equals(player.getInventory())) {
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true); // カスタムGUI内のクリックは基本的にキャンセル

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        String slotType = meta.getPersistentDataContainer().get(slotTypeKey, PersistentDataType.STRING);

        if ("fusion_button".equals(slotType)) {
            performFusion(player, clickedInventory, openedFusionGuis.get(player));
        } else if (isIngredientSlot(event.getRawSlot())) {
            // 素材スロットへのアイテム移動を許可
            event.setCancelled(false);
        } else if (event.getRawSlot() == RESULT_SLOT) {
            // 結果スロットからのアイテム取得を許可
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (openedFusionGuis.containsKey(player)) {
            // GUIが閉じられた際に、素材スロットに残っているアイテムをプレイヤーに返す
            Inventory gui = event.getInventory();
            for (int slot : INGREDIENT_SLOTS) {
                ItemStack item = gui.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    player.getInventory().addItem(item);
                }
            }
            openedFusionGuis.remove(player);
        }
    }

    private void performFusion(Player player, Inventory gui, String npcName) {
        // 1. 素材スロットからアイテムを収集
        Map<String, Integer> playerIngredients = new HashMap<>();
        Map<Integer, ItemStack> guiIngredientItems = new HashMap<>();

        for (int slot : INGREDIENT_SLOTS) {
            ItemStack item = gui.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                String itemId = itemPDCUtil.getItemId(item);
                if (itemId != null) {
                    playerIngredients.merge(itemId, item.getAmount(), Integer::sum);
                    guiIngredientItems.put(slot, item);
                } else {
                    // カスタムアイテムではないアイテムが置かれた場合
                    player.sendMessage("§cカスタムアイテムではない素材は使用できません。");
                    return;
                }
            }
        }

        if (playerIngredients.isEmpty()) {
            player.sendMessage("§c合成する素材がありません。");
            return;
        }

        // 2. NPCに紐づくレシピを検索し、素材が一致するものを探す
        List<FusionRecipe> availableRecipes = fusionManager.getRecipesForNpc(npcName);
        FusionRecipe matchedRecipe = null;

        for (FusionRecipe recipe : availableRecipes) {
            Map<String, Integer> requiredIngredients = recipe.getIngredients();
            boolean ingredientsMatch = true;

            // 必要な素材がすべて揃っているか
            if (playerIngredients.size() != requiredIngredients.size()) {
                ingredientsMatch = false;
            }

            if (ingredientsMatch) {
                for (Map.Entry<String, Integer> entry : requiredIngredients.entrySet()) {
                    String requiredItemId = entry.getKey();
                    int requiredAmount = entry.getValue();

                    if (!playerIngredients.containsKey(requiredItemId) || playerIngredients.get(requiredItemId) < requiredAmount) {
                        ingredientsMatch = false;
                        break;
                    }
                }
            }

            // プレイヤーレベルとスキルレベルのチェック (TODO: 実際の実装ではPlayerオブジェクトからレベルとスキルを取得)
            if (ingredientsMatch && recipe.getRequiredLevel() > 0 && player.getLevel() < recipe.getRequiredLevel()) {
                player.sendMessage("§c合成に必要なレベルが不足しています。(必要レベル: " + recipe.getRequiredLevel() + ")");
                ingredientsMatch = false;
            }
            // スキルチェックは省略

            if (ingredientsMatch) {
                matchedRecipe = recipe;
                break;
            }
        }

        if (matchedRecipe == null) {
            player.sendMessage("§cこの素材の組み合わせで合成できるレシピはありません。");
            return;
        }

        // 3. 素材を消費
        for (Map.Entry<String, Integer> entry : matchedRecipe.getIngredients().entrySet()) {
            String requiredItemId = entry.getKey();
            int requiredAmount = entry.getValue();

            for (int slot : INGREDIENT_SLOTS) {
                ItemStack item = gui.getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    String itemId = itemPDCUtil.getItemId(item);
                    if (requiredItemId.equals(itemId)) {
                        int consumeAmount = Math.min(item.getAmount(), requiredAmount);
                        item.setAmount(item.getAmount() - consumeAmount);
                        requiredAmount -= consumeAmount;
                        if (item.getAmount() <= 0) {
                            gui.setItem(slot, null);
                        }
                        if (requiredAmount <= 0) break;
                    }
                }
            }
        }

        // 4. 結果アイテムを生成し、結果スロットに配置
        ItemStack resultItem = itemManager.generate(matchedRecipe.getResultItemId());
        if (resultItem != null) {
            resultItem.setAmount(matchedRecipe.getResultAmount());
            gui.setItem(RESULT_SLOT, resultItem);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f); // 成功音
            player.sendMessage("§aアイテムの合成に成功しました！");
        } else {
            player.sendMessage("§c合成結果アイテムの生成に失敗しました。");
        }
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isIngredientSlot(int slot) {
        for (int ingredientSlot : INGREDIENT_SLOTS) {
            if (slot == ingredientSlot) return true;
        }
        return false;
    }
}

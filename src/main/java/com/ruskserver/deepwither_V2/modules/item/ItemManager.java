package com.ruskserver.deepwither_V2.modules.item;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.modifier.ModifierManager;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemManager implements Startable {

    private final Map<String, CustomItem> registry = new HashMap<>();
    private final DIContainer container;
    private final ItemPDCUtil pdcUtil;
    private final ModifierManager modifierManager;

    @Inject
    public ItemManager(DIContainer container, ItemPDCUtil pdcUtil, ModifierManager modifierManager) {
        this.container = container;
        this.pdcUtil = pdcUtil;
        this.modifierManager = modifierManager;
    }

    @Override
    public void start() {
        // DIコンテナによって収集されたCustomItemの実装クラスを自動登録
        // (すべてのコンポーネントのインスタンス化が完了した後に呼ばれます)
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof CustomItem) {
                CustomItem item = (CustomItem) instance;
                registry.put(item.getId(), item);
            }
        }
        org.bukkit.Bukkit.getLogger().info("[ItemManager] " + registry.size() + " 個のカスタムアイテムをロードしました。");
    }

    /**
     * 登録されているすべてのカスタムアイテムのIDを取得します。
     * @return アイテムIDのリスト
     */
    public List<String> getRegisteredItemIds() {
        return new ArrayList<>(registry.keySet());
    }

    /**
     * 指定されたIDのアイテム定義を取得します。
     * @param id アイテムID
     * @return CustomItemのインスタンス、存在しない場合はnull
     */
    public CustomItem getCustomItem(String id) {
        return registry.get(id);
    }

    /**
     * 指定されたIDのアイテムを新規生成します。
     * ランダムモディファイアの計算とPDCへの書き込みが行われます。
     *
     * @param itemId 生成するアイテムのID
     * @return 生成されたItemStack。IDが存在しない場合はnull
     */
    public ItemStack generate(String itemId) {
        CustomItem customItem = registry.get(itemId);
        if (customItem == null) {
            return null;
        }

        ItemStack item = new ItemStack(customItem.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (customItem.getCustomModelData() != 0) {
                meta.setCustomModelData(customItem.getCustomModelData());
            }
            item.setItemMeta(meta);
        }

        // ランダムモディファイアの決定
        Map<StatType, Double> modifiers = modifierManager.rollModifiers(customItem);

        // PDCにデータを書き込む
        pdcUtil.setItemId(item, itemId);
        pdcUtil.setModifiers(item, modifiers);

        // Lore等を適用する
        updateItemMeta(item);

        return item;
    }

    /**
     * ItemStackのPDCを読み取り、現在のクラス定義に合わせてDisplayNameやLoreを再構築します。
     *
     * @param item 更新対象のItemStack
     */
    public void updateItemMeta(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return;

        String id = pdcUtil.getItemId(item);
        if (id == null) return; // カスタムアイテムではない

        CustomItem customItem = registry.get(id);
        if (customItem == null) return; // 未定義・削除済みのアイテム定義

        Map<StatType, Double> modifiers = pdcUtil.getModifiers(item);
        ItemMeta meta = item.getItemMeta();

        // バニラの属性表示を非表示にする
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // 表示名 (イタリック解除)
        meta.displayName(Component.text(customItem.getDisplayName()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        // 1. レアリティ
        lore.add(Component.text("◆ " + customItem.getRarity().getDisplayName())
                .color(customItem.getRarity().getColor())
                .decoration(TextDecoration.ITALIC, false));

        // 2. フレーバーテキスト (30文字で自動改行)
        String flavorText = customItem.getFlavorText();
        if (flavorText != null && !flavorText.isEmpty()) {
            for (String line : wrapText(flavorText, 30)) {
                lore.add(Component.text(line).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
        }

        // 3. 装備ステータス
        if (!customItem.getBaseStats().isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("§8§m--------§7 装備ステータス §8§m--------")
                    .decoration(TextDecoration.ITALIC, false));

            for (Map.Entry<StatType, Double> entry : customItem.getBaseStats().entrySet()) {
                StatType type = entry.getKey();
                double baseValue = entry.getValue();
                double modValue = modifiers.getOrDefault(type, 0.0);

                String line = " §7" + type.getDisplayName() + ": §f" + baseValue;
                if (modValue > 0) {
                    line += " §a(+" + modValue + ")";
                } else if (modValue < 0) {
                    line += " §c(" + modValue + ")";
                }
                lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
            }
        }

        meta.lore(lore);

        // CustomModelDataも最新化
        if (customItem.getCustomModelData() != 0) {
            meta.setCustomModelData(customItem.getCustomModelData());
        }

        item.setItemMeta(meta);
    }

    /**
     * 文字列を指定された長さで簡易的に分割して返します。
     * @param text 対象の文字列
     * @param maxLength 1行の最大文字数
     * @return 分割された文字列のリスト
     */
    private List<String> wrapText(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        if (text == null) return result;

        int length = text.length();
        for (int i = 0; i < length; i += maxLength) {
            result.add(text.substring(i, Math.min(length, i + maxLength)));
        }
        return result;
    }
}

package com.ruskserver.deepwither_V2.modules.combat.damage.phases;

import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * ダメージパイプラインの途中で、プレイヤーが装備している CustomItem の固有能力（パッシブ）を呼び出すフェーズ。
 */
public class ItemAbilityPhase implements DamagePhase {

    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;

    public ItemAbilityPhase(ItemManager itemManager, ItemPDCUtil pdcUtil) {
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
    }

    @Override
    public void process(DamageContext context) {
        // 攻撃者の装備を走査し、onAttack を呼び出す
        if (context.getAttacker() != null) {
            triggerAbilities(context.getAttacker(), context, true);
        }

        // 防御者の装備を走査し、onDefend を呼び出す
        if (context.getDefender() != null) {
            triggerAbilities(context.getDefender(), context, false);
        }
    }

    private void triggerAbilities(LivingEntity entity, DamageContext context, boolean isAttacker) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;

        // メインハンド、オフハンド、防具すべてをチェックする
        ItemStack[] items = new ItemStack[]{
                equipment.getItemInMainHand(),
                equipment.getItemInOffHand(),
                equipment.getHelmet(),
                equipment.getChestplate(),
                equipment.getLeggings(),
                equipment.getBoots()
        };

        for (ItemStack item : items) {
            if (item == null || item.isEmpty()) continue;

            String customId = pdcUtil.getItemId(item);
            if (customId == null) continue;

            CustomItem customItem = itemManager.getCustomItem(customId);
            if (customItem != null) {
                if (isAttacker) {
                    customItem.onAttack(context);
                } else {
                    customItem.onDefend(context);
                }
            }
        }
    }
}

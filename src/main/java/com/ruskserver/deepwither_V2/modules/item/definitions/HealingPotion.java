package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Map;

/**
 * Lunaris Atelier製の回復ポーション。
 * 使用すると即座に体力を回復します。
 */
@Component
public class HealingPotion implements CustomItem {

    private final VirtualHealthManager healthManager;

    @Inject
    public HealingPotion(VirtualHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public String getId() {
        return "healing_potion";
    }

    @Override
    public Material getMaterial() {
        return Material.POTION;
    }

    @Override
    public String getDisplayName() {
        return "§a回復ポーション";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "Lunaris Atelier製の標準的な回復薬。月光樹の雫とエーテル結晶を調合して作られており、飲用することで傷を癒やす。";
    }

    @Override
    public double getSellPrice() {
        return 250.0;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null) return;

            event.setCancelled(true);
            
            double currentHealth = healthManager.getHealth(event.getPlayer());
            double maxHealth = healthManager.getMaxHealth(event.getPlayer());
            
            if (currentHealth >= maxHealth) {
                event.getPlayer().sendMessage("§c体力が満タンです。");
                return;
            }

            // 50 HP 回復
            healthManager.heal(event.getPlayer(), 50.0);
            
            // アイテムを消費
            item.setAmount(item.getAmount() - 1);
            
            // 効果音とメッセージ
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            event.getPlayer().sendMessage("§a回復ポーションを使用して体力を50回復しました。");
        }
    }
}

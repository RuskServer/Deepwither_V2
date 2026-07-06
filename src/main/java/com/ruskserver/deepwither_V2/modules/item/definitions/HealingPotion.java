package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillCooldownService;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Lunaris Atelier製の回復ポーション。
 * 使用すると即座に体力を回復します。
 */
@Component
public class HealingPotion implements CustomItem {

    private static final Duration COOLDOWN = Duration.ofSeconds(5);

    private final VirtualHealthManager healthManager;
    private final SkillCooldownService cooldownService;

    @Inject
    public HealingPotion(VirtualHealthManager healthManager, SkillCooldownService cooldownService) {
        this.healthManager = healthManager;
        this.cooldownService = cooldownService;
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
    public int getMaxStackSize() {
        return 6;
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

            var player = event.getPlayer();

            if (cooldownService.isOnCooldown(player.getUniqueId(), getId())) {
                double secs = cooldownService.getRemaining(player.getUniqueId(), getId()).toMillis() / 1000.0;
                player.sendMessage("§c回復ポーションはあと" + String.format("%.1f", secs) + "秒使用できません。");
                return;
            }

            double currentHealth = healthManager.getHealth(player);
            double maxHealth = healthManager.getMaxHealth(player);

            if (currentHealth >= maxHealth) {
                player.sendMessage("§c体力が満タンです。");
                return;
            }

            double amount = maxHealth * 0.25;
            healthManager.heal(player, amount);
            cooldownService.applyCooldown(player.getUniqueId(), getId(), COOLDOWN);
            
            // アイテムを消費
            item.setAmount(item.getAmount() - 1);
            
            // 効果音とメッセージ
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            player.sendMessage("§a回復ポーションを使用して最大体力の25%を回復しました。");
        }
    }
}

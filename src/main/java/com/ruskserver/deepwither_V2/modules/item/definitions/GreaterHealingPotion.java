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

@Component
public class GreaterHealingPotion implements CustomItem {

    private static final double HEALTH_RESTORE_PERCENT = 0.30;
    private static final Duration COOLDOWN = Duration.ofSeconds(5);
    private static final String COOLDOWN_KEY = "healing_potion";

    private final VirtualHealthManager healthManager;
    private final SkillCooldownService cooldownService;

    @Inject
    public GreaterHealingPotion(
            VirtualHealthManager healthManager,
            SkillCooldownService cooldownService
    ) {
        this.healthManager = healthManager;
        this.cooldownService = cooldownService;
    }

    @Override
    public String getId() {
        return "greater_healing_potion";
    }

    @Override
    public Material getMaterial() {
        return Material.POTION;
    }

    @Override
    public String getDisplayName() {
        return "§a高濃度回復ポーション";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public String getFlavorText() {
        return "Lazward DynamicsとKryos Industrial Mechanicsが共同規格化した高濃度回復薬。野外任務向けの安定剤により、標準品より高い治癒効果を持つ。";
    }

    @Override
    public int getMaxStackSize() {
        return 6;
    }

    @Override
    public double getSellPrice() {
        return 375.0;
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) return;
        event.setCancelled(true);

        var player = event.getPlayer();
        if (cooldownService.isOnCooldown(player.getUniqueId(), COOLDOWN_KEY)) {
            double seconds = cooldownService.getRemaining(player.getUniqueId(), COOLDOWN_KEY)
                    .toMillis() / 1000.0;
            player.sendMessage("§c回復ポーションはあと"
                    + String.format("%.1f", seconds) + "秒使用できません。");
            return;
        }

        double currentHealth = healthManager.getHealth(player);
        double maxHealth = healthManager.getMaxHealth(player);
        if (currentHealth >= maxHealth) {
            player.sendMessage("§c体力が満タンです。");
            return;
        }

        healthManager.heal(player, maxHealth * HEALTH_RESTORE_PERCENT);
        cooldownService.applyCooldown(player.getUniqueId(), COOLDOWN_KEY, COOLDOWN);
        item.setAmount(item.getAmount() - 1);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.8f);
        player.sendMessage("§a高濃度回復ポーションを使用して最大体力の30%を回復しました。");
    }
}

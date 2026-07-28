package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.combat.health.ManaManager;
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
public class GreaterManaPotion implements CustomItem {

    private static final double MANA_RESTORE_PERCENT = 0.35;
    private static final Duration COOLDOWN = Duration.ofSeconds(30);
    private static final String COOLDOWN_KEY = "mana_potion";

    private final ManaManager manaManager;
    private final SkillCooldownService cooldownService;

    @Inject
    public GreaterManaPotion(ManaManager manaManager, SkillCooldownService cooldownService) {
        this.manaManager = manaManager;
        this.cooldownService = cooldownService;
    }

    @Override
    public String getId() {
        return "greater_mana_potion";
    }

    @Override
    public Material getMaterial() {
        return Material.POTION;
    }

    @Override
    public String getDisplayName() {
        return "§b高濃度マナポーション";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public String getFlavorText() {
        return "Lazward DynamicsとKryos Industrial Mechanicsが共同規格化した高濃度マナ回復薬。結晶冷却技術により、標準品より多くの魔力を安全に補給できる。";
    }

    @Override
    public int getMaxStackSize() {
        return 6;
    }

    @Override
    public double getSellPrice() {
        return 700.0;
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
            player.sendMessage("§cマナポーションはあと"
                    + String.format("%.1f", seconds) + "秒使用できません。");
            return;
        }

        double currentMana = manaManager.getMana(player);
        double maxMana = manaManager.getMaxMana(player);
        if (currentMana >= maxMana) {
            player.sendMessage("§cマナが満タンです。");
            return;
        }

        manaManager.restore(player, maxMana * MANA_RESTORE_PERCENT);
        cooldownService.applyCooldown(player.getUniqueId(), COOLDOWN_KEY, COOLDOWN);
        item.setAmount(item.getAmount() - 1);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        player.sendMessage("§b高濃度マナポーションを使用して最大マナの35%を回復しました。");
    }
}

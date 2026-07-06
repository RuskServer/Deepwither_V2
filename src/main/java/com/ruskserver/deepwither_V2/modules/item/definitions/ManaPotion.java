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
public class ManaPotion implements CustomItem {

    private static final double MANA_RESTORE_PERCENT = 0.30;
    private static final Duration COOLDOWN = Duration.ofSeconds(30);

    private final ManaManager manaManager;
    private final SkillCooldownService cooldownService;

    @Inject
    public ManaPotion(ManaManager manaManager, SkillCooldownService cooldownService) {
        this.manaManager = manaManager;
        this.cooldownService = cooldownService;
    }

    @Override
    public String getId() {
        return "mana_potion";
    }

    @Override
    public Material getMaterial() {
        return Material.POTION;
    }

    @Override
    public String getDisplayName() {
        return "§bマナポーション";
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
        return "Lunaris Atelier製のマナ回復薬。エーテル結晶を高濃度で溶解しており、飲用することで内部の魔力を高速回復する。";
    }

    @Override
    public int getMaxStackSize() {
        return 6;
    }

    @Override
    public double getSellPrice() {
        return 500.0;
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
                player.sendMessage("§cマナポーションはあと" + String.format("%.1f", secs) + "秒使用できません。");
                return;
            }

            double currentMana = manaManager.getMana(player);
            double maxMana = manaManager.getMaxMana(player);

            if (currentMana >= maxMana) {
                player.sendMessage("§cマナが満タンです。");
                return;
            }

            double amount = maxMana * MANA_RESTORE_PERCENT;
            manaManager.restore(player, amount);
            cooldownService.applyCooldown(player.getUniqueId(), getId(), COOLDOWN);

            item.setAmount(item.getAmount() - 1);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            player.sendMessage("§bマナポーションを使用して最大マナの30%を回復しました。");
        }
    }
}

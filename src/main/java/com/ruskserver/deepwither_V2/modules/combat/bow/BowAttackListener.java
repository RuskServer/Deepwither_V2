package com.ruskserver.deepwither_V2.modules.combat.bow;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.util.ItemPDCUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

@Component
public class BowAttackListener implements Listener {

    private final NamespacedKey bowItemKey;
    private final ItemManager itemManager;
    private final ItemPDCUtil pdcUtil;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public BowAttackListener(Deepwither_V2 plugin, ItemManager itemManager, ItemPDCUtil pdcUtil,
                             DamagePipelineManager damagePipelineManager) {
        this.bowItemKey = new NamespacedKey(plugin, "bow_item_id");
        this.itemManager = itemManager;
        this.pdcUtil = pdcUtil;
        this.damagePipelineManager = damagePipelineManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;

        String itemId = pdcUtil.getItemId(bow);
        if (itemId == null) return;
        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (!(customItem instanceof BowItem bowItem)) return;

        if (event.getProjectile() instanceof AbstractArrow arrow) {
            arrow.getPersistentDataContainer().set(bowItemKey, PersistentDataType.STRING, itemId);
            double velocityMult = bowItem.getVelocityMultiplier();
            if (velocityMult != 1.0) {
                arrow.setVelocity(arrow.getVelocity().multiply(velocityMult));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        String itemId = arrow.getPersistentDataContainer().get(bowItemKey, PersistentDataType.STRING);
        if (itemId == null) return;

        CustomItem customItem = itemManager.getCustomItem(itemId);
        if (!(customItem instanceof BowItem bow)) return;

        if (!(arrow.getShooter() instanceof LivingEntity shooter)) return;

        event.setDamage(0);
        event.setCancelled(true);

        // 距離計算
        Location origin = arrow.getOrigin();
        double distance = (origin != null)
            ? origin.distance(target.getLocation())
            : shooter.getLocation().distance(target.getLocation());

        double distanceMultiplier = bow.getDamageMultiplier(distance);

        // パイプライン経由でダメージ処理
        damagePipelineManager.processDamage(shooter, target, DamageType.PHYSICAL, 0.0, bow.getTags(), distanceMultiplier);
    }
}

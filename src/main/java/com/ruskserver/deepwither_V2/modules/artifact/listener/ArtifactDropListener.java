package com.ruskserver.deepwither_V2.modules.artifact.listener;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

@Service
public class ArtifactDropListener implements Listener {

    private final ItemManager itemManager;
    private final Random random = new Random();

    // デフォルトのドロップ確率 (2%)
    private static final double DROP_CHANCE = 0.02;

    @Inject
    public ArtifactDropListener(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // モンスターのみ
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        // プレイヤーに倒されたかチェック
        if (event.getEntity().getKiller() == null) {
            return;
        }

        if (random.nextDouble() < DROP_CHANCE) {
            ItemStack box = itemManager.generate("artifact_box");
            if (box != null) {
                event.getDrops().add(box);
            }
        }
    }
}

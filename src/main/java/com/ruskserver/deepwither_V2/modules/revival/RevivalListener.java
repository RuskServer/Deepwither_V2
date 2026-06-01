package com.ruskserver.deepwither_V2.modules.revival;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.player.PlayerManager;
import com.ruskserver.deepwither_V2.modules.player.provider.PlayerLevelProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class RevivalListener implements Listener {

    private final RevivalManager revivalManager;
    private final PlayerDataRepository repository;
    private final PlayerManager playerManager;

    @Inject
    public RevivalListener(RevivalManager revivalManager, PlayerDataRepository repository,
                           PlayerManager playerManager) {
        this.revivalManager = revivalManager;
        this.repository = repository;
        this.playerManager = playerManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (revivalManager.hasDeathPenalty(uuid)) {
            revivalManager.consumeDeathPenalty(uuid);
            applyDeathPenalty(player);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player target && revivalManager.isDowned(target)) {
            event.setCancelled(true);
        }
    }

    private void applyDeathPenalty(Player player) {
        repository.get(player.getUniqueId()).ifPresent(data -> {
            PlayerLevelProvider.LevelData levelData = data.get(PlayerLevelProvider.KEY);
            if (levelData == null || levelData.getLevel() <= 0) return;

            int currentExp = levelData.getExp();
            int deduction = (int) (currentExp * 0.05);
            if (deduction <= 0) return;

            int newExp = Math.max(0, currentExp - deduction);
            levelData.setExp(newExp);
            data.markDirty(PlayerLevelProvider.KEY);
            repository.save(player.getUniqueId(), data);

            playerManager.syncVanillaExp(player);

            player.sendMessage(Component.text("§c>> デスペナルティ: ")
                    .append(Component.text(String.format("%,d", deduction) + " EXP", NamedTextColor.RED))
                    .append(Component.text(" を失いました (5%)", NamedTextColor.GRAY)));
        });
    }
}

package com.ruskserver.deepwither_V2.modules.mob.region;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@com.ruskserver.deepwither_V2.core.di.annotations.Component
public class SafeZoneListener implements Listener {

    private final MobRegionConfig regionConfig;
    private final PlayerDataRepository repository;

    @Inject
    public SafeZoneListener(MobRegionConfig regionConfig, PlayerDataRepository repository) {
        this.regionConfig = regionConfig;
        this.repository = repository;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        boolean wasInSafeZone = isInAnySafeZone(from);
        boolean nowInSafeZone = isInAnySafeZone(to);

        if (nowInSafeZone && !wasInSafeZone) {
            Title title = Title.title(
                    Component.text("セーフゾーン", NamedTextColor.AQUA),
                    Component.text("リスポーン地点を更新しました", NamedTextColor.GRAY)
            );
            player.showTitle(title);
            player.sendMessage(
                    Component.text(">> セーフゾーンに侵入しました。", NamedTextColor.AQUA)
                            .append(Component.text("リスポーン地点が現在地に設定されました。", NamedTextColor.WHITE, TextDecoration.BOLD))
            );
            saveSpawn(player, to);
        } else if (!nowInSafeZone && wasInSafeZone) {
            Title title = Title.title(
                    Component.text("危険区域", NamedTextColor.RED),
                    Component.empty()
            );
            player.showTitle(title);
            player.sendMessage(Component.text(">> 危険区域へ移動しました。", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        repository.get(player.getUniqueId()).ifPresent(data -> {
            Location spawn = data.get(SafeZoneSpawnProvider.KEY);
            if (spawn != null && spawn.getWorld() != null) {
                event.setRespawnLocation(spawn);
            }
        });
    }

    private boolean isInAnySafeZone(Location location) {
        return regionConfig.getRegions().stream()
                .filter(MobRegion::isSafeZone)
                .anyMatch(sz -> sz.contains(location));
    }

    private void saveSpawn(Player player, Location location) {
        repository.get(player.getUniqueId()).ifPresent(data -> {
            data.set(SafeZoneSpawnProvider.KEY, location.clone());
        });
    }
}

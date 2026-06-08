package com.ruskserver.deepwither_V2.modules.resourcepack;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * プレイヤーのログイン時にリソースパックを送信し、適用状態を監視するリスナー。
 */
@Component
public class ResourcePackListener implements Listener, PlayerLifecycleTask {

    private final JavaPlugin plugin;
    private final ResourcePackConfig config;

    @Inject
    public ResourcePackListener(JavaPlugin plugin, ResourcePackConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.JOIN);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.GUI;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        String url = config.getUrl();
        
        if (url == null || url.isEmpty() || url.equals("https://example.com/resourcepack.zip")) {
            return CompletableFuture.completedFuture(null);
        }

        // 旧コードの挙動（1秒待機）を踏襲
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            context.player().ifPresent(player -> {
                String hash = config.getHash();
                if (hash != null && !hash.isEmpty()) {
                    player.setResourcePack(url, hash);
                } else {
                    player.setResourcePack(url);
                }
            });
        }, 20L);
        return CompletableFuture.completedFuture(null);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (!config.isRequired()) {
            return;
        }

        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        
        // 拒否またはダウンロード失敗時に強制適用設定ならキック
        if (status == PlayerResourcePackStatusEvent.Status.DECLINED || 
            status == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
            
            event.getPlayer().kick(config.getKickMessage());
        }
    }
}

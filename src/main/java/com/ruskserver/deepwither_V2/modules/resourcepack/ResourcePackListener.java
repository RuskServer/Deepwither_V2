package com.ruskserver.deepwither_V2.modules.resourcepack;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * プレイヤーのログイン時にリソースパックを送信し、適用状態を監視するリスナー。
 */
@Component
public class ResourcePackListener implements Listener {

    private final JavaPlugin plugin;
    private final ResourcePackConfig config;

    @Inject
    public ResourcePackListener(JavaPlugin plugin, ResourcePackConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String url = config.getUrl();
        
        if (url == null || url.isEmpty() || url.equals("https://example.com/resourcepack.zip")) {
            return;
        }

        // 旧コードの挙動（1秒待機）を踏襲
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                String hash = config.getHash();
                if (hash != null && !hash.isEmpty()) {
                    player.setResourcePack(url, hash);
                } else {
                    player.setResourcePack(url);
                }
            }
        }, 20L);
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

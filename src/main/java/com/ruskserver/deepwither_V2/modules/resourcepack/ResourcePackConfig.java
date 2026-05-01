package com.ruskserver.deepwither_V2.modules.resourcepack;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.logging.Level;

/**
 * リソースパックの設定を保持・管理するコンポーネント。
 */
@Component
public class ResourcePackConfig {

    private final String url;
    private final String hash;
    private final boolean required;
    private final net.kyori.adventure.text.Component kickMessage;

    @Inject
    public ResourcePackConfig(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        
        this.url = config.getString("resource-pack.url", "");
        this.required = config.getBoolean("resource-pack.required", false);
        
        String kickMsgRaw = config.getString("resource-pack.kick-message", "<red>リソースパックを適用してください。");
        this.kickMessage = MiniMessage.miniMessage().deserialize(kickMsgRaw.replace("<br>", "\n"));

        // ハッシュの自動取得
        String configHash = config.getString("resource-pack.hash", "");
        if (configHash.isEmpty() && !url.isEmpty() && !url.equals("https://example.com/resourcepack.zip")) {
            plugin.getLogger().info("Downloading resource pack to calculate hash...");
            this.hash = calculateHash(plugin);
        } else {
            this.hash = configHash;
        }
    }

    private String calculateHash(JavaPlugin plugin) {
        try {
            URL urlObj = URI.create(this.url).toURL();
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            
            try (InputStream is = urlObj.openStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, n);
                }
            }
            
            byte[] hashBytes = digest.digest();
            String result = HexFormat.of().formatHex(hashBytes);
            plugin.getLogger().info("Successfully calculated resource pack hash: " + result);
            return result;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to calculate resource pack hash from URL: " + this.url, e);
            return "";
        }
    }

    public String getUrl() {
        return url;
    }

    public String getHash() {
        return hash;
    }

    public boolean isRequired() {
        return required;
    }

    public net.kyori.adventure.text.Component getKickMessage() {
        return kickMessage;
    }
}

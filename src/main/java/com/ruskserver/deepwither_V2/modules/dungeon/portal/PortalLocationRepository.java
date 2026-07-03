package com.ruskserver.deepwither_V2.modules.dungeon.portal;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Service
public class PortalLocationRepository implements Startable {

    private static final String FILE_NAME = "portal-locations.yml";

    private final JavaPlugin plugin;
    private final Logger log;

    private final List<PortalLocation> locations = new ArrayList<>();
    private File file;

    @Inject
    public PortalLocationRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    @Override
    public void start() {
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public List<PortalLocation> getAll() {
        return Collections.unmodifiableList(locations);
    }

    public List<PortalLocation> findByDungeonId(String dungeonId) {
        return locations.stream()
                .filter(loc -> loc.dungeonId().equals(dungeonId))
                .toList();
    }

    public PortalLocation get(String id) {
        return locations.stream()
                .filter(loc -> loc.id().equals(id))
                .findFirst().orElse(null);
    }

    public void add(PortalLocation location) {
        locations.removeIf(loc -> loc.id().equals(location.id()));
        locations.add(location);
        save();
    }

    public void remove(String id) {
        locations.removeIf(loc -> loc.id().equals(id));
        save();
    }

    private void load() {
        locations.clear();
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("portal-locations");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;
            locations.add(new PortalLocation(
                    key,
                    sec.getString("world", "world"),
                    sec.getDouble("x", 0),
                    sec.getDouble("y", 64),
                    sec.getDouble("z", 0),
                    sec.getString("dungeon-id", "")
            ));
        }
        log.info("[PortalLocationRepository] " + locations.size() + " 個のポータル位置を読み込みました。");
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (PortalLocation loc : locations) {
            String path = "portal-locations." + loc.id();
            config.set(path + ".world", loc.world());
            config.set(path + ".x", loc.x());
            config.set(path + ".y", loc.y());
            config.set(path + ".z", loc.z());
            config.set(path + ".dungeon-id", loc.dungeonId());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            log.severe("[PortalLocationRepository] 保存失敗: " + e.getMessage());
        }
    }
}

package com.ruskserver.deepwither_V2.modules.mob.region;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * config.yml を読み込み、{@link MobRegion} のリストを提供するサービス。
 * <p>
 * {@code worldguard-region} キーに指定した名前の WorldGuard Region を参照します。
 * プラグインの data フォルダに config.yml が存在しない場合、
 * リソース内のデフォルト設定をコピーして使用します。
 */
@Service
public class MobRegionConfig implements Startable {

    private final JavaPlugin plugin;
    private final Logger log;

    private List<MobRegion> regions = new ArrayList<>();

    @Inject
    public MobRegionConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    @Override
    public void start() {
        reload();
    }

    /**
     * config.yml を再読み込みし、Regionリストを更新します。
     * /reload コマンドやホットリロードに対応するために公開しています。
     */
    public void reload() {
        saveDefaultConfig();
        FileConfiguration config = loadConfig();
        regions = parseRegions(config);
        log.info("[MobRegionConfig] " + regions.size() + " 個のRegionを読み込みました。");
    }

    /** 読み込み済みのRegionリストを返します（読み取り専用）。 */
    public List<MobRegion> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    /** 指定座標がセーフゾーン内かどうかを判定します。 */
    public boolean isInSafeZone(Location location) {
        return regions.stream()
                .filter(MobRegion::isSafeZone)
                .anyMatch(sz -> sz.contains(location));
    }

    // --- 内部処理 ---

    /** config.yml が存在しない場合、デフォルト設定をコピーします。 */
    private void saveDefaultConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    private FileConfiguration loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // デフォルト値をリソースからマージする
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }
        return config;
    }

    private List<MobRegion> parseRegions(FileConfiguration config) {
        ConfigurationSection rootSection = config.getConfigurationSection("mob-regions");
        if (rootSection == null) {
            log.warning("[MobRegionConfig] config.yml に 'mob-regions' セクションが見つかりません。");
            return new ArrayList<>();
        }

        List<MobRegion> result = new ArrayList<>();
        for (String regionName : rootSection.getKeys(false)) {
            ConfigurationSection section = rootSection.getConfigurationSection(regionName);
            if (section == null) continue;

            try {
                MobRegion region = parseRegion(regionName, section);
                if (region != null) {
                    result.add(region);
                }
            } catch (Exception e) {
                log.warning("[MobRegionConfig] Region '" + regionName + "' の解析に失敗しました: " + e.getMessage());
            }
        }
        return result;
    }

    private MobRegion parseRegion(String name, ConfigurationSection section) {
        boolean isSafeZone = section.getBoolean("safe-zone", false);

        String worldName = section.getString("world", "world");
        World world = resolveWorld(worldName);
        if (world == null) {
            log.warning("[MobRegionConfig] Region '" + name + "' のワールド '" + worldName
                    + "' が見つかりません。読み込み済みワールド: " + getLoadedWorldNames() + "。スキップします。");
            return null;
        }

        // WorldGuard Region の解決
        String wgRegionId = section.getString("worldguard-region");
        if (wgRegionId == null || wgRegionId.isBlank()) {
            log.warning("[MobRegionConfig] Region '" + name + "' に 'worldguard-region' が設定されていません。スキップします。");
            return null;
        }

        ProtectedRegion wgRegion = resolveWgRegion(world, wgRegionId);
        if (wgRegion == null) {
            log.warning("[MobRegionConfig] WorldGuard に Region '" + wgRegionId
                    + "' が見つかりません（world=" + worldName + "）。スキップします。");
            return null;
        }

        int spawnIntervalTicks = section.getInt("spawn-interval-ticks", 200);
        int maxMobs = section.getInt("max-mobs", 10);
        int maxLevel = section.getInt("max-level", 1);

        // セーフゾーンはスポーンテーブルを空にする
        List<SpawnEntry> spawnTable = new ArrayList<>();
        if (!isSafeZone) {
            List<?> rawTable = section.getList("spawn-table", new ArrayList<>());
            for (Object raw : rawTable) {
                // getList() の要素はYAMLマップを LinkedHashMap<String,Object> として返す（MemorySection ではない）
                if (raw instanceof java.util.Map<?, ?> map) {
                    Object mobIdObj = map.get("mob-id");
                    Object weightObj = map.get("weight");
                    String mobId = mobIdObj instanceof String s ? s : null;
                    int weight = weightObj instanceof Number n ? n.intValue() : 1;
                    if (mobId != null && !mobId.isBlank() && weight > 0) {
                        spawnTable.add(new SpawnEntry(mobId, weight));
                    }
                }
            }
        }

        return new MobRegion(name, isSafeZone, world, wgRegion, spawnTable, spawnIntervalTicks, maxMobs, maxLevel);
    }

    private World resolveWorld(String configuredName) {
        if (configuredName == null || configuredName.isBlank()) {
            return null;
        }

        World direct = plugin.getServer().getWorld(configuredName);
        if (direct != null) {
            return direct;
        }

        String normalized = normalizeWorldName(configuredName);
        for (World world : plugin.getServer().getWorlds()) {
            if (normalizeWorldName(world.getName()).equals(normalized)) {
                return world;
            }
        }

        String alias = switch (normalized) {
            case "overworld" -> "world";
            case "nether", "the_nether" -> "world_nether";
            case "end", "the_end" -> "world_the_end";
            default -> null;
        };
        return alias == null ? null : plugin.getServer().getWorld(alias);
    }

    private String normalizeWorldName(String name) {
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        if (normalized.startsWith("the_")) {
            normalized = normalized.substring(4);
        }
        return normalized;
    }

    private String getLoadedWorldNames() {
        if (plugin.getServer().getWorlds().isEmpty()) {
            return "(none)";
        }
        return plugin.getServer().getWorlds().stream()
                .map(World::getName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /**
     * WorldGuard の RegionManager から ProtectedRegion を取得します。
     *
     * @return 見つかった場合は ProtectedRegion、ワールドまたはRegionが存在しない場合は null
     */
    private ProtectedRegion resolveWgRegion(World world, String regionId) {
        try {
            RegionManager manager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (manager == null) {
                log.warning("[MobRegionConfig] WorldGuard の RegionManager が null です（world=" + world.getName() + "）。"
                        + " WorldGuard がワールドを管理しているか確認してください。");
                return null;
            }

            return manager.getRegion(regionId);
        } catch (Exception e) {
            log.severe("[MobRegionConfig] WorldGuard API の呼び出しに失敗しました: " + e.getMessage());
            return null;
        }
    }
}

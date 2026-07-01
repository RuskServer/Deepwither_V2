package com.ruskserver.deepwither_V2.modules.dungeon.room;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinition;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.DungeonDefinitionRegistry;
import com.ruskserver.deepwither_V2.modules.dungeon.definition.RoomSlot;
import com.ruskserver.deepwither_V2.modules.dungeon.schematic.RoomSchematic;
import com.ruskserver.deepwither_V2.modules.dungeon.schematic.SchematicLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 全ダンジョンのルームテンプレートを管理するサービス。
 * <p>
 * 起動時に {@link DungeonDefinitionRegistry} から全定義を読み取り、
 * 各定義のスケマティックフォルダからルームテンプレートを読み込みます。
 * <p>
 * テンプレートは "ダンジョンID:schematicName" のキーで管理されます。
 */
@Service
public class RoomTemplateRegistry implements Startable {

    private final JavaPlugin plugin;
    private final SchematicLoader schematicLoader;
    private final DungeonDefinitionRegistry definitionRegistry;
    private final Logger log;

    private final Map<String, RoomSchematic> templates = new HashMap<>();

    @Inject
    public RoomTemplateRegistry(
            JavaPlugin plugin,
            SchematicLoader schematicLoader,
            DungeonDefinitionRegistry definitionRegistry
    ) {
        this.plugin = plugin;
        this.schematicLoader = schematicLoader;
        this.definitionRegistry = definitionRegistry;
        this.log = plugin.getLogger();
    }

    @Override
    public void start() {
        for (DungeonDefinition def : definitionRegistry.getAll()) {
            loadTemplatesForDungeon(def);
        }
        log.info("[RoomTemplateRegistry] " + templates.size() + " 個のルームテンプレートを読み込みました。");
    }

    /**
     * 指定ダンジョンのルームテンプレートを読み込みます。
     */
    private void loadTemplatesForDungeon(DungeonDefinition def) {
        for (RoomSlot slot : def.roomSlots()) {
            String key = def.id() + ":" + slot.schematicName();
            if (templates.containsKey(key)) {
                continue; // 既に読み込み済み
            }

            RoomSchematic schematic = schematicLoader.loadFromDungeonFolder(
                    def.schematicFolder(), def.id(), slot.schematicName()
            );

            if (schematic != null) {
                templates.put(key, schematic);
                log.info("[RoomTemplateRegistry] 読み込み: " + key
                        + " (entryDoors=" + schematic.entryDoors().size()
                        + ", exitDoors=" + schematic.exitDoors().size() + ")");
            } else {
                log.warning("[RoomTemplateRegistry] テンプレート読み込み失敗: " + key);
            }
        }

        // ボスルームも読み込み
        if (def.hasBossRoom()) {
            String bossKey = def.id() + ":" + def.bossRoomSchematic();
            if (!templates.containsKey(bossKey)) {
                RoomSchematic schematic = schematicLoader.loadFromDungeonFolder(
                        def.schematicFolder(), def.id(), def.bossRoomSchematic()
                );
                if (schematic != null) {
                    templates.put(bossKey, schematic);
                } else {
                    log.warning("[RoomTemplateRegistry] ボスルーム読み込み失敗: " + bossKey);
                }
            }
        }
    }

    /**
     * 指定キーのルームテンプレートを返します。
     *
     * @param key "ダンジョンID:schematicName"
     * @return テンプレート。見つからない場合は null
     */
    public RoomSchematic getTemplate(String key) {
        return templates.get(key);
    }

    /**
     * 指定ダンジョンのルームスロットに対応するテンプレートを返します。
     *
     * @param dungeonId ダンジョン識別子
     * @param slot      ルームスロット
     * @return テンプレート。見つからない場合は null
     */
    public RoomSchematic getTemplateForSlot(String dungeonId, RoomSlot slot) {
        return templates.get(dungeonId + ":" + slot.schematicName());
    }

    /**
     * 指定ダンジョンのボスルームテンプレートを返します。
     */
    public RoomSchematic getBossTemplate(DungeonDefinition def) {
        if (!def.hasBossRoom()) return null;
        return templates.get(def.id() + ":" + def.bossRoomSchematic());
    }

    /**
     * 全テンプレート数を返します。
     */
    public int getTemplateCount() {
        return templates.size();
    }
}

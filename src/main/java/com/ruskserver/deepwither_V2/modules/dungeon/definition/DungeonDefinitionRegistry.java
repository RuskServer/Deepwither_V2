package com.ruskserver.deepwither_V2.modules.dungeon.definition;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 全ダンジョン定義を管理するレジストリ。
 * <p>
 * 起動時にハードコーディングされたダンジョン定義を登録します。
 * 新規ダンジョンを追加する場合は {@link #start()} メソッドに new XxxDungeon() を追加してください。
 */
@Service
public class DungeonDefinitionRegistry implements Startable {

    private final JavaPlugin plugin;
    private final Logger log;

    private final Map<String, DungeonDefinition> definitions = new HashMap<>();

    @Inject
    public DungeonDefinitionRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    @Override
    public void start() {
        // --- ダンジョン定義をここに登録 ---
        register(new SimpleDungeon());
        register(new BranchDungeon());
        register(new RaidDungeon());
        register(new EternalIceDungeon());

        log.info("[DungeonDefinitionRegistry] " + definitions.size() + " 個のダンジョン定義を登録しました。");
    }

    /**
     * ダンジョン定義を登録します。
     */
    private void register(DungeonDefinition def) {
        if (definitions.containsKey(def.id())) {
            log.warning("[DungeonDefinitionRegistry] ダンジョンID '" + def.id() + "' は既に登録されています。上書きします。");
        }
        definitions.put(def.id(), def);
        log.fine("[DungeonDefinitionRegistry] 登録: " + def);
    }

    /**
     * 指定IDのダンジョン定義を返します。
     *
     * @return 定義。見つからない場合は null
     */
    public DungeonDefinition get(String id) {
        return definitions.get(id);
    }

    /**
     * 全ダンジョン定義を返します（読み取り専用）。
     */
    public Collection<DungeonDefinition> getAll() {
        return definitions.values();
    }

    /**
     * 登録されているダンジョン定義数を返します。
     */
    public int getCount() {
        return definitions.size();
    }
}

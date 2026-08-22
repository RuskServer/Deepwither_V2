package com.ruskserver.deepwither_V2.modules.dialogue.api;

import java.util.List;

/**
 * 会話定義インターフェース。
 * @Component を付けて実装すると、DialogueService の start() で自動収集されます。
 * NPC名をキーに右クリックで自動起動する本格的な会話に向きます。
 */
public interface Dialogue {

    default String getNpcName() {
        return null;
    }

    default List<String> getNpcNames() {
        String name = getNpcName();
        return name != null ? List.of(name) : List.of();
    }

    DialogueGraph getGraph();
}

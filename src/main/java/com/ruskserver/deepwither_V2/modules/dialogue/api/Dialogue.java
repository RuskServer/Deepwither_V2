package com.ruskserver.deepwither_V2.modules.dialogue.api;

/**
 * 会話定義インターフェース。
 * @Component を付けて実装すると、DialogueService の start() で自動収集されます。
 * NPC名をキーに右クリックで自動起動する本格的な会話に向きます。
 * 軽量な会話は DialogueService#startDialogue() に DialogueGraph を直接渡してください。
 */
public interface Dialogue {
    String getNpcName();
    DialogueGraph getGraph();
}

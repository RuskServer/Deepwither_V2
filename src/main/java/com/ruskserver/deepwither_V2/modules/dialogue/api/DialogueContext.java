package com.ruskserver.deepwither_V2.modules.dialogue.api;

import org.bukkit.entity.Player;
import java.util.Map;

public class DialogueContext {
    private final Player player;
    private final Map<String, String> flags;
    private boolean ended;

    public DialogueContext(Player player, Map<String, String> flags) {
        this.player = player;
        this.flags = flags;
    }

    public Player player() {
        return player;
    }

    public void setFlag(String key, String value) {
        flags.put(key, value);
    }

    public String getFlag(String key) {
        return flags.get(key);
    }

    public boolean hasFlag(String key) {
        return flags.containsKey(key);
    }

    public void endDialogue() {
        this.ended = true;
    }

    public boolean isEnded() {
        return ended;
    }
}

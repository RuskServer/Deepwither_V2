package com.ruskserver.deepwither_V2.modules.gui;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.party.PartyGUI;
import com.ruskserver.deepwither_V2.modules.skill.gui.SkillAssignmentGui;
import com.ruskserver.deepwither_V2.modules.skilltree.gui.SkillTreeGui;
import org.bukkit.entity.Player;

@Service
public class LegacyGuiBridge {

    private final SkillTreeGui skillTreeGui;
    private final SkillAssignmentGui skillAssignmentGui;
    private final PartyGUI partyGUI;

    @Inject
    public LegacyGuiBridge(SkillTreeGui skillTreeGui, SkillAssignmentGui skillAssignmentGui, PartyGUI partyGUI) {
        this.skillTreeGui = skillTreeGui;
        this.skillAssignmentGui = skillAssignmentGui;
        this.partyGUI = partyGUI;
    }

    public boolean open(Player player, String guiId) {
        switch (guiId) {
            case "skilltree" -> skillTreeGui.openSelectorOrFirst(player);
            case "skill_assignment" -> skillAssignmentGui.open(player);
            case "party" -> partyGUI.open(player);
            default -> {
                return false;
            }
        }
        return true;
    }
}

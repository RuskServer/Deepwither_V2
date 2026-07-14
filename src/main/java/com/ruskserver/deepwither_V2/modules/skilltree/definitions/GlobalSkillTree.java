package com.ruskserver.deepwither_V2.modules.skilltree.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@Component
public class GlobalSkillTree implements SkillTreeDefinition {

    private final StatManager statManager;

    @Inject
    public GlobalSkillTree(StatManager statManager) {
        this.statManager = statManager;
    }

    @Override
    public String getId() {
        return "global";
    }

    @Override
    public String getDisplayName() {
        return "アトラスツリー";
    }

    @Override
    public Material getIcon() {
        return Material.NETHER_STAR;
    }

    @Override
    public List<SkillTreeNode> getNodes() {
        List<SkillTreeNode> nodes = new ArrayList<>();
        
        nodes.addAll(GlobalWarriorNodes.getNodes(statManager));
        nodes.addAll(GlobalMageNodes.getNodes(statManager));
        nodes.addAll(GlobalArcherNodes.getNodes(statManager));
        nodes.addAll(GlobalHolyNodes.getNodes(statManager));
        
        return nodes;
    }
}

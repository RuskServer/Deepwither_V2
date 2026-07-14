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

        // ========== コネクトノード (遠征ブリッジ) ==========
        nodes.add(SkillTreeNode.passive("connect_holy_warrior")
                .name("信仰の盾")
                .description("【接続】神聖ツリーと戦士ツリーを繋ぐ。")
                .icon(Material.LEAD)
                .position(4, -1)
                .maxLevel(1).costPerLevel(1)
                .build());

        nodes.add(SkillTreeNode.passive("connect_warrior_archer")
                .name("狩人の歩法")
                .description("【接続】戦士ツリーと弓使いツリーを繋ぐ。")
                .icon(Material.LEAD)
                .position(4, 5)
                .maxLevel(1).costPerLevel(1)
                .build());

        nodes.add(SkillTreeNode.passive("connect_archer_mage")
                .name("魔力矢")
                .description("【接続】弓使いツリーと魔術師ツリーを繋ぐ。")
                .icon(Material.LEAD)
                .position(4, 9)
                .maxLevel(1).costPerLevel(1)
                .build());
        
        return nodes;
    }
}

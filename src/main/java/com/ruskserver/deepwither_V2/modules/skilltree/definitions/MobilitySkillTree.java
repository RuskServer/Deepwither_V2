package com.ruskserver.deepwither_V2.modules.skilltree.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import org.bukkit.Material;

import java.util.List;

@Component
public class MobilitySkillTree implements SkillTreeDefinition {

    @Override
    public String getId() {
        return "mobility";
    }

    @Override
    public String getDisplayName() {
        return "機動";
    }

    @Override
    public Material getIcon() {
        return Material.ELYTRA;
    }

    @Override
    public List<SkillTreeNode> getNodes() {
        return List.of(
                SkillTreeNode.skill("mobility_evade_node", "evade")
                        .name("回避")
                        .description("入力している移動方向へ素早く身をかわす。")
                        .icon(Material.FEATHER)
                        .position(1, 2)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.skill("mobility_charge_node", "charge")
                        .name("突撃")
                        .description("視線方向へ勢いよく突撃する。")
                        .icon(Material.RABBIT_FOOT)
                        .position(3, 2)
                        .requires("mobility_evade_node")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.skill("mobility_blink_node", "blink")
                        .name("瞬間移動")
                        .description("視線方向の安全な地点へ瞬間移動する。")
                        .icon(Material.ENDER_PEARL)
                        .position(5, 2)
                        .requires("mobility_charge_node")
                        .maxLevel(1)
                        .costPerLevel(4)
                        .build()
        );
    }
}

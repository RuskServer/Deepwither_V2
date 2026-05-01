package com.ruskserver.deepwither_V2.modules.skilltree.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Material;

import java.util.List;

@Component
public class MageSkillTree implements SkillTreeDefinition {

    private static final String ARCANE_FOCUS_SOURCE = "skilltree_arcane_focus";

    private final StatManager statManager;

    @Inject
    public MageSkillTree(StatManager statManager) {
        this.statManager = statManager;
    }

    @Override
    public String getId() {
        return "mage";
    }

    @Override
    public String getDisplayName() {
        return "魔術師";
    }

    @Override
    public Material getIcon() {
        return Material.AMETHYST_SHARD;
    }

    @Override
    public List<SkillTreeNode> getNodes() {
        return List.of(
                SkillTreeNode.skill("arcane_bolt_node", "arcane_bolt")
                        .name("アーケインボルト")
                        .description("前方へ魔力弾を放つ基本攻撃スキル。")
                        .icon(Material.AMETHYST_SHARD)
                        .position(4, 2)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("arcane_focus")
                        .name("魔力集中")
                        .description("魔法攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.ENCHANTED_BOOK)
                        .position(6, 1)
                        .requires("arcane_bolt_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreePassiveEffect() {
                            @Override
                            public void apply(org.bukkit.entity.Player player, int level, com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, ARCANE_FOCUS_SOURCE, level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(org.bukkit.entity.Player player, com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, ARCANE_FOCUS_SOURCE);
                            }
                        })
                        .build(),

                SkillTreeNode.skill("first_aid_node", "first_aid")
                        .name("応急手当")
                        .description("短い詠唱の後、自分を回復する補助スキル。")
                        .icon(Material.HONEY_BOTTLE)
                        .position(7, 3)
                        .requires("arcane_bolt_node")
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build()
        );
    }
}

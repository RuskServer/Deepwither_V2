package com.ruskserver.deepwither_V2.modules.skilltree.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeContext;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreePassiveEffect;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

@Component
public class MageSkillTree implements SkillTreeDefinition {

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
        return "魔術師 (火)";
    }

    @Override
    public Material getIcon() {
        return Material.FIRE_CHARGE;
    }

    @Override
    public List<SkillTreeNode> getNodes() {
        return List.of(
                // Skill 1: Fireball
                SkillTreeNode.skill("fireball_node", "fireball")
                        .name("ファイアボール")
                        .description("火球を放ち、爆発ダメージを与える。")
                        .icon(Material.FIRE_CHARGE)
                        .position(4, 1)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                // Passive 1: Fire Power I
                SkillTreeNode.passive("fire_power_1")
                        .name("火の心得 I")
                        .description("魔法攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.BLAZE_POWDER)
                        .position(4, 2)
                        .requires("fireball_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_fire_power_1", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_fire_power_1");
                            }
                        })
                        .build(),

                // Skill 2: Flame Pillar
                SkillTreeNode.skill("flame_pillar_node", "flame_pillar")
                        .name("フレイムピラー")
                        .description("足元から火柱を噴出させる。")
                        .icon(Material.BLAZE_ROD)
                        .position(4, 3)
                        .requires("fire_power_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                // Passive 2: Mana Flow
                SkillTreeNode.passive("mana_flow")
                        .name("魔力の流れ")
                        .description("最大マナをレベルごとに10%上昇させる。")
                        .icon(Material.LAPIS_LAZULI)
                        .position(4, 4)
                        .requires("flame_pillar_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_mana_flow", level * 0.10, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_mana_flow");
                            }
                        })
                        .build(),

                // Skill 3: Flame Breath
                SkillTreeNode.skill("flame_breath_node", "flame_breath")
                        .name("フレイムブレス")
                        .description("前方広範囲に炎を吹き付ける。")
                        .icon(Material.MAGMA_CREAM)
                        .position(4, 5)
                        .requires("mana_flow")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                // Passive 3: Fire Power II
                SkillTreeNode.passive("fire_power_2")
                        .name("火の心得 II")
                        .description("魔法攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.FIREWORK_STAR)
                        .position(4, 6)
                        .requires("flame_breath_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_fire_power_2", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_fire_power_2");
                            }
                        })
                        .build(),

                // Skill 4: Fire Nova
                SkillTreeNode.skill("fire_nova_node", "fire_nova")
                        .name("ファイアノヴァ")
                        .description("周囲を焼き尽くす衝撃波。")
                        .icon(Material.NETHER_STAR)
                        .position(4, 7)
                        .requires("fire_power_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build()
        );
    }
}

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
public class HolySkillTree implements SkillTreeDefinition {

    private final StatManager statManager;

    @Inject
    public HolySkillTree(StatManager statManager) {
        this.statManager = statManager;
    }

    @Override
    public String getId() { return "holy"; }

    @Override
    public String getDisplayName() { return "聖なる魔法"; }

    @Override
    public Material getIcon() { return Material.END_CRYSTAL; }

    @Override
    public List<SkillTreeNode> getNodes() {
        return List.of(
                // ========== RESTORATION BRANCH (Y=1) ==========

                SkillTreeNode.skill("holy_light_node", "holy_light")
                        .name("聖光")
                        .description("対象1体の最大HPの25%を回復する。")
                        .icon(Material.GLOWSTONE_DUST)
                        .position(0, 1)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("holy_mastery_1")
                        .name("癒しの糧 I")
                        .description("回復量をレベルごとに5%上昇させる。")
                        .icon(Material.SUGAR)
                        .position(1, 1)
                        .requires("holy_light_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_mastery_1", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_mastery_1");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("renewal_node", "renewal")
                        .name("再生")
                        .description("対象に6秒間の継続回復を付与する。")
                        .icon(Material.HONEY_BOTTLE)
                        .position(2, 1)
                        .requires("holy_mastery_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("holy_aura")
                        .name("祝福の光")
                        .description("範囲回復スキルの半径をレベルごとに0.5m増加させる。")
                        .icon(Material.LIGHT)
                        .position(3, 1)
                        .requires("renewal_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                            }
                        })
                        .build(),

                SkillTreeNode.skill("sanctuary_node", "sanctuary")
                        .name("聖域")
                        .description("設置した範囲内の味方を継続回復する。")
                        .icon(Material.LIGHT)
                        .position(4, 1)
                        .requires("holy_aura")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("holy_mastery_2")
                        .name("癒しの糧 II")
                        .description("回復量をレベルごとに8%上昇させる。")
                        .icon(Material.GLISTERING_MELON_SLICE)
                        .position(5, 1)
                        .requires("sanctuary_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_mastery_2", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_mastery_2");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("mass_heal_node", "mass_heal")
                        .name("大聖光")
                        .description("周囲の味方全員を大きく回復する。")
                        .icon(Material.NETHER_STAR)
                        .position(6, 1)
                        .requires("holy_mastery_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== PROTECTION BRANCH (Y=2) ==========

                SkillTreeNode.skill("purify_node", "purify")
                        .name("浄化")
                        .description("自身のデバフを解除し耐性を付与する。")
                        .icon(Material.MILK_BUCKET)
                        .position(0, 2)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("holy_defense_1")
                        .name("聖なる守り")
                        .description("魔法防御力をレベルごとに5%上昇させる。")
                        .icon(Material.ENDER_EYE)
                        .position(1, 2)
                        .requires("purify_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_holy_defense_1", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_holy_defense_1");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("divine_shield_node", "divine_shield")
                        .name("聖盾")
                        .description("自身にダメージ吸収バリアを付与する。")
                        .icon(Material.SHIELD)
                        .position(2, 2)
                        .requires("holy_defense_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("holy_defense_2")
                        .name("不屈の意思")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.OBSIDIAN)
                        .position(3, 2)
                        .requires("divine_shield_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_holy_defense_2", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_holy_defense_2");
                            }
                        })
                        .build(),

                SkillTreeNode.passive("holy_cdr")
                        .name("神の加護")
                        .description("クールタイム短縮をレベルごとに5%上昇させる。")
                        .icon(Material.EXPERIENCE_BOTTLE)
                        .position(4, 2)
                        .requires("holy_defense_2")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_holy_cdr", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_holy_cdr");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("guardian_angel_node", "guardian_angel")
                        .name("守護天使")
                        .description("周囲の味方にバリアを付与する。")
                        .icon(Material.TOTEM_OF_UNDYING)
                        .position(6, 2)
                        .requires("holy_cdr")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== SACRIFICE BRANCH (Y=3) ==========

                SkillTreeNode.skill("sacrificial_light_node", "sacrificial_light")
                        .name("献身の光")
                        .description("自分のHPを消費し対象を大きく回復する。")
                        .icon(Material.BLAZE_POWDER)
                        .position(0, 3)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("sacrifice_mastery_1")
                        .name("痛みの代償")
                        .description("HP消費スキルの回復量をレベルごとに7%上昇させる。")
                        .icon(Material.REDSTONE)
                        .position(1, 3)
                        .requires("sacrificial_light_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                            }
                        })
                        .build(),

                SkillTreeNode.skill("martyrdom_node", "martyrdom")
                        .name("殉教")
                        .description("一時的に範囲内味方の被ダメージを肩代わりする。")
                        .icon(Material.NETHER_STAR)
                        .position(2, 3)
                        .requires("sacrifice_mastery_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("sacrifice_endurance")
                        .name("忍耐")
                        .description("最大HPをレベルごとに8%上昇させる。")
                        .icon(Material.GOLDEN_APPLE)
                        .position(3, 3)
                        .requires("martyrdom_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_sacrifice_endurance", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_sacrifice_endurance");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("holy_resurrection_node", "holy_resurrection")
                        .name("聖なる復活")
                        .description("自らのHPを消費し遠くの味方を蘇生する。")
                        .icon(Material.TOTEM_OF_UNDYING)
                        .position(4, 3)
                        .requires("sacrifice_endurance")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("sacrifice_mastery_2")
                        .name("聖なる覚悟")
                        .description("HP50%以下で回復スキルの効果をレベルごとに5%増加させる。")
                        .icon(Material.BLAZE_POWDER)
                        .position(5, 3)
                        .requires("holy_resurrection_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                            }
                        })
                        .build()

                // (6,3) is reserved if needed for a future Sacrifice ultimate skill
        );
    }
}

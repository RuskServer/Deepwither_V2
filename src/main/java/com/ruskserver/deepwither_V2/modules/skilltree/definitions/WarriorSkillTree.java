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
public class WarriorSkillTree implements SkillTreeDefinition {

    private final StatManager statManager;

    @Inject
    public WarriorSkillTree(StatManager statManager) {
        this.statManager = statManager;
    }

    @Override
    public String getId() {
        return "warrior";
    }

    @Override
    public String getDisplayName() {
        return "戦士";
    }

    @Override
    public Material getIcon() {
        return Material.IRON_SWORD;
    }

    @Override
    public List<SkillTreeNode> getNodes() {
        return List.of(
                // ========== DEFENSE BRANCH (Y=1) ==========

                SkillTreeNode.skill("taunt_node", "taunt")
                        .name("挑発")
                        .description("周囲の敵を挑発し、ダメージを与える。")
                        .icon(Material.IRON_CHESTPLATE)
                        .position(0, 1)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("toughness_1")
                        .name("堅牢 I")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.LEATHER_CHESTPLATE)
                        .position(1, 1)
                        .requires("taunt_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_toughness_1", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_toughness_1");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("shield_wall_node", "shield_wall")
                        .name("シールドウォール")
                        .description("防御姿勢をとり、受けるダメージを減少させる。")
                        .icon(Material.SHIELD)
                        .position(2, 1)
                        .requires("toughness_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("vitality_boost")
                        .name("活力")
                        .description("最大HPをレベルごとに8%上昇させる。")
                        .icon(Material.GOLDEN_APPLE)
                        .position(3, 1)
                        .requires("shield_wall_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_vitality", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_vitality");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("battle_cry_node", "battle_cry")
                        .name("バトルクライ")
                        .description("雄叫びで味方を回復し攻撃力を強化する。")
                        .icon(Material.GOAT_HORN)
                        .position(4, 1)
                        .requires("vitality_boost")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("toughness_2")
                        .name("堅牢 II")
                        .description("防御力をレベルごとに8%上昇させる。")
                        .icon(Material.CHAINMAIL_CHESTPLATE)
                        .position(5, 1)
                        .requires("battle_cry_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_toughness_2", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_toughness_2");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("fortress_node", "fortress")
                        .name("フォートレス")
                        .description("大地を踏み鳴らし強力な防御バリアを張る。")
                        .icon(Material.ANVIL)
                        .position(6, 1)
                        .requires("toughness_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== TECHNIQUE BRANCH (Y=2) ==========

                SkillTreeNode.skill("power_strike_node", "power_strike")
                        .name("パワーストライク")
                        .description("目の前の敵1体に強力な一撃を与える。")
                        .icon(Material.IRON_SWORD)
                        .position(0, 2)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("technique_1")
                        .name("武技 I")
                        .description("攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.LEATHER)
                        .position(1, 2)
                        .requires("power_strike_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_technique_1", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_technique_1");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("multi_slash_node", "multi_slash")
                        .name("連撃")
                        .description("前方に3連続の斬撃を繰り出す。")
                        .icon(Material.DIAMOND_SWORD)
                        .position(2, 2)
                        .requires("technique_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("precision")
                        .name("精密")
                        .description("クリティカル率をレベルごとに5%上昇させる。")
                        .icon(Material.ARROW)
                        .position(3, 2)
                        .requires("multi_slash_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_precision", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_precision");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("whirlwind_node", "whirlwind")
                        .name("旋風斬り")
                        .description("その場で回転斬りを放つ。")
                        .icon(Material.IRON_SWORD)
                        .position(4, 2)
                        .requires("precision")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("technique_2")
                        .name("武技 II")
                        .description("攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.RABBIT_HIDE)
                        .position(5, 2)
                        .requires("whirlwind_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_technique_2", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_technique_2");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("executioner_node", "executioner")
                        .name("エクセキューショナー")
                        .description("敵の急所を突く。HP半減以下で追加ダメージ。")
                        .icon(Material.NETHERITE_SWORD)
                        .position(6, 2)
                        .requires("technique_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== HEAVY BRANCH (Y=3) ==========

                SkillTreeNode.skill("hammer_slam_node", "hammer_slam")
                        .name("ハンマースラム")
                        .description("地面を叩きつけノックバックを与える。")
                        .icon(Material.ANVIL)
                        .position(0, 3)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("impact_1")
                        .name("重撃 I")
                        .description("攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.STONE)
                        .position(1, 3)
                        .requires("hammer_slam_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_impact_1", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_impact_1");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("shockwave_node", "shockwave")
                        .name("衝撃波")
                        .description("前方に衝撃波を放ちノックバックさせる。")
                        .icon(Material.HEART_OF_THE_SEA)
                        .position(2, 3)
                        .requires("impact_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("sturdy")
                        .name("踏ん張り")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.OBSIDIAN)
                        .position(3, 3)
                        .requires("shockwave_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_sturdy", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_sturdy");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("seismic_stomp_node", "seismic_stomp")
                        .name("サイズミックストンプ")
                        .description("地面を踏み鳴らし、鈍足効果を与える。")
                        .icon(Material.IRON_BOOTS)
                        .position(4, 3)
                        .requires("sturdy")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("impact_2")
                        .name("重撃 II")
                        .description("攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.OAK_LOG)
                        .position(5, 3)
                        .requires("seismic_stomp_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_impact_2", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_impact_2");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("colossus_node", "colossus")
                        .name("コロッサス")
                        .description("全身全霊の一撃を放つ。")
                        .icon(Material.NETHER_STAR)
                        .position(6, 3)
                        .requires("impact_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build()
        );
    }
}

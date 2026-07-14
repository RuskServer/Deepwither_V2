package com.ruskserver.deepwither_V2.modules.skilltree.definitions;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeContext;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreePassiveEffect;
import com.ruskserver.deepwither_V2.modules.stat.ModifierType;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class GlobalWarriorNodes {

    public static List<SkillTreeNode> getNodes(StatManager statManager) {
        return List.of(
                // ========== WARRIOR START ==========
                SkillTreeNode.passive("warrior_start")
                        .name("戦士の道")
                        .description("戦士のクラスを選択する。最大HPが10%増加する。")
                        .icon(Material.IRON_SWORD)
                        .position(0, 2)
                        .maxLevel(1).costPerLevel(0)
                        .conflicts("mage_start", "archer_start", "holy_start")
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_class_warrior", 0.1, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_class_warrior"); }
                        }).build(),

                // ========== MOBILITY ==========
                SkillTreeNode.skill("mobility_charge_node", "charge")
                        .name("突撃")
                        .description("視線方向へ勢いよく突撃する。")
                        .icon(Material.RABBIT_FOOT)
                        .position(1, 2)
                        .requires("warrior_start")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                // ========== DEFENSE (Y = 0) ==========
                SkillTreeNode.skill("taunt_node", "taunt")
                        .name("挑発")
                        .description("周囲の敵を挑発し、ダメージを与える。")
                        .icon(Material.IRON_CHESTPLATE)
                        .position(2, 0)
                        .requires("mobility_charge_node")
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("toughness_1")
                        .name("堅牢 I")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.LEATHER_CHESTPLATE)
                        .position(3, 0)
                        .requires("taunt_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_toughness_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_toughness_1"); }
                        }).build(),

                SkillTreeNode.skill("shield_wall_node", "shield_wall")
                        .name("シールドウォール")
                        .description("防御姿勢をとり、受けるダメージを減少させる。")
                        .icon(Material.SHIELD)
                        .position(4, 0)
                        .requires("toughness_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("vitality_boost")
                        .name("活力")
                        .description("最大HPをレベルごとに8%上昇させる。")
                        .icon(Material.GOLDEN_APPLE)
                        .position(5, 0)
                        .requiresAny("shield_wall_node", "connect_holy_warrior")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_vitality", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_vitality"); }
                        }).build(),

                SkillTreeNode.skill("battle_cry_node", "battle_cry")
                        .name("バトルクライ")
                        .description("雄叫びで味方を回復し攻撃力を強化する。")
                        .icon(Material.GOAT_HORN)
                        .position(6, 0)
                        .requires("vitality_boost")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("toughness_2")
                        .name("堅牢 II")
                        .description("防御力をレベルごとに8%上昇させる。")
                        .icon(Material.CHAINMAIL_CHESTPLATE)
                        .position(7, 0)
                        .requires("battle_cry_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_toughness_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_toughness_2"); }
                        }).build(),

                SkillTreeNode.skill("fortress_node", "fortress")
                        .name("フォートレス")
                        .description("大地を踏み鳴らし強力な防御バリアを張る。")
                        .icon(Material.ANVIL)
                        .position(8, 0)
                        .requires("toughness_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== HEAVY (Y = 2) ==========
                SkillTreeNode.skill("hammer_slam_node", "hammer_slam")
                        .name("ハンマースラム")
                        .description("地面を叩きつけノックバックを与える。")
                        .icon(Material.ANVIL)
                        .position(2, 2)
                        .requires("mobility_charge_node")
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("impact_1")
                        .name("重撃 I")
                        .description("攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.STONE)
                        .position(3, 2)
                        .requires("hammer_slam_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_impact_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_impact_1"); }
                        }).build(),

                SkillTreeNode.skill("shockwave_node", "shockwave")
                        .name("衝撃波")
                        .description("前方に衝撃波を放ちノックバックさせる。")
                        .icon(Material.HEART_OF_THE_SEA)
                        .position(4, 2)
                        .requires("impact_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("sturdy")
                        .name("踏ん張り")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.OBSIDIAN)
                        .position(5, 2)
                        .requires("shockwave_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_sturdy", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_sturdy"); }
                        }).build(),

                SkillTreeNode.skill("seismic_stomp_node", "seismic_stomp")
                        .name("サイズミックストンプ")
                        .description("地面を踏み鳴らし、鈍足効果を与える。")
                        .icon(Material.IRON_BOOTS)
                        .position(6, 2)
                        .requires("sturdy")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("impact_2")
                        .name("重撃 II")
                        .description("攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.OAK_LOG)
                        .position(7, 2)
                        .requires("seismic_stomp_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_impact_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_impact_2"); }
                        }).build(),

                SkillTreeNode.skill("colossus_node", "colossus")
                        .name("コロッサス")
                        .description("全身全霊の一撃を放つ。")
                        .icon(Material.NETHER_STAR)
                        .position(8, 2)
                        .requires("impact_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== TECHNIQUE (Y = 4) ==========
                SkillTreeNode.skill("power_strike_node", "power_strike")
                        .name("パワーストライク")
                        .description("目の前の敵1体に強力な一撃を与える。")
                        .icon(Material.IRON_SWORD)
                        .position(2, 4)
                        .requires("mobility_charge_node")
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("technique_1")
                        .name("武技 I")
                        .description("攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.LEATHER)
                        .position(3, 4)
                        .requires("power_strike_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_technique_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_technique_1"); }
                        }).build(),

                SkillTreeNode.skill("multi_slash_node", "multi_slash")
                        .name("連撃")
                        .description("前方に3連続の斬撃を繰り出す。")
                        .icon(Material.DIAMOND_SWORD)
                        .position(4, 4)
                        .requires("technique_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("precision")
                        .name("精密")
                        .description("クリティカル率をレベルごとに5%上昇させる。")
                        .icon(Material.ARROW)
                        .position(5, 4)
                        .requiresAny("multi_slash_node", "connect_warrior_archer")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_precision", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_precision"); }
                        }).build(),

                SkillTreeNode.skill("whirlwind_node", "whirlwind")
                        .name("旋風斬り")
                        .description("その場で回転斬りを放つ。")
                        .icon(Material.IRON_SWORD)
                        .position(6, 4)
                        .requires("precision")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("technique_2")
                        .name("武技 II")
                        .description("攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.RABBIT_HIDE)
                        .position(7, 4)
                        .requires("whirlwind_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_technique_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_technique_2"); }
                        }).build(),

                SkillTreeNode.skill("executioner_node", "executioner")
                        .name("エクセキューショナー")
                        .description("敵の急所を突く。HP半減以下で追加ダメージ。")
                        .icon(Material.NETHERITE_SWORD)
                        .position(8, 4)
                        .requires("technique_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build()
        );
    }
}

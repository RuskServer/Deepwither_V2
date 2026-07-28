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

                SkillTreeNode.skill("mobility_dash_node_warrior", "charge")
                        .name("突撃")
                        .description("前方に素早くダッシュする。")
                        .icon(Material.RABBIT_FOOT)
                        .position(1, 2)
                        .maxLevel(1).costPerLevel(2).build(),

                // ----- TRAVEL NODES (分岐用) -----
                SkillTreeNode.passive("warrior_travel_start_up1")
                        .name("戦士の歩み")
                        .description("物理防御力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 1)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_warrior_travel_u1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_warrior_travel_u1"); }
                        }).build(),
                SkillTreeNode.passive("warrior_travel_start_up2")
                        .name("戦士の歩み")
                        .description("物理防御力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 0)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_warrior_travel_u2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_warrior_travel_u2"); }
                        }).build(),

                SkillTreeNode.passive("warrior_travel_start_down1")
                        .name("戦士の歩み")
                        .description("物理攻撃力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 3)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_warrior_travel_d1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_warrior_travel_d1"); }
                        }).build(),
                SkillTreeNode.passive("warrior_travel_start_down2")
                        .name("戦士の歩み")
                        .description("物理攻撃力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 4)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_warrior_travel_d2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_warrior_travel_d2"); }
                        }).build(),

                // ========== DEFENSE (Y = 0) ==========
                SkillTreeNode.skill("shield_wall_node", "shield_wall")
                        .name("シールドウォール")
                        .description("一時的に受けるダメージを大幅に軽減する。")
                        .icon(Material.SHIELD)
                        .position(2, 0)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("iron_skin_1")
                        .name("鉄の皮膚 I")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.IRON_CHESTPLATE)
                        .position(3, 0)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_iron_skin_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_iron_skin_1"); }
                        }).build(),

                SkillTreeNode.skill("taunt_node", "taunt")
                        .name("挑発")
                        .description("周囲の敵の注意を自分に引きつける。")
                        .icon(Material.GOAT_HORN)
                        .position(4, 0)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("vitality_boost")
                        .name("活力")
                        .description("最大HPをレベルごとに8%上昇させる。")
                        .icon(Material.GOLDEN_APPLE)
                        .position(5, 0)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_vitality_boost", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_vitality_boost"); }
                        }).build(),

                SkillTreeNode.skill("counter_stance_node", "battle_cry")
                        .name("カウンタースタンス")
                        .description("一定時間、受けたダメージを敵に跳ね返す。")
                        .icon(Material.DIAMOND_CHESTPLATE)
                        .position(6, 0)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("iron_skin_2")
                        .name("鉄の皮膚 II")
                        .description("防御力をレベルごとに8%上昇させる。")
                        .icon(Material.NETHERITE_CHESTPLATE)
                        .position(7, 0)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_iron_skin_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_iron_skin_2"); }
                        }).build(),

                SkillTreeNode.skill("last_stand_node", "fortress")
                        .name("ラストスタンド")
                        .description("HPがゼロになっても一定時間倒れなくなる。")
                        .icon(Material.TOTEM_OF_UNDYING)
                        .position(8, 0)
                        .maxLevel(1).costPerLevel(5).build(),

                // ----- ブランチ間コネクト (Y=0 と Y=2、Y=2 と Y=4 を繋ぐ) -----
                SkillTreeNode.passive("warrior_travel_mid_1")
                        .name("戦士の導き")
                        .description("最大HPが1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(4, 1)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_warrior_travel_m1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_warrior_travel_m1"); }
                        }).build(),
                SkillTreeNode.passive("warrior_travel_mid_2")
                        .name("戦士の導き")
                        .description("物理攻撃力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(4, 3)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_warrior_travel_m2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_warrior_travel_m2"); }
                        }).build(),

                // ========== HEAVY (Y = 2) ==========
                SkillTreeNode.skill("heavy_strike_node", "hammer_slam")
                        .name("ヘビーストライク")
                        .description("強力な一撃を放ち、敵に大ダメージを与える。")
                        .icon(Material.IRON_AXE)
                        .position(2, 2)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("muscle_training_1")
                        .name("筋力トレーニング I")
                        .description("物理攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.ROTTEN_FLESH)
                        .position(3, 2)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_muscle_training_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_muscle_training_1"); }
                        }).build(),

                SkillTreeNode.skill("earthquake_node", "shockwave")
                        .name("アースクエイク")
                        .description("地面を叩き割り、周囲の敵を転倒させる。")
                        .icon(Material.DIRT)
                        .position(4, 2)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("toughness")
                        .name("タフネス")
                        .description("最大HPをレベルごとに5%上昇させる。")
                        .icon(Material.LEATHER_CHESTPLATE)
                        .position(5, 2)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_toughness", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_toughness"); }
                        }).build(),

                SkillTreeNode.skill("berserk_node", "seismic_stomp")
                        .name("バーサーク")
                        .description("防御力を犠牲にして攻撃力を大幅に上げる。")
                        .icon(Material.RED_DYE)
                        .position(6, 2)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("muscle_training_2")
                        .name("筋力トレーニング II")
                        .description("物理攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.COOKED_BEEF)
                        .position(7, 2)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_muscle_training_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_muscle_training_2"); }
                        }).build(),

                SkillTreeNode.skill("execute_node", "colossus")
                        .name("エクスキュート")
                        .description("HPが低い敵を即死させる一撃。")
                        .icon(Material.NETHERITE_AXE)
                        .position(8, 2)
                        .maxLevel(1).costPerLevel(5).build(),

                // ========== TECHNIQUE (Y = 4) ==========
                SkillTreeNode.skill("multi_slash_node", "multi_slash")
                        .name("マルチスラッシュ")
                        .description("目にも留まらぬ速さで連続切りを行う。")
                        .icon(Material.IRON_SWORD)
                        .position(2, 4)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("agility_1")
                        .name("敏捷性 I")
                        .description("攻撃速度をレベルごとに5%上昇させる。")
                        .icon(Material.SUGAR)
                        .position(3, 4)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_SPEED, "st_agility_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_SPEED, "st_agility_1"); }
                        }).build(),

                SkillTreeNode.skill("blade_dance_node", "whirlwind")
                        .name("ブレードダンス")
                        .description("剣の舞を踊り、周囲の敵を切り刻む。")
                        .icon(Material.DIAMOND_SWORD)
                        .position(4, 4)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("precision")
                        .name("精密")
                        .description("クリティカル率をレベルごとに5%上昇させる。")
                        .icon(Material.ARROW)
                        .position(5, 4)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_precision", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_precision"); }
                        }).build(),

                SkillTreeNode.skill("parry_node", "power_strike")
                        .name("パリィ")
                        .description("敵の攻撃を弾き返し、隙を作る。")
                        .icon(Material.IRON_INGOT)
                        .position(6, 4)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("agility_2")
                        .name("敏捷性 II")
                        .description("攻撃速度をレベルごとに8%上昇させる。")
                        .icon(Material.FEATHER)
                        .position(7, 4)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_SPEED, "st_agility_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_SPEED, "st_agility_2"); }
                        }).build(),

                SkillTreeNode.skill("phantom_strike_node", "executioner")
                        .name("ファントムストライク")
                        .description("幻影のようになり、敵の背後から致命傷を与える。")
                        .icon(Material.NETHERITE_SWORD)
                        .position(8, 4)
                        .maxLevel(1).costPerLevel(5).build()
        );
    }
}

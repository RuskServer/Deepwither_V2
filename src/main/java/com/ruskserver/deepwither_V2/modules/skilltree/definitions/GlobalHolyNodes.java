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

public class GlobalHolyNodes {

    public static List<SkillTreeNode> getNodes(StatManager statManager) {
        return List.of(
                // ========== HOLY START ==========
                SkillTreeNode.passive("holy_start")
                        .name("神聖の道")
                        .description("神聖のクラスを選択する。魔法防御力が10%増加する。")
                        .icon(Material.GHAST_TEAR)
                        .position(0, -4)
                        .maxLevel(1).costPerLevel(0)
                        .conflicts("warrior_start", "archer_start", "mage_start")
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_class_holy", 0.1, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_class_holy"); }
                        }).build(),

                SkillTreeNode.skill("mobility_evade_node_holy", "evade")
                        .name("回避")
                        .description("移動方向へ素早くステップし攻撃を避ける。")
                        .icon(Material.FEATHER)
                        .position(1, -4)
                        .maxLevel(1).costPerLevel(2).build(),

                // ----- TRAVEL NODES (分岐用) -----
                SkillTreeNode.passive("holy_travel_start_up1")
                        .name("信仰の歩み")
                        .description("最大マナが1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, -5)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_holy_travel_u1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_holy_travel_u1"); }
                        }).build(),
                SkillTreeNode.passive("holy_travel_start_up2")
                        .name("信仰の歩み")
                        .description("最大マナが1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, -6)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_holy_travel_u2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_holy_travel_u2"); }
                        }).build(),

                SkillTreeNode.passive("holy_travel_start_down1")
                        .name("信仰の歩み")
                        .description("最大HPが1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, -3)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_travel_d1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_travel_d1"); }
                        }).build(),
                SkillTreeNode.passive("holy_travel_start_down2")
                        .name("信仰の歩み")
                        .description("最大HPが1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, -2)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_travel_d2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_travel_d2"); }
                        }).build(),

                // ========== RESTORATION (Y = -6) ==========
                SkillTreeNode.skill("heal_node", "holy_light")
                        .name("ヒール")
                        .description("自身のHPを回復する。")
                        .icon(Material.GOLDEN_APPLE)
                        .position(2, -6)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("restoration_1")
                        .name("治癒魔法 I")
                        .description("魔法攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.GLOWSTONE_DUST)
                        .position(3, -6)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_restoration_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_restoration_1"); }
                        }).build(),

                SkillTreeNode.skill("group_heal_node", "renewal")
                        .name("グループヒール")
                        .description("周囲の味方のHPを回復する。")
                        .icon(Material.ENCHANTED_GOLDEN_APPLE)
                        .position(4, -6)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("divine_grace")
                        .name("神の恩恵")
                        .description("最大HPをレベルごとに5%上昇させる。")
                        .icon(Material.TOTEM_OF_UNDYING)
                        .position(5, -6)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_divine_grace", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_divine_grace"); }
                        }).build(),

                SkillTreeNode.skill("resurrection_node", "holy_resurrection")
                        .name("リザレクション")
                        .description("倒れた味方を蘇生する。")
                        .icon(Material.NETHER_STAR)
                        .position(6, -6)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("restoration_2")
                        .name("治癒魔法 II")
                        .description("魔法攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.AMETHYST_SHARD)
                        .position(7, -6)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_restoration_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_restoration_2"); }
                        }).build(),

                SkillTreeNode.skill("holy_nova_node", "mass_heal")
                        .name("ホーリーノヴァ")
                        .description("周囲の敵にダメージを与え、味方を回復する。")
                        .icon(Material.BEACON)
                        .position(8, -6)
                        .maxLevel(1).costPerLevel(5).build(),

                // ----- ブランチ間コネクト (Y=-6 と Y=-4 を繋ぐ) -----
                SkillTreeNode.passive("holy_travel_mid_1")
                        .name("信仰の導き")
                        .description("最大マナが1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(4, -5)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_holy_travel_m1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_holy_travel_m1"); }
                        }).build(),
                SkillTreeNode.passive("holy_travel_mid_2")
                        .name("信仰の導き")
                        .description("最大HPが1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(4, -3)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_travel_m2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.HEALTH, "st_holy_travel_m2"); }
                        }).build(),

                // ========== SACRIFICE (Y = -4) ==========
                SkillTreeNode.skill("smite_node", "sacrificial_light")
                        .name("スマイト")
                        .description("対象に神聖な一撃を下す。")
                        .icon(Material.GOLDEN_SWORD)
                        .position(2, -4)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("holy_power_1")
                        .name("神聖力 I")
                        .description("魔法攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.BLAZE_POWDER)
                        .position(3, -4)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_holy_power_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_holy_power_1"); }
                        }).build(),

                SkillTreeNode.skill("martyr_node", "martyrdom")
                        .name("マーター")
                        .description("自身のHPを消費し、周囲の敵に大ダメージを与える。")
                        .icon(Material.CRIMSON_ROOTS)
                        .position(4, -4)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("zeal")
                        .name("狂信")
                        .description("攻撃速度をレベルごとに5%上昇させる。")
                        .icon(Material.REDSTONE)
                        .position(5, -4)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_SPEED, "st_zeal", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_SPEED, "st_zeal"); }
                        }).build(),

                SkillTreeNode.skill("judgement_node", "purify")
                        .name("ジャッジメント")
                        .description("天からの裁きを下し、広範囲の敵を浄化する。")
                        .icon(Material.END_CRYSTAL)
                        .position(6, -4)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("holy_power_2")
                        .name("神聖力 II")
                        .description("魔法攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.MAGMA_CREAM)
                        .position(7, -4)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_holy_power_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_holy_power_2"); }
                        }).build(),

                SkillTreeNode.skill("divine_wrath_node", "holy_nova")
                        .name("ディバインラース")
                        .description("神の怒りを顕現させ、全てを焼き尽くす。")
                        .icon(Material.DRAGON_BREATH)
                        .position(8, -4)
                        .maxLevel(1).costPerLevel(5).build(),

                // ========== PROTECTION (Y = -2) ==========
                SkillTreeNode.skill("divine_shield_node", "divine_shield")
                        .name("ディバインシールド")
                        .description("自身にダメージを防ぐバリアを張る。")
                        .icon(Material.SHIELD)
                        .position(2, -2)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("holy_defense_1")
                        .name("聖なる護り I")
                        .description("魔法防御力をレベルごとに5%上昇させる。")
                        .icon(Material.IRON_CHESTPLATE)
                        .position(3, -2)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_holy_defense_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_holy_defense_1"); }
                        }).build(),

                SkillTreeNode.skill("sanctuary_node", "sanctuary")
                        .name("サンクチュアリ")
                        .description("指定範囲に味方を守る聖域を展開する。")
                        .icon(Material.CAMPFIRE)
                        .position(4, -2)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("holy_defense_2")
                        .name("不屈の意思")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.OBSIDIAN)
                        .position(5, -2)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_holy_defense_2", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_holy_defense_2"); }
                        }).build(),

                SkillTreeNode.skill("aegis_node", "first_aid")
                        .name("イージス")
                        .description("一時的に無敵状態となる。")
                        .icon(Material.DIAMOND_CHESTPLATE)
                        .position(6, -2)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("holy_defense_3")
                        .name("聖なる護り II")
                        .description("魔法防御力をレベルごとに8%上昇させる。")
                        .icon(Material.NETHERITE_CHESTPLATE)
                        .position(7, -2)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_holy_defense_3", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_holy_defense_3"); }
                        }).build(),

                SkillTreeNode.skill("guardian_angel_node", "guardian_angel")
                        .name("ガーディアンエンジェル")
                        .description("対象の味方を死から一度だけ守る。")
                        .icon(Material.TOTEM_OF_UNDYING)
                        .position(8, -2)
                        .maxLevel(1).costPerLevel(5).build()
        );
    }
}

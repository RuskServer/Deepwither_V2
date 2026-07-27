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

public class GlobalArcherNodes {

    public static List<SkillTreeNode> getNodes(StatManager statManager) {
        return List.of(
                // ========== ARCHER START ==========
                SkillTreeNode.passive("archer_start")
                        .name("弓使いの道")
                        .description("弓使いのクラスを選択する。移動速度が10%増加する。")
                        .icon(Material.BOW)
                        .position(0, 7)
                        .maxLevel(1).costPerLevel(0)
                        .conflicts("warrior_start", "mage_start", "holy_start")
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.SPEED, "st_class_archer", 0.01, ModifierType.ADDITIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.SPEED, "st_class_archer"); }
                        }).build(),

                SkillTreeNode.skill("mobility_evade_node", "evade")
                        .name("回避")
                        .description("移動方向へ素早くステップし攻撃を避ける。")
                        .icon(Material.FEATHER)
                        .position(1, 7)
                        .maxLevel(1).costPerLevel(2).build(),

                // ----- TRAVEL NODES (分岐用) -----
                SkillTreeNode.passive("archer_travel_start_up1")
                        .name("狩人の歩み")
                        .description("攻撃速度が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 6)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_SPEED, "st_archer_travel_u1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_SPEED, "st_archer_travel_u1"); }
                        }).build(),
                SkillTreeNode.passive("archer_travel_start_down1")
                        .name("狩人の歩み")
                        .description("クリティカル率が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 8)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_archer_travel_d1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_archer_travel_d1"); }
                        }).build(),

                // ========== RAPID FIRE (Y = 6) ==========
                SkillTreeNode.skill("multi_shot_node", "multi_shot")
                        .name("マルチショット")
                        .description("前方に扇状に矢を放つ。")
                        .icon(Material.CROSSBOW)
                        .position(2, 6)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("rapid_fire_1")
                        .name("速射 I")
                        .description("クールタイム短縮をレベルごとに5%上昇させる。")
                        .icon(Material.SUGAR)
                        .position(3, 6)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_rapid_fire_1", level * 5.0, ModifierType.ADDITIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_rapid_fire_1"); }
                        }).build(),

                SkillTreeNode.skill("arrow_rain_node", "arrow_rain")
                        .name("アローレイン")
                        .description("指定範囲に矢の雨を降らせる。")
                        .icon(Material.ARROW)
                        .position(4, 6)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("nimble")
                        .name("身軽")
                        .description("移動速度をレベルごとに5%上昇させる。")
                        .icon(Material.RABBIT_FOOT)
                        .position(5, 6)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.SPEED, "st_nimble", level * 0.005, ModifierType.ADDITIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.SPEED, "st_nimble"); }
                        }).build(),

                SkillTreeNode.passive("rapid_fire_2")
                        .name("速射 II")
                        .description("クールタイム短縮をレベルごとに8%上昇させる。")
                        .icon(Material.FEATHER)
                        .position(6, 6)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_rapid_fire_2", level * 8.0, ModifierType.ADDITIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_rapid_fire_2"); }
                        }).build(),

                SkillTreeNode.skill("barrage_node", "barrage")
                        .name("バラージ")
                        .description("怒涛の勢いで連続射撃を行う。")
                        .icon(Material.FIREWORK_ROCKET)
                        .position(7, 6)
                        .maxLevel(1).costPerLevel(4).build(),

                // ----- ブランチ間コネクト (Y=6 と Y=8 を繋ぐ) -----
                SkillTreeNode.passive("archer_travel_mid_1")
                        .name("狩人の導き")
                        .description("移動速度が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(4, 7)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.SPEED, "st_archer_travel_m1", 0.001, ModifierType.ADDITIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.SPEED, "st_archer_travel_m1"); }
                        }).build(),

                // ========== SNIPER (Y = 8) ==========
                SkillTreeNode.skill("piercing_shot_node", "piercing_shot")
                        .name("ピアシングショット")
                        .description("敵を貫通する強力な矢を放つ。")
                        .icon(Material.SPECTRAL_ARROW)
                        .position(2, 8)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("sniper_1")
                        .name("狙撃 I")
                        .description("物理攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.FLINT)
                        .position(3, 8)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_sniper_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_sniper_1"); }
                        }).build(),

                SkillTreeNode.skill("heavy_draw_node", "heavy_draw")
                        .name("ヘビードロー")
                        .description("極めて重い一撃を放ち、敵を後退させる。")
                        .icon(Material.TIPPED_ARROW)
                        .position(4, 8)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("eagle_eye")
                        .name("鷹の目")
                        .description("クリティカルダメージをレベルごとに5%上昇させる。")
                        .icon(Material.SPYGLASS)
                        .position(5, 8)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.CRITICAL_DAMAGE, "st_eagle_eye", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.CRITICAL_DAMAGE, "st_eagle_eye"); }
                        }).build(),

                SkillTreeNode.passive("sniper_2")
                        .name("狙撃 II")
                        .description("物理攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.IRON_NUGGET)
                        .position(6, 8)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_sniper_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_sniper_2"); }
                        }).build(),

                SkillTreeNode.skill("headshot_node", "headshot")
                        .name("ヘッドショット")
                        .description("精神を集中させ致命的な一撃を放つ。")
                        .icon(Material.TARGET)
                        .position(7, 8)
                        .maxLevel(1).costPerLevel(5).build()
        );
    }
}

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

public class GlobalMageNodes {

    public static List<SkillTreeNode> getNodes(StatManager statManager) {
        return List.of(
                // ========== MAGE START ==========
                SkillTreeNode.passive("mage_start")
                        .name("魔術師の道")
                        .description("魔術師のクラスを選択する。最大マナが10%増加する。")
                        .icon(Material.BLAZE_ROD)
                        .position(0, 12)
                        .maxLevel(1).costPerLevel(0)
                        .conflicts("warrior_start", "archer_start", "holy_start")
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_class_mage", 0.1, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_class_mage"); }
                        }).build(),

                SkillTreeNode.skill("mobility_blink_node_mage", "blink")
                        .name("瞬間移動")
                        .description("視線方向の安全な地点へ瞬間移動する。")
                        .icon(Material.ENDER_PEARL)
                        .position(1, 12)
                        .maxLevel(1).costPerLevel(2).build(),

                // ----- TRAVEL NODES (分岐用) -----
                SkillTreeNode.passive("mage_travel_start_up1")
                        .name("魔術の歩み")
                        .description("魔法攻撃力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 11)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_mage_travel_u1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_mage_travel_u1"); }
                        }).build(),
                SkillTreeNode.passive("mage_travel_start_up2")
                        .name("魔術の歩み")
                        .description("魔法攻撃力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 10)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_mage_travel_u2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_mage_travel_u2"); }
                        }).build(),

                SkillTreeNode.passive("mage_travel_start_down1")
                        .name("魔術の歩み")
                        .description("魔法防御力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 13)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_mage_travel_d1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_mage_travel_d1"); }
                        }).build(),
                SkillTreeNode.passive("mage_travel_start_down2")
                        .name("魔術の歩み")
                        .description("魔法防御力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(1, 14)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_mage_travel_d2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_mage_travel_d2"); }
                        }).build(),

                // ========== LIGHTNING (Y = 10) ==========
                SkillTreeNode.skill("lightning_strike_node", "lightning_strike")
                        .name("ライトニングストライク")
                        .description("対象に雷を落としダメージを与える。")
                        .icon(Material.LIGHTNING_ROD)
                        .position(2, 10)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("mage_cdr_1")
                        .name("高速詠唱 I")
                        .description("クールタイム短縮をレベルごとに5%上昇させる。")
                        .icon(Material.FEATHER)
                        .position(3, 10)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_mage_cdr_1", level * 5.0, ModifierType.ADDITIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_mage_cdr_1"); }
                        }).build(),

                SkillTreeNode.skill("chain_lightning_node", "chain_lightning")
                        .name("チェインライトニング")
                        .description("敵から敵へと連鎖する雷を放つ。")
                        .icon(Material.END_ROD)
                        .position(4, 10)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("overload")
                        .name("オーバーロード")
                        .description("魔法攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.REDSTONE)
                        .position(5, 10)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_overload", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_overload"); }
                        }).build(),

                SkillTreeNode.skill("thunderstorm_node", "thunderstorm")
                        .name("サンダーストーム")
                        .description("指定範囲に激しい雷雨を呼び起こす。")
                        .icon(Material.DIAMOND_SWORD)
                        .position(6, 10)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("mage_cdr_2")
                        .name("高速詠唱 II")
                        .description("クールタイム短縮をレベルごとに8%上昇させる。")
                        .icon(Material.SUGAR)
                        .position(7, 10)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_mage_cdr_2", level * 8.0, ModifierType.ADDITIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_mage_cdr_2"); }
                        }).build(),

                SkillTreeNode.skill("emp_node", "emp")
                        .name("EMP")
                        .description("強力な電磁波で周囲の敵を沈黙させる。")
                        .icon(Material.BEACON)
                        .position(8, 10)
                        .maxLevel(1).costPerLevel(5).build(),

                // ----- ブランチ間コネクト (Y=10 と Y=12、Y=12 と Y=14 を繋ぐ) -----
                SkillTreeNode.passive("mage_travel_mid_1")
                        .name("魔術の導き")
                        .description("最大マナが1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(4, 11)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_mage_travel_m1", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_mage_travel_m1"); }
                        }).build(),
                SkillTreeNode.passive("mage_travel_mid_2")
                        .name("魔術の導き")
                        .description("魔法防御力が1%増加する。")
                        .icon(Material.IRON_NUGGET)
                        .position(4, 13)
                        .maxLevel(1).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_mage_travel_m2", 0.01, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_mage_travel_m2"); }
                        }).build(),

                // ========== FIRE (Y = 12) ==========
                SkillTreeNode.skill("fireball_node", "fireball")
                        .name("ファイアボール")
                        .description("着弾時に爆発する火球を放つ。")
                        .icon(Material.FIRE_CHARGE)
                        .position(2, 12)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("fire_mastery_1")
                        .name("火炎魔法 I")
                        .description("魔法攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.MAGMA_CREAM)
                        .position(3, 12)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_fire_mastery_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_fire_mastery_1"); }
                        }).build(),

                SkillTreeNode.skill("flame_breath_node", "flame_breath")
                        .name("フレイムブレス")
                        .description("前方に継続的な炎を放射する。")
                        .icon(Material.BLAZE_POWDER)
                        .position(4, 12)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("mana_boost")
                        .name("マナの泉")
                        .description("最大マナをレベルごとに8%上昇させる。")
                        .icon(Material.LAPIS_LAZULI)
                        .position(5, 12)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_mana_boost", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_mana_boost"); }
                        }).build(),

                SkillTreeNode.skill("meteor_node", "meteor")
                        .name("メテオ")
                        .description("巨大な隕石を落下させ大爆発を起こす。")
                        .icon(Material.NETHERRACK)
                        .position(6, 12)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("fire_mastery_2")
                        .name("火炎魔法 II")
                        .description("魔法攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.BLAZE_ROD)
                        .position(7, 12)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_fire_mastery_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DAMAGE, "st_fire_mastery_2"); }
                        }).build(),

                SkillTreeNode.skill("inferno_node", "inferno")
                        .name("インフェルノ")
                        .description("周囲一帯を業火で包み込む。")
                        .icon(Material.LAVA_BUCKET)
                        .position(8, 12)
                        .maxLevel(1).costPerLevel(5).build(),

                // ========== ICE (Y = 14) ==========
                SkillTreeNode.skill("ice_spike_node", "ice_spike")
                        .name("アイススパイク")
                        .description("地面から氷の棘を突き出させる。")
                        .icon(Material.ICE)
                        .position(2, 14)
                        .maxLevel(1).costPerLevel(1).build(),

                SkillTreeNode.passive("frost_mastery_1")
                        .name("氷結魔法 I")
                        .description("魔法防御力をレベルごとに5%上昇させる。")
                        .icon(Material.SNOWBALL)
                        .position(3, 14)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_frost_mastery_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_frost_mastery_1"); }
                        }).build(),

                SkillTreeNode.skill("blizzard_node", "blizzard")
                        .name("ブリザード")
                        .description("指定範囲に吹雪を起こし敵を遅くする。")
                        .icon(Material.PACKED_ICE)
                        .position(4, 14)
                        .maxLevel(1).costPerLevel(2).build(),

                SkillTreeNode.passive("ice_barrier")
                        .name("アイスバリア")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.BLUE_ICE)
                        .position(5, 14)
                        .maxLevel(3).costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_ice_barrier", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_ice_barrier"); }
                        }).build(),

                SkillTreeNode.skill("frost_nova_node", "frost_nova")
                        .name("フロストノヴァ")
                        .description("自身の周囲に氷の輪を放ち敵を凍結させる。")
                        .icon(Material.GHAST_TEAR)
                        .position(6, 14)
                        .maxLevel(1).costPerLevel(3).build(),

                SkillTreeNode.passive("frost_mastery_2")
                        .name("氷結魔法 II")
                        .description("魔法防御力をレベルごとに8%上昇させる。")
                        .icon(Material.PRISMARINE_CRYSTALS)
                        .position(7, 14)
                        .maxLevel(3).costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_frost_mastery_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAGIC_DEFENSE, "st_frost_mastery_2"); }
                        }).build(),

                SkillTreeNode.skill("absolute_zero_node", "absolute_zero")
                        .name("アブソリュートゼロ")
                        .description("周囲を絶対零度にし、全てを凍り付かせる。")
                        .icon(Material.HEART_OF_THE_SEA)
                        .position(8, 14)
                        .maxLevel(1).costPerLevel(5).build()
        );
    }
}

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
                        .position(1, 0)
                        .maxLevel(1).costPerLevel(0)
                        .conflicts("warrior_start", "archer_start", "holy_start")
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_class_mage", 0.1, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_class_mage"); }
                        }).build(),

                // ========== MOBILITY ==========
                SkillTreeNode.skill("mobility_blink_node_mage", "blink")
                        .name("瞬間移動")
                        .description("視線方向の安全な地点へ瞬間移動する。")
                        .icon(Material.ENDER_PEARL)
                        .position(2, 0)
                        .requires("mage_start")
                        .maxLevel(1)
                        .costPerLevel(4)
                        .build(),

                // ========== ICE ==========
                SkillTreeNode.skill("ice_shard_node", "ice_shard")
                        .name("アイスシャード")
                        .description("氷の欠片を放ち、爆発ダメージを与える。")
                        .icon(Material.ICE)
                        .position(3, 0)
                        .requires("mobility_blink_node_mage")
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("ice_power_1")
                        .name("氷の心得 I")
                        .description("氷属性ダメージをレベルごとに5%上昇させる。")
                        .icon(Material.SNOWBALL)
                        .position(4, 0)
                        .requires("ice_shard_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ICE_DAMAGE, "st_ice_power_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ICE_DAMAGE, "st_ice_power_1"); }
                        }).build(),

                SkillTreeNode.skill("ice_spike_node", "ice_spike")
                        .name("アイススパイク")
                        .description("標的の足元から氷の棘を噴出させる。")
                        .icon(Material.PACKED_ICE)
                        .position(5, 0)
                        .requires("ice_power_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("frost_armor")
                        .name("氷の鎧")
                        .description("防御力をレベルごとに5%上昇させる。")
                        .icon(Material.BLUE_ICE)
                        .position(6, 0)
                        .requires("ice_spike_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.DEFENSE, "st_frost_armor", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.DEFENSE, "st_frost_armor"); }
                        }).build(),

                SkillTreeNode.skill("frost_breath_node", "frost_breath")
                        .name("フロストブレス")
                        .description("前方広範囲に氷の息を吹き付ける。")
                        .icon(Material.SNOW_BLOCK)
                        .position(7, 0)
                        .requires("frost_armor")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("ice_power_2")
                        .name("氷の心得 II")
                        .description("氷属性ダメージをレベルごとに8%上昇させる。")
                        .icon(Material.DIAMOND)
                        .position(8, 0)
                        .requires("frost_breath_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.ICE_DAMAGE, "st_ice_power_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.ICE_DAMAGE, "st_ice_power_2"); }
                        }).build(),

                SkillTreeNode.skill("blizzard_node", "blizzard")
                        .name("ブリザード")
                        .description("周囲に極寒の吹雪を発生させる。")
                        .icon(Material.NETHER_STAR)
                        .position(9, 0)
                        .requires("ice_power_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== FIRE ==========
                SkillTreeNode.skill("fireball_node", "fireball")
                        .name("ファイアボール")
                        .description("火球を放ち、爆発ダメージを与える。")
                        .icon(Material.FIRE_CHARGE)
                        .position(10, 0)
                        .requires("blizzard_node")
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("fire_power_1")
                        .name("火の心得 I")
                        .description("火属性ダメージをレベルごとに5%上昇させる。")
                        .icon(Material.BLAZE_POWDER)
                        .position(11, 0)
                        .requires("fireball_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.FIRE_DAMAGE, "st_fire_power_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.FIRE_DAMAGE, "st_fire_power_1"); }
                        }).build(),

                SkillTreeNode.skill("flame_pillar_node", "flame_pillar")
                        .name("フレイムピラー")
                        .description("足元から火柱を噴出させる。")
                        .icon(Material.BLAZE_ROD)
                        .position(12, 0)
                        .requires("fire_power_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("mana_flow")
                        .name("魔力の流れ")
                        .description("最大マナをレベルごとに10%上昇させる。")
                        .icon(Material.LAPIS_LAZULI)
                        .position(13, 0)
                        .requires("flame_pillar_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.MAX_MANA, "st_mana_flow", level * 0.10, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.MAX_MANA, "st_mana_flow"); }
                        }).build(),

                SkillTreeNode.skill("flame_breath_node", "flame_breath")
                        .name("フレイムブレス")
                        .description("前方広範囲に炎を吹き付ける。")
                        .icon(Material.MAGMA_CREAM)
                        .position(14, 0)
                        .requires("mana_flow")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("fire_power_2")
                        .name("火の心得 II")
                        .description("火属性ダメージをレベルごとに8%上昇させる。")
                        .icon(Material.FIREWORK_STAR)
                        .position(15, 0)
                        .requires("flame_breath_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.FIRE_DAMAGE, "st_fire_power_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.FIRE_DAMAGE, "st_fire_power_2"); }
                        }).build(),

                SkillTreeNode.skill("fire_nova_node", "fire_nova")
                        .name("ファイアノヴァ")
                        .description("周囲を焼き尽くす衝撃波。")
                        .icon(Material.NETHER_STAR)
                        .position(16, 0)
                        .requires("fire_power_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== LIGHTNING ==========
                SkillTreeNode.skill("chain_lightning_node", "chain_lightning")
                        .name("チェインライトニング")
                        .description("雷球を放ち、命中した敵から周囲の敵へ連鎖する。")
                        .icon(Material.FIREWORK_STAR)
                        .position(17, 0)
                        .requires("fire_nova_node")
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("lightning_power_1")
                        .name("雷の心得 I")
                        .description("雷属性ダメージをレベルごとに5%上昇させる。")
                        .icon(Material.GUNPOWDER)
                        .position(18, 0)
                        .requires("chain_lightning_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.LIGHTNING_DAMAGE, "st_lightning_power_1", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.LIGHTNING_DAMAGE, "st_lightning_power_1"); }
                        }).build(),

                SkillTreeNode.skill("thunder_strike_node", "thunder_strike")
                        .name("サンダーストライク")
                        .description("対象地点に雷を落とし、範囲ダメージを与える。")
                        .icon(Material.NETHER_STAR)
                        .position(19, 0)
                        .requires("lightning_power_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("static_field")
                        .name("静電界")
                        .description("クールタイム短縮をレベルごとに5%上昇させる。")
                        .icon(Material.REDSTONE)
                        .position(20, 0)
                        .requires("thunder_strike_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_static_field", level * 0.05, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_static_field"); }
                        }).build(),

                SkillTreeNode.skill("lightning_storm_node", "lightning_storm")
                        .name("ライトニングストーム")
                        .description("前方に複数の雷撃を放つ。")
                        .icon(Material.FIREWORK_ROCKET)
                        .position(21, 0)
                        .requires("static_field")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("lightning_power_2")
                        .name("雷の心得 II")
                        .description("雷属性ダメージをレベルごとに8%上昇させる。")
                        .icon(Material.GLOWSTONE_DUST)
                        .position(22, 0)
                        .requires("lightning_storm_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override public void apply(Player player, int level, SkillTreeContext context) { statManager.setModifier(player.getUniqueId(), StatType.LIGHTNING_DAMAGE, "st_lightning_power_2", level * 0.08, ModifierType.MULTIPLICATIVE); }
                            @Override public void clear(Player player, SkillTreeContext context) { statManager.removeModifier(player.getUniqueId(), StatType.LIGHTNING_DAMAGE, "st_lightning_power_2"); }
                        }).build(),

                SkillTreeNode.skill("thunder_blast_node", "thunder_blast")
                        .name("サンダーブラスト")
                        .description("詠唱後、着弾時に爆発する雷球を放つ。")
                        .icon(Material.NETHER_STAR)
                        .position(23, 0)
                        .requires("lightning_power_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build()
        );
    }
}

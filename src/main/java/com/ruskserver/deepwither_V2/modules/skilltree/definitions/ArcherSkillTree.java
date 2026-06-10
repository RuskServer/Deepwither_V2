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
public class ArcherSkillTree implements SkillTreeDefinition {

    private final StatManager statManager;

    @Inject
    public ArcherSkillTree(StatManager statManager) {
        this.statManager = statManager;
    }

    @Override
    public String getId() {
        return "archer";
    }

    @Override
    public String getDisplayName() {
        return "弓兵";
    }

    @Override
    public Material getIcon() {
        return Material.BOW;
    }

    @Override
    public List<SkillTreeNode> getNodes() {
        return List.of(
                // ========== SNIPER BRANCH (Y=1): 一発が重い ==========

                SkillTreeNode.skill("archer_power_shot_node", "power_shot")
                        .name("パワーショット")
                        .description("弓を引き絞り、強力な一矢を放つ。最大30m先の敵1体に物理ダメージ(180%)を与える。")
                        .icon(Material.ARROW)
                        .position(0, 1)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("archer_marksman_1")
                        .name("狙撃訓練 I")
                        .description("攻撃力をレベルごとに5%上昇させる。")
                        .icon(Material.SPECTRAL_ARROW)
                        .position(1, 1)
                        .requires("archer_power_shot_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_archer_marksman_1", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_archer_marksman_1");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("archer_explosion_arrow_node", "explosion_arrow")
                        .name("爆発矢")
                        .description("青い光を纏った矢を放ち、着弾地点で爆発を引き起こす。半径5mの範囲へ物理ダメージ(120%)と鈍足(5秒)を与える。")
                        .icon(Material.FIREWORK_ROCKET)
                        .position(2, 1)
                        .requires("archer_marksman_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("archer_precision")
                        .name("精密射撃")
                        .description("クリティカル率をレベルごとに5%上昇させる。")
                        .icon(Material.TARGET)
                        .position(3, 1)
                        .requires("archer_explosion_arrow_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_archer_precision", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.CRITICAL_CHANCE, "st_archer_precision");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("archer_focused_shot_node", "focused_shot")
                        .name("狙い澄まし")
                        .description("狙いを定め、全神経を一点に集中させて放つ必中の一撃。0.8秒の射撃後、敵1体に物理ダメージ(300%)を与える。")
                        .icon(Material.CROSSBOW)
                        .position(4, 1)
                        .requires("archer_precision")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("archer_marksman_2")
                        .name("狙撃訓練 II")
                        .description("攻撃力をレベルごとに8%上昇させる。")
                        .icon(Material.ARROW)
                        .position(5, 1)
                        .requires("archer_focused_shot_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_archer_marksman_2", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.ATTACK_DAMAGE, "st_archer_marksman_2");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("archer_true_shot_node", "true_shot")
                        .name("トゥルーショット")
                        .description("全身全霊を込めた一射は、すべての防御を貫く。敵1体に確定ダメージ(攻撃力×4.0)を与え、防御力を完全に無視する。")
                        .icon(Material.NETHER_STAR)
                        .position(6, 1)
                        .requires("archer_marksman_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build(),

                // ========== RAPID FIRE BRANCH (Y=2): 連射バースト ==========

                SkillTreeNode.skill("archer_triple_shot_node", "triple_shot")
                        .name("三連射")
                        .description("息を整え、三本の矢を素早く放つ。3本の矢が前方へ扇状に飛翔し、命中した敵1体につき物理ダメージ(70%)を与える。")
                        .icon(Material.BOW)
                        .position(0, 2)
                        .maxLevel(1)
                        .costPerLevel(1)
                        .build(),

                SkillTreeNode.passive("archer_rapid_fire_1")
                        .name("速射訓練 I")
                        .description("クールタイム短縮をレベルごとに5%上昇させる。")
                        .icon(Material.SUGAR)
                        .position(1, 2)
                        .requires("archer_triple_shot_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_archer_rapid_fire_1", level * 0.05, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_archer_rapid_fire_1");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("archer_barrage_node", "barrage")
                        .name("乱れ撃ち")
                        .description("前方に向かって無数の矢をばらまき、広範囲を制圧する。扇状に5本の矢を放ち、命中した敵1体につき物理ダメージ(60%)を与える。")
                        .icon(Material.FIREWORK_STAR)
                        .position(2, 2)
                        .requires("archer_rapid_fire_1")
                        .maxLevel(1)
                        .costPerLevel(2)
                        .build(),

                SkillTreeNode.passive("archer_keen_eye")
                        .name("慧眼")
                        .description("クリティカルダメージをレベルごとに8%上昇させる。")
                        .icon(Material.SPYGLASS)
                        .position(3, 2)
                        .requires("archer_barrage_node")
                        .maxLevel(3)
                        .costPerLevel(1)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.CRITICAL_DAMAGE, "st_archer_keen_eye", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.CRITICAL_DAMAGE, "st_archer_keen_eye");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("archer_endless_volley_node", "endless_volley")
                        .name("連射")
                        .description("矢をつがえながら次々と放ち、高速連射を叩き込む。0.6秒間で5本の矢を連続して放ち、命中した敵1体につき物理ダメージ(50%)を与える。")
                        .icon(Material.ARROW)
                        .position(4, 2)
                        .requires("archer_keen_eye")
                        .maxLevel(1)
                        .costPerLevel(3)
                        .build(),

                SkillTreeNode.passive("archer_rapid_fire_2")
                        .name("速射訓練 II")
                        .description("クールタイム短縮をレベルごとに8%上昇させる。")
                        .icon(Material.FEATHER)
                        .position(5, 2)
                        .requires("archer_endless_volley_node")
                        .maxLevel(3)
                        .costPerLevel(2)
                        .passiveEffect(new SkillTreePassiveEffect() {
                            @Override
                            public void apply(Player player, int level, SkillTreeContext context) {
                                statManager.setModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_archer_rapid_fire_2", level * 0.08, ModifierType.MULTIPLICATIVE);
                            }

                            @Override
                            public void clear(Player player, SkillTreeContext context) {
                                statManager.removeModifier(player.getUniqueId(), StatType.COOLDOWN_REDUCTION, "st_archer_rapid_fire_2");
                            }
                        })
                        .build(),

                SkillTreeNode.skill("archer_rain_of_arrows_node", "rain_of_arrows")
                        .name("レインオブアロー")
                        .description("空高く放たれた合図の矢を皮切りに、無数の矢が降り注ぐ。標的の地点を中心に半径6mの範囲へ物理ダメージ(150%)を3回降らせる。")
                        .icon(Material.FIREWORK_ROCKET)
                        .position(6, 2)
                        .requires("archer_rapid_fire_2")
                        .maxLevel(1)
                        .costPerLevel(5)
                        .build()
        );
    }
}

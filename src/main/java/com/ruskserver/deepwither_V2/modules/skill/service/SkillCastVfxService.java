package com.ruskserver.deepwither_V2.modules.skill.service;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.util.AnimeVfxHelper;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 全スキルの発動成功時に、スキルごとの属性・カテゴリに応じた
 * 32×32 ドット調アニメ VFX（{@link AnimeVfxHelper}）を一括演出するディスパッチャ。
 * <p>
 * すでに個別演出を持つスキル（Whirlwind / MultiSlash / ShieldWall / Shockwave / Sanctuary / PowerShot / Fireball）は
 * 重複しないようスキップする。
 */
@Component
public class SkillCastVfxService {

    private static final Vector UP = new Vector(0, 1, 0);
    private final Deepwither_V2 plugin;

    @Inject
    public SkillCastVfxService(Deepwither_V2 plugin) {
        this.plugin = plugin;
    }

    public void playOnCastSuccess(Player player, Skill skill, SkillContext context) {
        if (player == null || skill == null || !player.isValid()) return;
        String id = skill.getId();
        if (id == null) return;

        Location feet = player.getLocation().add(0, 0.10, 0);
        Location chest = player.getLocation().add(0, 1.15, 0);
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().clone();
        if (dir.lengthSquared() < 1e-6) dir = new Vector(0, 0, 1);
        else dir.normalize();
        Location muzzle = eye.clone().add(dir.clone().multiply(0.65));

        switch (id) {
            // すでにスキル内で専用演出を持つもの
            case "whirlwind", "multi_slash", "shield_wall", "shockwave", "sanctuary", "power_shot", "fireball" -> {
            }

            // === 近接斬撃・強撃系 ===
            case "power_strike" ->
                    AnimeVfxHelper.spawnCrescentSlash(plugin, chest, dir, 3.6, Color.fromRGB(255, 225, 130), -24.0, 6);
            case "executioner" -> {
                AnimeVfxHelper.spawnCrescentSlash(plugin, chest, dir, 4.0, Color.fromRGB(255, 90, 90), 35.0, 6);
                AnimeVfxHelper.spawnCrescentSlash(plugin, chest, dir, 3.8, Color.fromRGB(185, 70, 255), -35.0, 6);
            }
            case "hammer_slam" -> {
                Location slamLoc = feet.clone().add(dir.clone().setY(0).normalize().multiply(2.0));
                AnimeVfxHelper.spawnShockwave(plugin, slamLoc, 0.6, 4.2, Color.fromRGB(255, 175, 70), 11, UP);
                AnimeVfxHelper.spawnImpactBurst(plugin, slamLoc.clone().add(0, 0.4, 0), 2.4, Color.fromRGB(255, 215, 110), 7);
            }
            case "seismic_stomp" -> {
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.7, 5.0, Color.fromRGB(235, 185, 105), 12, UP);
                AnimeVfxHelper.spawnRotatingSquare(plugin, null, feet, 3.2, Color.fromRGB(255, 210, 130), 14, 7.0, false);
            }
            case "charge" -> {
                AnimeVfxHelper.spawnProjectileMuzzle(plugin, chest, dir, 1.3, Color.fromRGB(255, 200, 105), 7);
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.5, 2.4, Color.fromRGB(255, 220, 145), 8, UP);
            }

            // === 防御・タンク・バフ系（足元追従の回転四角シール） ===
            case "fortress" -> {
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 2.1, Color.fromRGB(255, 205, 95), 100, 3.8, true);
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.6, 3.2, Color.fromRGB(255, 225, 140), 10, UP);
            }
            case "colossus" -> {
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 2.3, Color.fromRGB(255, 125, 60), 100, 3.2, true);
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.7, 3.8, Color.fromRGB(255, 150, 80), 10, UP);
            }
            case "battle_cry" -> {
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.7, 5.5, Color.fromRGB(255, 110, 75), 11, UP);
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 1.8, Color.fromRGB(255, 150, 95), 60, 5.5, false);
            }
            case "taunt" -> {
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.8, 6.0, Color.fromRGB(255, 75, 75), 12, UP);
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 2.0, Color.fromRGB(255, 95, 95), 40, -6.0, true);
            }
            case "divine_shield" -> {
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 1.7, Color.fromRGB(255, 242, 165), 100, 4.2, true);
                AnimeVfxHelper.spawnMagicCircle(plugin, player, null, 2.1, Color.fromRGB(255, 248, 200), 100, -2.5);
            }
            case "guardian_angel" -> {
                AnimeVfxHelper.spawnMagicCircle(plugin, player, null, 4.5, Color.fromRGB(255, 245, 175), 40, 3.0);
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.8, 5.0, Color.fromRGB(255, 250, 205), 11, UP);
            }
            case "martyrdom" -> {
                AnimeVfxHelper.spawnAbyssRift(plugin, feet, 2.2, Color.fromRGB(205, 85, 255), 80);
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 1.8, Color.fromRGB(255, 130, 175), 80, 4.0, false);
            }

            // === 移動・回避系 ===
            case "blink" -> {
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.4, 2.2, Color.fromRGB(165, 130, 255), 7, UP);
                AnimeVfxHelper.spawnRotatingSquare(plugin, null, feet, 1.4, Color.fromRGB(190, 155, 255), 10, 12.0, false);
            }
            case "evade" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, chest, dir.clone().multiply(-1), 1.1, Color.fromRGB(165, 245, 255), 6);

            // === プロジェクタイル・弓・ビーム系 ===
            case "arcane_bolt" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 0.85, Color.fromRGB(185, 125, 255), 6);
            case "ice_shard" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 0.9, Color.fromRGB(130, 230, 255), 6);
            case "thunder_blast", "chain_lightning" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 1.05, Color.fromRGB(255, 238, 105), 7);
            case "true_shot" -> {
                AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 1.35, Color.fromRGB(95, 200, 255), 8);
                AnimeVfxHelper.spawnRotatingSquare(plugin, null, feet, 1.6, Color.fromRGB(135, 225, 255), 12, 8.0, false);
            }
            case "focused_shot" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 1.15, Color.fromRGB(255, 235, 145), 7);
            case "triple_shot", "barrage", "endless_volley" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 1.0, Color.fromRGB(185, 245, 200), 6);
            case "explosion_arrow" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 1.1, Color.fromRGB(255, 130, 45), 7);

            // === 範囲ノヴァ・魔法陣・嵐・ブレス系 ===
            case "fire_nova" -> {
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.7, 5.2, Color.fromRGB(255, 105, 25), 12, UP);
                AnimeVfxHelper.spawnImpactBurst(plugin, chest, 2.8, Color.fromRGB(255, 185, 65), 7);
            }
            case "holy_nova" -> {
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.7, 5.2, Color.fromRGB(255, 242, 160), 12, UP);
                AnimeVfxHelper.spawnMagicCircle(plugin, null, feet, 4.8, Color.fromRGB(255, 248, 205), 18, 3.5);
            }
            case "flame_pillar" -> {
                // フレイムウォール本体は FlamePillarSkill 側で専用 3D VFX を展開、足元に詠唱印を表示
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 1.5, Color.fromRGB(255, 110, 30), 16, 7.0, true);
            }
            case "ice_spike" -> {
                // フロストピラー本体は IceSpikeSkill 側で専用 3D 氷柱を連鎖展開、足元に氷結詠唱印を表示
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 1.5, Color.fromRGB(130, 230, 255), 16, -7.0, true);
            }
            case "blizzard" -> {
                Location target = resolveGroundTarget(player, context, 12.0);
                AnimeVfxHelper.spawnMagicCircle(plugin, null, target, 5.0, Color.fromRGB(145, 232, 255), 80, 2.4);
                AnimeVfxHelper.spawnRotatingSquare(plugin, null, target.clone().add(0, 0.03, 0), 3.6, Color.fromRGB(205, 248, 255), 80, -3.0, false);
            }
            case "lightning_storm" -> {
                Location target = resolveGroundTarget(player, context, 12.0);
                AnimeVfxHelper.spawnMagicCircle(plugin, null, target, 5.0, Color.fromRGB(255, 240, 100), 70, 3.2);
                AnimeVfxHelper.spawnRotatingSquare(plugin, null, target.clone().add(0, 0.03, 0), 3.5, Color.fromRGB(145, 210, 255), 70, -4.2, false);
            }
            case "rain_of_arrows" -> {
                Location target = resolveGroundTarget(player, context, 14.0);
                AnimeVfxHelper.spawnMagicCircle(plugin, null, target, 4.8, Color.fromRGB(180, 255, 185), 60, 2.5);
            }
            case "flame_breath" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 1.25, Color.fromRGB(255, 105, 25), 10);
            case "frost_breath" ->
                    AnimeVfxHelper.spawnProjectileMuzzle(plugin, muzzle, dir, 1.25, Color.fromRGB(135, 235, 255), 10);
            case "thunder_strike" -> {
                Location target = resolveGroundTarget(player, context, 12.0);
                AnimeVfxHelper.spawnImpactBurst(plugin, target.clone().add(0, 1.0, 0), 2.5, Color.fromRGB(255, 245, 130), 7);
            }

            // === 回復・聖職サポート系 ===
            case "holy_light", "first_aid", "renewal", "sacrificial_light" -> {
                AnimeVfxHelper.spawnRotatingSquare(plugin, player, null, 1.5, Color.fromRGB(255, 245, 165), 24, 5.0, false);
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.4, 2.2, Color.fromRGB(165, 255, 195), 9, UP);
            }
            case "mass_heal", "purify" -> {
                AnimeVfxHelper.spawnMagicCircle(plugin, null, feet, 4.8, Color.fromRGB(170, 255, 200), 28, 3.0);
                AnimeVfxHelper.spawnShockwave(plugin, feet, 0.7, 5.2, Color.fromRGB(255, 250, 190), 11, UP);
            }
            case "holy_resurrection" -> {
                Location target = resolveGroundTarget(player, context, 8.0);
                AnimeVfxHelper.spawnMagicCircle(plugin, null, target, 3.2, Color.fromRGB(255, 245, 175), 50, 2.8);
                AnimeVfxHelper.spawnRotatingSquare(plugin, null, target.clone().add(0, 0.03, 0), 2.3, Color.fromRGB(255, 255, 225), 50, -3.5, false);
            }

            default -> {
                // 将来追加される深淵スキルや未知スキル用のスマートフォールバック
                if (skill.getTags().contains("abyss") || skill.getTags().contains("void")) {
                    AnimeVfxHelper.spawnAbyssRift(plugin, feet, 2.4, Color.fromRGB(180, 75, 255), 30);
                }
            }
        }
    }

    private Location resolveGroundTarget(Player player, SkillContext context, double maxDist) {
        Location target = context.getTargetLocation();
        if (target != null && target.getWorld() != null) {
            return target.clone().add(0, 0.08, 0);
        }
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        var hit = player.getWorld().rayTraceBlocks(eye, dir, maxDist, org.bukkit.FluidCollisionMode.NEVER, false);
        if (hit != null && hit.getHitPosition() != null) {
            return hit.getHitPosition().toLocation(player.getWorld()).add(0, 0.08, 0);
        }
        return player.getLocation().add(dir.clone().setY(0).normalize().multiply(Math.min(5.0, maxDist))).add(0, 0.08, 0);
    }
}

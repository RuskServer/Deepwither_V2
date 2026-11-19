package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import com.ruskserver.deepwither_V2.modules.skill.util.AnimeVfxHelper;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class PowerShotSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public PowerShotSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "power_shot"; }

    @Override
    public String getDisplayName() { return "パワーショット"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "弓を引き絞り、狙いすました強力な一矢を放つ。",
                "最大30m先の敵1体に物理ダメージ(180%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.ARROW; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("ranged", "archer", "power_shot"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 15.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(4); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var eye = player.getEyeLocation();
        var ray = player.getWorld().rayTrace(eye, eye.getDirection(), 30, FluidCollisionMode.NEVER, true, 1.0, e -> !e.equals(player));
        Entity raw = ray != null ? ray.getHitEntity() : null;
        if (!(raw instanceof LivingEntity target)) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("対象がいません。", NamedTextColor.RED));
        }

        var eyeLoc = player.getEyeLocation();
        var dir = eyeLoc.getDirection();

        var trailColor = Color.fromRGB(180, 210, 255);
        var plugin = JavaPlugin.getPlugin(Deepwither_V2.class);

        // 発射エフェクト: マズル衝撃波リング＋スクエア展開
        AnimeVfxHelper.spawnProjectileMuzzle(plugin, eyeLoc.clone().add(dir.clone().multiply(0.7)), dir, 1.1, trailColor, 7);

        // ビーム軌跡＋弾道ソニックリング
        var tLoc = target.getLocation().add(0, 1, 0);
        var beamDir = tLoc.toVector().subtract(eyeLoc.toVector());
        double beamLen = beamDir.length();
        beamDir.normalize();
        AnimeVfxHelper.spawnEnergyBeam(plugin, eyeLoc, beamDir, beamLen, 0.55, trailColor, 7);
        int ringCount = Math.min(5, Math.max(1, (int) (beamLen / 4.5)));
        for (int i = 1; i <= ringCount; i++) {
            double dist = (beamLen * i) / (ringCount + 1);
            Location ringLoc = eyeLoc.clone().add(beamDir.clone().multiply(dist));
            AnimeVfxHelper.spawnProjectileSonicRing(plugin, ringLoc, beamDir, 0.25, 1.05, trailColor, 6);
        }
        eyeLoc.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.8f);

        // ヒットエフェクト: 着弾衝撃波＋インパクト閃光
        tLoc.getWorld().spawnParticle(Particle.CRIT, tLoc, 16, 0.4, 0.4, 0.4, 0.3);
        tLoc.getWorld().playSound(tLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        AnimeVfxHelper.spawnProjectileImpact(plugin, tLoc, beamDir.clone().multiply(-1), 2.2, trailColor, 8);

        damagePipelineManager.processScaledDamage(player, target, DamageType.RANGED, 1.8, getTags(), getId(), 500L);

        return CastResult.success();
    }
}

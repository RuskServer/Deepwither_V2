package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamagePipelineManager;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillProjectile;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillProjectileService;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class ExplosionArrowSkill implements Skill {

    private final SkillProjectileService projectileService;
    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public ExplosionArrowSkill(SkillProjectileService projectileService, DamagePipelineManager damagePipelineManager) {
        this.projectileService = projectileService;
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "explosion_arrow"; }

    @Override
    public String getDisplayName() { return "爆発矢"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "青い光を纏った矢を放ち、着弾地点で爆発を引き起こす。",
                "直進する矢が命中した地点を中心に半径5mの範囲へ物理ダメージ(120%)と鈍足(5秒)を与える。内側1mは巻き込まない。"
        );
    }

    @Override
    public Material getIcon() { return Material.FIREWORK_ROCKET; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.PROJECTILE; }

    @Override
    public Set<String> getTags() { return Set.of("ranged", "archer", "aoe"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.DISPLACE); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public double getManaCost(SkillContext context) { return 25.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(8); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var spawnLoc = context.getEyeLocation().add(context.getDirection().multiply(1.0));
        var direction = context.getDirection();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.7f);

        var projectile = new SkillProjectile(player, spawnLoc, direction, 2.5, 1.0, 40) {

            private final Particle.DustOptions trailDust = new Particle.DustOptions(Color.fromRGB(0, 81, 255), 1.0f);

            @Override
            protected void onTick() {
                var loc = getCurrentLocation();
                loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.08, 0.08, 0.08, 0.02, trailDust);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 2, 0.05, 0.05, 0.05, 0.01);
            }

            @Override
            protected void onHitEntity(LivingEntity target) {
                explode(getCurrentLocation());
            }

            @Override
            protected void onHitBlock(Block block) {
                explode(getCurrentLocation());
            }

            private void explode(Location loc) {
                var world = loc.getWorld();
                if (world == null) return;

                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
                world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.6f);
                world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.5f);

                world.spawnParticle(Particle.EXPLOSION, loc, 3, 0.5, 0.5, 0.5, 0);
                world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2, 0.2, 0.2, 0.2, 0);
                world.spawnParticle(Particle.LAVA, loc, 40, 1.5, 1.5, 1.5, 0.2);
                world.spawnParticle(Particle.FLAME, loc, 60, 2.5, 2.5, 2.5, 0.1);
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 30, 1.0, 2.0, 1.0, 0.05);
                world.spawnParticle(Particle.FIREWORK, loc, 150, 3.0, 3.0, 3.0, 0.1);
                world.spawnParticle(Particle.GLOW, loc, 100, 2.0, 2.0, 2.0, 0.05);

                var caster = getCaster();
                for (Entity entity : world.getNearbyEntities(loc, 5.0, 5.0, 5.0)) {
                    if (!(entity instanceof LivingEntity living) || entity.equals(caster)) continue;
                    double dist = entity.getLocation().distance(loc);
                    if (dist < 1.0 || dist > 5.0) continue;
                    if (living.getNoDamageTicks() > 10) continue;

                    damagePipelineManager.processScaledDamage(caster, living, DamageType.RANGED, 1.2, getTags());
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                    living.setNoDamageTicks(10);
                }

                remove();
            }
        };

        boolean launched = projectileService.launch(projectile);
        return launched ? CastResult.success() : CastResult.fail();
    }
}

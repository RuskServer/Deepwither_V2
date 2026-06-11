package com.ruskserver.deepwither_V2.modules.skill.definitions;

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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class RainOfArrowsSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public RainOfArrowsSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "rain_of_arrows"; }

    @Override
    public String getDisplayName() { return "レインオブアロー"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "空高く放たれた合図の矢を皮切りに、無数の矢が降り注ぐ。",
                "標的の地点を中心に半径6mの範囲へ物理ダメージ(150%)を3回降らせる。"
        );
    }

    @Override
    public Material getIcon() { return Material.FIREWORK_ROCKET; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.LOCATION; }

    @Override
    public Set<String> getTags() { return Set.of("ranged", "archer", "aoe", "ultimate"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.LONG_CD, SkillTag.Constraint.HIGH_COST); }

    @Override
    public double getManaCost(SkillContext context) { return 45.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(25); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        var targetLoc = context.getTargetLocation();
        if (targetLoc == null) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("対象地点がありません。", NamedTextColor.RED));
        }

        var center = targetLoc.add(0, 1, 0);
        var world = center.getWorld();
        if (world == null) return CastResult.fail();

        // 合図の矢: プレイヤーから上空へ
        var playerEye = player.getEyeLocation();
        world.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.8f);
        for (double h = 0; h < 15; h += 0.5) {
            world.spawnParticle(Particle.CRIT, playerEye.clone().add(0, h, 0), 1, 0.05, 0.05, 0.05, 0);
        }

        new BukkitRunnable() {
            int wave = 0;

            @Override
            public void run() {
                if (wave >= 4) {
                    cancel();
                    return;
                }

                int arrowCount = 12 + wave * 3;

                for (int i = 0; i < arrowCount; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * 6.0;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double startY = 8.0 + Math.random() * 7.0;

                    for (double h = startY; h > 0.0; h -= 0.8) {
                        var trailPos = center.clone().add(x, h, z);
                        world.spawnParticle(Particle.CRIT, trailPos, 1, 0.03, 0.03, 0.03, 0);
                    }

                    var impactPos = center.clone().add(x, 0.3, z);
                    world.spawnParticle(Particle.CLOUD, impactPos, 2, 0.15, 0.05, 0.15, 0.01);
                }

                world.spawnParticle(Particle.EXPLOSION, center, 1, 1.5, 0.3, 1.5, 0);
                world.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.2f, 0.5f + wave * 0.1f);
                world.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 0.6f, 0.8f);

                for (Entity entity : world.getNearbyEntities(center, 6.0, 6.0, 6.0)) {
                    if (!(entity instanceof LivingEntity living) || entity.equals(player)) continue;
                    damagePipelineManager.processScaledDamage(player, living, DamageType.RANGED, 1.5, getTags());
                }

                wave++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(com.ruskserver.deepwither_V2.Deepwither_V2.class), 10L, 8L);

        world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 0.9f);

        return CastResult.success();
    }
}

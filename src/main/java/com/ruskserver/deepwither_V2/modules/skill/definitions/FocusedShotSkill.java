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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class FocusedShotSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public FocusedShotSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "focused_shot"; }

    @Override
    public String getDisplayName() { return "研ぎ澄まし"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "狙いを定め、全神経を一点に集中させて放つ必中の一撃。",
                "1秒間の予備動作の後、最大30m先の敵1体に射撃ダメージ(300%)を与える。"
        );
    }

    @Override
    public Material getIcon() { return Material.CROSSBOW; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("ranged", "archer", "focused"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.CHANNELING); }

    @Override
    public double getManaCost(SkillContext context) { return 30.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(10); }

    @Override
    public Duration getCastTime(SkillContext context) { return Duration.ofMillis(1000); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;
            final int CHARGE_TICKS = 20;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                if (ticks >= CHARGE_TICKS) {
                    fireBeam(player);
                    cancel();
                    return;
                }

                var eyeLoc = player.getEyeLocation();
                var dir = eyeLoc.getDirection();
                var spawnLoc = eyeLoc.clone().add(dir.clone().multiply(1.0));

                // 弾道予測線: 後半0.5秒のみ赤い点線
                if (ticks >= 10) {
                    var laser = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 0.5f);
                    for (double d = 2.0; d < 50; d += 4.0) {
                        var p = spawnLoc.clone().add(dir.clone().multiply(d));
                        player.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, laser);
                    }
                }

                // チャージエフェクト (成長するリング + 収束)
                double progress = (double) ticks / CHARGE_TICKS;
                drawChargeEffect(player, spawnLoc, progress);

                // チャージ音 (ピッチ上昇)
                if (ticks % 4 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.5f + ticks * 0.08f);
                }

                ticks++;
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 1L);

        return CastResult.success();
    }

    private void drawChargeEffect(LivingEntity player, Location spawnLoc, double progress) {
        var ringLoc = player.getLocation().add(0, 0.5, 0);
        double r = 0.5 + progress * 2.0;

        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            player.getWorld().spawnParticle(Particle.END_ROD, ringLoc.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);
        }

        player.getWorld().spawnParticle(Particle.END_ROD, spawnLoc, 3 + (int) (progress * 8), 0.05, 0.05, 0.05, 0.01);
        player.getWorld().spawnParticle(Particle.ENCHANT, spawnLoc, 2 + (int) (progress * 6), 0.1, 0.1, 0.1, 0.1);
    }

    private void fireBeam(LivingEntity player) {
        var eyeLoc = player.getEyeLocation();
        var dir = eyeLoc.getDirection();

        var ray = player.getWorld().rayTrace(eyeLoc, dir, 30, FluidCollisionMode.NEVER, true, 1.0, e -> e instanceof LivingEntity && !e.equals(player));
        var raw = ray != null ? ray.getHitEntity() : null;
        if (!(raw instanceof LivingEntity target)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("対象がいません。", NamedTextColor.RED));
            return;
        }

        var laser = new Particle.DustOptions(Color.fromRGB(255, 50, 50), 1.5f);
        for (double d = 0; d < player.getLocation().distance(target.getLocation()); d += 0.5) {
            var p = eyeLoc.clone().add(dir.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, laser);
        }

        player.getWorld().spawnParticle(Particle.FLASH, eyeLoc.add(dir.clone().multiply(1.5)), 1, 0, 0, 0, 0, Color.WHITE);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.8f);

        var tLoc = target.getLocation().add(0, 1, 0);
        tLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, tLoc, 1, 0.5, 0.5, 0.5, 0);
        tLoc.getWorld().spawnParticle(Particle.CRIT, tLoc, 40, 0.5, 0.5, 0.5, 0.6);
        tLoc.getWorld().spawnParticle(Particle.FLASH, tLoc, 1, 0.3, 0.3, 0.3, 0, Color.WHITE);
        tLoc.getWorld().playSound(tLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.6f);

        damagePipelineManager.processScaledDamage(player, target, DamageType.RANGED, 3.0, getTags());
    }
}

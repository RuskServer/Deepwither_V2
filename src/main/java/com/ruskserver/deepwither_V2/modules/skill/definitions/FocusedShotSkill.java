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
public class FocusedShotSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;

    @Inject
    public FocusedShotSkill(DamagePipelineManager damagePipelineManager) {
        this.damagePipelineManager = damagePipelineManager;
    }

    @Override
    public String getId() { return "focused_shot"; }

    @Override
    public String getDisplayName() { return "狙い澄まし"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "狙いを定め、全神経を一点に集中させて放つ必中の一撃。",
                "0.8秒の射撃後、最大30m先の敵1体に物理ダメージ(300%)を与える。"
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
    public Duration getCastTime(SkillContext context) { return Duration.ofMillis(800); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                var eyeLoc = player.getEyeLocation();
                var dir = eyeLoc.getDirection();

                float progress = (float) ticks / 16.0f;

                if (ticks == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
                }

                int count = 2 + (int) (progress * 10);
                var p = eyeLoc.clone().add(dir.clone().multiply(0.5));
                p.getWorld().spawnParticle(Particle.END_ROD, p, count, 0.03 + progress * 0.06, 0.03 + progress * 0.06, 0.03 + progress * 0.06, 0.01);
                p.getWorld().spawnParticle(Particle.ENCHANT, p, count, 0.1 + progress * 0.15, 0.1 + progress * 0.15, 0.1 + progress * 0.15, 0.1);

                if (ticks % 4 == 0 && ticks > 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.3f + progress * 0.4f, 1.5f + progress * 0.5f);
                }

                if (progress > 0.3f) {
                    player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 0.1, 0), 2, 0.3 + progress * 0.2, 0.02, 0.3 + progress * 0.2, 0.005);
                }

                ticks++;

                if (ticks >= 16) {
                    Entity raw = player.getTargetEntity(30);
                    if (!(raw instanceof LivingEntity target)) {
                        player.sendActionBar(net.kyori.adventure.text.Component.text("対象がいません。", NamedTextColor.RED));
                        cancel();
                        return;
                    }

                    var flashLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(0.5));
                    flashLoc.getWorld().spawnParticle(Particle.FLASH, flashLoc, 1, 0, 0, 0, 0, Color.WHITE);
                    flashLoc.getWorld().spawnParticle(Particle.CRIT, flashLoc, 25, 0.2, 0.2, 0.2, 0.4);
                    flashLoc.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);

                    var tLoc = target.getLocation().add(0, 1, 0);
                    tLoc.getWorld().spawnParticle(Particle.CRIT, tLoc, 30, 0.4, 0.4, 0.4, 0.5);
                    tLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, tLoc, 12, 0.5, 0.3, 0.5, 0);
                    tLoc.getWorld().playSound(tLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.6f);

                    damagePipelineManager.processScaledDamage(player, target, DamageType.PHYSICAL, 3.0, getTags());
                    cancel();
                }
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Deepwither_V2.class), 0L, 1L);

        return CastResult.success();
    }
}

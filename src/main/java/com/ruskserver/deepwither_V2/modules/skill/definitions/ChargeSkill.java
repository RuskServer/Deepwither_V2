package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTag;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import com.ruskserver.deepwither_V2.modules.skill.service.PlayerInputSnapshot;
import com.ruskserver.deepwither_V2.modules.skill.service.PlayerInputStateService;
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class ChargeSkill implements Skill {

    private final PlayerInputStateService inputStateService;

    @Inject
    public ChargeSkill(PlayerInputStateService inputStateService) {
        this.inputStateService = inputStateService;
    }

    @Override
    public String getId() {
        return "charge";
    }

    @Override
    public String getDisplayName() {
        return "突撃";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "視線方向へ勢いよく突撃し、入力に応じて軌道を変える。",
                "スプリント中は加速し、ジャンプ入力中は少し上方向へ飛び出す。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.RABBIT_FOOT;
    }

    @Override
    public SkillCategory getCategory() {
        return SkillCategory.ACTIVE;
    }

    @Override
    public SkillTargetType getTargetType() {
        return SkillTargetType.SELF;
    }

    @Override
    public Set<String> getTags() {
        return Set.of("mobility", "charge");
    }

    @Override
    public Set<SkillTag.Role> getRoles() {
        return Set.of(SkillTag.Role.UTILITY);
    }

    @Override
    public Set<SkillTag.Tactic> getTactics() {
        return Set.of(SkillTag.Tactic.MOBILITY, SkillTag.Tactic.DISPLACE);
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 20.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(7);
    }

    @Override
    public CastResult cast(SkillContext context) {
        Player player = context.getCaster();
        PlayerInputSnapshot input = inputStateService.getSnapshot(player);
        Vector direction = context.getDirection();
        direction.setY(Math.max(-0.05, Math.min(0.55, direction.getY())));
        if (direction.lengthSquared() < 0.0001) {
            direction = inputStateService.getHorizontalLookDirection(player.getLocation());
        } else {
            direction.normalize();
        }

        double speed = input.sneak() ? 1.25 : 1.65;
        if (input.forward() && input.sprint()) {
            speed += 0.25;
        }

        Vector velocity = direction.multiply(speed);
        if (input.jump()) {
            velocity.setY(Math.max(velocity.getY(), 0.35));
        }

        player.setVelocity(velocity);

        // 速度線トレイル
        var dir = velocity.clone().normalize();
        var pLoc = player.getLocation().add(0, 0.8, 0);
        for (double d = 0.3; d <= 4.0; d += 0.4) {
            var trailLoc = pLoc.clone().subtract(dir.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.CLOUD, trailLoc, 1, 0.15, 0.15, 0.15, 0.02);
            player.getWorld().spawnParticle(Particle.CRIT, trailLoc, 1, 0.15, 0.15, 0.15, 0.01);
        }
        TrailCircleHelper.spawnCircle(pLoc, 0.3, Color.fromRGB(180, 200, 255), 4, 4, dir, 0);
        TrailCircleHelper.spawnCircle(pLoc.subtract(dir.clone().multiply(1.5)), 0.5, Color.fromRGB(180, 200, 255), 3, 6, dir, 45);

        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0.0, 1.0, 0.0), 6, 0.25, 0.25, 0.25, 0.0);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.9f, 1.1f);
        return CastResult.success();
    }
}

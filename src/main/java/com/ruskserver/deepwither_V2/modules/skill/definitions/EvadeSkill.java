package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.skill.api.CastResult;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillCategory;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillContext;
import com.ruskserver.deepwither_V2.modules.skill.api.SkillTargetType;
import com.ruskserver.deepwither_V2.modules.skill.service.PlayerInputSnapshot;
import com.ruskserver.deepwither_V2.modules.skill.service.PlayerInputStateService;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class EvadeSkill implements Skill {

    private final PlayerInputStateService inputStateService;

    @Inject
    public EvadeSkill(PlayerInputStateService inputStateService) {
        this.inputStateService = inputStateService;
    }

    @Override
    public String getId() {
        return "evade";
    }

    @Override
    public String getDisplayName() {
        return "回避";
    }

    @Override
    public List<String> getDescription() {
        return List.of(
                "入力している移動方向へ素早く身をかわす。",
                "移動入力がない場合は視線方向へ回避し、ジャンプ入力中は少し高く跳ぶ。"
        );
    }

    @Override
    public Material getIcon() {
        return Material.FEATHER;
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
        return Set.of("mobility", "evade");
    }

    @Override
    public double getManaCost(SkillContext context) {
        return 12.0;
    }

    @Override
    public Duration getCooldown(SkillContext context) {
        return Duration.ofSeconds(4);
    }

    @Override
    public CastResult cast(SkillContext context) {
        Player player = context.getCaster();
        PlayerInputSnapshot input = inputStateService.getSnapshot(player);
        Vector direction = inputStateService.getMovementDirection(player, context.getDirection());
        double upward = input.jump() ? 0.28 : 0.18;

        player.setVelocity(direction.multiply(1.35).setY(upward));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 0.2, 0.0), 12, 0.35, 0.15, 0.35, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.6f);
        return CastResult.success();
    }
}

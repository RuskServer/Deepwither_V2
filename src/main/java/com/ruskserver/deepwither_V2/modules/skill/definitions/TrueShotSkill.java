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
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class TrueShotSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;
    private final StatManager statManager;

    @Inject
    public TrueShotSkill(DamagePipelineManager damagePipelineManager, StatManager statManager) {
        this.damagePipelineManager = damagePipelineManager;
        this.statManager = statManager;
    }

    @Override
    public String getId() { return "true_shot"; }

    @Override
    public String getDisplayName() { return "トゥルーショット"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "全身全霊を込めた一射は、すべての防御を貫く。",
                "最大30m先の敵1体に確定ダメージ(攻撃力×4.0)を与え、防御力を完全に無視する。"
        );
    }

    @Override
    public Material getIcon() { return Material.NETHER_STAR; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("ranged", "archer", "true_shot"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.ATTACK); }

    @Override
    public Set<SkillTag.Tactic> getTactics() { return Set.of(SkillTag.Tactic.ANTI_TANK, SkillTag.Tactic.BURST); }

    @Override
    public Set<SkillTag.Scaling> getScalings() { return Set.of(SkillTag.Scaling.PHYSICAL); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 40.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(25); }

    @Override
    public CastResult cast(SkillContext context) {
        var player = context.getCaster();
        Entity raw = player.getTargetEntity(30);
        if (!(raw instanceof LivingEntity target)) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("対象がいません。", NamedTextColor.RED));
        }

        var eyeLoc = player.getEyeLocation();
        var dir = eyeLoc.getDirection();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.5f);

        var sparkDust = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f);
        for (int i = 0; i < 20; i++) {
            var p = eyeLoc.clone().add(dir.clone().multiply(i * 0.3));
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 5, 0.1, 0.1, 0.1, 0.05);
            p.getWorld().spawnParticle(Particle.DUST, p, 3, 0.08, 0.08, 0.08, 0, sparkDust);
        }

        player.getWorld().spawnParticle(Particle.FLASH, eyeLoc.add(dir.multiply(1.0)), 1, 0, 0, 0, 0, Color.WHITE);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.8f);

        var tLoc = target.getLocation().add(0, 1, 0);
        tLoc.getWorld().spawnParticle(Particle.FIREWORK, tLoc, 100, 1.0, 1.0, 1.0, 0.2);
        tLoc.getWorld().spawnParticle(Particle.SONIC_BOOM, tLoc, 15, 0.5, 0.5, 0.5, 0);
        tLoc.getWorld().spawnParticle(Particle.GLOW, tLoc, 80, 1.5, 1.5, 1.5, 0.05);
        tLoc.getWorld().playSound(tLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

        double atk = statManager.getTotalStat(player, StatType.ATTACK_DAMAGE);
        damagePipelineManager.processDamage(player, target, DamageType.TRUE_DAMAGE, atk * 4.0, getTags());

        return CastResult.success();
    }
}

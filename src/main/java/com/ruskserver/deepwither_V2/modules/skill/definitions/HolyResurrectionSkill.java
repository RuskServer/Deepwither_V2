package com.ruskserver.deepwither_V2.modules.skill.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.revival.RevivalManager;
import com.ruskserver.deepwither_V2.modules.skill.api.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class HolyResurrectionSkill implements Skill {

    private final RevivalManager revivalManager;
    private final VirtualHealthManager healthManager;

    @Inject
    public HolyResurrectionSkill(RevivalManager revivalManager, VirtualHealthManager healthManager) {
        this.revivalManager = revivalManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getId() { return "holy_resurrection"; }

    @Override
    public String getDisplayName() { return "聖なる復活"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "自らの生命力を聖なる力に変え、倒れた仲間を蘇らせる。",
                "自分の最大HPの50%を消費し、直線方向上最も近い戦闘不能状態の味方を最大HP40%で蘇生する。"
        );
    }

    @Override
    public Material getIcon() { return Material.TOTEM_OF_UNDYING; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.SELF; }

    @Override
    public Set<String> getTags() { return Set.of("holy", "support", "resurrect", "hp_cost"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.HIGH_COST, SkillTag.Constraint.LONG_CD); }

    @Override
    public double getManaCost(SkillContext context) { return 30.0; }

    @Override
    public Duration getCooldown(SkillContext context) { return Duration.ofSeconds(180); }

    @Override
    public CastResult cast(SkillContext context) {
        var caster = context.getCaster();

        double maxHp = healthManager.getMaxHealth(caster);
        double currentHp = healthManager.getHealth(caster);
        double hpCost = maxHp * 0.5;

        if (currentHp <= hpCost) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("HPが足りません！", NamedTextColor.RED));
        }

        Player target = null;
        double nearest = Double.MAX_VALUE;

        for (Entity entity : caster.getNearbyEntities(20.0, 10.0, 20.0)) {
            if (!(entity instanceof Mannequin mannequin)) continue;
            if (!mannequin.getPersistentDataContainer().has(revivalManager.getCorpseKey(), PersistentDataType.BYTE)) continue;

            var profile = mannequin.getProfile();
            Optional<UUID> profileUuid = profile != null ? Optional.ofNullable(profile.uuid()) : Optional.empty();
            if (profileUuid.isEmpty()) continue;

            Player downed = Bukkit.getPlayer(profileUuid.get());
            if (downed == null || !revivalManager.isDowned(downed)) continue;

            double dist = entity.getLocation().distanceSquared(caster.getLocation());
            if (dist < nearest) {
                nearest = dist;
                target = downed;
            }
        }

        if (target == null) {
            return CastResult.fail(net.kyori.adventure.text.Component.text("近くに蘇生可能な味方がいません。", NamedTextColor.RED));
        }

        healthManager.damage(caster, hpCost);

        revivalManager.revive(target);
        double targetMaxHp = healthManager.getMaxHealth(target);
        healthManager.heal(target, targetMaxHp * 0.4);

        var casterLoc = caster.getLocation().add(0, 1, 0);
        casterLoc.getWorld().spawnParticle(Particle.END_ROD, casterLoc, 40, 1.0, 1.0, 1.0, 0.15);
        casterLoc.getWorld().spawnParticle(Particle.FLASH, casterLoc, 1, 0, 0, 0, 0);
        casterLoc.getWorld().playSound(casterLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.8f);

        var targetLoc = target.getLocation().add(0, 1, 0);
        targetLoc.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 40, 0.5, 0.5, 0.5, 0.1);
        targetLoc.getWorld().spawnParticle(Particle.HEART, targetLoc, 20, 0.5, 0.5, 0.5, 0);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        caster.sendMessage(net.kyori.adventure.text.Component.text("§a>> " + target.getName() + " を蘇生しました！"));
        target.sendMessage(net.kyori.adventure.text.Component.text("§a>> " + caster.getName() + " があなたを蘇生しました！"));

        return CastResult.success();
    }
}

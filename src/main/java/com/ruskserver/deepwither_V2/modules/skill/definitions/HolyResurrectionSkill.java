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
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class HolyResurrectionSkill implements Skill {

    private final VirtualHealthManager healthManager;
    private final RevivalManager revivalManager;

    @Inject
    public HolyResurrectionSkill(VirtualHealthManager healthManager, RevivalManager revivalManager) {
        this.healthManager = healthManager;
        this.revivalManager = revivalManager;
    }

    @Override
    public String getId() { return "holy_resurrection"; }

    @Override
    public String getDisplayName() { return "聖なる復活"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "自身のHPを半分捧げ、直線方向上最も近い戦闘不能状態の味方を蘇生する。",
                "蘇生された味方は最大HPの50%で復活する。"
        );
    }

    @Override
    public Material getIcon() { return Material.TOTEM_OF_UNDYING; }

    @Override
    public SkillCategory getCategory() { return SkillCategory.ACTIVE; }

    @Override
    public SkillTargetType getTargetType() { return SkillTargetType.ENTITY; }

    @Override
    public Set<String> getTags() { return Set.of("magic", "priest", "support"); }

    @Override
    public Set<SkillTag.Role> getRoles() { return Set.of(SkillTag.Role.SUPPORT); }

    @Override
    public Set<SkillTag.Constraint> getConstraints() { return Set.of(SkillTag.Constraint.LONG_CD, SkillTag.Constraint.HIGH_COST); }

    @Override
    public double getManaCost(SkillContext context) { return 80.0; }

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
        Vector casterEyeVec = caster.getEyeLocation().toVector();
        Vector casterDir = caster.getEyeLocation().getDirection().normalize();

        for (Entity entity : caster.getNearbyEntities(20.0, 10.0, 20.0)) {
            if (!(entity instanceof Mannequin mannequin)) continue;
            if (!mannequin.getPersistentDataContainer().has(revivalManager.getCorpseKey(), PersistentDataType.BYTE)) continue;

            var profile = mannequin.getProfile();
            Optional<UUID> profileUuid = profile != null ? Optional.ofNullable(profile.uuid()) : Optional.empty();
            if (profileUuid.isEmpty()) continue;

            Player downed = Bukkit.getPlayer(profileUuid.get());
            if (downed == null || !revivalManager.isDowned(downed)) continue;

            Vector toTarget = entity.getLocation().toVector().subtract(casterEyeVec);
            if (toTarget.lengthSquared() > 20.0 * 20.0 || toTarget.lengthSquared() == 0) continue;

            double dot = toTarget.normalize().dot(casterDir);
            if (dot < 0.75) continue;

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

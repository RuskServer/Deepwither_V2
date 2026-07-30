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
import com.ruskserver.deepwither_V2.modules.skill.util.TrailCircleHelper;
import com.ruskserver.deepwither_V2.modules.stat.StatManager;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
public class TrueShotSkill implements Skill {

    private final DamagePipelineManager damagePipelineManager;
    private final StatManager statManager;
    private final SkillProjectileService projectileService;

    @Inject
    public TrueShotSkill(DamagePipelineManager damagePipelineManager, StatManager statManager, SkillProjectileService projectileService) {
        this.damagePipelineManager = damagePipelineManager;
        this.statManager = statManager;
        this.projectileService = projectileService;
    }

    @Override
    public String getId() { return "true_shot"; }

    @Override
    public String getDisplayName() { return "トゥルーショット"; }

    @Override
    public List<String> getDescription() {
        return List.of(
                "全身全霊を込めた一射は、すべての防御を貫く。",
                "超高速の弾を発射し、命中地点で衝撃波を発生させ、防御力を完全に無視する。"
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
        var eyeLoc = player.getEyeLocation();
        var dir = eyeLoc.getDirection();

        // 発射エフェクト：円状のend_rodと青いパーティクル
        launchEffect(player, eyeLoc, dir);

        // まずヒットスキャンで着弾位置を計算
        
        // 30mまでのヒットスキャン
        var hitResult = eyeLoc.getWorld().rayTrace(
                eyeLoc,
                dir,
                30.0,
                org.bukkit.FluidCollisionMode.NEVER,
                true,
                0.3,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(player.getUniqueId())
        );
        
        Location impactLocation;
        LivingEntity target = null;
        
        if (hitResult != null && hitResult.getHitEntity() instanceof LivingEntity) {
            impactLocation = hitResult.getHitPosition().toLocation(eyeLoc.getWorld());
            target = (LivingEntity) hitResult.getHitEntity();
        } else if (hitResult != null && hitResult.getHitBlock() != null) {
            impactLocation = hitResult.getHitBlock().getLocation().add(0.5, 0.5, 0.5);
        } else {
            // ヒットしなかった場合は最大距離に着弾
            impactLocation = eyeLoc.clone().add(dir.clone().multiply(30.0));
        }
        
        // 超高速弾を発射
        SkillProjectile projectile = new SkillProjectile(
                player,
                eyeLoc.add(dir.multiply(0.6)),
                dir,
                8.0,  // 超高速
                0.3,
                60    // 最大生存時間
        ) {
            private boolean hasImpacted = false;
            private Location finalImpactLocation = impactLocation;
            
            @Override
            protected void onTick() {
                if (!hasImpacted) {
                    // 着弾前は小さなパーティクルのみ
                    var loc = getCurrentLocation();
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            @Override
            protected void onHitEntity(LivingEntity hitTarget) {
                if (!hasImpacted) {
                    hasImpacted = true;
                    finalImpactLocation = getCurrentLocation();
                    createImpactEffects(finalImpactLocation, hitTarget);
                }
            }

            @Override
            protected void onHitBlock(Block block) {
                if (!hasImpacted) {
                    hasImpacted = true;
                    finalImpactLocation = block.getLocation().add(0.5, 0.5, 0.5);
                    createImpactEffects(finalImpactLocation, null);
                }
            }
            
            @Override
            protected void expire() {
                if (!hasImpacted) {
                    // 最大生存時間に達した場合も着弾処理
                    hasImpacted = true;
                    createImpactEffects(finalImpactLocation, null);
                }
                remove();
            }
            
            private void createImpactEffects(Location impactLoc, LivingEntity hitTarget) {
                // ラインエフェクト：発射位置から着弾位置までのライン
                createLineEffect(eyeLoc.add(dir.multiply(0.6)), impactLoc, dir);
                
                // 衝撃波エフェクト
                impactEffect(impactLoc, dir);
                
                // ダメージ処理
                if (hitTarget != null) {
                    double atk = statManager.getTotalStat(getCaster(), StatType.RANGED_DAMAGE);
                    damagePipelineManager.processDamage(getCaster(), hitTarget, DamageType.TRUE_DAMAGE,
                            atk * 4.0, getTags(), getId(), 500L, impactLoc);
                }
            }
        };

        if (projectileService.launch(projectile)) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.5f);
            return CastResult.success();
        }
        return CastResult.fail();
    }

    private void launchEffect(LivingEntity player, Location eyeLoc, Vector direction) {
        // 円状のend_rodパーティクルエフェクト
        TrailCircleHelper.spawnCircle(eyeLoc, 2.0, Color.fromRGB(135, 206, 235), 10, 24);
        
        // 青い系のパーティクルエフェクト
        for (int i = 0; i < 15; i++) {
            var p = eyeLoc.clone().add(direction.clone().multiply(i * 0.2));
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.1, 0.1, 0.1, 0.03);
            p.getWorld().spawnParticle(Particle.BUBBLE, p, 2, 0.05, 0.05, 0.05, 0.01);
        }
        
        // 閃光エフェクト
        eyeLoc.getWorld().spawnParticle(Particle.FLASH, eyeLoc, 1, 0, 0, 0, 0, Color.WHITE);
    }

    private void createLineEffect(Location startLoc, Location endLoc, Vector direction) {
        // 発射位置から着弾位置までのラインをパーティクルで描画
        double distance = startLoc.distance(endLoc);
        int particleCount = (int) (distance * 2); // 距離に応じたパーティクル数
        
        for (int i = 0; i <= particleCount; i++) {
            double ratio = (double) i / particleCount;
            Location loc = startLoc.clone().add(
                    endLoc.clone().subtract(startLoc).multiply(ratio)
            );
            
            // ライン上にパーティクルを配置
            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0.1, 0.1, 0.1, 0.02);
            loc.getWorld().spawnParticle(Particle.GLOW, loc, 1, 0.05, 0.05, 0.05, 0);
            
            // ラインの両端を強調
            if (i == 0 || i == particleCount) {
                loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0, Color.WHITE);
            }
        }
        
        // 直線状の衝撃波エフェクト（角度指定）
        createLinearShockwave(startLoc, endLoc, direction);
    }
    
    private void createLinearShockwave(Location startLoc, Location endLoc, Vector direction) {
        // ラインに沿った衝撃波を扇状に表示
        int shockwaveCount = 8;
        double spreadAngle = 15.0; // 度単位の広がり角度
        
        for (int i = 0; i <= shockwaveCount; i++) {
            double t = (double) i / shockwaveCount;
            Location shockwaveLoc = startLoc.clone().add(
                    endLoc.clone().subtract(startLoc).multiply(t)
            );
            
            // ライン中心の衝撃波（最も強い）
            TrailCircleHelper.spawnCircle(shockwaveLoc, 0.8, Color.fromRGB(30, 144, 255), 8, 16, direction, 0);
            
            // 両側に小さな衝撃波（角度指定）
            if (i > 0 && i < shockwaveCount) {
                // 左右の角度を計算
                double leftAngle = -spreadAngle * Math.PI / 180.0;
                double rightAngle = spreadAngle * Math.PI / 180.0;
                
                // 左側の衝撃波
                Vector leftDir = rotateVector(direction, leftAngle);
                Location leftLoc = shockwaveLoc.clone().add(leftDir.multiply(1.0));
                TrailCircleHelper.spawnCircle(leftLoc, 0.4, Color.fromRGB(100, 149, 237), 5, 8, direction, 0);
                
                // 右側の衝撃波
                Vector rightDir = rotateVector(direction, rightAngle);
                Location rightLoc = shockwaveLoc.clone().add(rightDir.multiply(1.0));
                TrailCircleHelper.spawnCircle(rightLoc, 0.4, Color.fromRGB(100, 149, 237), 5, 8, direction, 0);
            }
        }
    }
    
    private Vector rotateVector(Vector vector, double angle) {
        // Y軸を中心にベクトルを回転
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        return new Vector(x, vector.getY(), z).normalize();
    }

    private void impactEffect(Location location, Vector direction) {
        // 着弾地点の前方に向かう直線状衝撃波
        createLinearBlast(location, direction);
        
        // 着弾地点の中心爆発
        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1, 0, 0, 0, 0);
        location.getWorld().spawnParticle(Particle.SONIC_BOOM, location, 15, 0.5, 0.5, 0.5, 0);
        location.getWorld().spawnParticle(Particle.GLOW, location, 40, 1.5, 1.5, 1.5, 0.05);
        
        // 前方に伸びる衝撃波
        for (int i = 1; i <= 5; i++) {
            Location forwardLoc = location.clone().add(direction.clone().multiply(i * 1.5));
            TrailCircleHelper.spawnCircle(forwardLoc, (6 - i) * 0.8, Color.fromRGB(30, 144, 255), 10 - i, 20 - i * 2, direction, 0);
        }
        
        // サウンドエフェクト
        location.getWorld().playSound(location, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);
        location.getWorld().playSound(location, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.2f);
    }
    
    private void createLinearBlast(Location center, Vector direction) {
        // 直線状の爆発エフェクト
        int blastCount = 6;
        for (int i = 0; i < blastCount; i++) {
            double distance = i * 0.8;
            Location blastLoc = center.clone().add(direction.clone().multiply(distance));
            
            // 前方への衝撃波
            TrailCircleHelper.spawnCircle(blastLoc, 1.5 - i * 0.2, Color.fromRGB(255, 69, 0), 8, 12, direction, 0);
            
            // 両側の小さな衝撃波
            if (i > 0) {
                Vector perpDir = getPerpendicularVector(direction);
                for (int side = -1; side <= 1; side += 2) {
                    Location sideLoc = blastLoc.clone().add(perpDir.clone().multiply(side * 0.5));
                    TrailCircleHelper.spawnCircle(sideLoc, 0.6, Color.fromRGB(255, 140, 0), 5, 8, direction, 0);
                }
            }
        }
    }
    
    private Vector getPerpendicularVector(Vector vector) {
        // ベクトルに直交するベクトルを取得
        Vector perp = new Vector(-vector.getZ(), 0, vector.getX());
        return perp.length() > 0 ? perp.normalize() : new Vector(1, 0, 0);
    }
}

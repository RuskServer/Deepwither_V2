package com.ruskserver.deepwither_V2.modules.combat.stagger;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.health.VirtualHealthManager;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMob;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.skill.util.AnimeVfxHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BossStaggerService implements Stoppable {

    private static final double AUDIENCE_RADIUS_SQ = 36.0 * 36.0;
    private static final Color BREAK_GOLD = Color.fromRGB(255, 220, 90);
    private static final Color BREAK_BRIGHT = Color.fromRGB(255, 244, 175);
    private static final DecimalFormat DAMAGE_FORMAT = new DecimalFormat("#,##0");

    private final Deepwither_V2 plugin;
    private final CustomMobManager customMobManager;
    private final VirtualHealthManager healthManager;
    private final Map<UUID, ActiveStaggerVisual> activeVisuals = new ConcurrentHashMap<>();

    @Inject
    public BossStaggerService(
            Deepwither_V2 plugin,
            CustomMobManager customMobManager,
            VirtualHealthManager healthManager) {
        this.plugin = plugin;
        this.customMobManager = customMobManager;
        this.healthManager = healthManager;
    }

    @Override
    public void stop() {
        for (ActiveStaggerVisual visual : activeVisuals.values()) {
            visual.cleanup();
        }
        activeVisuals.clear();
    }

    public void applyStaggeredDamageMultiplier(DamageContext context) {
        StaggerableBoss boss = resolveBoss(context);
        if (boss == null || !boss.getStaggerState().isStaggered()) {
            return;
        }
        context.multiplyDamage(boss.getStaggerProfile().staggeredDamageMultiplier());
    }

    /**
     * 体勢崩壊中ボスの「最初の致命の一撃（STAGGER RIPOSTE）」を消費し、追加倍率を返す。
     * 対象外・消費済みの場合は 1.0 を返す。
     */
    public double consumeStaggerRiposte(LivingEntity target) {
        StaggerableBoss boss = resolveBoss(target);
        if (boss == null) {
            return 1.0;
        }
        if (boss.getStaggerState().consumeRiposte()) {
            return boss.getStaggerProfile().riposteDamageMultiplier();
        }
        return 1.0;
    }

    public boolean isBossStaggered(LivingEntity target) {
        StaggerableBoss boss = resolveBoss(target);
        return boss != null && boss.getStaggerState().isStaggered();
    }

    public void recordResolvedDamage(DamageContext context) {
        StaggerableBoss boss = resolveBoss(context);
        if (boss == null || context.getDamage() <= 0.0) {
            return;
        }

        // 体勢崩壊中に殴った時の専用クリティカル・フィードバック
        if (boss.getStaggerState().isStaggered()) {
            playStaggeredHitFeedback(context.getDefender(), context.getImpactLocation());
            return;
        }

        if (context.getStaggerMultiplier() <= 0.0 || !boss.canReceiveStagger(context)) {
            return;
        }

        StaggerProfile profile = boss.getStaggerProfile();
        double typeMultiplier = profile.multiplierFor(context.getType());
        double criticalMultiplier = context.isCritical() ? 1.25 : 1.0;
        double baseAmount = context.getDamage() * typeMultiplier * criticalMultiplier;
        double amount = Math.min(profile.maxStaggerPerHit(), baseAmount)
                * context.getStaggerMultiplier();
        amount = Math.min(profile.maxStagger(), amount);

        if (boss.getStaggerState().add(amount, profile)) {
            boss.onStaggerStarted();
            triggerStaggerBreakPresentation(context.getDefender(), boss);
        }
    }

    private void triggerStaggerBreakPresentation(LivingEntity entity, StaggerableBoss boss) {
        if (entity == null || !entity.isValid() || entity.isDead()) {
            return;
        }

        World world = entity.getWorld();
        Location baseLoc = entity.getLocation();
        double height = Math.max(1.6, entity.getHeight());
        Location centerLoc = baseLoc.clone().add(0.0, height * 0.55, 0.0);
        Location groundLoc = baseLoc.clone().add(0.0, 0.15, 0.0);
        StaggerProfile profile = boss.getStaggerProfile();
        Color themeColor = boss.getStaggerThemeColor() != null ? boss.getStaggerThemeColor() : BREAK_GOLD;

        // 1. 重厚多層ブレイクSE（重低音＋鎧/盾破砕＋結晶砕け＋黄金の残響）
        world.playSound(centerLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.95f, 0.55f);
        world.playSound(centerLoc, Sound.BLOCK_ANVIL_LAND, 1.35f, 0.48f);
        world.playSound(centerLoc, Sound.ITEM_SHIELD_BREAK, 1.8f, 0.62f);
        world.playSound(centerLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.45f, 0.55f);
        world.playSound(centerLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.6f, 0.72f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (entity.isValid()) {
                world.playSound(entity.getLocation(), Sound.BLOCK_BELL_RESONATE, 1.35f, 0.68f);
            }
        }, 3L);

        // 2. 専用 3D VFX（爆発フラッシュ＋二重拡大衝撃波＋巨大クロス閃光＋放射パーティクル）
        AnimeVfxHelper.spawnImpactBurst(plugin, centerLoc, 3.8, BREAK_GOLD, 10);
        AnimeVfxHelper.spawnShockwave(plugin, groundLoc, 1.0, 6.8, BREAK_GOLD, 14, new Vector(0, 1, 0));
        AnimeVfxHelper.spawnShockwave(plugin, centerLoc, 0.8, 5.0, themeColor, 12, new Vector(0, 1, 0));
        AnimeVfxHelper.spawnHitSlashCross(plugin, centerLoc, BREAK_BRIGHT, 4.2, 10);

        world.spawnParticle(Particle.FLASH, centerLoc, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticle(Particle.END_ROD, centerLoc, 34, 0.5, 0.7, 0.5, 0.24);
        world.spawnParticle(Particle.CRIT, centerLoc, 42, 0.6, 0.8, 0.6, 0.35);

        // 3. ブレイク追加ダメージ（最大HP割合の確定ダメージ）
        double bonusDamage = 0.0;
        double maxHp = boss.getMaxHealthForStagger();
        if (maxHp <= 0.0) {
            maxHp = healthManager.getMaxHealth(entity);
        }
        if (profile.breakBonusDamageRatio() > 0.0 && maxHp > 0.0) {
            bonusDamage = Math.round(maxHp * profile.breakBonusDamageRatio());
            if (bonusDamage > 0.0) {
                healthManager.damage(entity, bonusDamage);
                spawnBreakFloatingBanner(entity, bonusDamage);
            }
        }

        // 4. 周囲のプレイヤーへのタイトル＆アクションバー通知
        String dmgMultStr = String.format("%.1fx", profile.staggeredDamageMultiplier());
        double totalRiposteMult = profile.staggeredDamageMultiplier() * profile.riposteDamageMultiplier();
        String riposteMultStr = String.format("%.1fx", totalRiposteMult);

        Component subtitle = Component.text("✦ ", TextColor.color(0xFFD54F), TextDecoration.BOLD)
                .append(Component.text("体勢崩壊", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" ✦  ", TextColor.color(0xFFD54F), TextDecoration.BOLD))
                .append(Component.text("被ダメ " + dmgMultStr, TextColor.color(0xFFE082)));
        Title title = Title.title(
                Component.empty(),
                subtitle,
                Title.Times.times(Duration.ofMillis(80), Duration.ofMillis(1250), Duration.ofMillis(450))
        );
        Component actionBar = Component.text("★ STAGGER BREAK! ", TextColor.color(0xFFD54F), TextDecoration.BOLD)
                .append(Component.text("最初の近接攻撃が ", NamedTextColor.WHITE))
                .append(Component.text("致命の一撃 (" + riposteMultStr + ")", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" になる！", NamedTextColor.WHITE));

        for (Player viewer : world.getPlayers()) {
            if (!viewer.isOnline() || viewer.isDead()) {
                continue;
            }
            if (viewer.getLocation().distanceSquared(baseLoc) <= AUDIENCE_RADIUS_SQ) {
                viewer.showTitle(title);
                viewer.sendActionBar(actionBar);
            }
        }

        // 5. 崩壊中のダウン姿勢＆頭上スタンリング＆弱点コア脈動タスクを開始
        startStaggeredVisualTask(entity, boss);
    }

    private void startStaggeredVisualTask(LivingEntity entity, StaggerableBoss boss) {
        UUID entityId = entity.getUniqueId();
        ActiveStaggerVisual previous = activeVisuals.remove(entityId);
        if (previous != null) {
            previous.cleanup();
        }

        boolean wasGlowing = entity.isGlowing();
        float originalYaw = entity.getLocation().getYaw();
        entity.setGlowing(true);
        entity.setRotation(originalYaw, 55.0f);

        int durationTicks = Math.max(10, boss.getStaggerProfile().staggerDurationTicks());
        Location headLoc = entity.getLocation().add(0.0, entity.getHeight() + 0.55, 0.0);
        BukkitTask ringTask = AnimeVfxHelper.spawnRotatingSquare(
                plugin, null, headLoc, 0.9, BREAK_GOLD, durationTicks, 8.0, true
        );

        ActiveStaggerVisual holder = new ActiveStaggerVisual(entity, wasGlowing, ringTask);
        BukkitTask loopTask = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || !boss.getStaggerState().isStaggered()) {
                    ActiveStaggerVisual removed = activeVisuals.remove(entityId);
                    if (removed != null) {
                        removed.cleanup();
                    } else {
                        holder.cleanup();
                    }
                    cancel();
                    return;
                }

                // うつむきダウン姿勢を維持
                Location loc = entity.getLocation();
                entity.setRotation(loc.getYaw(), 55.0f);

                World world = loc.getWorld();
                double height = Math.max(1.6, entity.getHeight());
                double angle = elapsed * 0.45;

                // 頭上に回転するスタンスター輪っか
                Location crownBase = loc.clone().add(0.0, height + 0.45, 0.0);
                for (int i = 0; i < 3; i++) {
                    double a = angle + i * (Math.PI * 2.0 / 3.0);
                    Location starLoc = crownBase.clone().add(Math.cos(a) * 0.65, 0.0, Math.sin(a) * 0.65);
                    world.spawnParticle(
                            Particle.DUST, starLoc, 1, 0.0, 0.0, 0.0, 0.0,
                            new Particle.DustOptions(BREAK_GOLD, 1.1f)
                    );
                }
                if (elapsed % 4 == 0) {
                    world.spawnParticle(Particle.CRIT, crownBase, 4, 0.45, 0.15, 0.45, 0.04);
                }

                // 致命の一撃（Riposte）が未消費なら、胸元に黄金の弱点コアを脈動表示
                if (boss.getStaggerState().isRiposteAvailable()) {
                    Location coreLoc = loc.clone().add(0.0, height * 0.55, 0.0);
                    float pulse = 1.25f + 0.35f * (float) Math.sin(elapsed * 0.6);
                    world.spawnParticle(
                            Particle.DUST, coreLoc, 4, 0.18, 0.18, 0.18, 0.0,
                            new Particle.DustOptions(BREAK_BRIGHT, pulse)
                    );
                    if (elapsed % 6 == 0) {
                        world.spawnParticle(Particle.END_ROD, coreLoc, 2, 0.15, 0.15, 0.15, 0.02);
                    }
                }

                // 復帰直前（残り20tick）に予兆の低い唸り音＆収束パーティクル
                int remaining = boss.getStaggerState().getStaggerTicksRemaining();
                if (remaining == 20) {
                    world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1.1f, 0.6f);
                }

                elapsed += 2;
            }
        }.runTaskTimer(plugin, 2L, 2L);

        holder.setLoopTask(loopTask);
        activeVisuals.put(entityId, holder);
    }

    private void playStaggeredHitFeedback(LivingEntity defender, Location impactLocation) {
        if (defender == null || !defender.isValid()) {
            return;
        }
        Location hitLoc = impactLocation != null
                ? impactLocation
                : defender.getLocation().add(0.0, defender.getHeight() * 0.55, 0.0);
        World world = hitLoc.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.95f, 1.25f);
        world.playSound(hitLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.55f);
        world.spawnParticle(Particle.CRIT, hitLoc, 10, 0.25, 0.25, 0.25, 0.18);
        world.spawnParticle(
                Particle.DUST, hitLoc, 8, 0.22, 0.22, 0.22, 0.0,
                new Particle.DustOptions(BREAK_GOLD, 1.15f)
        );
    }

    private void spawnBreakFloatingBanner(LivingEntity entity, double bonusDamage) {
        World world = entity.getWorld();
        Location spawnLoc = entity.getLocation().add(0.0, entity.getHeight() + 0.9, 0.0);
        Component text = Component.text("✦ BREAK! ", TextColor.color(0xFFD54F), TextDecoration.BOLD)
                .append(Component.text("-" + DAMAGE_FORMAT.format(bonusDamage), NamedTextColor.RED, TextDecoration.BOLD));

        TextDisplay display = world.spawn(spawnLoc, TextDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setInvulnerable(true);
            spawned.setGravity(false);
            spawned.setSilent(true);
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setTeleportDuration(2);
            spawned.setShadowed(true);
            spawned.setSeeThrough(true);
            spawned.setDefaultBackground(false);
            spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            spawned.text(text);
            Transformation t = spawned.getTransformation();
            t.getScale().set(1.25f, 1.25f, 1.25f);
            spawned.setTransformation(t);
        });

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!display.isValid() || tick >= 28) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    cancel();
                    return;
                }
                display.teleport(display.getLocation().add(0.0, 0.045, 0.0));
                if (tick >= 18) {
                    int alpha = Math.max(25, (int) (255 * (1.0 - (tick - 18) / 10.0)));
                    display.setTextOpacity((byte) alpha);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private StaggerableBoss resolveBoss(DamageContext context) {
        if (context == null || context.getDefender() == null) {
            return null;
        }
        return resolveBoss(context.getDefender());
    }

    private StaggerableBoss resolveBoss(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        CustomMob customMob = customMobManager.getCustomMob(entity);
        return customMob instanceof StaggerableBoss boss ? boss : null;
    }

    private static final class ActiveStaggerVisual {
        private final LivingEntity entity;
        private final boolean wasGlowing;
        private final BukkitTask ringTask;
        private BukkitTask loopTask;
        private boolean cleaned;

        ActiveStaggerVisual(LivingEntity entity, boolean wasGlowing, BukkitTask ringTask) {
            this.entity = entity;
            this.wasGlowing = wasGlowing;
            this.ringTask = ringTask;
        }

        void setLoopTask(BukkitTask loopTask) {
            this.loopTask = loopTask;
        }

        void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            if (loopTask != null && !loopTask.isCancelled()) {
                loopTask.cancel();
            }
            if (ringTask != null && !ringTask.isCancelled()) {
                ringTask.cancel();
            }
            if (entity != null && entity.isValid()) {
                entity.setGlowing(wasGlowing);
                entity.setRotation(entity.getLocation().getYaw(), 0.0f);
            }
        }
    }
}

package com.ruskserver.deepwither_V2.modules.combat.feedback;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageContext;
import com.ruskserver.deepwither_V2.modules.combat.damage.DamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Service
public class DamageIndicatorService implements Listener, Startable, Stoppable {

    private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("#,##0");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.0");
    private static final int LIFETIME_TICKS = 24;
    private static final int FADE_START_TICK = 14;
    private static final int MAX_PER_VIEWER = 24;
    private static final int MAX_GLOBAL = 128;
    private static final double MAX_VIEW_DISTANCE_SQUARED = 48.0 * 48.0;
    private static final double[] LANE_OFFSETS = {0.0, 0.12, -0.12, 0.24, -0.24};

    private final Deepwither_V2 plugin;
    private final DamageImpactResolver impactResolver;
    private final Deque<ActiveIndicator> activeIndicators = new ArrayDeque<>();
    private BukkitTask tickTask;

    @Inject
    public DamageIndicatorService(Deepwither_V2 plugin, DamageImpactResolver impactResolver) {
        this.plugin = plugin;
        this.impactResolver = impactResolver;
    }

    @Override
    public void start() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickIndicators, 1L, 1L);
    }

    @Override
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        while (!activeIndicators.isEmpty()) {
            removeIndicator(activeIndicators.removeFirst());
        }
    }

    public void show(DamageContext context) {
        if (context == null || context.getDamage() <= 0.0
                || context.getType() == DamageType.ENVIRONMENTAL) {
            return;
        }

        LivingEntity attacker = context.getAttacker();
        LivingEntity defender = context.getDefender();
        Location impact = impactResolver.resolve(context);

        if (attacker instanceof Player attackingPlayer) {
            showForViewer(attackingPlayer, defender, context, impact, false);
        }
        if (defender instanceof Player defendingPlayer
                && (attacker == null || !defendingPlayer.getUniqueId().equals(attacker.getUniqueId()))) {
            showForViewer(defendingPlayer, defender, context, impact, true);
        }
    }

    private void showForViewer(
            Player viewer,
            LivingEntity defender,
            DamageContext context,
            Location impact,
            boolean incoming) {
        if (!viewer.isOnline() || viewer.isDead()
                || !viewer.getWorld().equals(impact.getWorld())
                || viewer.getLocation().distanceSquared(impact) > MAX_VIEW_DISTANCE_SQUARED) {
            return;
        }

        trimToCapacity(viewer.getUniqueId());
        int lane = countActive(viewer.getUniqueId(), defender.getUniqueId()) % LANE_OFFSETS.length;
        Vector right = screenRightVector(viewer, impact);
        Location spawnLocation = impact.clone().add(right.clone().multiply(LANE_OFFSETS[lane]));
        float scale = context.isCritical() ? 0.82f : 0.68f;
        Component text = buildText(context, incoming);

        TextDisplay display = impact.getWorld().spawn(spawnLocation, TextDisplay.class, spawned -> {
            spawned.setVisibleByDefault(false);
            spawned.setPersistent(false);
            spawned.setInvulnerable(true);
            spawned.setGravity(false);
            spawned.setSilent(true);
            spawned.setBillboard(Display.Billboard.CENTER);
            spawned.setTeleportDuration(2);
            spawned.setViewRange(1.0f);
            spawned.setShadowed(true);
            spawned.setSeeThrough(true);
            spawned.setDefaultBackground(false);
            spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            spawned.setAlignment(TextDisplay.TextAlignment.CENTER);
            spawned.setLineWidth(120);
            spawned.setTextOpacity((byte) 255);
            spawned.text(text);

            Transformation transformation = spawned.getTransformation();
            transformation.getScale().set(scale, scale, scale);
            spawned.setTransformation(transformation);
        });

        viewer.showEntity(plugin, display);
        double horizontalDirection = lane == 0 ? 0.0 : Math.signum(LANE_OFFSETS[lane]);
        Vector drift = right.multiply(horizontalDirection * 0.018).setY(0.055);
        activeIndicators.addLast(new ActiveIndicator(
                display,
                viewer.getUniqueId(),
                defender.getUniqueId(),
                drift
        ));
    }

    private Component buildText(DamageContext context, boolean incoming) {
        String formattedDamage = formatDamage(context.getDamage());
        if (incoming) {
            return Component.text("−" + formattedDamage, NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, context.isCritical());
        }

        TextColor damageColor = resolveDamageColor(context);
        Component number = Component.text(formattedDamage, damageColor)
                .decoration(TextDecoration.BOLD, context.isCritical());
        if (!context.isCritical()) {
            return number;
        }
        return Component.text("✦ ", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .append(number)
                .append(Component.text(" ✦", NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
    }

    private TextColor resolveDamageColor(DamageContext context) {
        Set<String> tags = context.getTags();
        if (containsIgnoreCase(tags, "fire")) {
            return TextColor.color(0xFF7A24);
        }
        if (containsIgnoreCase(tags, "ice")) {
            return TextColor.color(0x8FE7FF);
        }
        if (containsIgnoreCase(tags, "lightning")) {
            return TextColor.color(0xFFF36B);
        }
        return switch (context.getType()) {
            case PHYSICAL -> NamedTextColor.WHITE;
            case RANGED -> NamedTextColor.YELLOW;
            case MAGIC -> NamedTextColor.LIGHT_PURPLE;
            case TRUE_DAMAGE -> NamedTextColor.AQUA;
            case ENVIRONMENTAL -> NamedTextColor.GRAY;
        };
    }

    private boolean containsIgnoreCase(Set<String> tags, String expected) {
        return tags.stream().anyMatch(tag -> tag.equalsIgnoreCase(expected));
    }

    private String formatDamage(double damage) {
        return damage < 10.0 ? DECIMAL_FORMAT.format(damage) : INTEGER_FORMAT.format(damage);
    }

    private void tickIndicators() {
        Iterator<ActiveIndicator> iterator = activeIndicators.iterator();
        while (iterator.hasNext()) {
            ActiveIndicator indicator = iterator.next();
            TextDisplay display = indicator.display();
            Player viewer = Bukkit.getPlayer(indicator.viewerId());
            indicator.incrementAge();

            if (!display.isValid() || viewer == null || !viewer.isOnline()
                    || !viewer.getWorld().equals(display.getWorld())
                    || indicator.age() >= LIFETIME_TICKS) {
                removeIndicator(indicator);
                iterator.remove();
                continue;
            }

            if (indicator.age() % 2 == 0) {
                display.teleport(display.getLocation().add(indicator.drift().clone().multiply(2.0)));
            }
            if (indicator.age() >= FADE_START_TICK) {
                double remaining = (LIFETIME_TICKS - indicator.age())
                        / (double) (LIFETIME_TICKS - FADE_START_TICK);
                int opacity = (int) Math.round(Math.max(0.0, remaining) * 255.0);
                display.setTextOpacity((byte) opacity);
            }
        }
    }

    private void trimToCapacity(UUID viewerId) {
        while (activeIndicators.size() >= MAX_GLOBAL) {
            removeIndicator(activeIndicators.removeFirst());
        }
        while (countActive(viewerId, null) >= MAX_PER_VIEWER) {
            ActiveIndicator oldest = activeIndicators.stream()
                    .filter(indicator -> indicator.viewerId().equals(viewerId))
                    .findFirst()
                    .orElse(null);
            if (oldest == null) {
                break;
            }
            activeIndicators.remove(oldest);
            removeIndicator(oldest);
        }
    }

    private int countActive(UUID viewerId, UUID defenderId) {
        int count = 0;
        for (ActiveIndicator indicator : activeIndicators) {
            if (!indicator.viewerId().equals(viewerId)) {
                continue;
            }
            if (defenderId == null || indicator.defenderId().equals(defenderId)) {
                count++;
            }
        }
        return count;
    }

    private Vector screenRightVector(Player viewer, Location impact) {
        Vector towardViewer = viewer.getEyeLocation().toVector().subtract(impact.toVector()).setY(0.0);
        if (towardViewer.lengthSquared() < 1.0E-6) {
            return new Vector(1.0, 0.0, 0.0);
        }
        towardViewer.normalize();
        return new Vector(-towardViewer.getZ(), 0.0, towardViewer.getX());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        activeIndicators.removeIf(indicator -> {
            if (!indicator.viewerId().equals(playerId)) {
                return false;
            }
            removeIndicator(indicator);
            return true;
        });
    }

    private void removeIndicator(ActiveIndicator indicator) {
        if (indicator.display().isValid()) {
            indicator.display().remove();
        }
    }

    private static final class ActiveIndicator {
        private final TextDisplay display;
        private final UUID viewerId;
        private final UUID defenderId;
        private final Vector drift;
        private int age;

        private ActiveIndicator(TextDisplay display, UUID viewerId, UUID defenderId, Vector drift) {
            this.display = display;
            this.viewerId = viewerId;
            this.defenderId = defenderId;
            this.drift = drift;
        }

        private TextDisplay display() {
            return display;
        }

        private UUID viewerId() {
            return viewerId;
        }

        private UUID defenderId() {
            return defenderId;
        }

        private Vector drift() {
            return drift;
        }

        private int age() {
            return age;
        }

        private void incrementAge() {
            age++;
        }
    }
}

package com.ruskserver.deepwither_V2.modules.gui;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class GuiService implements Startable, Stoppable {

    private static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final long CLEANUP_INTERVAL_TICKS = 20L * 60L;

    private final Deepwither_V2 plugin;
    private final Logger logger;
    private final LegacyGuiBridge legacyGuiBridge;
    private final Map<String, GuiView> views = new HashMap<>();
    private final Map<UUID, GuiSession> sessions = new HashMap<>();
    private BukkitTask cleanupTask;

    @Inject
    public GuiService(Deepwither_V2 plugin, Logger logger, List<GuiView> guiViews, LegacyGuiBridge legacyGuiBridge) {
        this.plugin = plugin;
        this.logger = logger;
        this.legacyGuiBridge = legacyGuiBridge;
        for (GuiView view : guiViews) {
            GuiView previous = views.put(view.getId(), view);
            if (previous != null) {
                throw new IllegalStateException("Duplicate GUI id: " + view.getId());
            }
        }
    }

    @Override
    public void start() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredSessions,
                CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
        logger.info("[GuiService] Registered " + views.size() + " GuiView(s).");
    }

    @Override
    public void stop() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        clearAll();
    }

    public void open(Player player, String guiId) {
        open(player, guiId, GuiContext.EMPTY, true);
    }

    public void open(Player player, String guiId, GuiContext context) {
        open(player, guiId, context, true);
    }

    public void open(Player player, String guiId, GuiContext context, boolean pushHistory) {
        GuiView view = views.get(guiId);
        if (view == null) {
            if (legacyGuiBridge.open(player, guiId)) {
                clear(player.getUniqueId());
                return;
            }
            logger.warning("[GuiService] Unknown GUI id: " + guiId);
            return;
        }

        GuiSession session = sessions.computeIfAbsent(player.getUniqueId(), GuiSession::new);
        if (pushHistory) {
            session.pushCurrentToHistory();
        }

        UUID token = UUID.randomUUID();
        GuiContext safeContext = context == null ? GuiContext.EMPTY : context;
        GuiInventoryHolder holder = new GuiInventoryHolder(player.getUniqueId(), guiId, token);
        Component title = view.getTitle(player, safeContext);
        Inventory inventory = Bukkit.createInventory(holder, view.getSize(player, safeContext), title);
        holder.setInventory(inventory);

        view.render(new GuiRenderContext(this, player, inventory, safeContext));
        session.setCurrent(guiId, safeContext, token);
        session.setTransitioning(true);
        player.openInventory(inventory);
        Bukkit.getScheduler().runTask(plugin, () -> {
            GuiSession current = sessions.get(player.getUniqueId());
            if (current != null && token.equals(current.getSessionToken())) {
                current.setTransitioning(false);
            }
        });
    }

    public void refresh(Player player) {
        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null || session.getCurrentGuiId() == null) return;
        open(player, session.getCurrentGuiId(), session.getCurrentContext(), false);
    }

    public void back(Player player) {
        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            open(player, "main_menu", GuiContext.EMPTY, false);
            return;
        }
        GuiHistoryEntry entry = session.popHistory();
        if (entry == null) {
            open(player, "main_menu", GuiContext.EMPTY, false);
            return;
        }
        open(player, entry.guiId(), entry.context(), false);
    }

    public void close(Player player) {
        clear(player.getUniqueId());
        player.closeInventory();
    }

    void openLater(Player player, String guiId, GuiContext context) {
        Bukkit.getScheduler().runTask(plugin, () -> open(player, guiId, context));
    }

    void backLater(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> back(player));
    }

    void closeLater(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> close(player));
    }

    void refreshLater(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> refresh(player));
    }

    void handleClick(GuiInventoryHolder holder, org.bukkit.event.inventory.InventoryClickEvent event) {
        GuiSession session = sessions.get(holder.getPlayerId());
        if (session == null || !holder.getSessionToken().equals(session.getSessionToken())) {
            event.setCancelled(true);
            return;
        }

        GuiView view = views.get(holder.getGuiId());
        if (view == null) {
            event.setCancelled(true);
            return;
        }

        session.touch();
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        view.onClick(new GuiClickContext(this, view, session.getCurrentContext(), event));
    }

    void handleClose(GuiInventoryHolder holder) {
        GuiSession session = sessions.get(holder.getPlayerId());
        if (session == null) return;
        if (!holder.getSessionToken().equals(session.getSessionToken())) return;
        if (session.isTransitioning()) return;
        clear(holder.getPlayerId());
    }

    public void clear(UUID playerId) {
        sessions.remove(playerId);
    }

    public void clearAll() {
        sessions.clear();
    }

    private void cleanupExpiredSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now, SESSION_TTL));
    }
}

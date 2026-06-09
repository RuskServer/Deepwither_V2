package com.ruskserver.deepwither_V2.modules.character.listener;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.character.gui.CharacterCreateGui;
import com.ruskserver.deepwither_V2.modules.character.gui.CharacterSelectGui;
import com.ruskserver.deepwither_V2.modules.gui.GuiInventoryHolder;
import com.ruskserver.deepwither_V2.modules.gui.GuiService;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CharacterSelectionGateListener implements Listener, PlayerLifecycleTask {
    private static final long PROMPT_INTERVAL_MILLIS = 2_000L;

    private final CharacterService characterService;
    private final GuiService guiService;
    private final Deepwither_V2 plugin;
    private final Map<UUID, Long> lastPromptAt = new ConcurrentHashMap<>();

    @Inject
    public CharacterSelectionGateListener(CharacterService characterService, GuiService guiService, Deepwither_V2 plugin) {
        this.characterService = characterService;
        this.guiService = guiService;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!requiresCharacterSelection(event.getPlayer()) || !event.hasChangedPosition()) return;
        event.setCancelled(true);
        promptSelection(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (requiresCharacterSelection(event.getPlayer())) {
            event.setCancelled(true);
            promptSelection(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (requiresCharacterSelection(event.getPlayer())) {
            event.setCancelled(true);
            promptSelection(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (requiresCharacterSelection(event.getPlayer())) {
            event.setCancelled(true);
            promptSelection(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && requiresCharacterSelection(player)) {
            event.setCancelled(true);
            promptSelection(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByPlayer(EntityDamageByEntityEvent event) {
        Player attacker = findAttackingPlayer(event);
        if (attacker != null && requiresCharacterSelection(attacker)) {
            event.setCancelled(true);
            promptSelection(attacker);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !requiresCharacterSelection(player)) return;
        if (isOpenCharacterGui(event.getView().getTopInventory().getHolder())) {
            return;
        }
        event.setCancelled(true);
        promptSelection(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !requiresCharacterSelection(player)) return;
        if (isOpenCharacterGui(event.getView().getTopInventory().getHolder())) {
            return;
        }
        event.setCancelled(true);
        promptSelection(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!requiresCharacterSelection(event.getPlayer())) return;
        String command = event.getMessage().split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (command.equals("/character") || command.equals("/char") || command.equals("/login") || command.equals("/l")
                || command.equals("/register") || command.equals("/reg")) {
            return;
        }
        event.setCancelled(true);
        promptSelection(event.getPlayer());
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.QUIT);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.CLEANUP;
    }

    @Override
    public int order() {
        return -100;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> {
            context.player().ifPresent(characterService::saveCharacterState);
            characterService.clearSharedBalanceCache(context.playerId());
            lastPromptAt.remove(context.playerId());
        });
    }

    private boolean requiresCharacterSelection(Player player) {
        if (player.hasPermission("deepwither.character.bypass")) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        return characterService.isSelectionLocked(playerId) || !characterService.hasCachedActiveCharacter(playerId);
    }

    private void promptSelection(Player player) {
        long now = System.currentTimeMillis();
        long lastPrompt = lastPromptAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastPrompt < PROMPT_INTERVAL_MILLIS) {
            return;
        }
        lastPromptAt.put(player.getUniqueId(), now);

        if (characterService.isSelectionLocked(player.getUniqueId())) {
            player.sendMessage(net.kyori.adventure.text.Component.text("死亡処理が完了するまでキャラクターを変更できません。", NamedTextColor.RED));
            return;
        }

        player.sendMessage(net.kyori.adventure.text.Component.text("プレイするにはキャラクターを選択してください。", NamedTextColor.YELLOW));
        guiService.open(player, CharacterSelectGui.ID);
    }

    private boolean isOpenCharacterGui(org.bukkit.inventory.InventoryHolder holder) {
        if (!(holder instanceof GuiInventoryHolder guiHolder)) {
            return false;
        }
        return CharacterSelectGui.ID.equals(guiHolder.getGuiId()) || CharacterCreateGui.ID.equals(guiHolder.getGuiId());
    }

    private Player findAttackingPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}

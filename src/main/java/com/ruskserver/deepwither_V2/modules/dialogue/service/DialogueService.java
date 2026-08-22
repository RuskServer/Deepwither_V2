package com.ruskserver.deepwither_V2.modules.dialogue.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.ruskserver.deepwither_V2.modules.dialogue.api.*;
import com.ruskserver.deepwither_V2.modules.dialogue.event.DialogueChoiceEvent;
import com.ruskserver.deepwither_V2.modules.dialogue.event.DialogueEndEvent;
import com.ruskserver.deepwither_V2.modules.dialogue.event.DialogueStartEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class DialogueService implements Startable, Stoppable {

    private static final int AUTO_ADVANCE_DELAY_TICKS = 10;
    private static final double MAX_DISTANCE_SQUARED = 36.0; // 6ブロック離れたら自動離脱
    private static final Component DIVIDER = MiniMessage.miniMessage()
            .deserialize("<gradient:#ffffff:#38b6ff><st>                                                </st></gradient>");

    private final Logger logger;
    private final JavaPlugin plugin;
    private final DIContainer container;
    private final Map<UUID, DialogueSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, DialogueGraph> npcDialogues = new ConcurrentHashMap<>();
    private BukkitTask distanceCheckTask;

    @Inject
    public DialogueService(JavaPlugin plugin, DIContainer container) {
        this.logger = plugin.getLogger();
        this.plugin = plugin;
        this.container = container;
    }

    @Override
    public void start() {
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof Dialogue dialogue) {
                for (String npcName : dialogue.getNpcNames()) {
                    if (npcName != null && !npcName.isBlank()) {
                        registerDialogue(npcName, dialogue.getGraph());
                        logger.info("[DialogueService] 会話定義を登録: " + npcName + " → " + dialogue.getGraph().id());
                    }
                }
            }
        }
        logger.info("[DialogueService] 会話システムを開始しました (" + npcDialogues.size() + " 件の会話定義)");

        // 一定距離離脱チェックタスク（10tick毎）
        distanceCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkDistance, 10L, 10L);
    }

    @Override
    public void stop() {
        if (distanceCheckTask != null) {
            distanceCheckTask.cancel();
            distanceCheckTask = null;
        }
        for (DialogueSession session : sessions.values()) {
            session.completeWithCancellation();
        }
        sessions.clear();
        logger.info("[DialogueService] 会話システムを停止しました");
    }

    private void checkDistance() {
        for (Iterator<Map.Entry<UUID, DialogueSession>> it = sessions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, DialogueSession> entry = it.next();
            DialogueSession session = entry.getValue();
            Player player = session.player();

            if (player == null || !player.isOnline() || player.isDead()) {
                session.completeWithCancellation();
                it.remove();
                continue;
            }

            Location startLoc = session.startLocation();
            Location currentLoc = player.getLocation();

            if (!Objects.equals(startLoc.getWorld(), currentLoc.getWorld())
                    || startLoc.distanceSquared(currentLoc) > MAX_DISTANCE_SQUARED) {
                player.sendActionBar(Component.empty());
                player.sendMessage(Component.text("§7(会話から離れました)"));
                session.completeWithCancellation();
                Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player,
                        new DialogueResult(session.graph().id(), session.currentNodeId(), null, Map.copyOf(session.flags()))));
                it.remove();
            }
        }
    }

    public void registerDialogue(String npcName, DialogueGraph graph) {
        npcDialogues.put(npcName.toLowerCase(), graph);
        logger.fine("[DialogueService] 会話を登録: " + npcName + " (" + graph.id() + ")");
    }

    public void unregisterDialogue(String npcName) {
        npcDialogues.remove(npcName.toLowerCase());
    }

    @Nullable
    public DialogueGraph getDialogue(String npcName) {
        return npcDialogues.get(npcName.toLowerCase());
    }

    public boolean hasDialogue(String npcName) {
        return npcDialogues.containsKey(npcName.toLowerCase());
    }

    public CompletableFuture<DialogueResult> startDialogue(Player player, DialogueGraph graph) {
        DialogueSession existing = sessions.get(player.getUniqueId());
        if (existing != null && !existing.isEnded()) {
            existing.completeWithCancellation();
        }

        DialogueStartEvent startEvent = new DialogueStartEvent(player, graph);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            return CompletableFuture.completedFuture(
                    new DialogueResult(graph.id(), null, null, Map.of()));
        }

        CompletableFuture<DialogueResult> future = new CompletableFuture<>();
        DialogueSession session = new DialogueSession(player, graph, future);
        sessions.put(player.getUniqueId(), session);

        DialogueNode startNode = graph.startNode();
        if (startNode == null) {
            session.complete(null, null);
            sessions.remove(player.getUniqueId());
            future.complete(new DialogueResult(graph.id(), null, null, Map.of()));
            return future;
        }

        Bukkit.getScheduler().runTask(plugin, () -> showNode(player, startNode, session));
        return future;
    }

    public CompletableFuture<DialogueResult> startNpcDialogue(Player player, String npcName) {
        DialogueGraph graph = npcDialogues.get(npcName.toLowerCase());
        if (graph == null) {
            return CompletableFuture.completedFuture(
                    new DialogueResult("none", null, null, Map.of()));
        }
        return startDialogue(player, graph);
    }

    public void selectChoice(Player player, int index) {
        DialogueSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isEnded() || !session.isAwaitingInput()) {
            return;
        }

        DialogueNode currentNode = session.graph().node(session.currentNodeId());
        if (currentNode == null) return;

        List<DialogueChoice> available = session.cachedChoices();
        if (index < 0 || index >= available.size()) return;

        DialogueChoice chosen = available.get(index);

        DialogueChoiceEvent choiceEvent = new DialogueChoiceEvent(player, session.graph(), chosen, session.flags());
        Bukkit.getPluginManager().callEvent(choiceEvent);
        if (choiceEvent.isCancelled()) return;

        DialogueContext ctx = new DialogueContext(player, session.flags());

        for (DialogueAction action : chosen.actions()) {
            action.execute(ctx);
            if (ctx.isEnded()) break;
        }

        if (ctx.isEnded()) {
            player.sendActionBar(Component.empty());
            session.complete(currentNode.id(), chosen.text());
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, new DialogueResult(
                    session.graph().id(), currentNode.id(), chosen.text(), Map.copyOf(session.flags()))));
            cleanupSession(player);
            return;
        }

        session.setAwaitingInput(false);

        String nextNodeId = chosen.nextNodeId();
        if (nextNodeId == null) {
            player.sendActionBar(Component.empty());
            DialogueResult r = new DialogueResult(session.graph().id(), currentNode.id(), chosen.text(), Map.copyOf(session.flags()));
            session.complete(currentNode.id(), chosen.text());
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, r));
            cleanupSession(player);
            return;
        }

        DialogueNode nextNode = session.graph().node(nextNodeId);
        if (nextNode == null) {
            player.sendActionBar(Component.empty());
            DialogueResult r = new DialogueResult(session.graph().id(), currentNode.id(), chosen.text(), Map.copyOf(session.flags()));
            session.complete(currentNode.id(), chosen.text());
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, r));
            cleanupSession(player);
            return;
        }

        session.setCurrentNodeId(nextNodeId);
        showNode(player, nextNode, session);
    }

    public void scrollChoice(Player player, int delta) {
        DialogueSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isEnded() || !session.isAwaitingInput()) return;

        List<DialogueChoice> choices = session.cachedChoices();
        if (choices.size() <= 1) return;

        int newIndex = session.selectedIndex() + delta;
        int size = choices.size();
        newIndex = ((newIndex % size) + size) % size;

        session.setSelectedIndex(newIndex);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
        renderChoiceScreen(player, session);
    }

    public void confirmCurrentChoice(Player player) {
        DialogueSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isEnded() || !session.isAwaitingInput()) return;

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        selectChoice(player, session.selectedIndex());
    }

    public void endDialogue(Player player) {
        DialogueSession session = sessions.get(player.getUniqueId());
        if (session != null && !session.isEnded()) {
            player.sendActionBar(Component.empty());
            DialogueResult r = new DialogueResult(session.graph().id(), session.currentNodeId(), null, Map.copyOf(session.flags()));
            session.completeWithCancellation();
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, r));
            cleanupSession(player);
        }
    }

    public boolean isInDialogue(Player player) {
        DialogueSession session = sessions.get(player.getUniqueId());
        return session != null && !session.isEnded();
    }

    public boolean isAwaitingInput(Player player) {
        DialogueSession session = sessions.get(player.getUniqueId());
        return session != null && !session.isEnded() && session.isAwaitingInput();
    }

    @Nullable
    public DialogueSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void removeSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    private void showNode(Player player, DialogueNode node, DialogueSession session) {
        DialogueContext ctx = new DialogueContext(player, session.flags());

        for (DialogueAction action : node.actions()) {
            action.execute(ctx);
            if (ctx.isEnded()) break;
        }

        if (ctx.isEnded()) {
            player.sendActionBar(Component.empty());
            DialogueResult result = new DialogueResult(session.graph().id(), node.id(), null, Map.copyOf(session.flags()));
            session.complete(node.id(), null);
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, result));
            cleanupSession(player);
            return;
        }

        List<DialogueChoice> available = filterAvailableChoices(node.choices(), ctx);

        if (available.isEmpty()) {
            // 選択肢がない終端ノードの場合
            player.sendActionBar(Component.empty());
            player.sendMessage(DIVIDER);
            if (!node.text().equals("/skip/")) {
                player.sendMessage(Component.text(node.text(), NamedTextColor.WHITE));
            }
            player.sendMessage(DIVIDER);

            DialogueResult result = new DialogueResult(session.graph().id(), node.id(), null, Map.copyOf(session.flags()));
            session.complete(node.id(), null);
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, result));
            cleanupSession(player);
            return;
        }

        if (available.size() == 1) {
            DialogueChoice only = available.get(0);
            DialogueNode target = only.nextNodeId() != null ? session.graph().node(only.nextNodeId()) : null;

            if (target != null && target.speaker() == SpeakerType.NPC && node.text().equals("/skip/")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    DialogueSession s = sessions.get(player.getUniqueId());
                    if (s == null || s.isEnded() || !s.currentNodeId().equals(node.id())) return;

                    DialogueContext actionCtx = new DialogueContext(player, s.flags());
                    for (DialogueAction action : only.actions()) {
                        action.execute(actionCtx);
                        if (actionCtx.isEnded()) break;
                    }

                    if (actionCtx.isEnded()) {
                        player.sendActionBar(Component.empty());
                        DialogueResult r = new DialogueResult(s.graph().id(), node.id(), only.text(), Map.copyOf(s.flags()));
                        s.complete(node.id(), only.text());
                        Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, r));
                        cleanupSession(player);
                        return;
                    }

                    s.setCurrentNodeId(only.nextNodeId());
                    showNode(player, target, s);
                }, AUTO_ADVANCE_DELAY_TICKS);
                return;
            }
        }

        session.setSelectedIndex(0);
        session.setCachedChoices(available);
        session.setAwaitingInput(true);
        renderChoiceScreen(player, session);
    }

    public void renderChoiceScreen(Player player, DialogueSession session) {
        DialogueNode currentNode = session.graph().node(session.currentNodeId());
        if (currentNode == null) return;

        List<DialogueChoice> choices = session.cachedChoices();
        if (choices.isEmpty()) return;

        int selectedIndex = session.selectedIndex();

        player.sendMessage(DIVIDER);
        if (!currentNode.text().equals("/skip/")) {
            player.sendMessage(Component.text(currentNode.text(), NamedTextColor.WHITE));
            player.sendMessage(Component.empty());
        }

        for (int i = 0; i < choices.size(); i++) {
            DialogueChoice choice = choices.get(i);
            final int index = i;
            boolean isSelected = (i == selectedIndex);

            Component line;
            if (isSelected) {
                line = Component.text(" §a>>> §f" + choice.text())
                        .clickEvent(ClickEvent.callback(s -> selectChoice(player, index)))
                        .hoverEvent(HoverEvent.showText(Component.text("クリックまたは [Fキー] で決定", NamedTextColor.YELLOW)));
            } else {
                line = Component.text(" §7    " + choice.text())
                        .clickEvent(ClickEvent.callback(s -> selectChoice(player, index)))
                        .hoverEvent(HoverEvent.showText(Component.text("クリックして選択・決定", NamedTextColor.GRAY)));
            }
            player.sendMessage(line);
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text(" §8[マウスホイール: 選択 / Fキー: 決定]"));
        player.sendMessage(DIVIDER);

        // アクションバーにも現在選択中の項目を表示
        if (selectedIndex >= 0 && selectedIndex < choices.size()) {
            player.sendActionBar(Component.text("§a>>> §f" + choices.get(selectedIndex).text() + " §8[Fキー: 決定]"));
        }
    }

    private List<DialogueChoice> filterAvailableChoices(List<DialogueChoice> choices, DialogueContext ctx) {
        return choices.stream()
                .filter(c -> c.condition() == null || c.condition().test(ctx))
                .collect(Collectors.toList());
    }

    private void cleanupSession(Player player) {
        sessionCleanup(player.getUniqueId());
    }

    void sessionCleanup(UUID uuid) {
        DialogueSession session = sessions.remove(uuid);
        if (session != null && !session.future().isDone()) {
            session.completeWithCancellation();
        }
    }
}

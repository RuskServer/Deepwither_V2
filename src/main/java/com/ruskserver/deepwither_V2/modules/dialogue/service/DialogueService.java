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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class DialogueService implements Startable, Stoppable {

    private static final int AUTO_ADVANCE_DELAY_TICKS = 10;

    private final Logger logger;
    private final JavaPlugin plugin;
    private final DIContainer container;
    private final Map<UUID, DialogueSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, DialogueGraph> npcDialogues = new ConcurrentHashMap<>();

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
                registerDialogue(dialogue.getNpcName(), dialogue.getGraph());
                logger.info("[DialogueService] 会話定義を登録: " + dialogue.getNpcName() + " → " + dialogue.getGraph().id());
            }
        }
        logger.info("[DialogueService] 会話システムを開始しました (" + npcDialogues.size() + " 件の会話定義)");
    }

    @Override
    public void stop() {
        for (DialogueSession session : sessions.values()) {
            session.completeWithCancellation();
        }
        sessions.clear();
        logger.info("[DialogueService] 会話システムを停止しました");
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

        List<DialogueChoice> available = filterAvailableChoices(currentNode.choices(),
                new DialogueContext(player, session.flags()));
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
            session.complete(currentNode.id(), chosen.text());
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, new DialogueResult(
                    session.graph().id(), currentNode.id(), chosen.text(), Map.copyOf(session.flags()))));
            cleanupSession(player);
            return;
        }

        session.setAwaitingInput(false);

        String nextNodeId = chosen.nextNodeId();
        if (nextNodeId == null) {
            DialogueResult r = new DialogueResult(session.graph().id(), currentNode.id(), chosen.text(), Map.copyOf(session.flags()));
            session.complete(currentNode.id(), chosen.text());
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, r));
            cleanupSession(player);
            return;
        }

        DialogueNode nextNode = session.graph().node(nextNodeId);
        if (nextNode == null) {
            DialogueResult r = new DialogueResult(session.graph().id(), currentNode.id(), chosen.text(), Map.copyOf(session.flags()));
            session.complete(currentNode.id(), chosen.text());
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, r));
            cleanupSession(player);
            return;
        }

        session.setCurrentNodeId(nextNodeId);
        showNode(player, nextNode, session);
    }

    public void endDialogue(Player player) {
        DialogueSession session = sessions.get(player.getUniqueId());
        if (session != null && !session.isEnded()) {
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
            DialogueResult result = new DialogueResult(session.graph().id(), node.id(), null, Map.copyOf(session.flags()));
            session.complete(node.id(), null);
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, result));
            cleanupSession(player);
            return;
        }

        if (!node.text().equals("/skip/")) {
            sendSpeakerMessage(player, node);
        }

        List<DialogueChoice> available = filterAvailableChoices(node.choices(), ctx);

        if (available.isEmpty()) {
            DialogueResult result = new DialogueResult(session.graph().id(), node.id(), null, Map.copyOf(session.flags()));
            session.complete(node.id(), null);
            Bukkit.getPluginManager().callEvent(new DialogueEndEvent(player, result));
            cleanupSession(player);
            return;
        }

        if (available.size() == 1) {
            DialogueChoice only = available.get(0);
            DialogueNode target = only.nextNodeId() != null ? session.graph().node(only.nextNodeId()) : null;

            if (target != null && target.speaker() == SpeakerType.NPC) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    DialogueSession s = sessions.get(player.getUniqueId());
                    if (s == null || s.isEnded() || !s.currentNodeId().equals(node.id())) return;

                    DialogueContext actionCtx = new DialogueContext(player, s.flags());
                    for (DialogueAction action : only.actions()) {
                        action.execute(actionCtx);
                        if (actionCtx.isEnded()) break;
                    }

                    if (actionCtx.isEnded()) {
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

        session.setAwaitingInput(true);
        showChoices(player, available);
    }

    private void sendSpeakerMessage(Player player, DialogueNode node) {
        Component prefix;
        if (node.speaker() == SpeakerType.NPC) {
            prefix = Component.text("[NPC] ", NamedTextColor.GOLD, TextDecoration.BOLD);
        } else {
            prefix = Component.text("[あなた] ", NamedTextColor.AQUA);
        }

        Component message = prefix.append(Component.text(node.text(), NamedTextColor.WHITE));
        player.sendMessage(message);
    }

    private void showChoices(Player player, List<DialogueChoice> choices) {
        player.sendMessage(Component.text("--- 選択肢 ---", NamedTextColor.GRAY, TextDecoration.ITALIC));

        for (int i = 0; i < choices.size(); i++) {
            DialogueChoice choice = choices.get(i);
            final int index = i;

            Component option = Component.text("[" + (i + 1) + "] ", NamedTextColor.GREEN)
                    .append(Component.text(choice.text(), NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.callback(s -> selectChoice(player, index)))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("クリックして選択", NamedTextColor.YELLOW)));

            player.sendMessage(option);
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

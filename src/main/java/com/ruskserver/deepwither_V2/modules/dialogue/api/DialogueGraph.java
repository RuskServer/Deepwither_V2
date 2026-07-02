package com.ruskserver.deepwither_V2.modules.dialogue.api;

import org.jetbrains.annotations.Nullable;
import java.util.*;

public class DialogueGraph {

    private final String id;
    private final Map<String, DialogueNode> nodes;
    private final String startNodeId;

    public DialogueGraph(String id, Map<String, DialogueNode> nodes, String startNodeId) {
        this.id = id;
        this.nodes = Map.copyOf(nodes);
        this.startNodeId = startNodeId;
    }

    public String id() {
        return id;
    }

    public Map<String, DialogueNode> nodes() {
        return nodes;
    }

    public String startNodeId() {
        return startNodeId;
    }

    @Nullable
    public DialogueNode node(String nodeId) {
        return nodes.get(nodeId);
    }

    @Nullable
    public DialogueNode startNode() {
        return nodes.get(startNodeId);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private final Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        private String startNodeId;

        private Builder(String id) {
            this.id = id;
        }

        public NodeBuilder node(String nodeId, SpeakerType speaker, String text) {
            return new NodeBuilder(this, nodeId, speaker, text);
        }

        void register(DialogueNode node) {
            if (startNodeId == null) {
                startNodeId = node.id();
            }
            nodes.put(node.id(), node);
        }

        public DialogueGraph build() {
            if (nodes.isEmpty()) {
                throw new IllegalStateException("DialogueGraph must have at least one node");
            }
            return new DialogueGraph(id, nodes, startNodeId);
        }
    }

    public static class NodeBuilder {
        private final Builder parent;
        private final String nodeId;
        private final SpeakerType speaker;
        private final String text;
        private final List<DialogueChoice> choices = new ArrayList<>();
        private final List<DialogueAction> actions = new ArrayList<>();

        NodeBuilder(Builder parent, String nodeId, SpeakerType speaker, String text) {
            this.parent = parent;
            this.nodeId = nodeId;
            this.speaker = speaker;
            this.text = text;
        }

        public NodeBuilder choice(String text, String nextNodeId) {
            choices.add(new DialogueChoice(text, nextNodeId));
            return this;
        }

        public NodeBuilder choice(String text, String nextNodeId, DialogueCondition condition) {
            choices.add(new DialogueChoice(text, nextNodeId, condition));
            return this;
        }

        public NodeBuilder action(DialogueAction action) {
            actions.add(action);
            return this;
        }

        public Builder end() {
            parent.register(new DialogueNode(nodeId, speaker, text, List.copyOf(actions), List.copyOf(choices)));
            return parent;
        }
    }
}

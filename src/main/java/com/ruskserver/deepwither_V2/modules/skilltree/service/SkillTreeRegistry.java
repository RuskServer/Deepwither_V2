package com.ruskserver.deepwither_V2.modules.skilltree.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SkillTreeRegistry implements Startable {

    private final DIContainer container;
    private final Map<String, SkillTreeDefinition> trees = new LinkedHashMap<>();
    private final Map<String, SkillTreeNode> nodesById = new LinkedHashMap<>();
    private final Map<String, SkillTreeDefinition> treeByNodeId = new LinkedHashMap<>();
    private final Map<String, SkillTreeNode> nodeBySkillId = new LinkedHashMap<>();

    @Inject
    public SkillTreeRegistry(DIContainer container) {
        this.container = container;
    }

    @Override
    public void start() {
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof SkillTreeDefinition tree) {
                register(tree);
            }
        }
        Bukkit.getLogger().info("[SkillTreeRegistry] " + trees.size() + " skill trees loaded.");
    }

    public SkillTreeDefinition getTree(String treeId) {
        return trees.get(treeId);
    }

    public SkillTreeNode getNode(String nodeId) {
        return nodesById.get(nodeId);
    }

    public SkillTreeDefinition getTreeByNode(String nodeId) {
        return treeByNodeId.get(nodeId);
    }

    public SkillTreeNode getNodeBySkillId(String skillId) {
        return nodeBySkillId.get(skillId);
    }

    public Collection<SkillTreeDefinition> getTrees() {
        return Collections.unmodifiableCollection(trees.values());
    }

    public Collection<SkillTreeNode> getNodes() {
        return Collections.unmodifiableCollection(nodesById.values());
    }

    private void register(SkillTreeDefinition tree) {
        if (trees.containsKey(tree.getId())) {
            Bukkit.getLogger().warning("[SkillTreeRegistry] Duplicate tree id skipped: " + tree.getId());
            return;
        }
        trees.put(tree.getId(), tree);

        for (SkillTreeNode node : tree.getNodes()) {
            if (nodesById.containsKey(node.getId())) {
                Bukkit.getLogger().warning("[SkillTreeRegistry] Duplicate node id skipped: " + node.getId());
                continue;
            }
            nodesById.put(node.getId(), node);
            treeByNodeId.put(node.getId(), tree);
            if (node.getSkillId() != null) {
                nodeBySkillId.putIfAbsent(node.getSkillId(), node);
            }
        }
    }
}

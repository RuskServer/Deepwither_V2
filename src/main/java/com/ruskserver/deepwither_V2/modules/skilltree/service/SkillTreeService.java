package com.ruskserver.deepwither_V2.modules.skilltree.service;

import com.ruskserver.deepwither_V2.core.database.character.CharacterDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleContext;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleEventType;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecyclePhase;
import com.ruskserver.deepwither_V2.core.lifecycle.player.PlayerLifecycleTask;
import com.ruskserver.deepwither_V2.modules.character.CharacterService;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillRegistry;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeContext;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNodeType;
import com.ruskserver.deepwither_V2.modules.skilltree.api.UnlockResult;
import com.ruskserver.deepwither_V2.modules.skilltree.event.SkillTreeNodeUnlockAttemptEvent;
import com.ruskserver.deepwither_V2.modules.skilltree.event.SkillTreeNodeUnlockEvent;
import com.ruskserver.deepwither_V2.modules.skilltree.event.SkillTreePointChangeEvent;
import com.ruskserver.deepwither_V2.modules.skilltree.provider.CharacterSkillTreeProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class SkillTreeService implements Listener, PlayerLifecycleTask {

    public static final int SKILL_POINTS_PER_LEVEL = 2;

    private final CharacterDataRepository characterDataRepository;
    private final CharacterService characterService;
    private final SkillTreeRegistry treeRegistry;
    private final SkillRegistry skillRegistry;

    @Inject
    public SkillTreeService(CharacterDataRepository characterDataRepository, CharacterService characterService, SkillTreeRegistry treeRegistry, SkillRegistry skillRegistry) {
        this.characterDataRepository = characterDataRepository;
        this.characterService = characterService;
        this.treeRegistry = treeRegistry;
        this.skillRegistry = skillRegistry;
    }

    public void grantLevelUpPoints(Player player, int gainedLevels) {
        if (gainedLevels <= 0) return;
        addSkillPoints(player, gainedLevels * SKILL_POINTS_PER_LEVEL, "level_up");
    }

    public void addSkillPoints(Player player, int amount, String reason) {
        if (amount == 0) return;
        characterService.getActiveCharacter(player.getUniqueId()).ifPresent(c -> {
            characterDataRepository.get(c.characterId()).ifPresent(data -> {
                CharacterSkillTreeProvider.SkillTreeData treeData = data.get(CharacterSkillTreeProvider.KEY);
                if (treeData == null) return;

                int before = treeData.getSkillPoints();
                treeData.addSkillPoints(amount);
                data.markDirty(CharacterSkillTreeProvider.KEY);
                characterDataRepository.save(c.characterId(), data);
                Bukkit.getPluginManager().callEvent(new SkillTreePointChangeEvent(player, before, treeData.getSkillPoints(), reason));
            });
        });
    }

    public boolean isSkillUnlocked(Player player, String skillId) {
        return getSkillLevel(player, skillId) > 0;
    }

    public int getSkillLevel(Player player, String skillId) {
        SkillTreeNode node = treeRegistry.getNodeBySkillId(skillId);
        if (node == null) {
            return 0;
        }
        var characterOpt = characterService.getActiveCharacter(player.getUniqueId());
        if (characterOpt.isEmpty()) return 0;
        var dataOpt = characterDataRepository.get(characterOpt.get().characterId());
        if (dataOpt.isEmpty()) return 0;
        CharacterSkillTreeProvider.SkillTreeData treeData = dataOpt.get().get(CharacterSkillTreeProvider.KEY);
        return treeData == null ? 0 : treeData.getNodeLevel(node.getId());
    }

    public UnlockResult unlock(Player player, String treeId, String nodeId) {
        SkillTreeDefinition tree = treeRegistry.getTree(treeId);
        SkillTreeNode node = treeRegistry.getNode(nodeId);
        if (tree == null || node == null || treeRegistry.getTreeByNode(nodeId) != tree) {
            return UnlockResult.fail(Component.text("ノード定義が見つかりません。", NamedTextColor.RED));
        }

        SkillTreeNodeUnlockAttemptEvent attemptEvent = new SkillTreeNodeUnlockAttemptEvent(player, tree, node);
        Bukkit.getPluginManager().callEvent(attemptEvent);
        if (attemptEvent.isCancelled()) {
            return UnlockResult.fail(Component.text("習得がキャンセルされました。", NamedTextColor.RED));
        }

        var characterOpt = characterService.getActiveCharacter(player.getUniqueId());
        if (characterOpt.isEmpty()) {
            return UnlockResult.fail(Component.text("アクティブなキャラクターがありません。", NamedTextColor.RED));
        }
        UUID characterId = characterOpt.get().characterId();
        var dataOpt = characterDataRepository.get(characterId);
        if (dataOpt.isEmpty()) {
            return UnlockResult.fail(Component.text("キャラクターデータを読み込めません。", NamedTextColor.RED));
        }

        var data = dataOpt.get();
        CharacterSkillTreeProvider.SkillTreeData treeData = data.get(CharacterSkillTreeProvider.KEY);
        if (treeData == null) {
            return UnlockResult.fail(Component.text("スキルツリーデータを読み込めません。", NamedTextColor.RED));
        }

        int currentLevel = treeData.getNodeLevel(nodeId);
        if (currentLevel >= node.getMaxLevel()) {
            return UnlockResult.fail(Component.text("これ以上レベルアップできません。", NamedTextColor.YELLOW));
        }

        if (node.getType() == SkillTreeNodeType.SKILL && skillRegistry.get(node.getSkillId()) == null) {
            return UnlockResult.fail(Component.text("対応するスキル定義が見つかりません。", NamedTextColor.RED));
        }

        for (String requirement : node.getRequirements()) {
            SkillTreeNode requiredNode = treeRegistry.getNode(requirement);
            if (requiredNode == null || treeData.getNodeLevel(requirement) < requiredNode.getMaxLevel()) {
                return UnlockResult.fail(Component.text("前提ノードが足りません。", NamedTextColor.RED));
            }
        }

        for (String conflict : node.getConflicts()) {
            if (treeData.hasNode(conflict)) {
                return UnlockResult.fail(Component.text("競合するノードを習得済みです。", NamedTextColor.RED));
            }
        }
        for (SkillTreeNode learnedNode : treeRegistry.getNodes()) {
            if (treeData.hasNode(learnedNode.getId()) && learnedNode.getConflicts().contains(nodeId)) {
                return UnlockResult.fail(Component.text("習得済みノードと競合しています。", NamedTextColor.RED));
            }
        }

        int cost = node.getCostPerLevel();
        if (treeData.getSkillPoints() < cost) {
            return UnlockResult.fail(Component.text("スキルポイントが不足しています。", NamedTextColor.RED));
        }

        treeData.setSkillPoints(treeData.getSkillPoints() - cost);
        int newLevel = currentLevel + 1;
        treeData.setNodeLevel(nodeId, newLevel);
        data.markDirty(CharacterSkillTreeProvider.KEY);
        characterDataRepository.save(characterId, data);

        recalculatePassives(player);
        Bukkit.getPluginManager().callEvent(new SkillTreeNodeUnlockEvent(player, tree, node, newLevel));
        return UnlockResult.success(Component.text("ノード習得: " + node.getDisplayName(), NamedTextColor.GREEN));
    }

    public void saveCamera(Player player, String treeId, int x, int y) {
        characterService.getActiveCharacter(player.getUniqueId()).ifPresent(c -> {
            characterDataRepository.get(c.characterId()).ifPresent(data -> {
                CharacterSkillTreeProvider.SkillTreeData treeData = data.get(CharacterSkillTreeProvider.KEY);
                if (treeData == null) return;
                treeData.setCameraPosition(treeId, x, y);
                data.markDirty(CharacterSkillTreeProvider.KEY);
                characterDataRepository.save(c.characterId(), data);
            });
        });
    }

    public CharacterSkillTreeProvider.CameraPosition getCamera(Player player, String treeId) {
        var characterOpt = characterService.getActiveCharacter(player.getUniqueId());
        if (characterOpt.isEmpty()) {
            return new CharacterSkillTreeProvider.CameraPosition(0, 0);
        }
        var dataOpt = characterDataRepository.get(characterOpt.get().characterId());
        if (dataOpt.isEmpty()) {
            return new CharacterSkillTreeProvider.CameraPosition(0, 0);
        }
        CharacterSkillTreeProvider.SkillTreeData treeData = dataOpt.get().get(CharacterSkillTreeProvider.KEY);
        return treeData == null ? new CharacterSkillTreeProvider.CameraPosition(0, 0) : treeData.getCameraPosition(treeId);
    }

    public void recalculatePassives(Player player) {
        UUID uuid = player.getUniqueId();
        characterService.getActiveCharacter(uuid).ifPresent(c -> {
            var dataOpt = characterDataRepository.get(c.characterId());
            if (dataOpt.isEmpty()) return;
            CharacterSkillTreeProvider.SkillTreeData treeData = dataOpt.get().get(CharacterSkillTreeProvider.KEY);
            if (treeData == null) return;

            for (SkillTreeNode node : treeRegistry.getNodes()) {
                if (node.getType() != SkillTreeNodeType.PASSIVE) continue;
                SkillTreeDefinition tree = treeRegistry.getTreeByNode(node.getId());
                SkillTreeContext context = new SkillTreeContext(player, tree, node, treeData);
                node.getPassiveEffect().clear(player, context);
            }
            for (SkillTreeNode node : treeRegistry.getNodes()) {
                if (node.getType() != SkillTreeNodeType.PASSIVE) continue;
                int level = treeData.getNodeLevel(node.getId());
                if (level <= 0) continue;
                SkillTreeDefinition tree = treeRegistry.getTreeByNode(node.getId());
                SkillTreeContext context = new SkillTreeContext(player, tree, node, treeData);
                node.getPassiveEffect().apply(player, level, context);
            }
        });
    }

    @Override
    public Set<PlayerLifecycleEventType> eventTypes() {
        return Set.of(PlayerLifecycleEventType.JOIN);
    }

    @Override
    public PlayerLifecyclePhase phase() {
        return PlayerLifecyclePhase.STATS;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public CompletableFuture<Void> run(PlayerLifecycleContext context) {
        return context.runSync(() -> context.player().ifPresent(player -> {
            if (characterService.hasCachedActiveCharacter(player.getUniqueId())) {
                recalculatePassives(player);
            }
        }));
    }

    @EventHandler
    public void onCharacterSelect(com.ruskserver.deepwither_V2.modules.character.event.CharacterSelectEvent event) {
        recalculatePassives(event.getPlayer());
    }
}

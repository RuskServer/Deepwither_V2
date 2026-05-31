package com.ruskserver.deepwither_V2.modules.ai.kd;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.ai.embedding.EmbeddingService;
import com.ruskserver.deepwither_V2.modules.ai.embedding.VectorStore;
import com.ruskserver.deepwither_V2.modules.item.ItemManager;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import com.ruskserver.deepwither_V2.modules.skill.service.SkillRegistry;
import com.ruskserver.deepwither_V2.modules.mob.framework.CustomMobManager;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import com.ruskserver.deepwither_V2.modules.skilltree.service.SkillTreeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class KdIndexer implements Startable {

    private final ItemManager itemManager;
    private final SkillRegistry skillRegistry;
    private final CustomMobManager mobManager;
    private final SkillTreeRegistry skillTreeRegistry;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final Logger log;

    @Inject
    public KdIndexer(ItemManager itemManager, SkillRegistry skillRegistry,
                     CustomMobManager mobManager, SkillTreeRegistry skillTreeRegistry,
                     EmbeddingService embeddingService, VectorStore vectorStore,
                     Logger log) {
        this.itemManager = itemManager;
        this.skillRegistry = skillRegistry;
        this.mobManager = mobManager;
        this.skillTreeRegistry = skillTreeRegistry;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.log = log;
        vectorStore.setLogger(log);
    }

    @Override
    public void start() {
        log.info("[KdIndexer] Indexing knowledge documents...");
        vectorStore.clear();

        List<KdDocument> docs = new ArrayList<>();
        docs.addAll(indexItems());
        docs.addAll(indexSkills());
        docs.addAll(indexMobs());
        docs.addAll(indexSkillTrees());

        vectorStore.addAll(docs);
        log.info("[KdIndexer] Indexed " + docs.size() + " documents ("
                + (embeddingService.isAvailable() ? "with embeddings" : "fallback embeddings") + ")");
    }

    public void reindex() {
        start();
    }

    private List<KdDocument> indexItems() {
        List<KdDocument> docs = new ArrayList<>();
        for (String id : itemManager.getRegisteredItemIds()) {
            CustomItem item = itemManager.getCustomItem(id);
            if (item == null) continue;

            StringBuilder text = new StringBuilder();
            text.append("ID: ").append(item.getId()).append("\n");
            text.append("名称: ").append(item.getDisplayName()).append("\n");
            text.append("素材: ").append(item.getMaterial().name()).append("\n");
            text.append("レアリティ: ").append(item.getRarity().getDisplayName()).append("\n");
            if (item.getWeaponType() != null) {
                text.append("武器種: ").append(item.getWeaponType()).append("\n");
            }
            if (item.getFlavorText() != null && !item.getFlavorText().isEmpty()) {
                text.append("説明: ").append(item.getFlavorText()).append("\n");
            }
            if (!item.getBaseStats().isEmpty()) {
                text.append("基本ステータス:\n");
                for (Map.Entry<StatType, Double> entry : item.getBaseStats().entrySet()) {
                    text.append("  ").append(entry.getKey().getDisplayName())
                            .append(": ").append(String.format("%.1f", entry.getValue())).append("\n");
                }
            }
            if (item.getSellPrice() > 0) {
                text.append("売却価格: ").append(String.format("%.0f", item.getSellPrice())).append("\n");
            }

            float[] vec = embeddingService.embed(text.toString());
            docs.add(new KdDocument("item_" + id, "アイテム", item.getDisplayName(), text.toString(), vec));
        }
        return docs;
    }

    private List<KdDocument> indexSkills() {
        List<KdDocument> docs = new ArrayList<>();
        for (Skill skill : skillRegistry.getAll()) {
            StringBuilder text = new StringBuilder();
            text.append("ID: ").append(skill.getId()).append("\n");
            text.append("名称: ").append(skill.getDisplayName()).append("\n");
            text.append("カテゴリ: ").append(skill.getCategory().name()).append("\n");
            text.append("対象: ").append(skill.getTargetType().name()).append("\n");
            if (!skill.getDescription().isEmpty()) {
                text.append("説明:\n");
                for (String line : skill.getDescription()) {
                    text.append("  ").append(line).append("\n");
                }
            }
            if (!skill.getTags().isEmpty()) {
                text.append("タグ: ").append(String.join(", ", skill.getTags())).append("\n");
            }
            if (!skill.getConflicts().isEmpty()) {
                text.append("競合: ").append(String.join(", ", skill.getConflicts())).append("\n");
            }
            text.append("必要レベル: ").append(skill.getRequiredLevel()).append("\n");
            text.append("最大レベル: ").append(skill.getMaxLevel()).append("\n");

            float[] vec = embeddingService.embed(text.toString());
            docs.add(new KdDocument("skill_" + skill.getId(), "スキル", skill.getDisplayName(), text.toString(), vec));
        }
        return docs;
    }

    private List<KdDocument> indexMobs() {
        List<KdDocument> docs = new ArrayList<>();
        for (String mobId : mobManager.getRegisteredMobIds()) {
            StringBuilder text = new StringBuilder();
            text.append("ID: ").append(mobId).append("\n");

            float[] vec = embeddingService.embed(text.toString());
            docs.add(new KdDocument("mob_" + mobId, "モブ", mobId, text.toString(), vec));
        }
        return docs;
    }

    private List<KdDocument> indexSkillTrees() {
        List<KdDocument> docs = new ArrayList<>();
        for (SkillTreeDefinition tree : skillTreeRegistry.getTrees()) {
            StringBuilder text = new StringBuilder();
            text.append("スキルツリーID: ").append(tree.getId()).append("\n");
            text.append("スキルツリー名: ").append(tree.getDisplayName()).append("\n");
            text.append("ノード一覧:\n");
            for (SkillTreeNode node : tree.getNodes()) {
                text.append("  - ").append(node.getDisplayName())
                        .append(" (").append(node.getId()).append(")");
                if (node.getType() != null) {
                    text.append(" [").append(node.getType().name()).append("]");
                }
                text.append("\n");
                for (String descLine : node.getDescription()) {
                    text.append("    ").append(descLine).append("\n");
                }
                if (!node.getRequirements().isEmpty()) {
                    text.append("    前提: ").append(String.join(", ", node.getRequirements())).append("\n");
                }
                if (!node.getConflicts().isEmpty()) {
                    text.append("    競合: ").append(String.join(", ", node.getConflicts())).append("\n");
                }
                if (node.getMaxLevel() > 1) {
                    text.append("    最大Lv: ").append(node.getMaxLevel()).append("\n");
                }
            }

            float[] vec = embeddingService.embed(text.toString());
            docs.add(new KdDocument("tree_" + tree.getId(), "スキルツリー", tree.getDisplayName(), text.toString(), vec));
        }
        return docs;
    }
}

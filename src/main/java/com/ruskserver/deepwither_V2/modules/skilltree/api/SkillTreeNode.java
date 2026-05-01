package com.ruskserver.deepwither_V2.modules.skilltree.api;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SkillTreeNode {

    private final String id;
    private final SkillTreeNodeType type;
    private final String skillId;
    private final String displayName;
    private final List<String> description;
    private final Material icon;
    private final int x;
    private final int y;
    private final int maxLevel;
    private final int costPerLevel;
    private final List<String> requirements;
    private final List<String> conflicts;
    private final SkillTreePassiveEffect passiveEffect;

    private SkillTreeNode(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.skillId = builder.skillId;
        this.displayName = builder.displayName;
        this.description = List.copyOf(builder.description);
        this.icon = builder.icon;
        this.x = builder.x;
        this.y = builder.y;
        this.maxLevel = builder.maxLevel;
        this.costPerLevel = builder.costPerLevel;
        this.requirements = List.copyOf(builder.requirements);
        this.conflicts = List.copyOf(builder.conflicts);
        this.passiveEffect = builder.passiveEffect;
    }

    public static Builder skill(String id, String skillId) {
        return new Builder(id, SkillTreeNodeType.SKILL).skillId(skillId);
    }

    public static Builder passive(String id) {
        return new Builder(id, SkillTreeNodeType.PASSIVE);
    }

    public String getId() {
        return id;
    }

    public SkillTreeNodeType getType() {
        return type;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getCostPerLevel() {
        return costPerLevel;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public SkillTreePassiveEffect getPassiveEffect() {
        return passiveEffect;
    }

    public static final class Builder {
        private final String id;
        private final SkillTreeNodeType type;
        private String skillId;
        private String displayName;
        private List<String> description = new ArrayList<>();
        private Material icon = Material.GRAY_STAINED_GLASS_PANE;
        private int x;
        private int y;
        private int maxLevel = 1;
        private int costPerLevel = 1;
        private List<String> requirements = new ArrayList<>();
        private List<String> conflicts = new ArrayList<>();
        private SkillTreePassiveEffect passiveEffect = SkillTreePassiveEffect.NONE;

        private Builder(String id, SkillTreeNodeType type) {
            this.id = id;
            this.type = type;
            this.displayName = id;
        }

        public Builder skillId(String skillId) {
            this.skillId = skillId;
            return this;
        }

        public Builder name(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String... description) {
            this.description = new ArrayList<>(List.of(description));
            return this;
        }

        public Builder description(List<String> description) {
            this.description = new ArrayList<>(description);
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = Math.max(1, maxLevel);
            return this;
        }

        public Builder costPerLevel(int costPerLevel) {
            this.costPerLevel = Math.max(1, costPerLevel);
            return this;
        }

        public Builder requires(String... requirements) {
            Collections.addAll(this.requirements, requirements);
            return this;
        }

        public Builder conflicts(String... conflicts) {
            Collections.addAll(this.conflicts, conflicts);
            return this;
        }

        public Builder passiveEffect(SkillTreePassiveEffect passiveEffect) {
            this.passiveEffect = passiveEffect == null ? SkillTreePassiveEffect.NONE : passiveEffect;
            return this;
        }

        public SkillTreeNode build() {
            return new SkillTreeNode(this);
        }
    }
}

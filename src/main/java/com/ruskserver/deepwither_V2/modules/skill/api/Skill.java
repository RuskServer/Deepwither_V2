package com.ruskserver.deepwither_V2.modules.skill.api;

import org.bukkit.Material;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface Skill {

    String getId();

    String getDisplayName();

    default List<String> getDescription() {
        return Collections.emptyList();
    }

    default Material getIcon() {
        return Material.BOOK;
    }

    default SkillCategory getCategory() {
        return SkillCategory.ACTIVE;
    }

    default SkillTargetType getTargetType() {
        return SkillTargetType.SELF;
    }

    default Set<String> getTags() {
        return Collections.emptySet();
    }

    default Set<String> getConflicts() {
        return Collections.emptySet();
    }

    default int getMaxLevel() {
        return 1;
    }

    default int getRequiredLevel() {
        return 1;
    }

    default double getManaCost(SkillContext context) {
        return 0.0;
    }

    default Duration getCooldown(SkillContext context) {
        return Duration.ZERO;
    }

    default Duration getCastTime(SkillContext context) {
        return Duration.ZERO;
    }

    CastResult cast(SkillContext context);
}

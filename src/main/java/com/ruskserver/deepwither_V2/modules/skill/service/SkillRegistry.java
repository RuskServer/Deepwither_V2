package com.ruskserver.deepwither_V2.modules.skill.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.container.DIContainer;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.modules.skill.api.Skill;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkillRegistry implements Startable {

    private final DIContainer container;
    private final Map<String, Skill> skills = new LinkedHashMap<>();

    @Inject
    public SkillRegistry(DIContainer container) {
        this.container = container;
    }

    @Override
    public void start() {
        for (Object instance : container.getAllInstances()) {
            if (instance instanceof Skill skill) {
                if (skills.containsKey(skill.getId())) {
                    Bukkit.getLogger().warning("[SkillRegistry] Duplicate skill id skipped: " + skill.getId());
                    continue;
                }
                skills.put(skill.getId(), skill);
            }
        }
        Bukkit.getLogger().info("[SkillRegistry] " + skills.size() + " skills loaded.");
    }

    public Skill get(String id) {
        return skills.get(id);
    }

    public Collection<Skill> getAll() {
        return Collections.unmodifiableCollection(skills.values());
    }

    public List<String> getIds() {
        return new ArrayList<>(skills.keySet());
    }
}

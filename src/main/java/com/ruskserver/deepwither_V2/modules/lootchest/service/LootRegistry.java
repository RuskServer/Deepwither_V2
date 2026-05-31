package com.ruskserver.deepwither_V2.modules.lootchest.service;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.lootchest.api.LootTableDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LootRegistry {

    private final Map<String, LootTableDefinition> definitions = new HashMap<>();

    @Inject
    public LootRegistry(List<LootTableDefinition> definitionList) {
        for (LootTableDefinition def : definitionList) {
            definitions.put(def.getId(), def);
        }
    }

    public LootTableDefinition getDefinition(String id) {
        return definitions.get(id);
    }

    public Map<String, LootTableDefinition> getDefinitions() {
        return definitions;
    }
}

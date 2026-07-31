package com.ruskserver.deepwither_V2.modules.profession;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ProfessionData {

    private final Map<ProfessionType, Long> experience = new EnumMap<>(ProfessionType.class);

    public long getExperience(ProfessionType type) {
        return experience.getOrDefault(type, 0L);
    }

    public void setExperience(ProfessionType type, long amount) {
        experience.put(type, Math.max(0L, amount));
    }

    public void addExperience(ProfessionType type, long amount) {
        setExperience(type, getExperience(type) + Math.max(0L, amount));
    }

    public Map<ProfessionType, Long> getAllExperience() {
        return Collections.unmodifiableMap(experience);
    }
}

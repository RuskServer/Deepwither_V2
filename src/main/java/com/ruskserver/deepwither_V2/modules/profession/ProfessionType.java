package com.ruskserver.deepwither_V2.modules.profession;

public enum ProfessionType {
    MINING("採掘"),
    FISHING("釣り");

    private final String displayName;

    ProfessionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

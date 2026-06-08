package com.ruskserver.deepwither_V2.modules.artifact;

import java.util.HashMap;
import java.util.Map;

public class ArtifactSaveData {
    private Map<Integer, String> equippedArtifacts = new HashMap<>();

    public Map<Integer, String> getEquippedArtifacts() {
        if (equippedArtifacts == null) {
            equippedArtifacts = new HashMap<>();
        }
        return equippedArtifacts;
    }

    public boolean isEmpty() {
        return equippedArtifacts == null || equippedArtifacts.isEmpty();
    }

    public void setEquippedArtifact(int slot, String base64) {
        if (equippedArtifacts == null) {
            equippedArtifacts = new HashMap<>();
        }
        if (base64 == null) {
            equippedArtifacts.remove(slot);
        } else {
            equippedArtifacts.put(slot, base64);
        }
    }
}

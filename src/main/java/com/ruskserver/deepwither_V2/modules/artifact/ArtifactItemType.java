package com.ruskserver.deepwither_V2.modules.artifact;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.Material;

public enum ArtifactItemType {

    // ABYSS_PULSATION
    ABYSS_HEART("§d§l虚無を喰らう心臓", ArtifactSetType.ABYSS_PULSATION, 
            Material.PLAYER_HEAD, "77d8ca152aab772d6019ffe8b029d6b4ffa5c61cbc53a52ec20d65120ac8345f", 
            StatType.HEALTH, 20.0), // Base is % in yaml, but let's just use flat or percentage mapped properly
    ABYSS_CONDUIT("§d§l星界の導管", ArtifactSetType.ABYSS_PULSATION, 
            Material.PLAYER_HEAD, "d909a565e705d66ba806fa3022c7f323f595f6eb29be3e2a3eebffa85513ecab", 
            StatType.MAGIC_DAMAGE, 10.0),

    // CELESTIAL_RESONANCE
    CELESTIAL_DISK("§d§l虚環の共鳴盤", ArtifactSetType.CELESTIAL_RESONANCE, 
            Material.PLAYER_HEAD, "ef0a64dc76a6567037fd9fe2d40a9803f0fea8931f1c6424ea48cab5f62950ee", 
            StatType.MAX_MANA, 40.0),
    CELESTIAL_ORB("§d§l蒼虚の導管", ArtifactSetType.CELESTIAL_RESONANCE, 
            Material.PLAYER_HEAD, "d909a565e705d66ba806fa3022c7f323f595f6eb29be3e2a3eebffa85513ecab", 
            StatType.MAGIC_DAMAGE, 15.0),

    // FAULT_LINE
    FAULT_RING("§d§l断層の輪", ArtifactSetType.FAULT_LINE, 
            Material.PLAYER_HEAD, "d936bb1cc4ab6ecce65b64298394fafc5fe3f7876d7c941d05a9294fa392b7c", 
            StatType.ATTACK_DAMAGE, 10.0),
    FAULT_WEDGE("§d§l裂輝の楔", ArtifactSetType.FAULT_LINE, 
            Material.PLAYER_HEAD, "98ec21d20f1aaf5635f48beda88626403c6385b81673fb7cfa7ff82179c63e39", 
            StatType.CRITICAL_DAMAGE, 20.0),

    // ASTRAL_STEEL_GUARD
    ASTRAL_SEAL("§d§l星盾の印", ArtifactSetType.ASTRAL_STEEL_GUARD, 
            Material.PLAYER_HEAD, "3ae923cbf3fab8f97418106ee1da83b0709006f540c78113e72db048d6dfb08b", 
            StatType.MAGIC_DEFENSE, 15.0),
    ASTRAL_CORE("§d§l再流の核", ArtifactSetType.ASTRAL_STEEL_GUARD, 
            Material.PLAYER_HEAD, "3cdaa5bf0bc542050c734c58f4283215c9a4f0f18c3ddad871913dbf667ce433", 
            StatType.DEFENSE, 20.0),

    // LUNAR_SKIRMISHER
    LUNAR_RING("§d§l月駆の紋輪", ArtifactSetType.LUNAR_SKIRMISHER, 
            Material.PLAYER_HEAD, "31f748a43f3b3ada04f44d5d290a8b9bf583d93e1c83ab93c60c4dec1fde1c5c", 
            StatType.SPEED, 0.05),
    LUNAR_CIRCUIT("§d§lアークレイン・サーキット", ArtifactSetType.LUNAR_SKIRMISHER, 
            Material.PLAYER_HEAD, "98ec21d20f1aaf5635f48beda88626403c6385b81673fb7cfa7ff82179c63e39", 
            StatType.CRITICAL_CHANCE, 5.0),

    // ETERNAL_HEARTS
    ETERNAL_RUBY("§c§l紅玉の心臓", ArtifactSetType.ETERNAL_HEARTS, 
            Material.PLAYER_HEAD, "3cdaa5bf0bc542050c734c58f4283215c9a4f0f18c3ddad871913dbf667ce433", 
            StatType.HEALTH, 30.0),
    ETERNAL_GEM("§c§l不朽の宝玉", ArtifactSetType.ETERNAL_HEARTS, 
            Material.PLAYER_HEAD, "77d8ca152aab772d6019ffe8b029d6b4ffa5c61cbc53a52ec20d65120ac8345f", 
            StatType.DEFENSE, 25.0);

    private final String displayName;
    private final ArtifactSetType setType;
    private final Material defaultMaterial;
    private final String textureUrl;
    private final StatType mainStatType;
    private final double mainStatValue;

    ArtifactItemType(String displayName, ArtifactSetType setType, Material defaultMaterial, String textureUrl, StatType mainStatType, double mainStatValue) {
        this.displayName = displayName;
        this.setType = setType;
        this.defaultMaterial = defaultMaterial;
        this.textureUrl = textureUrl;
        this.mainStatType = mainStatType;
        this.mainStatValue = mainStatValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ArtifactSetType getSetType() {
        return setType;
    }

    public Material getDefaultMaterial() {
        return defaultMaterial;
    }

    public String getTextureUrl() {
        return textureUrl;
    }

    public StatType getMainStatType() {
        return mainStatType;
    }

    public double getMainStatValue() {
        return mainStatValue;
    }
}

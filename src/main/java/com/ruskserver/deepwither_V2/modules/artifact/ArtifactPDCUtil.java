package com.ruskserver.deepwither_V2.modules.artifact;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ArtifactPDCUtil {

    private final NamespacedKey setTypeKey;
    private final NamespacedKey mainStatKey;
    private final NamespacedKey subStatsKey;
    private final NamespacedKey artifactFlagKey;

    @Inject
    public ArtifactPDCUtil(Deepwither_V2 plugin) {
        this.setTypeKey = new NamespacedKey(plugin, "artifact_set_type");
        this.mainStatKey = new NamespacedKey(plugin, "artifact_main_stat");
        this.subStatsKey = new NamespacedKey(plugin, "artifact_sub_stats");
        this.artifactFlagKey = new NamespacedKey(plugin, "is_artifact");
    }

    public void setArtifactData(ItemStack item, ArtifactData data) {
        if (item == null || item.getItemMeta() == null || data == null) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(artifactFlagKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(setTypeKey, PersistentDataType.STRING, data.getItemType().name());

        if (data.getSubStats() != null && !data.getSubStats().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<StatType, Double> entry : data.getSubStats().entrySet()) {
                if (!sb.isEmpty()) sb.append(",");
                sb.append(entry.getKey().name()).append(":").append(entry.getValue());
            }
            pdc.set(subStatsKey, PersistentDataType.STRING, sb.toString());
        } else {
            pdc.remove(subStatsKey);
        }

        item.setItemMeta(meta);
    }

    public ArtifactData getArtifactData(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        if (!pdc.has(artifactFlagKey, PersistentDataType.BYTE)) return null;

        String itemTypeStr = pdc.get(setTypeKey, PersistentDataType.STRING);

        if (itemTypeStr == null) return null;

        try {
            ArtifactItemType itemType = ArtifactItemType.valueOf(itemTypeStr);

            Map<StatType, Double> subStats = new EnumMap<>(StatType.class);
            String subStatsStr = pdc.get(subStatsKey, PersistentDataType.STRING);
            if (subStatsStr != null && !subStatsStr.isEmpty()) {
                String[] parts = subStatsStr.split(",");
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length == 2) {
                        subStats.put(StatType.valueOf(kv[0]), Double.parseDouble(kv[1]));
                    }
                }
            }

            return new ArtifactData(itemType, subStats);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return null; // データが破損している場合
        }
    }

    public boolean isArtifact(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(artifactFlagKey, PersistentDataType.BYTE);
    }
}

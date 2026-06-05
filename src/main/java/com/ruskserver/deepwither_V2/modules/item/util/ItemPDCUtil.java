package com.ruskserver.deepwither_V2.modules.item.util;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.modifier.ModifierRollResult;
import com.ruskserver.deepwither_V2.modules.item.modifier.SpecialEffect;
import com.ruskserver.deepwither_V2.modules.item.modifier.SpecialEffectInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

@Component
public class ItemPDCUtil {

    private final NamespacedKey idKey;
    private final NamespacedKey modifierKey;
    private final NamespacedKey addedStatKey;
    private final NamespacedKey specialEffectKey;

    @Inject
    public ItemPDCUtil(Deepwither_V2 plugin) {
        this.idKey = new NamespacedKey(plugin, "custom_item_id");
        this.modifierKey = new NamespacedKey(plugin, "custom_item_modifiers");
        this.addedStatKey = new NamespacedKey(plugin, "custom_item_added_stats");
        this.specialEffectKey = new NamespacedKey(plugin, "custom_item_special_effects");
    }

    public void setItemId(ItemStack item, String id) {
        if (item == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
    }

    public String getItemId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public void setModifiers(ItemStack item, ModifierRollResult result) {
        if (item == null || item.getItemMeta() == null || result == null) return;
        ItemMeta meta = item.getItemMeta();

        setBaseModifiers(meta, result.getBaseModifiers());
        setAddedStats(meta, result.getAddedStats());
        setSpecialEffects(meta, result.getSpecialEffects());

        item.setItemMeta(meta);
    }

    private void setBaseModifiers(ItemMeta meta, Map<StatType, Double> modifiers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<StatType, Double> entry : modifiers.entrySet()) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        meta.getPersistentDataContainer().set(modifierKey, PersistentDataType.STRING, sb.toString());
    }

    private void setAddedStats(ItemMeta meta, Map<StatType, Double> addedStats) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<StatType, Double> entry : addedStats.entrySet()) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        meta.getPersistentDataContainer().set(addedStatKey, PersistentDataType.STRING, sb.toString());
    }

    private void setSpecialEffects(ItemMeta meta, List<SpecialEffectInstance> effects) {
        StringBuilder sb = new StringBuilder();
        for (SpecialEffectInstance effect : effects) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(effect.getEffect().name()).append(":").append(effect.getLevel());
        }
        meta.getPersistentDataContainer().set(specialEffectKey, PersistentDataType.STRING, sb.toString());
    }

    public Map<StatType, Double> getModifiers(ItemStack item) {
        return deserializeStatMap(item, modifierKey);
    }

    public Map<StatType, Double> getAddedStats(ItemStack item) {
        return deserializeStatMap(item, addedStatKey);
    }

    public List<SpecialEffectInstance> getSpecialEffects(ItemStack item) {
        List<SpecialEffectInstance> result = new ArrayList<>();
        if (item == null || item.getItemMeta() == null) return result;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String raw = pdc.get(specialEffectKey, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return result;

        String[] parts = raw.split(",");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                try {
                    SpecialEffect effect = SpecialEffect.valueOf(kv[0]);
                    int level = Integer.parseInt(kv[1]);
                    result.add(new SpecialEffectInstance(effect, level));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }

    private Map<StatType, Double> deserializeStatMap(ItemStack item, NamespacedKey key) {
        Map<StatType, Double> result = new EnumMap<>(StatType.class);
        if (item == null || item.getItemMeta() == null) return result;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String raw = pdc.get(key, PersistentDataType.STRING);

        if (raw != null && !raw.isEmpty()) {
            String[] parts = raw.split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    try {
                        StatType type = StatType.valueOf(kv[0]);
                        double value = Double.parseDouble(kv[1]);
                        result.put(type, value);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return result;
    }
}

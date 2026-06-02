package com.ruskserver.deepwither_V2.modules.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GuiContext {

    public static final GuiContext EMPTY = new GuiContext(Map.of());

    private final Map<String, String> values;

    public GuiContext(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    public Map<String, String> values() {
        return values;
    }

    public String getString(String key) {
        return values.get(key);
    }

    public int getInt(String key, int defaultValue) {
        String value = values.get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = values.get(key);
        if (value == null) return defaultValue;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return defaultValue;
    }

    public UUID getUuid(String key) {
        String value = values.get(key);
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, String> values = new HashMap<>();

        public Builder put(String key, String value) {
            if (value != null) values.put(key, value);
            return this;
        }

        public Builder put(String key, int value) {
            values.put(key, Integer.toString(value));
            return this;
        }

        public Builder put(String key, long value) {
            values.put(key, Long.toString(value));
            return this;
        }

        public Builder put(String key, boolean value) {
            values.put(key, Boolean.toString(value));
            return this;
        }

        public Builder put(String key, UUID value) {
            if (value != null) values.put(key, value.toString());
            return this;
        }

        public GuiContext build() {
            return new GuiContext(values);
        }
    }
}

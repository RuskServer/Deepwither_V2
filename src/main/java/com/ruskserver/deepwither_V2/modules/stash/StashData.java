package com.ruskserver.deepwither_V2.modules.stash;

import java.util.HashMap;
import java.util.Map;

public class StashData {

    public static final int DEFAULT_UNLOCKED_PAGES = 1;

    private int unlockedPages = DEFAULT_UNLOCKED_PAGES;
    private boolean nativeEnderChestMigrated;
    private Map<Integer, String> items = new HashMap<>();

    public StashData() {
    }

    public StashData(StashData source) {
        this.unlockedPages = source.unlockedPages;
        this.nativeEnderChestMigrated = source.nativeEnderChestMigrated;
        this.items = new HashMap<>(source.items);
    }

    public int getUnlockedPages() {
        return unlockedPages;
    }

    public void setUnlockedPages(int unlockedPages) {
        this.unlockedPages = Math.max(DEFAULT_UNLOCKED_PAGES, unlockedPages);
    }

    public boolean isNativeEnderChestMigrated() {
        return nativeEnderChestMigrated;
    }

    public void setNativeEnderChestMigrated(boolean nativeEnderChestMigrated) {
        this.nativeEnderChestMigrated = nativeEnderChestMigrated;
    }

    public Map<Integer, String> getItems() {
        if (items == null) {
            items = new HashMap<>();
        }
        return items;
    }

    public void setItems(Map<Integer, String> items) {
        this.items = items != null ? new HashMap<>(items) : new HashMap<>();
    }

    public StashData copy() {
        return new StashData(this);
    }
}

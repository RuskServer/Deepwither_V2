package com.ruskserver.deepwither_V2.modules.ai.kd;

import java.util.List;

public class KdDocument {

    private final String id;
    private final String category;
    private final String title;
    private final String text;
    private final float[] vector;

    public KdDocument(String id, String category, String title, String text, float[] vector) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.text = text;
        this.vector = vector;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public float[] getVector() { return vector; }

    public String toKdText() {
        return "【" + category + "】" + title + "\n" + text;
    }
}

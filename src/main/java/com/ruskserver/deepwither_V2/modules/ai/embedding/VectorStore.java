package com.ruskserver.deepwither_V2.modules.ai.embedding;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.ai.kd.KdDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

@Service
public class VectorStore {

    private final List<KdDocument> documents = new ArrayList<>();
    private Logger log;

    public void setLogger(Logger log) {
        this.log = log;
    }

    public void add(KdDocument doc) {
        documents.add(doc);
    }

    public void addAll(List<KdDocument> docs) {
        documents.addAll(docs);
    }

    public void clear() {
        documents.clear();
    }

    public int size() {
        return documents.size();
    }

    public List<KdDocument> search(float[] queryVec, int topK) {
        if (queryVec == null || documents.isEmpty()) {
            return List.of();
        }

        List<ScoredDoc> scored = new ArrayList<>(documents.size());
        for (KdDocument doc : documents) {
            double sim = cosineSimilarity(queryVec, doc.getVector());
            scored.add(new ScoredDoc(doc, sim));
        }

        scored.sort(Comparator.comparingDouble(ScoredDoc::score).reversed());

        int limit = Math.min(topK, scored.size());
        List<KdDocument> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            result.add(scored.get(i).doc);
        }

        if (log != null) {
            log.fine("[VectorStore] searched " + documents.size() + " docs, top-1 similarity="
                    + (limit > 0 ? String.format("%.4f", scored.get(0).score) : "N/A"));
        }

        return result;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    private record ScoredDoc(KdDocument doc, double score) {}
}

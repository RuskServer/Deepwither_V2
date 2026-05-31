package com.ruskserver.deepwither_V2.modules.ai.kd;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.modules.ai.embedding.EmbeddingService;
import com.ruskserver.deepwither_V2.modules.ai.embedding.VectorStore;

import java.util.List;

@Service
public class KdRetriever {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    @Inject
    public KdRetriever(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public List<KdDocument> retrieve(String query, int topK) {
        float[] queryVec = embeddingService.embed(query);
        return vectorStore.search(queryVec, topK);
    }

    public String retrieveAsContext(String query, int topK) {
        List<KdDocument> docs = retrieve(query, topK);
        if (docs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【参考情報】\n\n");
        for (KdDocument doc : docs) {
            sb.append(doc.toKdText()).append("\n\n");
        }
        return sb.toString();
    }
}

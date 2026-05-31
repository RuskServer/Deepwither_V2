package com.ruskserver.deepwither_V2.modules.ai.embedding;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.bert.BertTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

@Service
public class EmbeddingService implements Startable, Stoppable {

    private static final int DEFAULT_DIMENSION = 384;

    private final Logger log;
    private final String modelPath;

    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;
    private boolean available = false;

    @Inject
    public EmbeddingService(Logger log) {
        this.log = log;
        this.modelPath = "plugins/Deepwither_V2/models/gemma-300m-onnx";
    }

    @Override
    public void start() {
        try {
            Path modelDir = Paths.get(modelPath);
            if (!modelDir.toFile().exists()) {
                log.warning("[EmbeddingService] Model not found at " + modelPath + ". Embedding will be disabled.");
                return;
            }

            Criteria<String, float[]> criteria = Criteria.builder()
                    .optApplication(Application.NLP.TEXT_EMBEDDING)
                    .setTypes(String.class, float[].class)
                    .optModelPath(modelDir)
                    .optEngine("OnnxRuntime")
                    .optProgress(new ProgressBar())
                    .build();

            model = criteria.loadModel();
            predictor = model.newPredictor();
            available = true;
            log.info("[EmbeddingService] Model loaded successfully from " + modelPath);
        } catch (ModelException | IOException e) {
            log.warning("[EmbeddingService] Failed to load model: " + e.getMessage() + ". Embedding disabled.");
            available = false;
        }
    }

    @Override
    public void stop() {
        if (predictor != null) predictor.close();
        if (model != null) model.close();
        available = false;
    }

    public float[] embed(String text) {
        if (!available || predictor == null) {
            return fallbackEmbed(text);
        }
        try {
            return predictor.predict(text);
        } catch (Exception e) {
            log.warning("[EmbeddingService] Embedding failed: " + e.getMessage());
            return fallbackEmbed(text);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    private float[] fallbackEmbed(String text) {
        float[] vec = new float[DEFAULT_DIMENSION];
        int hash = text.hashCode();
        for (int i = 0; i < DEFAULT_DIMENSION; i++) {
            vec[i] = (float) Math.sin(hash * (i + 1) * 0.1);
        }
        double norm = 0;
        for (float v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        }
        return vec;
    }
}

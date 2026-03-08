package com.lifeentity.perception;

import android.util.Log;

import com.lifeentity.memory.MemoryDao;
import com.lifeentity.memory.SemanticEmbeddings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EmbeddingsEngine {
    private static final String TAG = "EmbeddingsEngine";
    private static final int SIZE = 128;

    private MemoryDao memoryDao;
    private Map<String, float[]> randomCache;

    public EmbeddingsEngine(MemoryDao dao) {
        this.memoryDao = dao;
        this.randomCache = new HashMap<>();
        Log.i(TAG, "EmbeddingsEngine initialized with database");
    }

    public float[] getEmbedding(String word) {
        word = normalize(word);
        if (word.isEmpty()) return getRandomVector("empty");

        // ✅ استخدم EmbeddingEntity
        SemanticEmbeddings.EmbeddingEntity entity = memoryDao.getEmbedding(word);
        if (entity != null && entity.vector != null) {
            return entity.vector;
        }

        return getConsistentRandom(word);
    }

    public void learnAssociation(String word, float[] visualVector) {
        word = normalize(word);
        if (word.isEmpty() || visualVector == null) return;

        float[] wordVec = getEmbedding(word);
        float[] fused = new float[SIZE];

        for (int i = 0; i < SIZE; i++) {
            float v = (i < visualVector.length) ? visualVector[i % visualVector.length] : 0;
            fused[i] = wordVec[i] * 0.6f + v * 0.4f;
        }

        String conceptKey = "concept:" + word;
        // ✅ استخدم EmbeddingEntity
        SemanticEmbeddings.EmbeddingEntity entity = new SemanticEmbeddings.EmbeddingEntity(conceptKey, fused);
        memoryDao.saveEmbedding(entity);

        Log.i(TAG, "Learned association for: " + word);
    }

    public String findVisualConcept(float[] visualVector) {
        // ✅ استخدم EmbeddingEntity
        List<SemanticEmbeddings.EmbeddingEntity> allEmbeddings = memoryDao.getRecentEmbeddings();
        String bestConcept = null;
        float bestSimilarity = -1f;

        for (SemanticEmbeddings.EmbeddingEntity e : allEmbeddings) {
            if (e.concept.startsWith("concept:")) {
                float sim = cosineSimilarity(visualVector, e.vector);
                if (sim > bestSimilarity) {
                    bestSimilarity = sim;
                    bestConcept = e.concept.replace("concept:", "");
                }
            }
        }

        return (bestSimilarity > 0.6f) ? bestConcept : null;
    }

    private float[] getConsistentRandom(String word) {
        if (randomCache.containsKey(word)) {
            return randomCache.get(word);
        }

        Random r = new Random(word.hashCode());
        float[] vec = new float[SIZE];
        for (int i = 0; i < SIZE; i++) {
            vec[i] = r.nextFloat() * 2 - 1;
        }
        normalize(vec);
        randomCache.put(word, vec);
        return vec;
    }

    private void normalize(float[] v) {
        float sum = 0;
        for (float f : v) sum += f * f;
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, na = 0, nb = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / ((float) Math.sqrt(na * nb) + 0.0001f);
    }

    private String normalize(String w) {
        if (w == null) return "";
        return w.toLowerCase()
                .replaceAll("[\\u064B-\\u065F]", "")
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .trim();
    }

    private float[] getRandomVector(String seed) {
        return getConsistentRandom(seed);
    }
}

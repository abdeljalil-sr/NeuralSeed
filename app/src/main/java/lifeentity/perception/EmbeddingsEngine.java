package com.lifeentity.perception;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class EmbeddingsEngine {
    private static final String TAG = "Embeddings";
    private static final int SIZE = 128;
    
    private Map<String, float[]> embeddings;
    private Map<String, float[]> randomCache;
    
    public EmbeddingsEngine(Context context) {
        embeddings = new HashMap<>();
        randomCache = new HashMap<>();
    }
    
    public float[] getEmbedding(String word) {
        word = normalize(word);
        
        if (embeddings.containsKey(word)) {
            return embeddings.get(word);
        }
        
        return getConsistentRandom(word);
    }
    
    public void learnAssociation(String word, float[] visual) {
        float[] wordVec = getEmbedding(word);
        float[] fused = new float[SIZE];
        
        for (int i = 0; i < SIZE; i++) {
            float v = (i < visual.length) ? visual[i % visual.length] : 0;
            fused[i] = wordVec[i] * 0.6f + v * 0.4f;
        }
        
        embeddings.put("concept:" + word, fused);
        Log.i(TAG, "Learned: " + word);
    }
    
    public String findVisualConcept(float[] visual) {
        String best = null;
        float bestSim = -1;
        
        for (Map.Entry<String, float[]> e : embeddings.entrySet()) {
            if (e.getKey().startsWith("concept:")) {
                float sim = cosineSimilarity(visual, e.getValue());
                if (sim > bestSim) {
                    bestSim = sim;
                    best = e.getKey().replace("concept:", "");
                }
            }
        }
        
        return bestSim > 0.6 ? best : null;
    }
    
    private float[] getConsistentRandom(String word) {
        if (randomCache.containsKey(word)) {
            return randomCache.get(word);
        }
        
        java.util.Random r = new java.util.Random(word.hashCode());
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
        for (int i = 0; i < v.length; i++) v[i] /= norm;
    }
    
    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, na = 0, nb = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / ((float)Math.sqrt(na * nb) + 0.0001f);
    }
    
    private String normalize(String w) {
        return w.toLowerCase()
            .replaceAll("[\\u064B-\\u065F]", "")
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
            .replace("ة", "ه");
    }
}

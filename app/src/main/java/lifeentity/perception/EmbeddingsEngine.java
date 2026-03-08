package com.lifeentity.perception;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.lifeentity.memory.MemoryDao;
import com.lifeentity.memory.SemanticEmbeddings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmbeddingsEngine {
    private static final String TAG = "EmbeddingsEngine";
    private static final int SIZE = 128;

    private MemoryDao memoryDao;
    private Map<String, float[]> randomCache;
    private ExecutorService dbExecutor; // ✅ منفذ للعمليات في الخلفية
    private Handler mainHandler; // ✅ للعودة إلى الخيط الرئيسي إذا لزم الأمر

    public EmbeddingsEngine(MemoryDao dao) {
        this.memoryDao = dao;
        this.randomCache = new HashMap<>();
        this.dbExecutor = Executors.newSingleThreadExecutor(); // ✅ خيط واحد لعمليات قاعدة البيانات
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.i(TAG, "EmbeddingsEngine initialized with database");
    }

    // ✅ دالة غير متزامنة لتعلم الارتباط (لا تنتظر النتيجة)
    public void learnAssociationAsync(String word, float[] visualVector) {
        dbExecutor.execute(() -> {
            learnAssociation(word, visualVector);
        });
    }

    // ✅ دالة متزامنة (تستخدم داخلياً في الخلفية)
    private void learnAssociation(String word, float[] visualVector) {
        word = normalize(word);
        if (word.isEmpty() || visualVector == null) return;

        float[] wordVec = getEmbedding(word); // هذه تستدعي قاعدة البيانات
        float[] fused = new float[SIZE];

        for (int i = 0; i < SIZE; i++) {
            float v = (i < visualVector.length) ? visualVector[i % visualVector.length] : 0;
            fused[i] = wordVec[i] * 0.6f + v * 0.4f;
        }

        String conceptKey = "concept:" + word;
        SemanticEmbeddings.EmbeddingEntity entity = new SemanticEmbeddings.EmbeddingEntity(conceptKey, fused);
        memoryDao.saveEmbedding(entity); // ✅ هذا في الخلفية لأننا داخل dbExecutor

        Log.i(TAG, "Learned association for: " + word);
    }

    // ✅ دالة غير متزامنة مع Callback للحصول على النتيجة
    public void getEmbeddingAsync(String word, EmbeddingCallback callback) {
        dbExecutor.execute(() -> {
            float[] result = getEmbedding(word);
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    // ✅ دالة متزامنة (تستخدم داخلياً في الخلفية)
    private float[] getEmbedding(String word) {
        word = normalize(word);
        if (word.isEmpty()) return getRandomVector("empty");

        SemanticEmbeddings.EmbeddingEntity entity = memoryDao.getEmbedding(word);
        if (entity != null && entity.vector != null) {
            return entity.vector;
        }

        return getConsistentRandom(word);
    }

    public void findVisualConceptAsync(float[] visualVector, FindConceptCallback callback) {
        dbExecutor.execute(() -> {
            String result = findVisualConcept(visualVector);
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    private String findVisualConcept(float[] visualVector) {
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

    // ====================== دوال مساعدة ======================
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

    // ====================== واجهات Callback ======================
    public interface EmbeddingCallback {
        void onResult(float[] embedding);
    }

    public interface FindConceptCallback {
        void onResult(String concept);
    }

    // إيقاف التشغيل (يُستدعى عند إغلاق التطبيق)
    public void shutdown() {
        dbExecutor.shutdown();
    }
}

package com.lifeentity.perception;

import android.content.Context;
import android.util.Log;

import com.lifeentity.memory.MemoryDao;
import com.lifeentity.memory.SemanticEmbeddings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * محرك التضمينات (Embeddings) – مسؤول عن إدارة متجهات المعاني للمفاهيم.
 * يعتمد على قاعدة بيانات Room لتخزين واسترجاع المتجهات، مع توليد متجهات عشوائية متناسقة
 * للمفاهيم غير المعروفة (لا توجد في قاعدة البيانات).
 */
public class EmbeddingsEngine {
    private static final String TAG = "EmbeddingsEngine";
    private static final int SIZE = 128;  // حجم المتجه (يمكن تغييره حسب النموذج المستخدم)

    private MemoryDao memoryDao;           // للوصول إلى جدول embeddings في قاعدة البيانات
    private Map<String, float[]> randomCache;  // تخزين المتجهات العشوائية للمفاهيم غير المعروفة

    public EmbeddingsEngine(MemoryDao dao) {
        this.memoryDao = dao;
        this.randomCache = new HashMap<>();
        Log.i(TAG, "EmbeddingsEngine initialized with database");
    }

    /**
     * استرجاع متجه مفهوم (word) من قاعدة البيانات، أو توليد متجه عشوائي متناسق إذا لم يوجد.
     * @param word المفهوم (كلمة أو عبارة)
     * @return متجه float[] بطول SIZE
     */
    public float[] getEmbedding(String word) {
        word = normalize(word);
        if (word.isEmpty()) return getRandomVector("empty");

        // البحث في قاعدة البيانات
        SemanticEmbeddings.Entity entity = memoryDao.getEmbedding(word);
        if (entity != null && entity.vector != null) {
            return entity.vector;
        }

        // إذا لم يوجد، نولد متجه عشوائي متناسق
        return getConsistentRandom(word);
    }

    /**
     * تعلم ربط مفهوم بمتجه بصري (دمج المتجه اللفظي والبصري) وتخزينه في قاعدة البيانات.
     * @param word المفهوم اللفظي
     * @param visualVector المتجه البصري (من VisualMemory مثلاً)
     */
    public void learnAssociation(String word, float[] visualVector) {
        word = normalize(word);
        if (word.isEmpty() || visualVector == null) return;

        float[] wordVec = getEmbedding(word);  // قد يكون عشوائياً
        float[] fused = new float[SIZE];

        // دمج المتجهين (وزن 60% للفظي، 40% للبصري)
        for (int i = 0; i < SIZE; i++) {
            float v = (i < visualVector.length) ? visualVector[i % visualVector.length] : 0;
            fused[i] = wordVec[i] * 0.6f + v * 0.4f;
        }

        // تخزين في قاعدة البيانات كمفهوم جديد (ببادئة concept:)
        String conceptKey = "concept:" + word;
        SemanticEmbeddings.Entity entity = new SemanticEmbeddings.Entity(conceptKey, fused);
        memoryDao.saveEmbedding(entity);

        Log.i(TAG, "Learned association for: " + word);
    }

    /**
     * البحث عن أقرب مفهوم لمتجه بصري معين.
     * @param visualVector المتجه البصري
     * @return اسم المفهوم الأقرب (أو null إذا لم يوجد قريب كافٍ)
     */
    public String findVisualConcept(float[] visualVector) {
        List<SemanticEmbeddings.Entity> allEmbeddings = memoryDao.getRecentEmbeddings(); // كل المفاهيم
        String bestConcept = null;
        float bestSimilarity = -1f;

        for (SemanticEmbeddings.Entity e : allEmbeddings) {
            // نبحث فقط في المفاهيم التي تبدأ بـ "concept:" (المرتبطة بصرية)
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

    /**
     * توليد متجه عشوائي متناسق لمفهوم معين (يستخدم cache لضمان نفس المتجه لنفس الكلمة).
     */
    private float[] getConsistentRandom(String word) {
        if (randomCache.containsKey(word)) {
            return randomCache.get(word);
        }

        Random r = new Random(word.hashCode());
        float[] vec = new float[SIZE];
        for (int i = 0; i < SIZE; i++) {
            vec[i] = r.nextFloat() * 2 - 1;  // [-1, 1]
        }
        normalize(vec);
        randomCache.put(word, vec);
        return vec;
    }

    /**
     * تطبيع المتجه (جعله طوله 1).
     */
    private void normalize(float[] v) {
        float sum = 0;
        for (float f : v) sum += f * f;
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
    }

    /**
     * حساب التشابه بجيب التمام (cosine similarity) بين متجهين.
     */
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

    /**
     * تطبيع النص العربي: إزالة الحركات، توحيد أشكال الألف، تحويل التاء المربوطة إلى هاء.
     */
    private String normalize(String w) {
        if (w == null) return "";
        return w.toLowerCase()
                .replaceAll("[\\u064B-\\u065F]", "")  // إزالة الحركات
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .trim();
    }

    /**
     * الحصول على متجه عشوائي (للاستخدام الداخلي).
     */
    private float[] getRandomVector(String seed) {
        return getConsistentRandom(seed);
    }
}

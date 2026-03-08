package com.lifeentity.perception;

import android.util.Log;

import com.lifeentity.memory.IdentityMemory;
import com.lifeentity.memory.MemoryDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * نظام التعرف على الوجوه وتذكر الهويات.
 * يستخدم متجهات التضمين (face embeddings) للمقارنة، ويعتمد على قاعدة البيانات للتخزين الدائم.
 */
public class FaceIdentitySystem {
    private static final String TAG = "FaceIdentitySystem";
    private static final float SIMILARITY_THRESHOLD = 0.7f; // عتبة التشابه للوجوه المعروفة
    private static final int EMBEDDING_SIZE = 128; // يجب أن يتوافق مع حجم embedding المستخرج من ML Kit

    private MemoryDao memoryDao;
    private Map<String, IdentityMemory> knownIdentities; // cache للوجوه المعروفة (faceHash -> IdentityMemory)
    private ExecutorService dbExecutor;

    public FaceIdentitySystem(MemoryDao dao) {
        this.memoryDao = dao;
        this.knownIdentities = new HashMap<>();
        this.dbExecutor = Executors.newSingleThreadExecutor();

        // تحميل الهويات من قاعدة البيانات في الخلفية
        loadIdentitiesFromDatabase();
    }

    /**
     * تحميل جميع الهويات المخزنة في قاعدة البيانات إلى الذاكرة المؤقتة (cache).
     */
    private void loadIdentitiesFromDatabase() {
        dbExecutor.execute(() -> {
            List<IdentityMemory> identities = memoryDao.getAllIdentities();
            synchronized (knownIdentities) {
                for (IdentityMemory identity : identities) {
                    knownIdentities.put(identity.faceHash, identity);
                }
            }
            Log.i(TAG, "Loaded " + identities.size() + " identities from database.");
        });
    }

    /**
     * التعرف على وجه أو تعلمه كوجه جديد.
     * @param faceEmbedding متجه الوجه (128-256 بعد)
     * @param context سياق المشاهدة (مثلاً "camera")
     * @param currentAffect المتجه العاطفي الحالي للكائن (5 أبعاد)
     * @return نتيجة التعرف (IdentityResult)
     */
    public IdentityResult recognizeOrLearn(float[] faceEmbedding, String context, float[] currentAffect) {
        if (faceEmbedding == null || faceEmbedding.length == 0) {
            return new IdentityResult(false, null, 0, "neutral", "لا يوجد وجه");
        }

        // البحث عن أقرب تطابق
        IdentityMatch match = findBestMatch(faceEmbedding);

        if (match != null && match.similarity > SIMILARITY_THRESHOLD) {
            // وجه معروف
            IdentityMemory identity = match.identity;
            // تحديث معلومات اللقاء
            updateEncounter(identity, context, currentAffect);
            return new IdentityResult(true, identity.name, identity.familiarity,
                    identity.emotionalAssociation, "أعرف هذا الوجه");
        } else {
            // وجه جديد: إنشاء هوية مؤقتة وحفظها في قاعدة البيانات
            IdentityMemory newIdentity = createNewIdentity(faceEmbedding, context, currentAffect);
            return new IdentityResult(false, null, 0, "neutral", "وجه جديد");
        }
    }

    /**
     * إعطاء اسم لوجه (بعد التعلم).
     * @param faceEmbedding متجه الوجه
     * @param name الاسم الجديد
     * @param relationship العلاقة (صديق، عائلة، إلخ)
     */
    public void nameIdentity(float[] faceEmbedding, String name, String relationship) {
        IdentityMatch match = findBestMatch(faceEmbedding);
        IdentityMemory identity;
        if (match != null && match.similarity > SIMILARITY_THRESHOLD) {
            identity = match.identity;
        } else {
            // إذا لم يكن موجوداً، ننشئ هوية جديدة
            identity = new IdentityMemory();
            identity.faceHash = generateHash(faceEmbedding);
            identity.faceEmbedding = faceEmbedding;
            identity.firstSeen = System.currentTimeMillis();
        }
        identity.name = name;
        identity.relationship = relationship;
        identity.familiarity = 0.5f; // بعد التسمية تصبح الألفة متوسطة

        saveIdentity(identity);
        Log.i(TAG, "Identity named: " + name);
    }

    /**
     * البحث عن أفضل تطابق لمتجه وجه في قاعدة البيانات.
     * @param embedding متجه الوجه
     * @return كائن IdentityMatch يحوي الهوية ودرجة التشابه، أو null إذا لم يوجد قريب كافٍ
     */
    private IdentityMatch findBestMatch(float[] embedding) {
        synchronized (knownIdentities) {
            IdentityMatch bestMatch = null;
            for (IdentityMemory identity : knownIdentities.values()) {
                if (identity.faceEmbedding == null) continue;
                float similarity = cosineSimilarity(embedding, identity.faceEmbedding);
                if (similarity > SIMILARITY_THRESHOLD) {
                    if (bestMatch == null || similarity > bestMatch.similarity) {
                        bestMatch = new IdentityMatch(identity, similarity);
                    }
                }
            }
            return bestMatch;
        }
    }

    /**
     * إنشاء هوية جديدة لوجه غير معروف.
     */
    private IdentityMemory createNewIdentity(float[] embedding, String context, float[] affect) {
        IdentityMemory identity = new IdentityMemory();
        identity.faceHash = generateHash(embedding);
        identity.faceEmbedding = embedding;
        identity.name = "غير معروف";
        identity.relationship = "غريب";
        identity.familiarity = 0f;
        identity.trust = 0.5f;
        identity.emotionalAssociation = affectToEmotion(affect);
        identity.encounterCount = 1;
        identity.firstSeen = System.currentTimeMillis();
        identity.lastSeen = identity.firstSeen;

        saveIdentity(identity);
        return identity;
    }

    /**
     * تحديث بيانات لقاء وجه معروف.
     */
    private void updateEncounter(IdentityMemory identity, String context, float[] affect) {
        identity.encounterCount++;
        identity.lastSeen = System.currentTimeMillis();
        // زيادة الألفة (تصل حد أقصى 1)
        identity.familiarity = Math.min(1.0f, identity.familiarity + 0.05f);

        // تحديث الارتباط العاطفي بناءً على المشاعر الحالية
        String currentEmotion = affectToEmotion(affect);
        if (currentEmotion != null) {
            identity.emotionalAssociation = currentEmotion; // يمكن تحسين هذا بمتوسط مرجح
        }

        // تحديث الثقة إذا كانت المشاعر إيجابية
        if (affect != null && affect[1] > 0.6f) { // dopamine عالٍ
            identity.trust = Math.min(1.0f, identity.trust + 0.02f);
        } else if (affect != null && affect[2] > 0.5f) { // cortisol عالٍ
            identity.trust = Math.max(0f, identity.trust - 0.01f);
        }

        saveIdentity(identity);
    }

    /**
     * حفظ الهوية في قاعدة البيانات (بشكل غير متزامن) وتحديث cache.
     */
    private void saveIdentity(IdentityMemory identity) {
        synchronized (knownIdentities) {
            knownIdentities.put(identity.faceHash, identity);
        }
        dbExecutor.execute(() -> memoryDao.saveIdentity(identity));
    }

    /**
     * توليد هاش فريد من متجه الوجه (للاستخدام كمفتاح في قاعدة البيانات).
     */
    private String generateHash(float[] embedding) {
        // استخدام أول 16 قيمة لإنشاء هاش بسيط (يمكن تحسينه)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(16, embedding.length); i++) {
            int b = (int) ((embedding[i] + 1f) * 127.5); // تحويل [-1,1] إلى [0,255]
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * تحويل المتجه العاطفي إلى وصف نصي للمشاعر (مبسط).
     */
    private String affectToEmotion(float[] affect) {
        if (affect == null || affect.length < 3) return "neutral";
        // affect[0]=arousal, [1]=dopamine, [2]=cortisol, [3]=curiosity, [4]=attachment
        if (affect[1] > 0.7f && affect[2] < 0.3f) return "joy";
        if (affect[2] > 0.6f) return "fear";
        if (affect[3] > 0.7f) return "curiosity";
        if (affect[0] < 0.3f && affect[1] < 0.3f) return "sadness";
        return "neutral";
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
     * إيقاف تشغيل المنفذ (يُستدعى عند إغلاق التطبيق).
     */
    public void shutdown() {
        dbExecutor.shutdown();
    }

    // ======================= الفئات الداخلية =======================

    /**
     * نتيجة التعرف على الوجه.
     */
    public static class IdentityResult {
        public final boolean isKnown;
        public final String name;
        public final float familiarity;
        public final String emotionalTone;
        public final String description;

        IdentityResult(boolean k, String n, float f, String e, String d) {
            this.isKnown = k;
            this.name = n;
            this.familiarity = f;
            this.emotionalTone = e;
            this.description = d;
        }
    }

    /**
     * كائن داخلي للتمثيل المؤقت لأفضل تطابق.
     */
    private static class IdentityMatch {
        final IdentityMemory identity;
        final float similarity;

        IdentityMatch(IdentityMemory identity, float similarity) {
            this.identity = identity;
            this.similarity = similarity;
        }
    }
}

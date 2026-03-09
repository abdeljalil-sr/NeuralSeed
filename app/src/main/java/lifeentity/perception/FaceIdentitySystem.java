package com.lifeentity.perception;

import android.util.Log;

import com.lifeentity.memory.IdentityMemory;
import com.lifeentity.memory.MemoryDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * نظام متقدم للتعرف على الهوية باستخدام FaceNet embeddings
 * يدعم التعلم العاطفي والعتبات الديناميكية والذاكرة طويلة المدى
 */
public class FaceIdentitySystem {
    private static final String TAG = "FaceIdentitySystem";
    
    // إعدادات النظام المتقدمة
    private static final int EMBEDDING_SIZE = 128; // FaceNet standard
    private static final float BASE_SIMILARITY_THRESHOLD = 0.60f; // عتبة أساسية أقل تشدداً
    private static final float STRICT_THRESHOLD = 0.75f; // عتبة صارمة للمطابقات العالية
    private static final float MIN_CONFIDENCE_FOR_LEARNING = 0.45f; // أقل تشابه للتعلم
    private static final int MIN_SAMPLES_FOR_CONFIDENCE = 3; // الحد الأدنى للعينات لرفع الثقة
    
    // عوامل التعلم العاطفي
    private static final float POSITIVE_EMOTION_BOOST = 0.08f; // زيادة الثقة مع المشاعر الإيجابية
    private static final float NEGATIVE_EMOTION_PENALTY = 0.03f; // خصم مع المشاعر السلبية
    private static final float FAMILIARITY_DECAY_RATE = 0.001f; // معدل النسيان البطيء
    private static final long FORGETTING_PERIOD_MS = 7L * 24 * 60 * 60 * 1000; // أسبوع للتعتيم

    private MemoryDao memoryDao;
    private ConcurrentHashMap<String, IdentityMemory> knownIdentities;
    private ExecutorService dbExecutor;
    private volatile boolean isShutdown = false;
    
    // إحصائيات للتتبع
    private AtomicInteger totalRecognitions = new AtomicInteger(0);
    private AtomicInteger successfulRecognitions = new AtomicInteger(0);

    public FaceIdentitySystem(MemoryDao dao) {
        this.memoryDao = dao;
        this.knownIdentities = new ConcurrentHashMap<>();
        this.dbExecutor = Executors.newSingleThreadExecutor();
        loadIdentitiesFromDatabase();
    }

    /**
     * تحميل الهويات من قاعدة البيانات بشكل غير متزامن
     */
    private void loadIdentitiesFromDatabase() {
        if (memoryDao == null) {
            Log.w(TAG, "MemoryDao is null, operating in memory-only mode");
            return;
        }
        
        dbExecutor.execute(() -> {
            try {
                List<IdentityMemory> identities = memoryDao.getAllIdentities();
                for (IdentityMemory identity : identities) {
                    if (isValidEmbedding(identity.faceEmbedding)) {
                        knownIdentities.put(identity.faceHash, identity);
                    } else {
                        Log.w(TAG, "Invalid embedding for identity: " + identity.faceHash);
                    }
                }
                Log.i(TAG, "Loaded " + knownIdentities.size() + " valid identities from database.");
            } catch (Exception e) {
                Log.e(TAG, "Error loading identities", e);
            }
        });
    }

    /**
     * التعرف على وجه أو تعلم وجه جديد مع نظام عتبات ديناميكية
     */
    public IdentityResult recognizeOrLearn(float[] faceEmbedding, String context, float[] currentAffect) {
        totalRecognitions.incrementAndGet();
        
        if (!isValidEmbedding(faceEmbedding)) {
            Log.w(TAG, "Invalid face embedding provided");
            return new IdentityResult(false, null, 0, "neutral", "لا يوجد وجه", 0f, null);
        }

        // تطبيع الـ embedding للحصول على دقة أفضل
        float[] normalizedEmbedding = normalizeEmbedding(faceEmbedding);
        
        // البحث عن أفضل مطابقة
        IdentityMatch match = findBestMatch(normalizedEmbedding);
        
        // حساب العتبة الديناميكية بناءً on السياق
        float dynamicThreshold = calculateDynamicThreshold(match, currentAffect);
        
        if (match != null && match.similarity >= dynamicThreshold) {
            // وجه معروف - تحديث البيانات
            successfulRecognitions.incrementAndGet();
            IdentityMemory identity = match.identity;
            
            // تحديث الـ embedding بشكل تدريجي (moving average) للتكيف مع تغيرات الوجه
            updateEmbeddingAdaptive(identity, normalizedEmbedding, match.similarity);
            
            // تحديث بيانات اللقاء والمشاعر
            updateEncounterWithEmotionalLearning(identity, context, currentAffect, match.similarity);
            
            float confidence = calculateConfidence(identity, match.similarity);
            
            return new IdentityResult(
                true, 
                identity.name, 
                identity.familiarity,
                identity.emotionalAssociation, 
                generateRecognitionDescription(identity, match.similarity),
                confidence,
                identity.faceHash
            );
        } else if (match != null && match.similarity >= MIN_CONFIDENCE_FOR_LEARNING) {
            // حالة مشكوك فيها - ربما وجه معروف لكن بتغيير كبير
            Log.d(TAG, "Ambiguous match: " + match.similarity + " for " + match.identity.name);
            return handleAmbiguousCase(match, normalizedEmbedding, context, currentAffect);
        } else {
            // وجه جديد تماماً
            IdentityMemory newIdentity = createNewIdentity(normalizedEmbedding, context, currentAffect);
            return new IdentityResult(
                false, 
                null, 
                0f, 
                "neutral", 
                "وجه جديد",
                0f,
                newIdentity.faceHash
            );
        }
    }

    /**
     * تسمية هوية معروفة أو جديدة
     */
    public void nameIdentity(float[] faceEmbedding, String name, String relationship) {
        if (!isValidEmbedding(faceEmbedding)) {
            Log.e(TAG, "Cannot name identity: invalid embedding");
            return;
        }
        
        float[] normalized = normalizeEmbedding(faceEmbedding);
        IdentityMatch match = findBestMatch(normalized);
        
        dbExecutor.execute(() -> {
            IdentityMemory identity;
            
            if (match != null && match.similarity > BASE_SIMILARITY_THRESHOLD) {
                // تحديث هوية موجودة
                identity = match.identity;
                Log.i(TAG, "Updating existing identity with name: " + name);
            } else {
                // إنشاء هوية جديدة
                identity = new IdentityMemory();
                identity.faceHash = generateConsistentHash(normalized);
                identity.faceEmbedding = normalized;
                identity.firstSeen = System.currentTimeMillis();
                identity.encounterCount = 0;
                identity.familiarity = 0f;
                identity.trust = 0.5f;
                Log.i(TAG, "Creating new identity with name: " + name);
            }
            
            identity.name = name;
            identity.relationship = relationship != null ? relationship : "غريب";
            identity.familiarity = Math.max(identity.familiarity, 0.5f); // رفع الألفة عند التسمية
            identity.lastSeen = System.currentTimeMillis();
            
            saveIdentity(identity);
        });
    }

    /**
     * الحصول على جميع الهويات المعروفة (للتزامن الخارجي)
     */
    public List<IdentityProfile> getAllIdentities() {
        List<IdentityProfile> profiles = new ArrayList<>();
        for (IdentityMemory identity : knownIdentities.values()) {
            profiles.add(convertToProfile(identity));
        }
        return profiles;
    }

    /**
     * تحديث بيانات الهوية من مصدر خارجي (للتزامن)
     */
    public void updateIdentityFromSync(IdentityProfile profile) {
        if (profile == null || profile.faceHash == null) return;
        
        dbExecutor.execute(() -> {
            IdentityMemory existing = knownIdentities.get(profile.faceHash);
            if (existing != null) {
                // دمج البيانات مع إعطاء الأولوية للبيانات الأحدث
                if (profile.lastSeen > existing.lastSeen) {
                    existing.name = profile.name;
                    existing.relationship = profile.relationship;
                    existing.familiarity = Math.max(existing.familiarity, profile.familiarity);
                    existing.trust = profile.trust;
                    existing.emotionalAssociation = profile.emotionalAssociation;
                    existing.encounterCount = Math.max(existing.encounterCount, profile.encounterCount);
                    existing.lastSeen = profile.lastSeen;
                    saveIdentity(existing);
                }
            } else {
                // إضافة هوية جديدة من التزامن
                IdentityMemory newIdentity = convertFromProfile(profile);
                knownIdentities.put(profile.faceHash, newIdentity);
                if (memoryDao != null) {
                    memoryDao.saveIdentity(newIdentity);
                }
            }
        });
    }

    // ==================== الخوارزميات الأساسية ====================

    /**
     * البحث عن أفضل مطابقة باستخدام Cosine Similarity
     */
    private IdentityMatch findBestMatch(float[] embedding) {
        IdentityMatch bestMatch = null;
        float bestSimilarity = -1f;
        
        for (IdentityMemory identity : knownIdentities.values()) {
            if (!isValidEmbedding(identity.faceEmbedding)) continue;
            
            float similarity = cosineSimilarity(embedding, identity.faceEmbedding);
            
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = new IdentityMatch(identity, similarity);
            }
        }
        
        return bestMatch;
    }

    /**
     * حساب العتبة الديناميكية بناءً على السياق
     */
    private float calculateDynamicThreshold(IdentityMatch match, float[] affect) {
        float threshold = BASE_SIMILARITY_THRESHOLD;
        
        if (match != null) {
            // رفع العتبة للوجوه المألوفة جداً (للتأكد من عدم الخلط)
            if (match.identity.familiarity > 0.8f) {
                threshold = STRICT_THRESHOLD;
            } else if (match.identity.familiarity > 0.5f) {
                threshold = 0.65f;
            }
            
            // خفض العتبة إذا كان هناك سياق عاطفي إيجابي (ثقة أعلى)
            if (affect != null && affect.length > 1 && affect[1] > 0.6f) {
                threshold -= 0.05f;
            }
        }
        
        return threshold;
    }

    /**
     * التعلم التكيفي للـ embedding مع الوقت
     */
    private void updateEmbeddingAdaptive(IdentityMemory identity, float[] newEmbedding, float similarity) {
        // إذا كانت المطابقة جيدة لكن ليست مثالية، قم بتحديث الـ embedding تدريجياً
        if (similarity < 0.85f && similarity > 0.65f) {
            float alpha = 0.1f; // معدل التعلم
            float[] updated = new float[EMBEDDING_SIZE];
            
            for (int i = 0; i < EMBEDDING_SIZE; i++) {
                updated[i] = (1 - alpha) * identity.faceEmbedding[i] + alpha * newEmbedding[i];
            }
            
            identity.faceEmbedding = normalizeEmbedding(updated);
        }
    }

    /**
     * تحديث اللقاء مع التعلم العاطفي المتقدم
     */
    private void updateEncounterWithEmotionalLearning(IdentityMemory identity, String context, 
                                                     float[] affect, float matchConfidence) {
        long now = System.currentTimeMillis();
        identity.encounterCount++;
        identity.lastSeen = now;
        
        // حساب عامل الزمن (اللقاءات الأخيرة لها وزن أكبر)
        float timeFactor = calculateTimeFactor(identity);
        
        // تحديث الألفة مع التعتيم البطيء
        float familiarityBoost = 0.05f * timeFactor;
        identity.familiarity = Math.min(1.0f, identity.familiarity * (1 - FAMILIARITY_DECAY_RATE) + familiarityBoost);
        
        // التعلم العاطفي المتقدم
        if (affect != null && affect.length >= 4) {
            String currentEmotion = extractEmotionFromAffect(affect);
            
            // تحديث الارتباط العاطفي بشكل مرجح
            if (identity.emotionalAssociation == null || identity.emotionalAssociation.equals("neutral")) {
                identity.emotionalAssociation = currentEmotion;
            } else {
                // التحول التدريجي نحو المشاعر المتكررة
                if (shouldUpdateEmotionalAssociation(identity, currentEmotion, affect)) {
                    identity.emotionalAssociation = currentEmotion;
                }
            }
            
            // تحديث الثقة بناءً على المشاعر
            updateTrustBasedOnEmotion(identity, affect, matchConfidence);
        }
        
        // تحديث السياق إذا كان مفيداً
        if (context != null && !context.isEmpty()) {
            identity.lastContext = context;
        }
        
        saveIdentity(identity);
    }

    /**
     * استخراج المشاعر من متجه التأثير
     */
    private String extractEmotionFromAffect(float[] affect) {
        if (affect[1] > 0.7f && affect[2] < 0.3f) return "joy";
        if (affect[2] > 0.6f) return "fear";
        if (affect[3] > 0.7f) return "curiosity";
        if (affect[0] < 0.3f && affect[1] < 0.3f) return "sadness";
        if (affect[0] > 0.7f && affect[1] > 0.5f) return "excitement";
        if (affect[2] < 0.3f && affect[3] > 0.5f) return "calm";
        return "neutral";
    }

    /**
     * تحديث الثقة بناءً على المشاعر والسياق
     */
    private void updateTrustBasedOnEmotion(IdentityMemory identity, float[] affect, float confidence) {
        float joy = affect.length > 1 ? affect[1] : 0;
        float fear = affect.length > 2 ? affect[2] : 0;
        float curiosity = affect.length > 3 ? affect[3] : 0;
        
        // الثقة تزداد مع الفرح والفضول، وتنخفض مع الخوف
        if (joy > 0.6f && confidence > 0.7f) {
            identity.trust = Math.min(1.0f, identity.trust + POSITIVE_EMOTION_BOOST);
        } else if (fear > 0.5f) {
            identity.trust = Math.max(0f, identity.trust - NEGATIVE_EMOTION_PENALTY);
        }
        
        // زيادة إضافية للثقة مع تكرار اللقاءات الإيجابية
        if (identity.encounterCount > MIN_SAMPLES_FOR_CONFIDENCE && 
            identity.emotionalAssociation != null && 
            (identity.emotionalAssociation.equals("joy") || identity.emotionalAssociation.equals("calm"))) {
            identity.trust = Math.min(1.0f, identity.trust + 0.01f);
        }
    }

    /**
     * حساب عامل الزمن للألفة
     */
    private float calculateTimeFactor(IdentityMemory identity) {
        long timeSinceLast = System.currentTimeMillis() - identity.lastSeen;
        if (timeSinceLast < 60000) return 1.5f; // لقاء متكرر سريعاً
        if (timeSinceLast < 3600000) return 1.2f; // خلال ساعة
        if (timeSinceLast > FORGETTING_PERIOD_MS) return 0.8f; // بعد فترة طويلة
        return 1.0f;
    }

    /**
     * معالجة الحالات المشكوك فيها
     */
    private IdentityResult handleAmbiguousCase(IdentityMatch match, float[] embedding, 
                                              String context, float[] affect) {
        // إنشاء هوية مؤقتة أو إرجاع نتيجة غير مؤكدة
        return new IdentityResult(
            false, // غير مؤكد
            match.identity.name + "؟", 
            match.identity.familiarity * 0.5f,
            "uncertain",
            "هل هذا " + match.identity.name + "؟ أنا غير متأكد",
            match.similarity,
            null
        );
    }

    /**
     * إنشاء هوية جديدة
     */
    private IdentityMemory createNewIdentity(float[] embedding, String context, float[] affect) {
        IdentityMemory identity = new IdentityMemory();
        identity.faceHash = generateConsistentHash(embedding);
        identity.faceEmbedding = embedding;
        identity.name = "غير معروف";
        identity.relationship = "غريب";
        identity.familiarity = 0.05f; // ألفة أولية منخفضة
        identity.trust = 0.3f; // ثقة أولية منخفضة
        identity.emotionalAssociation = affect != null ? extractEmotionFromAffect(affect) : "neutral";
        identity.encounterCount = 1;
        identity.firstSeen = System.currentTimeMillis();
        identity.lastSeen = identity.firstSeen;
        identity.lastContext = context;
        
        saveIdentity(identity);
        Log.i(TAG, "Created new identity: " + identity.faceHash);
        return identity;
    }

    /**
     * حفظ الهوية في قاعدة البيانات والذاكرة
     */
    private void saveIdentity(IdentityMemory identity) {
        knownIdentities.put(identity.faceHash, identity);
        
        if (memoryDao != null && !isShutdown) {
            dbExecutor.execute(() -> {
                try {
                    memoryDao.saveIdentity(identity);
                } catch (Exception e) {
                    Log.e(TAG, "Error saving identity to database", e);
                }
            });
        }
    }

    // ==================== دوال المساعدة ====================

    private float[] normalizeEmbedding(float[] embedding) {
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm < 0.0001f) return embedding; // تجنب القسمة على صفر
        
        float[] normalized = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / norm;
        }
        return normalized;
    }

    private boolean isValidEmbedding(float[] embedding) {
        return embedding != null && embedding.length == EMBEDDING_SIZE;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        // افتراض أن المدخلات normalized
        float dot = 0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            dot += a[i] * b[i];
        }
        return dot; // لأنها normalized، الناتج هو cosine similarity مباشرة
    }

    private String generateConsistentHash(float[] embedding) {
        // استخدام quantization بسيط لإنشاء hash ثابت نسبياً
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < EMBEDDING_SIZE; i += 8) { // أخذ كل 8 قيمة
            int quantized = (int) ((embedding[i] + 1.0f) * 127.5f);
            sb.append(String.format("%02x", quantized & 0xFF));
        }
        return sb.toString();
    }

    private boolean shouldUpdateEmotionalAssociation(IdentityMemory identity, String newEmotion, float[] affect) {
        // تحديث إذا كان المشهد الجديد أقوى بكثير
        float currentIntensity = getEmotionIntensity(identity.emotionalAssociation, affect);
        float newIntensity = getEmotionIntensity(newEmotion, affect);
        
        return newIntensity > currentIntensity + 0.2f;
    }

    private float getEmotionIntensity(String emotion, float[] affect) {
        if (emotion == null || affect == null) return 0;
        switch (emotion) {
            case "joy": return affect.length > 1 ? affect[1] : 0;
            case "fear": return affect.length > 2 ? affect[2] : 0;
            case "curiosity": return affect.length > 3 ? affect[3] : 0;
            default: return 0.5f;
        }
    }

    private String generateRecognitionDescription(IdentityMemory identity, float similarity) {
        if (similarity > 0.9f) {
            return "أنا متأكد أن هذا " + identity.name;
        } else if (similarity > 0.75f) {
            return "أعتقد أن هذا " + identity.name;
        } else {
            return "ربما هذا " + identity.name;
        }
    }

    private float calculateConfidence(IdentityMemory identity, float similarity) {
        float conf = similarity * 0.6f + identity.familiarity * 0.3f + identity.trust * 0.1f;
        return Math.min(1.0f, conf);
    }

    private IdentityProfile convertToProfile(IdentityMemory identity) {
        IdentityProfile profile = new IdentityProfile(
            identity.name, 
            identity.relationship, 
            identity.faceHash
        );
        profile.faceEmbedding = identity.faceEmbedding;
        profile.familiarity = identity.familiarity;
        profile.trust = identity.trust;
        profile.emotionalAssociation = identity.emotionalAssociation;
        profile.encounterCount = identity.encounterCount;
        profile.firstSeen = identity.firstSeen;
        profile.lastSeen = identity.lastSeen;
        return profile;
    }

    private IdentityMemory convertFromProfile(IdentityProfile profile) {
        IdentityMemory identity = new IdentityMemory();
        identity.name = profile.name;
        identity.relationship = profile.relationship;
        identity.faceHash = profile.faceHash;
        identity.faceEmbedding = profile.faceEmbedding;
        identity.familiarity = profile.familiarity;
        identity.trust = profile.trust;
        identity.emotionalAssociation = profile.emotionalAssociation;
        identity.encounterCount = profile.encounterCount;
        identity.firstSeen = profile.firstSeen;
        identity.lastSeen = profile.lastSeen;
        return identity;
    }

    public void shutdown() {
        isShutdown = true;
        dbExecutor.shutdown();
        Log.i(TAG, "Shutdown complete. Total recognitions: " + totalRecognitions.get() + 
              ", Successful: " + successfulRecognitions.get());
    }

    // ==================== الفئات الداخلية العامة ====================

    public static class IdentityResult {
        public final boolean isKnown;
        public final String name;
        public final float familiarity;
        public final String emotionalTone;
        public final String description;
        public final float confidence;
        public final String faceHash; // للربط مع الهوية في قاعدة البيانات

        public IdentityResult(boolean k, String n, float f, String e, String d, float c, String h) {
            isKnown = k; 
            name = n; 
            familiarity = f; 
            emotionalTone = e; 
            description = d;
            confidence = c;
            faceHash = h;
        }
        
        @Override
        public String toString() {
            return String.format("IdentityResult{known=%s, name='%s', confidence=%.2f}", 
                isKnown, name, confidence);
        }
    }

    public static class IdentityProfile {
        public String name;
        public String relationship;
        public String faceHash;
        public float[] faceEmbedding;
        public float familiarity;
        public float trust;
        public String emotionalAssociation;
        public int encounterCount;
        public long firstSeen;
        public long lastSeen;

        public IdentityProfile(String n, String r, String h) {
            this.name = n;
            this.relationship = r;
            this.faceHash = h;
            this.familiarity = 0;
            this.trust = 0.5f;
            this.emotionalAssociation = "neutral";
            this.encounterCount = 0;
            this.firstSeen = System.currentTimeMillis();
            this.lastSeen = this.firstSeen;
        }

        public void recordEncounter(String context, float[] affect) {
            encounterCount++;
            lastSeen = System.currentTimeMillis();
            familiarity = Math.min(1, familiarity + 0.05f);
        }
    }

    private static class IdentityMatch {
        final IdentityMemory identity;
        final float similarity;
        
        IdentityMatch(IdentityMemory identity, float similarity) {
            this.identity = identity; 
            this.similarity = similarity;
        }
    }
}

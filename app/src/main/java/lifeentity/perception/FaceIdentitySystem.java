package com.lifeentity.perception;

import android.util.Log;

import com.lifeentity.memory.IdentityMemory;
import com.lifeentity.memory.MemoryDao;

import java.util.ArrayList;
import java.util.List;
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
    
    private static final int EMBEDDING_SIZE = 128;
    private static final float BASE_SIMILARITY_THRESHOLD = 0.60f;
    private static final float STRICT_THRESHOLD = 0.75f;
    private static final float MIN_CONFIDENCE_FOR_LEARNING = 0.45f;
    private static final int MIN_SAMPLES_FOR_CONFIDENCE = 3;
    
    private static final float POSITIVE_EMOTION_BOOST = 0.08f;
    private static final float NEGATIVE_EMOTION_PENALTY = 0.03f;
    private static final float FAMILIARITY_DECAY_RATE = 0.001f;
    private static final long FORGETTING_PERIOD_MS = 7L * 24 * 60 * 60 * 1000;

    private MemoryDao memoryDao;
    private ConcurrentHashMap<String, IdentityMemory> knownIdentities;
    private ExecutorService dbExecutor;
    private volatile boolean isShutdown = false;
    
    private AtomicInteger totalRecognitions = new AtomicInteger(0);
    private AtomicInteger successfulRecognitions = new AtomicInteger(0);

    public FaceIdentitySystem(MemoryDao dao) {
        this.memoryDao = dao;
        this.knownIdentities = new ConcurrentHashMap<>();
        this.dbExecutor = Executors.newSingleThreadExecutor();
        loadIdentitiesFromDatabase();
    }

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

    public IdentityResult recognizeOrLearn(float[] faceEmbedding, String context, float[] currentAffect) {
        totalRecognitions.incrementAndGet();
        
        if (!isValidEmbedding(faceEmbedding)) {
            Log.w(TAG, "Invalid face embedding provided");
            return new IdentityResult(false, null, 0, "neutral", "لا يوجد وجه", 0f, null);
        }

        float[] normalizedEmbedding = normalizeEmbedding(faceEmbedding);
        IdentityMatch match = findBestMatch(normalizedEmbedding);
        float dynamicThreshold = calculateDynamicThreshold(match, currentAffect);
        
        if (match != null && match.similarity >= dynamicThreshold) {
            successfulRecognitions.incrementAndGet();
            IdentityMemory identity = match.identity;
            
            updateEmbeddingAdaptive(identity, normalizedEmbedding, match.similarity);
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
            Log.d(TAG, "Ambiguous match: " + match.similarity + " for " + match.identity.name);
            return handleAmbiguousCase(match, normalizedEmbedding, context, currentAffect);
        } else {
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
                identity = match.identity;
                Log.i(TAG, "Updating existing identity with name: " + name);
            } else {
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
            identity.familiarity = Math.max(identity.familiarity, 0.5f);
            identity.lastSeen = System.currentTimeMillis();
            
            saveIdentity(identity);
        });
    }

    public List<IdentityProfile> getAllIdentities() {
        List<IdentityProfile> profiles = new ArrayList<>();
        for (IdentityMemory identity : knownIdentities.values()) {
            profiles.add(convertToProfile(identity));
        }
        return profiles;
    }

    public void updateIdentityFromSync(IdentityProfile profile) {
        if (profile == null || profile.faceHash == null) return;
        
        dbExecutor.execute(() -> {
            IdentityMemory existing = knownIdentities.get(profile.faceHash);
            if (existing != null) {
                if (profile.lastSeen > existing.lastSeen) {
                    existing.name = profile.name;
                    existing.relationship = profile.relationship;
                    existing.familiarity = Math.max(existing.familiarity, profile.familiarity);
                    existing.trust = profile.trust;
                    existing.emotionalAssociation = profile.emotionalAssociation;
                    existing.encounterCount = Math.max(existing.encounterCount, profile.encounterCount);
                    existing.lastSeen = profile.lastSeen;
                    existing.lastContext = profile.lastContext;
                    saveIdentity(existing);
                }
            } else {
                IdentityMemory newIdentity = convertFromProfile(profile);
                knownIdentities.put(profile.faceHash, newIdentity);
                if (memoryDao != null) {
                    memoryDao.saveIdentity(newIdentity);
                }
            }
        });
    }

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

    private float calculateDynamicThreshold(IdentityMatch match, float[] affect) {
        float threshold = BASE_SIMILARITY_THRESHOLD;
        if (match != null) {
            if (match.identity.familiarity > 0.8f) {
                threshold = STRICT_THRESHOLD;
            } else if (match.identity.familiarity > 0.5f) {
                threshold = 0.65f;
            }
            if (affect != null && affect.length > 1 && affect[1] > 0.6f) {
                threshold -= 0.05f;
            }
        }
        return threshold;
    }

    private void updateEmbeddingAdaptive(IdentityMemory identity, float[] newEmbedding, float similarity) {
        if (similarity < 0.85f && similarity > 0.65f) {
            float alpha = 0.1f;
            float[] updated = new float[EMBEDDING_SIZE];
            for (int i = 0; i < EMBEDDING_SIZE; i++) {
                updated[i] = (1 - alpha) * identity.faceEmbedding[i] + alpha * newEmbedding[i];
            }
            identity.faceEmbedding = normalizeEmbedding(updated);
        }
    }

    private void updateEncounterWithEmotionalLearning(IdentityMemory identity, String context, 
                                                       float[] affect, float matchConfidence) {
        long now = System.currentTimeMillis();
        identity.encounterCount++;
        identity.lastSeen = now;
        
        float timeFactor = calculateTimeFactor(identity);
        float familiarityBoost = 0.05f * timeFactor;
        identity.familiarity = Math.min(1.0f, identity.familiarity * (1 - FAMILIARITY_DECAY_RATE) + familiarityBoost);
        
        if (affect != null && affect.length >= 4) {
            String currentEmotion = extractEmotionFromAffect(affect);
            if (identity.emotionalAssociation == null || identity.emotionalAssociation.equals("neutral")) {
                identity.emotionalAssociation = currentEmotion;
            } else {
                if (shouldUpdateEmotionalAssociation(identity, currentEmotion, affect)) {
                    identity.emotionalAssociation = currentEmotion;
                }
            }
            updateTrustBasedOnEmotion(identity, affect, matchConfidence);
        }
        
        if (context != null && !context.isEmpty()) {
            identity.lastContext = context;
        }
        
        saveIdentity(identity);
    }

    private String extractEmotionFromAffect(float[] affect) {
        if (affect[1] > 0.7f && affect[2] < 0.3f) return "joy";
        if (affect[2] > 0.6f) return "fear";
        if (affect[3] > 0.7f) return "curiosity";
        if (affect[0] < 0.3f && affect[1] < 0.3f) return "sadness";
        if (affect[0] > 0.7f && affect[1] > 0.5f) return "excitement";
        if (affect[2] < 0.3f && affect[3] > 0.5f) return "calm";
        return "neutral";
    }

    private void updateTrustBasedOnEmotion(IdentityMemory identity, float[] affect, float confidence) {
        float joy = affect.length > 1 ? affect[1] : 0;
        float fear = affect.length > 2 ? affect[2] : 0;
        if (joy > 0.6f && confidence > 0.7f) {
            identity.trust = Math.min(1.0f, identity.trust + POSITIVE_EMOTION_BOOST);
        } else if (fear > 0.5f) {
            identity.trust = Math.max(0f, identity.trust - NEGATIVE_EMOTION_PENALTY);
        }
        if (identity.encounterCount > MIN_SAMPLES_FOR_CONFIDENCE && 
            identity.emotionalAssociation != null && 
            (identity.emotionalAssociation.equals("joy") || identity.emotionalAssociation.equals("calm"))) {
            identity.trust = Math.min(1.0f, identity.trust + 0.01f);
        }
    }

    private float calculateTimeFactor(IdentityMemory identity) {
        long timeSinceLast = System.currentTimeMillis() - identity.lastSeen;
        if (timeSinceLast < 60000) return 1.5f;
        if (timeSinceLast < 3600000) return 1.2f;
        if (timeSinceLast > FORGETTING_PERIOD_MS) return 0.8f;
        return 1.0f;
    }

    private IdentityResult handleAmbiguousCase(IdentityMatch match, float[] embedding, 
                                                String context, float[] affect) {
        return new IdentityResult(
            false,
            match.identity.name + "؟", 
            match.identity.familiarity * 0.5f,
            "uncertain",
            "هل هذا " + match.identity.name + "؟ أنا غير متأكد",
            match.similarity,
            null
        );
    }

    private IdentityMemory createNewIdentity(float[] embedding, String context, float[] affect) {
        IdentityMemory identity = new IdentityMemory();
        identity.faceHash = generateConsistentHash(embedding);
        identity.faceEmbedding = embedding;
        identity.name = "غير معروف";
        identity.relationship = "غريب";
        identity.familiarity = 0.05f;
        identity.trust = 0.3f;
        identity.emotionalAssociation = affect != null ? extractEmotionFromAffect(affect) : "neutral";
        identity.encounterCount = 1;
        identity.firstSeen = System.currentTimeMillis();
        identity.lastSeen = identity.firstSeen;
        identity.lastContext = context;
        
        saveIdentity(identity);
        Log.i(TAG, "Created new identity: " + identity.faceHash);
        return identity;
    }

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

    private float[] normalizeEmbedding(float[] embedding) {
        float norm = 0;
        for (float v : embedding) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm < 0.0001f) return embedding;
        float[] normalized = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) normalized[i] = embedding[i] / norm;
        return normalized;
    }

    private boolean isValidEmbedding(float[] embedding) {
        return embedding != null && embedding.length == EMBEDDING_SIZE;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) dot += a[i] * b[i];
        return dot;
    }

    private String generateConsistentHash(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < EMBEDDING_SIZE; i += 8) {
            int quantized = (int) ((embedding[i] + 1.0f) * 127.5f);
            sb.append(String.format("%02x", quantized & 0xFF));
        }
        return sb.toString();
    }

    private boolean shouldUpdateEmotionalAssociation(IdentityMemory identity, String newEmotion, float[] affect) {
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
        if (similarity > 0.9f) return "أنا متأكد أن هذا " + identity.name;
        if (similarity > 0.75f) return "أعتقد أن هذا " + identity.name;
        return "ربما هذا " + identity.name;
    }

    private float calculateConfidence(IdentityMemory identity, float similarity) {
        float conf = similarity * 0.6f + identity.familiarity * 0.3f + identity.trust * 0.1f;
        return Math.min(1.0f, conf);
    }

    private IdentityProfile convertToProfile(IdentityMemory identity) {
        IdentityProfile profile = new IdentityProfile(identity.name, identity.relationship, identity.faceHash);
        profile.faceEmbedding = identity.faceEmbedding;
        profile.familiarity = identity.familiarity;
        profile.trust = identity.trust;
        profile.emotionalAssociation = identity.emotionalAssociation;
        profile.encounterCount = identity.encounterCount;
        profile.firstSeen = identity.firstSeen;
        profile.lastSeen = identity.lastSeen;
        profile.lastContext = identity.lastContext;
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
        identity.lastContext = profile.lastContext;
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
        public final String faceHash;

        public IdentityResult(boolean k, String n, float f, String e, String d, float c, String h) {
            isKnown = k; name = n; familiarity = f; emotionalTone = e; description = d;
            confidence = c; faceHash = h;
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
        public String lastContext;

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
            this.identity = identity; this.similarity = similarity;
        }
    }
}

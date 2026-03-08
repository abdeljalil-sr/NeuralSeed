// ====================== FaceIdentitySystem.java (مع تعريف IdentityProfile كـ public static) ======================
package com.lifeentity.perception;

import android.util.Log;

import com.lifeentity.memory.IdentityMemory;
import com.lifeentity.memory.MemoryDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceIdentitySystem {
    private static final String TAG = "FaceIdentitySystem";
    private static final float SIMILARITY_THRESHOLD = 0.7f;
    private static final int EMBEDDING_SIZE = 128;

    private MemoryDao memoryDao;
    private Map<String, IdentityMemory> knownIdentities;
    private ExecutorService dbExecutor;

    public FaceIdentitySystem(MemoryDao dao) {
        this.memoryDao = dao;
        this.knownIdentities = new HashMap<>();
        this.dbExecutor = Executors.newSingleThreadExecutor();
        loadIdentitiesFromDatabase();
    }

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

    public IdentityResult recognizeOrLearn(float[] faceEmbedding, String context, float[] currentAffect) {
        if (faceEmbedding == null || faceEmbedding.length == 0) {
            return new IdentityResult(false, null, 0, "neutral", "لا يوجد وجه");
        }
        IdentityMatch match = findBestMatch(faceEmbedding);
        if (match != null && match.similarity > SIMILARITY_THRESHOLD) {
            IdentityMemory identity = match.identity;
            updateEncounter(identity, context, currentAffect);
            return new IdentityResult(true, identity.name, identity.familiarity,
                    identity.emotionalAssociation, "أعرف هذا الوجه");
        } else {
            IdentityMemory newIdentity = createNewIdentity(faceEmbedding, context, currentAffect);
            return new IdentityResult(false, null, 0, "neutral", "وجه جديد");
        }
    }

    public void nameIdentity(float[] faceEmbedding, String name, String relationship) {
        IdentityMatch match = findBestMatch(faceEmbedding);
        IdentityMemory identity;
        if (match != null && match.similarity > SIMILARITY_THRESHOLD) {
            identity = match.identity;
        } else {
            identity = new IdentityMemory();
            identity.faceHash = generateHash(faceEmbedding);
            identity.faceEmbedding = faceEmbedding;
            identity.firstSeen = System.currentTimeMillis();
        }
        identity.name = name;
        identity.relationship = relationship;
        identity.familiarity = 0.5f;
        saveIdentity(identity);
        Log.i(TAG, "Identity named: " + name);
    }

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

    private void updateEncounter(IdentityMemory identity, String context, float[] affect) {
        identity.encounterCount++;
        identity.lastSeen = System.currentTimeMillis();
        identity.familiarity = Math.min(1.0f, identity.familiarity + 0.05f);
        String currentEmotion = affectToEmotion(affect);
        if (currentEmotion != null) {
            identity.emotionalAssociation = currentEmotion;
        }
        if (affect != null && affect[1] > 0.6f) {
            identity.trust = Math.min(1.0f, identity.trust + 0.02f);
        } else if (affect != null && affect[2] > 0.5f) {
            identity.trust = Math.max(0f, identity.trust - 0.01f);
        }
        saveIdentity(identity);
    }

    private void saveIdentity(IdentityMemory identity) {
        synchronized (knownIdentities) {
            knownIdentities.put(identity.faceHash, identity);
        }
        dbExecutor.execute(() -> memoryDao.saveIdentity(identity));
    }

    private String generateHash(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(16, embedding.length); i++) {
            int b = (int) ((embedding[i] + 1f) * 127.5);
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    private String affectToEmotion(float[] affect) {
        if (affect == null || affect.length < 3) return "neutral";
        if (affect[1] > 0.7f && affect[2] < 0.3f) return "joy";
        if (affect[2] > 0.6f) return "fear";
        if (affect[3] > 0.7f) return "curiosity";
        if (affect[0] < 0.3f && affect[1] < 0.3f) return "sadness";
        return "neutral";
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

    public void shutdown() {
        dbExecutor.shutdown();
    }

    // ======================= الفئات الداخلية العامة =======================
    public static class IdentityResult {
        public final boolean isKnown;
        public final String name;
        public final float familiarity;
        public final String emotionalTone;
        public final String description;
        public IdentityResult(boolean k, String n, float f, String e, String d) {
            isKnown = k; name = n; familiarity = f; emotionalTone = e; description = d;
        }
    }

    // ✅ هذه الفئة يجب أن تكون public static حتى يمكن الوصول إليها من FirebaseSync
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

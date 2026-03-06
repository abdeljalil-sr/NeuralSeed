package com.lifeentity.perception;

import android.util.Log;

import com.google.mlkit.vision.face.Face;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class FaceIdentitySystem {
    private static final String TAG = "Identity";
    
    private Map<String, IdentityProfile> knownIdentities;
    private Map<String, String> temporaryEncounters;
    private java.security.MessageDigest hasher;
    
    public FaceIdentitySystem() {
        knownIdentities = new HashMap<>();
        temporaryEncounters = new HashMap<>();
        try {
            hasher = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            Log.e(TAG, "Hash not available");
        }
    }
    
    public String encodeFace(Face face) {
        float[] landmarks = extractSignature(face);
        
        StringBuilder seed = new StringBuilder();
        for (float f : landmarks) {
            seed.append(String.format("%.3f", f));
        }
        seed.append((int)(Math.random() * 1000));
        
        if (hasher != null) {
            byte[] hash = hasher.digest(seed.toString().getBytes());
            return bytesToHex(hash).substring(0, 16);
        }
        return String.valueOf(seed.toString().hashCode());
    }
    
    public IdentityResult recognizeOrLearn(Face face, String context) {
        String hash = encodeFace(face);
        String match = findApproximateMatch(hash);
        
        if (match != null) {
            IdentityProfile profile = knownIdentities.get(match);
            profile.recordEncounter(context, face.getSmilingProbability());
            
            return new IdentityResult(true, profile.name, profile.familiarity,
                profile.emotionalAssociation, "أعرف هذا الوجه");
        }
        
        temporaryEncounters.put(hash, "unknown_" + temporaryEncounters.size());
        return new IdentityResult(false, null, 0, "neutral", "وجه جديد");
    }
    
    public void nameIdentity(Face face, String name, String relationship) {
        String hash = encodeFace(face);
        String existing = findApproximateMatch(hash);
        
        IdentityProfile p;
        if (existing != null) {
            p = knownIdentities.get(existing);
            p.name = name;
            p.relationship = relationship;
        } else {
            p = new IdentityProfile(name, relationship, hash);
            knownIdentities.put(hash, p);
        }
        p.familiarity = 0.5f;
        
        Log.i(TAG, "Learned: " + name);
    }
    
    private String findApproximateMatch(String hash) {
        for (String known : knownIdentities.keySet()) {
            if (hammingDistance(hash, known) < 3) {
                return known;
            }
        }
        return null;
    }
    
    private float[] extractSignature(Face face) {
        float[] sig = new float[10];
        float w = face.getBoundingBox().width();
        float h = face.getBoundingBox().height();
        
        sig[0] = w / h;
        sig[1] = (face.getHeadEulerAngleY() + 90) / 180;
        sig[2] = (face.getHeadEulerAngleZ() + 90) / 180;
        
        Float smile = face.getSmilingProbability();
        Float left = face.getLeftEyeOpenProbability();
        Float right = face.getRightEyeOpenProbability();
        
        sig[3] = smile != null ? smile : 0.5f;
        sig[4] = left != null ? left : 0.5f;
        sig[5] = right != null ? right : 0.5f;
        
        for (int i = 6; i < 10; i++) {
            sig[i] = (float) Math.random();
        }
        
        return sig;
    }
    
    private int hammingDistance(String s1, String s2) {
        int dist = 0;
        int len = Math.min(s1.length(), s2.length());
        for (int i = 0; i < len; i++) {
            if (s1.charAt(i) != s2.charAt(i)) dist++;
        }
        return dist;
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    // الفئات الداخلية
    
    public static class IdentityProfile {
        public String name;
        public String relationship;
        public String faceHash;
        public float familiarity;
        public float trust;
        public String emotionalAssociation;
        public int encounterCount;
        public long firstSeen;
        public long lastSeen;
        
        IdentityProfile(String n, String r, String h) {
            this.name = n;
            this.relationship = r;
            this.faceHash = h;
            this.familiarity = 0;
            this.trust = 0.5f;
            this.emotionalAssociation = "neutral";
            this.encounterCount = 0;
            this.firstSeen = System.currentTimeMillis();
        }
        
        void recordEncounter(String context, Float smile) {
            encounterCount++;
            lastSeen = System.currentTimeMillis();
            familiarity = Math.min(1, familiarity + 0.05f);
            
            if (smile != null && smile > 0.7) {
                emotionalAssociation = "joy";
                trust = Math.min(1, trust + 0.1f);
            }
        }
    }
    
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
}

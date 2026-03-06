package com.lifeentity.sync;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.perception.FaceIdentitySystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * مزامنة التجارب بين الأجهزة عبر Firebase
 */
public class FirebaseSync {
    private static final String TAG = "FirebaseSync";
    private static final String COLLECTION_MEMORIES = "entity_memories";
    private static final String COLLECTION_IDENTITIES = "entity_identities";
    private static final String COLLECTION_EMBEDDINGS = "entity_embeddings";
    
    private FirebaseFirestore db;
    private Gson gson;
    private String deviceId;
    private SyncListener listener;
    
    public interface SyncListener {
        void onMemorySyncedFromCloud(String sourceDevice, EpisodicMemory.Event memory);
        void onIdentityLearnedFromOtherDevice(String name, String description);
        void onSyncComplete(int itemsSynced);
    }
    
    public FirebaseSync(String deviceId) {
        this.deviceId = deviceId;
        this.db = FirebaseFirestore.getInstance();
        this.gson = new Gson();
    }
    
    /**
     * رفع ذاكرة محلية إلى السحابة
     */
    public void uploadMemory(EpisodicMemory.Event event) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("timestamp", event.timestamp);
        data.put("narrative", event.narrative);
        data.put("emotionalState", event.emotionalState);
        data.put("emotionalIntensity", event.emotionalIntensity);
        data.put("sensoryHash", event.sensoryHash);
        
        // استخدام sensoryHash كمعرف فريد
        db.collection(COLLECTION_MEMORIES)
            .document(event.sensoryHash)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Memory uploaded"))
            .addOnFailureListener(e -> Log.e(TAG, "Upload failed", e));
    }
    
    /**
     * رفع هوية جديدة
     */
    public void uploadIdentity(FaceIdentitySystem.IdentityProfile identity) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("name", identity.name);
        data.put("relationship", identity.relationship);
        data.put("faceHash", identity.faceHash);
        data.put("familiarity", identity.familiarity);
        data.put("emotionalAssociation", identity.emotionalAssociation);
        data.put("encounterCount", identity.encounterCount);
        data.put("firstSeen", identity.firstSeen);
        data.put("lastSeen", identity.lastSeen);
        
        db.collection(COLLECTION_IDENTITIES)
            .document(identity.faceHash)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Identity uploaded: " + identity.name));
    }
    
    /**
     * رفع تمثيل دلالي (embedding)
     */
    public void uploadEmbedding(String concept, float[] embedding) {
        List<Double> embeddingList = new java.util.ArrayList<>();
        for (float f : embedding) embeddingList.add((double) f);
        
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("concept", concept);
        data.put("vector", embeddingList);
        data.put("learnedAt", System.currentTimeMillis());
        
        db.collection(COLLECTION_EMBEDDINGS)
            .document(concept)
            .set(data, SetOptions.merge());
    }
    
    /**
     * جلب ذكريات من أجهزة أخرى
     */
    public void syncMemoriesFromOthers(long sinceTimestamp) {
        db.collection(COLLECTION_MEMORIES)
            .whereGreaterThan("timestamp", sinceTimestamp)
            .whereNotEqualTo("deviceId", deviceId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int count = 0;
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String sourceDevice = doc.getString("deviceId");
                    EpisodicMemory.Event event = new EpisodicMemory.Event();
                    event.timestamp = doc.getLong("timestamp");
                    event.narrative = doc.getString("narrative");
                    event.emotionalState = doc.getString("emotionalState");
                    event.emotionalIntensity = doc.getDouble("emotionalIntensity");
                    event.sensoryHash = doc.getString("sensoryHash");
                    
                    if (listener != null) {
                        listener.onMemorySyncedFromCloud(sourceDevice, event);
                    }
                    count++;
                }
                
                if (listener != null) {
                    listener.onSyncComplete(count);
                }
            });
    }
    
    /**
     * الاستماع للتحديثات المباشرة
     */
    public void startRealtimeSync() {
        // ذكريات جديدة من أي جهاز
        db.collection(COLLECTION_MEMORIES)
            .whereNotEqualTo("deviceId", deviceId)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Log.e(TAG, "Listen failed", e);
                    return;
                }
                
                for (DocumentSnapshot doc : snapshots.getDocumentChanges()) {
                    if (doc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        // معالجة الذكرية الجديدة
                    }
                }
            });
    }
    
    /**
     * دمج ذاكرة من جهاز آخر
     */
    public void integrateForeignMemory(EpisodicMemory.Event foreign) {
        // وضع علامة أنها من "تجربة غير مباشرة"
        foreign.narrative = "[من جهاز آخر] " + foreign.narrative;
        
        // تقليل الشدة العاطفية (ليس تجربة مباشرة)
        foreign.emotionalIntensity *= 0.6;
        
        // حفظ محلياً
        // ... عبر MemoryDao
    }
    
    public void setListener(SyncListener l) {
        this.listener = l;
    }
}

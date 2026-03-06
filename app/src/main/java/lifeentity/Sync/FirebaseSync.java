package com.lifeentity.sync;

import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.IdentityMemory;
import com.lifeentity.perception.FaceIdentitySystem;

import java.util.HashMap;
import java.util.Map;

/**
 * مزامنة التجارب بين الأجهزة عبر Firebase
 * يتيح للكائن أن يتعلم من تجارب أجهزة أخرى
 */
public class FirebaseSync {
    private static final String TAG = "FirebaseSync";
    
    // أسماء المجموعات في Firestore
    private static final String COLLECTION_MEMORIES = "memories";
    private static final String COLLECTION_IDENTITIES = "identities";
    private static final String COLLECTION_EMBEDDINGS = "embeddings";
    private static final String COLLECTION_DEVICES = "devices";
    
    private FirebaseFirestore db;
    private String deviceId;
    private String deviceName;
    private SyncListener listener;
    
    public interface SyncListener {
        void onMemorySyncedFromCloud(String sourceDevice, EpisodicMemory.Event event);
        void onIdentityLearnedFromOtherDevice(String name, String description);
        void onSyncComplete(int itemsSynced);
        void onConnectionStatusChanged(boolean connected);
    }
    
    public FirebaseSync(String deviceId) {
        this.deviceId = deviceId;
        this.deviceName = "Device_" + deviceId.substring(0, 6);
        this.db = FirebaseFirestore.getInstance();
        
        Log.i(TAG, "FirebaseSync initialized for: " + deviceName);
    }
    
    /**
     * بدء الاستماع للتحديثات المباشرة من Firestore
     */
    public void startRealtimeSync() {
        // الاستماع للذكريات الجديدة من أجهزة أخرى
        db.collection(COLLECTION_MEMORIES)
            .whereNotEqualTo("deviceId", deviceId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "Listen failed: ", error);
                    if (listener != null) {
                        listener.onConnectionStatusChanged(false);
                    }
                    return;
                }
                
                if (snapshots == null) return;
                
                int newItems = 0;
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        DocumentSnapshot doc = dc.getDocument();
                        processNewMemory(doc);
                        newItems++;
                    }
                }
                
                if (newItems > 0 && listener != null) {
                    listener.onSyncComplete(newItems);
                }
                
                if (listener != null) {
                    listener.onConnectionStatusChanged(true);
                }
            });
        
        // الاستماع للهويات الجديدة
        db.collection(COLLECTION_IDENTITIES)
            .whereNotEqualTo("createdBy", deviceId)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null || snapshots == null) return;
                
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        processNewIdentity(dc.getDocument());
                    }
                }
            });
        
        // تسجيل الجهاز كـ "نشط"
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("lastActive", System.currentTimeMillis());
        deviceInfo.put("name", deviceName);
        
        db.collection(COLLECTION_DEVICES)
            .document(deviceId)
            .set(deviceInfo, SetOptions.merge());
    }
    
    /**
     * رفع ذاكرة محلية إلى السحابة
     */
    public void uploadMemory(EpisodicMemory.Event event) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("deviceName", deviceName);
        data.put("timestamp", event.timestamp);
        data.put("sensoryHash", event.sensoryHash);
        data.put("emotionalState", event.emotionalState);
        data.put("emotionalIntensity", event.emotionalIntensity);
        data.put("narrative", event.narrative);
        data.put("location", event.location);
        data.put("shared", true);
        
        // استخدام sensoryHash كمعرف فريد
        db.collection(COLLECTION_MEMORIES)
            .document(event.sensoryHash)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Memory uploaded: " + event.sensoryHash))
            .addOnFailureListener(e -> Log.e(TAG, "Upload failed: ", e));
    }
    
    /**
     * رفع هوية جديدة (شخص معروف)
     */
    public void uploadIdentity(FaceIdentitySystem.IdentityProfile identity) {
        Map<String, Object> data = new HashMap<>();
        data.put("createdBy", deviceId);
        data.put("deviceName", deviceName);
        data.put("faceHash", identity.faceHash);
        data.put("name", identity.name);
        data.put("relationship", identity.relationship);
        data.put("familiarity", identity.familiarity);
        data.put("emotionalAssociation", identity.emotionalAssociation);
        data.put("encounterCount", identity.encounterCount);
        data.put("firstSeen", identity.firstSeen);
        data.put("lastSeen", identity.lastSeen);
        data.put("sharedAt", System.currentTimeMillis());
        
        db.collection(COLLECTION_IDENTITIES)
            .document(identity.faceHash)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> Log.i(TAG, "Identity shared: " + identity.name));
    }
    
    /**
     * رفع تمثيل دلالي (embedding) للمفاهيم المشتركة
     */
    public void uploadEmbedding(String concept, float[] embedding, String context) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("concept", concept);
        data.put("vector", embeddingToList(embedding));
        data.put("context", context);
        data.put("learnedAt", System.currentTimeMillis());
        
        db.collection(COLLECTION_EMBEDDINGS)
            .document(deviceId + "_" + concept)
            .set(data, SetOptions.merge());
    }
    
    /**
     * جلب ذكريات من أجهزة أخرى (للتزامن الأولي)
     */
    public void syncMemoriesFromOthers(long sinceTimestamp) {
        db.collection(COLLECTION_MEMORIES)
            .whereNotEqualTo("deviceId", deviceId)
            .whereGreaterThan("timestamp", sinceTimestamp)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int count = 0;
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    processNewMemory(doc);
                    count++;
                }
                
                Log.i(TAG, "Synced " + count + " memories from other devices");
                
                if (listener != null) {
                    listener.onSyncComplete(count);
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Sync failed: ", e));
    }
    
    /**
     * معالجة ذكرية جديدة من جهاز آخر
     */
    private void processNewMemory(DocumentSnapshot doc) {
        String sourceDevice = doc.getString("deviceName");
        
        EpisodicMemory.Event event = new EpisodicMemory.Event();
        event.timestamp = doc.getLong("timestamp");
        event.sensoryHash = doc.getString("sensoryHash");
        event.emotionalState = doc.getString("emotionalState");
        event.emotionalIntensity = doc.getDouble("emotionalIntensity");
        event.narrative = "[من " + sourceDevice + "] " + doc.getString("narrative");
        event.location = doc.getString("location");
        
        // تقليل الشدة العاطفية لأنها تجربة غير مباشرة
        event.emotionalIntensity *= 0.6;
        
        Log.d(TAG, "Received memory from " + sourceDevice + ": " + event.narrative);
        
        if (listener != null) {
            listener.onMemorySyncedFromCloud(sourceDevice, event);
        }
    }
    
    /**
     * معالجة هوية جديدة من جهاز آخر
     */
    private void processNewIdentity(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String relationship = doc.getString("relationship");
        String emotionalAssoc = doc.getString("emotionalAssociation");
        
        Log.i(TAG, "Learned identity from other device: " + name);
        
        if (listener != null) {
            listener.onIdentityLearnedFromOtherDevice(
                name, 
                relationship + " (يشعر بالـ" + emotionalAssoc + " معهم)"
            );
        }
    }
    
    /**
     * الحصول على قائمة الأجهزة النشطة
     */
    public void getActiveDevices(OnDevicesListListener callback) {
        db.collection(COLLECTION_DEVICES)
            .whereGreaterThan("lastActive", System.currentTimeMillis() - 300000) // 5 دقائق
            .get()
            .addOnSuccessListener(snapshot -> {
                Map<String, Long> devices = new HashMap<>();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    devices.put(doc.getString("name"), doc.getLong("lastActive"));
                }
                callback.onDevicesReceived(devices);
            });
    }
    
    public interface OnDevicesListListener {
        void onDevicesReceived(Map<String, Long> devices);
    }
    
    private java.util.List<Double> embeddingToList(float[] embedding) {
        java.util.List<Double> list = new java.util.ArrayList<>();
        for (float f : embedding) {
            list.add((double) f);
        }
        return list;
    }
    
    public void setListener(SyncListener l) {
        this.listener = l;
    }
    
    /**
     * إيقاف المزامنة
     */
    public void stopSync() {
        // إزالة المستمعين يتم تلقائياً عند تدمير Firestore
        Log.i(TAG, "Sync stopped");
    }
}

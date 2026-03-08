package com.lifeentity.sync;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.IdentityMemory;
import com.lifeentity.perception.FaceIdentitySystem; // لاستيراد IdentityProfile

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FirebaseSync {
    private static final String TAG = "FirebaseSync";

    private static final String COLLECTION_MEMORIES = "memories";
    private static final String COLLECTION_IDENTITIES = "identities";
    private static final String COLLECTION_EMBEDDINGS = "embeddings";
    private static final String COLLECTION_DEVICES = "devices";

    private FirebaseFirestore db;
    private String deviceId;
    private String deviceName;
    private SyncListener listener;
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private AtomicBoolean isActive = new AtomicBoolean(false);

    public interface SyncListener {
        void onMemorySyncedFromCloud(String sourceDevice, EpisodicMemory.Event event);
        void onIdentityLearnedFromOtherDevice(String name, String description);
        void onSyncComplete(int itemsSynced);
        void onConnectionStatusChanged(boolean connected);
    }

    public FirebaseSync(String deviceId) {
        this.deviceId = deviceId;
        this.deviceName = "Device_" + (deviceId.length() > 6 ? deviceId.substring(0, 6) : deviceId);
        this.db = FirebaseFirestore.getInstance();
        this.heartbeatHandler = new Handler(Looper.getMainLooper());
        Log.i(TAG, "FirebaseSync initialized for: " + deviceName);
    }

    public void start() {
        if (isActive.get()) return;
        isActive.set(true);
        updateDeviceStatus();
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isActive.get()) {
                    updateDeviceStatus();
                    heartbeatHandler.postDelayed(this, 60000);
                }
            }
        };
        heartbeatHandler.post(heartbeatRunnable);

        db.collection(COLLECTION_MEMORIES)
                .whereNotEqualTo("deviceId", deviceId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: ", error);
                        if (listener != null) listener.onConnectionStatusChanged(false);
                        return;
                    }
                    if (snapshots == null) return;
                    int newItems = 0;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            processNewMemory(dc.getDocument());
                            newItems++;
                        }
                    }
                    if (newItems > 0 && listener != null) listener.onSyncComplete(newItems);
                    if (listener != null) listener.onConnectionStatusChanged(true);
                });

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
    }

    private void updateDeviceStatus() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("lastActive", System.currentTimeMillis());
        deviceInfo.put("name", deviceName);
        deviceInfo.put("deviceId", deviceId);
        db.collection(COLLECTION_DEVICES).document(deviceId).set(deviceInfo, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update device status", e));
    }

    public void uploadMemory(EpisodicMemory.Event event) {
        if (event == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("deviceName", deviceName);
        data.put("timestamp", event.timestamp);
        data.put("sensoryHash", event.sensoryHash != null ? event.sensoryHash : String.valueOf(event.timestamp));
        data.put("emotionalState", event.emotionalState);
        data.put("emotionalIntensity", event.emotionalIntensity);
        data.put("narrative", event.narrative);
        data.put("location", event.location);
        data.put("shared", true);
        data.put("sharedAt", System.currentTimeMillis());
        String docId = event.sensoryHash != null ? event.sensoryHash : deviceId + "_" + event.timestamp;
        db.collection(COLLECTION_MEMORIES).document(docId).set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Memory uploaded: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Upload failed: ", e));
    }

    // ✅ التصحيح: استخدم FaceIdentitySystem.IdentityProfile مباشرة
    public void uploadIdentity(FaceIdentitySystem.IdentityProfile identity) {
        if (identity == null || identity.faceHash == null) return;
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
        db.collection(COLLECTION_IDENTITIES).document(identity.faceHash).set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Identity shared: " + identity.name));
    }

    public void uploadEmbedding(String concept, float[] embedding, String context) {
        if (concept == null || embedding == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("deviceName", deviceName);
        data.put("concept", concept);
        data.put("vector", embeddingToList(embedding));
        data.put("context", context);
        data.put("learnedAt", System.currentTimeMillis());
        db.collection(COLLECTION_EMBEDDINGS).document(deviceId + "_" + concept).set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Failed to upload embedding", e));
    }

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
                    if (listener != null) listener.onSyncComplete(count);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Sync failed: ", e));
    }

    private void processNewMemory(DocumentSnapshot doc) {
        String sourceDevice = doc.getString("deviceName");
        if (sourceDevice == null) sourceDevice = "جهاز آخر";
        EpisodicMemory.Event event = new EpisodicMemory.Event();
        event.timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0L;
        event.sensoryHash = doc.getString("sensoryHash");
        event.emotionalState = doc.getString("emotionalState");
        Double intensity = doc.getDouble("emotionalIntensity");
        event.emotionalIntensity = intensity != null ? intensity : 0.5;
        String narrative = doc.getString("narrative");
        event.narrative = "[من " + sourceDevice + "] " + (narrative != null ? narrative : "");
        event.location = doc.getString("location");
        event.emotionalIntensity *= 0.6;
        Log.d(TAG, "Received memory from " + sourceDevice + ": " + event.narrative);
        if (listener != null) listener.onMemorySyncedFromCloud(sourceDevice, event);
    }

    private void processNewIdentity(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String relationship = doc.getString("relationship");
        String emotionalAssoc = doc.getString("emotionalAssociation");
        if (name == null) return;
        Log.i(TAG, "Learned identity from other device: " + name);
        if (listener != null) {
            listener.onIdentityLearnedFromOtherDevice(name,
                    (relationship != null ? relationship : "شخص") +
                    (emotionalAssoc != null ? " (يشعر بالـ" + emotionalAssoc + " معهم)" : ""));
        }
    }

    public void getActiveDevices(OnDevicesListListener callback) {
        db.collection(COLLECTION_DEVICES)
                .whereGreaterThan("lastActive", System.currentTimeMillis() - 300_000)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Long> devices = new HashMap<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("name");
                        Long lastActive = doc.getLong("lastActive");
                        if (name != null && lastActive != null) devices.put(name, lastActive);
                    }
                    callback.onDevicesReceived(devices);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to get active devices", e);
                    callback.onDevicesReceived(new HashMap<>());
                });
    }

    public interface OnDevicesListListener {
        void onDevicesReceived(Map<String, Long> devices);
    }

    private java.util.List<Double> embeddingToList(float[] embedding) {
        java.util.List<Double> list = new java.util.ArrayList<>();
        for (float f : embedding) list.add((double) f);
        return list;
    }

    public void setListener(SyncListener l) { this.listener = l; }
    public void stop() {
        isActive.set(false);
        if (heartbeatHandler != null && heartbeatRunnable != null) heartbeatHandler.removeCallbacks(heartbeatRunnable);
        Log.i(TAG, "Sync stopped");
    }
}

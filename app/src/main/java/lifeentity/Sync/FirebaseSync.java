package com.lifeentity.sync;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.IdentityMemory;
import com.lifeentity.perception.FaceIdentitySystem;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * نظام مزامنة متقدم مع Firebase
 * يدعم العتبات المنخفضة، ضغط الصور، Storage، ودمج الهويات الذكي
 */
public class FirebaseSync {
    private static final String TAG = "FirebaseSync";

    // عتبات المزامنة (خفضت من 0.7 إلى 0.4)
    private static final float MEMORY_SYNC_THRESHOLD = 0.4f;
    private static final float IDENTITY_SYNC_THRESHOLD = 0.3f;
    private static final float CONFLICT_FAMILIARITY_THRESHOLD = 0.2f;

    private static final String COLLECTION_MEMORIES = "memories";
    private static final String COLLECTION_IDENTITIES = "identities";
    private static final String COLLECTION_EMBEDDINGS = "embeddings";
    private static final String COLLECTION_DEVICES = "devices";
    private static final String STORAGE_BUCKET = "gs://lifeentity.appspot.com"; // تعديل حسب مشروعك

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String deviceId;
    private String deviceName;
    private SyncListener listener;
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private AtomicBoolean isActive = new AtomicBoolean(false);

    // تتبع الهويات المتعارضة للدمج
    private ConcurrentHashMap<String, List<IdentityConflict>> pendingIdentityMerges;

    public interface SyncListener {
        void onMemorySyncedFromCloud(String sourceDevice, EpisodicMemory.Event event, String thumbnailBase64);
        void onIdentityLearnedFromOtherDevice(String name, String description, FaceIdentitySystem.IdentityProfile mergedProfile);
        void onIdentityConflictDetected(String faceHash, List<FaceIdentitySystem.IdentityProfile> conflictingProfiles);
        void onSyncComplete(int itemsSynced);
        void onConnectionStatusChanged(boolean connected);
        void onThumbnailDownloaded(String memoryId, Bitmap thumbnail);
    }

    public FirebaseSync(String deviceId) {
        this.deviceId = deviceId;
        this.deviceName = "Device_" + (deviceId.length() > 6 ? deviceId.substring(0, 6) : deviceId);
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.storageRef = storage.getReferenceFromUrl(STORAGE_BUCKET).child("thumbnails");
        this.heartbeatHandler = new Handler(Looper.getMainLooper());
        this.pendingIdentityMerges = new ConcurrentHashMap<>();
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
                    processPendingIdentityMerges();
                    heartbeatHandler.postDelayed(this, 60000);
                }
            }
        };
        heartbeatHandler.post(heartbeatRunnable);

        // الاستماع للذكريات بعتبة منخفضة
        db.collection(COLLECTION_MEMORIES)
                .whereNotEqualTo("deviceId", deviceId)
                .whereGreaterThan("emotionalIntensity", MEMORY_SYNC_THRESHOLD)
                .orderBy("emotionalIntensity", Query.Direction.DESCENDING)
                .limit(100)
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

        // الاستماع للهويات مع كشف التعارض
        db.collection(COLLECTION_IDENTITIES)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        switch (dc.getType()) {
                            case ADDED:
                                detectAndQueueIdentityConflict(dc.getDocument());
                                break;
                            case MODIFIED:
                                detectAndQueueIdentityConflict(dc.getDocument());
                                break;
                        }
                    }
                });
    }

    /**
     * رفع ذكرى مع صورة مصغرة مضغوطة
     */
    public void uploadMemory(EpisodicMemory.Event event, Bitmap thumbnail) {
        if (event == null) return;
        
        // رفع الصورة إلى Storage أولاً
        if (thumbnail != null) {
            uploadThumbnailToStorage(event.sensoryHash, thumbnail, thumbnailUrl -> {
                // بعد نجاح رفع الصورة، ارفع البيانات النصية
                uploadMemoryData(event, thumbnailUrl);
            });
        } else {
            uploadMemoryData(event, null);
        }
    }

    private void uploadMemoryData(EpisodicMemory.Event event, String thumbnailUrl) {
        // فقط إذا كانت الشدة أعلى من العتبة المنخفضة (0.4)
        if (event.emotionalIntensity < MEMORY_SYNC_THRESHOLD) {
            Log.d(TAG, "Memory intensity " + event.emotionalIntensity + " below threshold, skipping sync");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("deviceName", deviceName);
        data.put("timestamp", event.timestamp);
        data.put("sensoryHash", event.sensoryHash != null ? event.sensoryHash : String.valueOf(event.timestamp));
        data.put("emotionalState", event.emotionalState);
        data.put("emotionalIntensity", event.emotionalIntensity);
        data.put("narrative", event.narrative);
        data.put("location", event.location);
        data.put("thumbnailUrl", thumbnailUrl); // رابط Storage بدلاً من Base64
        data.put("shared", true);
        data.put("sharedAt", System.currentTimeMillis());
        data.put("syncPriority", calculateSyncPriority(event)); // أولوية بناءً على الأهمية

        String docId = event.sensoryHash != null ? event.sensoryHash : deviceId + "_" + event.timestamp;
        
        db.collection(COLLECTION_MEMORIES).document(docId).set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Memory uploaded: " + docId + " (intensity: " + event.emotionalIntensity + ")"))
                .addOnFailureListener(e -> Log.e(TAG, "Upload failed: ", e));
    }

    /**
     * رفع صورة مصغرة مضغوطة إلى Firebase Storage
     */
    private void uploadThumbnailToStorage(String id, Bitmap thumbnail, OnThumbnailUploaded callback) {
        if (thumbnail == null || id == null) {
            if (callback != null) callback.onUploaded(null);
            return;
        }

        // ضغط الصورة
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, baos); // ضغط 70% للتوازن
        byte[] data = baos.toByteArray();

        // إذا كانت الصغيرة جداً، استخدم Base64 في Firestore مباشرة
        if (data.length < 1024 * 1024) { // أقل من 1MB
            StorageReference ref = storageRef.child(deviceId + "/" + id + ".jpg");
            UploadTask uploadTask = ref.putBytes(data);
            
            uploadTask.addOnSuccessListener(taskSnapshot -> 
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (callback != null) callback.onUploaded(uri.toString());
                })
            ).addOnFailureListener(e -> {
                Log.w(TAG, "Storage upload failed, falling back to Base64", e);
                // fallback: استخدام Base64 في Firestore
                String base64 = Base64.encodeToString(data, Base64.DEFAULT);
                if (callback != null) callback.onUploaded("base64://" + base64);
            });
        } else {
            // للصور الكبيرة، اضغط أكثر
            baos.reset();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            data = baos.toByteArray();
            String base64 = Base64.encodeToString(data, Base64.DEFAULT);
            if (callback != null) callback.onUploaded("base64://" + base64);
        }
    }

    /**
     * رفع هوية مع دمج ذكي للمتعارضة
     */
    public void uploadIdentity(FaceIdentitySystem.IdentityProfile identity) {
        if (identity == null || identity.faceHash == null) return;
        
        // فقط إذا كانت الألفة أعلى من العتبة المنخفضة
        if (identity.familiarity < IDENTITY_SYNC_THRESHOLD) {
            Log.d(TAG, "Identity familiarity " + identity.familiarity + " below threshold, skipping sync");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("createdBy", deviceId);
        data.put("deviceName", deviceName);
        data.put("faceHash", identity.faceHash);
        data.put("faceEmbedding", embeddingToList(identity.faceEmbedding)); // للمقارنة المستقبلية
        data.put("name", identity.name);
        data.put("relationship", identity.relationship);
        data.put("familiarity", identity.familiarity);
        data.put("trust", identity.trust);
        data.put("emotionalAssociation", identity.emotionalAssociation);
        data.put("encounterCount", identity.encounterCount);
        data.put("firstSeen", identity.firstSeen);
        data.put("lastSeen", identity.lastSeen);
        data.put("sharedAt", System.currentTimeMillis());
        data.put("needsMerge", false); // علامة للدمج

        // استخدام faceHash كمعرف موحد للكشف عن التعارض
        db.collection(COLLECTION_IDENTITIES).document(identity.faceHash)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.i(TAG, "Identity shared: " + identity.name + " (familiarity: " + identity.familiarity + ")"))
                .addOnFailureListener(e -> Log.e(TAG, "Identity upload failed: ", e));
    }

    /**
     * كشف ووضع في قائمة انتظار للهويات المتعارضة
     */
    private void detectAndQueueIdentityConflict(DocumentSnapshot doc) {
        String faceHash = doc.getString("faceHash");
        String createdBy = doc.getString("createdBy");
        
        if (faceHash == null || deviceId.equals(createdBy)) return;

        FaceIdentitySystem.IdentityProfile profile = documentToProfile(doc);
        
        pendingIdentityMerges.computeIfAbsent(faceHash, k -> new ArrayList<>())
                .add(new IdentityConflict(profile, doc.getId()));
        
        Log.d(TAG, "Queued potential identity conflict for: " + faceHash);
    }

    /**
     * معالجة دمج الهويات المتعارضة
     */
    private void processPendingIdentityMerges() {
        for (Map.Entry<String, List<IdentityConflict>> entry : pendingIdentityMerges.entrySet()) {
            String faceHash = entry.getKey();
            List<IdentityConflict> conflicts = entry.getValue();
            
            if (conflicts.size() < 2) continue; // لا يوجد تعارض حقيقي

            // دمج الهويات المتعددة
            FaceIdentitySystem.IdentityProfile merged = mergeIdentityProfiles(
                conflicts.stream().map(c -> c.profile).toList()
            );

            // تحديث الوثيقة المدمجة
            updateMergedIdentity(faceHash, merged, conflicts);
            
            // إعلام المستمع
            if (listener != null) {
                listener.onIdentityConflictDetected(faceHash, 
                    conflicts.stream().map(c -> c.profile).toList());
                listener.onIdentityLearnedFromOtherDevice(
                    merged.name, 
                    "دمج من " + conflicts.size() + " أجهزة",
                    merged
                );
            }
            
            // مسح من قائمة الانتظار
            pendingIdentityMerges.remove(faceHash);
        }
    }

    /**
     * دمج عدة هويات في واحدة ذكية
     */
    private FaceIdentitySystem.IdentityProfile mergeIdentityProfiles(
            List<FaceIdentitySystem.IdentityProfile> profiles) {
        
        if (profiles.isEmpty()) return null;
        if (profiles.size() == 1) return profiles.get(0);

        FaceIdentitySystem.IdentityProfile merged = new FaceIdentitySystem.IdentityProfile(
            "دمج", "متعدد", profiles.get(0).faceHash
        );

        // اختيار الاسم الأكثر ثقة (أعلى familiarity)
        FaceIdentitySystem.IdentityProfile mostTrusted = profiles.stream()
                .max((a, b) -> Float.compare(a.familiarity * a.trust, b.familiarity * b.trust))
                .orElse(profiles.get(0));
        merged.name = mostTrusted.name;

        // دمج الألفة والثقة (متوسط مرجح)
        float totalFamiliarity = 0, totalTrust = 0, totalEncounters = 0;
        String dominantEmotion = null;
        int emotionCount = 0;
        
        for (FaceIdentitySystem.IdentityProfile p : profiles) {
            totalFamiliarity += p.familiarity;
            totalTrust += p.trust;
            totalEncounters += p.encounterCount;
            
            if (p.emotionalAssociation != null) {
                dominantEmotion = p.emotionalAssociation;
                emotionCount++;
            }
        }
        
        merged.familiarity = Math.min(1.0f, totalFamiliarity / profiles.size() * 1.2f); // تعزيز للدمج
        merged.trust = totalTrust / profiles.size();
        merged.encounterCount = (int) totalEncounters;
        merged.emotionalAssociation = dominantEmotion;
        merged.firstSeen = profiles.stream().mapToLong(p -> p.firstSeen).min().orElse(System.currentTimeMillis());
        merged.lastSeen = profiles.stream().mapToLong(p -> p.lastSeen).max().orElse(System.currentTimeMillis());

        // دمج الـ embeddings (متوسط)
        if (!profiles.isEmpty() && profiles.get(0).faceEmbedding != null) {
            int embSize = profiles.get(0).faceEmbedding.length;
            float[] mergedEmb = new float[embSize];
            for (FaceIdentitySystem.IdentityProfile p : profiles) {
                if (p.faceEmbedding != null) {
                    for (int i = 0; i < embSize; i++) {
                        mergedEmb[i] += p.faceEmbedding[i];
                    }
                }
            }
            for (int i = 0; i < embSize; i++) {
                mergedEmb[i] /= profiles.size();
            }
            merged.faceEmbedding = mergedEmb;
        }

        return merged;
    }

    private void updateMergedIdentity(String faceHash, FaceIdentitySystem.IdentityProfile merged, 
                                     List<IdentityConflict> sources) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", merged.name);
        data.put("familiarity", merged.familiarity);
        data.put("trust", merged.trust);
        data.put("emotionalAssociation", merged.emotionalAssociation);
        data.put("encounterCount", merged.encounterCount);
        data.put("mergedFrom", sources.size());
        data.put("mergedAt", System.currentTimeMillis());
        data.put("mergedBy", deviceName);
        data.put("needsMerge", false);

        // تحديث الوثيقة الرئيسية وحذف المتعارضة
        WriteBatch batch = db.batch();
        
        // تحديث الوثيقة الرئيسية
        batch.set(db.collection(COLLECTION_IDENTITIES).document(faceHash), data, SetOptions.merge());
        
        // حذف أو دمج الوثائق الأخرى
        for (int i = 1; i < sources.size(); i++) {
            batch.delete(db.collection(COLLECTION_IDENTITIES).document(sources.get(i).documentId));
        }
        
        batch.commit()
            .addOnSuccessListener(aVoid -> Log.i(TAG, "Successfully merged identity: " + merged.name))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to merge identity: ", e));
    }

    private void processNewMemory(DocumentSnapshot doc) {
        String sourceDevice = doc.getString("deviceName");
        if (sourceDevice == null) sourceDevice = "جهاز آخر";
        
        EpisodicMemory.Event event = new EpisodicMemory.Event();
        event.timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0L;
        event.sensoryHash = doc.getString("sensoryHash");
        event.emotionalState = doc.getString("emotionalState");
        Double intensity = doc.getDouble("emotionalIntensity");
        event.emotionalIntensity = intensity != null ? intensity.floatValue() : 0.5f;
        String narrative = doc.getString("narrative");
        event.narrative = "[من " + sourceDevice + "] " + (narrative != null ? narrative : "");
        event.location = doc.getString("location");
        
        // تقليل الشدة قليلاً للذكريات المستوردة
        event.emotionalIntensity *= 0.8f;

        // جلب الصورة المصغرة إذا وجدت
        String thumbnailUrl = doc.getString("thumbnailUrl");
        String thumbnailBase64 = null;
        
        if (thumbnailUrl != null && thumbnailUrl.startsWith("base64://")) {
            thumbnailBase64 = thumbnailUrl.substring(9);
        }

        Log.d(TAG, "Received memory from " + sourceDevice + ": " + event.narrative + 
              " (intensity: " + event.emotionalIntensity + ")");

        if (listener != null) {
            listener.onMemorySyncedFromCloud(sourceDevice, event, thumbnailBase64);
        }

        // جلب الصورة من Storage إذا كانت URL
        if (thumbnailUrl != null && !thumbnailUrl.startsWith("base64://") && listener != null) {
            downloadThumbnail(thumbnailUrl, event.sensoryHash);
        }
    }

    private void downloadThumbnail(String url, String memoryId) {
        // في التطبيق الحقيقي، استخدم Glide أو Picasso
        // هنا نكتفي بالإعلام
        Log.d(TAG, "Thumbnail available at: " + url);
    }

    private FaceIdentitySystem.IdentityProfile documentToProfile(DocumentSnapshot doc) {
        FaceIdentitySystem.IdentityProfile profile = new FaceIdentitySystem.IdentityProfile(
            doc.getString("name"),
            doc.getString("relationship"),
            doc.getString("faceHash")
        );
        
        profile.familiarity = doc.getDouble("familiarity") != null ? doc.getDouble("familiarity").floatValue() : 0;
        profile.trust = doc.getDouble("trust") != null ? doc.getDouble("trust").floatValue() : 0.5f;
        profile.emotionalAssociation = doc.getString("emotionalAssociation");
        profile.encounterCount = doc.getLong("encounterCount") != null ? doc.getLong("encounterCount").intValue() : 0;
        profile.firstSeen = doc.getLong("firstSeen") != null ? doc.getLong("firstSeen") : 0;
        profile.lastSeen = doc.getLong("lastSeen") != null ? doc.getLong("lastSeen") : 0;
        
        List<Double> embList = (List<Double>) doc.get("faceEmbedding");
        if (embList != null) {
            profile.faceEmbedding = new float[embList.size()];
            for (int i = 0; i < embList.size(); i++) {
                profile.faceEmbedding[i] = embList.get(i).floatValue();
            }
        }
        
        return profile;
    }

    // ==================== باقي الوظائف ====================

    private void updateDeviceStatus() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("lastActive", System.currentTimeMillis());
        deviceInfo.put("name", deviceName);
        deviceInfo.put("deviceId", deviceId);
        deviceInfo.put("syncThreshold", MEMORY_SYNC_THRESHOLD); // إعلام الآخرين بالعتبة
        
        db.collection(COLLECTION_DEVICES).document(deviceId).set(deviceInfo, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Failed to update device status", e));
    }

    private float calculateSyncPriority(EpisodicMemory.Event event) {
        // أولوية أعلى للأحداث العاطفية أو الحديثة
        float recency = 1.0f - Math.min(1.0f, (System.currentTimeMillis() - event.timestamp) / (24f * 3600 * 1000));
        return event.emotionalIntensity * 0.6f + recency * 0.4f;
    }

    public void syncMemoriesFromOthers(long sinceTimestamp) {
        db.collection(COLLECTION_MEMORIES)
                .whereNotEqualTo("deviceId", deviceId)
                .whereGreaterThan("timestamp", sinceTimestamp)
                .whereGreaterThan("emotionalIntensity", MEMORY_SYNC_THRESHOLD)
                .orderBy("emotionalIntensity", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = 0;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        processNewMemory(doc);
                        count++;
                    }
                    Log.i(TAG, "Synced " + count + " memories from other devices (threshold: " + MEMORY_SYNC_THRESHOLD + ")");
                    if (listener != null) listener.onSyncComplete(count);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Sync failed: ", e));
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

    private interface OnThumbnailUploaded {
        void onUploaded(String urlOrBase64);
    }

    private List<Double> embeddingToList(float[] embedding) {
        List<Double> list = new ArrayList<>();
        for (float f : embedding) list.add((double) f);
        return list;
    }

    private static class IdentityConflict {
        final FaceIdentitySystem.IdentityProfile profile;
        final String documentId;
        
        IdentityConflict(FaceIdentitySystem.IdentityProfile profile, String documentId) {
            this.profile = profile;
            this.documentId = documentId;
        }
    }

    public void setListener(SyncListener l) { this.listener = l; }
    
    public void stop() {
        isActive.set(false);
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
        // معالجة أي دمج معلق قبل التوقف
        processPendingIdentityMerges();
        Log.i(TAG, "Sync stopped");
    }

    public float getMemorySyncThreshold() { return MEMORY_SYNC_THRESHOLD; }
    public float getIdentitySyncThreshold() { return IDENTITY_SYNC_THRESHOLD; }
}

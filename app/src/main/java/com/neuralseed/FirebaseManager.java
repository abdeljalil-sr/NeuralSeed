package com.neuralseed;

import android.content.Context;
import android.util.Log;
import com.google.firebase.database.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;

/**
 * مدير Firebase - المزامنة السحابية والتخزين
 */
public class FirebaseManager {
    
    private FirebaseDatabase database;
    private DatabaseReference lexiconRef;
    private DatabaseReference conversationsRef;
    private DatabaseReference stateRef;
    private FirebaseAuth auth;
    
    private String deviceId;
    private boolean isInitialized = false;
    
    public interface SyncListener {
        void onWordSynced(String word, String meaning);
        void onSyncError(String error);
        void onConnectionStateChanged(boolean connected);
    }
    
    private SyncListener listener;
    
    public FirebaseManager(Context context) {
        try {
            database = FirebaseDatabase.getInstance();
            auth = FirebaseAuth.getInstance();
            deviceId = getDeviceId(context);
            
            // المصادقة المجهولة
            authenticateAnonymous();
            
        } catch (Exception e) {
            Log.e("FirebaseManager", "Initialization error: " + e.getMessage());
        }
    }
    
    private void authenticateAnonymous() {
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                .addOnSuccessListener(result -> {
                    Log.i("FirebaseManager", "Anonymous auth successful");
                    initializeReferences();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseManager", "Auth failed: " + e.getMessage());
                    if (listener != null) listener.onSyncError("Authentication failed");
                });
        } else {
            initializeReferences();
        }
    }
    
    private void initializeReferences() {
        lexiconRef = database.getReference("lexicon");
        conversationsRef = database.getReference("conversations").child(deviceId);
        stateRef = database.getReference("states").child(deviceId);
        isInitialized = true;
        
        // مراقبة حالة الاتصال
        DatabaseReference connectedRef = database.getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (listener != null) {
                    listener.onConnectionStateChanged(connected);
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseManager", "Connection listener cancelled");
            }
        });
    }
    
    /**
     * حفظ كلمة في السحابة
     */
    public void saveWord(String word, String meaning) {
        if (!isInitialized || lexiconRef == null) return;
        
        lexiconRef.child(word).setValue(meaning)
            .addOnSuccessListener(aVoid -> {
                Log.i("FirebaseManager", "Word saved: " + word);
            })
            .addOnFailureListener(e -> {
                Log.e("FirebaseManager", "Failed to save word: " + e.getMessage());
            });
    }
    
    /**
     * استرجاع كلمة من السحابة
     */
    public void loadWord(String word, final WordLoadCallback callback) {
        if (!isInitialized || lexiconRef == null) {
            if (callback != null) callback.onWordLoaded(null, null);
            return;
        }
        
        lexiconRef.child(word).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String meaning = snapshot.getValue(String.class);
                if (callback != null) callback.onWordLoaded(word, meaning);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseManager", "Load word cancelled: " + error.getMessage());
                if (callback != null) callback.onWordLoaded(null, null);
            }
        });
    }
    
    public interface WordLoadCallback {
        void onWordLoaded(String word, String meaning);
    }
    
    /**
     * الاستماع للكلمات الجديدة
     */
    public void listenToNewWords(final NewWordListener listener) {
        if (!isInitialized || lexiconRef == null) return;
        
        lexiconRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String word = snapshot.getKey();
                String meaning = snapshot.getValue(String.class);
                if (listener != null) listener.onNewWord(word, meaning);
            }
            
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}
            
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseManager", "Listen cancelled: " + error.getMessage());
            }
        });
    }
    
    public interface NewWordListener {
        void onNewWord(String word, String meaning);
    }
    
    /**
     * حفظ محادثة
     */
    public void saveConversation(String userMessage, String aiResponse, long timestamp) {
        if (!isInitialized || conversationsRef == null) return;
        
        Map<String, Object> conversation = new HashMap<>();
        conversation.put("user", userMessage);
        conversation.put("ai", aiResponse);
        conversation.put("timestamp", timestamp);
        
        conversationsRef.push().setValue(conversation);
    }
    
    /**
     * حفظ حالة الوعي
     */
    public void saveState(NeuralSeed.InternalState state) {
        if (!isInitialized || stateRef == null || state == null) return;
        
        Map<String, Object> stateData = new HashMap<>();
        stateData.put("phase", state.currentPhase.name());
        stateData.put("chaosIndex", state.chaosIndex);
        stateData.put("existentialFitness", state.existentialFitness);
        stateData.put("internalConflict", state.internalConflict);
        stateData.put("timestamp", System.currentTimeMillis());
        
        if (state.dominantEgo != null) {
            stateData.put("dominantEgo", state.dominantEgo.name);
        }
        
        stateRef.setValue(stateData);
    }
    
    /**
     * مزامنة المعجم المحلي مع السحابة
     */
    public void syncLexicon(ArabicLexicon lexicon) {
        if (!isInitialized || lexiconRef == null) return;
        
        // رفع الكلمات المحلية
        for (ArabicLexicon.Word word : lexicon.getAllWords()) {
            if (!word.meanings.isEmpty()) {
                saveWord(word.word, word.meanings.get(0));
            }
        }
        
        // استرجاع الكلمات الجديدة من السحابة
        lexiconRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String word = child.getKey();
                    String meaning = child.getValue(String.class);
                    
                    if (!lexicon.contains(word)) {
                        lexicon.addWord(word, meaning);
                        if (listener != null) {
                            listener.onWordSynced(word, meaning);
                        }
                    }
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseManager", "Sync failed: " + error.getMessage());
                if (listener != null) {
                    listener.onSyncError(error.getMessage());
                }
            }
        });
    }
    
    /**
     * الحصول على معرف الجهاز
     */
    private String getDeviceId(Context context) {
        String id = android.provider.Settings.Secure.getString(
            context.getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        );
        return id != null ? id : "unknown_device";
    }
    
    /**
     * التحقق من حالة الاتصال
     */
    public boolean isConnected() {
        return isInitialized;
    }
    
    /**
     * تعيين المستمع
     */
    public void setListener(SyncListener listener) {
        this.listener = listener;
    }
}

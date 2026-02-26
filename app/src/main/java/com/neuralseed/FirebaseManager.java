package com.neuralseed;

import android.content.Context;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;
import com.google.firebase.auth.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * مدير Firebase - للتخزين السحابي والمزامنة
 */
public class FirebaseManager {
    
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    
    // مستمعي الأحداث
    public interface SyncListener {
        void onSyncComplete(boolean success);
        void onDataReceived(String collection, Map<String, Object> data);
        void onError(String error);
    }
    
    private SyncListener syncListener;
    private String userId;
    private boolean isAuthenticated;
    
    public FirebaseManager(Context context) {
        try {
            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
            auth = FirebaseAuth.getInstance();
            isAuthenticated = false;
        } catch (Exception e) {
            // Firebase غير مهيأ
        }
    }
    
    public void setSyncListener(SyncListener listener) {
        this.syncListener = listener;
    }
    
    // ===== المصادقة =====
    
    public void signInAnonymously() {
        if (auth == null) return;
        
        auth.signInAnonymously()
            .addOnSuccessListener(result -> {
                userId = result.getUser().getUid();
                isAuthenticated = true;
                if (syncListener != null) {
                    syncListener.onSyncComplete(true);
                }
            })
            .addOnFailureListener(e -> {
                isAuthenticated = false;
                if (syncListener != null) {
                    syncListener.onError(e.getMessage());
                }
            });
    }
    
    public void signOut() {
        if (auth != null) {
            auth.signOut();
            isAuthenticated = false;
            userId = null;
        }
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated && userId != null;
    }
    
    // ===== حفظ البيانات =====
    
    public void saveWord(ArabicLexicon.Word word) {
        if (!isAuthenticated() || db == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("word", word.word);
        data.put("root", word.root);
        data.put("type", word.type.name());
        data.put("meanings", word.meanings);
        data.put("emotions", word.emotions);
        data.put("familiarity", word.familiarity);
        data.put("usageCount", word.usageCount);
        data.put("lastUsed", word.lastUsed);
        data.put("timestamp", System.currentTimeMillis());
        
        db.collection("users").document(userId)
          .collection("words").document(word.word)
          .set(data)
          .addOnFailureListener(e -> {
              if (syncListener != null) syncListener.onError(e.getMessage());
          });
    }
    
    public void saveMeaning(SemanticEmotionalEngine.Meaning meaning) {
        if (!isAuthenticated() || db == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("concept", meaning.concept);
        data.put("definition", meaning.definition);
        data.put("synonyms", meaning.synonyms);
        data.put("antonyms", meaning.antonyms);
        data.put("relatedConcepts", meaning.relatedConcepts);
        data.put("timestamp", System.currentTimeMillis());
        
        db.collection("users").document(userId)
          .collection("meanings").document(meaning.concept)
          .set(data)
          .addOnFailureListener(e -> {
              if (syncListener != null) syncListener.onError(e.getMessage());
          });
    }
    
    public void saveConversation(String userMessage, String aiResponse, 
                                  Map<String, Double> emotions) {
        if (!isAuthenticated() || db == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("userMessage", userMessage);
        data.put("aiResponse", aiResponse);
        data.put("emotions", emotions);
        data.put("timestamp", System.currentTimeMillis());
        
        db.collection("users").document(userId)
          .collection("conversations")
          .add(data)
          .addOnFailureListener(e -> {
              if (syncListener != null) syncListener.onError(e.getMessage());
          });
    }
    
    public void saveExperience(String description, Map<String, Double> emotions,
                                String outcome, String lesson, double importance) {
        if (!isAuthenticated() || db == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("description", description);
        data.put("emotions", emotions);
        data.put("outcome", outcome);
        data.put("lesson", lesson);
        data.put("importance", importance);
        data.put("timestamp", System.currentTimeMillis());
        
        db.collection("users").document(userId)
          .collection("experiences")
          .add(data)
          .addOnFailureListener(e -> {
              if (syncListener != null) syncListener.onError(e.getMessage());
          });
    }
    
    public void saveCorrection(String original, String corrected, String explanation) {
        if (!isAuthenticated() || db == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("original", original);
        data.put("corrected", corrected);
        data.put("explanation", explanation);
        data.put("learned", true);
        data.put("timestamp", System.currentTimeMillis());
        
        db.collection("users").document(userId)
          .collection("corrections")
          .add(data)
          .addOnFailureListener(e -> {
              if (syncListener != null) syncListener.onError(e.getMessage());
          });
    }
    
    public void saveState(NeuralSeed.InternalState state) {
        if (!isAuthenticated() || db == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("chaosIndex", state.chaosIndex);
        data.put("existentialFitness", state.existentialFitness);
        data.put("internalConflict", state.internalConflict);
        data.put("currentPhase", state.currentPhase != null ? state.currentPhase.name() : "EMBRYONIC");
        data.put("dominantEgo", state.dominantEgo != null ? state.dominantEgo.name : "unknown");
        data.put("timestamp", System.currentTimeMillis());
        
        db.collection("users").document(userId)
          .collection("states")
          .add(data)
          .addOnFailureListener(e -> {
              if (syncListener != null) syncListener.onError(e.getMessage());
          });
    }
    
    // ===== استرجاع البيانات =====
    
    public void loadWords(OnWordsLoadedListener listener) {
        if (!isAuthenticated() || db == null) {
            if (listener != null) listener.onLoaded(new ArrayList<>());
            return;
        }
        
        db.collection("users").document(userId)
          .collection("words")
          .orderBy("timestamp", Query.Direction.DESCENDING)
          .limit(100)
          .get()
          .addOnSuccessListener(querySnapshot -> {
              List<Map<String, Object>> words = new ArrayList<>();
              for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                  words.add(doc.getData());
              }
              if (listener != null) listener.onLoaded(words);
          })
          .addOnFailureListener(e -> {
              if (syncListener != null) syncListener.onError(e.getMessage());
              if (listener != null) listener.onLoaded(new ArrayList<>());
          });
    }
    
    public interface OnWordsLoadedListener {
        void onLoaded(List<Map<String, Object>> words);
    }
    
    public void loadConversations(int limit, OnConversationsLoadedListener listener) {
        if (!isAuthenticated() || db == null) {
            if (listener != null) listener.onLoaded(new ArrayList<>());
            return;
        }
        
        db.collection("users").document(userId)
          .collection("conversations")
          .orderBy("timestamp", Query.Direction.DESCENDING)
          .limit(limit)
          .get()
          .addOnSuccessListener(querySnapshot -> {
              List<Map<String, Object>> conversations = new ArrayList<>();
              for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                  conversations.add(doc.getData());
              }
              if (listener != null) listener.onLoaded(conversations);
          })
          .addOnFailureListener(e -> {
              if (syncListener != null) syncListener.onError(e.getMessage());
              if (listener != null) listener.onLoaded(new ArrayList<>());
          });
    }
    
    public interface OnConversationsLoadedListener {
        void onLoaded(List<Map<String, Object>> conversations);
    }
    
    // ===== المزامنة =====
    
    public void syncWithLocal(LocalDatabase localDb) {
        if (!isAuthenticated() || db == null || localDb == null) return;
        
        // مزامنة الكلمات
        loadWords(words -> {
            for (Map<String, Object> wordData : words) {
                String word = (String) wordData.get("word");
                if (word != null && localDb.loadWord(word) == null) {
                    // إضافة الكلمة المحلية
                    ArabicLexicon.Word w = new ArabicLexicon.Word(
                        (String) wordData.get("root"),
                        word,
                        ArabicLexicon.WordType.valueOf((String) wordData.get("type"))
                    );
                    
                    List<String> meanings = (List<String>) wordData.get("meanings");
                    if (meanings != null) w.meanings.addAll(meanings);
                    
                    Map<String, Double> emotions = (Map<String, Double>) wordData.get("emotions");
                    if (emotions != null) w.emotions.putAll(emotions);
                    
                    w.familiarity = ((Number) wordData.get("familiarity")).doubleValue();
                    w.usageCount = ((Number) wordData.get("usageCount")).intValue();
                    
                    localDb.saveWord(w);
                }
            }
            
            if (syncListener != null) {
                syncListener.onSyncComplete(true);
            }
        });
    }
    
    // ===== الاستماع للتغييرات =====
    
    public void listenToConversations() {
        if (!isAuthenticated() || db == null) return;
        
        db.collection("users").document(userId)
          .collection("conversations")
          .orderBy("timestamp", Query.Direction.DESCENDING)
          .limit(1)
          .addSnapshotListener((snapshot, error) -> {
              if (error != null) {
                  if (syncListener != null) syncListener.onError(error.getMessage());
                  return;
              }
              
              if (snapshot != null && !snapshot.isEmpty()) {
                  for (DocumentChange change : snapshot.getDocumentChanges()) {
                      if (change.getType() == DocumentChange.Type.ADDED) {
                          if (syncListener != null) {
                              syncListener.onDataReceived("conversations", 
                                  change.getDocument().getData());
                          }
                      }
                  }
              }
          });
    }
    
    // ===== التخزين السحابي =====
    
    public void uploadBackup(String data, OnUploadCompleteListener listener) {
        if (storage == null) {
            if (listener != null) listener.onComplete(false, null);
            return;
        }
        
        String filename = "backup_" + System.currentTimeMillis() + ".json";
        StorageReference ref = storage.getReference().child("backups").child(userId).child(filename);
        
        ref.putBytes(data.getBytes())
            .addOnSuccessListener(taskSnapshot -> {
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (listener != null) listener.onComplete(true, uri.toString());
                });
            })
            .addOnFailureListener(e -> {
                if (listener != null) listener.onComplete(false, null);
            });
    }
    
    public interface OnUploadCompleteListener {
        void onComplete(boolean success, String downloadUrl);
    }
    
    // ===== الإحصائيات =====
    
    public void getStatistics(OnStatisticsLoadedListener listener) {
        if (!isAuthenticated() || db == null) {
            if (listener != null) listener.onLoaded(new HashMap<>());
            return;
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // عدد الكلمات
        db.collection("users").document(userId)
          .collection("words")
          .get()
          .addOnSuccessListener(snapshot -> {
              stats.put("wordCount", snapshot.size());
              
              // عدد المحادثات
              db.collection("users").document(userId)
                .collection("conversations")
                .get()
                .addOnSuccessListener(convSnapshot -> {
                    stats.put("conversationCount", convSnapshot.size());
                    
                    // عدد التجارب
                    db.collection("users").document(userId)
                      .collection("experiences")
                      .get()
                      .addOnSuccessListener(expSnapshot -> {
                          stats.put("experienceCount", expSnapshot.size());
                          
                          if (listener != null) listener.onLoaded(stats);
                      });
                });
          });
    }
    
    public interface OnStatisticsLoadedListener {
        void onLoaded(Map<String, Object> statistics);
    }
    
    // ===== حذف البيانات =====
    
    public void deleteAllData(OnDeleteCompleteListener listener) {
        if (!isAuthenticated() || db == null) {
            if (listener != null) listener.onComplete(false);
            return;
        }
        
        // حذف جميع المجموعات الفرعية
        deleteCollection(db.collection("users").document(userId).collection("words"));
        deleteCollection(db.collection("users").document(userId).collection("meanings"));
        deleteCollection(db.collection("users").document(userId).collection("conversations"));
        deleteCollection(db.collection("users").document(userId).collection("experiences"));
        deleteCollection(db.collection("users").document(userId).collection("corrections"));
        deleteCollection(db.collection("users").document(userId).collection("states"));
        
        if (listener != null) listener.onComplete(true);
    }
    
    private void deleteCollection(CollectionReference collection) {
        collection.get().addOnSuccessListener(snapshot -> {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                doc.getReference().delete();
            }
        });
    }
    
    public interface OnDeleteCompleteListener {
        void onComplete(boolean success);
    }
}

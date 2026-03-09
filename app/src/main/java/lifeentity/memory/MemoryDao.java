package com.lifeentity.memory;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * واجهة الوصول للذاكرة مع فهارس متقدمة واستعلامات محسّنة
 * تدعم البحث المتجهي المستقبلي (FAISS) والاستعلامات العاطفية المعقدة
 */
@Dao
public interface MemoryDao {

    // ========================== الأحداث (EpisodicMemory) ==========================
    
    @Insert
    void insertEvent(EpisodicMemory.EventEntity event);

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getRecentEvents(int limit);

    default List<EpisodicMemory.EventEntity> getRecentEvents() {
        return getRecentEvents(100);
    }

    @Query("SELECT * FROM events WHERE emotionalState = :emotion ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getEventsByEmotion(String emotion, int limit);

    default List<EpisodicMemory.EventEntity> getEventsByEmotion(String emotion) {
        return getEventsByEmotion(emotion, 50);
    }

    @Query("SELECT * FROM events WHERE emotionalState IN (:emotions) ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getEventsByEmotions(List<String> emotions, int limit);

    @Query("SELECT * FROM events WHERE importance > :minImportance ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getImportantEvents(float minImportance, int limit);

    default List<EpisodicMemory.EventEntity> getImportantEvents(float minImportance) {
        return getImportantEvents(minImportance, 50);
    }

    @Query("SELECT * FROM events WHERE emotionalIntensity > :threshold ORDER BY emotionalIntensity DESC, timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getEmotionalEvents(float threshold, int limit);

    default List<EpisodicMemory.EventEntity> getEmotionalEvents(float threshold) {
        return getEmotionalEvents(threshold, 100);
    }

    @Query("SELECT * FROM events WHERE visualMemoryId IS NOT NULL ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getEventsWithImages(int limit);

    default List<EpisodicMemory.EventEntity> getEventsWithImages() {
        return getEventsWithImages(50);
    }

    @Query("SELECT * FROM events WHERE faceId = :faceId ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getEventsByFaceId(String faceId, int limit);

    default List<EpisodicMemory.EventEntity> getEventsByFaceId(String faceId) {
        return getEventsByFaceId(faceId, 20);
    }

    @Query("SELECT * FROM events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<EpisodicMemory.EventEntity> getEventsInTimeRange(long startTime, long endTime);

    @Query("SELECT * FROM events WHERE narrative LIKE '%' || :keyword || '%' ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> searchEventsByKeyword(String keyword, int limit);

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT 1")
    EpisodicMemory.EventEntity getLatestEvent();

    @Query("SELECT COUNT(*) FROM events")
    int getEventCount();

    @Query("DELETE FROM events WHERE timestamp < :olderThan")
    void deleteOldEvents(long olderThan);

    // ========================== الهويات (IdentityMemory) ==========================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveIdentity(IdentityMemory identity);

    @Query("SELECT * FROM identities WHERE faceHash = :hash LIMIT 1")
    IdentityMemory getIdentity(String hash);

    @Query("SELECT * FROM identities ORDER BY familiarity DESC, lastSeen DESC LIMIT :limit")
    List<IdentityMemory> getAllIdentities(int limit);

    default List<IdentityMemory> getAllIdentities() {
        return getAllIdentities(1000);
    }

    @Query("SELECT * FROM identities WHERE familiarity > :minFamiliarity ORDER BY familiarity DESC")
    List<IdentityMemory> getFamiliarIdentities(float minFamiliarity);

    @Query("SELECT faceHash FROM identities WHERE familiarity > :minFamiliarity")
    List<String> getKnownFaceHashes(float minFamiliarity);

    @Query("UPDATE identities SET familiarity = familiarity + :increment, lastSeen = :currentTime WHERE faceHash = :hash")
    void incrementFamiliarity(String hash, float increment, long currentTime);

    default void incrementFamiliarity(String hash, float increment) {
        incrementFamiliarity(hash, increment, System.currentTimeMillis());
    }

    @Query("UPDATE identities SET emotionalAssociation = :emotion WHERE faceHash = :hash")
    void updateIdentityEmotion(String hash, String emotion);

    @Query("DELETE FROM identities WHERE faceHash = :hash")
    void deleteIdentity(String hash);

    @Query("SELECT * FROM identities WHERE lastSeen > :since ORDER BY lastSeen DESC")
    List<IdentityMemory> getRecentlySeenIdentities(long since);

    // ========================== الذاكرة البصرية (VisualMemory) ==========================
    
    @Insert
    long insertVisualMemory(VisualMemory visualMemory);

    @Query("SELECT * FROM visual_memories ORDER BY timestamp DESC LIMIT :limit")
    List<VisualMemory> getRecentVisualMemories(int limit);

    @Query("SELECT * FROM visual_memories WHERE concept = :concept ORDER BY timestamp DESC LIMIT :limit")
    List<VisualMemory> getVisualMemoriesByConcept(String concept, int limit);

    @Query("SELECT * FROM visual_memories WHERE affectValence BETWEEN :minValence AND :maxValence " +
           "AND affectArousal BETWEEN :minArousal AND :maxArousal " +
           "ORDER BY timestamp DESC LIMIT :limit")
    List<VisualMemory> getVisualMemoriesByAffectRange(
            float minValence, float maxValence, 
            float minArousal, float maxArousal, 
            int limit);

    default List<VisualMemory> getVisualMemoriesByEmotion(String emotionType, int limit) {
        switch (emotionType.toLowerCase()) {
            case "joy":
                return getVisualMemoriesByAffectRange(0.5f, 1.0f, 0.3f, 1.0f, limit);
            case "sadness":
                return getVisualMemoriesByAffectRange(-1.0f, -0.3f, -0.5f, 0.3f, limit);
            case "fear":
                return getVisualMemoriesByAffectRange(-0.5f, 0.0f, 0.5f, 1.0f, limit);
            case "calm":
                return getVisualMemoriesByAffectRange(-0.3f, 0.3f, -0.5f, 0.0f, limit);
            default:
                return getRecentVisualMemories(limit);
        }
    }

    @Query("SELECT * FROM visual_memories WHERE id IN (:ids)")
    List<VisualMemory> getVisualMemoriesByIds(List<Long> ids);

    @Query("SELECT id, latentVector FROM visual_memories WHERE latentVector IS NOT NULL")
    List<VisualMemory.VectorOnly> getAllLatentVectors();

    @Query("SELECT * FROM visual_memories WHERE retrievalCount > :minCount ORDER BY retrievalCount DESC LIMIT :limit")
    List<VisualMemory> getImpactfulMemories(int minCount, int limit);

    default List<VisualMemory> getImpactfulMemories(float minIntensity) {
        return getImpactfulMemories((int)(minIntensity * 10), 50);
    }

    @Query("UPDATE visual_memories SET retrievalCount = retrievalCount + 1 WHERE id = :id")
    void incrementRetrievalCount(long id);

    @Query("SELECT concept FROM visual_memories GROUP BY concept ORDER BY COUNT(*) DESC LIMIT :limit")
    List<String> getFrequentConcepts(int limit);

    default String getRandomConcept() {
        List<String> concepts = getFrequentConcepts(100);
        if (concepts.isEmpty()) return "شيء";
        return concepts.get((int) (Math.random() * concepts.size()));
    }

    // ========================== التضمينات الدلالية (SemanticEmbeddings) ==========================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveEmbedding(SemanticEmbeddings.EmbeddingEntity embedding);

    @Query("SELECT * FROM embeddings WHERE concept = :concept LIMIT 1")
    SemanticEmbeddings.EmbeddingEntity getEmbedding(String concept);

    @Query("SELECT * FROM embeddings ORDER BY learnedAt DESC LIMIT :limit")
    List<SemanticEmbeddings.EmbeddingEntity> getRecentEmbeddings(int limit);

    default List<SemanticEmbeddings.EmbeddingEntity> getRecentEmbeddings() {
        return getRecentEmbeddings(100);
    }

    @Query("SELECT concept FROM embeddings")
    List<String> getAllConcepts();

    @Query("SELECT concept FROM embeddings WHERE concept LIKE '%' || :partial || '%' LIMIT :limit")
    List<String> searchConcepts(String partial, int limit);

    // ========================== استعلامات مركبة (Transactions) ==========================
    
    @Transaction
    @Query("SELECT * FROM events WHERE id = :eventId")
    EpisodicMemory.EventWithVisual getEventWithVisual(long eventId);

    @Transaction
    default void saveEventWithVisual(EpisodicMemory.EventEntity event, VisualMemory visual) {
        long visualId = insertVisualMemory(visual);
        event.visualMemoryId = visualId;
        insertEvent(event);
    }

    // ========================== تنظيف وصيانة ==========================
    
    // ملاحظة: VACUUM و ANALYZE لا تعمل في Room على Android
    // يمكن تنفيذها عبر Migration أو RawQuery إذا لزم الأمر
}

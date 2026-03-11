package com.lifeentity.memory;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

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

    @Query("SELECT * FROM events WHERE emotionalIntensity > :threshold ORDER BY emotionalIntensity DESC, timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getEmotionalEvents(float threshold, int limit);

    default List<EpisodicMemory.EventEntity> getEmotionalEvents(float threshold) {
        return getEmotionalEvents(threshold, 100);
    }

    @Query("SELECT * FROM events WHERE importance > :minImportance ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> getImportantEvents(float minImportance, int limit);

    @Query("SELECT * FROM events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<EpisodicMemory.EventEntity> getEventsInTimeRange(long startTime, long endTime);

    @Query("SELECT * FROM events WHERE narrative LIKE '%' || :keyword || '%' ORDER BY timestamp DESC LIMIT :limit")
    List<EpisodicMemory.EventEntity> searchEventsByKeyword(String keyword, int limit);

    @Query("SELECT COUNT(*) FROM events")
    int getEventCount();

    // ========================== الذاكرة البصرية (VisualMemory) ==========================
    
    @Insert
    long insertVisualMemory(VisualMemory visualMemory);

    @Query("SELECT * FROM visual_memories ORDER BY timestamp DESC LIMIT :limit")
    List<VisualMemory> getRecentVisualMemories(int limit);

    default List<VisualMemory> getRecentVisualMemories() {
        return getRecentVisualMemories(50);
    }

    @Query("SELECT * FROM visual_memories WHERE concept = :concept ORDER BY timestamp DESC LIMIT :limit")
    List<VisualMemory> getVisualMemoriesByConcept(String concept, int limit);

    @Query("SELECT * FROM visual_memories WHERE retrievalCount > :minCount ORDER BY retrievalCount DESC LIMIT :limit")
    List<VisualMemory> getImpactfulMemories(int minCount, int limit);

    @Query("UPDATE visual_memories SET retrievalCount = retrievalCount + 1 WHERE id = :id")
    void incrementRetrievalCount(long id);

    @Query("SELECT * FROM visual_memories WHERE id IN (:ids)")
    List<VisualMemory> getVisualMemoriesByIds(List<Long> ids);

    @Query("SELECT concept FROM visual_memories GROUP BY concept ORDER BY COUNT(*) DESC LIMIT :limit")
    List<String> getFrequentConcepts(int limit);

    default String getRandomConcept() {
        List<String> concepts = getFrequentConcepts(100);
        if (concepts.isEmpty()) return "شيء";
        return concepts.get((int) (Math.random() * concepts.size()));
    }

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

    // ========================== التضمينات الدلالية ==========================
    
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

    // ========================== استعلامات مركبة ==========================
    
    @Transaction
    @Query("SELECT * FROM events WHERE id = :eventId")
    EpisodicMemory.EventWithVisual getEventWithVisual(long eventId);
}

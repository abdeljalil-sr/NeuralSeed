package com.lifeentity.memory;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * واجهة الوصول إلى قاعدة البيانات للذاكرة العرضية (episodic memory)
 * والهويات (identities) والتضمينات الدلالية (semantic embeddings).
 */
@Dao
public interface MemoryDao {

    // ========================== الأحداث (EpisodicMemory) ==========================

    @Insert
    void insertEvent(EpisodicMemory.EventEntity event);

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT 100")
    List<EpisodicMemory.EventEntity> getRecentEvents();

    @Query("SELECT * FROM events WHERE emotionalState = :emotion ORDER BY timestamp DESC LIMIT 50")
    List<EpisodicMemory.EventEntity> getEventsByEmotion(String emotion);

    @Query("SELECT * FROM events WHERE importance > :minImportance ORDER BY timestamp DESC LIMIT 50")
    List<EpisodicMemory.EventEntity> getImportantEvents(float minImportance);

    @Query("SELECT * FROM events WHERE visualMemoryId IS NOT NULL ORDER BY timestamp DESC LIMIT 50")
    List<EpisodicMemory.EventEntity> getEventsWithImages();

    @Query("SELECT * FROM events WHERE faceId = :faceId ORDER BY timestamp DESC LIMIT 20")
    List<EpisodicMemory.EventEntity> getEventsByFaceId(String faceId);

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT 1")
    EpisodicMemory.EventEntity getLatestEvent();

    // ========================== الهويات (IdentityMemory) ==========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveIdentity(IdentityMemory identity);

    @Query("SELECT * FROM identities WHERE faceHash = :hash")
    IdentityMemory getIdentity(String hash);

    @Query("SELECT * FROM identities")
    List<IdentityMemory> getAllIdentities();

    @Query("SELECT faceHash FROM identities WHERE familiarity > :minFamiliarity")
    List<String> getKnownFaceHashes(float minFamiliarity);

    @Query("UPDATE identities SET familiarity = familiarity + :increment WHERE faceHash = :hash")
    void incrementFamiliarity(String hash, float increment);

    @Query("DELETE FROM identities WHERE faceHash = :hash")
    void deleteIdentity(String hash);

    // ========================== التضمينات الدلالية (SemanticEmbeddings) ==========================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveEmbedding(SemanticEmbeddings.Entity embedding);

    @Query("SELECT * FROM embeddings WHERE concept = :concept")
    SemanticEmbeddings.Entity getEmbedding(String concept);

    @Query("SELECT * FROM embeddings ORDER BY learnedAt DESC LIMIT 100")
    List<SemanticEmbeddings.Entity> getRecentEmbeddings();

    @Query("SELECT concept FROM embeddings")
    List<String> getAllConcepts();
}

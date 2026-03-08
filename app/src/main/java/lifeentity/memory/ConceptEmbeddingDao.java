package com.lifeentity.memory;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConceptEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConceptEmbedding embedding);

    @Query("SELECT * FROM concept_embeddings WHERE concept = :concept")
    ConceptEmbedding get(String concept);

    @Query("SELECT * FROM concept_embeddings ORDER BY learnedAt DESC LIMIT 100")
    List<ConceptEmbedding> getRecent();

    @Query("SELECT concept FROM concept_embeddings")
    List<String> getAllConcepts();

    @Query("SELECT vector FROM concept_embeddings WHERE concept = :concept")
    float[] getVector(String concept);
}

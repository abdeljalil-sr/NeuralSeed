package com.lifeentity.memory;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MemoryDao {
    
    @Insert
    void insertEvent(EpisodicMemory.EventEntity event);
    
    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT 100")
    List<EpisodicMemory.EventEntity> getRecentEvents();
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveIdentity(IdentityMemory identity);
    
    @Query("SELECT * FROM identities WHERE faceHash = :hash")
    IdentityMemory getIdentity(String hash);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveEmbedding(SemanticEmbeddings.Entity embedding);
}

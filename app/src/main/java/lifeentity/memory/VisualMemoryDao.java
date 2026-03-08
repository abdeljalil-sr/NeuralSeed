package com.lifeentity.memory;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VisualMemoryDao {
    @Insert
    void insert(VisualMemory memory);

    @Query("SELECT * FROM visual_memories ORDER BY timestamp DESC LIMIT :limit")
    List<VisualMemory> getRecent(int limit);

    @Query("SELECT * FROM visual_memories WHERE concept LIKE :concept ORDER BY timestamp DESC LIMIT 50")
    List<VisualMemory> getByConcept(String concept);

    @Query("SELECT * FROM visual_memories ORDER BY RANDOM() LIMIT 1")
    VisualMemory getRandom();

    @Query("SELECT * FROM visual_memories ORDER BY timestamp DESC LIMIT 100")
    List<VisualMemory> getAllRecent();

    @Query("SELECT concept FROM visual_memories WHERE concept IS NOT NULL GROUP BY concept ORDER BY RANDOM() LIMIT 1")
    String getRandomConcept();

    @Query("SELECT COUNT(*) FROM visual_memories")
    int getCount();
}

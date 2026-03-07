package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

public class SemanticEmbeddings {
    
    @Entity(tableName = "embeddings")
    public static class Entity {
        @PrimaryKey
        public String concept;
        public float[] vector;
        public long learnedAt;
    }
}

package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

public class SemanticEmbeddings {
    
    @androidx.room.Entity(tableName = "embeddings")
    public static class Entity {
        @androidx.room.PrimaryKey
        public String concept;
        public float[] vector;
        public long learnedAt;
    }
}



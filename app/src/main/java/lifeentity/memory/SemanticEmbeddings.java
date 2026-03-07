package com.lifeentity.memory;

// ❌ احذف: import androidx.room.Entity;
// ❌ احذف: import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

public class SemanticEmbeddings {

    @androidx.room.Entity(tableName = "embeddings")
    public static class Entity {
        @androidx.room.PrimaryKey
        @NonNull
        public String concept;
        public float[] vector;
        public long learnedAt;
    }
}

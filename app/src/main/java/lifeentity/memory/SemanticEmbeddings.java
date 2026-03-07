package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
public class SemanticEmbeddings {

    @Entity(tableName = "embeddings")
        @androidx.room.Entity(tableName = "embeddings")
    public static class Entity {
        @androidx.room.PrimaryKey
        @NonNull  // ← إضافة واحدة فقط
        public String concept;
        public float[] vector;  // ← بدون تغيير، سيُحول عبر Converters
        public long learnedAt;
    }
}

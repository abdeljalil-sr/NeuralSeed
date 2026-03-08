package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

public class SemanticEmbeddings {

    @Entity(tableName = "embeddings")
    @TypeConverters(Converters.class)
    public static class EmbeddingEntity {
        @PrimaryKey
        @NonNull
        public String concept;
        public float[] vector;
        public long learnedAt;

        // منشئ فارغ (مطلوب لـ Room)
        public EmbeddingEntity() {}

        // منشئ مع parameters (سيتم تجاهله بواسطة Room)
        @Ignore
        public EmbeddingEntity(@NonNull String concept, float[] vector) {
            this.concept = concept;
            this.vector = vector;
            this.learnedAt = System.currentTimeMillis();
        }
    }
}

// ====================== memory/SemanticEmbeddings.java (مع التأكيد على EmbeddingEntity) ======================
package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

public class SemanticEmbeddings {

    @Entity(tableName = "embeddings")
    @TypeConverters(Converters.class)
    public static class EmbeddingEntity {  // ✅ الاسم الجديد
        @PrimaryKey
        @NonNull
        public String concept;
        public float[] vector;
        public long learnedAt;

        public EmbeddingEntity() {}
        public EmbeddingEntity(@NonNull String concept, float[] vector) {
            this.concept = concept;
            this.vector = vector;
            this.learnedAt = System.currentTimeMillis();
        }
    }
}

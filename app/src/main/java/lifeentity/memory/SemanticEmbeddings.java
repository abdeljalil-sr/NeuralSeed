// ====================== memory/SemanticEmbeddings.java (تم إصلاح مشكلة @Entity) ======================
package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

public class SemanticEmbeddings {

    @Entity(tableName = "embeddings")
    @TypeConverters(Converters.class)
    public static class EmbeddingEntity {  // ✅ تغيير الاسم من Entity إلى EmbeddingEntity لتجنب التعارض مع androidx.room.Entity
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

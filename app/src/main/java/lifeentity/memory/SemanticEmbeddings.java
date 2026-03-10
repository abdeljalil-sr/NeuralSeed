package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * تخزين التضمينات الدلالية للمفاهيم والكلمات
 * يستخدم لربط المفاهيم اللغوية بالتمثيلات البصرية والعاطفية
 */
public class SemanticEmbeddings {

    /**
     * كيان قاعدة البيانات لتخزين التضمينات
     */
    @Entity(tableName = "embeddings")
    @TypeConverters(Converters.class)
    public static class EmbeddingEntity {
        @PrimaryKey
        @NonNull
        public String concept;           // المفهوم أو الكلمة
        public float[] vector;            // المتجه الدلالي (128 بعداً)
        public long learnedAt;            // وقت التعلم
        public String source;             // مصدر التضمين (image, text, sync)

        // منشئ فارغ (مطلوب لـ Room)
        public EmbeddingEntity() {}

        // منشئ مع parameters (سيتم تجاهله بواسطة Room)
        @Ignore
        public EmbeddingEntity(@NonNull String concept, float[] vector) {
            this.concept = concept;
            this.vector = vector;
            this.learnedAt = System.currentTimeMillis();
            this.source = "local";
        }

        @Ignore
        public EmbeddingEntity(@NonNull String concept, float[] vector, String source) {
            this.concept = concept;
            this.vector = vector;
            this.learnedAt = System.currentTimeMillis();
            this.source = source;
        }
    }
}

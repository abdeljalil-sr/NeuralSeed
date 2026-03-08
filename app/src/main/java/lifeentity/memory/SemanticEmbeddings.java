package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * التضمينات الدلالية (Semantic Embeddings) - تخزين متجهات المعاني للمفاهيم.
 * يستخدم لربط الكلمات بالمفاهيم البصرية وتغذية ImaginationEngine.
 */
public class SemanticEmbeddings {

    /**
     * كيان قاعدة البيانات لتضمين مفهوم معين.
     */
    @Entity(tableName = "embeddings")
    @TypeConverters(Converters.class)  // لتحويل float[] إلى String
    public static class Entity {
        @PrimaryKey
        @NonNull
        public String concept;         // المفهوم (كلمة أو عبارة)

        public float[] vector;          // المتجه الدلالي (عادة 128-300 بعد)

        public long learnedAt;          // وقت التعلم (timestamp)

        // منشئ فارغ (يحتاجه Room)
        public Entity() {}

        // منشئ مناسب
        public Entity(@NonNull String concept, float[] vector) {
            this.concept = concept;
            this.vector = vector;
            this.learnedAt = System.currentTimeMillis();
        }
    }
}

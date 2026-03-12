package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "concept_embeddings")
@TypeConverters(Converters.class)
public class ConceptEmbedding {
    @PrimaryKey
    @NonNull
    public String concept;          // المفهوم (كلمة) - يجب أن يكون غير فارغ
    public float[] vector;           // المتجه الدلالي (128 بعداً)
    public long learnedAt;           // وقت التعلم
    public String source;            // مصدر التضمين (image, text, sync)

    // Constructor فارغ (مطلوب لـ Room)
    public ConceptEmbedding() {}

    // منشئ مع concept فقط (سيتم تجاهله بواسطة Room)
    @Ignore
    public ConceptEmbedding(@NonNull String concept, float[] vector) {
        this.concept = concept;
        this.vector = vector;
        this.learnedAt = System.currentTimeMillis();
        this.source = "local";
    }

    @Ignore
    public ConceptEmbedding(@NonNull String concept, float[] vector, String source) {
        this.concept = concept;
        this.vector = vector;
        this.learnedAt = System.currentTimeMillis();
        this.source = source;
    }
}

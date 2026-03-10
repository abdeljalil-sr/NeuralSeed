package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * تخزين تضمينات المفاهيم (Concept Embeddings) - كل مفهوم له متجه يمثل معناه.
 * يمكن استخدامه للربط بين الكلمات والصور.
 * 
 * ملاحظة: يوصى باستخدام SemanticEmbeddings.EmbeddingEntity بدلاً من هذا الكيان
 * لتوحيد تخزين التضمينات. هذا الملف محفوظ للتوافق مع الإصدارات السابقة.
 */
@Entity(tableName = "concept_embeddings")
@TypeConverters(Converters.class)
public class ConceptEmbedding {
    @PrimaryKey
    @NonNull
    public String concept;          // المفهوم (كلمة)
    public float[] vector;           // المتجه الدلالي (128 بعداً)
    public long learnedAt;           // وقت التعلم
    public String source;            // مصدر التضمين (image, text, sync)

    public ConceptEmbedding() {}

    public ConceptEmbedding(@NonNull String concept, float[] vector) {
        this.concept = concept;
        this.vector = vector;
        this.learnedAt = System.currentTimeMillis();
        this.source = "local";
    }

    public ConceptEmbedding(@NonNull String concept, float[] vector, String source) {
        this.concept = concept;
        this.vector = vector;
        this.learnedAt = System.currentTimeMillis();
        this.source = source;
    }
}

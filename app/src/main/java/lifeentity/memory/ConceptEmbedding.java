package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * تخزين تضمينات المفاهيم (Concept Embeddings) - كل مفهوم له متجه يمثل معناه.
 * يمكن استخدامه للربط بين الكلمات والصور.
 */
@Entity(tableName = "concept_embeddings")
@TypeConverters(Converters.class)
public class ConceptEmbedding {
    @PrimaryKey
    public String concept;          // المفهوم (كلمة)
    public float[] vector;           // المتجه الدلالي (128 بعداً)
    public long learnedAt;           // وقت التعلم

    public ConceptEmbedding() {}

    public ConceptEmbedding(String concept, float[] vector) {
        this.concept = concept;
        this.vector = vector;
        this.learnedAt = System.currentTimeMillis();
    }
}

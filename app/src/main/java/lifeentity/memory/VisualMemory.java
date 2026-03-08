package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * الذاكرة البصرية - تخزن الصور (كمصفوفات بايت) والمتجهات الكامنة المرتبطة بها.
 * كل صورة هي ذكرى بصرية يمكن للكائن استدعاؤها لاحقاً لتوليد صور جديدة.
 */
@Entity(tableName = "visual_memories")
@TypeConverters(Converters.class)
public class VisualMemory {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;                // وقت الالتقاط
    public float[] latentVector;           // المتجه الكامن (128 بعداً)
    public byte[] thumbnail;                // الصورة المصغرة (PNG)
    public String concept;                  // المفهوم المرتبط (مثلاً "وجه", "سيارة")
    public float[] affectAtEncoding;        // الحالة العاطفية عند التخزين (5 أبعاد)

    // منشئ فارغ (يحتاجه Room)
    public VisualMemory() {}

    // منشئ مفيد
    public VisualMemory(float[] latentVector, byte[] thumbnail, String concept, float[] affect) {
        this.timestamp = System.currentTimeMillis();
        this.latentVector = latentVector;
        this.thumbnail = thumbnail;
        this.concept = concept;
        this.affectAtEncoding = affect;
    }
}

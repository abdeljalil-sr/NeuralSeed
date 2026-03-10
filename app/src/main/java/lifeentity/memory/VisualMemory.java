package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "visual_memories")
@TypeConverters(Converters.class)
public class VisualMemory {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;
    public float[] latentVector;
    public byte[] thumbnail;
    public String concept;
    public float[] affectAtEncoding;
    public int retrievalCount;  // ✅ تمت إضافة هذا الحقل (عدد مرات الاسترجاع)

    public VisualMemory() {}

    public VisualMemory(float[] latentVector, byte[] thumbnail, String concept, float[] affect) {
        this.timestamp = System.currentTimeMillis();
        this.latentVector = latentVector;
        this.thumbnail = thumbnail;
        this.concept = concept;
        this.affectAtEncoding = affect;
        this.retrievalCount = 0;  // ✅ القيمة الافتراضية 0
    }
}

package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * الذاكرة العرضية (Episodic Memory): تخزن الأحداث والتجارب المهمة التي مر بها الكائن.
 * كل حدث هو "لحظة واعية" تم حفظها للاستدعاء المستقبلي.
 */
public class EpisodicMemory {

    /**
     * كيان قاعدة البيانات لجدول الأحداث.
     */
    @Entity(tableName = "events")
    @TypeConverters(Converters.class)  // لتحويل float[] إلى String
    public static class EventEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;                 // معرف فريد

        public long timestamp;           // وقت الحدث (milliseconds)

        public String narrative;         // وصف نصي للحدث (من narrativeThread)

        public String emotionalState;    // اسم الحالة العاطفية (مثل "سعيد")

        public float[] affectVector;     // المتجه العاطفي (5 أبعاد) لتغذية ImaginationEngine

        public float emotionalIntensity; // شدة العاطفة (0..1)

        public String location;          // مكان الحدث (مثلاً "camera", "user_speech")

        // روابط لبيانات أخرى (اختياري)
        public Long visualMemoryId;      // معرف الصورة المخزنة في VisualMemory (إذا كان للحدث صورة)
        public String faceId;            // معرف الوجه المرتبط (إذا كان هناك وجه)

        public float importance;         // أهمية الحدث (تحسب لاحقاً، 0..1)

        // حقل sensoryHash يمكن إضافته إذا أردنا حفظ بصمة للمدخلات الحسية
        // public String sensoryHash;
    }

    /**
     * تمثيل مبسط للحدث (للاستخدام خارج قاعدة البيانات).
     */
    public static class Event {
        public long timestamp;
        public String sensoryHash;
        public String emotionalState;
        public double emotionalIntensity;
        public String narrative;
        public String location;
        public float[] affectVector;

        public Event() {}

        public Event(EventEntity entity) {
            this.timestamp = entity.timestamp;
            this.emotionalState = entity.emotionalState;
            this.emotionalIntensity = entity.emotionalIntensity;
            this.narrative = entity.narrative;
            this.location = entity.location;
            this.affectVector = entity.affectVector;
        }
    }
}

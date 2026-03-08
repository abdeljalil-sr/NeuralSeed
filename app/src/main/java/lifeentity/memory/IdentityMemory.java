package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

/**
 * ذاكرة الهويات - تخزن معلومات عن الوجوه التي يتعرف عليها الكائن.
 * يتم تخزين كل وجه ككيان منفصل مع بصمة الوجه (embedding) والعلاقة معه.
 */
@Entity(tableName = "identities")
@TypeConverters(Converters.class)  // لتحويل float[] إلى String
public class IdentityMemory {

    @PrimaryKey
    @NonNull
    public String faceHash;               // بصمة الوجه المشفرة (هاش فريد)

    public String name;                    // اسم الشخص (إذا عُرف)
    public String relationship;            // نوع العلاقة (صديق، غريب، إلخ)

    public float[] faceEmbedding;           // متجه الوجه (128-256 بعد) للتشابه الدقيق

    public float familiarity;               // درجة الألفة (0..1) - تزداد مع تكرار المشاهدة
    public float trust;                     // درجة الثقة (0..1) - تتأثر بالمشاعر الإيجابية/السلبية

    public String emotionalAssociation;     // المشاعر المرتبطة بهذا الوجه (مثل "سعيد", "خائف")
    public int encounterCount;               // عدد مرات رؤية هذا الوجه
    public long firstSeen;                   // أول مرة شوهد فيها (timestamp)
    public long lastSeen;                     // آخر مرة شوهد فيها (timestamp)

    // منشئ فارغ (يحتاجه Room)
    public IdentityMemory() {}

    // منشئ للحقول الأساسية
    public IdentityMemory(@NonNull String faceHash, String name, String relationship, float[] faceEmbedding) {
        this.faceHash = faceHash;
        this.name = name;
        this.relationship = relationship;
        this.faceEmbedding = faceEmbedding;
        this.familiarity = 0f;
        this.trust = 0.5f;
        this.emotionalAssociation = "neutral";
        this.encounterCount = 0;
        this.firstSeen = System.currentTimeMillis();
        this.lastSeen = this.firstSeen;
    }
}

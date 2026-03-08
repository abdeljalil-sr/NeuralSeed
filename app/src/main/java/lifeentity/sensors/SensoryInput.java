package com.lifeentity.sensors;

/**
 * حاوية موحدة لكل المدخلات الحسية.
 * تحتوي على بيانات خام فقط من الحواس المختلفة.
 * لا تقوم بأي تفسير أو تصنيف (مثل التهديد أو الجدة)، فهذه مسؤولية الوعي.
 */
public class SensoryInput {

    // ====================== بصري (من VisualCortex) ======================
    public float brightness;                // السطوع المتوسط (0..1)
    public float motionLevel;               // مستوى الحركة في الإطار (0..1)
    public boolean hasHumanFace;            // هل يوجد وجه بشري؟
    public float faceProximity;              // قرب الوجه (حجم الوجه نسبة للإطار)
    public String faceId;                    // معرف الوجه (إن وجد)
    public float[] faceEmbedding;            // متجه الوجه (128-256 بعد) – من FaceIdentitySystem
    public String humanExpression;           // تعبير الوجه (إن وجد)
    public int objectCount;                   // عدد الأجسام المكتشفة
    public String dominantObject;             // الجسم الأكبر أو الأكثر وضوحاً
    public byte[] imageBytes;                 // صورة كاملة أو مصغرة (للتخزين في VisualMemory)

    // ====================== سمعي (من AuditoryCortex) ======================
    public float soundVolume;                 // شدة الصوت (0..1)
    public float soundPitch;                  // طبقة الصوت (تقريبية)
    public boolean speechDetected;             // هل تم اكتشاف كلام؟
    public String recognizedSpeech;            // النص المعروف (إن وجد)

    // ====================== لمسي (من واجهة اللمس) ======================
    public boolean isTouched;                  // هل هناك لمس؟
    public float touchX, touchY;                // إحداثيات اللمس (نسبة إلى الشاشة 0..1)
    public float touchPressure;                 // ضغط اللمس (0..1)
    public long touchDuration;                  // مدة اللمس (مللي ثانية)
    public String touchLocation;                 // موقع تقريبي (مثلاً "head", "body", "feet") – يمكن تحديده من الإحداثيات

    // ====================== حركي (من KinestheticSense) ======================
    public float motionIntensity;               // شدة الحركة (تسارع)
    public String posture;                       // الوضعية التقديرية ("upright", "face_down", ...)
    public boolean isShaking;                    // هل يهتز الجهاز بقوة؟
    public boolean isFalling;                     // هل يسقط الجهاز؟

    // ====================== مجالات مستقبلية / للتوسع ======================
    // يمكن إضافة حقول أخرى لاحقاً دون التأثير على التوافق

    public SensoryInput() {
        this.brightness = 0.5f;
        this.soundVolume = 0f;
        this.motionIntensity = 0f;
        // قيم افتراضية أخرى...
    }

    /**
     * دمج مدخلين حسّيين (يستخدم لجمع البيانات من عدة حواس في دورة واحدة).
     * القادم لاحقاً يحل محل القيم الافتراضية.
     */
    public void merge(SensoryInput other) {
        if (other == null) return;

        // بصري
        if (other.brightness != 0.5f) this.brightness = other.brightness;
        if (other.motionLevel > this.motionLevel) this.motionLevel = other.motionLevel;
        if (other.hasHumanFace) {
            this.hasHumanFace = true;
            this.faceProximity = other.faceProximity;
            this.faceId = other.faceId;
            if (other.faceEmbedding != null) this.faceEmbedding = other.faceEmbedding;
        }
        if (other.objectCount > 0) {
            this.objectCount = other.objectCount;
            this.dominantObject = other.dominantObject;
        }
        if (other.imageBytes != null) this.imageBytes = other.imageBytes;

        // سمعي
        if (other.soundVolume > this.soundVolume) {
            this.soundVolume = other.soundVolume;
            this.soundPitch = other.soundPitch;
            this.recognizedSpeech = other.recognizedSpeech;
            this.speechDetected = other.speechDetected;
        }

        // لمسي
        if (other.isTouched) {
            this.isTouched = true;
            this.touchX = other.touchX;
            this.touchY = other.touchY;
            this.touchPressure = other.touchPressure;
            this.touchDuration = other.touchDuration;
            this.touchLocation = other.touchLocation;
        }

        // حركي
        if (other.motionIntensity > this.motionIntensity) {
            this.motionIntensity = other.motionIntensity;
            this.posture = other.posture;
            this.isShaking = other.isShaking;
            this.isFalling = other.isFalling;
        }
    }
}

package com.lifeentity.core;

import com.lifeentity.sensors.SensoryInput;
import java.util.Map;

/**
 * لحظة واعية واحدة - تجربة الكائن في نقطة زمنية
 * تمثل هذه الفئة "الآن" بالنسبة للكائن، وتحتوي على كل ما يشعر به ويفكر فيه
 * في دورة زمنية واحدة (عادة 100ms).
 */
public class ConsciousMoment implements Cloneable {
    public long timestamp;              // الوقت الفعلي (milliseconds)
    public double deltaTime;             // المدة منذ آخر لحظة (ثواني)

    // المدخلات الحسية القادمة من البيئة
    public SensoryInput perception;

    // الحالة الداخلية للجسد (الكيمياء)
    public BodyState bodyState;

    // المحتوى الواعي (ما يدور في الذهن)
    public AttentionFocus focus;         // أين يتجه الانتباه؟
    public EmotionalState emotionalTone; // المشاعر الحالية
    public EmotionalState previousEmotion; // المشاعر في اللحظة السابقة (لقياس التغير)
    public String narrativeThread;       // سرد ذاتي قصير (فكرة أو كلمة)
    public Anticipation anticipation;    // توقع بسيط للمستقبل القريب
    public ExpressiveImpulse expressiveImpulse; // دافع للتعبير (بصري، صوتي، حركي)

    // متغيرات التشابك والفوضى (مضافة من HomeostasisSystem)
    public float[][] entanglementMatrix; // مصفوفة التشابك (9x9)
    public float chaosLevel;             // مستوى الفوضى الحالي (0-1)

    public ConsciousMoment() {
        this.timestamp = System.currentTimeMillis();
        this.deltaTime = 0.1; // 100ms (قيمة افتراضية)
        this.entanglementMatrix = new float[9][9]; // سيتم ملؤها لاحقاً
        this.chaosLevel = 0.5f;
    }

    @Override
    public ConsciousMoment clone() {
        try {
            ConsciousMoment cloned = (ConsciousMoment) super.clone();
            // نسخ المصفوفة يدوياً (لأن clone() لا ينسخ المصفوفات داخلياً)
            if (this.entanglementMatrix != null) {
                cloned.entanglementMatrix = new float[this.entanglementMatrix.length][];
                for (int i = 0; i < this.entanglementMatrix.length; i++) {
                    cloned.entanglementMatrix[i] = this.entanglementMatrix[i].clone();
                }
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            return new ConsciousMoment();
        }
    }

    // ======================= الفئات الداخلية =======================

    /**
     * تركيز الانتباه - ما الذي يشغل الكائن حالياً؟
     */
    public static class AttentionFocus {
        public String subject;      // الموضوع (مثلاً "وجه", "صوت", "الداخل")
        public String reason;       // السبب (ينشأ من الرغبات: "explore", "bond", "introspection", ...)
        public float visualX, visualY; // إحداثيات في مجال الرؤية (إذا كان التركيز بصرياً)

        public AttentionFocus(String s, String r) {
            this.subject = s;
            this.reason = r;
        }
    }

    /**
     * توقع - تخمين بسيط لما سيحدث بعد قليل
     */
    public static class Anticipation {
        public String predictedEvent;  // وصف الحدث المتوقع
        public float probability;      // احتمال حدوثه (0..1)
        public float emotionalValence; // الشحنة العاطفية المتوقعة (سالب=خوف، موجب=متعة)
    }

    /**
     * دافع تعبيري - الرغبة في إخراج شيء إلى العالم (رسم، كلام، حركة)
     * هذا هو مصدر الإبداع لدى الكائن.
     */
    public static class ExpressiveImpulse {
        public String modality;        // طريقة التعبير: "visual", "verbal", "movement"
        public float intensity;        // قوة الدافع (0..1)
        public float[] latentVector;   // المتجه الكامن (128-256 بعد) يمثل الفكرة البصرية
        public String associatedConcept; // مفهوم مرتبط (اختياري، من الذاكرة)
        public int[] hue;              // لون مقترح (احتفاظاً بالتوافق مع الإصدارات السابقة)

        public ExpressiveImpulse(String modality, float intensity, float[] latentVector, String concept) {
            this.modality = modality;
            this.intensity = intensity;
            this.latentVector = latentVector;
            this.associatedConcept = concept;
        }

        // منشئ للتوافق مع النسخ القديمة (يمكن إزالته لاحقاً)
        public ExpressiveImpulse(String form, float intensity, int[] hue) {
            this.modality = form; // نعتبر form هي modality
            this.intensity = intensity;
            this.hue = hue;
        }
    }

    /**
     * حالة الجسد - الترجمة الكمية للكيمياء الداخلية
     */
    public static class BodyState {
        // المتغيرات الأساسية (مشتقة من HomeostasisSystem)
        public double energy;       // الطاقة
        public double arousal;      // اليقظة/الإثارة
        public double stress;       // التوتر
        public double curiosity;    // الفضول
        public double attachment;   // التعلق/الارتباط

        // النواقل العصبية (تؤثر على المشاعر)
        public double dopamine;
        public double cortisol;
        public double serotonin;
        public double oxytocin;

        public BodyState() {}

        public BodyState(Map<String, Double> chemistry) {
            this.energy = chemistry.getOrDefault("energy", 0.5);
            this.arousal = chemistry.getOrDefault("arousal", 0.0);
            this.stress = chemistry.getOrDefault("stress", 0.0);
            this.curiosity = chemistry.getOrDefault("curiosity", 0.5);
            this.attachment = chemistry.getOrDefault("attachment", 0.0);
            this.dopamine = chemistry.getOrDefault("dopamine", 0.3);
            this.cortisol = chemistry.getOrDefault("cortisol", 0.0);
            this.serotonin = chemistry.getOrDefault("serotonin", 0.5);
            this.oxytocin = chemistry.getOrDefault("oxytocin", 0.0);
        }

        // تحويل الحالة إلى متجه عاطفي (5 أبعاد) لاستخدامه في ImaginationEngine
        public float[] toAffectVector() {
            return new float[]{
                (float) arousal,
                (float) dopamine,
                (float) cortisol,
                (float) curiosity,
                (float) attachment
            };
        }
    }
}

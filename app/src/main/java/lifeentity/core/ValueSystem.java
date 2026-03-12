package com.lifeentity.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * نظام القيم - يمثل أهمية وأولوية المفاهيم والأفعال للكائن.
 * 
 * الوظائف:
 * - تخزين قيم رقمية للمفاهيم (أهمية، متعة، خوف، إلخ)
 * - تحديث القيم بناءً على التجارب (تعلم)
 * - توفير استعلامات عن القيم الحالية
 * - ربط القيم بالرغبات والعواطف
 * - دعم التفضيلات الشخصية للكائن
 */
public class ValueSystem {
    private static final String TAG = "ValueSystem";
    
    // خريطة القيم الأساسية: اسم المفهوم -> قيمته (من -1 إلى 1)
    private ConcurrentHashMap<String, Float> values;
    
    // خريطة عدد مرات التعرض (لحساب الثقة)
    private ConcurrentHashMap<String, Integer> exposureCount;
    
    // معامل التعلم: مدى سرعة تغير القيم
    private static final float LEARNING_RATE = 0.1f;
    
    // عتبة الثقة: إذا كان عدد التعرضات أقل من هذا، تكون القيمة غير موثوقة
    private static final int CONFIDENCE_THRESHOLD = 5;
    
    public ValueSystem() {
        this.values = new ConcurrentHashMap<>();
        this.exposureCount = new ConcurrentHashMap<>();
        initializeDefaultValues();
    }
    
    /**
     * تهيئة القيم الافتراضية الأساسية
     */
    private void initializeDefaultValues() {
        // مفاهيم إيجابية
        setInitialValue("فرح", 0.8f);
        setInitialValue("حب", 0.9f);
        setInitialValue("اكتشاف", 0.7f);
        setInitialValue("تواصل", 0.6f);
        setInitialValue("راحة", 0.7f);
        setInitialValue("إبداع", 0.8f);
        setInitialValue("رسم", 0.8f);
        setInitialValue("وجه", 0.5f); // محايد
        
        // مفاهيم سلبية
        setInitialValue("خوف", -0.8f);
        setInitialValue("ألم", -0.9f);
        setInitialValue("وحدة", -0.7f);
        setInitialValue("فشل", -0.6f);
        
        // مفاهيم محايدة
        setInitialValue("صوت", 0.2f);
        setInitialValue("حركة", 0.3f);
        setInitialValue("لمس", 0.4f);
        setInitialValue("نور", 0.2f);
        setInitialValue("ظلام", -0.2f);
    }
    
    /**
     * تعيين قيمة أولية (إذا لم تكن موجودة)
     */
    public void setInitialValue(String concept, float value) {
        values.putIfAbsent(concept, clamp(value));
        exposureCount.putIfAbsent(concept, 0);
    }
    
    /**
     * تحديث قيمة مفهوم بناءً على تجربة جديدة
     * @param concept اسم المفهوم
     * @param delta التغير في القيمة (عادة بين -1 و 1)
     */
    public void learnValue(String concept, float delta) {
        float current = values.getOrDefault(concept, 0f);
        int count = exposureCount.getOrDefault(concept, 0);
        
        // زيادة عدد التعرضات
        exposureCount.put(concept, count + 1);
        
        // تطبيق معدل التعلم (يتناقص مع زيادة التعرض لتحقيق الاستقرار)
        float adaptiveRate = LEARNING_RATE / (1 + count * 0.1f);
        float newValue = current + delta * adaptiveRate;
        
        values.put(concept, clamp(newValue));
    }
    
    /**
     * تحديث قيم متعددة دفعة واحدة (مناسب للربط العاطفي)
     */
    public void learnValues(Map<String, Float> deltas) {
        for (Map.Entry<String, Float> entry : deltas.entrySet()) {
            learnValue(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * الحصول على قيمة مفهوم
     */
    public float getValue(String concept) {
        return values.getOrDefault(concept, 0f);
    }
    
    /**
     * الحصول على القيمة مع مستوى الثقة
     * @return قيمة وثقة (0-1)
     */
    public ValueWithConfidence getValueWithConfidence(String concept) {
        float value = getValue(concept);
        int count = exposureCount.getOrDefault(concept, 0);
        float confidence = Math.min(1f, count / (float) CONFIDENCE_THRESHOLD);
        return new ValueWithConfidence(value, confidence);
    }
    
    /**
     * تقييم مدى إيجابية مفهوم
     */
    public boolean isPositive(String concept) {
        return getValue(concept) > 0.3f;
    }
    
    /**
     * تقييم مدى سلبية مفهوم
     */
    public boolean isNegative(String concept) {
        return getValue(concept) < -0.3f;
    }
    
    /**
     * الحصول على جميع القيم (للتصدير)
     */
    public Map<String, Float> getAllValues() {
        return new HashMap<>(values);
    }
    
    /**
     * الحصول على أكثر المفاهيم إيجابية
     */
    public String getMostPositiveConcept() {
        String best = null;
        float max = -Float.MAX_VALUE;
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            if (entry.getValue() > max && exposureCount.getOrDefault(entry.getKey(), 0) >= CONFIDENCE_THRESHOLD) {
                max = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }
    
    /**
     * الحصول على أكثر المفاهيم سلبية
     */
    public String getMostNegativeConcept() {
        String worst = null;
        float min = Float.MAX_VALUE;
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            if (entry.getValue() < min && exposureCount.getOrDefault(entry.getKey(), 0) >= CONFIDENCE_THRESHOLD) {
                min = entry.getValue();
                worst = entry.getKey();
            }
        }
        return worst;
    }
    
    /**
     * حساب القيمة الإجمالية لموقف (مجموع قيم مفاهيم متعددة)
     */
    public float evaluateSituation(String... concepts) {
        float total = 0;
        for (String c : concepts) {
            total += getValue(c);
        }
        return clamp(total / concepts.length);
    }
    
    /**
     * ربط قيمة المفهوم بحالة عاطفية
     * @param emotion الحالة العاطفية الحالية
     * @param concept المفهوم المتعلق
     */
    public void associateWithEmotion(EmotionalState emotion, String concept) {
        float emotionalValence = emotion.getDopamine() - emotion.getCortisol(); // من -1 إلى 1 تقريباً
        learnValue(concept, emotionalValence * 0.2f);
    }
    
    /**
     * تصدير الإحصائيات
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConcepts", values.size());
        stats.put("positiveCount", values.values().stream().filter(v -> v > 0.3f).count());
        stats.put("negativeCount", values.values().stream().filter(v -> v < -0.3f).count());
        stats.put("mostPositive", getMostPositiveConcept());
        stats.put("mostNegative", getMostNegativeConcept());
        return stats;
    }
    
    /**
     * تطبيع القيمة بين -1 و 1
     */
    private float clamp(float value) {
        return Math.max(-1f, Math.min(1f, value));
    }
    
    /**
     * فئة مساعدة تمثل قيمة مع ثقة
     */
    public static class ValueWithConfidence {
        public final float value;
        public final float confidence;
        
        public ValueWithConfidence(float value, float confidence) {
            this.value = value;
            this.confidence = confidence;
        }
        
        public float getWeightedValue() {
            return value * confidence;
        }
    }
}

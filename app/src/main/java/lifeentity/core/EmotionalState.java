package com.lifeentity.core;

import java.util.HashMap;
import java.util.Map;

/**
 * الحالة العاطفية - ترجمة الكيمياء الداخلية إلى تجربة ذاتية
 * تحتوي على متجه عاطفي (affect vector) يستخدم في التوليد البصري والخيال.
 */
public class EmotionalState {
    private Map<String, Double> dimensions;

    public EmotionalState() {
        dimensions = new HashMap<>();
        dimensions.put("energy", 0.5);
        dimensions.put("arousal", 0.1);
        dimensions.put("stress", 0.0);
        dimensions.put("curiosity", 0.5);
        dimensions.put("attachment", 0.0);
        dimensions.put("dopamine", 0.3);
        dimensions.put("cortisol", 0.0);
        dimensions.put("serotonin", 0.5);
        dimensions.put("oxytocin", 0.0);
    }
    // أضف هذه الدوال داخل كلاس EmotionalState:

    public boolean isAngry() {
        return getArousal() > 0.7 && getCortisol() > 0.5 && getDopamine() < 0.3;
    }


    public boolean isHopeful() {
        return getDopamine() > 0.5 && getCortisol() < 0.3 && getCuriosity() > 0.4;
    }

    public EmotionalState(Map<String, Double> chemistry) {
        this.dimensions = new HashMap<>(chemistry);
    }

    // ========================== التوابع الأساسية ==========================

    public double getEnergy() { return dimensions.getOrDefault("energy", 0.5); }
    public double getArousal() { return dimensions.getOrDefault("arousal", 0.0); }
    public double getStress() { return dimensions.getOrDefault("stress", 0.0); }
    public double getCuriosity() { return dimensions.getOrDefault("curiosity", 0.5); }
    public double getAttachment() { return dimensions.getOrDefault("attachment", 0.0); }
    public double getDopamine() { return dimensions.getOrDefault("dopamine", 0.3); }
    public double getCortisol() { return dimensions.getOrDefault("cortisol", 0.0); }
    public double getSerotonin() { return dimensions.getOrDefault("serotonin", 0.5); }
    public double getOxytocin() { return dimensions.getOrDefault("oxytocin", 0.0); }

    public float getIntensity() {
        return (float)(getArousal() + Math.abs(getDopamine() - getCortisol()));
    }

    /**
     * تحويل الحالة العاطفية إلى متجه عاطفي (5 أبعاد) يستخدم في ImaginationEngine
     * الترتيب: arousal, dopamine, cortisol, curiosity, attachment
     */
    public float[] toAffectVector() {
        return new float[]{
            (float) getArousal(),
            (float) getDopamine(),
            (float) getCortisol(),
            (float) getCuriosity(),
            (float) getAttachment()
        };
    }

    // ========================== الاستعلامات النوعية ==========================

    public boolean isJoyful() { return getDopamine() > 0.7 && getCortisol() < 0.3; }
    public boolean isAfraid() { return getCortisol() > 0.6; }
    public boolean isCurious() { return getCuriosity() > 0.7; }
    public boolean isSad() { return getSerotonin() < 0.3 && getDopamine() < 0.3; }
    public boolean isCalm() { return getArousal() < 0.3 && getSerotonin() > 0.6; }
    public boolean isExcited() { return getArousal() > 0.7 && getDopamine() > 0.6; }
    public boolean isConfused() { return getArousal() > 0.5 && getCuriosity() > 0.5 && getStress() > 0.3; }
    public boolean isTurbulent() { return getArousal() > 0.7 || getStress() > 0.6; }
    public boolean isContemplative() { return getArousal() < 0.4 && getCuriosity() > 0.5; }
    public boolean isSocial() { return getAttachment() > 0.4 || getOxytocin() > 0.5; }

    // ========================== التمثيل النصي والبصري ==========================

    public String toExpression() {
        if (isAfraid()) return "fear";
        if (isJoyful()) return "joy";
        if (isSad()) return "sadness";
        if (isCurious()) return "curiosity";
        if (isCalm()) return "calm";
        return "neutral";
    }

    public String toArabic() {
        if (isAfraid()) return "خائف";
        if (isJoyful()) return "سعيد";
        if (isSad()) return "حزين";
        if (isCurious()) return "فضولي";
        if (isCalm()) return "هادئ";
        if (isExcited()) return "متحمس";
        if (isConfused()) return "مرتبك";
        return "محايد";
    }

    /**
     * اقتراح لون تقريبي للحالة العاطفية (يستخدم في واجهات قديمة)
     */
    public int[] getHue() {
        if (isAfraid()) return new int[]{255, 50, 50};      // أحمر
        if (isJoyful()) return new int[]{255, 215, 0};     // ذهبي
        if (isSad()) return new int[]{100, 100, 150};      // أزرق رمادي
        if (isCurious()) return new int[]{153, 50, 204};   // بنفسجي
        if (isCalm()) return new int[]{70, 130, 180};      // أزرق هادئ
        return new int[]{200, 200, 200};                   // رمادي
    }
}

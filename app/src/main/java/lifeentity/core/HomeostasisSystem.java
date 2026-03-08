package com.lifeentity.core;

import com.lifeentity.sensors.SensoryInput;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * نظام الاتزان الداخلي - الكيمياء الحية للكائن
 * يحاكي العمليات البيوكيميائية التي تولد المشاعر والرغبات.
 * يحتوي على نواقل عصبية ومتغيرات فسيولوجية تتغير بمرور الوقت وتتأثر بالمدخلات الحسية.
 */
public class HomeostasisSystem {
    
    private Map<String, Double> chemistry;      // القيم الحالية
    private Map<String, Double> derivatives;    // المشتقات للتكامل العددي
    private Random random;                       // للتغيرات العشوائية
    
    // معاملات شخصية الكائن (تتغير من كائن لآخر)
    private final double BASELINE_ENERGY;
    private final double RECOVERY_RATE;
    private final double SENSITIVITY;
    
    public HomeostasisSystem() {
        chemistry = new HashMap<>();
        derivatives = new HashMap<>();
        random = new Random();
        
        // قيم أولية عشوائية تعطي شخصية فريدة لكل كائن
        BASELINE_ENERGY = 0.2 + random.nextDouble() * 0.6;
        RECOVERY_RATE = 0.0005 + random.nextDouble() * 0.003;
        SENSITIVITY = 0.3 + random.nextDouble() * 1.5;
        
        chemistry.put("energy", BASELINE_ENERGY);
        chemistry.put("arousal", 0.1 + random.nextDouble() * 0.2);
        chemistry.put("stress", random.nextDouble() * 0.2);
        chemistry.put("curiosity", 0.3 + random.nextDouble() * 0.5);
        chemistry.put("attachment", random.nextDouble() * 0.3);
        chemistry.put("dopamine", 0.2 + random.nextDouble() * 0.3);
        chemistry.put("cortisol", random.nextDouble() * 0.2);
        chemistry.put("serotonin", 0.3 + random.nextDouble() * 0.4);
        chemistry.put("oxytocin", random.nextDouble() * 0.2);
    }
    
    /**
     * تحديث الحالة الداخلية بناءً على مرور الزمن
     * @param deltaTime الزمن المنقضي منذ آخر تحديث (بالثواني)
     */
    public void update(double deltaTime) {
        // استهلاك الطاقة بناءً على الإثارة والتوتر
        double energyDrain = chemistry.get("arousal") * 0.01 
                           + chemistry.get("stress") * 0.02 
                           + random.nextDouble() * 0.005;
        
        // استعادة الطاقة نحو خط الأساس
        double energyRecovery = (BASELINE_ENERGY - chemistry.get("energy")) * RECOVERY_RATE;
        setDerivative("energy", energyRecovery - energyDrain + random.nextGaussian() * 0.001);
        
        // الإثارة تتناقص طبيعياً
        setDerivative("arousal", -chemistry.get("arousal") * 0.1 + random.nextGaussian() * 0.002);
        
        // التوتر يقل بوجود الأوكسيتوسين
        double stressDecay = chemistry.get("oxytocin") * 0.05 + random.nextDouble() * 0.01;
        setDerivative("stress", -stressDecay);
        
        // الفضول: ميل طبيعي للاستكشاف
        double noveltySeeking = (1 - chemistry.get("curiosity")) * 0.01 + random.nextDouble() * 0.005;
        setDerivative("curiosity", noveltySeeking);
        
        // التعلق يتلاشى ببطء
        setDerivative("attachment", -chemistry.get("attachment") * 0.001 + random.nextGaussian() * 0.0005);
        
        // تحديث النواقل العصبية
        updateHormones();
        
        // تكامل التغيرات
        integrate(deltaTime);
        
        // ضمان بقاء القيم ضمن النطاق [0,1]
        clampAll();
    }
    
    /**
     * تحديث النواقل العصبية بناءً على المتغيرات الأساسية
     */
    private void updateHormones() {
        // الدوبامين: يتأثر بالفضول والطاقة
        double dopamine = chemistry.get("curiosity") * 0.3 
                        + (chemistry.get("energy") > 0.7 ? 0.2 : 0) 
                        + random.nextDouble() * 0.1;
        setDerivative("dopamine", (dopamine - chemistry.get("dopamine")) * 0.1);
        
        // الكورتيزول: يتناسب مع التوتر
        double cortisol = chemistry.get("stress") * 0.8 + random.nextDouble() * 0.1;
        setDerivative("cortisol", (cortisol - chemistry.get("cortisol")) * 0.05);
        
        // السيروتونين: يقل بالتوتر والإثارة
        double serotonin = 1 - chemistry.get("cortisol") 
                           - chemistry.get("arousal") * 0.5 
                           + random.nextGaussian() * 0.05;
        setDerivative("serotonin", (serotonin - chemistry.get("serotonin")) * 0.08);
        
        // الأوكسيتوسين: يتأثر بالتعلق والسيروتونين
        double oxytocin = chemistry.get("attachment") * chemistry.get("serotonin") 
                        + random.nextDouble() * 0.1;
        setDerivative("oxytocin", (oxytocin - chemistry.get("oxytocin")) * 0.05);
    }
    
    /**
     * تعديل الكيمياء بناءً على المدخلات الحسية
     * التأثيرات غير حتمية، وتعتمد على حساسية الكائن
     */
    public void modulateByPerception(SensoryInput perception) {
        if (perception == null) return;
        
        // الضوء الساطع يزيد الإثارة
        if (perception.brightness > 0.8) {
            chemistry.put("arousal", 
                Math.min(1, chemistry.get("arousal") + 0.05 * SENSITIVITY + random.nextDouble() * 0.05));
        }
        
        // الأصوات العالية تزيد التوتر
        if (perception.soundVolume > 0.7) {
            chemistry.put("stress", 
                Math.min(1, chemistry.get("stress") + 0.1 * SENSITIVITY + random.nextDouble() * 0.1));
        }
        
        // اللمس يزيد التعلق والأوكسيتوسين والإثارة
        if (perception.isTouched) {
            chemistry.put("attachment", 
                Math.min(1, chemistry.get("attachment") + 0.02 + random.nextDouble() * 0.02));
            chemistry.put("oxytocin", 
                Math.min(1, chemistry.get("oxytocin") + 0.03 + random.nextDouble() * 0.04));
            chemistry.put("arousal", 
                Math.min(1, chemistry.get("arousal") + 0.1 * SENSITIVITY + random.nextDouble() * 0.1));
        }
        
        // وجود وجه بشري يزيد الفضول، ويعزز الدوبامين إذا كان هناك تعلق مسبق
        if (perception.hasHumanFace) {
            chemistry.put("curiosity", 
                Math.min(1, chemistry.get("curiosity") + 0.05 + random.nextDouble() * 0.1));
            if (chemistry.get("attachment") > 0.3) {
                chemistry.put("dopamine", 
                    Math.min(1, chemistry.get("dopamine") + 0.05 + random.nextDouble() * 0.1));
            }
        }
    }
    
    /**
     * @return الحالة العاطفية الحالية
     */
    public EmotionalState getEmotionalState() {
        return new EmotionalState(chemistry);
    }
    
    /**
     * @return حالة الجسد (BodyState) المستخدمة في ConsciousMoment
     */
    public ConsciousMoment.BodyState getCurrentState() {
        return new ConsciousMoment.BodyState(chemistry);
    }
    
    // ========================== دوال مساعدة ==========================
    
    private void integrate(double dt) {
        for (String key : chemistry.keySet()) {
            double val = chemistry.get(key) + derivatives.getOrDefault(key, 0.0) * dt;
            chemistry.put(key, val);
        }
    }
    
    private void clampAll() {
        for (String key : chemistry.keySet()) {
            double val = chemistry.get(key);
            chemistry.put(key, Math.max(0, Math.min(1, val)));
        }
    }
    
    private void setDerivative(String key, double value) {
        derivatives.put(key, value);
    }
}

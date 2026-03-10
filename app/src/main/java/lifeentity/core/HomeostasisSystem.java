package com.lifeentity.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.lifeentity.memory.AppDatabase;
import com.lifeentity.sensors.SensoryInput;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * نظام الاتزان الداخلي المتقدم - الكيمياء الحية الديناميكية للكائن
 * يدعم التكيف المستمر، تأثيرات الأحلام، والتعلم من التجارب.
 * مع إضافة مصفوفة التشابك (entanglement matrix) لخلق فوضى منتظمة.
 */
public class HomeostasisSystem {
    private static final String TAG = "HomeostasisSystem";
    private static final String PREFS_NAME = "homeostasis_prefs";
    
    // معاملات قابلة للتكيف (تُحمل من SharedPreferences)
    private double baselineEnergy;
    private double recoveryRate;
    private double sensitivity;
    private double adaptability; // معامل جديد: سرعة التكيف مع التجارب
    
    // معاملات ثابتة للخوارزمية
    private static final double DREAM_CORTISOL_REDUCTION = 0.15;
    private static final double DREAM_SEROTONIN_BOOST = 0.12;
    private static final double LEARNING_RATE = 0.001;
    private static final double NOISE_AMPLITUDE = 0.05;
    
    private Map<String, Double> chemistry;
    private Map<String, Double> derivatives;
    private Map<String, Double> baselineTrends; // اتجاهات الأساس المتغيرة
    private Random random;
    private Context context;
    private SharedPreferences prefs;
    private ExecutorService dbExecutor;
    private AppDatabase database;
    
    // حالة النظام
    private boolean isDreaming = false;
    private long lastDreamTime = 0;
    private int experienceCount = 0;
    private double accumulatedStress = 0;
    private double accumulatedJoy = 0;
    
    // مصفوفة التشابك (Entanglement Matrix) - 9 متغيرات
    private float[][] entanglementMatrix;
    private float chaosLevel;
    private static final int NUM_VARS = 9; // energy, arousal, stress, curiosity, attachment, dopamine, cortisol, serotonin, oxytocin
    
    // أسماء المتغيرات للوصول بالمصفوفة
    private static final int IDX_ENERGY = 0;
    private static final int IDX_AROUSAL = 1;
    private static final int IDX_STRESS = 2;
    private static final int IDX_CURIOSITY = 3;
    private static final int IDX_ATTACHMENT = 4;
    private static final int IDX_DOPAMINE = 5;
    private static final int IDX_CORTISOL = 6;
    private static final int IDX_SEROTONIN = 7;
    private static final int IDX_OXYTOCIN = 8;

    public HomeostasisSystem() {
        this(null, null);
    }
    
    public HomeostasisSystem(Context context, AppDatabase db) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.database = db;
        this.chemistry = new HashMap<>();
        this.derivatives = new HashMap<>();
        this.baselineTrends = new HashMap<>();
        this.random = new Random();
        this.dbExecutor = Executors.newSingleThreadExecutor();
        
        // تهيئة مصفوفة التشابك
        this.entanglementMatrix = new float[NUM_VARS][NUM_VARS];
        initializeEntanglementMatrix();
        this.chaosLevel = 0.5f; // مستوى فوضوي معتدل
        
        initializeParameters();
        initializeChemistry();
        loadPersistedState();
    }
    
    /**
     * تهيئة مصفوفة التشابك بقيم عشوائية أولية
     */
    private void initializeEntanglementMatrix() {
        for (int i = 0; i < NUM_VARS; i++) {
            for (int j = 0; j < NUM_VARS; j++) {
                if (i == j) {
                    entanglementMatrix[i][j] = 0.1f; // تأثير الذات على الذات ضعيف
                } else {
                    entanglementMatrix[i][j] = random.nextFloat() * 0.3f; // 0-0.3
                }
            }
        }
    }
    
    /**
     * تهيئة المعاملات من SharedPreferences أو قيم عشوائية فريدة
     */
    private void initializeParameters() {
        if (context != null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            
            // تحميل المعاملات المحفوظة أو إنشاء قيم فريدة
            baselineEnergy = prefs.getFloat("baseline_energy", -1f);
            if (baselineEnergy < 0) {
                baselineEnergy = 0.2 + random.nextDouble() * 0.6;
                saveParameter("baseline_energy", baselineEnergy);
            }
            
            recoveryRate = prefs.getFloat("recovery_rate", -1f);
            if (recoveryRate < 0) {
                recoveryRate = 0.0005 + random.nextDouble() * 0.003;
                saveParameter("recovery_rate", recoveryRate);
            }
            
            sensitivity = prefs.getFloat("sensitivity", -1f);
            if (sensitivity < 0) {
                sensitivity = 0.3 + random.nextDouble() * 1.5;
                saveParameter("sensitivity", sensitivity);
            }
            
            adaptability = prefs.getFloat("adaptability", -1f);
            if (adaptability < 0) {
                adaptability = 0.1 + random.nextDouble() * 0.4;
                saveParameter("adaptability", adaptability);
            }
            
            experienceCount = prefs.getInt("experience_count", 0);
            
        } else {
            // وضع عدم الاتصال: استخدام قيم افتراضية
            baselineEnergy = 0.5;
            recoveryRate = 0.002;
            sensitivity = 1.0;
            adaptability = 0.25;
        }
        
        Log.i(TAG, String.format("Initialized: energy=%.3f, recovery=%.4f, sensitivity=%.2f, adaptability=%.2f",
            baselineEnergy, recoveryRate, sensitivity, adaptability));
    }
    
    /**
     * تهيئة القيم الكيميائية الأولية
     */
    private void initializeChemistry() {
        chemistry.put("energy", baselineEnergy);
        chemistry.put("arousal", sigmoid(0.1 + random.nextDouble() * 0.2));
        chemistry.put("stress", sigmoid(random.nextDouble() * 0.2));
        chemistry.put("curiosity", sigmoid(0.3 + random.nextDouble() * 0.5));
        chemistry.put("attachment", sigmoid(random.nextDouble() * 0.3));
        chemistry.put("dopamine", sigmoid(0.2 + random.nextDouble() * 0.3));
        chemistry.put("cortisol", sigmoid(random.nextDouble() * 0.2));
        chemistry.put("serotonin", sigmoid(0.3 + random.nextDouble() * 0.4));
        chemistry.put("oxytocin", sigmoid(random.nextDouble() * 0.2));
        
        // تهيئة الاتجاهات الأساسية
        baselineTrends.put("energy", baselineEnergy);
        baselineTrends.put("curiosity", 0.4);
        baselineTrends.put("serotonin", 0.5);
    }
    
    /**
     * تحميل الحالة المستمرة من قاعدة البيانات
     */
    private void loadPersistedState() {
        if (database == null) return;
        
        dbExecutor.execute(() -> {
            try {
                Log.d(TAG, "Database persistence ready for future implementation");
            } catch (Exception e) {
                Log.e(TAG, "Error loading persisted state", e);
            }
        });
    }

    /**
     * تحديث الحالة الداخلية مع ديناميكية غير خطية ومصفوفة تشابك
     */
    public void update(double deltaTime) {
        experienceCount++;
        
        // تخزين القيم القديمة
        double[] oldVars = new double[NUM_VARS];
        oldVars[IDX_ENERGY] = chemistry.get("energy");
        oldVars[IDX_AROUSAL] = chemistry.get("arousal");
        oldVars[IDX_STRESS] = chemistry.get("stress");
        oldVars[IDX_CURIOSITY] = chemistry.get("curiosity");
        oldVars[IDX_ATTACHMENT] = chemistry.get("attachment");
        oldVars[IDX_DOPAMINE] = chemistry.get("dopamine");
        oldVars[IDX_CORTISOL] = chemistry.get("cortisol");
        oldVars[IDX_SEROTONIN] = chemistry.get("serotonin");
        oldVars[IDX_OXYTOCIN] = chemistry.get("oxytocin");
        
        // دالة استهلاك الطاقة غير خطية (تتسارع مع الإثارة العالية)
        double arousalFactor = sigmoid(oldVars[IDX_AROUSAL] * 2 - 1);
        double stressFactor = tanh(oldVars[IDX_STRESS] * 3);
        double energyDrain = arousalFactor * 0.015 
                           + stressFactor * 0.025 
                           + gaussianNoise() * 0.005;
        
        // استعادة الطاقة مع تكيف البنية الأساسية
        double adaptiveBaseline = baselineTrends.get("energy");
        double energyRecovery = (adaptiveBaseline - oldVars[IDX_ENERGY]) * recoveryRate;
        setDerivative("energy", energyRecovery - energyDrain + gaussianNoise() * 0.001);
        
        // الإثارة: دالة تناقص غير خطية
        double arousalDecay = oldVars[IDX_AROUSAL] * (0.08 + oldVars[IDX_SEROTONIN] * 0.05);
        setDerivative("arousal", -arousalDecay + gaussianNoise() * 0.002);
        
        // التوتر: تأثير الأوكسيتوسين غير الخطي
        double oxytocinEffect = sigmoid(oldVars[IDX_OXYTOCIN] * 4 - 2) * 0.08;
        double stressDecay = oxytocinEffect + random.nextDouble() * 0.008;
        setDerivative("stress", -stressDecay);
        
        // الفضول: دالة سينية للتذبذب الطبيعي
        double time = System.currentTimeMillis() / 1000.0;
        double circadianRhythm = Math.sin(time / 86400 * 2 * Math.PI) * 0.1;
        double noveltySeeking = sigmoid((1 - oldVars[IDX_CURIOSITY]) * 2) * 0.012 
                              + circadianRhythm * 0.005;
        setDerivative("curiosity", noveltySeeking);
        
        // التعلق: تلاشٍ بطيء مع تعزيز عند التفاعل
        double attachmentDecay = oldVars[IDX_ATTACHMENT] * 0.0008;
        setDerivative("attachment", -attachmentDecay + gaussianNoise() * 0.0003);
        
        // تحديث النواقل العصبية المتقدم
        updateAdvancedHormones(deltaTime, oldVars);
        
        // تكامل التغيرات مع تأثير مصفوفة التشابك
        integrateWithEntanglement(deltaTime, oldVars);
        
        // ضمان النطاق مع smooth clipping
        smoothClampAll();
        
        // التكيف طويل المدى
        adaptBaselines();
        
        // حفظ الحالة دورياً
        if (experienceCount % 100 == 0) {
            saveCurrentState();
        }
    }
    
    /**
     * تحديث النواقل العصبية مع ديناميكية معقدة
     */
    private void updateAdvancedHormones(double deltaTime, double[] oldVars) {
        // الدوبامين: نظام مكافأة متكيف
        double rewardSignal = oldVars[IDX_CURIOSITY] * 0.25 
                            + (oldVars[IDX_ENERGY] > 0.7 ? 0.15 : 0)
                            + (oldVars[IDX_ATTACHMENT] > 0.5 ? 0.1 : 0);
        double dopamineTarget = sigmoid(rewardSignal * 3);
        double dopaminePlasticity = 0.1 + adaptability * 0.2;
        setDerivative("dopamine", (dopamineTarget - oldVars[IDX_DOPAMINE]) * dopaminePlasticity);
        
        // الكورتيزول: نظام إجهاد متكيف
        double stressInput = oldVars[IDX_STRESS] * 0.9 
                           + (1 - oldVars[IDX_ENERGY]) * 0.2;
        double cortisolTarget = tanh(stressInput * 2);
        
        // تأثير الأحلام على الكورتيزول
        if (isDreaming) {
            cortisolTarget *= (1 - DREAM_CORTISOL_REDUCTION);
        }
        
        setDerivative("cortisol", (cortisolTarget - oldVars[IDX_CORTISOL]) * 0.04);
        
        // السيروتونين: نظام مزاج مع تأثيرات الأحلام
        double serotoninBase = 1 - oldVars[IDX_CORTISOL] * 0.8 
                               - oldVars[IDX_AROUSAL] * 0.3;
        double serotoninTarget = sigmoid(serotoninBase * 2 - 1);
        
        // تعزيز السيروتونين أثناء الأحلام
        if (isDreaming) {
            serotoninTarget = Math.min(1.0, serotoninTarget + DREAM_SEROTONIN_BOOST);
        }
        
        setDerivative("serotonin", (serotoninTarget - oldVars[IDX_SEROTONIN]) * 0.06);
        
        // الأوكسيتوسين: رابطة اجتماعية ديناميكية
        double socialBonding = oldVars[IDX_ATTACHMENT] * oldVars[IDX_SEROTONIN];
        double oxytocinTarget = sigmoid(socialBonding * 3 - 1.5) * 0.7 + 0.3;
        setDerivative("oxytocin", (oxytocinTarget - oldVars[IDX_OXYTOCIN]) * 0.04);
    }
    
    /**
     * تكامل التغيرات مع مصفوفة التشابك
     */
    private void integrateWithEntanglement(double dt, double[] oldVars) {
        double[] newVars = new double[NUM_VARS];
        double[] deltas = new double[NUM_VARS];
        
        // حساب التغيرات الأساسية من المشتقات
        deltas[IDX_ENERGY] = derivatives.getOrDefault("energy", 0.0) * dt;
        deltas[IDX_AROUSAL] = derivatives.getOrDefault("arousal", 0.0) * dt;
        deltas[IDX_STRESS] = derivatives.getOrDefault("stress", 0.0) * dt;
        deltas[IDX_CURIOSITY] = derivatives.getOrDefault("curiosity", 0.0) * dt;
        deltas[IDX_ATTACHMENT] = derivatives.getOrDefault("attachment", 0.0) * dt;
        deltas[IDX_DOPAMINE] = derivatives.getOrDefault("dopamine", 0.0) * dt;
        deltas[IDX_CORTISOL] = derivatives.getOrDefault("cortisol", 0.0) * dt;
        deltas[IDX_SEROTONIN] = derivatives.getOrDefault("serotonin", 0.0) * dt;
        deltas[IDX_OXYTOCIN] = derivatives.getOrDefault("oxytocin", 0.0) * dt;
        
        // تطبيق تأثير التشابك: كل متغير يتأثر بتغيرات المتغيرات الأخرى
        for (int i = 0; i < NUM_VARS; i++) {
            double entangledDelta = deltas[i];
            for (int j = 0; j < NUM_VARS; j++) {
                if (i != j) {
                    entangledDelta += deltas[j] * entanglementMatrix[i][j];
                }
            }
            // إضافة تأثير عشوائي فوضوي
            entangledDelta += (random.nextGaussian() * 0.01) * chaosLevel;
            newVars[i] = oldVars[i] + entangledDelta;
        }
        
        // تحديث الخريطة
        chemistry.put("energy", newVars[IDX_ENERGY]);
        chemistry.put("arousal", newVars[IDX_AROUSAL]);
        chemistry.put("stress", newVars[IDX_STRESS]);
        chemistry.put("curiosity", newVars[IDX_CURIOSITY]);
        chemistry.put("attachment", newVars[IDX_ATTACHMENT]);
        chemistry.put("dopamine", newVars[IDX_DOPAMINE]);
        chemistry.put("cortisol", newVars[IDX_CORTISOL]);
        chemistry.put("serotonin", newVars[IDX_SEROTONIN]);
        chemistry.put("oxytocin", newVars[IDX_OXYTOCIN]);
        
        // تحديث مصفوفة التشابك بناءً على الارتباطات الفعلية (Hebbian-like learning)
        updateEntanglementMatrix(oldVars, newVars);
        
        // تحديث مستوى الفوضى
        updateChaosLevel(newVars);
    }
    
    /**
     * تحديث مصفوفة التشابك بناءً على التغيرات المتزامنة
     */
    private void updateEntanglementMatrix(double[] oldVars, double[] newVars) {
        double[] actualDeltas = new double[NUM_VARS];
        for (int i = 0; i < NUM_VARS; i++) {
            actualDeltas[i] = newVars[i] - oldVars[i];
        }
        
        float learningRate = 0.01f;
        for (int i = 0; i < NUM_VARS; i++) {
            for (int j = 0; j < NUM_VARS; j++) {
                if (i != j) {
                    // إذا تحرك المتغيران معاً، نعزز الارتباط
                    float correlation = (float) (actualDeltas[i] * actualDeltas[j]);
                    entanglementMatrix[i][j] += learningRate * correlation;
                    // قص القيم لتبقى ضمن نطاق معقول
                    if (entanglementMatrix[i][j] > 0.5f) entanglementMatrix[i][j] = 0.5f;
                    if (entanglementMatrix[i][j] < -0.5f) entanglementMatrix[i][j] = -0.5f;
                }
            }
        }
    }
    
    /**
     * تحديث مستوى الفوضى بناءً على تباين المتغيرات
     */
    private void updateChaosLevel(double[] vars) {
        double mean = 0;
        for (double v : vars) mean += v;
        mean /= NUM_VARS;
        
        double variance = 0;
        for (double v : vars) variance += (v - mean) * (v - mean);
        variance /= NUM_VARS;
        
        // مستوى الفوضى يتناسب مع التباين
        chaosLevel = (float) Math.min(1.0, variance * 4);
    }
    
    /**
     * تأثيرات الأحلام على الكيمياء
     */
    public void dreamCycle(double durationMinutes) {
        isDreaming = true;
        lastDreamTime = System.currentTimeMillis();
        
        Log.d(TAG, "Starting dream cycle for " + durationMinutes + " minutes");
        
        // تقليل الكورتيزول بشكل تدريجي
        double currentCortisol = chemistry.get("cortisol");
        chemistry.put("cortisol", Math.max(0, currentCortisol - DREAM_CORTISOL_REDUCTION * sigmoid(durationMinutes / 60)));
        
        // زيادة السيروتونين
        double currentSerotonin = chemistry.get("serotonin");
        chemistry.put("serotonin", Math.min(1.0, currentSerotonin + DREAM_SEROTONIN_BOOST * sigmoid(durationMinutes / 30)));
        
        // إعادة ضبط الطاقة بشكل طفيف
        double energyBoost = 0.05 * sigmoid(durationMinutes / 90);
        chemistry.put("energy", Math.min(1.0, chemistry.get("energy") + energyBoost));
        
        // معالجة الذكريات العاطفية أثناء النوم
        processEmotionalMemoryConsolidation();
        
        isDreaming = false;
        
        // حفظ الحالة بعد الاستيقاظ
        saveCurrentState();
    }
    
    /**
     * معالجة توطيد الذاكرة العاطفية
     */
    private void processEmotionalMemoryConsolidation() {
        // تقليل الأثر السلبي للتجارب المجهدة
        if (accumulatedStress > accumulatedJoy) {
            double stressRelief = accumulatedStress * 0.1;
            chemistry.put("stress", Math.max(0, chemistry.get("stress") - stressRelief));
            accumulatedStress *= 0.8; // تلاشٍ تدريجي
        }
        
        // تعزيز التجارب الإيجابية
        if (accumulatedJoy > 0) {
            baselineTrends.put("serotonin", Math.min(0.8, baselineTrends.get("serotonin") + accumulatedJoy * 0.01));
            accumulatedJoy *= 0.9;
        }
    }

    /**
     * تعديل الكيمياء بناءً على المدخلات الحسية - ديناميكية غير خطية
     */
    public void modulateByPerception(SensoryInput perception) {
        if (perception == null) return;
        
        experienceCount++;
        
        // الضوء الساطع: استجابة سينية
        if (perception.brightness > 0.6) {
            double lightImpact = sigmoid((perception.brightness - 0.6) * 5) * 0.08 * sensitivity;
            chemistry.put("arousal", smoothClamp(chemistry.get("arousal") + lightImpact));
            
            // تكيف مع الإضاءة المستمرة
            if (perception.brightness > 0.9 && experienceCount > 50) {
                sensitivity *= (1 - adaptability * 0.001); // تقليل الحساسية مع التكيف
            }
        }
        
        // الأصوات: استجابة تناسبية مع العتبة
        if (perception.soundVolume > 0.5) {
            double volumeImpact = tanh((perception.soundVolume - 0.5) * 4) * 0.12 * sensitivity;
            chemistry.put("stress", smoothClamp(chemistry.get("stress") + volumeImpact));
            
            // تسجيل التجربة
            if (volumeImpact > 0.05) accumulatedStress += volumeImpact;
        }
        
        // اللمس: تأثيرات متعددة غير خطية
        if (perception.isTouched) {
            double touchIntensity = 0.03 + random.nextDouble() * 0.02;
            
            // الأوكسيتوسين: استجابة سريعة
            chemistry.put("oxytocin", smoothClamp(chemistry.get("oxytocin") + touchIntensity * 1.5));
            
            // التعلق: تراكم بطيء
            double attachmentBoost = touchIntensity * (1 + chemistry.get("oxytocin"));
            chemistry.put("attachment", smoothClamp(chemistry.get("attachment") + attachmentBoost));
            
            // الإثارة: استجابة مؤقتة
            double arousalBoost = 0.08 * sensitivity * (1 - chemistry.get("attachment") * 0.3);
            chemistry.put("arousal", smoothClamp(chemistry.get("arousal") + arousalBoost));
            
            accumulatedJoy += touchIntensity;
        }
        
        // وجه بشري: استجابة اجتماعية معقدة
        if (perception.hasHumanFace) {
            double faceNovelty = sigmoid((1 - chemistry.get("curiosity")) * 3) * 0.06;
            chemistry.put("curiosity", smoothClamp(chemistry.get("curiosity") + faceNovelty));
            
            // مكافأة الدوبامين إذا كان هناك تعلق
            if (chemistry.get("attachment") > 0.3) {
                double socialReward = sigmoid(chemistry.get("attachment") * 3) * 0.08;
                chemistry.put("dopamine", smoothClamp(chemistry.get("dopamine") + socialReward));
                accumulatedJoy += socialReward * 0.5;
            }
        }
        
        // تعلم من التجربة
        learnFromExperience(perception);
    }
    
    /**
     * التعلم من التجارب وتعديل المعاملات
     */
    private void learnFromExperience(SensoryInput perception) {
        if (experienceCount % 50 != 0) return; // تعلم دوري
        
        // تكيف البنية الأساسية للطاقة
        double avgEnergy = chemistry.get("energy");
        if (avgEnergy < 0.2) {
            baselineTrends.put("energy", Math.min(0.8, baselineTrends.get("energy") + adaptability * 0.01));
        } else if (avgEnergy > 0.8) {
            baselineTrends.put("energy", Math.max(0.3, baselineTrends.get("energy") - adaptability * 0.005));
        }
        
        // تعديل الحساسية بناءً على التجارب
        if (accumulatedStress > 2.0) {
            sensitivity = Math.max(0.2, sensitivity - adaptability * 0.01);
            accumulatedStress *= 0.5;
        }
        
        // حفظ المعاملات المحدثة
        saveParameter("sensitivity", sensitivity);
        saveParameter("baseline_energy", baselineTrends.get("energy"));
    }
    
    /**
     * تكيف البنيات الأساسية طويلة المدى
     */
    private void adaptBaselines() {
        // التكيف البطيء للبنية الأساسية
        for (Map.Entry<String, Double> trend : baselineTrends.entrySet()) {
            String key = trend.getKey();
            double target = trend.getValue();
            double current = chemistry.getOrDefault(key, target);
            double newValue = current + (target - current) * adaptability * 0.001;
            chemistry.put(key, newValue);
        }
    }

    public EmotionalState getEmotionalState() {
        return new EmotionalState(chemistry);
    }
    
    public ConsciousMoment.BodyState getCurrentState() {
        return new ConsciousMoment.BodyState(chemistry);
    }
    
    /**
     * الحصول على مصفوفة التشابك (للمراقبة)
     */
    public float[][] getEntanglementMatrix() {
        return entanglementMatrix.clone();
    }
    
    /**
     * الحصول على مستوى الفوضى الحالي
     */
    public float getChaosLevel() {
        return chaosLevel;
    }
    
    /**
     * الحصول على المعاملات الحالية (للمراقبة والتعديل الخارجي)
     */
    public Map<String, Double> getParameters() {
        Map<String, Double> params = new HashMap<>();
        params.put("baseline_energy", baselineEnergy);
        params.put("recovery_rate", recoveryRate);
        params.put("sensitivity", sensitivity);
        params.put("adaptability", adaptability);
        params.put("experience_count", (double) experienceCount);
        return params;
    }
    
    /**
     * تعديل المعاملات يدوياً (للتخصيص)
     */
    public void setParameters(double baselineEnergy, double recoveryRate, double sensitivity, double adaptability) {
        this.baselineEnergy = baselineEnergy;
        this.recoveryRate = recoveryRate;
        this.sensitivity = sensitivity;
        this.adaptability = adaptability;
        
        saveParameter("baseline_energy", baselineEnergy);
        saveParameter("recovery_rate", recoveryRate);
        saveParameter("sensitivity", sensitivity);
        saveParameter("adaptability", adaptability);
        
        // تحديث الاتجاهات
        baselineTrends.put("energy", baselineEnergy);
    }

    // ==================== الدوال الرياضية ====================
    
    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }
    
    private double tanh(double x) {
        return Math.tanh(x);
    }
    
    private double gaussianNoise() {
        return random.nextGaussian() * NOISE_AMPLITUDE;
    }
    
    private double smoothClamp(double value) {
        // دالة clamp ناعمة باستخدام sigmoid
        return sigmoid(value * 6 - 3);
    }
    
    private void smoothClampAll() {
        for (String key : chemistry.keySet()) {
            double val = chemistry.get(key);
            chemistry.put(key, smoothClamp(val));
        }
    }
    
    private void setDerivative(String key, double value) {
        derivatives.put(key, value);
    }
    
    // ==================== حفظ الحالة ====================
    
    private void saveParameter(String key, double value) {
        if (prefs != null) {
            prefs.edit().putFloat(key, (float) value).apply();
        }
    }
    
    private void saveCurrentState() {
        if (prefs == null) return;
        
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, Double> chem : chemistry.entrySet()) {
            editor.putFloat("chem_" + chem.getKey(), chem.getValue().floatValue());
        }
        editor.putInt("experience_count", experienceCount);
        editor.putFloat("accumulated_stress", (float) accumulatedStress);
        editor.putFloat("accumulated_joy", (float) accumulatedJoy);
        editor.apply();
        
        Log.d(TAG, "Saved state with " + experienceCount + " experiences");
    }
    
    /**
     * استعادة الحالة الكيميائية المحفوظة
     */
    public void restoreState() {
        if (prefs == null) return;
        
        for (String key : chemistry.keySet()) {
            float saved = prefs.getFloat("chem_" + key, -1);
            if (saved >= 0) {
                chemistry.put(key, (double) saved);
            }
        }
        
        accumulatedStress = prefs.getFloat("accumulated_stress", 0);
        accumulatedJoy = prefs.getFloat("accumulated_joy", 0);
        
        Log.i(TAG, "Restored chemical state");
    }
    
    public void shutdown() {
        saveCurrentState();
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
    }
}

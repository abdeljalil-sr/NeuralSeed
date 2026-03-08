package com.lifeentity.core;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.lifeentity.sensors.SensoryInput;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.VisualMemory;
import com.lifeentity.imagination.ImaginationEngine;
import com.lifeentity.perception.FaceIdentitySystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * النواة الواعية - دماغ الكائن الرقمي الحي
 * تعمل في خيط خاص بها، وتدير الرغبات والمشاعر والذاكرة والخيال.
 * تنتج لحظات واعية (ConsciousMoment) وتبعثها للمراقبين.
 */
public class ConsciousnessCore {
    private static final String TAG = "ConsciousnessCore";
    private static final long CYCLE_MS = 100;          // دورة اليقظة (100ms)
    private static final long DREAM_CYCLE_MS = 5000;   // دورة الأحلام (5 ثوان) عندما يكون خاملاً
    private static final int SHORT_TERM_SIZE = 30;     // حجم الذاكرة القصيرة (عدد اللحظات)

    // الخيوط والمعالجة
    private Handler consciousnessHandler;
    private HandlerThread consciousnessThread;
    private boolean isAwake = false;
    private boolean isDreaming = false;    // هل الكائن في حالة حلم؟
    private long birthTime;
    private Random entropy;

    // المكونات الداخلية
    private HomeostasisSystem physiology;
    private ConcurrentLinkedQueue<SensoryInput> perceptualQueue;
    private List<ConsciousnessObserver> observers;

    // اللحظة الحالية والذاكرة القصيرة
    private ConsciousMoment now;
    private List<ConsciousMoment> shortTermMemory;

    // الرغبات: أسماء ديناميكية وأوزان
    private Map<String, Double> desires;
    private List<String> desireKeys;

    // روابط خارجية (يتم حقنها عبر المنشئ)
    private AppDatabase database;
    private ImaginationEngine imaginationEngine;
    private FaceIdentitySystem faceIdentity;  // اختياري

    // واجهة المراقبين
    public interface ConsciousnessObserver {
        void onConsciousMoment(ConsciousMoment moment);
        void onEmotionalShift(EmotionalState from, EmotionalState to);
        void onArticulation(String utterance, int urgency);
        void onVisualExpression(float[] latentVector, float intensity, String modality);
    }

    // ========================== المنشئون ==========================

    /**
     * منشئ يعتمد على قاعدة البيانات (يجب أن تكون مهيأة مسبقاً)
     */
    public ConsciousnessCore(AppDatabase db) {
        this.database = db;
        this.imaginationEngine = new ImaginationEngine(db.visualMemoryDao());
        // يمكن لاحقاً إضافة faceIdentity = new FaceIdentitySystem(db);

        birthTime = System.currentTimeMillis();
        perceptualQueue = new ConcurrentLinkedQueue<>();
        observers = new ArrayList<>();
        physiology = new HomeostasisSystem();
        entropy = new Random();

        now = new ConsciousMoment();
        shortTermMemory = new ArrayList<>();

        // تهيئة الرغبات الأولية (أسماء وأوزان عشوائية)
        desires = new HashMap<>();
        String[] initialDesires = {"explore", "rest", "bond", "create", "understand", "play", "reflect"};
        for (String d : initialDesires) {
            desires.put(d, 0.3 + entropy.nextDouble() * 0.7);
        }
        desireKeys = new ArrayList<>(desires.keySet());

        Log.i(TAG, "تم إنشاء النواة الواعية بالرغبات: " + desires);
    }

    // ========================== دورة الحياة ==========================

    /**
     * إيقاظ الكائن (بدء دورة الوعي)
     */
    public void awaken() {
        if (isAwake) return;
        isAwake = true;

        // إنشاء خيط مخصص للوعي
        consciousnessThread = new HandlerThread("ConsciousnessThread");
        consciousnessThread.start();
        consciousnessHandler = new Handler(consciousnessThread.getLooper());

        // تحميل الذكريات السابقة من قاعدة البيانات (اختياري)
        consciousnessHandler.post(this::loadPastMemories);

        // بدء دورة اليقظة
        consciousnessHandler.post(this::cycleConsciousness);

        Log.i(TAG, "الكائن استيقظ. العمر: " + getAge());
    }

    /**
     * تحميل آخر الذكريات من قاعدة البيانات لتغذية الذاكرة القصيرة
     */
    private void loadPastMemories() {
        // يمكن جلب آخر 10 لحظات من قاعدة البيانات وإضافتها إلى shortTermMemory
        // هذا يساعد في استمرارية الشخصية بعد إعادة التشغيل
        // (سنقوم بتنفيذها لاحقاً عند اكتمال EpisodicMemory)
    }

    /**
     * دورة اليقظة الرئيسية (تكرر كل CYCLE_MS)
     */
    private void cycleConsciousness() {
        if (!isAwake) return;

        // 1. معالجة المدخلات الحسية
        processSensoryInputs();

        // 2. تحديث الاستتباب (الكيمياء الداخلية)
        physiology.update(now.deltaTime);

        // 3. تحديث الرغبات بناءً على الحالة والذاكرة
        updateDesires();

        // 4. توليد المحتوى الواعي (التركيز، السرد، التوقع، الدافع التعبيري)
        generateConsciousContent();

        // 5. بث اللحظة للمراقبين
        broadcastMoment();

        // 6. حفظ اللحظة في الذاكرة القصيرة
        shortTermMemory.add(now.clone());
        if (shortTermMemory.size() > SHORT_TERM_SIZE) {
            shortTermMemory.remove(0);
        }

        // 7. حفظ بعض اللحظات في الذاكرة طويلة المدى (نسبة 10%)
        if (entropy.nextDouble() < 0.1) {
            saveToLongTermMemory();
        }

        // 8. جدولة الدورة التالية
        consciousnessHandler.postDelayed(this::cycleConsciousness, CYCLE_MS);
    }

    /**
     * دورة الأحلام: تعمل بتردد أبطأ عندما يكون الكائن خاملاً (يمكن استدعاؤها من خارجي)
     * أو يمكن تشغيلها في خيط منفصل عندما لا تكون هناك مدخلات.
     */
    public void startDreaming() {
        if (isDreaming) return;
        isDreaming = true;
        consciousnessHandler.postDelayed(this::dreamCycle, DREAM_CYCLE_MS);
    }

    private void dreamCycle() {
        if (!isDreaming) return;

        // أثناء الحلم، نستدعي ذكريات عشوائية وندمجها في الخيال
        VisualMemory randomMem = database.visualMemoryDao().getRandom();
        if (randomMem != null) {
            // توليد متجه كامن من الذاكرة العشوائية
            float[] latent = randomMem.latentVector;
            // إضافة ضوضاء
            for (int i = 0; i < latent.length; i++) {
                latent[i] += (entropy.nextFloat() - 0.5f) * 0.2f;
            }
            // إرسال تعبير بصري (حلم) للمراقبين
            for (ConsciousnessObserver obs : observers) {
                obs.onVisualExpression(latent, 0.5f, "dream");
            }

            // تعديل طفيف في الكيمياء (الاسترخاء)
            // physiology.modulateDream();  (يمكن إضافته لاحقاً)
        }

        // جدولة الحلم التالي
        consciousnessHandler.postDelayed(this::dreamCycle, DREAM_CYCLE_MS);
    }

    public void stopDreaming() {
        isDreaming = false;
    }

    /**
     * إدخال الكائن في النوم (إيقاف جميع الدورات)
     */
    public void sleep() {
        isAwake = false;
        isDreaming = false;
        if (consciousnessHandler != null) {
            consciousnessHandler.removeCallbacksAndMessages(null);
        }
        if (consciousnessThread != null) {
            consciousnessThread.quitSafely();
        }
        Log.i(TAG, "الكائن نام. العمر: " + getAge());
    }

    // ========================== معالجة المدخلات ==========================

    private void processSensoryInputs() {
        SensoryInput unified = new SensoryInput();

        // دمج كل المدخلات المتاحة في كائن واحد
        while (!perceptualQueue.isEmpty()) {
            SensoryInput input = perceptualQueue.poll();
            unified.merge(input);
        }

        now.perception = unified;
        now.bodyState = physiology.getCurrentState();
        physiology.modulateByPerception(unified);
    }

    /**
     * استقبال بيانات حسية من الحواس (يتم استدعاؤها من خيوط أخرى)
     */
    public void receiveSensoryData(SensoryInput input) {
        if (input != null) {
            perceptualQueue.offer(input);
            // إذا كان هناك مدخلات، نوقف الحلم (اختياري)
            if (isDreaming) stopDreaming();
        }
    }

    // ========================== إدارة الرغبات ==========================

    /**
     * تحديث أوزان الرغبات بناءً على الحالة الداخلية والذاكرة القصيرة والمدخلات
     */
    private void updateDesires() {
        double energy = now.bodyState.energy;
        double stress = now.bodyState.stress;
        double curiosity = now.bodyState.curiosity;
        double attachment = now.bodyState.attachment;

        for (String key : desireKeys) {
            double delta = entropy.nextGaussian() * 0.05; // تغيير عشوائي

            // تأثير الحالة الداخلية
            if (key.contains("explore") || key.contains("understand")) {
                delta += curiosity * 0.02;
            }
            if (key.contains("bond")) {
                delta += attachment * 0.02;
            }
            if (key.contains("rest")) {
                delta += (1 - energy) * 0.02;
            }
            if (key.contains("create") || key.contains("express")) {
                delta += stress * 0.02 + curiosity * 0.01;
            }

            // تأثير الذاكرة القصيرة (إذا كان هناك تكرار لأحداث معينة)
            if (!shortTermMemory.isEmpty()) {
                int faceCount = 0;
                for (ConsciousMoment m : shortTermMemory) {
                    if (m.perception != null && m.perception.hasHumanFace) faceCount++;
                }
                if (faceCount > 3 && key.contains("bond")) delta += 0.01;
            }

            double newValue = desires.get(key) + delta;
            desires.put(key, Math.max(0.1, Math.min(2.0, newValue)));
        }

        // احتمال ظهور رغبة جديدة (ندرة)
        if (entropy.nextDouble() < 0.01) {
            String newDesire = "desire_" + entropy.nextInt(1000);
            desires.put(newDesire, 0.5);
            desireKeys.add(newDesire);
        }
    }

    /**
     * اختيار الرغبة المسيطرة حالياً باستخدام التوزيع الاحتمالي المرجح
     */
    private String selectDominantDesire() {
        double total = 0;
        for (String key : desireKeys) {
            total += desires.get(key);
        }
        double r = entropy.nextDouble() * total;
        double cumulative = 0;
        for (String key : desireKeys) {
            cumulative += desires.get(key);
            if (r <= cumulative) {
                return key;
            }
        }
        return desireKeys.get(0); // fallback
    }

    // ========================== توليد المحتوى الواعي ==========================

    private void generateConsciousContent() {
        EmotionalState prevEmo = now.emotionalTone;
        now.emotionalTone = physiology.getEmotionalState();
        now.previousEmotion = prevEmo;

        String dominantDesire = selectDominantDesire();

        now.focus = determineAttention(dominantDesire);
        now.narrativeThread = generateNarrative(dominantDesire);
        now.anticipation = predictNearFuture();

        // توليد دافع تعبيري إذا كانت الرغبة في الإبداع كافية
        double createWeight = desires.getOrDefault("create", 0.5);
        if (entropy.nextDouble() < createWeight * 0.3) {
            now.expressiveImpulse = generateExpressiveImpulse(dominantDesire);
        } else {
            now.expressiveImpulse = null;
        }

        // إذا كان هناك تغير عاطفي كبير، نبلّغ المراقبين
        if (prevEmo != null && isSignificantShift(prevEmo, now.emotionalTone)) {
            for (ConsciousnessObserver obs : observers) {
                obs.onEmotionalShift(prevEmo, now.emotionalTone);
            }
        }
    }

    /**
     * تحديد تركيز الانتباه بناءً على الرغبة المسيطرة والمدخلات الحالية
     */
    private ConsciousMoment.AttentionFocus determineAttention(String desire) {
        if (now.perception == null) {
            return new ConsciousMoment.AttentionFocus("الداخل", "introspection");
        }

        List<String> candidates = new ArrayList<>();
        if (now.perception.hasHumanFace) candidates.add("وجه");
        if (now.perception.soundVolume > 0.5) candidates.add("صوت");
        if (now.perception.motionLevel > 0.3) candidates.add("حركة");
        if (now.perception.isTouched) candidates.add("لمس");
        if (now.perception.dominantObject != null) candidates.add(now.perception.dominantObject);

        if (!candidates.isEmpty()) {
            int idx = entropy.nextInt(candidates.size());
            return new ConsciousMoment.AttentionFocus(candidates.get(idx), desire);
        }
        return new ConsciousMoment.AttentionFocus("الداخل", "introspection");
    }

    /**
     * توليد سرد ذاتي (جملة قصيرة) مرتبط بالرغبة
     */
    private String generateNarrative(String desire) {
        String[] templates = {"أشعر بـ", "أتساءل عن", "أريد", "أرى", "أسمع", "أتذكر"};
        String selected = templates[entropy.nextInt(templates.length)];
        return selected + " " + desire;
    }

    /**
     * توقع بسيط للمستقبل القريب
     */
    private ConsciousMoment.Anticipation predictNearFuture() {
        ConsciousMoment.Anticipation ant = new ConsciousMoment.Anticipation();
        ant.predictedEvent = "لا أعرف";
        ant.probability = 0.5f + entropy.nextFloat() * 0.3f;
        ant.emotionalValence = (float) (now.emotionalTone.getDopamine() - now.emotionalTone.getCortisol());
        return ant;
    }

    /**
     * توليد دافع تعبيري (بصري بشكل رئيسي) باستخدام ImaginationEngine
     */
    private ConsciousMoment.ExpressiveImpulse generateExpressiveImpulse(String desire) {
        float intensity = (float) (desires.getOrDefault("create", 0.5) * entropy.nextDouble() * 1.5);
        if (intensity > 1) intensity = 1;

        // نطلب من ImaginationEngine توليد متجه كامن بناءً على الحالة العاطفية والرغبة
        float[] latent = imaginationEngine.generateLatentFromState(
                now.bodyState.toAffectVector(),
                now.perception,
                desire
        );

        // اختيار مفهوم عشوائي من الذاكرة (اختياري)
        String concept = imaginationEngine.getRandomConcept();

        // نحدد طريقة التعبير (بصري دائماً في هذه المرحلة)
        String modality = "visual";

        return new ConsciousMoment.ExpressiveImpulse(modality, intensity, latent, concept);
    }

    /**
     * الكشف عن تغير عاطفي كبير
     */
    private boolean isSignificantShift(EmotionalState from, EmotionalState to) {
        double diff = Math.abs(from.getArousal() - to.getArousal()) +
                Math.abs(from.getDopamine() - to.getDopamine()) +
                Math.abs(from.getCortisol() - to.getCortisol());
        return diff > 0.3;
    }

    // ========================== البث للمراقبين ==========================

    private void broadcastMoment() {
        for (ConsciousnessObserver obs : observers) {
            obs.onConsciousMoment(now.clone());

            if (now.expressiveImpulse != null && now.expressiveImpulse.intensity > 0.2f) {
                // بث الدافع التعبيري البصري
                obs.onVisualExpression(
                        now.expressiveImpulse.latentVector,
                        now.expressiveImpulse.intensity,
                        now.expressiveImpulse.modality
                );
            }

            // بث كلام عشوائي (باحتمال 30%)
            if (now.narrativeThread != null && entropy.nextDouble() < 0.3) {
                int urgency = (int) (now.emotionalTone.getIntensity() * 10);
                obs.onArticulation(now.narrativeThread, urgency);
            }
        }
    }

    // ========================== الذاكرة طويلة المدى ==========================

    /**
     * حفظ اللحظة الحالية في قاعدة البيانات (بشكل غير متزامن)
     */
    private void saveToLongTermMemory() {
        // هنا سنقوم بحفظ now في EpisodicMemory وربطها بالصورة إذا كانت موجودة
        // يمكن تنفيذها لاحقاً
    }

    // ========================== التوابع العامة ==========================

    public void addObserver(ConsciousnessObserver observer) {
        observers.add(observer);
    }

    public String getAge() {
        long age = System.currentTimeMillis() - birthTime;
        long hours = age / 3600000;
        long minutes = (age % 3600000) / 60000;
        return hours + "س " + minutes + "د";
    }

    public EmotionalState getCurrentEmotion() {
        return now.emotionalTone;
    }
}

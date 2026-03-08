package com.lifeentity.core;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.lifeentity.sensors.SensoryInput;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.VisualMemory;
import com.lifeentity.imagination.ImaginationEngine;
import com.lifeentity.imagination.VisualDream;
import com.lifeentity.perception.FaceIdentitySystem;
import com.lifeentity.perception.SceneUnderstanding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * النواة الواعية - دماغ الكائن الرقمي الحي
 */
public class ConsciousnessCore {
    private static final String TAG = "ConsciousnessCore";
    private static final long CYCLE_MS = 100;
    private static final long DREAM_CYCLE_MS = 5000;
    private static final int SHORT_TERM_SIZE = 30;
    private static final long IDLE_THRESHOLD = 10000;

    private Handler consciousnessHandler;
    private HandlerThread consciousnessThread;
    private boolean isAwake = false;
    private boolean isDreaming = false;
    private long lastInputTime;
    private long birthTime;
    private Random entropy;

    private HomeostasisSystem physiology;
    private DesireSystem desireSystem; // ⬅️ إضافة نظام الرغبات
    private ConcurrentLinkedQueue<SensoryInput> perceptualQueue;
    private List<ConsciousnessObserver> observers;

    private ConsciousMoment now;
    private List<ConsciousMoment> shortTermMemory;

    private AppDatabase database;
    private ImaginationEngine imaginationEngine;
    private VisualDream visualDream;
    private SceneUnderstanding sceneUnderstanding;
    private FaceIdentitySystem faceIdentity;

    public interface ConsciousnessObserver {
        void onConsciousMoment(ConsciousMoment moment);
        void onEmotionalShift(EmotionalState from, EmotionalState to);
        void onArticulation(String utterance, int urgency);
        void onVisualExpression(float[] latentVector, float intensity, String modality);
        void onDreamGenerated(android.graphics.Bitmap dreamImage, String description);
    }

    public ConsciousnessCore(Context context, AppDatabase db) {
        this.database = db;
        this.imaginationEngine = new ImaginationEngine(db.visualMemoryDao());
        this.visualDream = new VisualDream(db.visualMemoryDao(), imaginationEngine);
        // this.sceneUnderstanding = new SceneUnderstanding(context, db.visualMemoryDao());
        // this.faceIdentity = new FaceIdentitySystem(db.memoryDao());

        birthTime = System.currentTimeMillis();
        lastInputTime = birthTime;
        perceptualQueue = new ConcurrentLinkedQueue<>();
        observers = new ArrayList<>();
        physiology = new HomeostasisSystem();
        desireSystem = new DesireSystem(); // ⬅️ إنشاء نظام الرغبات
        entropy = new Random();

        now = new ConsciousMoment();
        shortTermMemory = new ArrayList<>();

        Log.i(TAG, "تم إنشاء النواة الواعية");
    }

    public void awaken() {
        if (isAwake) return;
        isAwake = true;

        consciousnessThread = new HandlerThread("ConsciousnessThread");
        consciousnessThread.start();
        consciousnessHandler = new Handler(consciousnessThread.getLooper());

        consciousnessHandler.post(this::loadPastMemories);
        consciousnessHandler.post(this::cycleConsciousness);

        Log.i(TAG, "الكائن استيقظ. العمر: " + getAge());
    }

    private void loadPastMemories() {}

    private void cycleConsciousness() {
        if (!isAwake) return;

        processSensoryInputs();
        physiology.update(now.deltaTime);
        desireSystem.update(now.bodyState); // ⬅️ تحديث الرغبات

        generateConsciousContent();
        broadcastMoment();

        shortTermMemory.add(now.clone());
        if (shortTermMemory.size() > SHORT_TERM_SIZE) {
            shortTermMemory.remove(0);
        }

        if (entropy.nextDouble() < 0.1) {
            saveToLongTermMemory();
        }

        consciousnessHandler.postDelayed(this::cycleConsciousness, CYCLE_MS);

        long nowTime = System.currentTimeMillis();
        if (!isDreaming && (nowTime - lastInputTime) > IDLE_THRESHOLD) {
            startDreaming();
        }
    }

    private void processSensoryInputs() {
        SensoryInput unified = new SensoryInput();
        while (!perceptualQueue.isEmpty()) {
            unified.merge(perceptualQueue.poll());
        }
        now.perception = unified;
        now.bodyState = physiology.getCurrentState();
        physiology.modulateByPerception(unified);
    }

    public void receiveSensoryData(SensoryInput input) {
        if (input != null) {
            perceptualQueue.offer(input);
            lastInputTime = System.currentTimeMillis();
            if (isDreaming) stopDreaming();
        }
    }

    private void generateConsciousContent() {
        EmotionalState prevEmo = now.emotionalTone;
        now.emotionalTone = physiology.getEmotionalState();
        now.previousEmotion = prevEmo;

        String dominantDesire = desireSystem.selectDominantDesire(); // ⬅️ استخدام نظام الرغبات

        now.focus = determineAttention(dominantDesire);
        now.narrativeThread = generateNarrative(dominantDesire);
        now.anticipation = predictNearFuture();

        double createWeight = desireSystem.getDesireStrength("create");
        if (entropy.nextDouble() < createWeight * 0.3) {
            now.expressiveImpulse = generateExpressiveImpulse(dominantDesire);
        } else {
            now.expressiveImpulse = null;
        }

        if (prevEmo != null && isSignificantShift(prevEmo, now.emotionalTone)) {
            for (ConsciousnessObserver obs : observers) {
                obs.onEmotionalShift(prevEmo, now.emotionalTone);
            }
        }
    }

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

    private String generateNarrative(String desire) {
        String[] templates = {"أشعر بـ", "أتساءل عن", "أريد", "أرى", "أسمع", "أتذكر"};
        String selected = templates[entropy.nextInt(templates.length)];
        return selected + " " + desire;
    }

    private ConsciousMoment.Anticipation predictNearFuture() {
        ConsciousMoment.Anticipation ant = new ConsciousMoment.Anticipation();
        ant.predictedEvent = "لا أعرف";
        ant.probability = 0.5f + entropy.nextFloat() * 0.3f;
        ant.emotionalValence = (float) (now.emotionalTone.getDopamine() - now.emotionalTone.getCortisol());
        return ant;
    }

    private ConsciousMoment.ExpressiveImpulse generateExpressiveImpulse(String desire) {
        float intensity = (float) (desireSystem.getDesireStrength("create") * entropy.nextDouble() * 1.5);
        if (intensity > 1) intensity = 1;

        float[] latent = imaginationEngine.generateLatentFromState(
                now.bodyState.toAffectVector(),
                now.perception,
                desire
        );

        String concept = imaginationEngine.getRandomConcept();
        String modality = "visual";

        return new ConsciousMoment.ExpressiveImpulse(modality, intensity, latent, concept);
    }

    private boolean isSignificantShift(EmotionalState from, EmotionalState to) {
        double diff = Math.abs(from.getArousal() - to.getArousal()) +
                Math.abs(from.getDopamine() - to.getDopamine()) +
                Math.abs(from.getCortisol() - to.getCortisol());
        return diff > 0.3;
    }

    private void broadcastMoment() {
        for (ConsciousnessObserver obs : observers) {
            obs.onConsciousMoment(now.clone());

            if (now.expressiveImpulse != null && now.expressiveImpulse.intensity > 0.2f) {
                obs.onVisualExpression(
                        now.expressiveImpulse.latentVector,
                        now.expressiveImpulse.intensity,
                        now.expressiveImpulse.modality
                );
            }

            if (now.narrativeThread != null && entropy.nextDouble() < 0.3) {
                int urgency = (int) (now.emotionalTone.getIntensity() * 10);
                obs.onArticulation(now.narrativeThread, urgency);
            }
        }
    }

    private void startDreaming() {
        if (isDreaming) return;
        isDreaming = true;
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation("أنا أحلم...", 3);
        }
        consciousnessHandler.post(this::dreamCycle);
    }

    private void dreamCycle() {
        if (!isDreaming) return;

        android.graphics.Bitmap dreamImage = visualDream.dreamOnce();
        if (dreamImage != null) {
            for (ConsciousnessObserver obs : observers) {
                obs.onDreamGenerated(dreamImage, "حلمت بشيء...");
            }
        }

        if (isDreaming) {
            consciousnessHandler.postDelayed(this::dreamCycle, DREAM_CYCLE_MS);
        }
    }

    public void stopDreaming() {
        if (!isDreaming) return;
        isDreaming = false;
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation("استيقظت", 2);
        }
    }

    public void sleep() {
        isAwake = false;
        stopDreaming();
        if (consciousnessHandler != null) {
            consciousnessHandler.removeCallbacksAndMessages(null);
        }
        if (consciousnessThread != null) {
            consciousnessThread.quitSafely();
        }
        Log.i(TAG, "الكائن نام. العمر: " + getAge());
    }

    private void saveToLongTermMemory() {}

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

    // ⬅️ إضافة دالة للوصول إلى الرغبة المسيطرة
    public String getDominantDesire() {
        return desireSystem.selectDominantDesire();
    }
}

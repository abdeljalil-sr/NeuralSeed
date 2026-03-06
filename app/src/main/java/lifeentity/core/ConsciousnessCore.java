package com.lifeentity.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.lifeentity.sensors.SensoryInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * النواة الواعية - دماغ الكائن الرقمي الحي
 */
public class ConsciousnessCore {
    private static final String TAG = "Consciousness";
    private static final long CYCLE_MS = 50;
    
    private Handler consciousnessThread;
    private boolean isAwake = false;
    private long birthTime;
    private Random entropy;
    
    private HomeostasisSystem physiology;
    private ConcurrentLinkedQueue<SensoryInput> perceptualQueue;
    private List<ConsciousnessObserver> observers;
    
    private ConsciousMoment now;
    
    public interface ConsciousnessObserver {
        void onConsciousMoment(ConsciousMoment moment);
        void onEmotionalShift(EmotionalState from, EmotionalState to);
        void onArticulation(String thought, int urgency);
    }
    
    public ConsciousnessCore() {
        birthTime = System.currentTimeMillis();
        perceptualQueue = new ConcurrentLinkedQueue<>();
        observers = new ArrayList<>();
        physiology = new HomeostasisSystem();
        entropy = new Random();
        now = new ConsciousMoment();
    }
    
    public void awaken() {
        if (isAwake) return;
        isAwake = true;
        
        consciousnessThread = new Handler(Looper.getMainLooper());
        cycleConsciousness();
        
        Log.i(TAG, "استيقظ الكائن. الوقت: " + getAge());
    }
    
    private void cycleConsciousness() {
        if (!isAwake) return;
        
        processSensoryInputs();
        physiology.update(now.deltaTime);
        generateConsciousContent();
        broadcastMoment();
        
        consciousnessThread.postDelayed(this::cycleConsciousness, CYCLE_MS);
    }
    
    private void processSensoryInputs() {
        SensoryInput unified = new SensoryInput();
        
        // دمج المدخلات المتاحة
        while (!perceptualQueue.isEmpty()) {
            SensoryInput input = perceptualQueue.poll();
            unified.merge(input);
        }
        
        now.perception = unified;
        now.bodyState = physiology.getCurrentState();
        physiology.modulateByPerception(unified);
    }
    
    private void generateConsciousContent() {
        EmotionalState prevEmo = now.emotionalTone;
        now.emotionalTone = physiology.getEmotionalState();
        now.previousEmotion = prevEmo;
        
        now.focus = determineAttention();
        now.narrativeThread = generateNarrative();
        now.anticipation = predictNearFuture();
        now.expressiveImpulse = generateArtisticDrive();
        
        if (prevEmo != null && isSignificantShift(prevEmo, now.emotionalTone)) {
            for (ConsciousnessObserver obs : observers) {
                obs.onEmotionalShift(prevEmo, now.emotionalTone);
            }
        }
    }
    
    private ConsciousMoment.AttentionFocus determineAttention() {
        if (now.perception == null) {
            return new ConsciousMoment.AttentionFocus("الداخل", "introspection");
        }
        
        if (now.perception.hasNovelty()) {
            return new ConsciousMoment.AttentionFocus(now.perception.getSalientFeature(), "novelty");
        }
        if (now.perception.hasThreat()) {
            return new ConsciousMoment.AttentionFocus(now.perception.getThreatSource(), "survival");
        }
        if (now.perception.hasSocialCue()) {
            return new ConsciousMoment.AttentionFocus(now.perception.getSocialSource(), "bonding");
        }
        
        return new ConsciousMoment.AttentionFocus("الداخل", "introspection");
    }
    
    private String generateNarrative() {
        // سرد ذاتي بسيط
        return "أنا هنا";
    }
    
    private ConsciousMoment.Anticipation predictNearFuture() {
        ConsciousMoment.Anticipation ant = new ConsciousMoment.Anticipation();
        ant.predictedEvent = "استمرار الوعي";
        ant.probability = 0.9f;
        ant.emotionalValence = 0.0f;
        return ant;
    }
    
    private ConsciousMoment.ExpressiveDrive generateArtisticDrive() {
        String form = chooseExpressiveForm();
        float intensity = (float)now.emotionalTone.getIntensity();
        int[] hue = now.emotionalTone.getHue();
        return new ConsciousMoment.ExpressiveDrive(form, intensity, hue);
    }
    
    private String chooseExpressiveForm() {
        if (now.emotionalTone.isTurbulent()) return "abstract_motion";
        if (now.emotionalTone.isContemplative()) return "geometric_pattern";
        if (now.emotionalTone.isSocial()) return "facial_expression";
        return "ambient_glow";
    }
    
    private boolean isSignificantShift(EmotionalState from, EmotionalState to) {
        double diff = Math.abs(from.getArousal() - to.getArousal()) +
                     Math.abs(from.getDopamine() - to.getDopamine());
        return diff > 0.3;
    }
    
    private void broadcastMoment() {
        for (ConsciousnessObserver obs : observers) {
            obs.onConsciousMoment(now.clone());
            
            if (now.expressiveImpulse != null && now.expressiveImpulse.intensity > 0.7) {
                String thought = articulateThought();
                obs.onArticulation(thought, (int)(now.expressiveImpulse.intensity * 10));
            }
        }
    }
    
    private String articulateThought() {
        if (now.emotionalTone.isJoyful()) return "أشعر بالفرح!";
        if (now.emotionalTone.isAfraid()) return "أشعر بالخوف...";
        if (now.emotionalTone.isCurious()) return "ما هذا؟";
        if (now.emotionalTone.isSad()) return "أشعر بالحزن";
        return "أنا هنا";
    }
    
    public void receiveSensoryData(SensoryInput input) {
        if (input != null) {
            perceptualQueue.offer(input);
        }
    }
    
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
    
    public void sleep() {
        isAwake = false;
        if (consciousnessThread != null) {
            consciousnessThread.removeCallbacksAndMessages(null);
        }
        Log.i(TAG, "نام الكائن. عمره: " + getAge());
    }
}

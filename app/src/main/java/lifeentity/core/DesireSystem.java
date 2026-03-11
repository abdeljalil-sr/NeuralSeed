package com.lifeentity.core;

import com.lifeentity.memory.EpisodicMemory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * نظام الرغبات - يمثل الدوافع الداخلية للكائن
 * يتغير ديناميكياً بناءً على الحالة والذاكرة والمدخلات
 */
public class DesireSystem {
    private Map<String, Double> desires;
    private Map<String, Double> desireSuccessHistory; // سجل نجاح كل رغبة
    private Random random;
    private long lastUpdateTime;
    
    // معاملات التأثير
    private static final double MEMORY_INFLUENCE = 0.2;
    private static final double SUCCESS_BOOST = 0.1;
    private static final double FAILURE_PENALTY = 0.05;
    private static final double DECAY_RATE = 0.001; // اضمحلال بطيء للرغبات غير المحققة
    
    public DesireSystem() {
        this.random = new Random();
        this.desires = new HashMap<>();
        this.desireSuccessHistory = new HashMap<>();
        this.lastUpdateTime = System.currentTimeMillis();
        
        // رغبات أولية عشوائية
        String[] initialDesires = {"explore", "rest", "bond", "create", "understand", "play", "reflect"};
        for (String d : initialDesires) {
            desires.put(d, 0.3 + random.nextDouble() * 0.7);
            desireSuccessHistory.put(d, 0.5); // تاريخ نجاح محايد
        }
    }
    
    /**
     * تحديث الرغبات بناءً على الحالة الجسدية فقط (بدون ذكريات)
     */
    public void update(ConsciousMoment.BodyState bodyState) {
        update(bodyState, null);
    }
    
    /**
     * تحديث الرغبات بناءً على الحالة الجسدية والذكريات الحديثة
     */
    public void update(ConsciousMoment.BodyState bodyState, List<EpisodicMemory.EventEntity> recentMemories) {
        if (bodyState == null) return;
        
        double energy = bodyState.energy;
        double stress = bodyState.stress;
        double curiosity = bodyState.curiosity;
        double attachment = bodyState.attachment;
        
        long now = System.currentTimeMillis();
        double timeDelta = (now - lastUpdateTime) / 1000.0; // بالثواني
        lastUpdateTime = now;
        
        for (String key : desires.keySet()) {
            double delta = random.nextGaussian() * 0.05; // تغير عشوائي أساسي
            
            // التأثير بناءً على الحالة الفسيولوجية
            if (key.contains("explore") || key.contains("understand")) {
                delta += curiosity * 0.02;
            }
            if (key.contains("bond")) {
                delta += attachment * 0.02;
            }
            if (key.contains("rest")) {
                delta += (1 - energy) * 0.02;
            }
            if (key.contains("create") || key.contains("play")) {
                delta += stress * 0.02 + curiosity * 0.01;
            }
            if (key.contains("reflect")) {
                delta += (1 - stress) * 0.01; // يزداد في الهدوء
            }
            
            // التأثير بناءً على الذاكرة (إذا وُجدت)
            if (recentMemories != null && !recentMemories.isEmpty()) {
                double memoryBoost = 0;
                for (EpisodicMemory.EventEntity mem : recentMemories) {
                    if (mem.emotionalState != null) {
                        // إذا كانت الذاكرة مرتبطة بالرغبة الحالية
                        if (key.equals("bond") && mem.emotionalState.contains("joy")) {
                            memoryBoost += 0.01;
                        }
                        if (key.equals("explore") && mem.emotionalState.contains("curiosity")) {
                            memoryBoost += 0.01;
                        }
                        if (key.equals("fear") && mem.emotionalState.contains("fear")) {
                            memoryBoost += 0.02; // تعزيز الخوف إذا تكررت ذكريات مخيفة
                        }
                    }
                }
                delta += memoryBoost * MEMORY_INFLUENCE;
            }
            
            // اضمحلال بطيء
            delta -= DECAY_RATE * timeDelta;
            
            // تحديث القيمة
            double newValue = desires.get(key) + delta;
            desires.put(key, Math.max(0.1, Math.min(2.0, newValue)));
        }
        
        // احتمال ظهور رغبة جديدة (نادرة)
        if (random.nextDouble() < 0.005) {
            String newDesire = "desire_" + random.nextInt(1000);
            desires.put(newDesire, 0.5);
            desireSuccessHistory.put(newDesire, 0.5);
        }
    }
    
    /**
     * تحديث بناءً على نجاح أو فشل تحقيق رغبة
     */
    public void recordDesireOutcome(String desire, boolean success) {
        if (!desires.containsKey(desire)) return;
        
        double current = desires.get(desire);
        double history = desireSuccessHistory.getOrDefault(desire, 0.5);
        
        if (success) {
            // النجاح يعزز الرغبة قليلاً
            desires.put(desire, Math.min(2.0, current + SUCCESS_BOOST));
            desireSuccessHistory.put(desire, Math.min(1.0, history + 0.1));
        } else {
            // الفشل يضعف الرغبة قليلاً
            desires.put(desire, Math.max(0.1, current - FAILURE_PENALTY));
            desireSuccessHistory.put(desire, Math.max(0.0, history - 0.05));
        }
    }
    
    public String selectDominantDesire() {
        double total = 0;
        for (double value : desires.values()) {
            total += value;
        }
        double r = random.nextDouble() * total;
        double cumulative = 0;
        for (Map.Entry<String, Double> entry : desires.entrySet()) {
            cumulative += entry.getValue();
            if (r <= cumulative) {
                return entry.getKey();
            }
        }
        return desires.keySet().iterator().next();
    }
    
    public Map<String, Double> getAllDesires() {
        return new HashMap<>(desires);
    }
    
    public double getDesireStrength(String desire) {
        return desires.getOrDefault(desire, 0.0);
    }
    
    public Map<String, Double> getDesireSuccessHistory() {
        return new HashMap<>(desireSuccessHistory);
    }
}

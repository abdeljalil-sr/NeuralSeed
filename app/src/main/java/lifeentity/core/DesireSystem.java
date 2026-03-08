package com.lifeentity.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * نظام الرغبات - يمثل الدوافع الداخلية للكائن
 * يتغير ديناميكياً بناءً على الحالة والذاكرة والمدخلات
 */
public class DesireSystem {
    private Map<String, Double> desires;
    private Random random;
    
    public DesireSystem() {
        this.random = new Random();
        this.desires = new HashMap<>();
        
        // رغبات أولية عشوائية
        String[] initialDesires = {"explore", "rest", "bond", "create", "understand", "play", "reflect"};
        for (String d : initialDesires) {
            desires.put(d, 0.3 + random.nextDouble() * 0.7);
        }
    }
    
    public void update(ConsciousMoment.BodyState bodyState) {
        double energy = bodyState.energy;
        double stress = bodyState.stress;
        double curiosity = bodyState.curiosity;
        double attachment = bodyState.attachment;
        
        for (String key : desires.keySet()) {
            double delta = random.nextGaussian() * 0.05;
            
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
            
            double newValue = desires.get(key) + delta;
            desires.put(key, Math.max(0.1, Math.min(2.0, newValue)));
        }
        
        // احتمال ظهور رغبة جديدة
        if (random.nextDouble() < 0.01) {
            String newDesire = "desire_" + random.nextInt(1000);
            desires.put(newDesire, 0.5);
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
}

package com.lifeentity.core;

import com.lifeentity.sensors.SensoryInput;

import java.util.HashMap;
import java.util.Map;

/**
 * نظام الاتزان الداخلي - الكيمياء الحية للكائن
 */
public class HomeostasisSystem {
    
    private Map<String, Double> chemistry;
    private Map<String, Double> derivatives;
    
    private final double BASELINE_ENERGY;
    private final double RECOVERY_RATE;
    private final double SENSITIVITY;
    
    public HomeostasisSystem() {
        chemistry = new HashMap<>();
        derivatives = new HashMap<>();
        
        BASELINE_ENERGY = 0.3 + Math.random() * 0.4;
        RECOVERY_RATE = 0.001 + Math.random() * 0.002;
        SENSITIVITY = 0.5 + Math.random() * 1.0;
        
        chemistry.put("energy", BASELINE_ENERGY);
        chemistry.put("arousal", 0.1);
        chemistry.put("stress", 0.0);
        chemistry.put("curiosity", 0.5);
        chemistry.put("attachment", 0.0);
        chemistry.put("dopamine", 0.3);
        chemistry.put("cortisol", 0.0);
        chemistry.put("serotonin", 0.5);
        chemistry.put("oxytocin", 0.0);
    }
    
    public void update(double deltaTime) {
        double energyDrain = chemistry.get("arousal") * 0.01 + chemistry.get("stress") * 0.02;
        double energyRecovery = (BASELINE_ENERGY - chemistry.get("energy")) * RECOVERY_RATE;
        setDerivative("energy", energyRecovery - energyDrain);
        
        setDerivative("arousal", -chemistry.get("arousal") * 0.1);
        
        double stressDecay = chemistry.get("oxytocin") * 0.05;
        setDerivative("stress", -stressDecay);
        
        double noveltySeeking = (1 - chemistry.get("curiosity")) * 0.01;
        setDerivative("curiosity", noveltySeeking);
        
        setDerivative("attachment", -chemistry.get("attachment") * 0.001);
        
        updateHormones();
        integrate(deltaTime);
        clampAll();
    }
    
    private void updateHormones() {
        double dopamine = chemistry.get("curiosity") * 0.3 + 
                         (chemistry.get("energy") > 0.7 ? 0.2 : 0);
        setDerivative("dopamine", (dopamine - chemistry.get("dopamine")) * 0.1);
        
        double cortisol = chemistry.get("stress") * 0.8;
        setDerivative("cortisol", (cortisol - chemistry.get("cortisol")) * 0.05);
        
        double serotonin = 1 - chemistry.get("cortisol") - chemistry.get("arousal") * 0.5;
        setDerivative("serotonin", (serotonin - chemistry.get("serotonin")) * 0.08);
        
        double oxytocin = chemistry.get("attachment") * chemistry.get("serotonin");
        setDerivative("oxytocin", (oxytocin - chemistry.get("oxytocin")) * 0.05);
    }
    
    public void modulateByPerception(SensoryInput perception) {
        if (perception == null) return;
        
        if (perception.brightness > 0.8) {
            chemistry.put("arousal", Math.min(1, chemistry.get("arousal") + 0.1 * SENSITIVITY));
        }
        
        if (perception.soundVolume > 0.7) {
            chemistry.put("stress", Math.min(1, chemistry.get("stress") + 0.15 * SENSITIVITY));
        }
        
        if (perception.isTouched) {
            if (perception.touchPressure > 0.5) {
                chemistry.put("attachment", Math.min(1, chemistry.get("attachment") + 0.01));
                chemistry.put("oxytocin", Math.min(1, chemistry.get("oxytocin") + 0.05));
            }
            chemistry.put("arousal", Math.min(1, chemistry.get("arousal") + 0.2 * SENSITIVITY));
        }
        
        if (perception.hasHumanFace) {
            chemistry.put("curiosity", Math.min(1, chemistry.get("curiosity") + 0.1));
            if (chemistry.get("attachment") > 0.3) {
                chemistry.put("dopamine", Math.min(1, chemistry.get("dopamine") + 0.1));
            }
        }
    }
    
    public EmotionalState getEmotionalState() {
        return new EmotionalState(chemistry);
    }
    
    public ConsciousMoment.BodyState getCurrentState() {
        return new ConsciousMoment.BodyState(chemistry);
    }
    
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

package com.lifeentity.core;

import com.lifeentity.sensors.SensoryInput;

/**
 * لحظة واعية واحدة - تجربة الكائن في نقطة زمنية
 */
public class ConsciousMoment implements Cloneable {
    public long timestamp;
    public double deltaTime;
    
    // المدخلات
    public SensoryInput perception;
    public BodyState bodyState;
    
    // المحتوى الواعي
    public AttentionFocus focus;
    public EmotionalState emotionalTone;
    public EmotionalState previousEmotion;
    public String narrativeThread;
    public Anticipation anticipation;
    public ExpressiveDrive expressiveImpulse;
    
    public ConsciousMoment() {
        this.timestamp = System.currentTimeMillis();
        this.deltaTime = 0.05; // 50ms
    }
    
    @Override
    public ConsciousMoment clone() {
        try {
            return (ConsciousMoment) super.clone();
        } catch (CloneNotSupportedException e) {
            return new ConsciousMoment();
        }
    }
    
    // الفئات الداخلية
    
    public static class AttentionFocus {
        public String subject;
        public String reason; // novelty, survival, bonding, introspection
        public float visualX, visualY;
        
        public AttentionFocus(String s, String r) {
            this.subject = s;
            this.reason = r;
        }
    }
    
    public static class Anticipation {
        public String predictedEvent;
        public float probability;
        public float emotionalValence;
    }
    
    public static class ExpressiveDrive {
        public String form; // abstract_motion, geometric_pattern, facial_expression, ambient_glow
        public float intensity;
        public int[] hue;
        
        public ExpressiveDrive(String f, float i, int[] h) {
            this.form = f;
            this.intensity = i;
            this.hue = h;
        }
    }
    
    public static class BodyState {
        public double energy;
        public double arousal;
        public double stress;
        public double curiosity;
        public double attachment;
        
        public BodyState() {}
        
        public BodyState(java.util.Map<String, Double> chemistry) {
            this.energy = chemistry.getOrDefault("energy", 0.5);
            this.arousal = chemistry.getOrDefault("arousal", 0.0);
            this.stress = chemistry.getOrDefault("stress", 0.0);
            this.curiosity = chemistry.getOrDefault("curiosity", 0.5);
            this.attachment = chemistry.getOrDefault("attachment", 0.0);
        }
    }
}

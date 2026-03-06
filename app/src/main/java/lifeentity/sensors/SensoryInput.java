package com.lifeentity.sensors;

/**
 * حاوية موحدة لكل المدخلات الحسية
 */
public class SensoryInput {
    // بصري
    public float brightness;
    public float motionLevel;
    public boolean hasHumanFace;
    public float faceProximity;
    public String faceId;
    public String humanExpression;
    public int objectCount;
    public String dominantObject;
    
    // سمعي
    public float soundVolume;
    public float soundPitch;
    public boolean speechDetected;
    public String recognizedSpeech;
    
    // لمسي
    public boolean isTouched;
    public float touchX, touchY;
    public float touchPressure;
    public long touchDuration;
    public String touchLocation;
    
    // حركي
    public float motionIntensity;
    public String posture;
    public boolean isShaking;
    public boolean isFalling;
    
    // اصطناعي
    public float novelty;
    public float threatLevel;
    public float socialValence;
    
    public SensoryInput() {
        this.brightness = 0.5f;
        this.novelty = 0;
    }
    
    public void merge(SensoryInput other) {
        if (other == null) return;
        
        if (other.brightness != 0.5f) this.brightness = other.brightness;
        if (other.motionLevel > this.motionLevel) this.motionLevel = other.motionLevel;
        if (other.hasHumanFace) {
            this.hasHumanFace = true;
            this.faceProximity = other.faceProximity;
            this.faceId = other.faceId;
        }
        if (other.isTouched) {
            this.isTouched = true;
            this.touchX = other.touchX;
            this.touchY = other.touchY;
            this.touchPressure = other.touchPressure;
        }
        if (other.soundVolume > this.soundVolume) {
            this.soundVolume = other.soundVolume;
            this.recognizedSpeech = other.recognizedSpeech;
        }
        if (other.motionIntensity > this.motionIntensity) {
            this.motionIntensity = other.motionIntensity;
            this.posture = other.posture;
        }
    }
    
    public boolean hasNovelty() {
        return novelty > 0.5f || motionLevel > 0.3f;
    }
    
    public boolean hasThreat() {
        return threatLevel > 0.5f || soundVolume > 0.9f;
    }
    
    public boolean hasSocialCue() {
        return hasHumanFace || socialValence > 0.5f;
    }
    
    public String getSalientFeature() {
        if (hasHumanFace) return "human_face";
        if (dominantObject != null) return dominantObject;
        return "unknown";
    }
    
    public String getThreatSource() {
        if (soundVolume > 0.9f) return "loud_sound";
        return "unknown";
    }
    
    public String getSocialSource() {
        if (hasHumanFace) return "face_" + faceId;
        return "unknown";
    }
}

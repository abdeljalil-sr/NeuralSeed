package com.neuralseed;

import java.util.ArrayList;
import java.util.List;

/**
 * فكرة في وعي الكائن
 */
public class Thought {
    
    public String id;
    public String meaning; // المعنى الداخلي
    public List<Concept> concepts; // المفاهيم المكونة
    public EmotionalTone emotionalTone;
    public double confidence;
    public double novelty; // مدى جديدها
    public double playfulness;
    public boolean isDriveGenerated;
    public boolean seeksLinguisticExpression;
    
    Object origin; // مرجع للمسار العصبي
    
    public Thought() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.concepts = new ArrayList<>();
        this.confidence = 0.5;
        this.novelty = 0.5;
        this.playfulness = 0;
        this.isDriveGenerated = false;
        this.seeksLinguisticExpression = true;
    }
    
    public boolean isProfound() {
        return confidence > 0.8 && novelty > 0.7;
    }
    
    public boolean suggestsDesire() {
        return emotionalTone != null &&
            (emotionalTone.dominant.equals("longing") ||
            emotionalTone.dominant.equals("curiosity"));
    }
    
    public String extractedDesire() {
        return concepts.isEmpty() ? "فهم_شيء" :
            "فهم_" + concepts.get(0).signature;
    }
}

package com.neuralseed;

import java.util.HashMap;
import java.util.Map;

/**
 * كلمة تعلمها الوعي
 */
public class LearnedWord {
    
    public String form;
    public Map<String, Double> emotions;
    public String learnedFromContext;
    public long learnedAt;
    
    public LearnedWord(String f, Map<String, Double> e) {
        this.form = f;
        this.emotions = e != null ? e : new HashMap<>();
        this.learnedAt = System.currentTimeMillis();
        this.learnedFromContext = "conversation";
    }
    
    public LearnedWord(String f, String emotion, double intensity) {
        this.form = f;
        this.emotions = new HashMap<>();
        this.emotions.put(emotion, intensity);
        this.learnedAt = System.currentTimeMillis();
    }
}

package com.neuralseed;

import java.util.ArrayList;
import java.util.List;

/**
 * الصورة الذهنية التي يولدها الوعي
 */
public class MentalImage {
    
    public String id;
    public long createdAt;
    public int[] palette; // 5 ألوان يختارها هو
    public List<VisualElement> elements; // الأشكال التي يريدها
    public String emotionalTheme;
    public float chaosLevel; // مستوى الفوضى في رأسه
    
    public MentalImage() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.createdAt = System.currentTimeMillis();
        this.palette = new int[5];
        this.elements = new ArrayList<>();
        this.chaosLevel = 0.5f;
    }
    
    public static class VisualElement {
        public String type; // "circle", "spiral", "burst", "line", "node"
        public float x, y; // 0.0 to 1.0
        public float size;
        public int color;
        public String animation; // "pulse", "flow", "chaos", "stable"
        public float speed;
        public float phase;
    }
    
    public boolean isExpressive() {
        return elements.size() > 0;
    }
    
    public boolean isVivid() {
        return chaosLevel > 0.6 || elements.size() > 5;
    }
    
    public boolean hasChaos() {
        return chaosLevel > 0.7;
    }
    
    public boolean hasOrder() {
        return chaosLevel < 0.3;
    }
    
    public void mutateByThought(Thought thought) {
        chaosLevel += 0.05f;
        if (chaosLevel > 1) chaosLevel = 1;
    }
}

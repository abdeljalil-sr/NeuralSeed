package com.neuralseed;

/**
 * مفهوم في شبكة الوعي
 */
public class Concept {
    
    public String signature;
    public String type;
    
    public Concept(String s, String t) {
        this.signature = s;
        this.type = t;
    }
    
    public MentalImage.VisualElement toVisual() {
        MentalImage.VisualElement e = new MentalImage.VisualElement();
        e.type = type.equals("word") ? "node" : "circle";
        e.x = (float)(Math.random());
        e.y = (float)(Math.random());
        e.size = 30;
        e.color = 0xFFFFFFFF;
        e.animation = "pulse";
        e.speed = 0.02f;
        e.phase = (float)(Math.random() * Math.PI * 2);
        return e;
    }
}

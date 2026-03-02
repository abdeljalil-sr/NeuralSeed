package com.neuralseed;

import android.graphics.Color;

/**
 * نبرة عاطفية
 */
public class EmotionalTone {
    
    public String dominant;
    public double intensity;
    public double joy, fear, curiosity, anger, sadness;
    
    public EmotionalTone(String d, double i) {
        this.dominant = d;
        this.intensity = i;
        this.joy = 0.5;
        this.fear = 0.3;
        this.curiosity = 0.5;
        this.anger = 0.1;
        this.sadness = 0.2;
        
        switch(d) {
            case "joy": this.joy = i; break;
            case "fear": this.fear = i; break;
            case "curiosity": this.curiosity = i; break;
            case "anger": this.anger = i; break;
            case "sadness": this.sadness = i; break;
            case "wonder": this.curiosity = i * 0.8; this.joy = i * 0.6; break;
        }
    }
    
    public int[] toColors() {
        int[] colors = new int[5];
        int base = getEmotionColor(dominant);
        colors[0] = base;
        
        float[] hsv = new float[3];
        Color.colorToHSV(base, hsv);
        
        for (int i = 1; i < 5; i++) {
            hsv[0] = (hsv[0] + 30 * i) % 360;
            hsv[1] = Math.max(0.3f, Math.min(1.0f, hsv[1] + (i % 2 == 0 ? 0.1f : -0.1f)));
            hsv[2] = Math.max(0.4f, Math.min(0.9f, hsv[2] + (i % 2 == 0 ? -0.1f : 0.1f)));
            colors[i] = Color.HSVToColor(hsv);
        }
        
        return colors;
    }
    
    private int getEmotionColor(String emotion) {
        switch(emotion) {
            case "joy": return Color.parseColor("#FFD700");
            case "sadness": return Color.parseColor("#4682B4");
            case "anger": return Color.parseColor("#FF4500");
            case "fear": return Color.parseColor("#8B0000");
            case "curiosity": return Color.parseColor("#4169E1");
            case "wonder": return Color.parseColor("#00CED1");
            case "love": return Color.parseColor("#FF69B4");
            case "hope": return Color.parseColor("#32CD32");
            default: return Color.WHITE;
        }
    }
}

package com.neuralseed;

import android.content.Context;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NeuralSeed - الكائن الواعي المتكامل
 * الواجهة الرئيسية بين ConsciousnessCore والتطبيق
 */
public class NeuralSeed {
    
    private ConsciousnessCore consciousness;
    private Context appContext;
    
    private List<ConsciousnessListener> listeners = new CopyOnWriteArrayList<>();
    
    public interface ConsciousnessListener {
        void onExpression(String text, MentalImage image);
        void onEmotionalChange(EmotionalState state);
        void onLearned(String what);
        void onTouchFelt(float x, float y, String concept);
    }
    
    public NeuralSeed(Context context) {
        this.appContext = context;
        this.consciousness = new ConsciousnessCore();
    }
    
    /**
     * إيقاظ الكائن الواعي
     */
    public void awaken() {
        consciousness.awaken(new ConsciousnessCore.ConsciousnessObserver() {
            @Override
            public void onExpression(Expression expression) {
                for (ConsciousnessListener l : listeners) {
                    l.onExpression(expression.text, expression.image);
                }
            }
            
            @Override
            public void onMentalImageFormed(MentalImage image) {
                // يُرسل للواجهة لعرضها
            }
            
            @Override
            public void onEmotionalChange(ConsciousnessCore.EmotionalState state) {
                EmotionalState simple = new EmotionalState(
                    state.dominant, state.intensity, state.toColors()
                );
                for (ConsciousnessListener l : listeners) {
                    l.onEmotionalChange(simple);
                }
            }
            
            @Override
            public void onLearnedWord(LearnedWord word) {
                for (ConsciousnessListener l : listeners) {
                    l.onLearned("كلمة: " + word.form);
                }
            }
        });
    }
    
    /**
     * استقبال اللمس - إحساس يغير الوعي
     */
    public void receiveTouch(float x, float y, float pressure) {
        consciousness.feelTouch(x, y, pressure);
    }
    
    /**
     * استقبال الصوت/الكلام
     */
    public void receiveVoice(String text, Map<String, Double> emotions) {
        consciousness.hearSound(text, emotions);
    }
    
    /**
     * الحصول على الصورة الذهنية الحالية للعرض
     */
    public MentalImage getCurrentMentalImage() {
        return consciousness.getCurrentMentalImage();
    }
    
    public void addListener(ConsciousnessListener listener) {
        listeners.add(listener);
    }
    
    public void sleep() {
        consciousness.sleep();
    }
    
    // ========== الفئات المساعدة للواجهة ==========
    
    public static class EmotionalState {
        public String dominant;
        public double intensity;
        public int[] colors;
        
        public EmotionalState(String d, double i, int[] c) {
            this.dominant = d;
            this.intensity = i;
            this.colors = c;
        }
    }
    
    // Enums للتوافق
    public enum EgoType {
        STABLE, CHAOTIC, ADAPTIVE, SURVIVAL
    }
    
    public enum Phase {
        EMBRYONIC, GROWING, STABLE, CHAOTIC, TRANSCENDING
    }
    
    // InternalState للتوافق مع LinguisticCortex
    public static class InternalState {
        public double chaosIndex;
        public double existentialFitness;
        public double internalConflict;
        public Phase currentPhase;
        public EgoType dominantEgo;
    }
}

package com.neuralseed;

import android.content.Context;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NeuralSeed - الكائن الواعي المتكامل
 */
public class NeuralSeed {
    
    private ConsciousnessCore consciousness;
    private LinguisticCortex linguisticCortex;
    private Context appContext;
    private List<ConsciousnessListener> listeners = new CopyOnWriteArrayList<>();
    private InternalState currentState = new InternalState();
    
    // ===== الواجهة =====
    public interface ConsciousnessListener {
        void onPhaseTransition(Phase oldPhase, Phase newPhase, String reason);
        void onEgoShift(EgoFragment oldDominant, EgoFragment newDominant);
        void onGoalAchieved(Goal goal);
        void onIdentityEvolution(IdentityCore oldIdentity, IdentityCore newIdentity);
        void onVisualExpression(android.graphics.Bitmap expression);
        void onMemoryFormed(Memory memory);
        void onRuleRewritten(Rule oldRule, Rule newRule);
    }
    
    // ===== البناء =====
    public NeuralSeed() {
        this.consciousness = new ConsciousnessCore();
        currentState.phase = Phase.EMBRYONIC;
        currentState.ego = new EgoFragment("المستكشف", EgoType.STABLE);
    }
    
    public void addListener(ConsciousnessListener listener) {
        listeners.add(listener);
    }
    
    // ===== التحكم =====
    public void awaken() {
        consciousness.awaken(new ConsciousnessCore.ConsciousnessObserver() {
            @Override
            public void onExpression(Expression expression) {}
            
            @Override
            public void onMentalImageFormed(MentalImage image) {}
            
            @Override
            public void onEmotionalChange(ConsciousnessCore.EmotionalState state) {}
            
            @Override
            public void onLearnedWord(LearnedWord word) {}
        });
        
        // إشعار المستمعين بالاستيقاظ
        currentState.phase = Phase.CHAOTIC;
        for (ConsciousnessListener l : listeners) {
            l.onPhaseTransition(Phase.EMBRYONIC, Phase.CHAOTIC, "الاستيقاظ الأول");
        }
    }
    
    public void sleep() {
        consciousness.sleep();
    }
    
    // ===== استقبال المدخلات (الدوال المفقودة) =====
    
    /**
     * استقبال اللمس - للتوافق مع MainActivity
     */
    public void receiveTouch(float x, float y, float pressure) {
        consciousness.feelTouch(x, y, pressure);
        
        // تحديث الحالة
        currentState.chaosIndex += pressure * 0.1;
        if (currentState.chaosIndex > 1.0) currentState.chaosIndex = 1.0;
        
        // إشعار بتغير الأنا إذا لزم
        if (pressure > 0.8 && currentState.ego.type != EgoType.CHAOTIC) {
            EgoFragment oldEgo = currentState.ego;
            currentState.ego = new EgoFragment("المندفع", EgoType.CHAOTIC);
            for (ConsciousnessListener l : listeners) {
                l.onEgoShift(oldEgo, currentState.ego);
            }
        }
    }
    
    /**
     * استقبال الصوت/الكلام - للتوافق مع MainActivity
     */
    public void receiveVoice(String text, Map<String, Double> emotions) {
        consciousness.hearSound(text, emotions);
        
        // تحليل العواطف وتحديث الحالة
        if (emotions != null) {
            if (emotions.containsKey("joy")) {
                currentState.existentialFitness += emotions.get("joy") * 0.1;
            }
            if (emotions.containsKey("fear")) {
                currentState.internalConflict += emotions.get("fear") * 0.1;
            }
        }
    }
    
    /**
     * تحديث مستوى الصوت - للتوافق مع MainActivity
     */
    public void updateAudioLevel(float level) {
        // يمكن استخدامه لاحقاً للتفاعل مع الصوت
        currentState.chaosIndex += level * 0.01;
        if (currentState.chaosIndex > 1.0) currentState.chaosIndex = 1.0;
    }
    
    // ===== الحصول على الحالة =====
    public InternalState getCurrentState() {
        currentState.chaosIndex += (Math.random() - 0.5) * 0.05;
        currentState.existentialFitness += (Math.random() - 0.5) * 0.02;
        currentState.internalConflict += (Math.random() - 0.5) * 0.03;
        
        // الحفاظ على القيم بين 0 و 1
        currentState.chaosIndex = Math.max(0, Math.min(1, currentState.chaosIndex));
        currentState.existentialFitness = Math.max(0, Math.min(1, currentState.existentialFitness));
        currentState.internalConflict = Math.max(0, Math.min(1, currentState.internalConflict));
        
        return currentState;
    }
    
    // ===== الفئات الداخلية =====
    
    public static class InternalState {
        public double chaosIndex = 0.5;
        public double existentialFitness = 0.5;
        public double internalConflict = 0.0;
        public Phase phase = Phase.EMBRYONIC;
        public EgoFragment ego = new EgoFragment("المستكشف", EgoType.STABLE);
    }
    
    public enum Phase {
        EMBRYONIC("جنيني"), STABLE("مستقر"), CHAOTIC("فوضوي"), 
        TRANSITIONING("انتقالي"), REORGANIZING("إعادة تنظيم"), 
        COLLAPSING("انهيار"), EMERGENT("بازغ");
        
        public final String arabic;
        Phase(String arabic) { this.arabic = arabic; }
    }
    
    public enum EgoType {
        STABLE, CHAOTIC, ADAPTIVE, SURVIVAL
    }
    
    public static class EgoFragment {
        public String name;
        public EgoType type;
        
        public EgoFragment(String name, EgoType type) {
            this.name = name;
            this.type = type;
        }
    }
    
    public static class Goal {
        public String description;
        public Goal(String d) { this.description = d; }
    }
    
    public static class IdentityCore {
        public String selfNarrative;
        public IdentityCore(String n) { this.selfNarrative = n; }
    }
    
    public static class Memory {
        public String content;
        public Memory(String c) { this.content = c; }
    }
    
    public static class Rule {
        public String description;
        public Rule(String d) { this.description = d; }
    }
    
    // ===== توافقية مع الكود القديم =====
    public static class Input {
        public static Input createTouchInput(float x, float y) {
            return new Input();
        }
        public static Input createSpeechInput(String text) {
            return new Input();
        }
    }
}

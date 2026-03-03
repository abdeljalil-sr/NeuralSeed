package com.neuralseed;

import android.content.Context;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NeuralSeed - الكائن الواعي المتكامل
 * نسخة موحدة ومتوافقة مع جميع الملفات
 */
public class NeuralSeed {
    
    // ===== النواحي المختلفة للوعي =====
    private ConsciousnessCore consciousness;
    private Context appContext;
    private List<ConsciousnessListener> listeners = new CopyOnWriteArrayList<>();
    
    // ===== الحالة الداخلية =====
    private InternalState currentState = new InternalState();
    
    // ===== الواجهة - للتوافق مع MainActivity =====
    public interface ConsciousnessListener {
        // للتوافق مع MainActivity الجديد
        void onPhaseTransition(Phase oldPhase, Phase newPhase, String reason);
        void onEgoShift(EgoFragment oldDominant, EgoFragment newDominant);
        void onGoalAchieved(Goal goal);
        void onIdentityEvolution(IdentityCore oldIdentity, IdentityCore newIdentity);
        void onVisualExpression(android.graphics.Bitmap expression);
        void onMemoryFormed(Memory memory);
        void onRuleRewritten(Rule oldRule, Rule newRule);
        
        // للتوافق مع MainActivity القديم (إذا لزم)
        void onExpression(String text, MentalImage image);
        void onEmotionalChange(EmotionalState state);
        void onLearned(String what);
        void onTouchFelt(float x, float y, String concept);
        void onVisualThought(PulseView.VisualThought thought);
    }
    
    // ===== البناء =====
    public NeuralSeed() {
        this.consciousness = new ConsciousnessCore();
        initializeState();
    }
    
    public NeuralSeed(Context context) {
        this.appContext = context;
        this.consciousness = new ConsciousnessCore();
        initializeState();
    }
    
    private void initializeState() {
        currentState.phase = Phase.EMBRYONIC;
        currentState.currentPhase = Phase.EMBRYONIC; // للتوافق
        currentState.ego = new EgoFragment("المستكشف", EgoType.STABLE);
        currentState.dominantEgo = "المستكشف"; // للتوافق
        currentState.chaosIndex = 0.5;
        currentState.existentialFitness = 0.5;
        currentState.internalConflict = 0.0;
        currentState.learningLevel = "0%";
    }
    
    public void addListener(ConsciousnessListener listener) {
        listeners.add(listener);
    }
    
    // ===== التحكم =====
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
                for (ConsciousnessListener l : listeners) {
                    l.onVisualExpression(null);
                    l.onVisualThought(convertToPulseVisualThought(image));
                }
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
        
        // إشعار بالاستيقاظ
        Phase oldPhase = currentState.phase;
        currentState.phase = Phase.CHAOTIC;
        currentState.currentPhase = Phase.CHAOTIC;
        for (ConsciousnessListener l : listeners) {
            l.onPhaseTransition(oldPhase, Phase.CHAOTIC, "الاستيقاظ الأول");
        }
    }
    
    public void sleep() {
        consciousness.sleep();
    }
    
    // ===== استقبال المدخلات - للتوافق مع MainActivity =====
    
    public void receiveTouch(float x, float y, float pressure) {
        // استدعاء ConsciousnessCore
        consciousness.feelTouch(x, y, pressure);
        
        // تحديث الحالة
        currentState.chaosIndex += pressure * 0.1;
        if (currentState.chaosIndex > 1.0) currentState.chaosIndex = 1.0;
        
        // إشعار بتغير الأنا إذا لزم
        if (pressure > 0.8 && currentState.ego.type != EgoType.CHAOTIC) {
            EgoFragment oldEgo = currentState.ego;
            currentState.ego = new EgoFragment("المندفع", EgoType.CHAOTIC);
            currentState.dominantEgo = currentState.ego.name;
            for (ConsciousnessListener l : listeners) {
                l.onEgoShift(oldEgo, currentState.ego);
            }
        }
        
        // إشعار اللمس
        for (ConsciousnessListener l : listeners) {
            l.onTouchFelt(x, y, "موقع_لمس_" + (int)x + "_" + (int)y);
        }
    }
    
    public void receiveVoice(String text, Map<String, Double> emotions) {
        // استدعاء ConsciousnessCore
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
    
    public void updateAudioLevel(float level) {
        currentState.chaosIndex += level * 0.01;
        if (currentState.chaosIndex > 1.0) currentState.chaosIndex = 1.0;
    }
    
    // ===== للتوافق مع الكود القديم =====
    public void receiveInput(Input input) {
        // فارغ للتوافق
    }
    
    public static class Input {
        public static Input createTouchInput(float x, float y) {
            return new Input();
        }
        public static Input createSpeechInput(String text) {
            return new Input();
        }
    }
    
    // ===== الحصول على الحالة - للتوافق مع LinguisticCortex =====
    public InternalState getCurrentState() {
        // تحديث عشوائي للقيم
        currentState.chaosIndex += (Math.random() - 0.5) * 0.05;
        currentState.existentialFitness += (Math.random() - 0.5) * 0.02;
        currentState.internalConflict += (Math.random() - 0.5) * 0.03;
        
        // الحفاظ على القيم بين 0 و 1
        currentState.chaosIndex = Math.max(0, Math.min(1, currentState.chaosIndex));
        currentState.existentialFitness = Math.max(0, Math.min(1, currentState.existentialFitness));
        currentState.internalConflict = Math.max(0, Math.min(1, currentState.internalConflict));
        
        return currentState;
    }
    
    public MentalImage getCurrentMentalImage() {
        return consciousness.getCurrentMentalImage();
    }
    
    // ===== محول للتوافق =====
    private PulseView.VisualThought convertToPulseVisualThought(MentalImage image) {
        PulseView.VisualThought thought = new PulseView.VisualThought("تخيل من الوعي");
        thought.chaosLevel = image.chaosLevel;
        thought.colorPalette = image.palette;
        thought.emotionalTheme = image.emotionalTheme;
        // تحويل العناصر البصرية إذا لزم
        return thought;
    }
    
    // ===== الفئات الداخلية - موحدة =====
    
    public static class InternalState {
        // للتوافق مع NeuralSeed
        public double chaosIndex;
        public double existentialFitness;
        public double internalConflict;
        public Phase phase;
        public EgoFragment ego;
        public String learningLevel;
        
        // للتوافق مع LinguisticCortex (أسماء بديلة)
        public Phase currentPhase;    // ← نفس phase لكن باسم مختلف
        public String dominantEgo;    // ← ego.name
    }
    
    // للتوافق مع PulseView و MainActivity
    public enum EgoType {
        STABLE, CHAOTIC, ADAPTIVE, SURVIVAL
    }
    
    public enum Phase {
        EMBRYONIC("جنيني"), 
        STABLE("مستقر"), 
        CHAOTIC("فوضوي"), 
        TRANSITIONING("انتقالي"), 
        REORGANIZING("إعادة تنظيم"), 
        COLLAPSING("انهيار"), 
        EMERGENT("بازغ");
        
        public final String arabic;
        Phase(String arabic) { this.arabic = arabic; }
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
    
    // للتوافق مع MainActivity القديم
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
}

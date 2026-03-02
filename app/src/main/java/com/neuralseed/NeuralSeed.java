package com.neuralseed;

import android.content.Context;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NeuralSeed - الكائن الواعي المتكامل
 * يدمج بين ConsciousnessCore (الوعي الحي) و LinguisticCortex (الدماغ اللغوي)
 */
public class NeuralSeed {
    
    // ===== النواحي المختلفة للوعي =====
    private ConsciousnessCore consciousness;      // الوعي الحقيقي (الحياة الداخلية)
    private LinguisticCortex linguisticCortex;    // الدماغ اللغوي (الفهم والتعلم)
    private Context appContext;
    
    // ===== المستمعون =====
    private List<ConsciousnessListener> listeners = new CopyOnWriteArrayList<>();
    
    public interface ConsciousnessListener {
        void onExpression(String text, MentalImage image);
        void onEmotionalChange(EmotionalState state);
        void onLearned(String what);
        void onTouchFelt(float x, float y, String concept);
        void onVisualThought(PulseView.VisualThought thought);
    }
    
    // ===== البناء =====
    public NeuralSeed(Context context) {
        this.appContext = context;
        this.consciousness = new ConsciousnessCore();
        this.linguisticCortex = new LinguisticCortex();
        this.linguisticCortex.initialize(context);
        
        // ربط الدماغين معاً
        linkConsciousnessAndLinguistic();
    }
    
    /**
     * ربط الوعي الحي بالدماغ اللغوي
     */
    private void linkConsciousnessAndLinguistic() {
        // عندما يولد LinguisticCortex تخيلاً بصرياً، أرسله للواجهة
        linguisticCortex.setVisualListener(new LinguisticCortex.VisualImaginationListener() {
            @Override
            public void onVisualThought(LinguisticCortex.VisualThought thought) {
                // تحويل إلى PulseView.VisualThought
                PulseView.VisualThought visualThought = convertToPulseVisualThought(thought);
                
                for (ConsciousnessListener l : listeners) {
                    l.onVisualThought(visualThought);
                }
                
                // أيضاً أرسل للوعي الحي ليؤثر على عواطفه
                consciousness.influenceByVisualThought(thought.description, thought.chaosLevel);
            }
        });
        
        // عندما يتعلم LinguisticCortex كلمة جديدة، أخبر الوعي الحي
        linguisticCortex.setListener(new LinguisticCortex.LinguisticListener() {
            @Override
            public void onWordLearned(String word, String meaning, String context) {
                // أضف للوعي الحي أيضاً
                consciousness.learnWord(word, meaning);
                for (ConsciousnessListener l : listeners) {
                    l.onLearned("كلمة: " + word + " = " + meaning);
                }
            }
            
            @Override
            public void onSentenceCorrected(String original, String corrected) {
                consciousness.registerCorrection(original, corrected);
            }
            
            @Override
            public void onEmotionDetected(String emotion, double intensity) {
                consciousness.influenceEmotion(emotion, intensity);
            }
            
            @Override
            public void onNewConceptLearned(String concept, String definition) {
                for (ConsciousnessListener l : listeners) {
                    l.onLearned("مفهوم: " + concept);
                }
            }
            
            @Override
            public void onRelationshipLearned(String subject, String relationship, String object) {
                consciousness.learnRelationship(subject, relationship, object);
            }
            
            @Override
            public void onThoughtFormed(String thought, String type) {
                // أفكار LinguisticCortex تغذي ConsciousnessCore
                consciousness.feedExternalThought(thought, type);
            }
            
            @Override
            public void onImaginationCreated(String description, int[] colors) {
                // تم معالجته في VisualListener
            }
            
            @Override
            public void onContextAnalyzed(String context, double complexity) {
                consciousness.setContext(context, complexity);
            }
        });
    }
    
    /**
     * تحويل VisualThought من LinguisticCortex إلى PulseView
     */
    private PulseView.VisualThought convertToPulseVisualThought(LinguisticCortex.VisualThought thought) {
        PulseView.VisualThought result = new PulseView.VisualThought(thought.description);
        result.chaosLevel = thought.chaosLevel;
        result.emotionalTheme = thought.emotionalTheme;
        result.colorPalette = thought.colorPalette;
        
        for (LinguisticCortex.ShapeElement elem : thought.shapes) {
            PulseView.ShapeElement se = new PulseView.ShapeElement();
            se.type = elem.type;
            se.x = elem.x;
            se.y = elem.y;
            se.size = elem.size;
            se.color = elem.color;
            se.animationSpeed = elem.animationSpeed;
            se.phase = elem.phase;
            result.shapes.add(se);
        }
        
        return result;
    }
    
    /**
     * إيقاظ الكائن الواعي
     */
    public void awaken() {
        // إيقاظ الوعي الحي
        consciousness.awaken(new ConsciousnessCore.ConsciousnessObserver() {
            @Override
            public void onExpression(Expression expression) {
                // إذا كان الوعي الحي عبر، استخدم LinguisticCortex لتحسين التعبير
                if (expression.text != null && !expression.text.isEmpty()) {
                    String enhanced = linguisticCortex.enhanceExpression(expression.text);
                    
                    for (ConsciousnessListener l : listeners) {
                        l.onExpression(enhanced, expression.image);
                    }
                } else {
                    for (ConsciousnessListener l : listeners) {
                        l.onExpression(null, expression.image);
                    }
                }
            }
            
            @Override
            public void onMentalImageFormed(MentalImage image) {
                // دمج الصورة مع تخيل LinguisticCortex
                PulseView.VisualThought visual = createVisualFromMentalImage(image);
                for (ConsciousnessListener l : listeners) {
                    l.onVisualThought(visual);
                }
            }
            
            @Override
            public void onEmotionalChange(ConsciousnessCore.EmotionalState state) {
                // تحديث LinguisticCortex بالحالة العاطفية
                Map<String, Double> emotions = new HashMap<>();
                emotions.put("joy", state.joy);
                emotions.put("fear", state.fear);
                emotions.put("curiosity", state.curiosity);
                emotions.put("anger", state.anger);
                emotions.put("sadness", state.sadness);
                
                linguisticCortex.updateEmotionalState(emotions);
                
                // إرسال للواجهة
                EmotionalState simple = new EmotionalState(
                    state.dominant, state.intensity, state.toColors()
                );
                for (ConsciousnessListener l : listeners) {
                    l.onEmotionalChange(simple);
                }
            }
            
            @Override
            public void onLearnedWord(LearnedWord word) {
                // إضافة للمعجم اللغوي أيضاً
                linguisticCortex.learnWordFromConsciousness(word.form, word.emotions);
                
                for (ConsciousnessListener l : listeners) {
                    l.onLearned("كلمة: " + word.form);
                }
            }
        });
        
        // بدء التفكير المستمر في LinguisticCortex
        linguisticCortex.startContinuousReflection();
    }
    
    /**
     * استقبال اللمس - يؤثر على الوعي والدماغ معاً
     */
    public void receiveTouch(float x, float y, float pressure) {
        // إرسال للوعي الحي
        consciousness.feelTouch(x, y, pressure);
        
        // إرسال للدماغ اللغوي للتحليل
        String concept = linguisticCortex.analyzeTouchLocation(x, y);
        
        for (ConsciousnessListener l : listeners) {
            l.onTouchFelt(x, y, concept);
        }
    }
    
    /**
     * استقبال الصوت/الكلام - المعالجة الكاملة
     */
    public void receiveVoice(String text, Map<String, Double> emotions) {
        // 1. إرسال للوعي الحي مباشرة
        consciousness.hearSound(text, emotions);
        
        // 2. معالجة عميقة بالدماغ اللغوي
        LinguisticCortex.ProcessedResult result = linguisticCortex.processInput(text);
        
        // 3. توليد رد ذكي
        InternalState state = getCurrentState();
        LinguisticCortex.GeneratedResponse response = linguisticCortex.generateResponse(text, state);
        
        // 4. إذا كان الرد قوياً، عبر به
        if (response.confidence > 0.5 && response.text != null) {
            // تحويل الرد إلى تعبير وعي
            consciousness.expressExternalThought(response.text, response.underlyingThought);
        }
        
        // 5. تعلم من المحادثة
        linguisticCortex.learnSentence(text, state);
    }
    
    /**
     * الحصول على الحالة الداخلية الحالية
     */
    public InternalState getCurrentState() {
        InternalState state = new InternalState();
        
        // من الوعي الحي
        ConsciousnessCore.EmotionalState emotional = consciousness.getEmotionalState();
        state.chaosIndex = emotional.intensity;
        state.dominantEmotion = emotional.dominant;
        
        // من الدماغ اللغوي
        Map<String, Object> stats = linguisticCortex.getStatistics();
        state.learningLevel = (String) stats.getOrDefault("learning_level", "0%");
        
        return state;
    }
    
    /**
     * الحصول على الصورة الذهنية الحالية
     */
    public MentalImage getCurrentMentalImage() {
        return consciousness.getCurrentMentalImage();
    }
    
    /**
     * الحصول على التخيل البصري الحالي
     */
    public PulseView.VisualThought getCurrentVisualThought() {
        LinguisticCortex.VisualThought thought = linguisticCortex.getCurrentVisualThought();
        return thought != null ? convertToPulseVisualThought(thought) : null;
    }
    
    public void addListener(ConsciousnessListener listener) {
        listeners.add(listener);
    }
    
    public void sleep() {
        consciousness.sleep();
        linguisticCortex.shutdown();
    }
    
    // ===== مساعدات =====
    
    private PulseView.VisualThought createVisualFromMentalImage(MentalImage image) {
        PulseView.VisualThought thought = new PulseView.VisualThought("تخيل من الوعي الحي");
        thought.chaosLevel = image.chaosLevel;
        thought.colorPalette = image.palette;
        thought.emotionalTheme = image.emotionalTheme;
        
        for (MentalImage.VisualElement elem : image.elements) {
            PulseView.ShapeElement se = new PulseView.ShapeElement();
            se.type = elem.type;
            se.x = elem.x;
            se.y = elem.y;
            se.size = elem.size;
            se.color = elem.color;
            se.animationSpeed = elem.speed;
            se.phase = elem.phase;
            thought.shapes.add(se);
        }
        
        return thought;
    }
    
    // ===== الفئات المساعدة =====
    
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
    
    public static class InternalState {
        public double chaosIndex;
        public double existentialFitness;
        public double internalConflict;
        public String currentPhase;
        public String dominantEgo;
        public String dominantEmotion;
        public String learningLevel;
    }
    
    public enum EgoType {
        STABLE, CHAOTIC, ADAPTIVE, SURVIVAL
    }
    
    public enum Phase {
        EMBRYONIC, GROWING, STABLE, CHAOTIC, TRANSCENDING
    }
}

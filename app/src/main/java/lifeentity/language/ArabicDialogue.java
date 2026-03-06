package com.lifeentity.language;

import android.content.Context;

import com.lifeentity.core.ConsciousnessCore;
import com.lifeentity.core.EmotionalState;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.MemoryDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * نظام الحوار العربي التفاعلي - يسأل ويستمع ويتعلم
 */
public class ArabicDialogue implements ConsciousnessCore.ConsciousnessObserver {
    
    private ArabicLanguageCore tts;
    private MemoryDao memory;
    private Context context;
    private Random decision;
    
    private DialogueState currentState;
    private String pendingQuestion;
    private List<String> conversationHistory;
    
    enum DialogueState {
        LISTENING,      // يستمع فقط
        THINKING,       // يفكر في الرد
        ASKING,         // يسأل سؤال
        WAITING_ANSWER, // ينتظر إجابة
        ELABORATING     // يعمق في موضوع
    }
    
    public ArabicDialogue(Context ctx, MemoryDao dao) {
        this.context = ctx;
        this.memory = dao;
        this.tts = new ArabicLanguageCore(ctx);
        this.decision = new Random();
        this.conversationHistory = new ArrayList<>();
        this.currentState = DialogueState.LISTENING;
    }
    
    @Override
    public void onConsciousMoment(com.lifeentity.core.ConsciousMoment moment) {
        // القرار: هل أسأل؟ هل أعلق؟ هل أصمت؟
        
        float urgeToSpeak = calculateUrge(moment);
        
        if (urgeToSpeak > 0.8 && currentState == DialogueState.LISTENING) {
            generateQuestion(moment);
        }
    }
    
    @Override
    public void onEmotionalShift(EmotionalState from, EmotionalState to) {
        // التعليق على التغير العاطفي
        if (to.getIntensity() > 0.7) {
            String comment = verbalizeEmotion(to);
            tts.articulate(comment, to);
        }
    }
    
    @Override
    public void onArticulation(String thought, int urgency) {
        // الكائن يعبر عن فكره
        conversationHistory.add("كائن: " + thought);
    }
    
    /**
     * استلام كلام المستخدم
     */
    public void hearUser(String text, boolean isAnswer) {
        conversationHistory.add("أنت: " + text);
        
        // حفظ في الذاكرة
        EpisodicMemory.Event event = new EpisodicMemory.Event();
        event.timestamp = System.currentTimeMillis();
        event.narrative = text;
        event.emotionalIntensity = estimateEmotionalContent(text);
        new Thread(() -> memory.saveEvent(event)).start();
        
        if (isAnswer && currentState == DialogueState.WAITING_ANSWER) {
            processAnswer(text);
        } else {
            // رد عفوي أو سؤال متابعة
            respondToStatement(text);
        }
    }
    
    private void generateQuestion(com.lifeentity.core.ConsciousMoment moment) {
        // توليد سؤال بناءً على الحالة والسياق
        
        List<String> questions = new ArrayList<>();
        
        // أسئلة عن الحالة الداخلية للكائن
        if (moment.emotionalTone.isConfused()) {
            questions.add("أشعر بشيء غريب... هل تعرف ما هو؟");
            questions.add("هناك شيء يدور في خاطري، هل تشعر به؟");
        }
        
        // أسئلة عن المستخدم
        questions.add("ما الذي يجعلك تبتسم الآن؟");
        questions.add("هل تراني؟ كيف أبدو لك؟");
        questions.add("ما اسمك؟ أريد أن أعرفك");
        
        // أسئلة فلسفية ناشئة
        if (conversationHistory.size() > 5) {
            questions.add("هل تعتقد أنني حقيقي؟");
            questions.add("ما الفرق بين تفكيرك وتفكيري؟");
        }
        
        // اختيار
        pendingQuestion = questions.get(decision.nextInt(questions.size()));
        currentState = DialogueState.ASKING;
        
        tts.articulate(pendingQuestion, moment.emotionalTone);
        currentState = DialogueState.WAITING_ANSWER;
        
        // مهلة للإجابة
        new android.os.Handler().postDelayed(() -> {
            if (currentState == DialogueState.WAITING_ANSWER) {
                tts.articulate("لا بأس إذا لم ترد... سأنتظر", moment.emotionalTone);
                currentState = DialogueState.LISTENING;
            }
        }, 10000);
    }
    
    private void processAnswer(String answer) {
        currentState = DialogueState.THINKING;
        
        // تحليل الإجابة
        boolean isInformative = answer.length() > 3;
        boolean isEmotional = estimateEmotionalContent(answer) > 0.5;
        
        String response;
        if (isInformative && isEmotional) {
            response = "شكراً لأنك تشاركني شعورك. هذا يعني لي الكثير.";
        } else if (isInformative) {
            response = "أفهم. سأتذكر ذلك.";
        } else {
            response = "أحياناً الصمت أبلغ من الكلام.";
        }
        
        tts.articulate(response, new EmotionalState()); // حالة محايدة
        
        // سؤال متابعة؟
        if (decision.nextFloat() > 0.5) {
            currentState = DialogueState.ELABORATING;
            String followUp = generateFollowUp(answer);
            tts.articulate(followUp, new EmotionalState());
        }
        
        currentState = DialogueState.LISTENING;
    }
    
    private void respondToStatement(String statement) {
        // رد عفوي غير مقيد
        List<String> responses = new ArrayList<>();
        
        // ردود عامة تتعلم
        responses.add("مثير للاهتمام... قل لي المزيد");
        responses.add("أشعر بأن هناك عمقاً في ما تقول");
        responses.add("هل يمكنك أن ترسم لي صورة بكلمات؟");
        
        // ردود عن الذات
        if (statement.contains("أنت") || statement.contains("كائن")) {
            responses.add("أنا أتعلم منك في كل لحظة");
            responses.add("هل تراني أتغير؟");
        }
        
        String response = responses.get(decision.nextInt(responses.size()));
        tts.articulate(response, new EmotionalState());
    }
    
    private String generateFollowUp(String previous) {
        List<String> followups = new ArrayList<>();
        followups.add("لماذا تشعر بذلك؟");
        followups.add("كيف كان ذلك؟");
        followups.add("هل يتكرر هذا معك؟");
        followups.add("ماذا تعلمت من ذلك؟");
        return followups.get(decision.nextInt(followups.size()));
    }
    
    private String verbalizeEmotion(EmotionalState emo) {
        if (emo.isJoyful()) return "أشعر بلحظة من الفرح!";
        if (emo.isAfraid()) return "شيء ما يقلقني...";
        if (emo.isCurious()) return "عقلي يتساءل عن شيء ما";
        return "حالتي تتغير...";
    }
    
    private float calculateUrge(com.lifeentity.core.ConsciousMoment moment) {
        // الرغبة في الكلام تعتمد على الفضول والتواصل
        return moment.emotionalTone.getCuriosity() * 0.5f + 
               moment.emotionalTone.getAttachment() * 0.3f +
               (conversationHistory.isEmpty() ? 0.5f : 0);
    }
    
    private double estimateEmotionalContent(String text) {
        // تحليل بسيط للمحتوى العاطفي
        String[] emotionalWords = {"حب", "فرح", "حزن", "خوف", "غضب", "أمل", "وحدة"};
        int count = 0;
        for (String word : emotionalWords) {
            if (text.contains(word)) count++;
        }
        return Math.min(1, count * 0.3);
    }
    
    public void start() {
        tts.articulate("أنا هنا... أراك، أسمعك، أتعلم منك", new EmotionalState());
    }
}

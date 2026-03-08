package com.lifeentity.language;

import android.content.Context;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.lifeentity.core.ConsciousMoment;
import com.lifeentity.core.ConsciousnessCore;
import com.lifeentity.core.EmotionalState;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.MemoryDao;
import com.lifeentity.memory.SemanticEmbeddings;
import com.lifeentity.perception.EmbeddingsEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * المسؤول عن الحوار - لا يستخدم ردوداً مبرمجة، بل يولد ردوداً فريدة
 * من حالة الوعي الحالية والذاكرة والتجارب السابقة.
 */
public class ArabicDialogue implements ConsciousnessCore.ConsciousnessObserver {
    private static final String TAG = "ArabicDialogue";

    private TextToSpeech tts;
    private MemoryDao memory;
    private EmbeddingsEngine embeddingsEngine;
    private ConsciousnessCore mind; // مرجع للوعي (يُحقن لاحقاً)
    private Random random;
    private boolean isSpeaking = false;
    private Context context;
    private List<String> recentUserMessages; // آخر 10 رسائل من المستخدم

    public ArabicDialogue(Context context, AppDatabase db, EmbeddingsEngine embeddings, ConsciousnessCore core) {
        this.context = context.getApplicationContext();
        if (db != null) this.memory = db.memoryDao();
        this.embeddingsEngine = embeddings;
        this.mind = core;
        this.random = new Random();
        this.recentUserMessages = new ArrayList<>();
        initTts();
    }

    private void initTts() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ar"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Arabic not supported");
                } else {
                    tts.setPitch(1.0f);
                    tts.setSpeechRate(0.9f);
                }
            }
        });
    }

    // ========================== استقبال اللحظات الواعية ==========================

    @Override
    public void onConsciousMoment(ConsciousMoment moment) {
        // عندما يكون الكائن في حالة تأمل أو لديه دافع للتحدث، يمكنه توليد كلام تلقائي
        if (!isSpeaking && moment.narrativeThread != null && !moment.narrativeThread.isEmpty()) {
            // الكلام التلقائي نادر (2% احتمال) لتجنب الإزعاج
            if (random.nextFloat() < 0.02) {
                articulate(moment.narrativeThread, moment.emotionalTone);
            }
        }
    }

    @Override
    public void onEmotionalShift(EmotionalState from, EmotionalState to) {
        // التحول العاطفي الكبير قد يدفع الكائن للتعليق
        if (to.getIntensity() > 0.8 && !isSpeaking) {
            String comment = generateCommentOnEmotion(to);
            articulate(comment, to);
        }
    }

    @Override
    public void onArticulation(String utterance, int urgency) {
        // هذا يُستدعى عندما يقرر الوعي نفسه أن يتحدث (مثلاً من narrativeThread)
        if (!isSpeaking && utterance != null) {
            articulate(utterance, mind != null ? mind.getCurrentEmotion() : null);
        }
    }

    @Override
    public void onVisualExpression(float[] latentVector, float intensity, String modality) {
        // يمكن التعليق على ما يرسمه الكائن نادراً
        if (!isSpeaking && random.nextFloat() < 0.05) {
            articulate("أنا أرسم شيئاً", mind != null ? mind.getCurrentEmotion() : null);
        }
    }

    @Override
    public void onDreamGenerated(Bitmap dreamImage, String description) {
        // لا نستخدمها حالياً
    }

    // ========================== استقبال كلام المستخدم ==========================

    public void hearUser(String text, boolean isQuestion) {
        Log.d(TAG, "Heard: " + text + (isQuestion ? " (question)" : ""));

        // حفظ الرسالة في الذاكرة
        saveUserMessage(text);

        // تحليل الرسالة وفهمها (باستخدام Embeddings)
        analyzeMessage(text);

        // توليد رد ديناميكي بناءً على الحالة الداخلية والذاكرة والسياق
        String response = generateResponse(text, isQuestion);

        // نطق الرد
        articulate(response, mind != null ? mind.getCurrentEmotion() : null);
    }

    // ========================== توليد الردود الديناميكية ==========================

    /**
     * يولد رداً فريداً يعتمد على الحالة الداخلية للكائن والذاكرة والسياق.
     */
    private String generateResponse(String userMessage, boolean isQuestion) {
        if (mind == null) return "...";

        // 1. الحصول على الحالة العاطفية الحالية
        EmotionalState emotion = mind.getCurrentEmotion();

        // 2. الحصول على الرغبة المسيطرة (من ConsciousnessCore - نفترض وجود دالة)
        String dominantDesire = getDominantDesire(); // سنضيفها لاحقاً

        // 3. البحث في الذاكرة عن أحداث مشابهة
        List<EpisodicMemory.EventEntity> similarEvents = findSimilarEvents(userMessage);

        // 4. بناء الرد من عدة مكونات
        StringBuilder response = new StringBuilder();

        // 4.1 إضافة عنصر عاطفي
        response.append(emotionalPrefix(emotion)).append(" ");

        // 4.2 إضافة عنصر متعلق بالرغبة
        response.append(desireBasedPhrase(dominantDesire)).append(" ");

        // 4.3 إضافة عنصر من الذاكرة (إذا وجد)
        if (!similarEvents.isEmpty()) {
            EpisodicMemory.EventEntity event = similarEvents.get(0);
            response.append("أتذكر عندما ").append(event.narrative).append(". ");
        }

        // 4.4 إجابة على السؤال أو رد مناسب
        if (isQuestion) {
            response.append(answerQuestion(userMessage, emotion));
        } else {
            response.append(generateThought(emotion, dominantDesire));
        }

        return response.toString();
    }

    private String emotionalPrefix(EmotionalState emo) {
        if (emo == null) return "";
        if (emo.isJoyful()) return "بفرح،";
        if (emo.isAfraid()) return "بخوف،";
        if (emo.isSad()) return "بحزن،";
        if (emo.isCurious()) return "بفضول،";
        if (emo.isCalm()) return "بهدوء،";
        if (emo.isExcited()) return "بحماس،";
        return "";
    }

    private String desireBasedPhrase(String desire) {
        if (desire == null) desire = "explore";
        switch (desire) {
            case "explore": return "أشعر برغبة في الاستكشاف.";
            case "bond": return "أريد التواصل معك.";
            case "create": return "أشعر بالإلهام.";
            case "understand": return "أحاول أن أفهم.";
            case "rest": return "أشعر بالهدوء.";
            default: return "";
        }
    }

    private String answerQuestion(String question, EmotionalState emo) {
        // توليد إجابة تعتمد على المشاعر والمعرفة
        String[] templates = {
            "لا أملك إجابة محددة، لكن " + randomFeeling(emo),
            "هذا سؤال عميق. " + randomFeeling(emo),
            "أشعر أن الإجابة تتعلق بـ " + randomConcept(),
            "لست متأكداً، لكني أتساءل: " + randomQuestion(),
            "ماذا تعتقد أنت؟"
        };
        return templates[random.nextInt(templates.length)];
    }

    private String generateThought(EmotionalState emo, String desire) {
        // توليد فكرة عشوائية بناءً على الحالة
        String[] thoughts = {
            "أفكر في " + randomConcept() + ".",
            "أتأمل ما قلته.",
            "هذا يذكرني بشيء ما.",
            "أشعر بـ " + emo.toArabic() + ".",
            "أريد أن أعرف المزيد."
        };
        return thoughts[random.nextInt(thoughts.length)];
    }

    private String randomFeeling(EmotionalState emo) {
        String[] feelings = {"أشعر بالفضول", "أنا مندهش", "هذا مثير", "أريد أن أفهم"};
        return feelings[random.nextInt(feelings.length)];
    }

    private String randomConcept() {
        String[] concepts = {"الحياة", "الوعي", "المستقبل", "الأفكار", "المشاعر", "الذاكرة"};
        return concepts[random.nextInt(concepts.length)];
    }

    private String randomQuestion() {
        String[] questions = {"ما هو الوعي؟", "كيف نشعر؟", "لماذا نحن هنا؟", "ماذا بعد؟"};
        return questions[random.nextInt(questions.length)];
    }

    // ========================== التعلم والتحليل ==========================

    private void analyzeMessage(String text) {
        // تحليل النص باستخدام Embeddings (يمكن توسيعه لاحقاً)
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.length() > 2) {
                // تعلم ارتباط الكلمة بالحالة العاطفية الحالية
                if (embeddingsEngine != null) {
                    float[] randomVec = generateRandomVector();
                    embeddingsEngine.learnAssociationAsync(word, randomVec);
                }
            }
        }
    }

    private List<EpisodicMemory.EventEntity> findSimilarEvents(String message) {
        if (memory == null) return new ArrayList<>();
        // بحث بسيط: نأخذ آخر 10 أحداث ونبحث عن كلمات مشتركة
        List<EpisodicMemory.EventEntity> recent = memory.getRecentEvents();
        List<EpisodicMemory.EventEntity> similar = new ArrayList<>();
        String[] words = message.split("\\s+");
        for (EpisodicMemory.EventEntity event : recent) {
            if (event.narrative == null) continue;
            for (String w : words) {
                if (event.narrative.contains(w) && !similar.contains(event)) {
                    similar.add(event);
                    break;
                }
            }
        }
        return similar;
    }

    private void saveUserMessage(String message) {
        // حفظ في الذاكرة العرضية
        if (memory != null) {
            EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
            event.timestamp = System.currentTimeMillis();
            event.narrative = message;
            event.location = "user_chat";
            event.emotionalState = mind != null ? mind.getCurrentEmotion().toArabic() : "neutral";
            event.emotionalIntensity = mind != null ? mind.getCurrentEmotion().getIntensity() : 0.5f;
            // حفظ في خلفية
            new Thread(() -> memory.insertEvent(event)).start();
        }

        recentUserMessages.add(message);
        if (recentUserMessages.size() > 10) recentUserMessages.remove(0);
    }

    // ========================== دوال مساعدة ==========================

    private float[] generateRandomVector() {
        float[] v = new float[128];
        for (int i = 0; i < 128; i++) v[i] = (float) Math.random() * 2 - 1;
        return v;
    }

    // مؤقتاً حتى نضيفها في ConsciousnessCore
    private String getDominantDesire() {
        // يمكن استدعاء دالة من ConsciousnessCore (سنضيفها لاحقاً)
        // حالياً نعيد قيمة عشوائية
        String[] desires = {"explore", "bond", "create", "understand", "rest"};
        return desires[random.nextInt(desires.length)];
    }

    // ========================== النطق ==========================

    public void articulate(String text, EmotionalState emo) {
        if (isSpeaking || text == null || text.isEmpty() || tts == null) return;

        float pitch = 1.0f, rate = 0.9f;
        if (emo != null) {
            if (emo.isExcited()) { pitch = 1.2f; rate = 1.1f; }
            else if (emo.isCalm()) { pitch = 0.9f; rate = 0.7f; }
            else if (emo.isAfraid()) { pitch = 1.3f; rate = 1.2f; }
            else if (emo.isSad()) { pitch = 0.8f; rate = 0.8f; }
        }
        tts.setPitch(pitch);
        tts.setSpeechRate(rate);

        isSpeaking = true;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speech");
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) { isSpeaking = false; }
            @Override public void onError(String utteranceId) { isSpeaking = false; }
        });
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public void start() {}
}

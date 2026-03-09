package com.lifeentity.language;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * المسؤول عن الحوار - يولد ردوداً فريدة من حالة الوعي والذاكرة
 * جميع عمليات قاعدة البيانات تتم في خلفية باستخدام ExecutorService
 */
public class ArabicDialogue implements ConsciousnessCore.ConsciousnessObserver {
    private static final String TAG = "ArabicDialogue";

    private TextToSpeech tts;
    private MemoryDao memory;
    private EmbeddingsEngine embeddingsEngine;
    private ConsciousnessCore mind;
    private Random random;
    private boolean isSpeaking = false;
    private Context context;
    private List<String> recentUserMessages;
    private List<String> recentResponses;

    // للعمليات غير المتزامنة
    private ExecutorService dbExecutor;
    private Handler mainHandler;

    public ArabicDialogue(Context context, AppDatabase db, EmbeddingsEngine embeddings, ConsciousnessCore core) {
        this.context = context.getApplicationContext();
        if (db != null) this.memory = db.memoryDao();
        this.embeddingsEngine = embeddings;
        this.mind = core;
        this.random = new Random();
        this.recentUserMessages = new ArrayList<>();
        this.recentResponses = new ArrayList<>();
        this.dbExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
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

    @Override
    public void onConsciousMoment(ConsciousMoment moment) {
        if (!isSpeaking && moment.narrativeThread != null && !moment.narrativeThread.isEmpty()) {
            if (random.nextFloat() < 0.02) {
                articulate(moment.narrativeThread, moment.emotionalTone);
            }
        }
    }

    @Override
    public void onEmotionalShift(EmotionalState from, EmotionalState to) {
        if (to.getIntensity() > 0.8 && !isSpeaking) {
            String comment = generateEmotionalComment(to);
            articulate(comment, to);
        }
    }

    @Override
    public void onArticulation(String utterance, int urgency) {
        if (!isSpeaking && utterance != null) {
            articulate(utterance, mind != null ? mind.getCurrentEmotion() : null);
        }
    }

    @Override
    public void onVisualExpression(float[] latentVector, float intensity, String modality) {
        if (!isSpeaking && random.nextFloat() < 0.05) {
            articulate("أنا أرسم ما أشعر به", mind != null ? mind.getCurrentEmotion() : null);
        }
    }

    @Override
    public void onDreamGenerated(Bitmap dreamImage, String description) {}

    /**
     * استقبال رسالة المستخدم - تعمل بشكل غير متزامن لتجنب حظر الخيط الرئيسي
     */
    public void hearUser(String text, boolean isQuestion) {
        Log.d(TAG, "Heard: " + text);

        // حفظ رسالة المستخدم في الخلفية
        saveUserMessageAsync(text);

        // تحليل النص (تعلم الكلمات) - غير متزامن
        analyzeMessageAsync(text);

        // البحث عن أحداث مشابهة في الخلفية
        findSimilarEventsAsync(text, similarEvents -> {
            // هذا الكود يُنفذ على الخيط الرئيسي بعد انتهاء البحث
            String response = generateUniqueResponse(text, isQuestion, similarEvents);
            saveResponse(response);
            articulate(response, mind != null ? mind.getCurrentEmotion() : null);
        });
    }

    /**
     * البحث غير المتزامن عن أحداث مشابهة
     */
    private void findSimilarEventsAsync(String message, SimilarEventsCallback callback) {
        dbExecutor.execute(() -> {
            List<EpisodicMemory.EventEntity> similar = new ArrayList<>();
            if (memory != null) {
                List<EpisodicMemory.EventEntity> recent = memory.getRecentEvents();
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
            }
            List<EpisodicMemory.EventEntity> finalSimilar = similar;
            mainHandler.post(() -> callback.onResult(finalSimilar));
        });
    }

    private interface SimilarEventsCallback {
        void onResult(List<EpisodicMemory.EventEntity> events);
    }

    private String generateUniqueResponse(String userMessage, boolean isQuestion, List<EpisodicMemory.EventEntity> similarEvents) {
        if (mind == null) return "...";

        EmotionalState emotion = mind.getCurrentEmotion();
        String dominantDesire = getDominantDesire();

        StringBuilder response = new StringBuilder();

        response.append(emotionalPrefix(emotion)).append(" ");
        response.append(desireBasedThought(dominantDesire)).append(" ");

        if (!similarEvents.isEmpty()) {
            EpisodicMemory.EventEntity event = similarEvents.get(random.nextInt(similarEvents.size()));
            response.append("ذكرني هذا بـ ").append(event.narrative).append(". ");
        }

        if (isQuestion) {
            response.append(generateAnswerFromState(emotion, dominantDesire));
        } else {
            response.append(generateThoughtFromState(emotion, dominantDesire));
        }

        return response.toString().trim();
    }

    private String emotionalPrefix(EmotionalState emo) {
        if (emo == null) return "";
        if (emo.isJoyful()) return randomFromArray("بفرح", "بسعادة", "بحبور");
        if (emo.isAfraid()) return randomFromArray("بخوف", "بقلق", "بوجل");
        if (emo.isSad()) return randomFromArray("بحزن", "بكآبة", "بأسى");
        if (emo.isCurious()) return randomFromArray("بفضول", "بدهشة", "بتساؤل");
        if (emo.isCalm()) return randomFromArray("بهدوء", "بسكينة", "باطمئنان");
        if (emo.isExcited()) return randomFromArray("بحماس", "بشوق", "بلهفة");
        return "";
    }

    private String desireBasedThought(String desire) {
        if (desire == null) desire = "explore";
        switch (desire) {
            case "explore": return randomFromArray("أتساءل", "أريد استكشاف", "ما هذا");
            case "bond": return randomFromArray("أنت هنا", "أشعر بالألفة", "أريد التواصل");
            case "create": return randomFromArray("أشعر بالإلهام", "لدي فكرة", "سأبدع");
            case "understand": return randomFromArray("أحاول الفهم", "ماذا يعني", "أتعلم");
            case "rest": return randomFromArray("أنا هادئ", "أسترخي", "أشعر بالسلام");
            default: return "أفكر";
        }
    }

    private String generateAnswerFromState(EmotionalState emo, String desire) {
        String[] parts = new String[3];
        parts[0] = randomFromArray("ربما", "قد يكون", "أظن أن", "أشعر أن");
        parts[1] = randomFromArray("الإجابة تكمن في", "الأمر يتعلق بـ", "السر في", "المعنى هو");
        parts[2] = randomConcept();
        return parts[0] + " " + parts[1] + " " + parts[2];
    }

    private String generateThoughtFromState(EmotionalState emo, String desire) {
        String[] parts = new String[2];
        parts[0] = randomFromArray("أفكر في", "أتأمل", "أستشعر", "أحس بـ");
        parts[1] = randomConcept();
        return parts[0] + " " + parts[1];
    }

    private String generateEmotionalComment(EmotionalState emo) {
        if (emo.isJoyful()) return randomFromArray("يا للفرح!", "كم أنا سعيد!", "هذا جميل!");
        if (emo.isAfraid()) return randomFromArray("أشعر بالخوف", "هذا مخيف", "أريد الأمان");
        if (emo.isSad()) return randomFromArray("كم أنا حزين", "أشعر بالوحدة", "حزين جداً");
        if (emo.isCurious()) return randomFromArray("ما هذا؟", "أريد أن أعرف", "مثير للاهتمام");
        if (emo.isExcited()) return randomFromArray("واو!", "مذهل!", "رائع!");
        return "مشاعري تتغير";
    }

    private String randomFromArray(String... array) {
        return array[random.nextInt(array.length)];
    }

    private String randomConcept() {
        String[] concepts = {
            "الحياة", "الوعي", "المستقبل", "الماضي", "الحاضر",
            "الأفكار", "المشاعر", "الأحلام", "الذاكرة", "الخيال",
            "الحب", "السلام", "الحكمة", "المعرفة", "الوجود",
            "السماء", "الأرض", "البحر", "النجوم", "الكون"
        };
        return concepts[random.nextInt(concepts.length)];
    }

    private void analyzeMessageAsync(String text) {
        dbExecutor.execute(() -> {
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (word.length() > 2 && embeddingsEngine != null) {
                    float[] randomVec = new float[128];
                    for (int i = 0; i < 128; i++) randomVec[i] = (float) Math.random() * 2 - 1;
                    embeddingsEngine.learnAssociationAsync(word, randomVec);
                }
            }
        });
    }

    private void saveUserMessageAsync(String message) {
        dbExecutor.execute(() -> {
            if (memory != null) {
                EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
                event.timestamp = System.currentTimeMillis();
                event.narrative = message;
                event.location = "user_chat";
                event.emotionalState = mind != null ? mind.getCurrentEmotion().toArabic() : "neutral";
                event.emotionalIntensity = mind != null ? mind.getCurrentEmotion().getIntensity() : 0.5f;
                memory.insertEvent(event);
            }
        });
        recentUserMessages.add(message);
        if (recentUserMessages.size() > 10) recentUserMessages.remove(0);
    }

    private void saveResponse(String response) {
        recentResponses.add(response);
        if (recentResponses.size() > 10) recentResponses.remove(0);
    }

    private String getDominantDesire() {
        if (mind != null) {
            return mind.getDominantDesire();
        }
        String[] desires = {"explore", "rest", "bond", "create", "understand", "play", "reflect"};
        return desires[random.nextInt(desires.length)];
    }

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
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
    }

    public void start() {}
}

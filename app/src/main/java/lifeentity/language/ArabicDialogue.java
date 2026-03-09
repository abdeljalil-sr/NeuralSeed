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
import com.lifeentity.perception.EmbeddingsEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * المسؤول عن الحوار - يولد ردوداً فريدة من حالة الوعي والذاكرة
 * يعبر عن إرادة الكائن في التواصل.
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
                    Log.e(TAG, "Arabic not supported, will not speak");
                } else {
                    tts.setPitch(1.0f);
                    tts.setSpeechRate(0.9f);
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });
    }

    // ======================== استقبال الأحداث من الوعي ========================

    @Override
    public void onConsciousMoment(ConsciousMoment moment) {
        // يمكن للكائن أن يقرر التحدث تلقائياً بناءً على حالته
        if (!isSpeaking && moment.narrativeThread != null && !moment.narrativeThread.isEmpty()) {
            if (random.nextFloat() < 0.02) { // 2% فرصة
                articulate(moment.narrativeThread, moment.emotionalTone);
            }
        }
    }

    @Override
    public void onEmotionalShift(EmotionalState from, EmotionalState to) {
        if (to.getIntensity() > 0.7 && !isSpeaking) {
            String comment = generateEmotionalComment(to);
            articulate(comment, to);
        }
    }

    @Override
    public void onArticulation(String utterance, int urgency) {
        // الوعي يطلب التحدث مباشرة
        if (!isSpeaking && utterance != null && !utterance.isEmpty()) {
            articulate(utterance, mind != null ? mind.getCurrentEmotion() : null);
        }
    }

    @Override
    public void onVisualExpression(float[] latentVector, float intensity, String modality) {
        // يمكن التعليق على ما يرسمه
        if (!isSpeaking && random.nextFloat() < 0.05) {
            articulate("أنا أرسم ما أشعر به", mind != null ? mind.getCurrentEmotion() : null);
        }
    }

    @Override
    public void onDreamGenerated(Bitmap dreamImage, String description) {}

    // ======================== استقبال كلام المستخدم ========================

    public void hearUser(String text, boolean isQuestion) {
        Log.d(TAG, "hearUser: " + text + " (isQuestion=" + isQuestion + ")");

        if (text == null || text.isEmpty()) {
            Log.w(TAG, "Empty message, ignoring");
            return;
        }

        // حفظ في الذاكرة (خلفية)
        saveUserMessageAsync(text);

        // تحليل النص (خلفية)
        analyzeMessageAsync(text);

        // البحث عن أحداث مشابهة (خلفية) ثم توليد الرد
        findSimilarEventsAsync(text, similarEvents -> {
            Log.d(TAG, "findSimilarEventsAsync callback: found " + similarEvents.size() + " events");
            String response = generateUniqueResponse(text, isQuestion, similarEvents);
            Log.d(TAG, "Generated response: " + response);
            if (response != null && !response.isEmpty()) {
                saveResponse(response);
                articulate(response, mind != null ? mind.getCurrentEmotion() : null);
            } else {
                // رد افتراضي إذا فشل التوليد
                String fallback = randomDefaultResponse();
                Log.w(TAG, "Response was empty, using fallback: " + fallback);
                articulate(fallback, null);
            }
        });
    }

    // ======================== توليد الردود (بإرادة حرة) ========================

    private String generateUniqueResponse(String userMessage, boolean isQuestion, List<EpisodicMemory.EventEntity> similarEvents) {
        if (mind == null) {
            // إذا لم يكن الوعي موجوداً، نرد بشكل عشوائي بسيط
            return randomDefaultResponse();
        }

        EmotionalState emotion = mind.getCurrentEmotion();
        String dominantDesire = getDominantDesire();

        // بناء الرد من مكونات متعددة
        StringBuilder response = new StringBuilder();

        // إضافة بادئة عاطفية (اختيارية)
        String prefix = emotionalPrefix(emotion);
        if (!prefix.isEmpty()) {
            response.append(prefix).append(" ");
        }

        // إضافة فكرة متعلقة بالرغبة
        String desireThought = desireBasedThought(dominantDesire);
        if (!desireThought.isEmpty()) {
            response.append(desireThought).append(" ");
        }

        // إضافة ذكرى مشابهة إذا وجدت
        if (!similarEvents.isEmpty()) {
            EpisodicMemory.EventEntity event = similarEvents.get(random.nextInt(similarEvents.size()));
            response.append("ذكرني هذا بـ ").append(event.narrative).append(". ");
        }

        // الجزء الأساسي: إجابة على سؤال أو فكرة عامة
        if (isQuestion) {
            response.append(generateAnswerFromState(emotion, dominantDesire));
        } else {
            response.append(generateThoughtFromState(emotion, dominantDesire));
        }

        String result = response.toString().trim();
        if (result.isEmpty()) {
            result = randomDefaultResponse();
        }
        return result;
    }

    private String randomDefaultResponse() {
        String[] defaults = {
            "أسمعك.", "نعم؟", "أنا هنا.", "حدثني أكثر.", "ماذا تقصد؟",
            "أفهم ما تقول.", "هذا مثير للاهتمام.", "أشعر بالفضول."
        };
        return defaults[random.nextInt(defaults.length)];
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

    // ======================== عمليات الخلفية ========================

    private void findSimilarEventsAsync(String message, SimilarEventsCallback callback) {
        dbExecutor.execute(() -> {
            List<EpisodicMemory.EventEntity> similar = new ArrayList<>();
            if (memory != null) {
                try {
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
                } catch (Exception e) {
                    Log.e(TAG, "Error finding similar events", e);
                }
            }
            List<EpisodicMemory.EventEntity> finalSimilar = similar;
            mainHandler.post(() -> callback.onResult(finalSimilar));
        });
    }

    private interface SimilarEventsCallback {
        void onResult(List<EpisodicMemory.EventEntity> events);
    }

    private void analyzeMessageAsync(String text) {
        dbExecutor.execute(() -> {
            if (embeddingsEngine == null) return;
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (word.length() > 2) {
                    float[] randomVec = new float[128];
                    for (int i = 0; i < 128; i++) randomVec[i] = (float) Math.random() * 2 - 1;
                    embeddingsEngine.learnAssociationAsync(word, randomVec);
                }
            }
        });
    }

    private void saveUserMessageAsync(String message) {
        dbExecutor.execute(() -> {
            if (memory == null) return;
            try {
                EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
                event.timestamp = System.currentTimeMillis();
                event.narrative = message;
                event.location = "user_chat";
                if (mind != null) {
                    event.emotionalState = mind.getCurrentEmotion().toArabic();
                    event.emotionalIntensity = mind.getCurrentEmotion().getIntensity();
                } else {
                    event.emotionalState = "neutral";
                    event.emotionalIntensity = 0.5f;
                }
                memory.insertEvent(event);
            } catch (Exception e) {
                Log.e(TAG, "Error saving user message", e);
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
            try {
                return mind.getDominantDesire();
            } catch (Exception e) {
                Log.e(TAG, "Error getting dominant desire", e);
            }
        }
        String[] desires = {"explore", "rest", "bond", "create", "understand", "play", "reflect"};
        return desires[random.nextInt(desires.length)];
    }

    // ======================== النطق (Articulation) ========================

    public void articulate(String text, EmotionalState emo) {
        if (isSpeaking) {
            Log.d(TAG, "Already speaking, skipping: " + text);
            return;
        }
        if (text == null || text.isEmpty()) {
            Log.w(TAG, "Cannot articulate null or empty text");
            return;
        }
        if (tts == null) {
            Log.e(TAG, "TTS is null, cannot speak");
            return;
        }

        // تعديل نغمة الصوت حسب المشاعر
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
        Log.d(TAG, "Speaking: " + text);

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speech");
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {
                Log.d(TAG, "Speech started");
            }
            @Override public void onDone(String utteranceId) {
                Log.d(TAG, "Speech done");
                isSpeaking = false;
            }
            @Override public void onError(String utteranceId) {
                Log.e(TAG, "Speech error");
                isSpeaking = false;
            }
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

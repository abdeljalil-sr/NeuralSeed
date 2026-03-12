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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * المسؤول عن الحوار - يحلل اللغة ويوجهها إلى الوعي، وينطق ردود الوعي
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
    
    private AdvancedArabicLexicon.TextAnalysis currentAnalysis;
    private Map<String, Object> messageContext;

    // مؤشر إذا كان TTS جاهزًا
    private boolean ttsReady = false;

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
        this.messageContext = new HashMap<>();
        initTts();
    }

    private void initTts() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ar"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Arabic not supported, will not speak");
                    ttsReady = false;
                } else {
                    tts.setPitch(1.0f);
                    tts.setSpeechRate(0.9f);
                    ttsReady = true;
                    Log.i(TAG, "TTS initialized successfully");
                    
                    // اختبار نطق عند بدء التشغيل
                    mainHandler.postDelayed(() -> {
                        performTTS("مرحباً، أنا هنا", null);
                    }, 2000);
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
                ttsReady = false;
            }
        });
    }

    // ==================== واجهة ConsciousnessObserver ====================

    @Override
    public void onConsciousMoment(ConsciousMoment moment) {
        // يمكن استخدامها لمراقبة حالة الوعي
    }

    @Override
    public void onEmotionalShift(EmotionalState from, EmotionalState to) {
        // يمكن استخدامها للتعليق على تغير المشاعر إذا أردنا
    }

    @Override
    public void onArticulation(String utterance, int urgency) {
        Log.d(TAG, "onArticulation received: " + utterance);
        // الوعي يريد التحدث - نقوم بتشغيل الصوت
        performTTS(utterance, mind != null ? mind.getCurrentEmotion() : null);
    }

    @Override
    public void onVisualExpression(Bitmap image, String description) {
        // لا نهتم حالياً بالصور من الوعي
        Log.d(TAG, "Visual expression: " + description);
    }

    @Override
    public void onDreamGenerated(Bitmap dreamImage, String description) {
        Log.d(TAG, "Dream generated: " + description);
    }

    @Override
    public void onMovementImpulse(String direction, float intensity) {
        // لا نهتم
    }

    @Override
    public void onVerbalExpression(String text, float intensity) {
        Log.d(TAG, "Verbal expression: " + text);
        if (!isSpeaking && text != null && !text.isEmpty()) {
            performTTS(text, mind != null ? mind.getCurrentEmotion() : null);
        }
    }

    @Override
    public void onDeepThinkingInsight(String insight, List<EpisodicMemory.EventEntity> connectedMemories) {
        Log.d(TAG, "Deep thinking insight: " + insight);
        if (!isSpeaking && insight != null && !insight.isEmpty()) {
            performTTS("أدركت: " + insight, mind != null ? mind.getCurrentEmotion() : null);
        }
    }

    // ==================== استقبال رسائل المستخدم ====================

    public void hearUser(String text, boolean isQuestion) {
    Log.d(TAG, "hearUser: " + text + " (isQuestion=" + isQuestion + ")");

    if (text == null || text.isEmpty()) {
        Log.w(TAG, "Empty message, ignoring");
        return;
    }

    // تحليل النص باستخدام المعجم العربي المتقدم (لأغراض التعلم المحلي فقط)
    currentAnalysis = AdvancedArabicLexicon.analyze(text);
    updateMessageContext(text, isQuestion);
    
    Log.d(TAG, "Lexicon analysis: " + currentAnalysis.getWordCount() + " words, " +
          "recognition: " + String.format("%.1f%%", currentAnalysis.getRecognitionRate() * 100));

    // حفظ الرسالة في الذاكرة
    saveUserMessageAsync(text);

    // تحليل النص لتعلم الارتباطات (اختياري)
    analyzeMessageAsync(text);

    // إرسال النص الخام إلى الوعي (هو من سيقوم بالتحليل المتقدم والرد)
    if (mind != null) {
        mind.processUserMessage(text);
        Log.d(TAG, "Raw text sent to ConsciousnessCore");
    } else {
        Log.e(TAG, "mind is null, cannot process message");
    }
    }

    private String detectQuestionType() {
        if (currentAnalysis == null) return "general";
        
        boolean hasWhat = false, hasWho = false, hasWhere = false, hasWhen = false, 
                hasHow = false, hasWhy = false, hasYesNo = false;
        
        for (AdvancedArabicLexicon.WordAnalysis word : currentAnalysis.getWords()) {
            String norm = word.getNormalizedWord();
            if (norm.equals("ما") || norm.equals("ماذا")) hasWhat = true;
            if (norm.equals("من")) hasWho = true;
            if (norm.equals("اين") || norm.equals("أين")) hasWhere = true;
            if (norm.equals("متى")) hasWhen = true;
            if (norm.equals("كيف")) hasHow = true;
            if (norm.equals("لماذا")) hasWhy = true;
            if (norm.equals("هل")) hasYesNo = true;
        }
        
        if (hasYesNo) return "yesno";
        if (hasWhat) return "what";
        if (hasWho) return "who";
        if (hasWhere) return "where";
        if (hasWhen) return "when";
        if (hasHow) return "how";
        if (hasWhy) return "why";
        
        return "general";
    }

    private void updateMessageContext(String text, boolean isQuestion) {
        messageContext.clear();
        messageContext.put("isQuestion", isQuestion);
        messageContext.put("text", text);
        
        if (currentAnalysis != null) {
            if (isQuestion) {
                messageContext.put("questionType", detectQuestionType());
            }
            
            List<String> nouns = new ArrayList<>();
            List<String> verbs = new ArrayList<>();
            List<String> emotions = new ArrayList<>();
            List<String> adjectives = new ArrayList<>();
            List<String> places = new ArrayList<>();
            List<String> times = new ArrayList<>();
            
            for (AdvancedArabicLexicon.WordAnalysis word : currentAnalysis.getWords()) {
                String normalized = word.getNormalizedWord();
                AdvancedArabicLexicon.WordCategory cat = word.getCategory();
                
                if (cat.name().contains("NOUN")) {
                    nouns.add(normalized);
                    if (cat == AdvancedArabicLexicon.WordCategory.NOUN_PLACE) {
                        places.add(normalized);
                    } else if (cat == AdvancedArabicLexicon.WordCategory.NOUN_TIME) {
                        times.add(normalized);
                    }
                } else if (cat.name().contains("VERB")) {
                    verbs.add(normalized);
                } else if (cat.name().contains("ADJECTIVE")) {
                    adjectives.add(normalized);
                }
                
                // كلمات العواطف
                if (normalized.contains("فرح") || normalized.contains("سعيد") || 
                    normalized.contains("سرور") || normalized.contains("بهجة")) {
                    emotions.add("joy");
                } else if (normalized.contains("حزن") || normalized.contains("بكاء") || 
                           normalized.contains("اسى") || normalized.contains("هم")) {
                    emotions.add("sadness");
                } else if (normalized.contains("خوف") || normalized.contains("قلق") || 
                           normalized.contains("فزع") || normalized.contains("رعب")) {
                    emotions.add("fear");
                } else if (normalized.contains("حب") || normalized.contains("عشق") || 
                           normalized.contains("هوى") || normalized.contains("ولع")) {
                    emotions.add("love");
                } else if (normalized.contains("دهشة") || normalized.contains("مفاجأة") || 
                           normalized.contains("عجب") || normalized.contains("استغراب")) {
                    emotions.add("surprise");
                } else if (normalized.contains("فضول") || normalized.contains("تساؤل") || 
                           normalized.contains("استفسار") || normalized.contains("بحث")) {
                    emotions.add("curiosity");
                } else if (normalized.contains("غضب") || normalized.contains("حنق") || 
                           normalized.contains("سخط") || normalized.contains("غيظ")) {
                    emotions.add("anger");
                }
            }
            
            messageContext.put("keyNouns", nouns);
            messageContext.put("keyVerbs", verbs);
            messageContext.put("mentionedEmotions", emotions);
            messageContext.put("keyAdjectives", adjectives);
            messageContext.put("mentionedPlaces", places);
            messageContext.put("mentionedTimes", times);
            
            boolean hasImperative = currentAnalysis.getWords().stream()
                .anyMatch(w -> w.getCategory() == AdvancedArabicLexicon.WordCategory.VERB_IMPERATIVE);
            messageContext.put("hasImperative", hasImperative);
            
            boolean isNegative = currentAnalysis.getWords().stream()
                .anyMatch(w -> w.getCategory() == AdvancedArabicLexicon.WordCategory.PARTICLE_NEGATION);
            messageContext.put("isNegative", isNegative);
            
            boolean isFuture = currentAnalysis.getWords().stream()
                .anyMatch(w -> w.getCategory() == AdvancedArabicLexicon.WordCategory.PARTICLE_FUTURE);
            messageContext.put("isFuture", isFuture);
        }
    }

    // ==================== تشغيل الصوت ====================

    private void performTTS(String text, EmotionalState emo) {
        if (!ttsReady) {
            Log.e(TAG, "TTS not ready, cannot speak: " + text);
            return;
        }
        if (isSpeaking) {
            Log.d(TAG, "Already speaking, skipping TTS: " + text);
            return;
        }
        if (text == null || text.isEmpty()) {
            Log.w(TAG, "Cannot speak null or empty text");
            return;
        }
        if (tts == null) {
            Log.e(TAG, "TTS is null, cannot speak");
            return;
        }

        float pitch = 1.0f, rate = 0.9f;
        if (emo != null) {
            if (emo.isExcited()) { pitch = 1.2f; rate = 1.1f; }
            else if (emo.isCalm()) { pitch = 0.9f; rate = 0.7f; }
            else if (emo.isAfraid()) { pitch = 1.3f; rate = 1.2f; }
            else if (emo.isSad()) { pitch = 0.8f; rate = 0.8f; }
            else if (emo.isJoyful()) { pitch = 1.15f; rate = 1.0f; }
            else if (emo.isAngry()) { pitch = 0.95f; rate = 1.05f; }
        }
        tts.setPitch(pitch);
        tts.setSpeechRate(rate);

        isSpeaking = true;
        Log.i(TAG, "Speaking: " + text);

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

    // ==================== التفاعل مع قاعدة البيانات ====================

    private void analyzeMessageAsync(String text) {
        dbExecutor.execute(() -> {
            if (embeddingsEngine == null) return;
            List<String> meaningfulWords = new ArrayList<>();
            if (currentAnalysis != null) {
                for (AdvancedArabicLexicon.WordAnalysis word : currentAnalysis.getWords()) {
                    if (word.getCategory() != AdvancedArabicLexicon.WordCategory.UNKNOWN &&
                        word.getNormalizedWord().length() > 2) {
                        meaningfulWords.add(word.getNormalizedWord());
                    }
                }
            }
            if (meaningfulWords.isEmpty()) {
                String[] words = text.split("\\s+");
                for (String word : words) {
                    if (word.length() > 2) {
                        meaningfulWords.add(word);
                    }
                }
            }
            for (String word : meaningfulWords) {
                float[] randomVec = new float[128];
                for (int i = 0; i < 128; i++) randomVec[i] = (float) Math.random() * 2 - 1;
                embeddingsEngine.learnAssociationAsync(word, randomVec);
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

    // ==================== دورة الحياة ====================

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

    public AdvancedArabicLexicon.TextAnalysis getLastAnalysis() {
        return currentAnalysis;
    }

    public Map<String, Object> getMessageContext() {
        return new HashMap<>(messageContext);
    }
}

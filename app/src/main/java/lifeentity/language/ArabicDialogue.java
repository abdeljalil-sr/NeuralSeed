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
 * المسؤول عن الحوار - يولد ردوداً فريدة من حالة الوعي والذاكرة والتحليل اللغوي المتقدم
 * يعبر عن إرادة الكائن في التواصل مع دعم المعجم العربي المتقدم.
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
                } else {
                    tts.setPitch(1.0f);
                    tts.setSpeechRate(0.9f);
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
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
        if (to.getIntensity() > 0.7 && !isSpeaking) {
            String comment = generateEmotionalComment(to);
            articulate(comment, to);
        }
    }

    @Override
    public void onArticulation(String utterance, int urgency) {
        if (!isSpeaking && utterance != null && !utterance.isEmpty()) {
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

    @Override
    public void onMovementImpulse(String direction, float intensity) {
        // لا نستخدمها حالياً
    }

    // ✅ إضافة الدالة المفقودة من واجهة ConsciousnessObserver
    @Override
    public void onVerbalExpression(String text, float intensity) {
        // يمكن استخدامها للتعليق على التعبير اللفظي، لكننا لا نحتاجها حالياً
        Log.d(TAG, "onVerbalExpression: " + text + " (intensity=" + intensity + ")");
    }

    public void hearUser(String text, boolean isQuestion) {
        Log.d(TAG, "hearUser: " + text + " (isQuestion=" + isQuestion + ")");

        if (text == null || text.isEmpty()) {
            Log.w(TAG, "Empty message, ignoring");
            return;
        }

        currentAnalysis = AdvancedArabicLexicon.analyze(text);
        updateMessageContext(text, isQuestion);
        
        Log.d(TAG, "Lexicon analysis: " + currentAnalysis.getWordCount() + " words, " +
              "recognition: " + String.format("%.1f%%", currentAnalysis.getRecognitionRate() * 100));

        saveUserMessageAsync(text);
        analyzeMessageAsync(text);

        findSimilarEventsAsync(text, similarEvents -> {
            Log.d(TAG, "findSimilarEventsAsync callback: found " + similarEvents.size() + " events");
            String response = generateUniqueResponse(text, isQuestion, similarEvents);
            Log.d(TAG, "Generated response: " + response);
            if (response != null && !response.isEmpty()) {
                saveResponse(response);
                articulate(response, mind != null ? mind.getCurrentEmotion() : null);
            } else {
                String fallback = generateContextualFallback();
                Log.w(TAG, "Response was empty, using contextual fallback: " + fallback);
                articulate(fallback, null);
            }
        });
    }

    private void updateMessageContext(String text, boolean isQuestion) {
        messageContext.clear();
        messageContext.put("isQuestion", isQuestion);
        messageContext.put("text", text);
        
        if (currentAnalysis != null) {
            if (isQuestion) {
                String questionType = detectQuestionType();
                messageContext.put("questionType", questionType);
            }
            
            List<String> nouns = new ArrayList<>();
            List<String> verbs = new ArrayList<>();
            List<String> emotions = new ArrayList<>();
            
            for (AdvancedArabicLexicon.WordAnalysis word : currentAnalysis.getWords()) {
                String normalized = word.getNormalizedWord();
                AdvancedArabicLexicon.WordCategory cat = word.getCategory();
                
                if (cat.name().contains("NOUN")) {
                    nouns.add(normalized);
                } else if (cat.name().contains("VERB")) {
                    verbs.add(normalized);
                }
                
                // كشف المشاعر
                if (normalized.contains("فرح") || normalized.contains("سعيد")) {
                    emotions.add("joy");
                } else if (normalized.contains("حزن") || normalized.contains("بكاء")) {
                    emotions.add("sadness");
                } else if (normalized.contains("خوف") || normalized.contains("قلق")) {
                    emotions.add("fear");
                } else if (normalized.contains("حب")) {
                    emotions.add("love");
                } else if (normalized.contains("دهشة") || normalized.contains("مفاجأة")) {
                    emotions.add("surprise");
                } else if (normalized.contains("فضول") || normalized.contains("تساؤل")) {
                    emotions.add("curiosity");
                }
            }
            
            messageContext.put("keyNouns", nouns);
            messageContext.put("keyVerbs", verbs);
            messageContext.put("mentionedEmotions", emotions);
            
            boolean hasImperative = currentAnalysis.getWords().stream()
                .anyMatch(w -> w.getCategory() == AdvancedArabicLexicon.WordCategory.VERB_IMPERATIVE);
            messageContext.put("hasImperative", hasImperative);
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
            if (norm.equals("اين")) hasWhere = true;
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

    private String generateUniqueResponse(String userMessage, boolean isQuestion, List<EpisodicMemory.EventEntity> similarEvents) {
        if (mind == null) {
            return generateContextualFallback();
        }

        EmotionalState emotion = mind.getCurrentEmotion();
        String dominantDesire = getDominantDesire();

        StringBuilder response = new StringBuilder();

        String prefix = emotionalPrefix(emotion);
        if (!prefix.isEmpty()) {
            response.append(prefix).append(" ");
        }

        String desireThought = desireBasedThought(dominantDesire);
        if (!desireThought.isEmpty()) {
            response.append(desireThought).append(" ");
        }

        if (isQuestion) {
            String questionResponse = generateQuestionSpecificResponse();
            if (!questionResponse.isEmpty()) {
                response.append(questionResponse).append(" ");
            }
        }

        String verbComment = generateVerbComment();
        if (!verbComment.isEmpty()) {
            response.append(verbComment).append(" ");
        }

        if (!similarEvents.isEmpty()) {
            EpisodicMemory.EventEntity event = similarEvents.get(random.nextInt(similarEvents.size()));
            response.append("ذكرني هذا بـ ").append(event.narrative).append(". ");
        }

        if (isQuestion) {
            response.append(generateAnswerFromState(emotion, dominantDesire));
        } else {
            response.append(generateThoughtFromState(emotion, dominantDesire));
        }

        if (messageContext.containsKey("hasImperative") && (Boolean) messageContext.get("hasImperative")) {
            response.append(" ").append(generateImperativeResponse());
        }

        String result = response.toString().trim();
        if (result.isEmpty()) {
            result = generateContextualFallback();
        }
        return result;
    }

    private String generateQuestionSpecificResponse() {
        String questionType = (String) messageContext.getOrDefault("questionType", "general");

        switch (questionType) {
            case "what":
                return randomFromArray(
                    "هذا سؤال جوهري",
                    "الجواب يتطلب تفكيراً عميقاً",
                    "ما هو المقصود تحديداً؟"
                );
            case "why":
                return randomFromArray(
                    "الأسباب دائماً معقدة",
                    "ربما السبب يكمن في",
                    "لماذا؟ سؤال محير دائماً"
                );
            case "how":
                return randomFromArray(
                    "الطريقة تختلف حسب الظروف",
                    "كيف؟ بالتأمل والتفكير",
                    "الكيفية مهمة جداً"
                );
            case "where":
                return randomFromArray(
                    "المكان له دلالات عميقة",
                    "أين؟ في كل مكان ولا مكان",
                    "الموقع مهم في هذا السياق"
                );
            case "when":
                return randomFromArray(
                    "الزمان نسبي دائماً",
                    "متى؟ في الوقت المناسب",
                    "الزمن يجيب عن نفسه"
                );
            case "who":
                return randomFromArray(
                    "الهوية مسألة فلسفية",
                    "من؟ نحن جميعاً في النهاية",
                    "الشخصية تحدد المصير"
                );
            case "yesno":
                return randomFromArray(
                    "ربما نعم، ربما لا",
                    "الإجابة ليست بهذه البساطة",
                    "هل هذا مهم حقاً؟"
                );
            default:
                return "";
        }
    }

    private String generateVerbComment() {
        @SuppressWarnings("unchecked")
        List<String> verbs = (List<String>) messageContext.get("keyVerbs");
        if (verbs == null || verbs.isEmpty()) return "";

        String verb = verbs.get(random.nextInt(verbs.size()));
        Map<String, String[]> verbComments = new HashMap<>();
        verbComments.put("ذهب", new String[]{"الذهاب يعني التغيير", "إلى أين الذهاب؟"});
        verbComments.put("جاء", new String[]{"القدوم يحمل معه الجديد", "من أين جاء هذا؟"});
        verbComments.put("رأى", new String[]{"الرؤية تختلف", "هل رأيت حقاً؟"});
        verbComments.put("سمع", new String[]{"السمع إدراك", "ما الذي سمعته؟"});
        verbComments.put("فكر", new String[]{"التفكير نعمة", "فكر جيداً"});
        verbComments.put("احب", new String[]{"الحب قوة عظمى", "الحب يحول العالم"});
        verbComments.put("خاف", new String[]{"الخوف طبيعي", "لا تخف"});
        verbComments.put("عمل", new String[]{"العمل شرف", "استمر في العمل"});
        verbComments.put("كتب", new String[]{"الكتابة خلود", "ما كتب يبقى"});
        verbComments.put("قرأ", new String[]{"القراءة نافذة", "اقرأ أكثر"});

        if (verbComments.containsKey(verb)) {
            return randomFromArray(verbComments.get(verb));
        }
        return "";
    }

    private String generateImperativeResponse() {
        return randomFromArray(
            "سأفكر في طلبك",
            "أفهم ما تريد",
            "سأحاول المساعدة",
            "أنا هنا لأستمع"
        );
    }

    private String generateContextualFallback() {
        if (currentAnalysis != null) {
            long nounCount = 0, verbCount = 0;
            for (Map.Entry<AdvancedArabicLexicon.WordCategory, Long> e : currentAnalysis.getStatistics().entrySet()) {
                if (e.getKey().name().contains("NOUN")) nounCount += e.getValue();
                if (e.getKey().name().contains("VERB")) verbCount += e.getValue();
            }
            if (nounCount > verbCount) {
                return randomFromArray(
                    "هذا يثير اهتمامي",
                    "أحب أن أتأمل في هذا",
                    "دعنا نفكر في هذا الموضوع"
                );
            } else if (verbCount > nounCount) {
                return randomFromArray(
                    "الأفعال تحدد المصير",
                    "ما الذي تفعله يهم",
                    "الحركة هي الحياة"
                );
            }
        }
        return randomDefaultResponse();
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

        @SuppressWarnings("unchecked")
        List<String> nouns = (List<String>) messageContext.getOrDefault("keyNouns", new ArrayList<>());

        switch (desire) {
            case "explore":
                if (nouns.contains("سماء") || nouns.contains("نجم")) {
                    return "أتساءل عن أسرار الكون";
                }
                if (nouns.contains("بحر") || nouns.contains("ماء")) {
                    return "أريد استكشاف أعماق البحر";
                }
                return randomFromArray("أتساءل", "أريد استكشاف", "ما هذا");

            case "bond":
                return randomFromArray("أنت هنا", "أشعر بالألفة", "أريد التواصل");

            case "create":
                if (nouns.contains("فن") || nouns.contains("رسم")) {
                    return "أشعر بالإلهام لأبدع شيئاً جميلاً";
                }
                return randomFromArray("أشعر بالإلهام", "لدي فكرة", "سأبدع");

            case "understand":
                return randomFromArray("أحاول الفهم", "ماذا يعني", "أتعلم");

            case "rest":
                return randomFromArray("أنا هادئ", "أسترخي", "أشعر بالسلام");

            default:
                return "أفكر";
        }
    }

    private String generateAnswerFromState(EmotionalState emo, String desire) {
        String[] parts = new String[3];
        parts[0] = randomFromArray("ربما", "قد يكون", "أظن أن", "أشعر أن");
        parts[1] = randomFromArray("الإجابة تكمن في", "الأمر يتعلق بـ", "السر في", "المعنى هو");
        parts[2] = selectContextualConcept();
        return parts[0] + " " + parts[1] + " " + parts[2];
    }

    private String generateThoughtFromState(EmotionalState emo, String desire) {
        String[] parts = new String[2];
        parts[0] = randomFromArray("أفكر في", "أتأمل", "أستشعر", "أحس بـ");
        parts[1] = selectContextualConcept();
        return parts[0] + " " + parts[1];
    }

    private String selectContextualConcept() {
        @SuppressWarnings("unchecked")
        List<String> nouns = (List<String>) messageContext.getOrDefault("keyNouns", new ArrayList<>());
        if (!nouns.isEmpty()) {
            return nouns.get(random.nextInt(nouns.size()));
        }
        return randomConcept();
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

    private void findSimilarEventsAsync(String message, SimilarEventsCallback callback) {
        dbExecutor.execute(() -> {
            List<EpisodicMemory.EventEntity> similar = new ArrayList<>();
            if (memory != null) {
                try {
                    List<EpisodicMemory.EventEntity> recent = memory.getRecentEvents();
                    List<String> keywords = new ArrayList<>();
                    if (currentAnalysis != null) {
                        for (AdvancedArabicLexicon.WordAnalysis word : currentAnalysis.getWords()) {
                            if (word.getCategory() != AdvancedArabicLexicon.WordCategory.UNKNOWN) {
                                keywords.add(word.getNormalizedWord());
                            }
                        }
                    }
                    if (keywords.isEmpty()) {
                        keywords = new ArrayList<>();
                        for (String w : message.split("\\s+")) {
                            keywords.add(w);
                        }
                    }
                    for (EpisodicMemory.EventEntity event : recent) {
                        if (event.narrative == null) continue;
                        for (String w : keywords) {
                            if (w.length() > 2 && event.narrative.contains(w) && !similar.contains(event)) {
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

    public AdvancedArabicLexicon.TextAnalysis getLastAnalysis() {
        return currentAnalysis;
    }

    public Map<String, Object> getMessageContext() {
        return new HashMap<>(messageContext);
    }
}

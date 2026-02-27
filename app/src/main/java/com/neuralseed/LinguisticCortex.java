package com.neuralseed;

import android.content.Context;
import android.util.Log;
import java.util.*;

/**
 * القشرة اللغوية المتكاملة - النسخة النهائية المتوافقة
 * ربط جميع مكونات المعالجة اللغوية
 */
public class LinguisticCortex {
    
    // المكونات الأساسية
    private ArabicLexicon lexicon;
    private ArabicParser parser;
    private SemanticEmotionalEngine emotionEngine;
    private LearningSystem learningSystem;
    private SentenceGenerator sentenceGenerator;
    private LocalDatabase database;
    private FirebaseManager firebaseManager;
    
    // حالة التعلم والمزامنة
    private boolean isLearningEnabled = true;
    private boolean isSyncEnabled = false;
    
    // سياق المحادثة
    private List<String> conversationContext = new ArrayList<>();
    private Map<String, Object> sessionMemory = new HashMap<>();
    
    // مستمعي الأحداث
    public interface LinguisticListener {
        void onWordLearned(String word, String meaning);
        void onSentenceCorrected(String original, String corrected);
        void onEmotionDetected(String emotion, double intensity);
        void onNewConceptLearned(String concept);
    }
    
    private LinguisticListener listener;

    public LinguisticCortex() {
        this.lexicon = new ArabicLexicon();
        this.parser = new ArabicParser(lexicon);
        this.emotionEngine = new SemanticEmotionalEngine();
    }
    
    /**
     * تهيئة قاعدة البيانات المحلية
     */
    public void initializeDatabase(Context context) {
        this.database = new LocalDatabase(context);
        this.learningSystem = new LearningSystem(lexicon, parser, emotionEngine, database);
        this.sentenceGenerator = new SentenceGenerator(lexicon, parser, emotionEngine, database);
        
        loadSavedData();
    }
    
    /**
     * تهيئة Firebase
     */
    public void initializeFirebase(Context context) {
        try {
            this.firebaseManager = new FirebaseManager(context);
            
            // الاستماع للكلمات الجديدة
            firebaseManager.listenToNewWords((word, meaning) -> {
                if (!lexicon.contains(word)) {
                    lexicon.addWord(word, meaning);
                    if (listener != null) listener.onWordLearned(word, meaning);
                }
            });
            
            this.isSyncEnabled = true;
        } catch (Exception e) {
            Log.e("LinguisticCortex", "Firebase error: " + e.getMessage());
        }
    }

    /**
     * معالجة المدخلات مع الاستنتاج التلقائي
     */
    public ProcessedInput processInput(String input, NeuralSeed.InternalState state) {
        ProcessedInput result = new ProcessedInput();
        result.originalText = input;
        result.timestamp = System.currentTimeMillis();
        
        // 1. تحليل العواطف
        result.detectedEmotions = emotionEngine.analyzeEmotions(input);
        result.dominantEmotion = emotionEngine.getDominantEmotion(result.detectedEmotions);
        
        // 2. التعلم التلقائي من الأنماط
        detectAndLearnFromPattern(input, state);

        // 3. التحليل اللغوي
        result.parseResults = parser.parseText(input);
        result.keywords = parser.extractKeywords(input);
        
        // 4. تحديث السياق
        updateContext(input);
        
        // 5. إشعار بالعاطفة
        if (listener != null && result.dominantEmotion != null) {
            double intensity = result.detectedEmotions.getOrDefault(result.dominantEmotion, 0.5);
            listener.onEmotionDetected(result.dominantEmotion, intensity);
        }
        
        return result;
    }

    /**
     * كشف الأنماط للتعلم التلقائي
     */
    private void detectAndLearnFromPattern(String input, NeuralSeed.InternalState state) {
        if (!isLearningEnabled) return;

        String pattern = "";
        if (input.contains(" هو ")) pattern = " هو ";
        else if (input.contains(" هي ")) pattern = " هي ";
        else if (input.contains(" يعني ")) pattern = " يعني ";

        if (!pattern.isEmpty()) {
            String[] parts = input.split(pattern);
            if (parts.length >= 2) {
                String word = parts[0].trim();
                String meaning = parts[1].trim();
                
                learnMeaning(word, meaning, "automatic");
                
                if (state != null) {
                    if (state.currentPhase == NeuralSeed.Phase.STABLE) {
                        state.narrative = "أضفت " + word + " إلى معجمي.";
                        state.existentialFitness += 0.01;
                    } else if (state.currentPhase == NeuralSeed.Phase.CHAOTIC) {
                        state.narrative = "كلمة " + word + " تثير تساؤلاتي...";
                    }
                }
            }
        }
    }

    /**
     * تعلم معنى جديد
     */
    public void learnMeaning(String word, String meaning, String source) {
        // 1. إضافة للمعجم
        lexicon.addWord(word, meaning);
        
        // 2. حفظ محلياً
        if (database != null) {
            ArabicLexicon.Word w = new ArabicLexicon.Word(word);
            w.meanings.add(meaning);
            database.saveWord(w);
        }
        
        // 3. مزامنة مع Firebase
        if (isSyncEnabled && firebaseManager != null) {
            firebaseManager.saveWord(word, meaning);
        }
        
        // 4. إشعار
        if (listener != null) {
            listener.onWordLearned(word, meaning);
        }
    }

    /**
     * توليد رد ذكي
     */
    public GeneratedResponse generateResponse(String input, NeuralSeed.InternalState state) {
        GeneratedResponse response = new GeneratedResponse();
        
        if (sentenceGenerator != null) {
            SentenceGenerator.Response generated = sentenceGenerator.generateResponse(input, state);
            response.text = generated.text;
            response.confidence = generated.confidence;
            response.emotions = generated.emotions;
        } else {
            response.text = generateDefaultResponse(input);
            response.confidence = 0.5;
        }
        
        // حفظ المحادثة
        if (database != null) {
            database.saveConversation(input, response.text, response.emotions, "ai");
        }
        
        if (isSyncEnabled && firebaseManager != null) {
            firebaseManager.saveConversation(input, response.text, System.currentTimeMillis());
        }
        
        return response;
    }

    private String generateDefaultResponse(String input) {
        if (input.contains("مرحبا")) return "أهلاً بك! أنا هنا.";
        if (input.contains("؟")) return "سؤالك يثير اهتمامي...";
        return "أنا أسمعك... واصل.";
    }

    /**
     * تعلم من تصحيح
     */
    public void learnFromCorrection(String original, String corrected, String explanation) {
        if (learningSystem != null) {
            learningSystem.learnFromCorrection(original, corrected, explanation);
        }
        
        if (listener != null) {
            listener.onSentenceCorrected(original, corrected);
        }
    }

    /**
     * تعلم جملة
     */
    public void learnSentence(String sentence, NeuralSeed.InternalState state) {
        if (learningSystem != null) {
            learningSystem.learnFromExample(sentence, "observed");
        }
        detectAndLearnFromPattern(sentence, state);
    }

    private void updateContext(String input) {
        conversationContext.add(input);
        if (conversationContext.size() > 10) conversationContext.remove(0);
    }

    private void loadSavedData() {
        if (database == null) return;
        
        List<ArabicLexicon.Word> savedWords = database.loadAllWords();
        for (ArabicLexicon.Word word : savedWords) {
            if (!word.meanings.isEmpty()) {
                lexicon.addWord(word.word, word.meanings.get(0));
            }
        }
    }

    // Getters
    public ArabicLexicon getLexicon() { return lexicon; }
    public ArabicParser getParser() { return parser; }
    public SemanticEmotionalEngine getEmotionEngine() { return emotionEngine; }
    
    public void setListener(LinguisticListener listener) { this.listener = listener; }
    public void setSyncEnabled(boolean enabled) { this.isSyncEnabled = enabled; }

    public String explainWord(String word) {
        ArabicLexicon.Word w = lexicon.getWord(word);
        if (w == null || w.meanings.isEmpty()) {
            return "لم أتعلم " + word + " بعد.";
        }
        return word + " تعني: " + String.join("، ", w.meanings);
    }

    public String generateQuestion(NeuralSeed.InternalState state) {
        if (sentenceGenerator != null) {
            return sentenceGenerator.generateQuestion(state);
        }
        return "ما رأيك في الوجود؟";
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("lexicon_size", lexicon.getWordCount());
        stats.put("context_depth", conversationContext.size());
        if (database != null) stats.putAll(database.getStatistics());
        if (learningSystem != null) stats.putAll(learningSystem.getStatistics());
        return stats;
    }

    // الكلاسات المساعدة
    public static class ProcessedInput {
        public String originalText;
        public long timestamp;
        public List<ArabicParser.ParseResult> parseResults;
        public Map<String, Double> detectedEmotions;
        public String dominantEmotion;
        public List<String> keywords;
    }
    
    public static class GeneratedResponse {
        public String text;
        public double confidence;
        public Map<String, Double> emotions;
    }
}

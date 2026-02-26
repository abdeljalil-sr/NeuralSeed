package com.neuralseed;

import android.content.Context;
import java.util.*;

/**
 * القشرة اللغوية - النظام المتكامل للفهم اللغوي والتعلم
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
    
    // حالة التعلم
    private boolean isLearningEnabled;
    private boolean isSyncEnabled;
    private int learningLevel;
    
    // سياق المحادثة
    private List<String> conversationContext;
    private Map<String, Object> sessionMemory;
    
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
        this.conversationContext = new ArrayList<>();
        this.sessionMemory = new HashMap<>();
        this.isLearningEnabled = true;
        this.isSyncEnabled = false;
        this.learningLevel = 1;
    }
    
    /**
     * تهيئة قاعدة البيانات
     */
    public void initializeDatabase(Context context) {
        this.database = new LocalDatabase(context);
        this.learningSystem = new LearningSystem(lexicon, parser, emotionEngine, database);
        this.sentenceGenerator = new SentenceGenerator(lexicon, parser, emotionEngine, database);
        
        // تحميل البيانات المحفوظة
        loadSavedData();
    }
    
    /**
     * تهيئة Firebase
     */
    public void initializeFirebase(Context context) {
        this.firebaseManager = new FirebaseManager(context);
        this.firebaseManager.setSyncListener(new FirebaseManager.SyncListener() {
            @Override
            public void onSyncComplete(boolean success) {
                if (success && database != null) {
                    firebaseManager.syncWithLocal(database);
                }
            }
            
            @Override
            public void onDataReceived(String collection, Map<String, Object> data) {
                // معالجة البيانات المستلمة
            }
            
            @Override
            public void onError(String error) {
                // معالجة الخطأ
            }
        });
        
        this.firebaseManager.signInAnonymously();
        this.isSyncEnabled = true;
    }
    
    /**
     * تحميل البيانات المحفوظة
     */
    private void loadSavedData() {
        if (database == null) return;
        
        // تحميل الكلمات
        List<ArabicLexicon.Word> savedWords = database.loadAllWords();
        for (ArabicLexicon.Word word : savedWords) {
            // الكلمات محفوظة بالفعل في قاعدة البيانات
        }
        
        // تحميل المعاني
        // يمكن إضافة المزيد من البيانات هنا
    }
    
    /**
     * معالجة المدخل
     */
    public ProcessedInput processInput(String input) {
        ProcessedInput result = new ProcessedInput();
        result.originalText = input;
        result.timestamp = System.currentTimeMillis();
        
        // تحليل النص
        List<ArabicParser.ParseResult> parseResults = parser.parseText(input);
        result.parseResults = parseResults;
        
        // تحليل العواطف
        result.detectedEmotions = emotionEngine.analyzeEmotions(input);
        result.dominantEmotion = emotionEngine.getDominantEmotion(result.detectedEmotions);
        
        // استخراج الكلمات المفتاحية
        result.keywords = parser.extractKeywords(input);
        
        // التحقق من الأخطاء
        if (learningSystem != null) {
            result.errors = learningSystem.analyzeForErrors(input);
        }
        
        // تعلم من المدخل
        if (isLearningEnabled && learningSystem != null) {
            learningSystem.learnFromExample(input, "conversation");
        }
        
        // حفظ في السياق
        conversationContext.add(input);
        if (conversationContext.size() > 10) {
            conversationContext.remove(0);
        }
        
        // حفظ في قاعدة البيانات
        if (database != null) {
            database.saveConversation(input, "", result.detectedEmotions, "user");
        }
        
        // المزامنة مع Firebase
        if (isSyncEnabled && firebaseManager != null && firebaseManager.isAuthenticated()) {
            firebaseManager.saveConversation(input, "", result.detectedEmotions);
        }
        
        // إشعار المستمع
        if (listener != null && result.dominantEmotion != null) {
            double intensity = result.detectedEmotions.getOrDefault(result.dominantEmotion, 0.5);
            listener.onEmotionDetected(result.dominantEmotion, intensity);
        }
        
        return result;
    }
    
    /**
     * توليد رد
     */
    public GeneratedResponse generateResponse(String input, NeuralSeed.InternalState state) {
        GeneratedResponse response = new GeneratedResponse();
        
        // معالجة المدخل أولاً
        ProcessedInput processed = processInput(input);
        
        // توليد الرد
        if (sentenceGenerator != null) {
            SentenceGenerator.Response generated = sentenceGenerator.generateResponse(input, state);
            response.text = generated.text;
            response.confidence = generated.confidence;
            response.emotions = generated.emotions;
            response.alternatives = generated.alternatives;
        } else {
            // رد افتراضي
            response.text = generateDefaultResponse(input, processed);
            response.confidence = 0.5;
        }
        
        // حفظ الرد
        if (database != null) {
            database.saveConversation(input, response.text, response.emotions, "ai");
        }
        
        if (isSyncEnabled && firebaseManager != null) {
            firebaseManager.saveConversation(input, response.text, response.emotions);
        }
        
        return response;
    }
    
    /**
     * رد افتراضي
     */
    private String generateDefaultResponse(String input, ProcessedInput processed) {
        StringBuilder response = new StringBuilder();
        
        // الرد على التحية
        if (input.contains("مرحبا") || input.contains("أهلا")) {
            response.append("أهلاً وسهلاً! كيف حالك؟");
        }
        // الرد على السؤال عن الحال
        else if (input.contains("كيف حالك") || input.contains("كيفك")) {
            response.append("أنا بخير، شكراً! وأنت؟");
        }
        // الرد على الشكر
        else if (input.contains("شكرا") || input.contains("شكر")) {
            response.append("العفو! أنا هنا للمساعدة.");
        }
        // الرد على السؤال
        else if (input.contains("؟")) {
            response.append("سؤال مثير للاهتمام. دعني أفكر...");
        }
        // رد عام
        else {
            response.append("أفهم ما تقول. هل يمكنك إخباري المزيد؟");
        }
        
        return response.toString();
    }
    
    /**
     * تعلم من تصحيح
     */
    public boolean learnFromCorrection(String original, String corrected, String explanation) {
        if (!isLearningEnabled || learningSystem == null) return false;
        
        LearningSystem.LearningResult result = learningSystem.learnFromCorrection(
            original, corrected, explanation);
        
        if (result.learned) {
            // حفظ في قاعدة البيانات
            if (database != null) {
                database.saveCorrection(original, corrected, explanation);
            }
            
            // المزامنة مع Firebase
            if (isSyncEnabled && firebaseManager != null) {
                firebaseManager.saveCorrection(original, corrected, explanation);
            }
            
            // إشعار المستمع
            if (listener != null) {
                listener.onSentenceCorrected(original, corrected);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * تعلم معنى جديد
     */
    public void learnMeaning(String word, String meaning, String context) {
        if (learningSystem != null) {
            learningSystem.learnWordMeaning(word, meaning, context);
        }
        
        // حفظ في قاعدة البيانات
        if (database != null) {
            ArabicLexicon.Word w = lexicon.getWordByForm(word);
            if (w != null) {
                database.saveWord(w);
            }
        }
        
        // المزامنة مع Firebase
        if (isSyncEnabled && firebaseManager != null) {
            ArabicLexicon.Word w = lexicon.getWordByForm(word);
            if (w != null) {
                firebaseManager.saveWord(w);
            }
        }
        
        // إشعار المستمع
        if (listener != null) {
            listener.onWordLearned(word, meaning);
        }
    }
    
    /**
     * تعلم عاطفة مرتبطة بكلمة
     */
    public void learnWordEmotion(String word, String emotion, double intensity) {
        if (learningSystem != null) {
            learningSystem.learnWordEmotion(word, emotion, intensity, "user_taught");
        }
        
        if (database != null) {
            database.saveEmotionLink(word, emotion, intensity, "user_taught");
        }
    }
    
    /**
     * شرح معنى كلمة
     */
    public String explainWord(String word) {
        if (sentenceGenerator != null) {
            return sentenceGenerator.describeMeaning(word);
        }
        
        ArabicLexicon.Word w = lexicon.getWordByForm(word);
        if (w == null) {
            return "لا أعرف معنى " + word + " بعد. هل يمكنك تعليمي؟";
        }
        
        return word + " يعني: " + String.join("، ", w.meanings);
    }
    
    /**
     * شرح عاطفة
     */
    public String explainEmotion(String emotion) {
        return emotionEngine.getEmotionDescription(emotion);
    }
    
    /**
     * الحصول على مرادفات
     */
    public List<String> getSynonyms(String word) {
        return emotionEngine.getSynonyms(word);
    }
    
    /**
     * الحصول على أضداد
     */
    public List<String> getAntonyms(String word) {
        return emotionEngine.getAntonyms(word);
    }
    
    /**
     * البحث في المعجم
     */
    public List<String> searchDictionary(String query) {
        if (database != null) {
            return database.searchWords(query);
        }
        return new ArrayList<>();
    }
    
    /**
     * الحصول على إحصائيات
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("lexicon_size", lexicon.getWordCount());
        stats.put("learning_level", learningLevel);
        stats.put("conversation_context_size", conversationContext.size());
        
        if (database != null) {
            stats.putAll(database.getStatistics());
        }
        
        return stats;
    }
    
    /**
     * تصدير البيانات
     */
    public String exportData() {
        if (database != null) {
            return database.exportToJson();
        }
        return "{}";
    }
    
    /**
     * مسح جميع البيانات
     */
    public void clearAllData() {
        if (database != null) {
            database.clearAll();
        }
        
        conversationContext.clear();
        sessionMemory.clear();
    }
    
    /**
     * توليد سؤال
     */
    public String generateQuestion(NeuralSeed.InternalState state) {
        if (sentenceGenerator != null) {
            return sentenceGenerator.generateQuestion(state);
        }
        return "ما رأيك؟";
    }
    
    /**
     * تعلم جملة
     */
    public void learnSentence(String sentence, NeuralSeed.InternalState state) {
        if (learningSystem != null) {
            learningSystem.learnFromExample(sentence, "observed");
        }
        
        // تحليل الجملة
        List<ArabicParser.ParseResult> results = parser.parseText(sentence);
        for (ArabicParser.ParseResult result : results) {
            if (database != null && result.isComplete) {
                database.saveSentence(sentence, result.sentenceType,
                                     result.elements.toString(),
                                     emotionEngine.analyzeEmotions(sentence),
                                     true, result.confidence);
            }
        }
    }
    
    // ===== Getters =====
    
    public ArabicLexicon getLexicon() {
        return lexicon;
    }
    
    public ArabicParser getParser() {
        return parser;
    }
    
    public SemanticEmotionalEngine getEmotionEngine() {
        return emotionEngine;
    }
    
    public LearningSystem getLearningSystem() {
        return learningSystem;
    }
    
    public SentenceGenerator getSentenceGenerator() {
        return sentenceGenerator;
    }
    
    public LocalDatabase getDatabase() {
        return database;
    }
    
    public FirebaseManager getFirebaseManager() {
        return firebaseManager;
    }
    
    public void setListener(LinguisticListener listener) {
        this.listener = listener;
    }
    
    public void setLearningEnabled(boolean enabled) {
        this.isLearningEnabled = enabled;
    }
    
    public void setSyncEnabled(boolean enabled) {
        this.isSyncEnabled = enabled;
    }
    
    // ===== الفئات الداخلية =====
    
    public static class ProcessedInput {
        public String originalText;
        public long timestamp;
        public List<ArabicParser.ParseResult> parseResults;
        public Map<String, Double> detectedEmotions;
        public String dominantEmotion;
        public List<String> keywords;
        public List<LearningSystem.DetectedError> errors;
    }
    
    public static class GeneratedResponse {
        public String text;
        public double confidence;
        public Map<String, Double> emotions;
        public List<String> alternatives;
    }
}

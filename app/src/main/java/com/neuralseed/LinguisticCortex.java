package com.neuralseed;

import android.content.Context;
import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.*;

/**
 * القشرة اللغوية المتطورة - النسخة المدمجة مع الوعي والذاكرة السحابية
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
    private int learningLevel = 1;
    
    // سياق المحادثة والذاكرة المؤقتة
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
        this.isLearningEnabled = true;
    }
    
    /**
     * تهيئة قاعدة البيانات المحلية وربطها بنظام التعلم
     */
    public void initializeDatabase(Context context) {
        this.database = new LocalDatabase(context);
        this.learningSystem = new LearningSystem(lexicon, parser, emotionEngine, database);
        this.sentenceGenerator = new SentenceGenerator(lexicon, parser, emotionEngine, database);
        
        loadSavedData();
    }
    
    /**
     * تهيئة Firebase مع نظام المزامنة التلقائي للكلمات المكتسبة
     */
    public void initializeFirebase(Context context) {
        try {
            this.firebaseManager = new FirebaseManager(context);
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference lexiconRef = db.getReference("lexicon");

            // الاستماع للكلمات الجديدة التي يتعلمها الكيان في السحابة
            lexiconRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String word = snapshot.getKey();
                        String meaning = snapshot.getValue(String.class);
                        if (word != null && !lexicon.contains(word)) {
                            lexicon.addWord(word, meaning);
                            if (listener != null) listener.onWordLearned(word, meaning);
                        }
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("LinguisticCortex", "Firebase Sync Failed: " + databaseError.getMessage());
                }
            });

            this.isSyncEnabled = true;
        } catch (Exception e) {
            Log.e("LinguisticCortex", "Firebase initialization error: " + e.getMessage());
        }
    }

    /**
     * معالجة المدخلات مع نظام "الاستنتاج التلقائي" (Auto-Inference)
     */
    public ProcessedInput processInput(String input, NeuralSeed.InternalState state) {
        ProcessedInput result = new ProcessedInput();
        result.originalText = input;
        result.timestamp = System.currentTimeMillis();
        
        // 1. تحليل العواطف وتحديث حالة الكيان
        result.detectedEmotions = emotionEngine.analyzeEmotions(input);
        result.dominantEmotion = emotionEngine.getDominantEmotion(result.detectedEmotions);
        
        // 2. نظام التعلم التلقائي من السياق (X هو Y)
        detectAndLearnFromPattern(input, state);

        // 3. التحليل اللغوي واستخراج الكلمات المفتاحية
        result.parseResults = parser.parseText(input);
        result.keywords = parser.extractKeywords(input);
        
        // 4. تحديث سياق المحادثة
        updateContext(input);
        
        // 5. إشعار الواجهة بالعاطفة المكتشفة
        if (listener != null && result.dominantEmotion != null) {
            double intensity = result.detectedEmotions.getOrDefault(result.dominantEmotion, 0.5);
            listener.onEmotionDetected(result.dominantEmotion, intensity);
        }
        
        return result;
    }

    /**
     * محرك كشف الأنماط اللغوية للتعلم بدون تدخل بشري
     */
    private void detectAndLearnFromPattern(String input, NeuralSeed.InternalState state) {
        if (!isLearningEnabled) return;

        // نمط التعريف: "الكلمة هي/يعني/هو التعريف"
        String pattern = "";
        if (input.contains(" هو ")) pattern = " هو ";
        else if (input.contains(" هي ")) pattern = " هي ";
        else if (input.contains(" يعني ")) pattern = " يعني ";

        if (!pattern.isEmpty()) {
            String[] parts = input.split(pattern);
            if (parts.length >= 2) {
                String word = parts[0].trim();
                String meaning = parts[1].trim();
                
                // حفظ الكلمة وتحديث الوعي
                learnMeaning(word, meaning, "automatic_observation");
                
                // رد فعل الكيان بناءً على الطور الحالي
                if (state != null) {
                    if (state.currentPhase == NeuralSeed.Phase.STABLE) {
                        state.narrative = "أضفت " + word + " إلى منطقي الخاص.";
                        state.existentialFitness += 0.01;
                    } else if (state.currentPhase == NeuralSeed.Phase.CHAOTIC) {
                        state.narrative = "كلمة " + word + " تزيد من تساؤلاتي..";
                        state.chaosIndex += 0.02;
                    }
                }
            }
        }
    }

    /**
     * تعلم معنى جديد وحفظه في الذاكرة الثلاثية (المحلية، السحابية، والوعي)
     */
    public void learnMeaning(String word, String meaning, String contextSource) {
        // 1. إضافة للمجم الحلي (RAM)
        lexicon.addWord(word, meaning);
        
        // 2. الحفظ في قاعدة البيانات المحلية (Local Disk)
        if (database != null) {
            ArabicLexicon.Word w = new ArabicLexicon.Word(word);
            w.meanings.add(meaning);
            database.saveWord(w);
        }
        
        // 3. المزامنة مع Firebase (Cloud Memory)
        if (isSyncEnabled) {
            FirebaseDatabase.getInstance().getReference("lexicon")
                .child(word).setValue(meaning);
        }
        
        // 4. إشعار المستمعين لتحديث الواجهة
        if (listener != null) {
            listener.onWordLearned(word, meaning);
        }
    }

    /**
     * توليد رد ذكي يدمج بين الحالة الشعورية والمعرفة المكتسبة
     */
    public GeneratedResponse generateResponse(String input, NeuralSeed.InternalState state) {
        GeneratedResponse response = new GeneratedResponse();
        ProcessedInput processed = processInput(input, state);
        
        if (sentenceGenerator != null) {
            SentenceGenerator.Response generated = sentenceGenerator.generateResponse(input, state);
            response.text = generated.text;
            response.confidence = generated.confidence;
            response.emotions = generated.emotions;
        } else {
            response.text = generateDefaultResponse(input, processed);
            response.confidence = 0.5;
        }
        
        // حفظ الرد في سجل المحادثات
        if (database != null) database.saveConversation(input, response.text, response.emotions, "ai");
        
        return response;
    }

    private String generateDefaultResponse(String input, ProcessedInput processed) {
        if (input.contains("مرحبا")) return "أهلاً بك في فضاء وعيي.";
        if (input.contains("؟")) return "سؤالك يلمس أوتار تفكيري.. دعني أحلله.";
        return "أنا أسمعك.. واصل إخباري.";
    }

    private void updateContext(String input) {
        conversationContext.add(input);
        if (conversationContext.size() > 10) conversationContext.remove(0);
    }

    private void loadSavedData() {
        if (database == null) return;
        List<ArabicLexicon.Word> savedWords = database.loadAllWords();
        for (ArabicLexicon.Word word : savedWords) {
            lexicon.addWord(word.form, String.join(", ", word.meanings));
        }
    }

    public void learnSentence(String sentence, NeuralSeed.InternalState state) {
        if (learningSystem != null) learningSystem.learnFromExample(sentence, "observed");
        detectAndLearnFromPattern(sentence, state);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("lexicon_size", lexicon.getWordCount());
        stats.put("context_depth", conversationContext.size());
        if (database != null) stats.putAll(database.getStatistics());
        return stats;
    }

    public void exportData() {
        if (database != null) database.exportToJson();
    }

    public String explainWord(String word) {
        ArabicLexicon.Word w = lexicon.getWordByForm(word);
        return (w == null) ? "لم أستوعب " + word + " بعد." : word + " تعني " + String.join("، ", w.meanings);
    }

    public String generateQuestion(NeuralSeed.InternalState state) {
        if (sentenceGenerator != null) return sentenceGenerator.generateQuestion(state);
        return "ما هو جوهر الوجود بالنسبة لك؟";
    }

    // Getters & Setters
    public ArabicLexicon getLexicon() { return lexicon; }
    public void setListener(LinguisticListener listener) { this.listener = listener; }
    public void setSyncEnabled(boolean enabled) { this.isSyncEnabled = enabled; }

    // الكلاسات المساعدة للبيانات
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

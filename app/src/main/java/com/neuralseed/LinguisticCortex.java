package com.neuralseed;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class LinguisticCortex {
    
    public interface LinguisticListener {
        void onWordLearned(String word, String meaning, String context);
        void onSentenceCorrected(String original, String corrected);
        void onEmotionDetected(String emotion, double intensity);
        void onNewConceptLearned(String concept, String definition);
        void onRelationshipLearned(String subject, String relationship, String object);
    }
    
    private LinguisticListener listener;
    private Lexicon lexicon = new Lexicon();
    private KnowledgeGraph knowledgeGraph = new KnowledgeGraph();
    private Context appContext;
    private static final String PREFS_NAME = "NeuralSeedKnowledge";
    private static final String TAG = "LinguisticCortex";
    
    public void initializeDatabase(Context context) {
        this.appContext = context;
        loadKnowledge();
    }
    
    public void setListener(LinguisticListener listener) {
        this.listener = listener;
    }
    
    public ProcessedInput processInput(String text) {
        ProcessedInput input = new ProcessedInput(text);
        input.emotion = detectEmotion(text);
        input.intensity = 0.5;
        input.extractedConcepts = extractConceptsSimple(text);
        return input;
    }
    
    public GeneratedResponse generateResponse(String userInput, NeuralSeed.InternalState state) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return new GeneratedResponse("لم أفهم ما قلت");
        }
        
        // تعلم بسيط بدون regex
        learnSimple(userInput);
        
        // رد بسيط
        if (userInput.contains("؟") || userInput.contains("ما")) {
            return answerQuestionSimple(userInput);
        }
        
        return new GeneratedResponse("فهمت ما قلت. شكراً لتعليمي!");
    }
    
    public String generateQuestion(NeuralSeed.InternalState state) {
        return "هل يمكنك أن تعلمني شيئاً جديداً؟";
    }
    
    public void learnSentence(String sentence, NeuralSeed.InternalState state) {
        learnSimple(sentence);
        saveKnowledge();
    }
    
    private void learnSimple(String text) {
        // تعلم بسيط: البحث عن "هي" أو "هو"
        if (text.contains("هي") || text.contains("هو")) {
            String[] parts = text.split("(?:هي|هو)", 2);
            if (parts.length == 2) {
                String concept = parts[0].trim();
                String definition = parts[1].trim();
                
                if (!concept.isEmpty() && !definition.isEmpty()) {
                    lexicon.addWord(concept, definition, "مفهوم", text);
                    
                    if (listener != null) {
                        listener.onWordLearned(concept, definition, text);
                        listener.onNewConceptLearned(concept, definition);
                    }
                    
                    Log.d(TAG, "تعلمت: " + concept + " = " + definition);
                }
            }
        }
    }
    
    private GeneratedResponse answerQuestionSimple(String question) {
        // إزالة كلمات السؤال
        String cleaned = question.replace("ما", "").replace("هي", "").replace("هو", "")
                                .replace("؟", "").trim();
        
        if (lexicon.hasWord(cleaned)) {
            Definition def = lexicon.getDefinition(cleaned);
            return new GeneratedResponse(cleaned + " " + def.meaning);
        }
        
        return new GeneratedResponse("لم أتعلم بعد عن " + cleaned + ". هل يمكنك أن تعلمني؟");
    }
    
    private List<String> extractConceptsSimple(String text) {
        List<String> concepts = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            word = word.trim();
            if (word.length() > 2 && !isCommonWord(word)) {
                concepts.add(word);
            }
        }
        
        return concepts;
    }
    
    private boolean isCommonWord(String word) {
        Set<String> common = new HashSet<>(Arrays.asList(
            "هي", "هو", "التي", "الذي", "في", "من", "إلى", "على", "هذا", "هذه",
            "أن", "أو", "و", "مع", "عن", "كان", "يكون"
        ));
        return common.contains(word);
    }
    
    private String detectEmotion(String text) {
        if (text.contains("سعيد") || text.contains("فرح")) return "joy";
        if (text.contains("حزين") || text.contains("سيئ")) return "sadness";
        return "neutral";
    }
    
    public void learnMeaning(String word, String meaning, String source) {
        if (word == null || meaning == null) return;
        lexicon.addWord(word.trim(), meaning.trim(), "مفهوم", source);
        saveKnowledge();
    }
    
    public void learnWordEmotion(String word, String emotion, double intensity) {
        // مبسط
    }
    
    public boolean learnFromCorrection(String original, String corrected, String explanation) {
        if (listener != null) {
            listener.onSentenceCorrected(original, corrected);
        }
        return true;
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("lexicon_size", lexicon.getWordCount());
        stats.put("learning_level", calculateLearningLevel());
        stats.put("word_count", lexicon.getWordCount());
        return stats;
    }
    
    public Lexicon getLexicon() {
        return lexicon;
    }
    
    private String calculateLearningLevel() {
        int words = lexicon.getWordCount();
        if (words < 10) return "مبتدئ";
        if (words < 50) return "متعلم";
        return "متوسط";
    }
    
    private void saveKnowledge() {
        if (appContext == null) return;
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("knowledge", lexicon.toSimpleString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving", e);
        }
    }
    
    private void loadKnowledge() {
        if (appContext == null) return;
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String data = prefs.getString("knowledge", null);
            if (data != null) {
                lexicon.fromSimpleString(data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading", e);
        }
    }
    
    // ===== Classes =====
    
    public static class ProcessedInput {
        public String text;
        public String emotion;
        public double intensity;
        public List<String> extractedConcepts = new ArrayList<>();
        
        public ProcessedInput(String text) {
            this.text = text != null ? text : "";
        }
    }
    
    public static class GeneratedResponse {
        public String text;
        public double confidence;
        
        public GeneratedResponse(String text) {
            this.text = text != null ? text : "";
            this.confidence = 0.8;
        }
    }
    
    public static class Definition {
        public String meaning;
        public String category;
        public String source;
        public long timestamp;
        
        public Definition(String meaning, String category, String source) {
            this.meaning = meaning != null ? meaning : "";
            this.category = category != null ? category : "";
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static class Lexicon {
        private Map<String, Definition> words = new HashMap<>();
        
        public void addWord(String word, String meaning, String category, String source) {
            if (word == null || word.isEmpty()) return;
            words.put(word, new Definition(meaning, category, source));
        }
        
        public boolean hasWord(String word) {
            return word != null && words.containsKey(word);
        }
        
        public Definition getDefinition(String word) {
            return word != null ? words.get(word) : null;
        }
        
        public int getWordCount() {
            return words.size();
        }
        
        public String toSimpleString() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Definition> e : words.entrySet()) {
                sb.append(e.getKey()).append("=").append(e.getValue().meaning).append(";");
            }
            return sb.toString();
        }
        
        public void fromSimpleString(String data) {
            words.clear();
            String[] entries = data.split(";");
            for (String entry : entries) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    addWord(parts[0], parts[1], "مفهوم", "saved");
                }
            }
        }
    }
    
    public static class KnowledgeGraph {
        // مبسط
        public int getConceptsCount() { return 0; }
        public int getRelationshipsCount() { return 0; }
    }
}

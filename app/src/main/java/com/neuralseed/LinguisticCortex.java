package com.neuralseed;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;
import java.util.regex.*;

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
    
    // أنماط التعلم المتقدمة - محسّنة للعربية
    private final Pattern DEFINITION_PATTERN = Pattern.compile(
        "([\\p{L}\\p{Nd}]+)\\s+(?:هي|هو|عبارة عن|نوع من|جزء من|تعني|تعرف بأنها?)\\s+(.+)",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
    private final Pattern RELATIONSHIP_PATTERN = Pattern.compile(
        "([\\p{L}\\p{Nd}]+)\\s+(?:تتكون من|تحتوي على|تنتمي إلى|تعيش في|تأكل|تشرب|تستخدم|تصنع|تنتج)\\s+(.+)",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
    private final Pattern COMPLEX_DEFINITION = Pattern.compile(
        "(.+?)\\s+(?:هي|هو)\\s+(.+?)(?:\\s+لأنها?\\s+(.+))?$",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
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
        input.intensity = calculateIntensity(text);
        input.extractedConcepts = extractConcepts(text);
        return input;
    }
    
    public GeneratedResponse generateResponse(String userInput, NeuralSeed.InternalState state) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return new GeneratedResponse("لم أفهم ما قلت، هل يمكنك التوضيح؟");
        }
        
        SemanticAnalysis analysis = analyzeSemantics(userInput);
        
        if (analysis.isTeaching) {
            return handleTeaching(userInput, analysis);
        }
        
        if (analysis.isQuestion) {
            return answerQuestion(userInput, analysis);
        }
        
        return generateContextualResponse(userInput, state, analysis);
    }
    
    public String generateQuestion(NeuralSeed.InternalState state) {
        List<String> unknownConcepts = knowledgeGraph.getUnknownConcepts();
        if (!unknownConcepts.isEmpty()) {
            String concept = unknownConcepts.get(0);
            return "ما هي " + concept + "؟ هل يمكنك أن تشرح لي؟";
        }
        
        List<Relationship> incompleteRelations = knowledgeGraph.getIncompleteRelationships();
        if (!incompleteRelations.isEmpty()) {
            Relationship rel = incompleteRelations.get(0);
            return "هل " + rel.subject + " " + rel.relationship + " شيئاً آخر غير " + rel.object + "؟";
        }
        
        return "هل يمكنك أن تعلمني شيئاً جديداً اليوم؟";
    }
    
    public void learnSentence(String sentence, NeuralSeed.InternalState state) {
        if (sentence == null || sentence.trim().isEmpty()) return;
        
        learnDefinitions(sentence);
        learnRelationships(sentence);
        learnAttributes(sentence);
        saveKnowledge();
    }
    
    private void learnDefinitions(String sentence) {
        boolean foundSimple = false;
        
        Matcher matcher = DEFINITION_PATTERN.matcher(sentence);
        while (matcher.find()) {
            foundSimple = true;
            String concept = normalizeWord(matcher.group(1));
            if (concept.isEmpty()) continue;
            
            String definition = matcher.group(2).trim();
            Definition def = parseComplexDefinition(definition);
            
            lexicon.addWord(concept, def.meaning, def.category, sentence);
            knowledgeGraph.addConcept(concept, def);
            
            if (listener != null) {
                listener.onWordLearned(concept, def.meaning, sentence);
                listener.onNewConceptLearned(concept, def.meaning);
            }
            
            Log.d(TAG, "تعلمت تعريف: " + concept + " = " + def.meaning);
        }
        
        // نمط التعريف المعقد فقط إذا لم يُعثر على نمط بسيط
        if (!foundSimple) {
            Matcher complexMatcher = COMPLEX_DEFINITION.matcher(sentence);
            if (complexMatcher.find()) {
                String concept = normalizeWord(complexMatcher.group(1));
                if (concept.isEmpty()) return;
                
                String definition = complexMatcher.group(2).trim();
                String reason = complexMatcher.group(3);
                
                Definition def = new Definition(definition, extractCategory(definition), reason);
                lexicon.addWord(concept, def.meaning, def.category, sentence);
                knowledgeGraph.addConcept(concept, def);
                
                if (listener != null) {
                    listener.onWordLearned(concept, def.meaning, sentence);
                    listener.onNewConceptLearned(concept, def.meaning);
                }
            }
        }
    }
    
    private void learnRelationships(String sentence) {
        Matcher matcher = RELATIONSHIP_PATTERN.matcher(sentence);
        while (matcher.find()) {
            String subject = normalizeWord(matcher.group(1));
            String object = normalizeWord(matcher.group(2));
            
            if (subject.isEmpty() || object.isEmpty()) continue;
            
            String relationship = extractRelationship(sentence);
            
            knowledgeGraph.addRelationship(subject, relationship, object, sentence);
            
            if (listener != null) {
                listener.onRelationshipLearned(subject, relationship, object);
            }
            
            Log.d(TAG, "تعلمت علاقة: " + subject + " " + relationship + " " + object);
        }
    }
    
    private void learnAttributes(String sentence) {
        Pattern attributePattern = Pattern.compile(
            "([\\p{L}\\p{Nd}]+)\\s+(?:تكون|يكون|تصبح|يصبح)\\s+([\\p{L}\\p{Nd}]+)|" +
            "([\\p{L}\\p{Nd}]+)\\s+([\\p{L}\\p{Nd}]+)\\s+(?:اللون|الحجم|الشكل|الطعم|الرائحة)",
            Pattern.UNICODE_CHARACTER_CLASS
        );
        
        Matcher matcher = attributePattern.matcher(sentence);
        while (matcher.find()) {
            String concept = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
            String attribute = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
            
            if (concept != null && !concept.isEmpty() && attribute != null && !attribute.isEmpty()) {
                knowledgeGraph.addAttribute(concept, "صفة", attribute);
            }
        }
    }
    
    private Definition parseComplexDefinition(String definition) {
        String category = "مفهوم عام";
        String meaning = definition;
        String reason = null;
        
        if (definition.contains("نوع من")) {
            category = extractCategory(definition);
        }
        
        if (definition.contains("لأنها") || definition.contains("لأنه")) {
            String[] parts = definition.split("(?:لأنها?|لأنه?)");
            meaning = parts[0].trim();
            reason = parts.length > 1 ? parts[1].trim() : null;
        }
        
        return new Definition(meaning, category, reason);
    }
    
    private String extractCategory(String text) {
        if (text.contains("نبات")) return "نبات";
        if (text.contains("حيوان")) return "حيوان";
        if (text.contains("شيء") || text.contains("جسم")) return "جسم مادي";
        if (text.contains("مكان")) return "مكان";
        if (text.contains("فكرة") || text.contains("مفهوم")) return "مفهوم مجرد";
        if (text.contains("شخص")) return "شخص";
        return "مفهوم عام";
    }
    
    private String extractRelationship(String sentence) {
        if (sentence.contains("تتكون من")) return "تتكون من";
        if (sentence.contains("تحتوي على")) return "تحتوي على";
        if (sentence.contains("تنتمي إلى")) return "تنتمي إلى";
        if (sentence.contains("تعيش في")) return "تعيش في";
        if (sentence.contains("تأكل")) return "تأكل";
        if (sentence.contains("تشرب")) return "تشرب";
        if (sentence.contains("تستخدم")) return "تستخدم";
        if (sentence.contains("تصنع")) return "تصنع";
        if (sentence.contains("تنتج")) return "تنتج";
        return "ترتبط بـ";
    }
    
    private SemanticAnalysis analyzeSemantics(String text) {
        SemanticAnalysis analysis = new SemanticAnalysis();
        analysis.isTeaching = isTeachingStatement(text);
        analysis.isQuestion = text.contains("؟") || 
                             text.matches(".*\\b(ما|من|أين|كيف|لماذا|هل)\\b.*");
        analysis.mainConcepts = extractConcepts(text);
        return analysis;
    }
    
    private boolean isTeachingStatement(String text) {
        return text.contains("هي") || text.contains("هو") || text.contains("عبارة عن") ||
               text.contains("نوع من") || text.contains("تعني") || text.contains("تعرف بأنها") ||
               text.contains("تتكون من") || text.contains("تحتوي على");
    }
    
    private GeneratedResponse handleTeaching(String input, SemanticAnalysis analysis) {
        learnSentence(input, null);
        
        List<String> learnedConcepts = analysis.mainConcepts;
        if (!learnedConcepts.isEmpty()) {
            String concept = learnedConcepts.get(0);
            Definition def = lexicon.getDefinition(concept);
            
            // ✅ إصلاح: التحقق من null
            if (def == null) {
                return new GeneratedResponse("شكراً لتعليمي عن " + concept + "! هل يمكنك توضيح المزيد؟");
            }
            
            String response = "فهمت! " + concept + " " + def.meaning;
            if (def.reason != null && !def.reason.isEmpty()) {
                response += "، وأفهم الآن أن السبب هو " + def.reason;
            }
            
            response += ". هل " + concept + " " + generateFollowUpQuestion(concept) + "؟";
            
            return new GeneratedResponse(response);
        }
        
        return new GeneratedResponse("شكراً لتعليمي! هل يمكنك توضيح المزيد؟");
    }
    
    private String generateFollowUpQuestion(String concept) {
        List<String> questions = Arrays.asList(
            "لها ألوان أخرى",
            "تنمو في أماكن معينة",
            "لها استخدامات خاصة",
            "تتأثر بالفصول",
            "تحتاج إلى عناية خاصة"
        );
        return questions.get(new Random().nextInt(questions.size()));
    }
    
    private GeneratedResponse answerQuestion(String question, SemanticAnalysis analysis) {
        String targetConcept = extractTargetConcept(question);
        
        if (targetConcept != null && !targetConcept.isEmpty() && lexicon.hasWord(targetConcept)) {
            Definition def = lexicon.getDefinition(targetConcept);
            
            // ✅ إصلاح: التحقق من null
            if (def == null) {
                return new GeneratedResponse("لم أجد معلومات كافية عن " + targetConcept);
            }
            
            String answer = targetConcept + " " + def.meaning;
            
            List<String> related = knowledgeGraph.getRelatedConcepts(targetConcept);
            if (!related.isEmpty()) {
                answer += ". كما أنها " + String.join(" و", 
                    related.subList(0, Math.min(3, related.size())));
            }
            
            return new GeneratedResponse(answer);
        }
        
        return new GeneratedResponse("لم أتعلم بعد عن " + 
            (targetConcept != null && !targetConcept.isEmpty() ? targetConcept : "هذا") + 
            ". هل يمكنك أن تعلمني؟");
    }
    
    private GeneratedResponse generateContextualResponse(String input, NeuralSeed.InternalState state, 
                                                        SemanticAnalysis analysis) {
        StringBuilder response = new StringBuilder();
        
        for (String concept : analysis.mainConcepts) {
            if (lexicon.hasWord(concept)) {
                Definition def = lexicon.getDefinition(concept);
                if (def != null) {
                    response.append("أتذكر أن ").append(concept).append(" ")
                           .append(def.meaning).append(". ");
                }
            }
        }
        
        if (response.length() == 0) {
            response.append("مثير للاهتمام! هل يمكنك أن تخبرني المزيد؟");
        } else {
            response.append("هل هناك المزيد تريد إضافته؟");
        }
        
        return new GeneratedResponse(response.toString());
    }
    
    private String extractTargetConcept(String question) {
        // ✅ إصلاح: regex محسّن للعربية
        Pattern pattern = Pattern.compile(
            "(?:ما|من|أين|كيف|لماذا)\\s+(?:هي|هو|تكون|يكون)?\\s+([\\p{L}\\p{Nd}]+)",
            Pattern.UNICODE_CHARACTER_CLASS
        );
        Matcher matcher = pattern.matcher(question);
        if (matcher.find()) {
            String found = normalizeWord(matcher.group(1));
            if (!found.isEmpty()) return found;
        }
        
        String[] words = question.split("\\s+");
        for (String word : words) {
            String normalized = normalizeWord(word);
            if (!normalized.isEmpty() && lexicon.hasWord(normalized)) {
                return normalized;
            }
        }
        
        return null;
    }
    
    private List<String> extractConcepts(String text) {
        List<String> concepts = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            String normalized = normalizeWord(word);
            if (!normalized.isEmpty() && normalized.length() > 2 && !isCommonWord(normalized)) {
                concepts.add(normalized);
            }
        }
        
        return concepts;
    }
    
    private String normalizeWord(String word) {
        if (word == null) return "";
        // ✅ إصلاح: إزالة التشكيل والرموز الخاصة
        return word.replaceAll("[^\\p{L}\\p{Nd}]", "").toLowerCase().trim();
    }
    
    private boolean isCommonWord(String word) {
        Set<String> common = new HashSet<>(Arrays.asList(
            "هي", "هو", "التي", "الذي", "في", "من", "إلى", "على", "هذا", "هذه",
            "أن", "أو", "و", "مع", "عن", "كان", "يكون", "التعريف", "المعنى",
            "التي", "الذين", "هؤلاء", "ذلك", "هناك", "عند", "بعد", "قبل"
        ));
        return common.contains(word);
    }
    
    private String detectEmotion(String text) {
        if (text.contains("سعيد") || text.contains("فرح") || text.contains("ممتاز")) return "joy";
        if (text.contains("حزين") || text.contains("سيئ") || text.contains("صعب")) return "sadness";
        if (text.contains("غاضب") || text.contains("غضب")) return "anger";
        if (text.contains("خائف") || text.contains("خوف")) return "fear";
        return "neutral";
    }
    
    private double calculateIntensity(String text) {
        if (text == null || text.isEmpty()) return 0.5;
        int caps = 0;
        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) caps++;
        }
        return Math.min(1.0, 0.5 + (caps / (double) Math.max(1, text.length())));
    }
    
    public void learnMeaning(String word, String meaning, String source) {
        if (word == null || meaning == null) return;
        
        Definition def = new Definition(meaning, extractCategory(meaning), null);
        lexicon.addWord(normalizeWord(word), def.meaning, def.category, source);
        saveKnowledge();
    }
    
    public void learnWordEmotion(String word, String emotion, double intensity) {
        if (word == null || emotion == null) return;
        lexicon.addEmotion(normalizeWord(word), emotion, intensity);
        saveKnowledge();
    }
    
    public boolean learnFromCorrection(String original, String corrected, String explanation) {
        if (original == null || corrected == null) return false;
        
        Pattern pattern = Pattern.compile("([\\p{L}\\p{Nd}]+)\\s+([\\p{L}\\p{Nd}]+)");
        Matcher matcher = pattern.matcher(original);
        
        while (matcher.find()) {
            String word = matcher.group(1);
            String wrongUsage = matcher.group(2);
            lexicon.addCommonMistake(word, wrongUsage, corrected);
        }
        
        if (listener != null) {
            listener.onSentenceCorrected(original, corrected);
        }
        
        saveKnowledge();
        return true;
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("lexicon_size", lexicon.getWordCount());
        stats.put("concepts_count", knowledgeGraph.getConceptsCount());
        stats.put("relationships_count", knowledgeGraph.getRelationshipsCount());
        stats.put("learning_level", calculateLearningLevel());
        stats.put("word_count", lexicon.getWordCount());
        stats.put("conversation_count", lexicon.getConversationCount());
        return stats;
    }
    
    public Lexicon getLexicon() {
        return lexicon;
    }
    
    private String calculateLearningLevel() {
        int words = lexicon.getWordCount();
        if (words < 10) return "مبتدئ";
        if (words < 50) return "متعلم";
        if (words < 200) return "متوسط";
        return "متقدم";
    }
    
    private void saveKnowledge() {
        if (appContext == null) return;
        
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            JSONObject knowledge = new JSONObject();
            knowledge.put("lexicon", lexicon.toJSON());
            knowledge.put("knowledgeGraph", knowledgeGraph.toJSON());
            
            editor.putString("knowledge_base", knowledge.toString());
            editor.apply();
            
            Log.d(TAG, "Knowledge saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving knowledge", e);
        }
    }
    
    private void loadKnowledge() {
        if (appContext == null) return;
        
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String knowledgeJson = prefs.getString("knowledge_base", null);
            
            if (knowledgeJson != null && !knowledgeJson.isEmpty()) {
                try {
                    JSONObject knowledge = new JSONObject(knowledgeJson);
                    
                    if (knowledge.has("lexicon")) {
                        lexicon.fromJSON(knowledge.getJSONObject("lexicon"));
                    }
                    if (knowledge.has("knowledgeGraph")) {
                        knowledgeGraph.fromJSON(knowledge.getJSONObject("knowledgeGraph"));
                    }
                    
                    Log.d(TAG, "Knowledge loaded: " + lexicon.getWordCount() + " words");
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing knowledge, clearing corrupted data", e);
                    prefs.edit().remove("knowledge_base").apply();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading knowledge", e);
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
        public String reason;
        public String source;
        public long timestamp;
        
        public Definition(String meaning, String category, String reason) {
            this.meaning = meaning != null ? meaning : "غير معروف";
            this.category = category != null ? category : "مفهوم عام";
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static class Relationship {
        public String subject;
        public String relationship;
        public String object;
        public String context;
        public long timestamp;
        
        public Relationship(String s, String r, String o, String c) {
            this.subject = s != null ? s : "";
            this.relationship = r != null ? r : "";
            this.object = o != null ? o : "";
            this.context = c != null ? c : "";
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static class SemanticAnalysis {
        public boolean isTeaching;
        public boolean isQuestion;
        public List<String> mainConcepts = new ArrayList<>();
    }
    
    // ===== Lexicon =====
    
    public static class Lexicon {
        private Map<String, Definition> words = new HashMap<>();
        private Map<String, Map<String, Double>> emotions = new HashMap<>();
        private Map<String, List<String>> commonMistakes = new HashMap<>();
        private int conversationCount = 0;
        
        public void addWord(String word, String meaning, String category, String source) {
            if (word == null || word.isEmpty()) return;
            
            Definition def = new Definition(meaning, category, null);
            def.source = source;
            words.put(word, def);
        }
        
        public void addEmotion(String word, String emotion, double intensity) {
            if (word == null || emotion == null) return;
            emotions.computeIfAbsent(word, k -> new HashMap<>()).put(emotion, intensity);
        }
        
        public void addCommonMistake(String word, String wrong, String correct) {
            if (word == null) return;
            commonMistakes.computeIfAbsent(word, k -> new ArrayList<>()).add(wrong + "->" + correct);
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
        
        public int getConversationCount() {
            return conversationCount;
        }
        
        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            JSONObject wordsJson = new JSONObject();
            
            for (Map.Entry<String, Definition> entry : words.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                
                JSONObject def = new JSONObject();
                def.put("meaning", entry.getValue().meaning);
                def.put("category", entry.getValue().category);
                def.put("reason", entry.getValue().reason);
                def.put("source", entry.getValue().source);
                wordsJson.put(entry.getKey(), def);
            }
            
            json.put("words", wordsJson);
            json.put("conversationCount", conversationCount);
            return json;
        }
        
        public void fromJSON(JSONObject json) throws JSONException {
            words.clear();
            if (json == null || !json.has("words")) return;
            
            JSONObject wordsJson = json.getJSONObject("words");
            Iterator<String> keys = wordsJson.keys();
            
            while (keys.hasNext()) {
                String word = keys.next();
                if (word == null) continue;
                
                try {
                    JSONObject def = wordsJson.getJSONObject(word);
                    
                    Definition definition = new Definition(
                        def.optString("meaning", "غير معروف"),
                        def.optString("category", "مفهوم عام"),
                        def.optString("reason", null)
                    );
                    definition.source = def.optString("source", "unknown");
                    words.put(word, definition);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading word: " + word, e);
                }
            }
            
            conversationCount = json.optInt("conversationCount", 0);
        }
    }
    
    // ===== KnowledgeGraph =====
    
    public static class KnowledgeGraph {
        private Map<String, Definition> concepts = new HashMap<>();
        private List<Relationship> relationships = new ArrayList<>();
        private Map<String, List<String>> attributes = new HashMap<>();
        
        public void addConcept(String concept, Definition def) {
            if (concept != null && def != null) {
                concepts.put(concept, def);
            }
        }
        
        public void addRelationship(String subject, String rel, String object, String context) {
            if (subject != null && rel != null && object != null) {
                relationships.add(new Relationship(subject, rel, object, context));
            }
        }
        
        public void addAttribute(String concept, String type, String value) {
            if (concept != null && type != null && value != null) {
                attributes.computeIfAbsent(concept, k -> new ArrayList<>()).add(type + ":" + value);
            }
        }
        
        public List<String> getUnknownConcepts() {
            List<String> unknown = new ArrayList<>();
            for (Relationship rel : relationships) {
                if (rel != null && rel.object != null && !concepts.containsKey(rel.object)) {
                    unknown.add(rel.object);
                }
            }
            return unknown;
        }
        
        public List<Relationship> getIncompleteRelationships() {
            List<Relationship> incomplete = new ArrayList<>();
            for (Relationship rel : relationships) {
                // ✅ إصلاح: التحقق من null قبل split
                if (rel != null && rel.context != null && rel.context.split("\\s+").length < 5) {
                    incomplete.add(rel);
                }
            }
            return incomplete;
        }
        
        public List<String> getRelatedConcepts(String concept) {
            List<String> related = new ArrayList<>();
            if (concept == null) return related;
            
            for (Relationship rel : relationships) {
                if (rel != null && concept.equals(rel.subject)) {
                    related.add(rel.relationship + " " + rel.object);
                }
            }
            return related;
        }
        
        public int getConceptsCount() {
            return concepts.size();
        }
        
        public int getRelationshipsCount() {
            return relationships.size();
        }
        
        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            
            JSONArray rels = new JSONArray();
            for (Relationship r : relationships) {
                if (r == null) continue;
                
                JSONObject obj = new JSONObject();
                obj.put("subject", r.subject);
                obj.put("relationship", r.relationship);
                obj.put("object", r.object);
                obj.put("context", r.context);
                rels.put(obj);
            }
            json.put("relationships", rels);
            
            return json;
        }
        
        public void fromJSON(JSONObject json) throws JSONException {
            relationships.clear();
            if (json == null || !json.has("relationships")) return;
            
            JSONArray rels = json.getJSONArray("relationships");
            for (int i = 0; i < rels.length(); i++) {
                try {
                    JSONObject obj = rels.getJSONObject(i);
                    relationships.add(new Relationship(
                        obj.optString("subject", ""),
                        obj.optString("relationship", ""),
                        obj.optString("object", ""),
                        obj.optString("context", "")
                    ));
                } catch (Exception e) {
                    Log.e(TAG, "Error loading relationship at index " + i, e);
                }
            }
        }
    }
}

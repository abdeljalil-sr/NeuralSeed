package com.neuralseed;

import java.util.*;

/**
 * نظام التعلم - التعلم من الأمثلة والتصحيحات
 */
public class LearningSystem {
    
    private ArabicLexicon lexicon;
    private ArabicParser parser;
    private SemanticEmotionalEngine emotionEngine;
    private LocalDatabase database;
    
    private List<LearningPattern> patterns = new ArrayList<>();
    private Map<String, Correction> corrections = new HashMap<>();
    
    public static class LearningPattern {
        public String pattern;
        public String example;
        public String result;
        public double confidence;
        public int usageCount;
        
        public LearningPattern(String pattern, String example, String result) {
            this.pattern = pattern;
            this.example = example;
            this.result = result;
            this.confidence = 0.5;
            this.usageCount = 0;
        }
    }
    
    public static class Correction {
        public String original;
        public String corrected;
        public String explanation;
        public String context;
        public long timestamp;
        public boolean learned;
        
        public Correction(String original, String corrected, String explanation) {
            this.original = original;
            this.corrected = corrected;
            this.explanation = explanation;
            this.timestamp = System.currentTimeMillis();
            this.learned = false;
        }
    }
    
    public LearningSystem(ArabicLexicon lexicon, ArabicParser parser, 
                        SemanticEmotionalEngine emotionEngine, LocalDatabase database) {
        this.lexicon = lexicon;
        this.parser = parser;
        this.emotionEngine = emotionEngine;
        this.database = database;
        
        initializePatterns();
        loadCorrections();
    }
    
    private void initializePatterns() {
        // أنماط تعلم أساسية
        patterns.add(new LearningPattern("X هو Y", "الحب هو السعادة", "تعريف"));
        patterns.add(new LearningPattern("X يعني Y", "السلام يعني الأمان", "تعريف"));
        patterns.add(new LearningPattern("X مثل Y", "القلب مثل البحر", "تشبيه"));
        patterns.add(new LearningPattern("X ليس Y", "الكراهية ليست حلاً", "نفي"));
    }
    
    private void loadCorrections() {
        if (database != null) {
            List<Map<String, Object>> pending = database.getPendingCorrections();
            for (Map<String, Object> corr : pending) {
                Correction correction = new Correction(
                    (String) corr.get("original"),
                    (String) corr.get("corrected"),
                    (String) corr.get("explanation")
                );
                corrections.put(correction.original, correction);
            }
        }
    }
    
    /**
     * التعلم من مثال
     */
    public void learnFromExample(String example, String context) {
        // تحليل المثال
        List<ArabicParser.ParseResult> parseResults = parser.parseText(example);
        
        // استخراج الكلمات الجديدة
        for (ArabicParser.ParseResult result : parseResults) {
            if (result.confidence < 0.5) {
                // كلمة جديدة
                learnNewWord(result.word, context);
            }
        }
        
        // البحث عن أنماط
        detectPattern(example);
        
        // حفظ في قاعدة البيانات
        if (database != null) {
            database.recordLearning(example, "processed", context, true, null);
        }
    }
    
    /**
     * تعلم كلمة جديدة
     */
    private void learnNewWord(String word, String context) {
        // محاولة استنتاج المعنى من السياق
        String inferredMeaning = inferMeaningFromContext(word, context);
        
        if (inferredMeaning != null) {
            lexicon.addWord(word, inferredMeaning);
            
            if (database != null) {
                ArabicLexicon.Word w = new ArabicLexicon.Word(word);
                w.meanings.add(inferredMeaning);
                database.saveWord(w);
            }
        }
    }
    
    /**
     * استنتاج المعنى من السياق
     */
    private String inferMeaningFromContext(String word, String context) {
        // استخراج الكلمات المحيطة
        List<String> tokens = parser.tokenize(context);
        int index = tokens.indexOf(word);
        
        if (index >= 0 && index < tokens.size() - 1) {
            // البحث عن تعريف صريح
            String next = tokens.get(index + 1);
            if (next.equals("هو") || next.equals("هي") || next.equals("يعني")) {
                if (index + 2 < tokens.size()) {
                    return tokens.get(index + 2);
                }
            }
        }
        
        return "مفهوم جديد";
    }
    
    /**
     * اكتشاف نمط
     */
    private void detectPattern(String text) {
        for (LearningPattern pattern : patterns) {
            if (text.contains(pattern.pattern.replace("X", "").replace("Y", "").trim())) {
                pattern.usageCount++;
                pattern.confidence = Math.min(1.0, pattern.confidence + 0.05);
            }
        }
    }
    
    /**
     * تعلم من تصحيح
     */
    public void learnFromCorrection(String original, String corrected, String explanation) {
        Correction correction = new Correction(original, corrected, explanation);
        corrections.put(original, correction);
        
        // حفظ في قاعدة البيانات
        if (database != null) {
            database.saveCorrection(original, corrected, explanation);
        }
        
        // تحديث المعجم
        ArabicLexicon.Word word = lexicon.getWord(original);
        if (word != null) {
            word.meanings.add("تصحيح: " + corrected);
        }
    }
    
    /**
     * تطبيق تصحيح
     */
    public String applyCorrection(String text) {
        String result = text;
        
        for (Correction correction : corrections.values()) {
            if (correction.learned && result.contains(correction.original)) {
                result = result.replace(correction.original, correction.corrected);
            }
        }
        
        return result;
    }
    
    /**
     * تقييم جملة
     */
    public double evaluateSentence(String sentence) {
        double score = 0.5; // قاعدة
        
        // تحليل الجملة
        List<ArabicParser.ParseResult> results = parser.parseText(sentence);
        
        // وجود أخطاء؟
        boolean hasUnknownWords = results.stream()
            .anyMatch(r -> r.confidence < 0.3);
        
        if (hasUnknownWords) {
            score -= 0.2;
        }
        
        // طول مناسب؟
        if (results.size() >= 2 && results.size() <= 20) {
            score += 0.2;
        }
        
        // وجود فعل واسم؟
        boolean hasVerb = results.stream().anyMatch(r -> r.type == ArabicLexicon.WordType.VERB);
        boolean hasNoun = results.stream().anyMatch(r -> r.type == ArabicLexicon.WordType.NOUN);
        
        if (hasVerb || hasNoun) {
            score += 0.1;
        }
        
        return Math.max(0, Math.min(1, score));
    }
    
    /**
     * اقتراح تحسينات
     */
    public List<String> suggestImprovements(String sentence) {
        List<String> suggestions = new ArrayList<>();
        
        List<ArabicParser.ParseResult> results = parser.parseText(sentence);
        
        // كلمات غير معروفة
        for (ArabicParser.ParseResult result : results) {
            if (result.confidence < 0.3) {
                String suggestion = parser.suggestCorrection(result.word);
                if (!suggestion.equals(result.word)) {
                    suggestions.add("هل تقصد: " + suggestion + " بدلاً من " + result.word + "؟");
                }
            }
        }
        
        // أنماط مشابهة
        for (LearningPattern pattern : patterns) {
            if (pattern.usageCount > 5 && pattern.confidence > 0.7) {
                if (sentence.contains(pattern.pattern.split(" ")[0])) {
                    suggestions.add("نمط مشابه: " + pattern.example);
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * الحصول على إحصائيات التعلم
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("patterns_count", patterns.size());
        stats.put("corrections_count", corrections.size());
        stats.put("learned_corrections", corrections.values().stream().filter(c -> c.learned).count());
        
        double avgConfidence = patterns.stream()
            .mapToDouble(p -> p.confidence)
            .average()
            .orElse(0);
        stats.put("average_pattern_confidence", avgConfidence);
        
        return stats;
    }
    
    /**
     * الحصول على الأنماط الشائعة
     */
    public List<LearningPattern> getCommonPatterns(int limit) {
        return patterns.stream()
            .sorted((a, b) -> Integer.compare(b.usageCount, a.usageCount))
            .limit(limit)
            .toList();
    }
}

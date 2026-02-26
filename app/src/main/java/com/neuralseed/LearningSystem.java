package com.neuralseed;

import java.util.*;

/**
 * نظام التعلم والتصحيح - يتيح للوعي التعلم من التصحيحات والأخطاء
 */
public class LearningSystem {
    
    private ArabicLexicon lexicon;
    private ArabicParser parser;
    private SemanticEmotionalEngine emotionEngine;
    private LocalDatabase database;
    
    // نمط التعلم
    public static class LearningPattern {
        String pattern;           // النمط
        String replacement;       // البديل
        String context;           // السياق
        double confidence;        // الثقة
        int successCount;         // عدد النجاحات
        int failureCount;         // عدد الفشل
        long lastUsed;            // آخر استخدام
        
        public LearningPattern(String pattern, String replacement, String context) {
            this.pattern = pattern;
            this.replacement = replacement;
            this.context = context;
            this.confidence = 0.5;
            this.successCount = 0;
            this.failureCount = 0;
            this.lastUsed = System.currentTimeMillis();
        }
        
        public void recordSuccess() {
            successCount++;
            updateConfidence();
        }
        
        public void recordFailure() {
            failureCount++;
            updateConfidence();
        }
        
        private void updateConfidence() {
            int total = successCount + failureCount;
            if (total > 0) {
                confidence = (double) successCount / total;
            }
        }
        
        public double getEffectiveness() {
            int total = successCount + failureCount;
            if (total == 0) return 0.5;
            return confidence * Math.min(1.0, total / 10.0);
        }
    }
    
    // خطأ تم اكتشافه
    public static class DetectedError {
        String original;          // النص الأصلي
        String errorType;         // نوع الخطأ
        String description;       // الوصف
        List<String> suggestions; // الاقتراحات
        int position;             // الموقع
        
        public DetectedError(String original, String errorType, String description) {
            this.original = original;
            this.errorType = errorType;
            this.description = description;
            this.suggestions = new ArrayList<>();
        }
    }
    
    // نتيجة التعلم
    public static class LearningResult {
        boolean learned;          // هل تم التعلم
        String whatLearned;       // ما تم تعلمه
        double confidence;        // الثقة في التعلم
        List<String> relatedPatterns; // الأنماط المرتبطة
        
        public LearningResult() {
            this.learned = false;
            this.confidence = 0.0;
            this.relatedPatterns = new ArrayList<>();
        }
    }
    
    // سجل التعلم
    public static class LearningRecord {
        String input;             // المدخل
        String expected;          // المتوقع
        String actual;            // الفعلي
        boolean wasCorrected;     // هل تم تصحيحه
        String correction;        // التصحيح
        String feedback;          // التغذية الراجعة
        long timestamp;           // الوقت
        Map<String, Double> emotions; // العواطف المرتبطة
        
        public LearningRecord() {
            this.timestamp = System.currentTimeMillis();
            this.emotions = new HashMap<>();
        }
    }
    
    private List<LearningPattern> patterns;
    private List<LearningRecord> history;
    private Map<String, List<String>> wordCorrections;
    private Map<String, String> grammarRules;
    
    public LearningSystem(ArabicLexicon lexicon, ArabicParser parser, 
                         SemanticEmotionalEngine emotionEngine, LocalDatabase database) {
        this.lexicon = lexicon;
        this.parser = parser;
        this.emotionEngine = emotionEngine;
        this.database = database;
        this.patterns = new ArrayList<>();
        this.history = new ArrayList<>();
        this.wordCorrections = new HashMap<>();
        this.grammarRules = new HashMap<>();
        
        initializePatterns();
    }
    
    private void initializePatterns() {
        // أنماط أخطاء شائعة في اللغة العربية
        patterns.add(new LearningPattern("ان", "أن", "grammatical"));
        patterns.add(new LearningPattern("لا تستطيع", "لا أستطيع", "pronoun"));
        patterns.add(new LearningPattern("هو يكون", "يكون", "redundancy"));
        patterns.add(new LearningPattern("في داخل", "داخل", "redundancy"));
        patterns.add(new LearningPattern("قدر كبير", "كثير", "style"));
        patterns.add(new LearningPattern("في الماضي", "سابقاً", "style"));
        patterns.add(new LearningPattern("في المستقبل", "لاحقاً", "style"));
        patterns.add(new LearningPattern("أنا أعتقد", "أعتقد", "redundancy"));
        patterns.add(new LearningPattern("أنا أحب", "أحب", "redundancy"));
        patterns.add(new LearningPattern("أنا أريد", "أريد", "redundancy"));
        
        // أنماط صرفية
        patterns.add(new LearningPattern("كتبتة", "كتابة", "spelling"));
        patterns.add(new LearningPattern("قرأة", "قراءة", "spelling"));
        patterns.add(new LearningPattern("ذهبة", "ذهاب", "spelling"));
        patterns.add(new LearningPattern("جاية", "جاءة", "spelling"));
        patterns.add(new LearningPattern("شايف", "رأى", "dialect"));
        patterns.add(new LearningPattern("عايز", "يريد", "dialect"));
        patterns.add(new LearningPattern("عامل", "يعمل", "dialect"));
        patterns.add(new LearningPattern("فاهم", "يفهم", "dialect"));
    }
    
    /**
     * تحليل النص للبحث عن أخطاء
     */
    public List<DetectedError> analyzeForErrors(String text) {
        List<DetectedError> errors = new ArrayList<>();
        
        // تقسيم النص إلى كلمات
        String[] words = text.split("\\s+");
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            
            // التحقق من وجود الكلمة في المعجم
            if (!lexicon.hasWord(word)) {
                // البحث عن تصحيح معروف
                String correction = findCorrection(word);
                if (correction != null) {
                    DetectedError error = new DetectedError(word, "spelling", "كلمة غير معروفة");
                    error.suggestions.add(correction);
                    error.position = i;
                    errors.add(error);
                } else {
                    // محاولة اقتراح تصحيح
                    List<String> suggestions = suggestCorrections(word);
                    if (!suggestions.isEmpty()) {
                        DetectedError error = new DetectedError(word, "unknown", "كلمة غير معروفة");
                        error.suggestions.addAll(suggestions);
                        error.position = i;
                        errors.add(error);
                    }
                }
            }
        }
        
        // التحقق من أنماط الأخطاء
        for (LearningPattern pattern : patterns) {
            if (text.contains(pattern.pattern) && pattern.confidence > 0.3) {
                DetectedError error = new DetectedError(pattern.pattern, "pattern", "نمط غير صحيح");
                error.suggestions.add(pattern.replacement);
                errors.add(error);
            }
        }
        
        // التحقق من القواعد النحوية
        List<ArabicParser.ParseResult> parseResults = parser.parseText(text);
        for (ArabicParser.ParseResult result : parseResults) {
            if (!result.isComplete) {
                for (String error : result.errors) {
                    DetectedError detectedError = new DetectedError(result.originalText, "grammar", error);
                    detectedError.suggestions.addAll(parser.suggestCorrections(result.originalText));
                    errors.add(detectedError);
                }
            }
        }
        
        return errors;
    }
    
    /**
     * البحث عن تصحيح معروف
     */
    private String findCorrection(String word) {
        // البحث في قاعدة البيانات
        if (database != null) {
            return database.findCorrection(word);
        }
        return null;
    }
    
    /**
     * اقتراح تصحيحات للكلمة
     */
    private List<String> suggestCorrections(String word) {
        List<String> suggestions = new ArrayList<>();
        
        // البحث عن كلمات مشابهة في المعجم
        for (ArabicLexicon.Word knownWord : lexicon.getAllWords()) {
            double similarity = calculateSimilarity(word, knownWord.word);
            if (similarity > 0.7) {
                suggestions.add(knownWord.word);
            }
        }
        
        // ترتيب حسب التشابه
        suggestions.sort((a, b) -> {
            double simA = calculateSimilarity(word, a);
            double simB = calculateSimilarity(word, b);
            return Double.compare(simB, simA);
        });
        
        return suggestions.subList(0, Math.min(3, suggestions.size()));
    }
    
    /**
     * حساب التشابه بين كلمتين (Levenshtein distance)
     */
    private double calculateSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // حذف
                    dp[i][j - 1] + 1),     // إدراج
                    dp[i - 1][j - 1] + cost // استبدال
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * تعلم من تصحيح
     */
    public LearningResult learnFromCorrection(String original, String corrected, String explanation) {
        LearningResult result = new LearningResult();
        
        // حفظ التصحيح
        if (database != null) {
            database.saveCorrection(original, corrected, explanation);
        }
        
        // إنشاء نمط تعلم جديد
        LearningPattern pattern = new LearningPattern(original, corrected, "user_correction");
        patterns.add(pattern);
        
        // تعلم الكلمات الجديدة
        String[] words = corrected.split("\\s+");
        for (String word : words) {
            if (!lexicon.hasWord(word)) {
                // إضافة الكلمة للمعجم
                ArabicLexicon.Word newWord = new ArabicLexicon.Word(word, word, ArabicLexicon.WordType.NOUN);
                newWord.addMeaning("تم التعلم من التصحيح");
                // لا يمكن إضافتها مباشرة، نحتاج طريقة لإضافة كلمات جديدة
            }
        }
        
        // تحليل الفرق
        List<String> differences = findDifferences(original, corrected);
        result.whatLearned = "التصحيح: " + String.join(", ", differences);
        result.confidence = 0.8;
        result.learned = true;
        
        // تسجيل في السجل
        LearningRecord record = new LearningRecord();
        record.input = original;
        record.expected = corrected;
        record.wasCorrected = true;
        record.correction = corrected;
        record.feedback = explanation;
        history.add(record);
        
        if (database != null) {
            database.recordLearning(original, corrected, "correction", true, explanation);
        }
        
        return result;
    }
    
    /**
     * تعلم من مثال صحيح
     */
    public void learnFromExample(String example, String context) {
        // تحليل الجملة
        List<ArabicParser.ParseResult> results = parser.parseText(example);
        
        for (ArabicParser.ParseResult result : results) {
            if (result.isComplete) {
                // حفظ الجملة الصحيحة
                if (database != null) {
                    database.saveSentence(example, result.sentenceType,
                                         result.elements.toString(),
                                         emotionEngine.analyzeEmotions(example),
                                         true, result.confidence);
                }
                
                // تعلم الكلمات
                for (ArabicParser.SentenceElement element : result.elements) {
                    ArabicLexicon.Word word = lexicon.getWordByForm(element.word);
                    if (word != null) {
                        word.use();
                        if (database != null) {
                            database.updateWordUsage(element.word);
                        }
                    }
                }
                
                // تسجيل في السجل
                LearningRecord record = new LearningRecord();
                record.input = example;
                record.expected = example;
                record.actual = example;
                record.wasCorrected = false;
                record.feedback = "مثال صحيح";
                history.add(record);
                
                if (database != null) {
                    database.recordLearning(example, example, context, true, "مثال صحيح");
                }
            }
        }
    }
    
    /**
     * تعلم معنى جديد لكلمة
     */
    public void learnWordMeaning(String word, String meaning, String context) {
        ArabicLexicon.Word w = lexicon.getWordByForm(word);
        if (w != null) {
            w.addMeaning(meaning);
            
            if (database != null) {
                database.saveWord(w);
            }
            
            // تسجيل
            LearningRecord record = new LearningRecord();
            record.input = word;
            record.expected = meaning;
            record.feedback = "معنى جديد: " + context;
            history.add(record);
        }
    }
    
    /**
     * تعلم عاطفة مرتبطة بكلمة
     */
    public void learnWordEmotion(String word, String emotion, double intensity, String context) {
        ArabicLexicon.Word w = lexicon.getWordByForm(word);
        if (w != null) {
            w.addEmotion(emotion, intensity);
            
            if (database != null) {
                database.saveEmotionLink(word, emotion, intensity, context);
            }
        }
    }
    
    /**
     * العثور على الاختلافات بين نصين
     */
    private List<String> findDifferences(String s1, String s2) {
        List<String> differences = new ArrayList<>();
        
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");
        
        // مقارنة الكلمات
        int minLen = Math.min(words1.length, words2.length);
        for (int i = 0; i < minLen; i++) {
            if (!words1[i].equals(words2[i])) {
                differences.add(words1[i] + " -> " + words2[i]);
            }
        }
        
        // كلمات إضافية
        if (words1.length > words2.length) {
            for (int i = words2.length; i < words1.length; i++) {
                differences.add("حذف: " + words1[i]);
            }
        } else if (words2.length > words1.length) {
            for (int i = words1.length; i < words2.length; i++) {
                differences.add("إضافة: " + words2[i]);
            }
        }
        
        return differences;
    }
    
    /**
     * الحصول على أنماط التعلم
     */
    public List<LearningPattern> getPatterns() {
        return new ArrayList<>(patterns);
    }
    
    /**
     * الحصول على أنماط فعالة
     */
    public List<LearningPattern> getEffectivePatterns(double minEffectiveness) {
        List<LearningPattern> effective = new ArrayList<>();
        for (LearningPattern pattern : patterns) {
            if (pattern.getEffectiveness() >= minEffectiveness) {
                effective.add(pattern);
            }
        }
        return effective;
    }
    
    /**
     * الحصول على سجل التعلم
     */
    public List<LearningRecord> getHistory(int limit) {
        int start = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(start, history.size()));
    }
    
    /**
     * تطبيق ما تم تعلمه على النص
     */
    public String applyLearning(String text) {
        String result = text;
        
        // تطبيق الأنماط
        for (LearningPattern pattern : patterns) {
            if (pattern.confidence > 0.5) {
                result = result.replace(pattern.pattern, pattern.replacement);
            }
        }
        
        // البحث عن تصحيحات معروفة
        if (database != null) {
            String[] words = result.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                String correction = database.findCorrection(word);
                if (correction != null) {
                    sb.append(correction);
                } else {
                    sb.append(word);
                }
                sb.append(" ");
            }
            result = sb.toString().trim();
        }
        
        return result;
    }
    
    /**
     * تقييم جملة
     */
    public double evaluateSentence(String sentence) {
        double score = 0.5;
        
        // التحقق من صحة النحو
        List<ArabicParser.ParseResult> results = parser.parseText(sentence);
        for (ArabicParser.ParseResult result : results) {
            if (result.isComplete) {
                score += 0.3;
            }
            score += result.confidence * 0.2;
        }
        
        // التحقق من وجود الكلمات
        String[] words = sentence.split("\\s+");
        int knownWords = 0;
        for (String word : words) {
            if (lexicon.hasWord(word)) {
                knownWords++;
            }
        }
        score += (double) knownWords / words.length * 0.3;
        
        return Math.min(1.0, score);
    }
    
    /**
     * الحصول على إحصائيات التعلم
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_patterns", patterns.size());
        stats.put("high_confidence_patterns", 
                  patterns.stream().filter(p -> p.confidence > 0.7).count());
        stats.put("total_records", history.size());
        
        int successfulCorrections = 0;
        for (LearningRecord record : history) {
            if (record.wasCorrected) successfulCorrections++;
        }
        stats.put("successful_corrections", successfulCorrections);
        
        return stats;
    }
    
    /**
     * واجهة للتصحيح التفاعلي
     */
    public interface CorrectionListener {
        void onCorrectionNeeded(String original, List<String> suggestions);
        void onLearned(String pattern, String correction);
    }
    
    private CorrectionListener correctionListener;
    
    public void setCorrectionListener(CorrectionListener listener) {
        this.correctionListener = listener;
    }
    
    /**
     * معالجة المدخل مع إمكانية التصحيح
     */
    public String processWithLearning(String input, boolean allowCorrection) {
        String processed = applyLearning(input);
        
        if (allowCorrection && !processed.equals(input)) {
            List<DetectedError> errors = analyzeForErrors(input);
            if (!errors.isEmpty() && correctionListener != null) {
                for (DetectedError error : errors) {
                    correctionListener.onCorrectionNeeded(error.original, error.suggestions);
                }
            }
        }
        
        return processed;
    }
}

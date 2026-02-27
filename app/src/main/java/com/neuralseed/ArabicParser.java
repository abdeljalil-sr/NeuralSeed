package com.neuralseed;

import java.util.*;
import java.util.regex.*;

/**
 * محلل اللغة العربية - تحليل بنية الجمل واستخراج المعاني
 */
public class ArabicParser {
    
    private ArabicLexicon lexicon;
    
    public enum SentenceType {
        VERBAL, NOMINAL, QUESTION, EXCLAMATORY, UNKNOWN
    }
    
    public static class ParseResult {
        public String word;
        public ArabicLexicon.WordType type;
        public String root;
        public List<String> possibleMeanings = new ArrayList<>();
        public Map<String, Double> emotions = new HashMap<>();
        public double confidence = 0.5;
        public String role; // فاعل، مفعول، مبتدأ، خبر
        
        public ParseResult(String word) {
            this.word = word;
        }
    }
    
    public ArabicParser(ArabicLexicon lexicon) {
        this.lexicon = lexicon;
    }
    
    /**
     * تحليل نص كامل
     */
    public List<ParseResult> parseText(String text) {
        List<ParseResult> results = new ArrayList<>();
        List<String> tokens = tokenize(text);
        
        for (String token : tokens) {
            ParseResult result = parseWord(token);
            results.add(result);
        }
        
        // تحديد أدوار الكلمات
        determineRoles(results);
        
        return results;
    }
    
    /**
     * تقطيع النص إلى كلمات
     */
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        
        // إزالة التشكيل
        String cleanText = removeTashkeel(text);
        
        // تقطيع حسب المسافات والعلامات
        String[] parts = cleanText.split("[\\s\\.,؛،:!?]+");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        
        return tokens;
    }
    
    /**
     * إزالة علامات التشكيل
     */
    private String removeTashkeel(String text) {
        return text.replaceAll("[\\u064B-\\u065F\\u0670]", "");
    }
    
    /**
     * تحليل كلمة واحدة
     */
    public ParseResult parseWord(String word) {
        ParseResult result = new ParseResult(word);
        
        ArabicLexicon.Word lexiconWord = lexicon.getWord(word);
        if (lexiconWord != null) {
            result.type = lexiconWord.type;
            result.root = lexiconWord.root;
            result.possibleMeanings.addAll(lexiconWord.meanings);
            result.emotions.putAll(lexiconWord.emotions);
            result.confidence = 0.8 + (lexiconWord.familiarity * 0.2);
        } else {
            // محاولة استخراج الجذر للكلمات غير المعروفة
            result.root = extractRoot(word);
            result.type = guessType(word);
            result.confidence = 0.3;
        }
        
        return result;
    }
    
    /**
     * استخراج الجذر
     */
    private String extractRoot(String word) {
        // تبسيط: أخذ الحروف الأولى
        String clean = word.replaceAll("[الأإآ]", "");
        if (clean.length() >= 3) {
            return clean.substring(0, 3);
        }
        return clean;
    }
    
    /**
     * تخمين نوع الكلمة
     */
    private ArabicLexicon.WordType guessType(String word) {
        // قواعد بسيطة للتخمين
        if (word.startsWith("ال")) {
            return ArabicLexicon.WordType.NOUN;
        }
        if (word.endsWith("ت") || word.endsWith("ة")) {
            return ArabicLexicon.WordType.NOUN;
        }
        if (word.endsWith("ى") || word.endsWith("اء")) {
            return ArabicLexicon.WordType.ADJECTIVE;
        }
        return ArabicLexicon.WordType.UNKNOWN;
    }
    
    /**
     * تحديد أدوار الكلمات في الجملة
     */
    private void determineRoles(List<ParseResult> results) {
        if (results.isEmpty()) return;
        
        // الجملة الفعلية: تبدأ بفعل
        if (results.get(0).type == ArabicLexicon.WordType.VERB) {
            results.get(0).role = "فعل";
            
            // البحث عن فاعل ومفعول
            boolean foundSubject = false;
            for (int i = 1; i < results.size(); i++) {
                ParseResult r = results.get(i);
                if (r.type == ArabicLexicon.WordType.NOUN || 
                    r.type == ArabicLexicon.WordType.PRONOUN) {
                    if (!foundSubject) {
                        r.role = "فاعل";
                        foundSubject = true;
                    } else {
                        r.role = "مفعول";
                    }
                }
            }
        }
        // الجملة الاسمية: تبدأ باسم
        else if (results.get(0).type == ArabicLexicon.WordType.NOUN ||
                 results.get(0).type == ArabicLexicon.WordType.PRONOUN) {
            results.get(0).role = "مبتدأ";
            
            // البحث عن خبر
            for (int i = 1; i < results.size(); i++) {
                ParseResult r = results.get(i);
                if (r.type == ArabicLexicon.WordType.ADJECTIVE ||
                    r.type == ArabicLexicon.WordType.NOUN) {
                    if (r.role == null) {
                        r.role = "خبر";
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * تحديد نوع الجملة
     */
    public SentenceType determineSentenceType(List<ParseResult> results) {
        if (results.isEmpty()) return SentenceType.UNKNOWN;
        
        String firstWord = results.get(0).word;
        
        // أسئلة
        if (firstWord.matches("(هل|ما|من|كم|أين|متى|كيف|لماذا)")) {
            return SentenceType.QUESTION;
        }
        
        // تعجب
        if (firstWord.matches("(ما|كيف|يا)")) {
            return SentenceType.EXCLAMATORY;
        }
        
        // فعلية
        if (results.get(0).type == ArabicLexicon.WordType.VERB) {
            return SentenceType.VERBAL;
        }
        
        // اسمية
        return SentenceType.NOMINAL;
    }
    
    /**
     * استخراج الكلمات المفتاحية
     */
    public List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        List<ParseResult> results = parseText(text);
        
        for (ParseResult result : results) {
            // تجاهل الأدوات والحروف
            if (result.type != ArabicLexicon.WordType.PREPOSITION &&
                result.type != ArabicLexicon.WordType.CONJUNCTION &&
                result.type != ArabicLexicon.WordType.PARTICLE &&
                result.confidence > 0.5) {
                keywords.add(result.word);
            }
        }
        
        return keywords;
    }
    
    /**
     * تحليل العواطف في النص
     */
    public Map<String, Double> analyzeEmotions(String text) {
        Map<String, Double> emotions = new HashMap<>();
        List<ParseResult> results = parseText(text);
        
        for (ParseResult result : results) {
            for (Map.Entry<String, Double> entry : result.emotions.entrySet()) {
                emotions.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        
        // تطبيع
        double max = emotions.values().stream().max(Double::compare).orElse(1.0);
        if (max > 0) {
            for (String key : emotions.keySet()) {
                emotions.put(key, emotions.get(key) / max);
            }
        }
        
        return emotions;
    }
    
    /**
     * الحصول على العاطفة السائدة
     */
    public String getDominantEmotion(Map<String, Double> emotions) {
        if (emotions.isEmpty()) return "neutral";
        
        return emotions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("neutral");
    }
    
    /**
     * تصحيح إملائي بسيط
     */
    public String suggestCorrection(String word) {
        // البحث عن كلمات مشابهة
        List<String> candidates = lexicon.search(word);
        
        if (!candidates.isEmpty()) {
            // إرجاع الأقرب
            return candidates.get(0);
        }
        
        return word;
    }
    
    /**
     * تحليل بنية الجملة
     */
    public String analyzeStructure(String text) {
        List<ParseResult> results = parseText(text);
        SentenceType type = determineSentenceType(results);
        
        StringBuilder structure = new StringBuilder();
        structure.append("نوع الجملة: ").append(type.name()).append("\\n");
        structure.append("الكلمات:\\n");
        
        for (ParseResult r : results) {
            structure.append(String.format("  - %s (%s): %s\\n", 
                r.word, 
                r.type.name(),
                r.role != null ? r.role : "غير محدد"));
        }
        
        return structure.toString();
    }
}

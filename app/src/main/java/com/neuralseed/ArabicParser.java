package com.neuralseed;

import java.util.*;

/**
 * محلل اللغة العربية - يحلل الجمل ويفهم بنيتها النحوية
 */
public class ArabicParser {
    
    private ArabicLexicon lexicon;
    
    // أنواع الجمل
    public enum SentenceType {
        VERBAL,         // جملة فعلية
        NOMINAL,        // جملة اسمية
        CONDITIONAL,    // جملة شرطية
        INTERROGATIVE,  // جملة استفهامية
        EXCLAMATORY,    // جملة تعجبية
        NEGATIVE,       // جملة منفية
        IMPERATIVE,     // جملة أمر
        PROHIBITIVE,    // جملة نهي
        OPTATIVE,       // جملة دعاء/تمنٍ
        INFORMATIVE     // جملة خبرية
    }
    
    // عناصر الجملة
    public static class SentenceElement {
        String word;
        ArabicLexicon.WordType type;
        String role;           // دورها في الجملة
        int position;
        Map<String, Double> features;
        
        public SentenceElement(String word, ArabicLexicon.WordType type, String role, int position) {
            this.word = word;
            this.type = type;
            this.role = role;
            this.position = position;
            this.features = new HashMap<>();
        }
        
        public void addFeature(String key, double value) {
            features.put(key, value);
        }
    }
    
    // نتيجة التحليل
    public static class ParseResult {
        String originalText;
        SentenceType sentenceType;
        List<SentenceElement> elements;
        String subject;        // المبتدأ/الفاعل
        String predicate;      // الخبر/المفعول
        String verb;           // الفعل
        Map<String, Object> metadata;
        double confidence;
        boolean isComplete;
        List<String> errors;
        
        public ParseResult(String text) {
            this.originalText = text;
            this.elements = new ArrayList<>();
            this.metadata = new HashMap<>();
            this.errors = new ArrayList<>();
            this.confidence = 0.5;
            this.isComplete = false;
        }
        
        public void addElement(SentenceElement element) {
            elements.add(element);
        }
        
        public void addError(String error) {
            errors.add(error);
        }
    }
    
    // قاعدة نحوية
    public static class SyntaxRule {
        String name;
        String description;
        List<ArabicLexicon.WordType> pattern;
        double confidence;
        boolean isValid;
        
        public SyntaxRule(String name, String description) {
            this.name = name;
            this.description = description;
            this.pattern = new ArrayList<>();
            this.confidence = 0.5;
            this.isValid = true;
        }
    }
    
    // التصريف
    public static class Conjugation {
        String root;
        String form;
        String tense;          // زمن
        String person;         // شخص
        String gender;         // جنس
        String number;         // عدد
        String voice;          // مبني للمعلوم/المجهول
        
        public Conjugation(String root) {
            this.root = root;
        }
    }
    
    private List<SyntaxRule> syntaxRules;
    
    public ArabicParser(ArabicLexicon lexicon) {
        this.lexicon = lexicon;
        this.syntaxRules = new ArrayList<>();
        initializeSyntaxRules();
    }
    
    private void initializeSyntaxRules() {
        // جملة فعلية: فعل + فاعل
        SyntaxRule verbal1 = new SyntaxRule("verbal_basic", "جملة فعلية أساسية");
        verbal1.pattern.add(ArabicLexicon.WordType.VERB);
        verbal1.pattern.add(ArabicLexicon.WordType.NOUN);
        syntaxRules.add(verbal1);
        
        // جملة فعلية: فعل + فاعل + مفعول
        SyntaxRule verbal2 = new SyntaxRule("verbal_with_object", "جملة فعلية بمفعول");
        verbal2.pattern.add(ArabicLexicon.WordType.VERB);
        verbal2.pattern.add(ArabicLexicon.WordType.NOUN);
        verbal2.pattern.add(ArabicLexicon.WordType.NOUN);
        syntaxRules.add(verbal2);
        
        // جملة اسمية: مبتدأ + خبر
        SyntaxRule nominal1 = new SyntaxRule("nominal_basic", "جملة اسمية أساسية");
        nominal1.pattern.add(ArabicLexicon.WordType.NOUN);
        nominal1.pattern.add(ArabicLexicon.WordType.NOUN);
        syntaxRules.add(nominal1);
        
        // جملة اسمية: مبتدأ + خبر صفة
        SyntaxRule nominal2 = new SyntaxRule("nominal_adjective", "جملة اسمية بصفة");
        nominal2.pattern.add(ArabicLexicon.WordType.NOUN);
        nominal2.pattern.add(ArabicLexicon.WordType.ADJECTIVE);
        syntaxRules.add(nominal2);
        
        // جملة استفهام: أداة استفهام + جملة
        SyntaxRule interrogative = new SyntaxRule("interrogative", "جملة استفهام");
        interrogative.pattern.add(ArabicLexicon.WordType.INTERROGATIVE);
        interrogative.pattern.add(ArabicLexicon.WordType.VERB);
        interrogative.pattern.add(ArabicLexicon.WordType.NOUN);
        syntaxRules.add(interrogative);
        
        // جملة منفية: أداة نفي + جملة
        SyntaxRule negative = new SyntaxRule("negative", "جملة منفية");
        negative.pattern.add(ArabicLexicon.WordType.NEGATION);
        negative.pattern.add(ArabicLexicon.WordType.VERB);
        syntaxRules.add(negative);
        
        // جملة بشرط: إذا + جملة
        SyntaxRule conditional = new SyntaxRule("conditional", "جملة شرطية");
        conditional.pattern.add(ArabicLexicon.WordType.PARTICLE);
        conditional.pattern.add(ArabicLexicon.WordType.VERB);
        syntaxRules.add(conditional);
    }
    
    /**
     * تحليل نص كامل
     */
    public List<ParseResult> parseText(String text) {
        List<ParseResult> results = new ArrayList<>();
        
        // تقسيم النص إلى جمل
        String[] sentences = splitIntoSentences(text);
        
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                ParseResult result = parseSentence(sentence.trim());
                results.add(result);
            }
        }
        
        return results;
    }
    
    /**
     * تقسيم النص إلى جمل
     */
    private String[] splitIntoSentences(String text) {
        // تقسيم بناءً على علامات الترقيم
        return text.split("[.!?؟]+");
    }
    
    /**
     * تحليل جملة واحدة
     */
    public ParseResult parseSentence(String sentence) {
        ParseResult result = new ParseResult(sentence);
        
        // تقسيم الجملة إلى كلمات
        List<String> tokens = tokenize(sentence);
        
        // تحديد نوع الجملة
        result.sentenceType = determineSentenceType(tokens);
        
        // تحليل الكلمات
        int position = 0;
        for (String token : tokens) {
            SentenceElement element = analyzeToken(token, position);
            result.addElement(element);
            position++;
        }
        
        // تحديد العناصر الأساسية
        identifyCoreElements(result);
        
        // التحقق من اكتمال الجملة
        validateSentence(result);
        
        return result;
    }
    
    /**
     * تقسيم الجملة إلى كلمات (tokenization)
     */
    private List<String> tokenize(String sentence) {
        List<String> tokens = new ArrayList<>();
        
        // إزالة التشكيل
        String normalized = removeTashkeel(sentence);
        
        // تقسيم بناءً على المسافات
        String[] words = normalized.split("\\s+");
        
        for (String word : words) {
            word = word.trim();
            if (!word.isEmpty()) {
                // معالجة التعريف "ال"
                if (word.startsWith("ال") && word.length() > 2) {
                    tokens.add("ال");
                    tokens.add(word.substring(2));
                } else {
                    tokens.add(word);
                }
            }
        }
        
        return tokens;
    }
    
    /**
     * إزالة التشكيل
     */
    private String removeTashkeel(String text) {
        return text.replaceAll("[\\u064B-\\u065F\\u0670]", "");
    }
    
    /**
     * تحديد نوع الجملة
     */
    private SentenceType determineSentenceType(List<String> tokens) {
        if (tokens.isEmpty()) return SentenceType.INFORMATIVE;
        
        String firstWord = tokens.get(0);
        
        // التحقق من أدوات الاستفهام
        ArabicLexicon.Word word = lexicon.getWordByForm(firstWord);
        if (word != null) {
            if (word.type == ArabicLexicon.WordType.INTERROGATIVE) {
                return SentenceType.INTERROGATIVE;
            }
            if (word.type == ArabicLexicon.WordType.NEGATION) {
                return SentenceType.NEGATIVE;
            }
        }
        
        // التحقق من حروف الشرط
        if (firstWord.equals("إذا") || firstWord.equals("لو") || firstWord.equals("لولا")) {
            return SentenceType.CONDITIONAL;
        }
        
        // التحقق من أدوات التمني
        if (firstWord.equals("ليت") || firstWord.equals("لعل") || firstWord.equals("يا")) {
            return SentenceType.OPTATIVE;
        }
        
        // التحقق إذا كانت الجملة تبدأ بفعل
        if (word != null && word.type == ArabicLexicon.WordType.VERB) {
            return SentenceType.VERBAL;
        }
        
        // إذا بدأت باسم، فهي جملة اسمية
        if (word != null && word.type == ArabicLexicon.WordType.NOUN) {
            return SentenceType.NOMINAL;
        }
        
        return SentenceType.INFORMATIVE;
    }
    
    /**
     * تحليل كلمة
     */
    private SentenceElement analyzeToken(String token, int position) {
        ArabicLexicon.Word word = lexicon.getWordByForm(token);
        
        if (word == null) {
            // محاولة إيجاد الكلمة بدون "ال"
            if (token.startsWith("ال")) {
                word = lexicon.getWordByForm(token.substring(2));
            }
        }
        
        if (word == null) {
            // كلمة غير معروفة
            SentenceElement element = new SentenceElement(token, ArabicLexicon.WordType.NOUN, "unknown", position);
            element.addFeature("known", 0.0);
            return element;
        }
        
        // استخدام الكلمة لزيادة الألفة
        word.use();
        
        String role = determineRole(word.type, position);
        SentenceElement element = new SentenceElement(token, word.type, role, position);
        element.addFeature("known", word.familiarity);
        element.addFeature("emotional_intensity", getEmotionalIntensity(word));
        
        return element;
    }
    
    /**
     * تحديد دور الكلمة في الجملة
     */
    private String determineRole(ArabicLexicon.WordType type, int position) {
        switch (type) {
            case VERB:
                return position == 0 ? "فعل" : "فعل تابع";
            case NOUN:
                if (position == 0) return "مبتدأ";
                if (position == 1) return "خبر/فاعل";
                return "مفعول/نعت";
            case ADJECTIVE:
                return "صفة";
            case PREPOSITION:
                return "حرف جر";
            case CONJUNCTION:
                return "حرف عطف";
            case INTERROGATIVE:
                return "أداة استفهام";
            case NEGATION:
                return "أداة نفي";
            case PARTICLE:
                return "حرف";
            default:
                return "غير محدد";
        }
    }
    
    /**
     * الحصول على شدة العاطفة
     */
    private double getEmotionalIntensity(ArabicLexicon.Word word) {
        if (word.emotions.isEmpty()) return 0.0;
        
        double sum = 0;
        for (double value : word.emotions.values()) {
            sum += value;
        }
        return sum / word.emotions.size();
    }
    
    /**
     * تحديد العناصر الأساسية للجملة
     */
    private void identifyCoreElements(ParseResult result) {
        List<SentenceElement> elements = result.elements;
        
        if (elements.isEmpty()) return;
        
        // تحديد الفعل/المبتدأ
        SentenceElement first = elements.get(0);
        
        if (result.sentenceType == SentenceType.VERBAL || 
            first.type == ArabicLexicon.WordType.VERB) {
            result.verb = first.word;
            
            // البحث عن الفاعل
            for (int i = 1; i < elements.size(); i++) {
                SentenceElement elem = elements.get(i);
                if (elem.type == ArabicLexicon.WordType.NOUN) {
                    if (result.subject == null) {
                        result.subject = elem.word;
                        elem.role = "فاعل";
                    } else if (result.predicate == null) {
                        result.predicate = elem.word;
                        elem.role = "مفعول به";
                    }
                }
            }
        } else {
            // جملة اسمية
            result.subject = first.word;
            first.role = "مبتدأ";
            
            // البحث عن الخبر
            for (int i = 1; i < elements.size(); i++) {
                SentenceElement elem = elements.get(i);
                if (elem.type == ArabicLexicon.WordType.NOUN || 
                    elem.type == ArabicLexicon.WordType.ADJECTIVE) {
                    result.predicate = elem.word;
                    elem.role = "خبر";
                    break;
                }
            }
        }
    }
    
    /**
     * التحقق من اكتمال الجملة
     */
    private void validateSentence(ParseResult result) {
        boolean hasSubject = result.subject != null;
        boolean hasPredicate = result.predicate != null || result.verb != null;
        
        result.isComplete = hasSubject && hasPredicate;
        
        if (!hasSubject) {
            result.addError("الجملة تفتقر إلى فاعل/مبتدأ");
        }
        if (!hasPredicate) {
            result.addError("الجملة تفتقر إلى خبر/فعل");
        }
        
        // حساب الثقة
        double confidence = 0.5;
        if (result.isComplete) confidence += 0.3;
        if (result.errors.isEmpty()) confidence += 0.2;
        result.confidence = Math.min(1.0, confidence);
    }
    
    /**
     * الحصول على معلومات إضافية عن الكلمة
     */
    public Map<String, Object> getWordInfo(String word) {
        Map<String, Object> info = new HashMap<>();
        
        ArabicLexicon.Word w = lexicon.getWordByForm(word);
        if (w == null) {
            info.put("known", false);
            return info;
        }
        
        info.put("known", true);
        info.put("root", w.root);
        info.put("type", w.type);
        info.put("meanings", w.meanings);
        info.put("emotions", w.emotions);
        info.put("familiarity", w.familiarity);
        info.put("usage_count", w.usageCount);
        
        return info;
    }
    
    /**
     * التحقق من صحة الجملة
     */
    public boolean isValidSentence(String sentence) {
        ParseResult result = parseSentence(sentence);
        return result.isComplete && result.errors.isEmpty();
    }
    
    /**
     * اقتراح تصحيحات للجملة
     */
    public List<String> suggestCorrections(String sentence) {
        List<String> suggestions = new ArrayList<>();
        ParseResult result = parseSentence(sentence);
        
        if (result.isComplete) {
            return suggestions; // لا حاجة للتصحيح
        }
        
        // اقتراح إضافة فاعل
        if (result.subject == null && result.verb != null) {
            suggestions.add("هل تقصد: " + result.verb + " أنا؟");
            suggestions.add("هل تقصد: " + result.verb + " هو؟");
        }
        
        // اقتراح إضافة خبر
        if (result.predicate == null && result.subject != null) {
            suggestions.add("هل تقصد: " + result.subject + " موجود؟");
            suggestions.add("هل تقصد: " + result.subject + " جميل؟");
        }
        
        return suggestions;
    }
    
    /**
     * استخراج الكلمات المفتاحية
     */
    public List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        List<ParseResult> results = parseText(text);
        
        for (ParseResult result : results) {
            for (SentenceElement element : result.elements) {
                if (element.type == ArabicLexicon.WordType.NOUN ||
                    element.type == ArabicLexicon.WordType.VERB) {
                    keywords.add(element.word);
                }
            }
        }
        
        return keywords;
    }
    
    /**
     * استخراج العواطف من النص
     */
    public Map<String, Double> extractEmotions(String text) {
        Map<String, Double> emotions = new HashMap<>();
        List<ParseResult> results = parseText(text);
        
        for (ParseResult result : results) {
            for (SentenceElement element : result.elements) {
                ArabicLexicon.Word word = lexicon.getWordByForm(element.word);
                if (word != null) {
                    for (Map.Entry<String, Double> entry : word.emotions.entrySet()) {
                        emotions.merge(entry.getKey(), entry.getValue(), Double::sum);
                    }
                }
            }
        }
        
        // تطبيع
        double max = emotions.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max > 0) {
            for (String key : emotions.keySet()) {
                emotions.put(key, emotions.get(key) / max);
            }
        }
        
        return emotions;
    }
}

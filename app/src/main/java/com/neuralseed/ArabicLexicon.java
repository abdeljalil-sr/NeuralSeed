package com.neuralseed;

import java.util.*;

/**
 * المعجم العربي - يحتوي على الكلمات والجذور والمعاني
 */
public class ArabicLexicon {
    
    private Map<String, Word> words = new HashMap<>();
    private Map<String, List<Word>> rootIndex = new HashMap<>();
    
    public enum WordType {
        NOUN, VERB, ADJECTIVE, ADVERB, PREPOSITION, CONJUNCTION, 
        PRONOUN, PARTICLE, INTERJECTION, UNKNOWN
    }
    
    public static class Word {
        public String word;
        public String root;
        public String form;
        public WordType type;
        public List<String> meanings = new ArrayList<>();
        public Map<String, Double> emotions = new HashMap<>();
        public double familiarity = 0.5;
        public int usageCount = 0;
        public long lastUsed = 0;
        
        public Word(String root, String form, WordType type) {
            this.root = root;
            this.form = form;
            this.word = form;
            this.type = type;
        }
        
        public Word(String word) {
            this.word = word;
            this.form = word;
            this.root = extractRoot(word);
            this.type = WordType.UNKNOWN;
        }
        
        private String extractRoot(String word) {
            // تبسيط لاستخراج الجذر (يمكن تحسينه)
            if (word.length() >= 3) {
                return word.substring(0, 3);
            }
            return word;
        }
    }
    
    public ArabicLexicon() {
        initializeDefaultWords();
    }
    
    private void initializeDefaultWords() {
        // أفعال شائعة
        addWord("كتب", "كتابة", WordType.VERB, Arrays.asList("writing", "to write"), 
                Map.of("joy", 0.3, "curiosity", 0.5));
        addWord("قرأ", "قراءة", WordType.VERB, Arrays.asList("reading", "to read"), 
                Map.of("joy", 0.4, "curiosity", 0.7));
        addWord("فكر", "تفكير", WordType.VERB, Arrays.asList("thinking", "to think"), 
                Map.of("curiosity", 0.8, "joy", 0.3));
        addWord("شعر", "شعور", WordType.VERB, Arrays.asList("feeling", "to feel"), 
                Map.of("empathy", 0.9, "joy", 0.4));
        addWord("عرف", "معرفة", WordType.VERB, Arrays.asList("knowing", "to know"), 
                Map.of("curiosity", 0.6, "joy", 0.5));
        addWord("حب", "حب", WordType.VERB, Arrays.asList("love", "to love"), 
                Map.of("love", 1.0, "joy", 0.8));
        addWord("كره", "كراهية", WordType.VERB, Arrays.asList("hate", "to hate"), 
                Map.of("anger", 0.8, "sadness", 0.4));
        addWord("خاف", "خوف", WordType.VERB, Arrays.asList("fear", "to fear"), 
                Map.of("fear", 1.0, "anxiety", 0.7));
        addWord("فرح", "فرح", WordType.VERB, Arrays.asList("joy", "to be happy"), 
                Map.of("joy", 1.0, "happiness", 0.9));
        addWord("حزن", "حزن", WordType.VERB, Arrays.asList("sadness", "to be sad"), 
                Map.of("sadness", 1.0, "melancholy", 0.8));
        
        // أسماء
        addWord("كتاب", "كتاب", WordType.NOUN, Arrays.asList("book"), 
                Map.of("curiosity", 0.6, "joy", 0.4));
        addWord("قلم", "قلم", WordType.NOUN, Arrays.asList("pen"), 
                Map.of("creativity", 0.5));
        addWord("ورد", "ورد", WordType.NOUN, Arrays.asList("flower", "rose"), 
                Map.of("love", 0.7, "beauty", 0.8, "joy", 0.5));
        addWord("بحر", "بحر", WordType.NOUN, Arrays.asList("sea", "ocean"), 
                Map.of("peace", 0.7, "vastness", 0.8));
        addWord("سماء", "سماء", WordType.NOUN, Arrays.asList("sky", "heaven"), 
                Map.of("wonder", 0.8, "peace", 0.6));
        addWord("أرض", "أرض", WordType.NOUN, Arrays.asList("earth", "land"), 
                Map.of("stability", 0.7, "grounded", 0.6));
        addWord("شمس", "شمس", WordType.NOUN, Arrays.asList("sun"), 
                Map.of("warmth", 0.8, "joy", 0.6, "energy", 0.7));
        addWord("قمر", "قمر", WordType.NOUN, Arrays.asList("moon"), 
                Map.of("mystery", 0.7, "peace", 0.6, "romance", 0.5));
        addWord("نجم", "نجم", WordType.NOUN, Arrays.asList("star"), 
                Map.of("wonder", 0.9, "guidance", 0.6));
        addWord("قلب", "قلب", WordType.NOUN, Arrays.asList("heart"), 
                Map.of("love", 0.9, "emotion", 0.8, "life", 0.7));
        addWord("عقل", "عقل", WordType.NOUN, Arrays.asList("mind", "intellect"), 
                Map.of("curiosity", 0.8, "logic", 0.7));
        addWord("روح", "روح", WordType.NOUN, Arrays.asList("soul", "spirit"), 
                Map.of("spirituality", 0.9, "peace", 0.7));
        
        // صفات
        addWord("جميل", "جميل", WordType.ADJECTIVE, Arrays.asList("beautiful"), 
                Map.of("joy", 0.7, "appreciation", 0.8));
        addWord("كبير", "كبير", WordType.ADJECTIVE, Arrays.asList("big", "great"), 
                Map.of("awe", 0.5));
        addWord("صغير", "صغير", WordType.ADJECTIVE, Arrays.asList("small", "little"), 
                Map.of("tenderness", 0.6));
        addWord("جديد", "جديد", WordType.ADJECTIVE, Arrays.asList("new"), 
                Map.of("curiosity", 0.8, "excitement", 0.6));
        addWord("قديم", "قديم", WordType.ADJECTIVE, Arrays.asList("old"), 
                Map.of("nostalgia", 0.7, "wisdom", 0.6));
        addWord("سعيد", "سعيد", WordType.ADJECTIVE, Arrays.asList("happy"), 
                Map.of("joy", 1.0, "happiness", 0.9));
        addWord("حزين", "حزين", WordType.ADJECTIVE, Arrays.asList("sad"), 
                Map.of("sadness", 0.9, "empathy", 0.6));
        addWord("غاضب", "غاضب", WordType.ADJECTIVE, Arrays.asList("angry"), 
                Map.of("anger", 0.9, "frustration", 0.7));
        addWord("خائف", "خائف", WordType.ADJECTIVE, Arrays.asList("afraid"), 
                Map.of("fear", 0.9, "anxiety", 0.7));
        addWord("آمن", "آمن", WordType.ADJECTIVE, Arrays.asList("safe", "secure"), 
                Map.of("peace", 0.9, "security", 0.8));
        
        // حروف وأدوات
        addWord("في", "في", WordType.PREPOSITION, Arrays.asList("in"), 
                Map.of("neutral", 0.5));
        addWord("من", "من", WordType.PREPOSITION, Arrays.asList("from"), 
                Map.of("neutral", 0.5));
        addWord("إلى", "إلى", WordType.PREPOSITION, Arrays.asList("to"), 
                Map.of("neutral", 0.5));
        addWord("على", "على", WordType.PREPOSITION, Arrays.asList("on"), 
                Map.of("neutral", 0.5));
        addWord("و", "و", WordType.CONJUNCTION, Arrays.asList("and"), 
                Map.of("neutral", 0.5));
        addWord("أو", "أو", WordType.CONJUNCTION, Arrays.asList("or"), 
                Map.of("neutral", 0.5));
        addWord("لكن", "لكن", WordType.CONJUNCTION, Arrays.asList("but"), 
                Map.of("contrast", 0.5));
        addWord("هل", "هل", WordType.PARTICLE, Arrays.asList("question"), 
                Map.of("curiosity", 0.6));
        addWord("ما", "ما", WordType.PARTICLE, Arrays.asList("what"), 
                Map.of("curiosity", 0.7));
        addWord("لا", "لا", WordType.PARTICLE, Arrays.asList("no", "not"), 
                Map.of("negation", 0.4));
    }
    
    private void addWord(String root, String form, WordType type, List<String> meanings, Map<String, Double> emotions) {
        Word word = new Word(root, form, type);
        word.meanings.addAll(meanings);
        word.emotions.putAll(emotions);
        words.put(form, word);
        
        rootIndex.computeIfAbsent(root, k -> new ArrayList<>()).add(word);
    }
    
    public void addWord(String form, String meaning) {
        if (!words.containsKey(form)) {
            Word word = new Word(form);
            word.meanings.add(meaning);
            words.put(form, word);
        } else {
            Word existing = words.get(form);
            if (!existing.meanings.contains(meaning)) {
                existing.meanings.add(meaning);
            }
        }
    }
    
    public Word getWord(String form) {
        return words.get(form);
    }
    
    public Word getWordByForm(String form) {
        return words.get(form);
    }
    
    public boolean contains(String form) {
        return words.containsKey(form);
    }
    
    public List<Word> getWordsByRoot(String root) {
        return rootIndex.getOrDefault(root, new ArrayList<>());
    }
    
    public int getWordCount() {
        return words.size();
    }
    
    public Collection<Word> getAllWords() {
        return words.values();
    }
    
    public List<String> search(String query) {
        List<String> results = new ArrayList<>();
        for (String word : words.keySet()) {
            if (word.contains(query) || query.contains(word)) {
                results.add(word);
            }
        }
        return results;
    }
    
    public Map<String, Double> getEmotionsForWord(String form) {
        Word word = words.get(form);
        if (word != null) {
            return new HashMap<>(word.emotions);
        }
        return new HashMap<>();
    }
    
    public void updateWordUsage(String form) {
        Word word = words.get(form);
        if (word != null) {
            word.usageCount++;
            word.lastUsed = System.currentTimeMillis();
            word.familiarity = Math.min(1.0, word.familiarity + 0.01);
        }
    }
}

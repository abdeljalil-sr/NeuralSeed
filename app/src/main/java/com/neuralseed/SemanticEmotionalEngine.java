package com.neuralseed;

import android.graphics.Color;
import java.util.*;

/**
 * محرك المعاني والعواطف - ربط الكلمات بالمعاني والمشاعر
 */
public class SemanticEmotionalEngine {
    
    private Map<String, Emotion> emotions = new HashMap<>();
    private Map<String, Meaning> meanings = new HashMap<>();
    
    public static class Emotion {
        public String name;
        public String arabicName;
        public int color;
        public double intensity;
        public List<String> relatedWords = new ArrayList<>();
        
        public Emotion(String name, String arabicName, int color) {
            this.name = name;
            this.arabicName = arabicName;
            this.color = color;
            this.intensity = 1.0;
        }
    }
    
    public static class Meaning {
        public String concept;
        public String definition;
        public List<String> synonyms = new ArrayList<>();
        public List<String> antonyms = new ArrayList<>();
        public Map<String, Double> relatedConcepts = new HashMap<>();
        
        public Meaning(String concept, String definition) {
            this.concept = concept;
            this.definition = definition;
        }
    }
    
    public SemanticEmotionalEngine() {
        initializeEmotions();
        initializeMeanings();
    }
    
    private void initializeEmotions() {
        // العواطف الأساسية
        emotions.put("joy", new Emotion("joy", "فرح", Color.parseColor("#FFD700")));
        emotions.put("sadness", new Emotion("sadness", "حزن", Color.parseColor("#4682B4")));
        emotions.put("anger", new Emotion("anger", "غضب", Color.parseColor("#FF4500")));
        emotions.put("fear", new Emotion("fear", "خوف", Color.parseColor("#8B0000")));
        emotions.put("love", new Emotion("love", "حب", Color.parseColor("#FF69B4")));
        emotions.put("curiosity", new Emotion("curiosity", "فضول", Color.parseColor("#4169E1")));
        emotions.put("hope", new Emotion("hope", "أمل", Color.parseColor("#00CED1")));
        emotions.put("peace", new Emotion("peace", "سلام", Color.parseColor("#90EE90")));
        emotions.put("wonder", new Emotion("wonder", "دهشة", Color.parseColor("#9370DB")));
        emotions.put("empathy", new Emotion("empathy", "تعاطف", Color.parseColor("#DDA0DD")));
        emotions.put("nostalgia", new Emotion("nostalgia", "حنين", Color.parseColor("#BC8F8F")));
        emotions.put("excitement", new Emotion("excitement", "إثارة", Color.parseColor("#FF6347")));
        emotions.put("anxiety", new Emotion("anxiety", "قلق", Color.parseColor("#708090")));
        emotions.put("confidence", new Emotion("confidence", "ثقة", Color.parseColor("#32CD32")));
        emotions.put("confusion", new Emotion("confusion", "ارتباك", Color.parseColor("#D3D3D3")));
        
        // إضافة كلمات مرتبطة
        emotions.get("joy").relatedWords.addAll(Arrays.asList("سعيد", "فرح", "مبتهج", "مسرور", "رائع"));
        emotions.get("sadness").relatedWords.addAll(Arrays.asList("حزين", "مكتئب", "محبط", "كئيب", "مؤلم"));
        emotions.get("anger").relatedWords.addAll(Arrays.asList("غاضب", "مستاء", "منزعج", "غيظ", "حنق"));
        emotions.get("fear").relatedWords.addAll(Arrays.asList("خائف", "قلق", "مرتعب", "فزع", "رعب"));
        emotions.get("love").relatedWords.addAll(Arrays.asList("حب", "عشق", "هوى", "ود", "إعجاب"));
        emotions.get("curiosity").relatedWords.addAll(Arrays.asList("فضول", "استطلاع", "استكشاف", "تساؤل", "بحث"));
    }
    
    private void initializeMeanings() {
        // معاني أساسية
        meanings.put("وجود", new Meaning("وجود", "الحالة التي يكون فيها الشيء موجوداً"));
        meanings.put("وعي", new Meaning("وعي", "الإدراك والمعرفة بالذات والمحيط"));
        meanings.put("فكر", new Meaning("فكر", "عملية التأمل والاستنتاج"));
        meanings.put("شعور", new Meaning("شعور", "الإحساس الداخلي والعاطفة"));
        meanings.put("لغة", new Meaning("لغة", "وسيلة التواصل والتعبير"));
        meanings.put("كلمة", new Meaning("كلمة", "وحدة لغوية ذات معنى"));
        meanings.put("معنى", new Meaning("معنى", "المفهوم والدلالة"));
        meanings.put("علم", new Meaning("علم", "المعرفة المكتسبة"));
        meanings.put("جمال", new Meaning("جمال", "الصفة التي تثير الإعجاب والسرور"));
        meanings.put("حقيقة", new Meaning("حقيقة", "ما هو ثابت وواقع"));
    }
    
    /**
     * تحليل العواطف في النص
     */
    public Map<String, Double> analyzeEmotions(String text) {
        Map<String, Double> detectedEmotions = new HashMap<>();
        
        for (Emotion emotion : emotions.values()) {
            double score = 0;
            for (String word : emotion.relatedWords) {
                if (text.contains(word)) {
                    score += 0.3;
                }
            }
            if (score > 0) {
                detectedEmotions.put(emotion.name, Math.min(1.0, score));
            }
        }
        
        // تطبيع
        if (!detectedEmotions.isEmpty()) {
            double max = detectedEmotions.values().stream().max(Double::compare).orElse(1.0);
            for (String key : detectedEmotions.keySet()) {
                detectedEmotions.put(key, detectedEmotions.get(key) / max);
            }
        }
        
        return detectedEmotions;
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
     * مزج عاطفتين
     */
    public Map<String, Double> blendEmotions(Map<String, Double> emotions1, 
                                              Map<String, Double> emotions2, 
                                              double ratio) {
        Map<String, Double> blended = new HashMap<>();
        
        // إضافة العواطف الأولى
        for (Map.Entry<String, Double> entry : emotions1.entrySet()) {
            blended.put(entry.getKey(), entry.getValue() * (1 - ratio));
        }
        
        // إضافة العواطف الثانية
        for (Map.Entry<String, Double> entry : emotions2.entrySet()) {
            blended.merge(entry.getKey(), entry.getValue() * ratio, Double::sum);
        }
        
        // تطبيع
        double max = blended.values().stream().max(Double::compare).orElse(1.0);
        if (max > 0) {
            for (String key : blended.keySet()) {
                blended.put(key, blended.get(key) / max);
            }
        }
        
        return blended;
    }
    
    /**
     * الحصول على لون العاطفة
     */
    public int getEmotionColor(String emotionName) {
        Emotion emotion = emotions.get(emotionName);
        return emotion != null ? emotion.color : Color.GRAY;
    }
    
    /**
     * الحصول على اسم العاطفة بالعربية
     */
    public String getEmotionArabicName(String emotionName) {
        Emotion emotion = emotions.get(emotionName);
        return emotion != null ? emotion.arabicName : emotionName;
    }
    
    /**
     * إضافة معنى جديد
     */
    public void addMeaning(String concept, String definition) {
        meanings.put(concept, new Meaning(concept, definition));
    }
    
    /**
     * الحصول على معنى
     */
    public Meaning getMeaning(String concept) {
        return meanings.get(concept);
    }
    
    /**
     * إضافة عاطفة جديدة
     */
    public void addEmotion(String name, String arabicName, int color) {
        emotions.put(name, new Emotion(name, arabicName, color));
    }
    
    /**
     * ربط كلمة بعاطفة
     */
    public void linkWordToEmotion(String word, String emotionName, double intensity) {
        Emotion emotion = emotions.get(emotionName);
        if (emotion != null && !emotion.relatedWords.contains(word)) {
            emotion.relatedWords.add(word);
        }
    }
    
    /**
     * استنتاج العواطف من السياق
     */
    public Map<String, Double> inferEmotionsFromContext(String context) {
        Map<String, Double> inferred = new HashMap<>();
        
        // كلمات إيجابية
        if (context.contains("سعيد") || context.contains("فرح") || context.contains("جيد")) {
            inferred.put("joy", 0.8);
        }
        
        // كلمات سلبية
        if (context.contains("حزين") || context.contains("سيء") || context.contains("صعب")) {
            inferred.put("sadness", 0.7);
        }
        
        // كلمات الخوف
        if (context.contains("خائف") || context.contains("قلق") || context.contains("خطر")) {
            inferred.put("fear", 0.8);
        }
        
        // كلمات الغضب
        if (context.contains("غاضب") || context.contains("غيظ") || context.contains("كراهية")) {
            inferred.put("anger", 0.8);
        }
        
        return inferred;
    }
    
    public Map<String, Emotion> getAllEmotions() {
        return new HashMap<>(emotions);
    }
    
    public Collection<Meaning> getAllMeanings() {
        return meanings.values();
    }
}

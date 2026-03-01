package com.neuralseed;

import java.util.*;

/**
 * المعجم العربي - يحتوي على جميع الكلمات العربية الأساسية
 * مع تصنيفاتها النحوية والدلالية والعاطفية
 */
public class ArabicLexicon {
    
    public enum WordType {
        VERB, NOUN, PARTICLE, ADJECTIVE, ADVERB, PRONOUN, 
        NUMBER, PREPOSITION, CONJUNCTION, INTERROGATIVE, NEGATION, EMPHASIS
    }
    
    public static class Word {
        String root, word;
        WordType type;
        List<String> categories;
        Map<String, Double> emotions;
        List<String> meanings;
        double familiarity;
        long lastUsed;
        int usageCount;
        
        public Word(String root, String word, WordType type) {
            this.root = root;
            this.word = word;
            this.type = type;
            this.categories = new ArrayList<>();
            this.emotions = new HashMap<>();
            this.meanings = new ArrayList<>();
            this.familiarity = 0.5;
            this.lastUsed = System.currentTimeMillis();
            this.usageCount = 0;
        }
        
        public void use() {
            usageCount++;
            lastUsed = System.currentTimeMillis();
            familiarity = Math.min(1.0, familiarity + 0.01);
        }
                // أضف هذه الدوال هنا
        public void addMeaning(String meaning) {
            if (!this.meanings.contains(meaning)) {
                this.meanings.add(meaning);
            }
        }

        public void addEmotion(String emotion, double intensity) {
            this.emotions.put(emotion, intensity);
        }

    }
    
    private Map<String, Word> wordsByRoot = new HashMap<>();
    private Map<String, Word> wordsByForm = new HashMap<>();
    private Map<WordType, List<Word>> wordsByType = new EnumMap<>(WordType.class);
    
    public ArabicLexicon() {
        for (WordType type : WordType.values()) {
            wordsByType.put(type, new ArrayList<>());
        }
        initializeVerbs();
        initializeNouns();
        initializeParticles();
        initializeAdjectives();
    }
    
    private void addWord(String word, String root, WordType type, 
                        Map<String, Double> emotions, List<String> meanings) {
        Word w = new Word(root, word, type);
        if (emotions != null) w.emotions.putAll(emotions);
        if (meanings != null) w.meanings.addAll(meanings);
        wordsByRoot.put(root, w);
        wordsByForm.put(word, w);
        wordsByType.get(type).add(w);
    }
    
    private void initializeVerbs() {
        // أفعال الحركة
        addWord("ذهب", "ذهب", WordType.VERB, 
               Map.of("curiosity", 0.6), Arrays.asList("الانتقال", "المغادرة"));
        addWord("جاء", "جيء", WordType.VERB, 
               Map.of("joy", 0.5), Arrays.asList("الوصول", "القدوم"));
        addWord("رجع", "رجع", WordType.VERB, 
               Map.of("stable", 0.5), Arrays.asList("العودة"));
        addWord("دخل", "دخل", WordType.VERB, 
               Map.of("curiosity", 0.7), Arrays.asList("الدخول"));
        addWord("خرج", "خرج", WordType.VERB, 
               Map.of("freedom", 0.6), Arrays.asList("الخروج"));
        
        // أفعال الكلام
        addWord("قال", "قول", WordType.VERB, 
               Map.of("communication", 0.8), Arrays.asList("الكلام", "الحديث"));
        addWord("سأل", "سؤل", WordType.VERB, 
               Map.of("curiosity", 0.9), Arrays.asList("السؤال", "الاستفسار"));
        addWord("أجاب", "جواب", WordType.VERB, 
               Map.of("help", 0.7), Arrays.asList("الإجابة"));
        addWord("شرح", "شرح", WordType.VERB, 
               Map.of("clarity", 0.8), Arrays.asList("الشرح", "التفسير"));
        
        // أفعال التفكير
        addWord("فكر", "فكر", WordType.VERB, 
               Map.of("curiosity", 0.8), Arrays.asList("التفكير"));
        addWord("علم", "علم", WordType.VERB, 
               Map.of("joy", 0.6), Arrays.asList("المعرفة", "التعلم"));
        addWord("فهم", "فهم", WordType.VERB, 
               Map.of("clarity", 0.9), Arrays.asList("الفهم"));
        addWord("ذكر", "ذكر", WordType.VERB, 
               Map.of("memory", 0.8), Arrays.asList("التذكر"));
        
        // أفعال الشعور
        addWord("أحب", "حب", WordType.VERB, 
               Map.of("love", 1.0, "joy", 0.9), Arrays.asList("الحب"));
        addWord("كره", "كره", WordType.VERB, 
               Map.of("anger", 0.8), Arrays.asList("الكراهية"));
        addWord("خاف", "خوف", WordType.VERB, 
               Map.of("fear", 1.0), Arrays.asList("الخوف"));
        addWord("فرح", "فرح", WordType.VERB, 
               Map.of("joy", 1.0), Arrays.asList("الفرح"));
        addWord("حزن", "حزن", WordType.VERB, 
               Map.of("sadness", 1.0), Arrays.asList("الحزن"));
        addWord("غضب", "غضب", WordType.VERB, 
               Map.of("anger", 1.0), Arrays.asList("الغضب"));
        
        // أفعال الفعل
        addWord("عمل", "عمل", WordType.VERB, 
               Map.of("achievement", 0.8), Arrays.asList("العمل"));
        addWord("فعل", "فعل", WordType.VERB, 
               Map.of("action", 0.9), Arrays.asList("الفعل"));
        addWord("صنع", "صنع", WordType.VERB, 
               Map.of("creativity", 0.8), Arrays.asList("الصناعة"));
        addWord("كتب", "كتب", WordType.VERB, 
               Map.of("communication", 0.8), Arrays.asList("الكتابة"));
        addWord("قرأ", "قرأ", WordType.VERB, 
               Map.of("learning", 0.9), Arrays.asList("القراءة"));
        
        // أفعال الوجود
        addWord("كان", "كون", WordType.VERB, 
               Map.of("existence", 0.8), Arrays.asList("الوجود"));
        addWord("صار", "صير", WordType.VERB, 
               Map.of("change", 0.8), Arrays.asList("التغير"));
        
        // أفعال الملكية
        addWord("ملك", "ملك", WordType.VERB, 
               Map.of("possession", 0.8), Arrays.asList("الملكية"));
        addWord("أخذ", "أخذ", WordType.VERB, 
               Map.of("action", 0.7), Arrays.asList("الأخذ"));
        addWord("أعطى", "عطاء", WordType.VERB, 
               Map.of("generosity", 0.9), Arrays.asList("الإعطاء"));
        
        // أفعال الرؤية
        addWord("رأى", "رؤية", WordType.VERB, 
               Map.of("perception", 0.8), Arrays.asList("الرؤية"));
        addWord("سمع", "سمع", WordType.VERB, 
               Map.of("perception", 0.8), Arrays.asList("السمع"));
        
        // أفعال المساعدة
        addWord("ساعد", "مساعدة", WordType.VERB, 
               Map.of("help", 0.9), Arrays.asList("المساعدة"));
        
        // أفعال البدء
        addWord("بدأ", "بدء", WordType.VERB, 
               Map.of("beginning", 0.8), Arrays.asList("البداية"));
        addWord("أنهى", "إنهاء", WordType.VERB, 
               Map.of("completion", 0.8), Arrays.asList("النهاية"));
        
        // أفعال البحث
        addWord("وجد", "وجد", WordType.VERB, 
               Map.of("discovery", 0.9), Arrays.asList("العثور"));
        addWord("فقد", "فقدان", WordType.VERB, 
               Map.of("loss", 0.8), Arrays.asList("الفقدان"));
        
        // أفعال النوم
        addWord("نام", "نوم", WordType.VERB, 
               Map.of("rest", 0.7), Arrays.asList("النوم"));
        addWord("استيقظ", "يقظة", WordType.VERB, 
               Map.of("awareness", 0.8), Arrays.asList("الاستيقاظ"));
        
        // أفعال الأكل
        addWord("أكل", "أكل", WordType.VERB, 
               Map.of("sustenance", 0.7), Arrays.asList("الأكل"));
        addWord("شرب", "شرب", WordType.VERB, 
               Map.of("sustenance", 0.6), Arrays.asList("الشرب"));
        
        // أفعال اللعب
        addWord("لعب", "لعب", WordType.VERB, 
               Map.of("joy", 0.8), Arrays.asList("اللعب"));
        addWord("ركض", "ركض", WordType.VERB, 
               Map.of("energy", 0.8), Arrays.asList("الركض"));
        
        // أفعال الجلوس
        addWord("جلس", "جلوس", WordType.VERB, 
               Map.of("rest", 0.6), Arrays.asList("الجلوس"));
        addWord("وقف", "وقوف", WordType.VERB, 
               Map.of("readiness", 0.6), Arrays.asList("الوقوف"));
        
        // أفعال الاشتياق
        addWord("اشتاق", "شوق", WordType.VERB, 
               Map.of("longing", 0.9), Arrays.asList("الاشتياق"));
    }
    
    private void initializeNouns() {
        // أسماء الأشخاص
        addWord("إنسان", "إنسان", WordType.NOUN, 
               Map.of("humanity", 0.8), Arrays.asList("الإنسان"));
        addWord("رجل", "رجل", WordType.NOUN, 
               Map.of("strength", 0.6), Arrays.asList("الرجل"));
        addWord("امرأة", "مرء", WordType.NOUN, 
               Map.of("beauty", 0.6), Arrays.asList("المرأة"));
        addWord("طفل", "طفل", WordType.NOUN, 
               Map.of("innocence", 0.9), Arrays.asList("الطفل"));
        addWord("صديق", "صدق", WordType.NOUN, 
               Map.of("trust", 0.9), Arrays.asList("الصديق"));
        addWord("أب", "أب", WordType.NOUN, 
               Map.of("love", 0.9), Arrays.asList("الأب"));
        addWord("أم", "أم", WordType.NOUN, 
               Map.of("love", 1.0), Arrays.asList("الأم"));
        addWord("أخ", "أخ", WordType.NOUN, 
               Map.of("love", 0.8), Arrays.asList("الأخ"));
        addWord("أخت", "أخت", WordType.NOUN, 
               Map.of("love", 0.8), Arrays.asList("الأخت"));
        
        // أسماء المشاعر
        addWord("حب", "حب", WordType.NOUN, 
               Map.of("love", 1.0, "joy", 0.9), Arrays.asList("الحب"));
        addWord("فرح", "فرح", WordType.NOUN, 
               Map.of("joy", 1.0), Arrays.asList("الفرح"));
        addWord("حزن", "حزن", WordType.NOUN, 
               Map.of("sadness", 1.0), Arrays.asList("الحزن"));
        addWord("خوف", "خوف", WordType.NOUN, 
               Map.of("fear", 1.0), Arrays.asList("الخوف"));
        addWord("غضب", "غضب", WordType.NOUN, 
               Map.of("anger", 1.0), Arrays.asList("الغضب"));
        addWord("أمل", "أمل", WordType.NOUN, 
               Map.of("hope", 0.9), Arrays.asList("الأمل"));
        addWord("سلام", "سلم", WordType.NOUN, 
               Map.of("peace", 0.9), Arrays.asList("السلام"));
        addWord("سعادة", "سعد", WordType.NOUN, 
               Map.of("joy", 1.0, "happiness", 1.0), Arrays.asList("السعادة"));
        addWord("شجاعة", "شجع", WordType.NOUN, 
               Map.of("courage", 0.9), Arrays.asList("الشجاعة"));
        
        // أسماء الطبيعة
        addWord("شمس", "شمس", WordType.NOUN, 
               Map.of("warmth", 0.7), Arrays.asList("الشمس"));
        addWord("قمر", "قمر", WordType.NOUN, 
               Map.of("beauty", 0.8), Arrays.asList("القمر"));
        addWord("نجم", "نجم", WordType.NOUN, 
               Map.of("wonder", 0.8), Arrays.asList("النجم"));
        addWord("سماء", "سمو", WordType.NOUN, 
               Map.of("vastness", 0.8), Arrays.asList("السماء"));
        addWord("أرض", "أرض", WordType.NOUN, 
               Map.of("stability", 0.8), Arrays.asList("الأرض"));
        addWord("بحر", "بحر", WordType.NOUN, 
               Map.of("vastness", 0.8), Arrays.asList("البحر"));
        addWord("جبل", "جبل", WordType.NOUN, 
               Map.of("strength", 0.8), Arrays.asList("الجبل"));
        addWord("شجرة", "شجر", WordType.NOUN, 
               Map.of("life", 0.7), Arrays.asList("الشجرة"));
        addWord("ورد", "ورد", WordType.NOUN, 
               Map.of("beauty", 0.9), Arrays.asList("الورد"));
        addWord("ماء", "ماء", WordType.NOUN, 
               Map.of("life", 0.8), Arrays.asList("الماء"));
        
        // أسماء الوقت
        addWord("يوم", "يوم", WordType.NOUN, 
               Map.of("time", 0.7), Arrays.asList("اليوم"));
        addWord("ليل", "ليل", WordType.NOUN, 
               Map.of("rest", 0.7), Arrays.asList("الليل"));
        addWord("صباح", "صبح", WordType.NOUN, 
               Map.of("hope", 0.8), Arrays.asList("الصباح"));
        addWord("مساء", "مسو", WordType.NOUN, 
               Map.of("rest", 0.7), Arrays.asList("المساء"));
        addWord("وقت", "وقت", WordType.NOUN, 
               Map.of("importance", 0.8), Arrays.asList("الوقت"));
        addWord("عمر", "عمر", WordType.NOUN, 
               Map.of("life", 0.8), Arrays.asList("العمر"));
        
        // أسماء المكان
        addWord("بيت", "بيت", WordType.NOUN, 
               Map.of("safety", 0.9), Arrays.asList("البيت"));
        addWord("مدرسة", "درس", WordType.NOUN, 
               Map.of("learning", 0.9), Arrays.asList("المدرسة"));
        addWord("مدينة", "مدن", WordType.NOUN, 
               Map.of("civilization", 0.7), Arrays.asList("المدينة"));
        addWord("طريق", "طريق", WordType.NOUN, 
               Map.of("journey", 0.7), Arrays.asList("الطريق"));
        addWord("عالم", "علم", WordType.NOUN, 
               Map.of("knowledge", 0.9), Arrays.asList("العالم"));
        
        // أسماء العلم
        addWord("علم", "علم", WordType.NOUN, 
               Map.of("wisdom", 0.9), Arrays.asList("العلم"));
        addWord("كتاب", "كتب", WordType.NOUN, 
               Map.of("knowledge", 0.8), Arrays.asList("الكتاب"));
        addWord("قلم", "قلم", WordType.NOUN, 
               Map.of("creativity", 0.7), Arrays.asList("القلم"));
        addWord("فكر", "فكر", WordType.NOUN, 
               Map.of("intelligence", 0.9), Arrays.asList("الفكر"));
        addWord("قلب", "قلب", WordType.NOUN, 
               Map.of("love", 0.9), Arrays.asList("القلب"));
        addWord("عقل", "عقل", WordType.NOUN, 
               Map.of("intelligence", 0.9), Arrays.asList("العقل"));
        addWord("روح", "روح", WordType.NOUN, 
               Map.of("spirituality", 0.9), Arrays.asList("الروح"));
        
        // أسماء الجسم
        addWord("رأس", "رأس", WordType.NOUN, 
               Map.of("importance", 0.7), Arrays.asList("الرأس"));
        addWord("عين", "عين", WordType.NOUN, 
               Map.of("vision", 0.8), Arrays.asList("العين"));
        addWord("يد", "يد", WordType.NOUN, 
               Map.of("action", 0.7), Arrays.asList("اليد"));
        
        // أسماء الطعام
        addWord("طعام", "طعم", WordType.NOUN, 
               Map.of("sustenance", 0.8), Arrays.asList("الطعام"));
        addWord("خبز", "خبز", WordType.NOUN, 
               Map.of("sustenance", 0.7), Arrays.asList("الخبز"));
        
        // أسماء الألوان
        addWord("أحمر", "حمر", WordType.NOUN, 
               Map.of("passion", 0.8), Arrays.asList("الأحمر"));
        addWord("أزرق", "زرق", WordType.NOUN, 
               Map.of("calm", 0.8), Arrays.asList("الأزرق"));
        addWord("أخضر", "خضر", WordType.NOUN, 
               Map.of("life", 0.8), Arrays.asList("الأخضر"));
        addWord("أبيض", "بيض", WordType.NOUN, 
               Map.of("purity", 0.9), Arrays.asList("الأبيض"));
        addWord("أسود", "سود", WordType.NOUN, 
               Map.of("mystery", 0.7), Arrays.asList("الأسود"));
        
        // أسماء المهن
        addWord("طبيب", "طبب", WordType.NOUN, 
               Map.of("help", 0.9), Arrays.asList("الطبيب"));
        addWord("معلم", "علم", WordType.NOUN, 
               Map.of("guidance", 0.9), Arrays.asList("المعلم"));
        
        // أسماء الأدوات
        addWord("سيارة", "سير", WordType.NOUN, 
               Map.of("speed", 0.7), Arrays.asList("السيارة"));
        addWord("هاتف", "هاتف", WordType.NOUN, 
               Map.of("connection", 0.8), Arrays.asList("الهاتف"));
        addWord("باب", "باب", WordType.NOUN, 
               Map.of("opportunity", 0.7), Arrays.asList("الباب"));
        addWord("نافذة", "نفذ", WordType.NOUN, 
               Map.of("vision", 0.7), Arrays.asList("النافذة"));
        
        // أسماء الحياة والموت
        addWord("حياة", "حياة", WordType.NOUN, 
               Map.of("joy", 0.9), Arrays.asList("الحياة"));
        addWord("موت", "موت", WordType.NOUN, 
               Map.of("fear", 0.8), Arrays.asList("الموت"));
        addWord("نور", "نور", WordType.NOUN, 
               Map.of("hope", 0.9), Arrays.asList("النور"));
        addWord("ظلام", "ظلم", WordType.NOUN, 
               Map.of("fear", 0.7), Arrays.asList("الظلام"));
        
        // أسماء الصفات
        addWord("جمال", "جمل", WordType.NOUN, 
               Map.of("beauty", 1.0), Arrays.asList("الجمال"));
        addWord("قوة", "قوة", WordType.NOUN, 
               Map.of("strength", 0.9), Arrays.asList("القوة"));
        addWord("صدق", "صدق", WordType.NOUN, 
               Map.of("trust", 0.9), Arrays.asList("الصدق"));
        addWord("كذب", "كذب", WordType.NOUN, 
               Map.of("distrust", 0.3), Arrays.asList("الكذب"));
        addWord("عدل", "عدل", WordType.NOUN, 
               Map.of("justice", 0.9), Arrays.asList("العدل"));
        addWord("ظلم", "ظلم", WordType.NOUN, 
               Map.of("injustice", 0.2), Arrays.asList("الظلم"));
        addWord("كرم", "كرم", WordType.NOUN, 
               Map.of("generosity", 0.9), Arrays.asList("الكرم"));
        addWord("صبر", "صبر", WordType.NOUN, 
               Map.of("patience", 0.9), Arrays.asList("الصبر"));
        addWord("حلم", "حلم", WordType.NOUN, 
               Map.of("dream", 0.8), Arrays.asList("الحلم"));
        addWord("هدف", "هدف", WordType.NOUN, 
               Map.of("purpose", 0.8), Arrays.asList("الهدف"));
        addWord("نجاح", "نجح", WordType.NOUN, 
               Map.of("achievement", 0.9), Arrays.asList("النجاح"));
        addWord("فشل", "فشل", WordType.NOUN, 
               Map.of("failure", 0.3), Arrays.asList("الفشل"));
    }
    
    private void initializeParticles() {
        // حروف الجر
        addWord("في", "في", WordType.PREPOSITION, 
               Map.of("location", 0.8), Arrays.asList("في", "داخل"));
        addWord("من", "من", WordType.PREPOSITION, 
               Map.of("origin", 0.8), Arrays.asList("من"));
        addWord("إلى", "إلى", WordType.PREPOSITION, 
               Map.of("direction", 0.8), Arrays.asList("إلى"));
        addWord("على", "على", WordType.PREPOSITION, 
               Map.of("elevation", 0.7), Arrays.asList("على"));
        addWord("عن", "عن", WordType.PREPOSITION, 
               Map.of("distance", 0.6), Arrays.asList("عن"));
        addWord("بـ", "باء", WordType.PREPOSITION, 
               Map.of("instrument", 0.7), Arrays.asList("بـ"));
        addWord("كـ", "كاف", WordType.PREPOSITION, 
               Map.of("similarity", 0.8), Arrays.asList("كـ"));
        addWord("لـ", "لام", WordType.PREPOSITION, 
               Map.of("purpose", 0.8), Arrays.asList("لـ"));
        addWord("حتى", "حتى", WordType.PREPOSITION, 
               Map.of("end", 0.7), Arrays.asList("حتى"));
        addWord("مع", "مع", WordType.PREPOSITION, 
               Map.of("companionship", 0.8), Arrays.asList("مع"));
        addWord("فوق", "فوق", WordType.PREPOSITION, 
               Map.of("elevation", 0.8), Arrays.asList("فوق"));
        addWord("تحت", "تحت", WordType.PREPOSITION, 
               Map.of("below", 0.8), Arrays.asList("تحت"));
        addWord("بين", "بين", WordType.PREPOSITION, 
               Map.of("middle", 0.7), Arrays.asList("بين"));
        addWord("أمام", "أمام", WordType.PREPOSITION, 
               Map.of("front", 0.8), Arrays.asList("أمام"));
        addWord("خلف", "خلف", WordType.PREPOSITION, 
               Map.of("behind", 0.8), Arrays.asList("خلف"));
        
        // حروف العطف
        addWord("و", "واو", WordType.CONJUNCTION, 
               Map.of("connection", 0.9), Arrays.asList("و"));
        addWord("ثم", "ثم", WordType.CONJUNCTION, 
               Map.of("sequence", 0.8), Arrays.asList("ثم"));
        addWord("أو", "أو", WordType.CONJUNCTION, 
               Map.of("choice", 0.8), Arrays.asList("أو"));
        addWord("لكن", "لكن", WordType.CONJUNCTION, 
               Map.of("contrast", 0.8), Arrays.asList("لكن"));
        addWord("بل", "بل", WordType.CONJUNCTION, 
               Map.of("correction", 0.7), Arrays.asList("بل"));
        addWord("لأن", "لأن", WordType.CONJUNCTION, 
               Map.of("reason", 0.8), Arrays.asList("لأن"));
        addWord("لذلك", "لذلك", WordType.CONJUNCTION, 
               Map.of("result", 0.8), Arrays.asList("لذلك"));
        addWord("كي", "كي", WordType.CONJUNCTION, 
               Map.of("purpose", 0.8), Arrays.asList("كي"));
        
        // أدوات الاستفهام
        addWord("هل", "هل", WordType.INTERROGATIVE, 
               Map.of("question", 0.9), Arrays.asList("هل"));
        addWord("من", "من", WordType.INTERROGATIVE, 
               Map.of("question", 0.9), Arrays.asList("من"));
        addWord("ما", "ما", WordType.INTERROGATIVE, 
               Map.of("question", 0.9), Arrays.asList("ما"));
        addWord("متى", "متى", WordType.INTERROGATIVE, 
               Map.of("question", 0.9), Arrays.asList("متى"));
        addWord("أين", "أين", WordType.INTERROGATIVE, 
               Map.of("question", 0.9), Arrays.asList("أين"));
        addWord("كيف", "كيف", WordType.INTERROGATIVE, 
               Map.of("question", 0.9), Arrays.asList("كيف"));
        addWord("لماذا", "لماذا", WordType.INTERROGATIVE, 
               Map.of("question", 0.9), Arrays.asList("لماذا"));
        addWord("كم", "كم", WordType.INTERROGATIVE, 
               Map.of("question", 0.8), Arrays.asList("كم"));
        
        // أدوات النفي
        addWord("لا", "لا", WordType.NEGATION, 
               Map.of("negation", 0.9), Arrays.asList("لا"));
        addWord("لم", "لم", WordType.NEGATION, 
               Map.of("negation", 0.9), Arrays.asList("لم"));
        addWord("لن", "لن", WordType.NEGATION, 
               Map.of("negation", 0.9), Arrays.asList("لن"));
        addWord("ما", "ما", WordType.NEGATION, 
               Map.of("negation", 0.8), Arrays.asList("ما"));
        addWord("ليس", "ليس", WordType.NEGATION, 
               Map.of("negation", 0.8), Arrays.asList("ليس"));
        addWord("غير", "غير", WordType.NEGATION, 
               Map.of("negation", 0.7), Arrays.asList("غير"));
        
        // أدوات التوكيد
        addWord("إن", "إن", WordType.EMPHASIS, 
               Map.of("emphasis", 0.9), Arrays.asList("إن"));
        addWord("أن", "أن", WordType.EMPHASIS, 
               Map.of("emphasis", 0.8), Arrays.asList("أن"));
        addWord("قد", "قد", WordType.EMPHASIS, 
               Map.of("emphasis", 0.8), Arrays.asList("قد"));
        addWord("لقد", "لقد", WordType.EMPHASIS, 
               Map.of("emphasis", 0.9), Arrays.asList("لقد"));
        
        // حروف أخرى
        addWord("إذا", "إذا", WordType.PARTICLE, 
               Map.of("condition", 0.8), Arrays.asList("إذا"));
        addWord("لو", "لو", WordType.PARTICLE, 
               Map.of("condition", 0.8), Arrays.asList("لو"));
        addWord("سوف", "سوف", WordType.PARTICLE, 
               Map.of("future", 0.9), Arrays.asList("سوف"));
        addWord("يا", "يا", WordType.PARTICLE, 
               Map.of("vocative", 0.9), Arrays.asList("يا"));
        addWord("ال", "ال", WordType.PARTICLE, 
               Map.of("definition", 0.9), Arrays.asList("ال"));
    }
    
    private void initializeAdjectives() {
        // صفات الحجم
        addWord("كبير", "كبر", WordType.ADJECTIVE, 
               Map.of("importance", 0.8), Arrays.asList("كبير"));
        addWord("صغير", "صغر", WordType.ADJECTIVE, 
               Map.of("delicacy", 0.5), Arrays.asList("صغير"));
        addWord("طويل", "طول", WordType.ADJECTIVE, 
               Map.of("stability", 0.5), Arrays.asList("طويل"));
        addWord("قصير", "قصر", WordType.ADJECTIVE, 
               Map.of("compact", 0.5), Arrays.asList("قصير"));
        addWord("واسع", "وسع", WordType.ADJECTIVE, 
               Map.of("freedom", 0.7), Arrays.asList("واسع"));
        
        // صفات الجودة
        addWord("جيد", "جود", WordType.ADJECTIVE, 
               Map.of("satisfaction", 0.8), Arrays.asList("جيد"));
        addWord("سيئ", "سوء", WordType.ADJECTIVE, 
               Map.of("displeasure", 0.7), Arrays.asList("سيئ"));
        addWord("جميل", "جمل", WordType.ADJECTIVE, 
               Map.of("joy", 0.9), Arrays.asList("جميل"));
        addWord("قبيح", "قبح", WordType.ADJECTIVE, 
               Map.of("displeasure", 0.6), Arrays.asList("قبيح"));
        addWord("جديد", "جدد", WordType.ADJECTIVE, 
               Map.of("excitement", 0.8), Arrays.asList("جديد"));
        addWord("قديم", "قدم", WordType.ADJECTIVE, 
               Map.of("nostalgia", 0.6), Arrays.asList("قديم"));
        
        // صفات القوة
        addWord("قوي", "قوة", WordType.ADJECTIVE, 
               Map.of("power", 0.9), Arrays.asList("قوي"));
        addWord("ضعيف", "ضعف", WordType.ADJECTIVE, 
               Map.of("weakness", 0.5), Arrays.asList("ضعيف"));
        addWord("سريع", "سرع", WordType.ADJECTIVE, 
               Map.of("energy", 0.8), Arrays.asList("سريع"));
        addWord("بطيء", "بطء", WordType.ADJECTIVE, 
               Map.of("patience", 0.5), Arrays.asList("بطيء"));
        
        // صفات الذكاء
        addWord("ذكي", "ذكاء", WordType.ADJECTIVE, 
               Map.of("admiration", 0.9), Arrays.asList("ذكي"));
        addWord("غبي", "غباء", WordType.ADJECTIVE, 
               Map.of("disappointment", 0.4), Arrays.asList("غبي"));
        
        // صفات الأخلاق
        addWord("طيب", "طيب", WordType.ADJECTIVE, 
               Map.of("love", 0.8), Arrays.asList("طيب"));
        addWord("شرير", "شر", WordType.ADJECTIVE, 
               Map.of("anger", 0.7), Arrays.asList("شرير"));
        addWord("كريم", "كرم", WordType.ADJECTIVE, 
               Map.of("admiration", 0.9), Arrays.asList("كريم"));
        addWord("بخيل", "بخل", WordType.ADJECTIVE, 
               Map.of("dislike", 0.4), Arrays.asList("بخيل"));
        addWord("صادق", "صدق", WordType.ADJECTIVE, 
               Map.of("trust", 0.9), Arrays.asList("صادق"));
        addWord("كاذب", "كذب", WordType.ADJECTIVE, 
               Map.of("distrust", 0.3), Arrays.asList("كاذب"));
        addWord("شجاع", "شجع", WordType.ADJECTIVE, 
               Map.of("admiration", 0.9), Arrays.asList("شجاع"));
        addWord("جبان", "جبن", WordType.ADJECTIVE, 
               Map.of("disappointment", 0.4), Arrays.asList("جبان"));
        
        // صفات الحالة
        addWord("سعيد", "سعد", WordType.ADJECTIVE, 
               Map.of("joy", 1.0), Arrays.asList("سعيد"));
        addWord("حزين", "حزن", WordType.ADJECTIVE, 
               Map.of("sadness", 0.9), Arrays.asList("حزين"));
        addWord("غاضب", "غضب", WordType.ADJECTIVE, 
               Map.of("anger", 0.9), Arrays.asList("غاضب"));
        addWord("خائف", "خوف", WordType.ADJECTIVE, 
               Map.of("fear", 0.9), Arrays.asList("خائف"));
        addWord("راضٍ", "رضي", WordType.ADJECTIVE, 
               Map.of("contentment", 0.8), Arrays.asList("راضٍ"));
        addWord("متعب", "تعب", WordType.ADJECTIVE, 
               Map.of("fatigue", 0.6), Arrays.asList("متعب"));
        addWord("نشيط", "نشط", WordType.ADJECTIVE, 
               Map.of("energy", 0.8), Arrays.asList("نشيط"));
        addWord("مريض", "مرض", WordType.ADJECTIVE, 
               Map.of("concern", 0.5), Arrays.asList("مريض"));
        addWord("صحيح", "صح", WordType.ADJECTIVE, 
               Map.of("wellness", 0.8), Arrays.asList("صحيح"));
        
        // صفات اللون
        addWord("أحمر", "حمر", WordType.ADJECTIVE, 
               Map.of("passion", 0.7), Arrays.asList("أحمر"));
        addWord("أزرق", "زرق", WordType.ADJECTIVE, 
               Map.of("calm", 0.7), Arrays.asList("أزرق"));
        addWord("أخضر", "خضر", WordType.ADJECTIVE, 
               Map.of("life", 0.7), Arrays.asList("أخضر"));
        addWord("أبيض", "بيض", WordType.ADJECTIVE, 
               Map.of("purity", 0.8), Arrays.asList("أبيض"));
        addWord("أسود", "سود", WordType.ADJECTIVE, 
               Map.of("mystery", 0.6), Arrays.asList("أسود"));
        
        // صفات الدرجة
        addWord("كثير", "كثر", WordType.ADJECTIVE, 
               Map.of("abundance", 0.7), Arrays.asList("كثير"));
        addWord("قليل", "قلل", WordType.ADJECTIVE, 
               Map.of("scarcity", 0.5), Arrays.asList("قليل"));
        addWord("كامل", "كمل", WordType.ADJECTIVE, 
               Map.of("satisfaction", 0.8), Arrays.asList("كامل"));
        addWord("ناقص", "نقص", WordType.ADJECTIVE, 
               Map.of("incompleteness", 0.5), Arrays.asList("ناقص"));
        
        // صفات الموقف
        addWord("ممكن", "ممكن", WordType.ADJECTIVE, 
               Map.of("hope", 0.7), Arrays.asList("ممكن"));
        addWord("مستحيل", "مستحيل", WordType.ADJECTIVE, 
               Map.of("despair", 0.4), Arrays.asList("مستحيل"));
        addWord("سهل", "سهل", WordType.ADJECTIVE, 
               Map.of("ease", 0.8), Arrays.asList("سهل"));
        addWord("صعب", "صعب", WordType.ADJECTIVE, 
               Map.of("difficulty", 0.5), Arrays.asList("صعب"));
        addWord("مهم", "مهم", WordType.ADJECTIVE, 
               Map.of("importance", 0.9), Arrays.asList("مهم"));
        
        // صفات الحرارة
        addWord("حار", "حر", WordType.ADJECTIVE, 
               Map.of("intensity", 0.6), Arrays.asList("حار"));
        addWord("بارد", "برد", WordType.ADJECTIVE, 
               Map.of("calm", 0.6), Arrays.asList("بارد"));
        addWord("دافئ", "دفء", WordType.ADJECTIVE, 
               Map.of("comfort", 0.8), Arrays.asList("دافئ"));
        
        // صفات النظافة
        addWord("نظيف", "نظف", WordType.ADJECTIVE, 
               Map.of("purity", 0.8), Arrays.asList("نظيف"));
        addWord("وسخ", "وسخ", WordType.ADJECTIVE, 
               Map.of("disgust", 0.4), Arrays.asList("وسخ"));
        
        // صفات الوضوح
        addWord("واضح", "وضح", WordType.ADJECTIVE, 
               Map.of("clarity", 0.9), Arrays.asList("واضح"));
        addWord("غامض", "غمض", WordType.ADJECTIVE, 
               Map.of("confusion", 0.5), Arrays.asList("غامض"));
        
        // صفات الصوت
        addWord("هادئ", "هدوء", WordType.ADJECTIVE, 
               Map.of("peace", 0.9), Arrays.asList("هادئ"));
        addWord("صاخب", "صخب", WordType.ADJECTIVE, 
               Map.of("annoyance", 0.5), Arrays.asList("صاخب"));
        
        // صفات الضوء
        addWord("مضيء", "ضياء", WordType.ADJECTIVE, 
               Map.of("hope", 0.8), Arrays.asList("مضيء"));
        addWord("مظلم", "ظلام", WordType.ADJECTIVE, 
               Map.of("fear", 0.6), Arrays.asList("مظلم"));
        
        // صفات القرب والبعد
        addWord("قريب", "قرب", WordType.ADJECTIVE, 
               Map.of("closeness", 0.8), Arrays.asList("قريب"));
        addWord("بعيد", "بعد", WordType.ADJECTIVE, 
               Map.of("distance", 0.5), Arrays.asList("بعيد"));
        
        // صفات الكفاية
        addWord("كافٍ", "كفاية", WordType.ADJECTIVE, 
               Map.of("sufficiency", 0.8), Arrays.asList("كافٍ"));
        addWord("غير كافٍ", "كفاية", WordType.ADJECTIVE, 
               Map.of("insufficiency", 0.4), Arrays.asList("غير كافٍ"));
        
        // صفات التميز
        addWord("مميز", "ميز", WordType.ADJECTIVE, 
               Map.of("distinction", 0.9), Arrays.asList("مميز"));
        addWord("عادي", "عادي", WordType.ADJECTIVE, 
               Map.of("normalcy", 0.5), Arrays.asList("عادي"));
        
        // صفات الاحترام
        addWord("محترم", "حترم", WordType.ADJECTIVE, 
               Map.of("respect", 0.9), Arrays.asList("محترم"));
        addWord("وقح", "وقح", WordType.ADJECTIVE, 
               Map.of("disrespect", 0.3), Arrays.asList("وقح"));
        
        // صفات التنظيم
        addWord("منظم", "نظم", WordType.ADJECTIVE, 
               Map.of("order", 0.8), Arrays.asList("منظم"));
        addWord("فوضوي", "فوضى", WordType.ADJECTIVE, 
               Map.of("chaos", 0.4), Arrays.asList("فوضوي"));
        
        // صفات الدقة
        addWord("دقيق", "دقة", WordType.ADJECTIVE, 
               Map.of("precision", 0.9), Arrays.asList("دقيق"));
        addWord("خاطئ", "خطأ", WordType.ADJECTIVE, 
               Map.of("error", 0.4), Arrays.asList("خاطئ"));
        
        // صفات الشهرة
        addWord("مشهور", "شهر", WordType.ADJECTIVE, 
               Map.of("fame", 0.8), Arrays.asList("مشهور"));
        addWord("مجهول", "جهل", WordType.ADJECTIVE, 
               Map.of("unknown", 0.5), Arrays.asList("مجهول"));
        
        // صفات القيمة
        addWord("ثمين", "ثمن", WordType.ADJECTIVE, 
               Map.of("value", 0.9), Arrays.asList("ثمين"));
        addWord("رخيص", "رخص", WordType.ADJECTIVE, 
               Map.of("affordability", 0.5), Arrays.asList("رخيص"));
        
        // صفات الخطورة
        addWord("خطير", "خطر", WordType.ADJECTIVE, 
               Map.of("danger", 0.7), Arrays.asList("خطير"));
        addWord("آمن", "أمن", WordType.ADJECTIVE, 
               Map.of("safety", 0.9), Arrays.asList("آمن"));
        
        // صفات الاستعداد
        addWord("جاهز", "جاهز", WordType.ADJECTIVE, 
               Map.of("readiness", 0.8), Arrays.asList("جاهز"));
        addWord("غير جاهز", "جاهز", WordType.ADJECTIVE, 
               Map.of("unpreparedness", 0.4), Arrays.asList("غير جاهز"));
        
        // صفات الراحة
        addWord("مريح", "راحة", WordType.ADJECTIVE, 
               Map.of("comfort", 0.9), Arrays.asList("مريح"));
        addWord("غير مريح", "راحة", WordType.ADJECTIVE, 
               Map.of("discomfort", 0.4), Arrays.asList("غير مريح"));
        
        // صفات الاهتمام
        addWord("مهتم", "اهتمام", WordType.ADJECTIVE, 
               Map.of("interest", 0.8), Arrays.asList("مهتم"));
        addWord("غير مبالٍ", "مبالاة", WordType.ADJECTIVE, 
               Map.of("indifference", 0.3), Arrays.asList("غير مبالٍ"));
        
        // صفات الحماس
        addWord("متحمس", "حماس", WordType.ADJECTIVE, 
               Map.of("excitement", 0.9), Arrays.asList("متحمس"));
        addWord("متردد", "تردد", WordType.ADJECTIVE, 
               Map.of("hesitation", 0.4), Arrays.asList("متردد"));
        
        // صفات الثقة
        addWord("واثق", "ثقة", WordType.ADJECTIVE, 
               Map.of("confidence", 0.9), Arrays.asList("واثق"));
        addWord("شاك", "شك", WordType.ADJECTIVE, 
               Map.of("doubt", 0.4), Arrays.asList("شاك"));
        
        // صفات الرضا
        addWord("راضٍ", "رضا", WordType.ADJECTIVE, 
               Map.of("satisfaction", 0.9), Arrays.asList("راضٍ"));
        addWord("غاضب", "غضب", WordType.ADJECTIVE, 
               Map.of("anger", 0.8), Arrays.asList("غاضب"));
        
        // صفات النضج
        addWord("ناضج", "نضج", WordType.ADJECTIVE, 
               Map.of("maturity", 0.8), Arrays.asList("ناضج"));
        addWord("غير ناضج", "نضج", WordType.ADJECTIVE, 
               Map.of("immaturity", 0.4), Arrays.asList("غير ناضج"));
        
        // صفات الاكتمال
        addWord("مكتمل", "اكتمال", WordType.ADJECTIVE, 
               Map.of("completion", 0.9), Arrays.asList("مكتمل"));
        addWord("غير مكتمل", "اكتمال", WordType.ADJECTIVE, 
               Map.of("incompleteness", 0.4), Arrays.asList("غير مكتمل"));
        
        // صفات الفعالية
        addWord("فعال", "فاعلية", WordType.ADJECTIVE, 
               Map.of("effectiveness", 0.9), Arrays.asList("فعال"));
        addWord("غير فعال", "فاعلية", WordType.ADJECTIVE, 
               Map.of("ineffectiveness", 0.4), Arrays.asList("غير فعال"));
        
        // صفات المرونة
        addWord("مرن", "مرونة", WordType.ADJECTIVE, 
               Map.of("flexibility", 0.8), Arrays.asList("مرن"));
        addWord("صلب", "صلابة", WordType.ADJECTIVE, 
               Map.of("rigidity", 0.5), Arrays.asList("صلب"));
        
        // صفات الحداثة
        addWord("حديث", "حداثة", WordType.ADJECTIVE, 
               Map.of("modernity", 0.8), Arrays.asList("حديث"));
        addWord("قديم", "قدم", WordType.ADJECTIVE, 
               Map.of("antiquity", 0.5), Arrays.asList("قديم"));
        
        // صفات الأصالة
        addWord("أصيل", "أصالة", WordType.ADJECTIVE, 
               Map.of("authenticity", 0.9), Arrays.asList("أصيل"));
        addWord("مزيف", "تزييف", WordType.ADJECTIVE, 
               Map.of("fakeness", 0.3), Arrays.asList("مزيف"));
        
        // صفات الاستقلالية
        addWord("مستقل", "استقلال", WordType.ADJECTIVE, 
               Map.of("independence", 0.8), Arrays.asList("مستقل"));
        addWord("تابع", "تبعية", WordType.ADJECTIVE, 
               Map.of("dependence", 0.5), Arrays.asList("تابع"));
        
        // صفات العدل
        addWord("عادل", "عدل", WordType.ADJECTIVE, 
               Map.of("justice", 0.9), Arrays.asList("عادل"));
        addWord("ظالم", "ظلم", WordType.ADJECTIVE, 
               Map.of("injustice", 0.2), Arrays.asList("ظالم"));
        
        // صفات الصبر
        addWord("صبور", "صبر", WordType.ADJECTIVE, 
               Map.of("patience", 0.9), Arrays.asList("صبور"));
        addWord("عجول", "عجلة", WordType.ADJECTIVE, 
               Map.of("impatience", 0.4), Arrays.asList("عجول"));
        
        // صفات التواضع
        addWord("متواضع", "تواضع", WordType.ADJECTIVE, 
               Map.of("humility", 0.9), Arrays.asList("متواضع"));
        addWord("متكبر", "تكبر", WordType.ADJECTIVE, 
               Map.of("arrogance", 0.3), Arrays.asList("متكبر"));
        
        // صفات الأمانة
        addWord("أمين", "أمانة", WordType.ADJECTIVE, 
               Map.of("trustworthiness", 0.9), Arrays.asList("أمين"));
        addWord("خائن", "خيانة", WordType.ADJECTIVE, 
               Map.of("betrayal", 0.2), Arrays.asList("خائن"));
        
        // صفات الوفاء
        addWord("وفي", "وفاء", WordType.ADJECTIVE, 
               Map.of("loyalty", 0.9), Arrays.asList("وفي"));
        addWord("غادر", "غدر", WordType.ADJECTIVE, 
               Map.of("treachery", 0.2), Arrays.asList("غادر"));
        
        // صفات الرحمة
        addWord("رحيم", "رحمة", WordType.ADJECTIVE, 
               Map.of("mercy", 0.9), Arrays.asList("رحيم"));
        addWord("قاسٍ", "قسوة", WordType.ADJECTIVE, 
               Map.of("harshness", 0.3), Arrays.asList("قاسٍ"));
        
        // صفات اللطف
        addWord("لطيف", "لطف", WordType.ADJECTIVE, 
               Map.of("kindness", 0.9), Arrays.asList("لطيف"));
        addWord("فظ", "فظاظة", WordType.ADJECTIVE, 
               Map.of("rudeness", 0.3), Arrays.asList("فظ"));
        
        // صفات الحكمة
        addWord("حكيم", "حكمة", WordType.ADJECTIVE, 
               Map.of("wisdom", 0.9), Arrays.asList("حكيم"));
        addWord("أحمق", "حمق", WordType.ADJECTIVE, 
               Map.of("foolishness", 0.3), Arrays.asList("أحمق"));
        
        // صفات الحزم
        addWord("حازم", "حزم", WordType.ADJECTIVE, 
               Map.of("firmness", 0.8), Arrays.asList("حازم"));
        addWord("متردد", "تردد", WordType.ADJECTIVE, 
               Map.of("hesitation", 0.4), Arrays.asList("متردد"));
        
        // صفات الحلم
        addWord("حليم", "حلم", WordType.ADJECTIVE, 
               Map.of("forbearance", 0.9), Arrays.asList("حليم"));
        addWord("سريع الغضب", "غضب", WordType.ADJECTIVE, 
               Map.of("irritability", 0.3), Arrays.asList("سريع الغضب"));
        
        // صفات الأدب
        addWord("مؤدب", "أدب", WordType.ADJECTIVE, 
               Map.of("politeness", 0.9), Arrays.asList("مؤدب"));
        addWord("فظ", "فظاظة", WordType.ADJECTIVE, 
               Map.of("rudeness", 0.3), Arrays.asList("فظ"));
        
        // صفات الاجتهاد
        addWord("مجتهد", "اجتهاد", WordType.ADJECTIVE, 
               Map.of("diligence", 0.9), Arrays.asList("مجتهد"));
        addWord("كسول", "كسل", WordType.ADJECTIVE, 
               Map.of("laziness", 0.3), Arrays.asList("كسول"));
        
        // صفات النجاح
        addWord("ناجح", "نجاح", WordType.ADJECTIVE, 
               Map.of("success", 0.9), Arrays.asList("ناجح"));
        addWord("فاشل", "فشل", WordType.ADJECTIVE, 
               Map.of("failure", 0.3), Arrays.asList("فاشل"));
        
        // صفات التعاسة
        addWord("تعيس", "تعاسة", WordType.ADJECTIVE, 
               Map.of("misery", 0.3), Arrays.asList("تعيس"));
        
        // صفات الغنى
        addWord("غني", "غنى", WordType.ADJECTIVE, 
               Map.of("wealth", 0.8), Arrays.asList("غني"));
        addWord("فقير", "فقر", WordType.ADJECTIVE, 
               Map.of("poverty", 0.3), Arrays.asList("فقير"));
        
        // صفات الاستقرار
        addWord("مستقر", "استقرار", WordType.ADJECTIVE, 
               Map.of("stability", 0.9), Arrays.asList("مستقر"));
        addWord("متقلب", "تقلب", WordType.ADJECTIVE, 
               Map.of("instability", 0.4), Arrays.asList("متقلب"));
        
        // صفات النقاء
        addWord("نقي", "نقاء", WordType.ADJECTIVE, 
               Map.of("purity", 0.9), Arrays.asList("نقي"));
        addWord("ملوث", "تلوث", WordType.ADJECTIVE, 
               Map.of("pollution", 0.3), Arrays.asList("ملوث"));
        
        // صفات الحب
        addWord("محبوب", "حب", WordType.ADJECTIVE, 
               Map.of("love", 0.9), Arrays.asList("محبوب"));
        addWord("مكروه", "كراهية", WordType.ADJECTIVE, 
               Map.of("hate", 0.3), Arrays.asList("مكروه"));
        
        // صفات الاهتمام
        addWord("مهمل", "إهمال", WordType.ADJECTIVE, 
               Map.of("neglect", 0.3), Arrays.asList("مهمل"));
        
        // صفات الجدية
        addWord("جاد", "جدية", WordType.ADJECTIVE, 
               Map.of("seriousness", 0.8), Arrays.asList("جاد"));
        addWord("هازل", "هزل", WordType.ADJECTIVE, 
               Map.of("playfulness", 0.5), Arrays.asList("هازل"));
    }
    
    // ===== Getters =====
    public Word getWordByRoot(String root) {
        return wordsByRoot.get(root);
    }
    
    public Word getWordByForm(String form) {
        return wordsByForm.get(form);
    }
    
    public List<Word> getWordsByType(WordType type) {
        return wordsByType.get(type);
    }
    
    public boolean hasWord(String word) {
        return wordsByForm.containsKey(word) || wordsByRoot.containsKey(word);
    }
    
    public Collection<Word> getAllWords() {
        return wordsByForm.values();
    }
    
    public int getWordCount() {
        return wordsByForm.size();
    }
    
    public List<Word> findWordsByEmotion(String emotion, double threshold) {
        List<Word> result = new ArrayList<>();
        for (Word word : wordsByForm.values()) {
            Double intensity = word.emotions.get(emotion);
            if (intensity != null && intensity >= threshold) {
                result.add(word);
            }
        }
        return result;
    }
   // إضافة في ArabicLexicon.java
public void addWordFromUser(String word, String root, String meaning) {
    // السماح للمستخدم بإضافة كلمات جديدة
}

// تحسين ArabicParser
public List<String> suggestGrammarFixes(String sentence) {
    // اقتراح تصحيحات نحوية
}
 
    public List<Word> findWordsByMeaning(String meaning) {
        List<Word> result = new ArrayList<>();
        for (Word word : wordsByForm.values()) {
            for (String m : word.meanings) {
                if (m.contains(meaning) || meaning.contains(m)) {
                    result.add(word);
                    break;
                }
            }
        }
        return result;
    }
}

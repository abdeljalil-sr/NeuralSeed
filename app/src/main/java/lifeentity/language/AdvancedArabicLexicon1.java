package com.lifeentity.language;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * معجم عربي متقدم للتحليل اللغوي العميق
 * Advanced Arabic Lexicon for Deep Linguistic Analysis
 */
public class AdvancedArabicLexicon {
    
    // ==================== التصنيفات الرئيسية ====================
    
    /**
     * تصنيفات الكلمات المفصلة
     */
    public enum WordCategory {
        // الضمائر
        PRONOUN_PERSONAL("ضمير متكلم"),
        PRONOUN_OBJECT("ضمير مفعول"),
        PRONOUN_POSSESSIVE("ضمير ملك"),
        
        // أسماء الإشارة
        DEMONSTRATIVE_NEAR("إشارة قريب"),
        DEMONSTRATIVE_FAR("إشارة بعيد"),
        
        // الاستفهام
        QUESTION_TOOL("أداة استفهام"),
        
        // حروف الجر
        PREPOSITION("حرف جر"),
        
        // الربط
        CONJUNCTION_COORDINATING("عاطف"),
        CONJUNCTION_SUBORDINATING("تفسيري"),
        
        // الأفعال
        VERB_PAST("فعل ماضٍ"),
        VERB_PRESENT("فعل مضارع"),
        VERB_IMPERATIVE("فعل أمر"),
        VERB_AUXILIARY("فعل ناقص"),
        
        // الأسماء
        NOUN_HUMAN("اسم إنسان"),
        NOUN_PLACE("اسم مكان"),
        NOUN_TIME("اسم زمان"),
        NOUN_OBJECT("اسم جماد"),
        NOUN_ABSTRACT("اسم معنوي"),
        NOUN_COLLECTIVE("جمع سالم"),
        
        // الصفات
        ADJECTIVE_COLOR("صفة لون"),
        ADJECTIVE_SIZE("صفة حجم"),
        ADJECTIVE_QUALITY("صفة معنوية"),
        ADJECTIVE_NUMBER("صفة عدد"),
        
        // الأعداد
        NUMBER_CARDINAL("عدد أصلي"),
        NUMBER_ORDINAL("عدد ترتيبي"),
        
        // حروف أخرى
        PARTICLE_NEGATION("نافية"),
        PARTICLE_FUTURE("استقبال"),
        PARTICLE_EMPHASIS("توكيد"),
        
        UNKNOWN("غير معروف");
        
        private final String arabicName;
        
        WordCategory(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() {
            return arabicName;
        }
    }
    
    // ==================== قواعد البيانات الموسعة ====================
    
    // الضمائر المتكاملة
    private static final Map<String, WordCategory> PRONOUNS = new HashMap<>();
    static {
        // المتكلم
        PRONOUNS.put("انا", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("نحن", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("احنا", WordCategory.PRONOUN_PERSONAL); // عامية
        
        // المخاطب
        PRONOUNS.put("انت", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("انتي", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("انتما", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("انتم", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("انتن", WordCategory.PRONOUN_PERSONAL);
        
        // الغائب
        PRONOUNS.put("هو", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("هي", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("هما", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("هم", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("هن", WordCategory.PRONOUN_PERSONAL);
        
        // الضمائر المتصلة
        PRONOUNS.put("ني", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("ك", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("ه", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("ها", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("نا", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("كم", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("كن", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("هم", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("هن", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("هما", WordCategory.PRONOUN_OBJECT);
    }
    
    // أسماء الإشارة
    private static final Map<String, WordCategory> DEMONSTRATIVES = new HashMap<>();
    static {
        // المفرد
        DEMONSTRATIVES.put("هذا", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("هذه", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("ذلك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("تلك", WordCategory.DEMONSTRATIVE_FAR);
        
        // المثنى
        DEMONSTRATIVES.put("هذان", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("هاتان", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("ذانك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("تانك", WordCategory.DEMONSTRATIVE_FAR);
        
        // الجمع
        DEMONSTRATIVES.put("هؤلاء", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("اولئك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("هولاء", WordCategory.DEMONSTRATIVE_NEAR); // شائع
    }
    
    // أدوات الاستفهام
    private static final Map<String, WordCategory> QUESTION_WORDS = new HashMap<>();
    static {
        QUESTION_WORDS.put("هل", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("من", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("ما", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("ماذا", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("اين", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("متى", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("كيف", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("كم", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("لماذا", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("لمن", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("بكم", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("عن", WordCategory.QUESTION_TOOL);
    }
    
    // حروف الجر الشاملة
    private static final Map<String, WordCategory> PREPOSITIONS = new HashMap<>();
    static {
        String[] prepList = {
            "في", "من", "الى", "عن", "على", "مع", "حتى", "خلال",
            "بين", "تحت", "فوق", "دون", "ب", "ك", "ل", "مذ", "منذ",
            "رُبَّ", "مثل", "شبه", "نحو", "عند", "لدى", "لدن",
            "حول", "بعد", "قبل", "فوق", "تحت", "يمين", "شمال",
            "خارج", "داخل", "وسط", "فيم", "مما", "عما", "لما"
        };
        for (String prep : prepList) {
            PREPOSITIONS.put(prep, WordCategory.PREPOSITION);
        }
    }
    
    // أدوات الربط
    private static final Map<String, WordCategory> CONJUNCTIONS = new HashMap<>();
    static {
        // العاطفة
        CONJUNCTIONS.put("و", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("ف", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("ثم", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("او", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("ام", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("لا", WordCategory.CONJUNCTION_COORDINATING);
        
        // التفسيرية
        CONJUNCTIONS.put("لكن", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("لان", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("اذا", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("عندما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("بينما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("حيث", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("حين", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("لما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("ان", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("لو", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("لولا", WordCategory.CONJUNCTION_SUBORDINATING);
    }
    
    // الأفعال المتقدمة
    private static final Map<String, WordCategory> VERBS = new HashMap<>();
    static {
        // أفعال الوجود والتغير
        VERBS.put("كان", WordCategory.VERB_AUXILIARY);
        VERBS.put("صار", WordCategory.VERB_AUXILIARY);
        VERBS.put("اصبح", WordCategory.VERB_AUXILIARY);
        VERBS.put("اضحى", WordCategory.VERB_AUXILIARY);
        VERBS.put("امسى", WordCategory.VERB_AUXILIARY);
        VERBS.put("بات", WordCategory.VERB_AUXILIARY);
        VERBS.put("ظل", WordCategory.VERB_AUXILIARY);
        VERBS.put("ما زال", WordCategory.VERB_AUXILIARY);
        VERBS.put("ما برح", WordCategory.VERB_AUXILIARY);
        VERBS.put("ما فتئ", WordCategory.VERB_AUXILIARY);
        VERBS.put("ليس", WordCategory.VERB_AUXILIARY);
        VERBS.put("عاد", WordCategory.VERB_AUXILIARY);
        VERBS.put("مادام", WordCategory.VERB_AUXILIARY);
        
        // أفعال القول
        String[] sayVerbs = {"قال", "تكلم", "نطق", "حدث", "اخبر", "ابلغ", "صرح", "اعلن", "كتب", "ذكر"};
        for (String v : sayVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الحركة
        String[] moveVerbs = {"ذهب", "جاء", "اتى", "رجع", "سافر", "مشى", "ركض", "طار", "سقط", "صعد", "نزل"};
        for (String v : moveVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الإدراك
        String[] senseVerbs = {"رأى", "شاهد", "نظر", "ابصر", "سمع", "استمع", "شم", "ذاق", "لمس", "احس"};
        for (String v : senseVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الأكل والشرب
        String[] eatVerbs = {"اكل", "شرب", "تناول", "ابتلع", "مضغ", "ذاب", "عصر", "طحن"};
        for (String v : eatVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال النوم والاستيقاظ
        String[] sleepVerbs = {"نام", "رقد", "استيقظ", "صحا", "استراح", "تثاءب"};
        for (String v : sleepVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الجلوس والوقوف
        String[] sitVerbs = {"جلس", "وقف", "قعد", "استلقى", "انحنى", "ركع", "سجد"};
        for (String v : sitVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال التفكير
        String[] thinkVerbs = {"فكر", "تامل", "تذكر", "نسي", "علم", "تعلم", "فهم", "ادرك", "ذكى", "عبقري"};
        for (String v : thinkVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال المشاعر
        String[] feelVerbs = {"احب", "كره", "خاف", "فرح", "حزن", "غضب", "رضي", "حقد", "حسد", "شفق"};
        for (String v : feelVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال العمل والإنتاج
        String[] workVerbs = {"عمل", "صنع", "بنى", "رسم", "كتب", "قرأ", "خاط", "حاك", "زرع", "حصد"};
        for (String v : workVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال السؤال والجواب
        String[] askVerbs = {"سال", "اجاب", "استفسر", "استجاب", "رد", "علق"};
        for (String v : askVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الملكية
        String[] ownVerbs = {"ملك", "اشترى", "باع", "اعطى", "اخذ", "وهب", "ورث", "سلب", "نقل"};
        for (String v : ownVerbs) VERBS.put(v, WordCategory.VERB_PAST);
    }
    
    // الأسماء الشاملة
    private static final Map<String, WordCategory> NOUNS = new HashMap<>();
    static {
        // البشر
        String[] humans = {
            "انسان", "رجل", "امرأة", "طفل", "طفلة", "صبي", "فتاة", "شاب", "شيخ", "عجوز",
            "صديق", "صديقة", "عدو", "جار", "زوج", "زوجة", "اب", "ام", "اخ", "اخت",
            "ابن", "بنت", "عم", "خال", "عمة", "خالة", "جد", "جدة", "حفيد", "حفيدة",
            "مدير", "موظف", "طبيب", "ممرض", "مهندس", "معلم", "طالب", "تاجر", "فلاح", "عامل",
            "كاتب", "شاعر", "فنان", "مغني", "ممثل", "رياضي", "عالم", "فقيه", "قاضي", "محامي"
        };
        for (String n : humans) NOUNS.put(n, WordCategory.NOUN_HUMAN);
        
        // الأماكن
        String[] places = {
            "مكان", "موقع", "بيت", "منزل", "سكن", "قصر", "خيمة", "مدينة", "قرية", "بلد",
            "شارع", "ساحة", "حديقة", "غابة", "صحراء", "جبل", "وادي", "نهر", "بحر", "محيط",
            "مسجد", "كنيسة", "معبد", "مدرسة", "جامعة", "مستشفى", "سوق", "مطعم", "فندق", "مطار",
            "محطة", "سجن", "متحف", "مكتبة", "مسرح", "سينما", "ملعب", "نادي", "مقهى", "متجر"
        };
        for (String n : places) NOUNS.put(n, WordCategory.NOUN_PLACE);
        
        // الزمان
        String[] times = {
            "وقت", "زمن", "عصر", "فترة", "سنة", "شهر", "اسبوع", "يوم", "ليلة", "ساعة",
            "دقيقة", "ثانية", "صباح", "مساء", "ليل", "نهار", "فجر", "ظهر", "عصر", "مغرب",
            "عشاء", "امس", "غد", "مستقبل", "ماضي", "حاضر", "عهد", "حقبة", "زمان", "اثناء"
        };
        for (String n : times) NOUNS.put(n, WordCategory.NOUN_TIME);
        
        // الجمادات
        String[] objects = {
            "شيء", "جسم", "سيارة", "هاتف", "حاسوب", "جهاز", "كرسي", "طاولة", "سرير", "باب",
            "نافذة", "سقف", "جدار", "ارض", "سجادة", "ستارة", "مصباح", "ثريا", "مروحة", "مكيف",
            "ثلاجة", "فرن", "غسالة", "تلفاز", "راديو", "كاميرا", "ساعة", "قلم", "كتاب", "ورقة",
            "صندوق", "حقيبة", "سلة", "زجاجة", "كوب", "صحن", "ملعقة", "سكين", "شوكة", "منشفة"
        };
        for (String n : objects) NOUNS.put(n, WordCategory.NOUN_OBJECT);
        
        // المعنويات
        String[] abstracts = {
            "حب", "سلام", "حرب", "عدل", "ظلم", "حرية", "اسر", "كرامة", "عزة", "ذل",
            "فرح", "حزن", "امل", "يأس", "شجاعة", "جبن", "صدق", "كذب", "امانة", "خيانة",
            "علم", "جهل", "فهم", "غموض", "وضوح", "جمال", "قبح", "نور", "ظلام", "برود",
            "حرارة", "قوة", "ضعف", "غنى", "فقر", "صحة", "مرض", "حياة", "موت", "خلود"
        };
        for (String n : abstracts) NOUNS.put(n, WordCategory.NOUN_ABSTRACT);
    }
    
    // الصفات المتقدمة
    private static final Map<String, WordCategory> ADJECTIVES = new HashMap<>();
    static {
        // الألوان
        String[] colors = {
            "احمر", "اخضر", "ازرق", "اصفر", "اسود", "ابيض", "رمادي", "بني", "برتقالي", "وردي",
            "بنفسجي", "ذهبي", "فضي", "بيج", "عنابي", "نيلي", "فيروزي", "زيتوني", "ليموني", "خوخي"
        };
        for (String a : colors) ADJECTIVES.put(a, WordCategory.ADJECTIVE_COLOR);
        
        // الأحجام والأبعاد
        String[] sizes = {
            "كبير", "صغير", "طويل", "قصير", "عريض", "ضيق", "سميك", "رفيع", "عميق", "ضحل",
            "ضخم", "هائل", "ضئيل", "واسع", "محدود"
        };
        for (String a : sizes) ADJECTIVES.put(a, WordCategory.ADJECTIVE_SIZE);
        
        // الصفات المعنوية
        String[] qualities = {
            "جميل", "قبيح", "نظيف", "قذر", "سريع", "بطيء", "قوي", "ضعيف", "سعيد", "حزين",
            "ذكي", "غبي", "شجاع", "جبان", "لطيف", "فظ", "هادئ", "صاخب", "ناعم", "خشن",
            "لذيذ", "مر", "حلو", "مالح", "حامض", "حار", "بارد", "دافئ", "رطب", "جاف",
            "طازج", "عفن", "جديد", "قديم", "حديث", "عتيق", "اصلي", "مزيف", "نادر", "شائع"
        };
        for (String a : qualities) ADJECTIVES.put(a, WordCategory.ADJECTIVE_QUALITY);
        
        // الأعداد
        String[] numbers = {
            "اول", "ثان", "ثالث", "رابع", "خامس", "سادس", "سابع", "ثامن", "تاسع", "عاشر",
            "احدى عشر", "اثنا عشر", "اخر", "سابق", "لاحق", "مقبل", "متأخر", "باكر", "مبكر"
        };
        for (String a : numbers) ADJECTIVES.put(a, WordCategory.ADJECTIVE_NUMBER);
    }
    
    // الأعداد
    private static final Map<String, WordCategory> NUMBERS = new HashMap<>();
    static {
        String[] units = {"صفر", "واحد", "اثنان", "ثلاثة", "اربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة"};
        String[] teens = {"عشرة", "احدى عشر", "اثنا عشر", "ثلاثة عشر", "اربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"};
        String[] tens = {"عشرون", "ثلاثون", "اربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"};
        String[] hundreds = {"مئة", "مئتان", "ثلاثمئة", "اربعمئة", "خمسمئة", "ستمئة", "سبعمئة", "ثمانمئة", "تسعمئة"};
        
        for (String n : units) NUMBERS.put(n, WordCategory.NUMBER_CARDINAL);
        for (String n : teens) NUMBERS.put(n, WordCategory.NUMBER_CARDINAL);
        for (String n : tens) NUMBERS.put(n, WordCategory.NUMBER_CARDINAL);
        for (String n : hundreds) NUMBERS.put(n, WordCategory.NUMBER_CARDINAL);
        
        NUMBERS.put("الف", WordCategory.NUMBER_CARDINAL);
        NUMBERS.put("مليون", WordCategory.NUMBER_CARDINAL);
        NUMBERS.put("مليار", WordCategory.NUMBER_CARDINAL);
        NUMBERS.put("ترليون", WordCategory.NUMBER_CARDINAL);
    }
    
    // الحروف والأدوات
    private static final Map<String, WordCategory> PARTICLES = new HashMap<>();
    static {
        PARTICLES.put("لا", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("لم", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("لن", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("ما", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("ليس", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("غير", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("دون", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("بلا", WordCategory.PARTICLE_NEGATION);
        
        PARTICLES.put("س", WordCategory.PARTICLE_FUTURE);
        PARTICLES.put("سوف", WordCategory.PARTICLE_FUTURE);
        
        PARTICLES.put("ن", WordCategory.PARTICLE_EMPHASIS);
        PARTICLES.put("ان", WordCategory.PARTICLE_EMPHASIS);
        PARTICLES.put("قد", WordCategory.PARTICLE_EMPHASIS);
        PARTICLES.put("لقد", WordCategory.PARTICLE_EMPHASIS);
    }
    
    // ==================== أنماط التعرف ====================
    
    // أنماط الجذور العربية الثلاثية والرباعية
    private static final Pattern ROOT_PATTERN = Pattern.compile("^[ء-ي]{3,4}$");
    
    // ==================== البنية الداخلية للتحليل ====================
    
    /**
     * معلومات الكلمة التفصيلية
     */
    public static class WordAnalysis {
        private final String originalWord;
        private final String normalizedWord;
        private final WordCategory category;
        private final double confidence;
        private final List<String> possibleRoots;
        private final Map<String, Object> metadata;
        
        public WordAnalysis(String originalWord, String normalizedWord, 
                           WordCategory category, double confidence) {
            this.originalWord = originalWord;
            this.normalizedWord = normalizedWord;
            this.category = category;
            this.confidence = confidence;
            this.possibleRoots = new ArrayList<>();
            this.metadata = new HashMap<>();
        }
        
        public String getOriginalWord() { return originalWord; }
        public String getNormalizedWord() { return normalizedWord; }
        public WordCategory getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public List<String> getPossibleRoots() { return possibleRoots; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        public void addRoot(String root) { possibleRoots.add(root); }
        public void addMetadata(String key, Object value) { metadata.put(key, value); }
        
        @Override
        public String toString() {
            return String.format("%s [%s] (%.0f%%)", 
                originalWord, category.getArabicName(), confidence * 100);
        }
    }
    
    /**
     * نتيجة تحليل النص الكامل
     */
    public static class TextAnalysis {
        private final String originalText;
        private final List<WordAnalysis> words;
        private final Map<WordCategory, Long> statistics;
        private final List<String> sentences;
        
        public TextAnalysis(String originalText, List<WordAnalysis> words, List<String> sentences) {
            this.originalText = originalText;
            this.words = words;
            this.sentences = sentences;
            this.statistics = words.stream()
                .collect(Collectors.groupingBy(
                    WordAnalysis::getCategory, 
                    Collectors.counting()
                ));
        }
        
        public String getOriginalText() { return originalText; }
        public List<WordAnalysis> getWords() { return words; }
        public Map<WordCategory, Long> getStatistics() { return statistics; }
        public List<String> getSentences() { return sentences; }
        
        public long getWordCount() { return words.size(); }
        public long getKnownWords() { 
            return words.stream().filter(w -> w.getCategory() != WordCategory.UNKNOWN).count(); 
        }
        
        public double getRecognitionRate() {
            return words.isEmpty() ? 0 : (double) getKnownWords() / words.size();
        }
    }
    
    // ==================== الوظائف الرئيسية ====================
    
    /**
     * تحليل نص كامل
     */
    public static TextAnalysis analyze(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new TextAnalysis("", new ArrayList<>(), new ArrayList<>());
        }
        
        // تقسيم الجمل
        List<String> sentences = splitSentences(text);
        
        // تحليل الكلمات
        List<WordAnalysis> wordAnalyses = new ArrayList<>();
        
        for (String sentence : sentences) {
            String[] tokens = tokenize(sentence);
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    wordAnalyses.add(analyzeWord(token));
                }
            }
        }
        
        return new TextAnalysis(text, wordAnalyses, sentences);
    }
    
    /**
     * تحليل كلمة واحدة
     */
    public static WordAnalysis analyzeWord(String word) {
        String original = word;
        String normalized = normalize(word);
        
        // البحث المباشر في المعجم
        WordCategory category = lookup(normalized);
        double confidence = 1.0;
        
        // إذا لم يُعثر عليها، محاولة استنتاج الجذر
        if (category == WordCategory.UNKNOWN) {
            category = inferCategory(normalized);
            confidence = 0.6; // ثقة أقل للاستنتاج
        }
        
        WordAnalysis analysis = new WordAnalysis(original, normalized, category, confidence);
        
        // استخراج الجذور المحتملة
        List<String> roots = extractPossibleRoots(normalized);
        roots.forEach(analysis::addRoot);
        
        // إضافة بيانات وصفية
        analysis.addMetadata("length", normalized.length());
        analysis.addMetadata("hasTashkeel", hasTashkeel(original));
        
        return analysis;
    }
    
    /**
     * البحث في المعجم
     */
    private static WordCategory lookup(String word) {
        if (PRONOUNS.containsKey(word)) return PRONOUNS.get(word);
        if (DEMONSTRATIVES.containsKey(word)) return DEMONSTRATIVES.get(word);
        if (QUESTION_WORDS.containsKey(word)) return QUESTION_WORDS.get(word);
        if (PREPOSITIONS.containsKey(word)) return PREPOSITIONS.get(word);
        if (CONJUNCTIONS.containsKey(word)) return CONJUNCTIONS.get(word);
        if (VERBS.containsKey(word)) return VERBS.get(word);
        if (NOUNS.containsKey(word)) return NOUNS.get(word);
        if (ADJECTIVES.containsKey(word)) return ADJECTIVES.get(word);
        if (NUMBERS.containsKey(word)) return NUMBERS.get(word);
        if (PARTICLES.containsKey(word)) return PARTICLES.get(word);
        return WordCategory.UNKNOWN;
    }
    
    /**
     * استنتاج التصنيف من أنماط الكلمة
     */
    private static WordCategory inferCategory(String word) {
        // التحقق من أنماط الأفعال
        if (word.startsWith("ي") && word.length() > 3) {
            return WordCategory.VERB_PRESENT;
        }
        if (word.startsWith("ت") && word.length() > 3) {
            return WordCategory.VERB_PRESENT;
        }
        if (word.startsWith("ا") && word.length() > 3 && !word.startsWith("ال")) {
            return WordCategory.VERB_IMPERATIVE;
        }
        
        // التحقق من الجمع
        if (word.endsWith("ون") || word.endsWith("ين") || word.endsWith("ات")) {
            return WordCategory.NOUN_COLLECTIVE;
        }
        
        // التحقق من التصغير
        if (word.matches("^.ُ.َيْ.ِ.$")) {
            return WordCategory.NOUN_OBJECT;
        }
        
        // التحقق من اسم الآلة
        if (word.startsWith("مِ") || word.matches("^مِ.{2,4}$")) {
            return WordCategory.NOUN_OBJECT;
        }
        
        return WordCategory.UNKNOWN;
    }
    
    /**
     * استخراج الجذور المحتملة
     */
    private static List<String> extractPossibleRoots(String word) {
        List<String> roots = new ArrayList<>();
        
        if (word.length() < 3) return roots;
        
        // إزالة الزوائد الشائعة
        String stripped = word
            .replaceAll("^(ال|وال|فال|بال|كال|لل)", "")
            .replaceAll("(ة|ات|ون|ين|ان|ت|ن|ي|ا|و)$", "");
        
        // إذا تبقت 3-4 أحرف، قد تكون جذراً
        if (stripped.length() >= 3 && stripped.length() <= 4 && ROOT_PATTERN.matcher(stripped).matches()) {
            roots.add(stripped);
        }
        
        // محاولة استخراج الجذر الثلاثي
        if (word.length() >= 3) {
            // نمط فَعَلَ -> ف ع ل
            StringBuilder root = new StringBuilder();
            for (int i = 0; i < word.length() && root.length() < 3; i++) {
                char c = word.charAt(i);
                if (isArabicConsonant(c)) {
                    root.append(c);
                }
            }
            if (root.length() == 3) {
                roots.add(root.toString());
            }
        }
        
        return roots;
    }
    
    /**
     * تقسيم النص إلى جمل
     */
    private static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            current.append(c);
            
            if (c == '.' || c == '!' || c == '؟' || c == '?') {
                String sentence = current.toString().trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
                current = new StringBuilder();
            }
        }
        
        if (current.length() > 0) {
            sentences.add(current.toString().trim());
        }
        
        return sentences.isEmpty() ? Arrays.asList(text) : sentences;
    }
    
    /**
     * تقطيع النص إلى كلمات
     */
    private static String[] tokenize(String text) {
        // إزالة التشكيل للتقطيع
        String clean = text.replaceAll("[\\u064B-\\u065F\\u0670\\u0640]", "");
        return clean.split("\\s+");
    }
    
    /**
     * تطبيع الكلمة
     */
    private static String normalize(String text) {
        if (text == null) return "";
        
        return text
            // إزالة التشكيل
            .replaceAll("[\\u064B-\\u065F\\u0670\\u0640]", "")
            // توحيد الهمزات
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ٱ", "ا")
            // توحيد التاء المربوطة والهاء
            .replace("ة", "ه")
            // توحيد الألف المقصورة والياء
            .replace("ى", "ي")
            // إزالة التطويل
            .replace("ـ", "")
            // تحويل إلى حروف صغيرة (للإنجليزية المختلطة)
            .toLowerCase()
            .trim();
    }
    
    /**
     * التحقق من وجود تشكيل
     */
    private static boolean hasTashkeel(String text) {
        return text.matches(".*[\\u064B-\\u065F\\u0670].*");
    }
    
    /**
     * التحقق من حرف عربي صحيح
     */
    private static boolean isArabicConsonant(char c) {
        return (c >= '\u0621' && c <= '\u064A') && 
               !"اوي".contains(String.valueOf(c)); // استبعاد الأحرف المتحركة
    }
    
    // ==================== وظائف مساعدة ====================
    
    /**
     * البحث عن كلمات مشابهة
     */
    public static List<String> findSimilarWords(String word, int maxDistance) {
        String normalized = normalize(word);
        List<String> similar = new ArrayList<>();
        
        // جمع كل الكلمات المعروفة
        Set<String> allWords = new HashSet<>();
        allWords.addAll(PRONOUNS.keySet());
        allWords.addAll(DEMONSTRATIVES.keySet());
        allWords.addAll(QUESTION_WORDS.keySet());
        allWords.addAll(PREPOSITIONS.keySet());
        allWords.addAll(CONJUNCTIONS.keySet());
        allWords.addAll(VERBS.keySet());
        allWords.addAll(NOUNS.keySet());
        allWords.addAll(ADJECTIVES.keySet());
        
        for (String dictWord : allWords) {
            int distance = calculateLevenshtein(normalized, dictWord);
            if (distance <= maxDistance && distance > 0) {
                similar.add(dictWord);
            }
        }
        
        return similar.stream()
            .sorted(Comparator.comparingInt(w -> calculateLevenshtein(normalized, w)))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * حساب مسافة ليفنشتاين
     */
    private static int calculateLevenshtein(String s1, String s2) {
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
     * الحصول على إحصائيات المعجم
     */
    public static Map<String, Integer> getLexiconStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("ضمائر", PRONOUNS.size());
        stats.put("إشارات", DEMONSTRATIVES.size());
        stats.put("استفهام", QUESTION_WORDS.size());
        stats.put("حروف جر", PREPOSITIONS.size());
        stats.put("روابط", CONJUNCTIONS.size());
        stats.put("أفعال", VERBS.size());
        stats.put("أسماء", NOUNS.size());
        stats.put("صفات", ADJECTIVES.size());
        stats.put("أعداد", NUMBERS.size());
        stats.put("أدوات", PARTICLES.size());
        
        int total = stats.values().stream().mapToInt(Integer::intValue).sum();
        stats.put("الإجمالي", total);
        
        return stats;
    }
    
    /**
     * طباعة تحليل منسق
     */
    public static void printAnalysis(TextAnalysis analysis) {
        System.out.println("=== تحليل النص ===");
        System.out.println("النص: " + analysis.getOriginalText());
        System.out.println("عدد الجمل: " + analysis.getSentences().size());
        System.out.println("عدد الكلمات: " + analysis.getWordCount());
        System.out.println("نسبة التعرف: " + String.format("%.1f%%", analysis.getRecognitionRate() * 100));
        System.out.println("\nالتفاصيل:");
        
        for (WordAnalysis word : analysis.getWords()) {
            System.out.printf("  %-15s [%s] (%.0f%%)%n", 
                word.getOriginalWord(), 
                word.getCategory().getArabicName(),
                word.getConfidence() * 100);
            
            if (!word.getPossibleRoots().isEmpty()) {
                System.out.printf("    الجذور المحتملة: %s%n", 
                    String.join(", ", word.getPossibleRoots()));
            }
        }
        
        System.out.println("\nالإحصائيات:");
        analysis.getStatistics().forEach((cat, count) -> 
            System.out.printf("  %s: %d%n", cat.getArabicName(), count));
    }
}

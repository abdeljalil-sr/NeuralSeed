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
        PRONOUN_DEMONSTRATIVE("ضمير إشارة"),
        
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
        CONJUNCTION_CAUSAL("سببي"),
        CONJUNCTION_CONCESSIVE("استدراكي"),
        
        // الأفعال
        VERB_PAST("فعل ماضٍ"),
        VERB_PRESENT("فعل مضارع"),
        VERB_IMPERATIVE("فعل أمر"),
        VERB_AUXILIARY("فعل ناقص"),
        VERB_TRANSITIVE("فعل متعدٍ"),
        VERB_INTRANSITIVE("فعل لازم"),
        
        // الأسماء
        NOUN_HUMAN("اسم إنسان"),
        NOUN_PLACE("اسم مكان"),
        NOUN_TIME("اسم زمان"),
        NOUN_OBJECT("اسم جماد"),
        NOUN_ABSTRACT("اسم معنوي"),
        NOUN_COLLECTIVE("جمع سالم"),
        NOUN_INSTRUMENT("اسم آلة"),
        NOUN_ANIMAL("اسم حيوان"),
        NOUN_PLANT("اسم نبات"),
        NOUN_FOOD("اسم طعام"),
        NOUN_BODY("اسم جسم"),
        NOUN_NATURE("اسم طبيعة"),
        NOUN_SCIENCE("اسم علم"),
        NOUN_TECHNOLOGY("اسم تكنولوجيا"),
        
        // الصفات
        ADJECTIVE_COLOR("صفة لون"),
        ADJECTIVE_SIZE("صفة حجم"),
        ADJECTIVE_QUALITY("صفة معنوية"),
        ADJECTIVE_NUMBER("صفة عدد"),
        ADJECTIVE_TEMPERATURE("صفة حرارة"),
        ADJECTIVE_SHAPE("صفة شكل"),
        ADJECTIVE_TEXTURE("صفة ملمس"),
        ADJECTIVE_SOUND("صفة صوت"),
        ADJECTIVE_TASTE("صفة طعم"),
        ADJECTIVE_SMELL("صفة رائحة"),
        ADJECTIVE_SPEED("صفة سرعة"),
        ADJECTIVE_INTELLIGENCE("صفة ذكاء"),
        ADJECTIVE_MORAL("صفة أخلاق"),
        ADJECTIVE_EMOTIONAL("صفة عاطفية"),
        
        // الأعداد
        NUMBER_CARDINAL("عدد أصلي"),
        NUMBER_ORDINAL("عدد ترتيبي"),
        NUMBER_FRACTION("عدد كسري"),
        NUMBER_MULTIPLICATIVE("عدد توكيدي"),
        
        // حروف أخرى
        PARTICLE_NEGATION("نافية"),
        PARTICLE_FUTURE("استقبال"),
        PARTICLE_EMPHASIS("توكيد"),
        PARTICLE_INTERROGATION("استفهام"),
        PARTICLE_PROHIBITION("نهي"),
        PARTICLE_EXCEPTION("استثناء"),
        PARTICLE_CAUSATION("تعليل"),
        PARTICLE_RESUMPTION("استئناف"),
        
        // النداء
        VOCATIVE_PARTICLE("أداة نداء"),
        
        // التعجب
        INTERJECTION_PARTICLE("أداة تعجب"),
        
        // الظروف
        ADVERB_TIME("ظرف زمان"),
        ADVERB_PLACE("ظرف مكان"),
        ADVERB_MANNER("ظرف حال"),
        ADVERB_DEGREE("ظرف درجة"),
        
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
        PRONOUNS.put("احنا", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("إنا", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("نحنا", WordCategory.PRONOUN_PERSONAL);
        
        // المخاطب
        PRONOUNS.put("انت", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("انتي", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("انتما", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("انتم", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("انتن", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("أنت", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("أنتِ", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("أنتما", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("أنتم", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("أنتن", WordCategory.PRONOUN_PERSONAL);
        
        // الغائب
        PRONOUNS.put("هو", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("هي", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("هما", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("هم", WordCategory.PRONOUN_PERSONAL);
        PRONOUNS.put("هن", WordCategory.PRONOUN_PERSONAL);
        
        // الضمائر المتصلة - المتكلم
        PRONOUNS.put("ني", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("ي", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("نا", WordCategory.PRONOUN_OBJECT);
        
        // الضمائر المتصلة - المخاطب
        PRONOUNS.put("ك", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("كما", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("كم", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("كن", WordCategory.PRONOUN_OBJECT);
        
        // الضمائر المتصلة - الغائب
        PRONOUNS.put("ه", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("ها", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("هما", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("هم", WordCategory.PRONOUN_OBJECT);
        PRONOUNS.put("هن", WordCategory.PRONOUN_OBJECT);
        
        // الضمائر الملكية
        PRONOUNS.put("لي", WordCategory.PRONOUN_POSSESSIVE);
        PRONOUNS.put("لك", WordCategory.PRONOUN_POSSESSIVE);
        PRONOUNS.put("له", WordCategory.PRONOUN_POSSESSIVE);
        PRONOUNS.put("لها", WordCategory.PRONOUN_POSSESSIVE);
        PRONOUNS.put("لنا", WordCategory.PRONOUN_POSSESSIVE);
        PRONOUNS.put("لكم", WordCategory.PRONOUN_POSSESSIVE);
        PRONOUNS.put("لكن", WordCategory.PRONOUN_POSSESSIVE);
        PRONOUNS.put("لهم", WordCategory.PRONOUN_POSSESSIVE);
        PRONOUNS.put("لهن", WordCategory.PRONOUN_POSSESSIVE);
        
        // ضمائر الإشارة
        PRONOUNS.put("هذا", WordCategory.PRONOUN_DEMONSTRATIVE);
        PRONOUNS.put("هذه", WordCategory.PRONOUN_DEMONSTRATIVE);
        PRONOUNS.put("ذلك", WordCategory.PRONOUN_DEMONSTRATIVE);
        PRONOUNS.put("تلك", WordCategory.PRONOUN_DEMONSTRATIVE);
        PRONOUNS.put("هؤلاء", WordCategory.PRONOUN_DEMONSTRATIVE);
        PRONOUNS.put("اولئك", WordCategory.PRONOUN_DEMONSTRATIVE);
    }
    
    // أسماء الإشارة
    private static final Map<String, WordCategory> DEMONSTRATIVES = new HashMap<>();
    static {
        // المفرد المذكر
        DEMONSTRATIVES.put("هذا", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("ذلك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("ذاك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("ذانك", WordCategory.DEMONSTRATIVE_FAR);
        
        // المفرد المؤنث
        DEMONSTRATIVES.put("هذه", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("تلك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("تاينك", WordCategory.DEMONSTRATIVE_FAR);
        
        // المثنى
        DEMONSTRATIVES.put("هذان", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("هاتان", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("ذانك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("تانك", WordCategory.DEMONSTRATIVE_FAR);
        
        // الجمع
        DEMONSTRATIVES.put("هؤلاء", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("اولئك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("هولاء", WordCategory.DEMONSTRATIVE_NEAR);
        DEMONSTRATIVES.put("أولئك", WordCategory.DEMONSTRATIVE_FAR);
        DEMONSTRATIVES.put("هاتين", WordCategory.DEMONSTRATIVE_NEAR);
    }
    
    // أدوات الاستفهام
    private static final Map<String, WordCategory> QUESTION_WORDS = new HashMap<>();
    static {
        QUESTION_WORDS.put("هل", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("من", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("ما", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("ماذا", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("اين", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("أين", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("متى", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("كيف", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("كم", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("لماذا", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("لمن", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("بكم", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("عن", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("اي", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("أي", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("ايان", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("أيان", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("كم", WordCategory.QUESTION_TOOL);
        QUESTION_WORDS.put("كيف", WordCategory.QUESTION_TOOL);
    }
    
    // حروف الجر الشاملة
    private static final Map<String, WordCategory> PREPOSITIONS = new HashMap<>();
    static {
        String[] prepList = {
            "في", "من", "الى", "إلى", "عن", "على", "مع", "حتى", "خلال",
            "بين", "تحت", "فوق", "دون", "ب", "ك", "ل", "مذ", "منذ",
            "رب", "مثل", "شبه", "نحو", "عند", "لدى", "لدن",
            "حول", "بعد", "قبل", "يمين", "شمال", "خلف", "امام",
            "خارج", "داخل", "وسط", "فيم", "مما", "عما", "لما",
            "ب", "بِ", "كي", "لعل", "حاشا", "خلا", "عدا",
            "فوق", "تحت", "وراء", "أمام", "جانب", "قرب", "تلقاء",
            "بين", "فوق", "تحت", "يمين", "شمال", "شرق", "غرب",
            "شطر", "نحو", "جهة", "اتجاه", "حذاء", "عند", "لدن"
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
        CONJUNCTIONS.put("أو", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("ام", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("أم", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("لا", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("لكن", WordCategory.CONJUNCTION_COORDINATING);
        CONJUNCTIONS.put("بل", WordCategory.CONJUNCTION_COORDINATING);
        
        // التفسيرية
        CONJUNCTIONS.put("لان", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("لأن", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("اذا", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("إذا", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("عندما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("بينما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("حيث", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("حين", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("حينما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("لما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("ان", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("أن", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("لو", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("لولا", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("كلما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("مهما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("اينما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("حيثما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("كيفما", WordCategory.CONJUNCTION_SUBORDINATING);
        CONJUNCTIONS.put("متى", WordCategory.CONJUNCTION_SUBORDINATING);
        
        // السببية
        CONJUNCTIONS.put("لان", WordCategory.CONJUNCTION_CAUSAL);
        CONJUNCTIONS.put("لأن", WordCategory.CONJUNCTION_CAUSAL);
        CONJUNCTIONS.put("بما", WordCategory.CONJUNCTION_CAUSAL);
        CONJUNCTIONS.put("كي", WordCategory.CONJUNCTION_CAUSAL);
        CONJUNCTIONS.put("حتى", WordCategory.CONJUNCTION_CAUSAL);
        
        // الاستدراكية
        CONJUNCTIONS.put("لكن", WordCategory.CONJUNCTION_CONCESSIVE);
        CONJUNCTIONS.put("ولكن", WordCategory.CONJUNCTION_CONCESSIVE);
        CONJUNCTIONS.put("بل", WordCategory.CONJUNCTION_CONCESSIVE);
        CONJUNCTIONS.put("غير", WordCategory.CONJUNCTION_CONCESSIVE);
        CONJUNCTIONS.put("على", WordCategory.CONJUNCTION_CONCESSIVE);
    }
    
    // الأفعال المتقدمة
    private static final Map<String, WordCategory> VERBS = new HashMap<>();
    static {
        // أفعال الوجود والتغير
        VERBS.put("كان", WordCategory.VERB_AUXILIARY);
        VERBS.put("صار", WordCategory.VERB_AUXILIARY);
        VERBS.put("اصبح", WordCategory.VERB_AUXILIARY);
        VERBS.put("أصبح", WordCategory.VERB_AUXILIARY);
        VERBS.put("اضحى", WordCategory.VERB_AUXILIARY);
        VERBS.put("أضحى", WordCategory.VERB_AUXILIARY);
        VERBS.put("امسى", WordCategory.VERB_AUXILIARY);
        VERBS.put("أمسى", WordCategory.VERB_AUXILIARY);
        VERBS.put("بات", WordCategory.VERB_AUXILIARY);
        VERBS.put("ظل", WordCategory.VERB_AUXILIARY);
        VERBS.put("ما زال", WordCategory.VERB_AUXILIARY);
        VERBS.put("ما برح", WordCategory.VERB_AUXILIARY);
        VERBS.put("ما فتئ", WordCategory.VERB_AUXILIARY);
        VERBS.put("ليس", WordCategory.VERB_AUXILIARY);
        VERBS.put("عاد", WordCategory.VERB_AUXILIARY);
        VERBS.put("مادام", WordCategory.VERB_AUXILIARY);
        VERBS.put("دام", WordCategory.VERB_AUXILIARY);
        VERBS.put("ليس", WordCategory.VERB_AUXILIARY);
        VERBS.put("وجد", WordCategory.VERB_AUXILIARY);
        VERBS.put("صير", WordCategory.VERB_AUXILIARY);
        
        // أفعال القول
        String[] sayVerbs = {"قال", "تكلم", "نطق", "حدث", "اخبر", "أخبر", "ابلغ", "أبلغ", 
            "صرح", "اعلن", "أعلن", "كتب", "ذكر", "روى", "حدث", "سرد", "وصف", "نقل",
            "رد", "اجاب", "أجاب", "علق", "اشار", "أشار", "اوضح", "أوضح", "بيّن",
            "فسر", "عرف", "علم", "اخبر", "نبه", "انذر", "أنذر", "وعظ", "نصح"};
        for (String v : sayVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الحركة
        String[] moveVerbs = {"ذهب", "جاء", "اتى", "أتى", "رجع", "سافر", "مشى", "ركض", 
            "طار", "سقط", "صعد", "نزل", "دخل", "خرج", "مر", "عبر", "اجتاز", "اجتاز",
            "وصل", "غادر", "فارق", "لحق", "سبق", "تبع", "لحق", "هرب", "لاذ", "اختبأ",
            "تسلل", "تسلق", "قفز", "وثب", "انطلق", "اندفع", "انحدر", "صعد", "هبط"};
        for (String v : moveVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الإدراك
        String[] senseVerbs = {"رأى", "شاهد", "نظر", "ابصر", "أبصر", "سمع", "استمع", 
            "شم", "ذاق", "لمس", "احس", "أحس", "ادرك", "أدرك", "فهم", "عقل", "لاحظ",
            "تأمل", "راقب", "تفحص", "دقق", "فحص", "كشف", "اكتشف", "وجد", "لاحظ"};
        for (String v : senseVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الأكل والشرب
        String[] eatVerbs = {"اكل", "أكل", "شرب", "تناول", "ابتلع", "أبتلع", "مضغ", 
            "ذاب", "عصر", "طحن", "طهى", "طبخ", "شوى", "قلى", "سلق", "خبز", "عجن",
            "قطع", "شرح", "قشر", "قشر", "سلخ", "نزع", "نظف", "غسل"};
        for (String v : eatVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال النوم والاستيقاظ
        String[] sleepVerbs = {"نام", "رقد", "استيقظ", "صحا", "استراح", "تثاءب", 
            "غفا", "نعس", "هجر", "سهر", "انتبه", "يقظ", "يقظ"};
        for (String v : sleepVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الجلوس والوقوف
        String[] sitVerbs = {"جلس", "وقف", "قعد", "استلقى", "انحنى", "ركع", "سجد", 
            "انصب", "اعتدل", "استقام", "انثنى", "انحنى", "انخفض", "ارتفع"};
        for (String v : sitVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال التفكير
        String[] thinkVerbs = {"فكر", "تامل", "تأمل", "تذكر", "نسي", "علم", "تعلم", 
            "فهم", "ادرك", "أدرك", "ذكى", "عبقري", "حلل", "نقاش", "نظر", "درس",
            "بحث", "قارن", "ميز", "فرق", "جمع", "طرح", "ضرب", "قسم", "حسب"};
        for (String v : thinkVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال المشاعر
        String[] feelVerbs = {"احب", "أحب", "كره", "خاف", "فرح", "حزن", "غضب", "رضي", 
            "حقد", "حسد", "شفق", "رحم", "عطف", "شفق", "حن", "ولع", "هوى", "عشق",
            "اعجب", "أعجب", "انبهر", "تعجب", "دهش", "ارتعب", "رعب", "فزع", "ذعر"};
        for (String v : feelVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال العمل والإنتاج
        String[] workVerbs = {"عمل", "صنع", "بنى", "رسم", "كتب", "قرأ", "خاط", "حاك", 
            "زرع", "حصد", "حفر", "نحت", "صاغ", "شكل", "كون", "كون", "كون",
            "صمم", "اخترع", "ابتكر", "أبتكر", "نظم", "رتب", "اعد", "أعد"};
        for (String v : workVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال السؤال والجواب
        String[] askVerbs = {"سال", "سأل", "اجاب", "أجاب", "استفسر", "استجاب", "رد", 
            "علق", "نقاش", "ناقش", "حاور", "سال", "استنكر", "اعترض", "أعترض"};
        for (String v : askVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال الملكية
        String[] ownVerbs = {"ملك", "اشترى", "أشترى", "باع", "اعطى", "أعطى", "اخذ", 
            "أخذ", "وهب", "ورث", "سلب", "نقل", "حاز", "انتزع", "استولى", "استولى"};
        for (String v : ownVerbs) VERBS.put(v, WordCategory.VERB_PAST);
        
        // أفعال إضافية متنوعة
        String[] otherVerbs = {"فعل", "صنع", "عمل", "حدث", "وجد", "فقد", "خسر", "ربح",
            "نجح", "فشل", "فاز", "هزم", "غلب", "انتصر", "انهزم", "صمد", "قاوم",
            "سلم", "استسلم", "حارب", "قاتل", "دافع", "هاجم", "داهم", "اقتحم"};
        for (String v : otherVerbs) VERBS.put(v, WordCategory.VERB_PAST);
    }
    
    // الأسماء الشاملة
    private static final Map<String, WordCategory> NOUNS = new HashMap<>();
    static {
        // البشر
        String[] humans = {
            "انسان", "إنسان", "رجل", "امرأة", "أمرأة", "طفل", "طفلة", "صبي", "فتاة", "شاب", "شيخ", "عجوز",
            "صديق", "صديقة", "عدو", "جار", "زوج", "زوجة", "اب", "أب", "ام", "أم", "اخ", "أخ", "اخت", "أخت",
            "ابن", "أبن", "بنت", "عم", "خال", "عمة", "خالة", "جد", "جدة", "حفيد", "حفيدة",
            "مدير", "موظف", "طبيب", "ممرض", "مهندس", "معلم", "طالب", "تاجر", "فلاح", "عامل",
            "كاتب", "شاعر", "فنان", "مغني", "ممثل", "رياضي", "عالم", "فقيه", "قاضي", "محامي",
            "شرطي", "جندي", "ضابط", "قائد", "ملك", "رئيس", "وزير", "سفير", "قنصل", "محاسب",
            "نجار", "حداد", "خياط", "حلاق", "بناء", "سباك", "كهربائي", "ميكانيكي", "سائق", "طيار",
            "بحار", "غواص", "صياد", "راعي", "مزارع", "بستاني", "حداد", "صائغ", "جزار", "خباز"
        };
        for (String n : humans) NOUNS.put(n, WordCategory.NOUN_HUMAN);
        
        // الأماكن
        String[] places = {
            "مكان", "موقع", "بيت", "منزل", "سكن", "قصر", "خيمة", "مدينة", "قرية", "بلد",
            "شارع", "ساحة", "حديقة", "غابة", "صحراء", "جبل", "وادي", "نهر", "بحر", "محيط",
            "مسجد", "كنيسة", "معبد", "مدرسة", "جامعة", "مستشفى", "سوق", "مطعم", "فندق", "مطار",
            "محطة", "سجن", "متحف", "مكتبة", "مسرح", "سينما", "ملعب", "نادي", "مقهى", "متجر",
            "مخبز", "مسلخ", "مستودع", "مصنع", "مكتب", "غرفة", "صالة", "مطبخ", "حمام", "مرحاض",
            "سطح", "قبو", "حديقة", "منتزه", "غابة", "جungle", "صحراء", "هضبة", "سهل", "وادي"
        };
        for (String n : places) NOUNS.put(n, WordCategory.NOUN_PLACE);
        
        // الزمان
        String[] times = {
            "وقت", "زمن", "عصر", "فترة", "سنة", "شهر", "اسبوع", "يوم", "ليلة", "ساعة",
            "دقيقة", "ثانية", "صباح", "مساء", "ليل", "نهار", "فجر", "ظهر", "عصر", "مغرب",
            "عشاء", "امس", "أمس", "غد", "مستقبل", "ماضي", "حاضر", "عهد", "حقبة", "زمان", "اثناء",
            "اثناء", "أثناء", "لحظة", "هنيهة", "ثانية", "دقيقة", "ساعة", "يوم", "ليلة", "اسبوع",
            "شهر", "سنة", "عقد", "قرن", "الفية", "دهر", "دهور", "ابد", "أبد", "سرمد"
        };
        for (String n : times) NOUNS.put(n, WordCategory.NOUN_TIME);
        
        // الجمادات
        String[] objects = {
            "شيء", "جسم", "سيارة", "هاتف", "حاسوب", "جهاز", "كرسي", "طاولة", "سرير", "باب",
            "نافذة", "سقف", "جدار", "ارض", "أرض", "سجادة", "ستارة", "مصباح", "ثريا", "مروحة", "مكيف",
            "ثلاجة", "فرن", "غسالة", "تلفاز", "راديو", "كاميرا", "ساعة", "قلم", "كتاب", "ورقة",
            "صندوق", "حقيبة", "سلة", "زجاجة", "كوب", "صحن", "ملعقة", "سكين", "شوكة", "منشفة",
            "مفتاح", "قفل", "سلسلة", "حبل", "خيط", "ابرة", "أبرة", "مسامير", "صامولة", "مطرقة",
            "مفك", "منشار", "مقص", "سكين", "ساطور", "فأس", "معول", "جرافة", "عربة", "دراجة"
        };
        for (String n : objects) NOUNS.put(n, WordCategory.NOUN_OBJECT);
        
        // المعنويات
        String[] abstracts = {
            "حب", "سلام", "حرب", "عدل", "ظلم", "حرية", "اسر", "أسر", "كرامة", "عزة", "ذل",
            "فرح", "حزن", "امل", "أمل", "يأس", "شجاعة", "جبن", "صدق", "كذب", "امانة", "أمانة", "خيانة",
            "علم", "جهل", "فهم", "غموض", "وضوح", "جمال", "قبح", "نور", "ظلام", "برود",
            "حرارة", "قوة", "ضعف", "غنى", "فقر", "صحة", "مرض", "حياة", "موت", "خلود",
            "عدم", "وجود", "حقيقة", "وهم", "خيال", "واقع", "فكر", "عقل", "روح", "نفس",
            "قلب", "جوارح", "احساس", "أحساس", "شعور", "إحساس", "إدراك", "وعي", "لاوعي", "لاوعي"
        };
        for (String n : abstracts) NOUNS.put(n, WordCategory.NOUN_ABSTRACT);
        
        // الحيوانات
        String[] animals = {
            "حيوان", "كلب", "قطة", "حصان", "بقرة", "خروف", "ماعز", "جمل", "حمار", "بغل",
            "فيل", "اسد", "ليث", "نمر", "فهد", "ذئب", "ثعلب", "دب", "قرد", "غوريلا",
            "شمبانزي", "زرافة", "زرافة", "زرافة", "زرافة", "زرافة", "زرافة", "زرافة", "زرافة", "زرافة",
            "دجاجة", "ديك", "بطة", "وزة", "طائر", "عصفور", "حمامة", "نسر", "صقر", "بومة",
            "سمكة", "حوت", "دلفين", "قرش", "تمساح", "ثعبان", "عقرب", "عنكبوت", "نحلة", "فراشة"
        };
        for (String n : animals) NOUNS.put(n, WordCategory.NOUN_ANIMAL);
        
        // النباتات
        String[] plants = {
            "نبات", "شجرة", "زهرة", "ورد", "عشب", "بذرة", "ثمرة", "فاكهة", "خضار", "ورق",
            "ساق", "جذر", "غصن", "فرع", "ثمر", "ثمار", "تفاح", "برتقال", "موز", "عنب",
            "تين", "رمان", "خوخ", "مشمش", "كرز", "توت", "فراولة", "بطيخ", "شمام", "خيار",
            "طماطم", "بصل", "ثوم", "جزر", "بطاطا", "باذنجان", "فلفل", "ملفوف", "سبانخ", "جرجير"
        };
        for (String n : plants) NOUNS.put(n, WordCategory.NOUN_PLANT);
        
        // الطعام
        String[] foods = {
            "طعام", "شراب", "ماء", "خبز", "ارز", "أرز", "لحم", "دجاج", "سمك", "بيض",
            "حليب", "جبن", "زبدة", "زيت", "سكر", "ملح", "فلفل", "بهارات", "عسل", "مربى",
            "شاي", "قهوة", "عصير", "حساء", "شوربة", "سلطة", "مقبلات", "حلوى", "كعك", "بسكويت",
            "معكرونة", "بيتزا", "برجر", "ساندويتش", "فلافل", "شاورما", "كباب", "كفتة", "مشاوي", "مندي"
        };
        for (String n : foods) NOUNS.put(n, WordCategory.NOUN_FOOD);
        
        // جسم الإنسان
        String[] bodyParts = {
            "رأس", "وجه", "عين", "اذن", "أذن", "انف", "أنف", "فم", "شفة", "لسان",
            "سن", "أسنان", "شعر", "جبهة", "خد", "ذقن", "عنق", "رقبة", "كتف", "صدر",
            "ظهر", "بطن", "يد", "ذراع", "كف", "اصبع", "أصبع", "رجل", "فخذ", "ساق",
            "قدم", "كعب", "اصبع", "أصبع", "ظفر", "جلد", "عظم", "عضل", "دم", "قلب"
        };
        for (String n : bodyParts) NOUNS.put(n, WordCategory.NOUN_BODY);
        
        // الطبيعة
        String[] nature = {
            "شمس", "قمر", "نجم", "سماء", "ارض", "أرض", "هواء", "نار", "ماء", "تراب",
            "حجر", "صخر", "رمل", "طين", "غبار", "ضباب", "سحاب", "مطر", "ثلج", "برد",
            "ريح", "عاصفة", "رعد", "برق", "قوس", "قزح", "فجر", "غروب", "شروق", "ليل",
            "نهار", "فصل", "ربيع", "صيف", "خريف", "شتاء", "حر", "برد", "رطوبة", "جفاف"
        };
        for (String n : nature) NOUNS.put(n, WordCategory.NOUN_NATURE);
        
        // العلوم
        String[] science = {
            "علم", "فيزياء", "كيمياء", "احياء", "أحياء", "رياضيات", "هندسة", "فلك", "جيولوجيا", "جغرافيا",
            "تاريخ", "فلسفة", "منطق", "نفس", "اجتماع", "اقتصاد", "سياسة", "قانون", "لغة", "ادب",
            "أدب", "شعر", "نثر", "رواية", "قصة", "مسرح", "سينما", "موسيقى", "رسم", "نحت",
            "عمارة", "طب", "صيدلة", "تمريض", "بيطرة", "زراعة", "صناعة", "تجارة", "تقنية", "تكنولوجيا"
        };
        for (String n : science) NOUNS.put(n, WordCategory.NOUN_SCIENCE);
        
        // التكنولوجيا
        String[] tech = {
            "حاسوب", "كمبيوتر", "هاتف", "جوال", "نقال", "شبكة", "انترنت", "أنترنت", "موقع", "تطبيق",
            "برنامج", "نظام", "ذكاء", "اصطناعي", "أصطناعي", "روبوت", "آلة", "جهاز", "شاشة", "لوحة",
            "مفاتيح", "فأرة", "ماوس", "طابعة", "ماسح", "ضوئي", "كاميرا", "ميكروفون", "سماعة", "مكبر",
            "صوت", "بطارية", "شاحن", "سلك", "كابل", "واي", "فاي", "بلوتوث", "usb", "hdmi"
        };
        for (String n : tech) NOUNS.put(n, WordCategory.NOUN_TECHNOLOGY);
    }
    
    // الصفات المتقدمة
    private static final Map<String, WordCategory> ADJECTIVES = new HashMap<>();
    static {
        // الألوان
        String[] colors = {
            "احمر", "أحمر", "اخضر", "أخضر", "ازرق", "أزرق", "اصفر", "أصفر", "اسود", "أسود", 
            "ابيض", "أبيض", "رمادي", "بني", "برتقالي", "وردي", "بنفسجي", "ذهبي", "فضي", "بيج", 
            "عنابي", "نيلي", "فيروزي", "زيتوني", "ليموني", "خوخي", "كحلي", "بنفسجي", "فوشي", "تركواز",
            "بيج", "كريمي", "عاجي", "نحاسي", "برونزي", "نحاسي", "فضي", "ذهبي", "لؤلؤي", "ياقوتي"
        };
        for (String a : colors) ADJECTIVES.put(a, WordCategory.ADJECTIVE_COLOR);
        
        // الأحجام والأبعاد
        String[] sizes = {
            "كبير", "صغير", "طويل", "قصير", "عريض", "ضيق", "سميك", "رفيع", "عميق", "ضحل",
            "ضخم", "هائل", "ضئيل", "واسع", "محدود", "ضيق", "فسيح", "موسع", "مضغوط", "متراص",
            "طويل", "قصير", "ممتد", "متواضع", "ضخم", "عملاق", "صغير", "صغير", "صغير", "صغير"
        };
        for (String a : sizes) ADJECTIVES.put(a, WordCategory.ADJECTIVE_SIZE);
        
        // الصفات المعنوية
        String[] qualities = {
            "جميل", "قبيح", "نظيف", "قذر", "سريع", "بطيء", "قوي", "ضعيف", "سعيد", "حزين",
            "ذكي", "غبي", "شجاع", "جبان", "لطيف", "فظ", "هادئ", "صاخب", "ناعم", "خشن",
            "لذيذ", "مر", "حلو", "مالح", "حامض", "حار", "بارد", "دافئ", "رطب", "جاف",
            "طازج", "عفن", "جديد", "قديم", "حديث", "عتيق", "اصلي", "أصلي", "مزيف", "نادر", "شائع",
            "ممتاز", "سيء", "جيد", "رديء", "مثالي", "كامل", "ناقص", "تام", "ناقص", "مثالي"
        };
        for (String a : qualities) ADJECTIVES.put(a, WordCategory.ADJECTIVE_QUALITY);
        
        // الأعداد الترتيبية
        String[] numbers = {
            "اول", "أول", "ثان", "ثاني", "ثالث", "رابع", "خامس", "سادس", "سابع", "ثامن", 
            "تاسع", "عاشر", "احدى عشر", "أحدى عشر", "اثنا عشر", "أثنا عشر", "اخر", "أخر", "سابق", "لاحق", 
            "مقبل", "متأخر", "باكر", "مبكر", "اولى", "ثانية", "ثالثة", "رابعة", "خامسة", "سادسة"
        };
        for (String a : numbers) ADJECTIVES.put(a, WordCategory.ADJECTIVE_NUMBER);
        
        // درجات الحرارة
        String[] temps = {
            "ساخن", "بارد", "دافئ", "فاتر", "مغلي", "متجمد", "حار", "بارد", "معتدل", "رطب",
            "جاف", "رطب", "مشمس", "غائم", "ماطر", "ثلجي", "عاصف", "هادئ", "ريحي", "رطب"
        };
        for (String a : temps) ADJECTIVES.put(a, WordCategory.ADJECTIVE_TEMPERATURE);
        
        // الأشكال
        String[] shapes = {
            "مربع", "مستطيل", "مثلث", "دائري", "بيضاوي", "مستدير", "مستطيل", "مكعب", "اسطواني", "هرمي",
            "مخروطي", "كروي", "مسطح", "محور", "متماثل", "غير", "منتظم", "منتظم", "متوازي", "منحني"
        };
        for (String a : shapes) ADJECTIVES.put(a, WordCategory.ADJECTIVE_SHAPE);
        
        // الملمس
        String[] textures = {
            "ناعم", "خشن", "صلب", "طري", "هش", "متين", "قوي", "ضعيف", "مطاطي", "بلاستيكي",
            "معدني", "خشبي", "حجري", "زجاجي", "ورقي", "قماشي", "صوفي", "قطني", "حريري", "صخري"
        };
        for (String a : textures) ADJECTIVES.put(a, WordCategory.ADJECTIVE_TEXTURE);
        
        // الأصوات
        String[] sounds = {
            "عالي", "منخفض", "صاخب", "هادئ", "نغمي", "موسيقي", "لحني", "إيقاعي", "رنين", "صدى",
            "جهير", "حاد", "ناعم", "عذب", "بشع", "مزعج", "مريح", "مسموع", "خافت", "صامت"
        };
        for (String a : sounds) ADJECTIVES.put(a, WordCategory.ADJECTIVE_SOUND);
        
        // الطعم
        String[] tastes = {
            "حلو", "حامض", "مالح", "مر", "لذيذ", "شهي", "فاسد", "طازج", "حار", "بارد",
            "فلفلي", "بهاري", "منكه", "بشع", "لذيذ", "شهي", "لذيذ", "شهي", "لذيذ", "شهي"
        };
        for (String a : tastes) ADJECTIVES.put(a, WordCategory.ADJECTIVE_TASTE);
        
        // الرائحة
        String[] smells = {
            "عطرة", "كريهة", "منعشة", "حادة", "خفيفة", "ثقيلة", "زكية", "فواحة", "نتنة", "عفنة",
            "زهرية", "عشبية", "ترابية", "بحرية", "مطيرية", "صيفية", "شتائية", "ربيعية", "خريفية", "عطرية"
        };
        for (String a : smells) ADJECTIVES.put(a, WordCategory.ADJECTIVE_SMELL);
        
        // السرعة
        String[] speeds = {
            "سريع", "بطيء", "متوسط", "خاطف", "فوري", "لحظي", "تدريجي", "مفاجئ", "مستمر", "متقطع",
            "ثابت", "متغير", "متسارع", "متباطئ", "راكد", "جامد", "متحرك", "ديناميكي", "ساكن", "هادئ"
        };
        for (String a : speeds) ADJECTIVES.put(a, WordCategory.ADJECTIVE_SPEED);
        
        // الذكاء
        String[] intelligence = {
            "ذكي", "غبي", "عبقري", "متوسط", "فطن", "بليد", "حصيف", "حكيم", "عاقل", "مجنون",
            "عقلاني", "عاطفي", "منطقي", "بديهي", "معقد", "بسيط", "عميق", "سطحي", "تحليلي", "تركيبي"
        };
        for (String a : intelligence) ADJECTIVES.put(a, WordCategory.ADJECTIVE_INTELLIGENCE);
        
        // الأخلاق
        String[] moral = {
            "صادق", "كاذب", "امين", "أمين", "خائن", "عادل", "ظالم", "كريم", "بخيل", "شجاع",
            "جبان", "صبور", "عصبي", "حليم", "حكيم", "سفيه", "عفيف", "فاجر", "تقي", "فاسق"
        };
        for (String a : moral) ADJECTIVES.put(a, WordCategory.ADJECTIVE_MORAL);
        
        // العاطفية
        String[] emotional = {
            "فرحان", "حزين", "غاضب", "خائف", "قلق", "متوتر", "هادئ", "مسترخي", "متحمس", "محبط",
            "متفائل", "متشائم", "راضي", "ساخط", "مندهش", "مصدوم", "محرج", "خجول", "جريء", "خائف"
        };
        for (String a : emotional) ADJECTIVES.put(a, WordCategory.ADJECTIVE_EMOTIONAL);
    }
    
    // الأعداد
    private static final Map<String, WordCategory> NUMBERS = new HashMap<>();
    static {
        String[] units = {"صفر", "واحد", "اثنان", "ثلاثة", "اربعة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة"};
        String[] teens = {"عشرة", "احدى عشر", "أحدى عشر", "اثنا عشر", "أثنا عشر", "ثلاثة عشر", "اربعة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"};
        String[] tens = {"عشرون", "ثلاثون", "اربعون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"};
        String[] hundreds = {"مئة", "مئتان", "ثلاثمئة", "اربعمئة", "أربعمئة", "خمسمئة", "ستمئة", "سبعمئة", "ثمانمئة", "تسعمئة"};
        
        for (String n : units) NUMBERS.put(n, WordCategory.NUMBER_CARDINAL);
        for (String n : teens) NUMBERS.put(n, WordCategory.NUMBER_CARDINAL);
        for (String n : tens) NUMBERS.put(n, WordCategory.NUMBER_CARDINAL);
        for (String n : hundreds) NUMBERS.put(n, WordCategory.NUMBER_CARDINAL);
        
        NUMBERS.put("الف", WordCategory.NUMBER_CARDINAL);
        NUMBERS.put("ألف", WordCategory.NUMBER_CARDINAL);
        NUMBERS.put("مليون", WordCategory.NUMBER_CARDINAL);
        NUMBERS.put("مليار", WordCategory.NUMBER_CARDINAL);
        NUMBERS.put("ترليون", WordCategory.NUMBER_CARDINAL);
        NUMBERS.put("تريليون", WordCategory.NUMBER_CARDINAL);
        
        // الأعداد الترتيبية
        String[] ordinals = {"اول", "أول", "ثان", "ثاني", "ثالث", "رابع", "خامس", "سادس", "سابع", "ثامن", "تاسع", "عاشر"};
        for (String n : ordinals) NUMBERS.put(n, WordCategory.NUMBER_ORDINAL);
        
        // الكسور
        String[] fractions = {"نصف", "ثلث", "ربع", "خمس", "سدس", "سبع", "ثمن", "تسع", "عشر", "ثلاثة ارباع"};
        for (String n : fractions) NUMBERS.put(n, WordCategory.NUMBER_FRACTION);
        
        // التوكيدية
        String[] multiplicatives = {"مرة", "مرتان", "ثلاث", "اربع", "خمس"};
        for (String n : multiplicatives) NUMBERS.put(n, WordCategory.NUMBER_MULTIPLICATIVE);
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
        PARTICLES.put("ان", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("أن", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("لن", WordCategory.PARTICLE_NEGATION);
        PARTICLES.put("لم", WordCategory.PARTICLE_NEGATION);
        
        PARTICLES.put("س", WordCategory.PARTICLE_FUTURE);
        PARTICLES.put("سوف", WordCategory.PARTICLE_FUTURE);
        PARTICLES.put("سوف", WordCategory.PARTICLE_FUTURE);
        
        PARTICLES.put("ن", WordCategory.PARTICLE_EMPHASIS);
        PARTICLES.put("ان", WordCategory.PARTICLE_EMPHASIS);
        PARTICLES.put("أن", WordCategory.PARTICLE_EMPHASIS);
        PARTICLES.put("قد", WordCategory.PARTICLE_EMPHASIS);
        PARTICLES.put("لقد", WordCategory.PARTICLE_EMPHASIS);
        PARTICLES.put("ان", WordCategory.PARTICLE_EMPHASIS);
        
        PARTICLES.put("هل", WordCategory.PARTICLE_INTERROGATION);
        PARTICLES.put("من", WordCategory.PARTICLE_INTERROGATION);
        PARTICLES.put("ما", WordCategory.PARTICLE_INTERROGATION);
        PARTICLES.put("اين", WordCategory.PARTICLE_INTERROGATION);
        PARTICLES.put("أين", WordCategory.PARTICLE_INTERROGATION);
        PARTICLES.put("متى", WordCategory.PARTICLE_INTERROGATION);
        PARTICLES.put("كيف", WordCategory.PARTICLE_INTERROGATION);
        PARTICLES.put("لماذا", WordCategory.PARTICLE_INTERROGATION);
        
        PARTICLES.put("لا", WordCategory.PARTICLE_PROHIBITION);
        PARTICLES.put("لا", WordCategory.PARTICLE_PROHIBITION);
        
        PARTICLES.put("الا", WordCategory.PARTICLE_EXCEPTION);
        PARTICLES.put("إلا", WordCategory.PARTICLE_EXCEPTION);
        PARTICLES.put("غير", WordCategory.PARTICLE_EXCEPTION);
        PARTICLES.put("سوى", WordCategory.PARTICLE_EXCEPTION);
        
        PARTICLES.put("لان", WordCategory.PARTICLE_CAUSATION);
        PARTICLES.put("لأن", WordCategory.PARTICLE_CAUSATION);
        PARTICLES.put("كي", WordCategory.PARTICLE_CAUSATION);
        PARTICLES.put("حتى", WordCategory.PARTICLE_CAUSATION);
        PARTICLES.put("ل", WordCategory.PARTICLE_CAUSATION);
        
        PARTICLES.put("ف", WordCategory.PARTICLE_RESUMPTION);
        PARTICLES.put("ثم", WordCategory.PARTICLE_RESUMPTION);
        PARTICLES.put("و", WordCategory.PARTICLE_RESUMPTION);
    }
    
    // أدوات النداء
    private static final Map<String, WordCategory> VOCATIVES = new HashMap<>();
    static {
        VOCATIVES.put("يا", WordCategory.VOCATIVE_PARTICLE);
        VOCATIVES.put("ا", WordCategory.VOCATIVE_PARTICLE);
        VOCATIVES.put("أ", WordCategory.VOCATIVE_PARTICLE);
        VOCATIVES.put("هيا", WordCategory.VOCATIVE_PARTICLE);
        VOCATIVES.put("يا", WordCategory.VOCATIVE_PARTICLE);
        VOCATIVES.put("أيها", WordCategory.VOCATIVE_PARTICLE);
        VOCATIVES.put("أيتها", WordCategory.VOCATIVE_PARTICLE);
        VOCATIVES.put("وا", WordCategory.VOCATIVE_PARTICLE);
    }
    
    // أدوات التعجب
    private static final Map<String, WordCategory> INTERJECTIONS = new HashMap<>();
    static {
        INTERJECTIONS.put("ما", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("ما", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("ها", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("ها", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("هيهات", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("وا", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("وا", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("اف", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("أف", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("واح", WordCategory.INTERJECTION_PARTICLE);
        INTERJECTIONS.put("وا", WordCategory.INTERJECTION_PARTICLE);
    }
    
    // الظروف
    private static final Map<String, WordCategory> ADVERBS = new HashMap<>();
    static {
        // ظروف الزمان
        String[] timeAdverbs = {"الان", "الآن", "اليوم", "غدا", "امس", "أمس", "صباح", "مساء", "ليلا", "نهارا",
            "دائما", "ابدا", "أبدا", "احيانا", "أحيانا", "غالبا", "نادرا", "كثيرا", "قليلا", "مؤخرا",
            "سابقا", "لاحقا", "قريبا", "بعيدا", "فورا", "حالا", "لحظيا", "سريعا", "بطيئا", "تدريجيا"};
        for (String a : timeAdverbs) ADVERBS.put(a, WordCategory.ADVERB_TIME);
        
        // ظروف المكان
        String[] placeAdverbs = {"هنا", "هناك", "امام", "أمام", "خلف", "فوق", "تحت", "يمين", "شمال", "شرق",
            "غرب", "داخل", "خارج", "وسط", "جانب", "قرب", "بعيد", "قريب", "فوق", "تحت"};
        for (String a : placeAdverbs) ADVERBS.put(a, WordCategory.ADVERB_PLACE);
        
        // ظروف الحال
        String[] mannerAdverbs = {"بسرعة", "ببطء", "بهدوء", "بصوت", "عاليا", "منخفضا", "جيدا", "سيئا", "بشكل", "طريقة",
            "جيدة", "جميلة", "قبيحة", "سريعة", "بطيئة", "هادئة", "صاخبة", "ناعمة", "خشنة"};
        for (String a : mannerAdverbs) ADVERBS.put(a, WordCategory.ADVERB_MANNER);
        
        // ظروف الدرجة
        String[] degreeAdverbs = {"جدا", "كثيرا", "قليلا", "نوعا", "ما", "تماما", "كليا", "جزئيا", "نسبيا", "مطلقا",
            "تقريبا", "حوالي", "نحو", "اكثر", "أكثر", "اقل", "أقل", "اكبر", "أكبر", "اصغر"};
        for (String a : degreeAdverbs) ADVERBS.put(a, WordCategory.ADVERB_DEGREE);
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
        
        public boolean isQuestion() {
            return words.stream().anyMatch(w -> 
                w.getCategory() == WordCategory.QUESTION_TOOL ||
                w.getCategory() == WordCategory.PARTICLE_INTERROGATION
            );
        }
        
        public List<String> getNouns() {
            return words.stream()
                .filter(w -> w.getCategory().name().contains("NOUN"))
                .map(WordAnalysis::getNormalizedWord)
                .collect(Collectors.toList());
        }
        
        public List<String> getVerbs() {
            return words.stream()
                .filter(w -> w.getCategory().name().contains("VERB"))
                .map(WordAnalysis::getNormalizedWord)
                .collect(Collectors.toList());
        }
        
        public List<String> getAdjectives() {
            return words.stream()
                .filter(w -> w.getCategory().name().contains("ADJECTIVE"))
                .map(WordAnalysis::getNormalizedWord)
                .collect(Collectors.toList());
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
        if (VOCATIVES.containsKey(word)) return VOCATIVES.get(word);
        if (INTERJECTIONS.containsKey(word)) return INTERJECTIONS.get(word);
        if (ADVERBS.containsKey(word)) return ADVERBS.get(word);
        return WordCategory.UNKNOWN;
    }
    
    /**
     * استنتاج التصنيف من أنماط الكلمة
     */
    private static WordCategory inferCategory(String word) {
        // التحقق من أنماط الأفعال المضارعة
        if (word.startsWith("ي") && word.length() > 3) {
            return WordCategory.VERB_PRESENT;
        }
        if (word.startsWith("ت") && word.length() > 3) {
            return WordCategory.VERB_PRESENT;
        }
        if (word.startsWith("ا") && word.length() > 3 && !word.startsWith("ال")) {
            return WordCategory.VERB_IMPERATIVE;
        }
        if (word.startsWith("ن") && word.length() > 3) {
            return WordCategory.VERB_PRESENT;
        }
        
        // التحقق من الجمع
        if (word.endsWith("ون") || word.endsWith("ين") || word.endsWith("ات") || word.endsWith("وا")) {
            return WordCategory.NOUN_COLLECTIVE;
        }
        
        // التحقق من التصغير
        if (word.matches("^.ُ.َيْ.ِ.$")) {
            return WordCategory.NOUN_OBJECT;
        }
        
        // التحقق من اسم الآلة
        if (word.startsWith("مِ") || word.matches("^مِ.{2,4}$")) {
            return WordCategory.NOUN_INSTRUMENT;
        }
        
        // التحقق من اسم المكان
        if (word.startsWith("م") && word.length() > 4) {
            return WordCategory.NOUN_PLACE;
        }
        
        // التحقق من اسم الزمان
        if (word.startsWith("س") && word.length() > 3) {
            return WordCategory.NOUN_TIME;
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
            .replaceAll("^(ال|وال|فال|بال|كال|لل|وال|فال|بال|كال|لل)", "")
            .replaceAll("(ة|ات|ون|ين|ان|ت|ن|ي|ا|و|وا|ون|ين)$", "");
        
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
            
            if (c == '.' || c == '!' || c == '؟' || c == '?' || c == '!' || c == '؛') {
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
        stats.put("نداء", VOCATIVES.size());
        stats.put("تعجب", INTERJECTIONS.size());
        stats.put("ظروف", ADVERBS.size());
        
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

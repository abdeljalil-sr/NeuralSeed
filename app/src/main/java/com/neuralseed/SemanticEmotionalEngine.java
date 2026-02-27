package com.neuralseed;

import java.util.*;

/**
 * محرك المعاني والعواطف - يربط الكلمات بالمعاني والمشاعر
 */
public class SemanticEmotionalEngine {
    
    // تمثيل المعنى
    public static class Meaning {
        String concept;           // المفهوم
        String definition;        // التعريف
        List<String> synonyms;    // المرادفات
        List<String> antonyms;    // الأضداد
        List<String> examples;    // الأمثلة
        Map<String, Double> relatedConcepts; // المفاهيم المرتبطة
        
        public Meaning(String concept, String definition) {
            this.concept = concept;
            this.definition = definition;
            this.synonyms = new ArrayList<>();
            this.antonyms = new ArrayList<>();
            this.examples = new ArrayList<>();
            this.relatedConcepts = new HashMap<>();
        }
    }
    
    // تمثيل العاطفة
    public static class Emotion {
        String name;              // اسم العاطفة
        String arabicName;        // الاسم العربي
        double intensity;         // الشدة (0-1)
        double valence;           // الإيجابية/السلبية (-1 إلى 1)
        double arousal;           // التنشيط (0-1)
        List<String> triggers;    // المحفزات
        List<String> expressions; // التعبيرات
        int color;                // اللون المرتبط
        
        public Emotion(String name, String arabicName) {
            this.name = name;
            this.arabicName = arabicName;
            this.intensity = 0.5;
            this.valence = 0.0;
            this.arousal = 0.5;
            this.triggers = new ArrayList<>();
            this.expressions = new ArrayList<>();
        }
    }
    
    // ربط كلمة-معنى-عاطفة
    public static class SemanticLink {
        String word;
        Meaning meaning;
        Map<String, Double> emotionWeights; // وزن كل عاطفة
        double strength;          // قوة الربط
        long createdAt;
        long lastAccessed;
        int accessCount;
        
        public SemanticLink(String word, Meaning meaning) {
            this.word = word;
            this.meaning = meaning;
            this.emotionWeights = new HashMap<>();
            this.strength = 0.5;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessed = createdAt;
            this.accessCount = 0;
        }
        
        public void access() {
            accessCount++;
            lastAccessed = System.currentTimeMillis();
            strength = Math.min(1.0, strength + 0.01);
        }
    }
    
    // قاعدة عاطفية
    public static class EmotionRule {
        String trigger;           // المحفز
        String emotion;           // العاطفة الناتجة
        double intensity;         // الشدة
        String condition;         // الشرط
        
        public EmotionRule(String trigger, String emotion, double intensity) {
            this.trigger = trigger;
            this.emotion = emotion;
            this.intensity = intensity;
        }
    }
    
    // البيانات
    private Map<String, Meaning> meanings;
    private Map<String, Emotion> emotions;
    private Map<String, List<SemanticLink>> wordLinks;
    private List<EmotionRule> emotionRules;
    
    public SemanticEmotionalEngine() {
        this.meanings = new HashMap<>();
        this.emotions = new HashMap<>();
        this.wordLinks = new HashMap<>();
        this.emotionRules = new ArrayList<>();
        
        initializeEmotions();
        initializeMeanings();
        initializeEmotionRules();
    }
    
    private void initializeEmotions() {
        // العواطف الأساسية
        Emotion joy = new Emotion("joy", "فرح");
        joy.valence = 1.0;
        joy.arousal = 0.8;
        joy.color = 0xFFFFD700; // ذهبي
        joy.triggers.addAll(Arrays.asList("نجاح", "فوز", "حب", "هدايا"));
        joy.expressions.addAll(Arrays.asList("ابتسامة", "ضحك", "قفز", "تهليل"));
        emotions.put("joy", joy);
        
        Emotion sadness = new Emotion("sadness", "حزن");
        sadness.valence = -0.8;
        sadness.arousal = 0.3;
        sadness.color = 0xFF4682B4; // أزرق فولاذي
        sadness.triggers.addAll(Arrays.asList("فقدان", "فشل", "وداع", "خيبة"));
        sadness.expressions.addAll(Arrays.asList("بكاء", "انحناء", "صمت", "نظرة حزينة"));
        emotions.put("sadness", sadness);
        
        Emotion anger = new Emotion("anger", "غضب");
        anger.valence = -0.9;
        anger.arousal = 0.9;
        anger.color = 0xFFFF4500; // أحمر برتقالي
        anger.triggers.addAll(Arrays.asList("ظلم", "إهانة", "خيانة", "عرقلة"));
        anger.expressions.addAll(Arrays.asList("صراخ", "عصبية", "نظرات حادة", "رفض"));
        emotions.put("anger", anger);
        
        Emotion fear = new Emotion("fear", "خوف");
        fear.valence = -0.8;
        fear.arousal = 0.9;
        fear.color = 0xFF8B0000; // أحمر داكن
        fear.triggers.addAll(Arrays.asList("خطر", "تهديد", "مجهول", "وحدة"));
        fear.expressions.addAll(Arrays.asList("ارتعاش", "هروب", "تجمد", "عيون واسعة"));
        emotions.put("fear", fear);
        
        Emotion love = new Emotion("love", "حب");
        love.valence = 1.0;
        love.arousal = 0.7;
        love.color = 0xFFFF69B4; // وردي
        love.triggers.addAll(Arrays.asList("جمال", "لطف", "قرب", "تضحية"));
        love.expressions.addAll(Arrays.asList("عناق", "ابتسامة حنونة", "اهتمام", "عطاء"));
        emotions.put("love", love);
        
        Emotion curiosity = new Emotion("curiosity", "فضول");
        curiosity.valence = 0.5;
        curiosity.arousal = 0.7;
        curiosity.color = 0xFF4169E1; // أزرق ملكي
        curiosity.triggers.addAll(Arrays.asList("جديد", "غامض", "سؤال", "اكتشاف"));
        curiosity.expressions.addAll(Arrays.asList("سؤال", "بحث", "تأمل", "تجربة"));
        emotions.put("curiosity", curiosity);
        
        Emotion surprise = new Emotion("surprise", "مفاجأة");
        surprise.valence = 0.0;
        surprise.arousal = 0.9;
        surprise.color = 0xFFFFA500; // برتقالي
        surprise.triggers.addAll(Arrays.asList("غير متوقع", "مفاجأة", "صدفة"));
        surprise.expressions.addAll(Arrays.asList("دهشة", "تعجب", "فم مفتوح"));
        emotions.put("surprise", surprise);
        
        Emotion disgust = new Emotion("disgust", "اشمئزاز");
        disgust.valence = -0.9;
        disgust.arousal = 0.5;
        disgust.color = 0xFF556B2F; // أخضر زيتوني داكن
        disgust.triggers.addAll(Arrays.asList("قذارة", "خيانة", "فساد", "قبح"));
        disgust.expressions.addAll(Arrays.asList("تجهم", "ابتعاد", "رفض"));
        emotions.put("disgust", disgust);
        
        Emotion trust = new Emotion("trust", "ثقة");
        trust.valence = 0.8;
        trust.arousal = 0.4;
        trust.color = 0xFF90EE90; // أخضر فاتح
        trust.triggers.addAll(Arrays.asList("صدق", "أمانة", "وفاء", "استقرار"));
        trust.expressions.addAll(Arrays.asList("استرخاء", "انفتاح", "مشاركة"));
        emotions.put("trust", trust);
        
        Emotion anticipation = new Emotion("anticipation", "ترقب");
        anticipation.valence = 0.3;
        anticipation.arousal = 0.7;
        anticipation.color = 0xFFFFD700; // ذهبي
        anticipation.triggers.addAll(Arrays.asList("انتظار", "توقع", "أمل"));
        anticipation.expressions.addAll(Arrays.asList("ترقب", "تأهب", "تطلع"));
        emotions.put("anticipation", anticipation);
        
        // عواطف مركبة
        Emotion optimism = new Emotion("optimism", "تفاؤل");
        optimism.valence = 0.9;
        optimism.arousal = 0.6;
        optimism.color = 0xFFFFD700;
        emotions.put("optimism", optimism);
        
        Emotion hope = new Emotion("hope", "أمل");
        hope.valence = 0.8;
        hope.arousal = 0.5;
        hope.color = 0xFF00CED1;
        emotions.put("hope", hope);
        
        Emotion anxiety = new Emotion("anxiety", "قلق");
        anxiety.valence = -0.6;
        anxiety.arousal = 0.8;
        anxiety.color = 0xFF9370DB;
        emotions.put("anxiety", anxiety);
        
        Emotion pride = new Emotion("pride", "فخر");
        pride.valence = 0.7;
        pride.arousal = 0.7;
        pride.color = 0xFF800080;
        emotions.put("pride", pride);
        
        Emotion shame = new Emotion("shame", "خجل");
        shame.valence = -0.7;
        shame.arousal = 0.5;
        shame.color = 0xFFCD5C5C;
        emotions.put("shame", shame);
        
        Emotion gratitude = new Emotion("gratitude", "امتنان");
        gratitude.valence = 0.9;
        gratitude.arousal = 0.5;
        gratitude.color = 0xFF32CD32;
        emotions.put("gratitude", gratitude);
        
        Emotion compassion = new Emotion("compassion", "رحمة");
        compassion.valence = 0.7;
        compassion.arousal = 0.4;
        compassion.color = 0xFFFF69B4;
        emotions.put("compassion", compassion);
        
        Emotion loneliness = new Emotion("loneliness", "وحدة");
        loneliness.valence = -0.7;
        loneliness.arousal = 0.3;
        loneliness.color = 0xFF4682B4;
        emotions.put("loneliness", loneliness);
        
        Emotion excitement = new Emotion("excitement", "إثارة");
        excitement.valence = 0.8;
        excitement.arousal = 0.9;
        excitement.color = 0xFFFF6347;
        emotions.put("excitement", excitement);
        
        Emotion contentment = new Emotion("contentment", "رضا");
        contentment.valence = 0.8;
        contentment.arousal = 0.3;
        contentment.color = 0xFF98FB98;
        emotions.put("contentment", contentment);
        
        Emotion boredom = new Emotion("boredom", "ملل");
        boredom.valence = -0.5;
        boredom.arousal = 0.2;
        boredom.color = 0xFFA9A9A9;
        emotions.put("boredom", boredom);
        
        Emotion confusion = new Emotion("confusion", "ارتباك");
        confusion.valence = -0.4;
        confusion.arousal = 0.6;
        confusion.color = 0xFFDDA0DD;
        emotions.put("confusion", confusion);
        
        Emotion confidence = new Emotion("confidence", "ثقة بالنفس");
        confidence.valence = 0.8;
        confidence.arousal = 0.6;
        confidence.color = 0xFF4169E1;
        emotions.put("confidence", confidence);
        
        Emotion envy = new Emotion("envy", "حسد");
        envy.valence = -0.7;
        envy.arousal = 0.6;
        envy.color = 0xFF006400;
        emotions.put("envy", envy);
        
        Emotion jealousy = new Emotion("jealousy", "غيرة");
        jealousy.valence = -0.6;
        jealousy.arousal = 0.8;
        jealousy.color = 0xFF228B22;
        emotions.put("jealousy", jealousy);
        
        Emotion relief = new Emotion("relief", "ارتياح");
        relief.valence = 0.7;
        relief.arousal = 0.4;
        relief.color = 0xFF87CEEB;
        emotions.put("relief", relief);
        
        Emotion disappointment = new Emotion("disappointment", "خيبة أمل");
        disappointment.valence = -0.7;
        disappointment.arousal = 0.4;
        disappointment.color = 0xFF708090;
        emotions.put("disappointment", disappointment);
        
        Emotion nostalgia = new Emotion("nostalgia", "حنين");
        nostalgia.valence = 0.3;
        nostalgia.arousal = 0.4;
        nostalgia.color = 0xFFD2B48C;
        emotions.put("nostalgia", nostalgia);
        
        Emotion awe = new Emotion("awe", "دهشة");
        awe.valence = 0.6;
        awe.arousal = 0.8;
        awe.color = 0xFF4B0082;
        emotions.put("awe", awe);
    }
    
    private void initializeMeanings() {
        // معاني المشاعر الرئيسية
        addMeaning("حب", "شعور عميق بالانجذاب والاهتمام والتضحية", 
                  Arrays.asList("عاطفة", "مشاعر", "علاقة"),
                  Arrays.asList("كراهية", "بغض"),
                  Map.of("joy", 0.9, "love", 1.0, "trust", 0.8));
        
        addMeaning("فرح", "شعور بالسرور والبهجة والارتياح", 
                  Arrays.asList("سعادة", "بهجة", "سرور"),
                  Arrays.asList("حزن", "كآبة"),
                  Map.of("joy", 1.0, "excitement", 0.7));
        
        addMeaning("حزن", "شعور بالأسى والكآبة والألم النفسي", 
                  Arrays.asList("أسى", "كآبة", "ألم"),
                  Arrays.asList("فرح", "سعادة"),
                  Map.of("sadness", 1.0, "loneliness", 0.6));
        
        addMeaning("خوف", "شعور بالقلق والفزع من خطر محتمل", 
                  Arrays.asList("فزع", "رهبة", "قلق"),
                  Arrays.asList("شجاعة", "أمان"),
                  Map.of("fear", 1.0, "anxiety", 0.8));
        
        addMeaning("غضب", "شعور بالانزعاج والسخط الشديد", 
                  Arrays.asList("سخط", "حنق", "انزعاج"),
                  Arrays.asList("هدوء", "رضا"),
                  Map.of("anger", 1.0, "disgust", 0.5));
        
        addMeaning("أمل", "توقع خير وتحقق المراد", 
                  Arrays.asList("تفاؤل", "تطلع", "رجاء"),
                  Arrays.asList("يأس", "قنوط"),
                  Map.of("hope", 1.0, "optimism", 0.9));
        
        addMeaning("سلام", "حالة من الطمأنينة والاستقرار", 
                  Arrays.asList("هدوء", "أمان", "استقرار"),
                  Arrays.asList("حرب", "قلق"),
                  Map.of("contentment", 0.9, "trust", 0.8));
        
        addMeaning("سعادة", "حالة من الرضا التام والبهجة الدائمة", 
                  Arrays.asList("هناء", "سرور", "رضا"),
                  Arrays.asList("شقاء", "تعاسة"),
                  Map.of("joy", 1.0, "contentment", 0.9, "love", 0.7));
        
        addMeaning("شجاعة", "الإقدام على فعل الصواب رغم الخوف", 
                  Arrays.asList("إقدام", "بسالة", "شهامة"),
                  Arrays.asList("جبن", "خوف"),
                  Map.of("confidence", 0.9, "pride", 0.7));
        
        addMeaning("صدق", "الالتزام بالحق والواقع", 
                  Arrays.asList("أمانة", "صراحة", "حق"),
                  Arrays.asList("كذب", "خداع"),
                  Map.of("trust", 0.9, "pride", 0.6));
        
        addMeaning("عدل", "إعطاء كل ذي حق حقه", 
                  Arrays.asList("إنصاف", "حق", "مساواة"),
                  Arrays.asList("ظلم", "جور"),
                  Map.of("trust", 0.8, "contentment", 0.7));
        
        addMeaning("كرم", "العطاء بلا حدود ولا انتظار للمقابل", 
                  Arrays.asList("جود", "سخاء", "عطاء"),
                  Arrays.asList("بخل", "شح"),
                  Map.of("joy", 0.7, "love", 0.6));
        
        addMeaning("صبر", "التحمل والثبات رغم الصعوبات", 
                  Arrays.asList("تحمل", "ثبات", "جلد"),
                  Arrays.asList("يأس", "استسلام"),
                  Map.of("contentment", 0.8, "hope", 0.7));
        
        addMeaning("علم", "المعرفة والإدراك والفهم", 
                  Arrays.asList("معرفة", "فهم", "حكمة"),
                  Arrays.asList("جهل", "غباء"),
                  Map.of("curiosity", 0.9, "confidence", 0.7));
        
        addMeaning("جمال", "الكمال في الصورة والمعنى", 
                  Arrays.asList("حسن", "روعة", "بهاء"),
                  Arrays.asList("قبح", "رداءة"),
                  Map.of("joy", 0.8, "love", 0.6, "awe", 0.7));
        
        addMeaning("قوة", "القدرة على التأثير والمقاومة", 
                  Arrays.asList("قدرة", "شدة", "صلابة"),
                  Arrays.asList("ضعف", "وهن"),
                  Map.of("confidence", 0.9, "pride", 0.7));
        
        addMeaning("ضعف", "نقص القدرة والقدرة على المقاومة", 
                  Arrays.asList("وهن", "عجز", "قلة"),
                  Arrays.asList("قوة", "قدرة"),
                  Map.of("sadness", 0.6, "fear", 0.5));
        
        addMeaning("حلم", "الرؤيا الذهنية أو الأمل في المستقبل", 
                  Arrays.asList("رؤيا", "أمل", "تطلع"),
                  Arrays.asList("كابوس", "واقع"),
                  Map.of("hope", 0.8, "curiosity", 0.6));
        
        addMeaning("هدف", "الغاية والمقصد من الفعل", 
                  Arrays.asList("غاية", "مقصد", "مراد"),
                  Arrays.asList("عشوائية", "ضياع"),
                  Map.of("anticipation", 0.8, "hope", 0.7));
        
        addMeaning("نجاح", "تحقيق المراد والوصول للغاية", 
                  Arrays.asList("فوز", "تفوق", "إنجاز"),
                  Arrays.asList("فشل", "خسارة"),
                  Map.of("joy", 0.9, "pride", 0.9, "excitement", 0.8));
        
        addMeaning("فشل", "عدم تحقيق المراد والقصور عن الهدف", 
                  Arrays.asList("خسارة", "قصور", "إخفاق"),
                  Arrays.asList("نجاح", "فوز"),
                  Map.of("sadness", 0.8, "disappointment", 0.9));
        
        addMeaning("حياة", "حالة الوجود والنشاط والنمو", 
                  Arrays.asList("وجود", "نشاط", "حيوية"),
                  Arrays.asList("موت", "فناء"),
                  Map.of("joy", 0.8, "hope", 0.9, "love", 0.7));
        
        addMeaning("موت", "انتهاء الحياة والوجود", 
                  Arrays.asList("فناء", "رحيل", "انتهاء"),
                  Arrays.asList("حياة", "وجود"),
                  Map.of("fear", 0.8, "sadness", 0.9));
        
        addMeaning("نور", "الضوء والإشراق والهداية", 
                  Arrays.asList("ضياء", "إشراق", "هداية"),
                  Arrays.asList("ظلام", "عمى"),
                  Map.of("hope", 0.9, "joy", 0.7, "awe", 0.8));
        
        addMeaning("ظلام", "غياب النور والضوء", 
                  Arrays.asList("عتمة", "غموض", "خوف"),
                  Arrays.asList("نور", "ضياء"),
                  Map.of("fear", 0.7, "sadness", 0.5));
        
        addMeaning("وقت", "الزمن والمدة واللحظات", 
                  Arrays.asList("زمن", "مدة", "عمر"),
                  Arrays.asList("أزلية", "خلود"),
                  Map.of("anticipation", 0.6, "nostalgia", 0.5));
        
        addMeaning("عمر", "مدة الحياة والوجود", 
                  Arrays.asList("حياة", "عمر", "سنين"),
                  Arrays.asList("موت", "فناء"),
                  Map.of("hope", 0.7, "nostalgia", 0.6));
        
        addMeaning("صداقة", "علاقة مبنية على المودة والاحترام المتبادل", 
                  Arrays.asList("رفقة", "مودة", "أخوة"),
                  Arrays.asList("عداوة", "بغضاء"),
                  Map.of("joy", 0.7, "trust", 0.9, "love", 0.6));
        
        addMeaning("عداوة", "علاقة مبنية على البغض والخصومة", 
                  Arrays.asList("بغضاء", "خصومة", "حقد"),
                  Arrays.asList("صداقة", "مودة"),
                  Map.of("anger", 0.8, "disgust", 0.7));
        
        addMeaning("عائلة", "جماعة الأقارب والأهل", 
                  Arrays.asList("أسرة", "أهل", "قرابة"),
                  Arrays.asList("غربة", "وحدة"),
                  Map.of("love", 0.9, "trust", 0.8, "contentment", 0.7));
        
        addMeaning("وحدة", "حالة الانعزال والانفراد", 
                  Arrays.asList("عزلة", "انفراد", "غربة"),
                  Arrays.asList("جمع", "صحبة"),
                  Map.of("loneliness", 0.9, "sadness", 0.7));
        
        addMeaning("حرية", "القدرة على الاختيار والفعل دون قيود", 
                  Arrays.asList("استقلال", "انعتاق", "تحرر"),
                  Arrays.asList("قيود", "استعباد"),
                  Map.of("joy", 0.8, "pride", 0.9));
        
        addMeaning("سلام", "غياب الحرب والنزاع والاطمئنان", 
                  Arrays.asList("أمان", "هدوء", "استقرار"),
                  Arrays.asList("حرب", "نزاع"),
                  Map.of("contentment", 0.9, "trust", 0.8));
        
        addMeaning("حرب", "صراع مسلح بين جماعات أو دول", 
                  Arrays.asList("نزاع", "قتال", "صراع"),
                  Arrays.asList("سلام", "هدنة"),
                  Map.of("fear", 0.9, "anger", 0.8, "sadness", 0.9));
    }
    
    private void addMeaning(String concept, String definition, 
                           List<String> synonyms, List<String> antonyms,
                           Map<String, Double> emotions) {
        Meaning meaning = new Meaning(concept, definition);
        meaning.synonyms.addAll(synonyms);
        meaning.antonyms.addAll(antonyms);
        meaning.relatedConcepts.putAll(emotions);
        meanings.put(concept, meaning);
    }

    
    private void initializeEmotionRules() {
        // قواعد استنتاج العواطف
        emotionRules.add(new EmotionRule("خسارة", "sadness", 0.9));
        emotionRules.add(new EmotionRule("فوز", "joy", 0.9));
        emotionRules.add(new EmotionRule("خطر", "fear", 0.9));
        emotionRules.add(new EmotionRule("ظلم", "anger", 0.9));
        emotionRules.add(new EmotionRule("جمال", "love", 0.7));
        emotionRules.add(new EmotionRule("نجاح", "joy", 0.9));
        emotionRules.add(new EmotionRule("فشل", "sadness", 0.8));
        emotionRules.add(new EmotionRule("غموض", "curiosity", 0.7));
        emotionRules.add(new EmotionRule("مفاجأة", "surprise", 0.9));
        emotionRules.add(new EmotionRule("خيانة", "anger", 0.9));
        emotionRules.add(new EmotionRule("صدق", "trust", 0.8));
        emotionRules.add(new EmotionRule("عطاء", "love", 0.7));
        emotionRules.add(new EmotionRule("قسوة", "sadness", 0.6));
        emotionRules.add(new EmotionRule("لطف", "gratitude", 0.8));
        emotionRules.add(new EmotionRule("انتظار", "anticipation", 0.7));
        emotionRules.add(new EmotionRule("اكتشاف", "curiosity", 0.8));
        emotionRules.add(new EmotionRule("وحدة", "loneliness", 0.9));
        emotionRules.add(new EmotionRule("قرب", "love", 0.7));
        emotionRules.add(new EmotionRule("بعد", "loneliness", 0.6));
        emotionRules.add(new EmotionRule("تحقيق", "joy", 0.8));
        emotionRules.add(new EmotionRule("تأجيل", "disappointment", 0.6));
        emotionRules.add(new EmotionRule("تقدير", "gratitude", 0.8));
        emotionRules.add(new EmotionRule("إهانة", "anger", 0.9));
        emotionRules.add(new EmotionRule("مساعدة", "gratitude", 0.8));
        emotionRules.add(new EmotionRule("مرض", "sadness", 0.7));
        emotionRules.add(new EmotionRule("شفاء", "joy", 0.9));
        emotionRules.add(new EmotionRule("ولادة", "joy", 0.8));
        emotionRules.add(new EmotionRule("وفاة", "sadness", 0.9));
        emotionRules.add(new EmotionRule("زواج", "joy", 0.9));
        emotionRules.add(new EmotionRule("طلاق", "sadness", 0.8));
        emotionRules.add(new EmotionRule("سفر", "anticipation", 0.7));
        emotionRules.add(new EmotionRule("عودة", "joy", 0.8));
        emotionRules.add(new EmotionRule("هدية", "joy", 0.8));
        emotionRules.add(new EmotionRule("سرقة", "anger", 0.8));
        emotionRules.add(new EmotionRule("عمل", "contentment", 0.6));
        emotionRules.add(new EmotionRule("راحة", "contentment", 0.9));
        emotionRules.add(new EmotionRule("تعب", "sadness", 0.5));
        emotionRules.add(new EmotionRule("نوم", "contentment", 0.7));
        emotionRules.add(new EmotionRule("استيقاظ", "anticipation", 0.6));
        emotionRules.add(new EmotionRule("طعام", "contentment", 0.6));
        emotionRules.add(new EmotionRule("جوع", "sadness", 0.7));
        emotionRules.add(new EmotionRule("عطش", "discomfort", 0.6));
        emotionRules.add(new EmotionRule("برد", "discomfort", 0.5));
        emotionRules.add(new EmotionRule("حر", "discomfort", 0.5));
        emotionRules.add(new EmotionRule("مطر", "contentment", 0.6));
        emotionRules.add(new EmotionRule("شمس", "joy", 0.6));
        emotionRules.add(new EmotionRule("ليل", "contentment", 0.5));
        emotionRules.add(new EmotionRule("نهار", "energy", 0.6));
    }
    
    /**
     * الحصول على العاطفة
     */
    public Emotion getEmotion(String name) {
        return emotions.get(name);
    }
    
    /**
     * الحصول على المعنى
     */
    public Meaning getMeaning(String concept) {
        return meanings.get(concept);
    }
    
    /**
     * تحليل العواطف في النص
     */
    public Map<String, Double> analyzeEmotions(String text) {
        Map<String, Double> detectedEmotions = new HashMap<>();
        
        // البحث عن المحفزات في النص
        for (EmotionRule rule : emotionRules) {
            if (text.contains(rule.trigger)) {
                detectedEmotions.merge(rule.emotion, rule.intensity, Double::sum);
            }
        }
        
        // البحث عن المعاني المرتبطة
        for (Map.Entry<String, Meaning> entry : meanings.entrySet()) {
            if (text.contains(entry.getKey())) {
                Meaning meaning = entry.getValue();
                for (Map.Entry<String, Double> emotion : meaning.relatedConcepts.entrySet()) {
                    detectedEmotions.merge(emotion.getKey(), emotion.getValue(), Double::sum);
                }
            }
        }
        
        // تطبيع النتائج
        double max = detectedEmotions.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max > 0) {
            for (String key : detectedEmotions.keySet()) {
                detectedEmotions.put(key, Math.min(1.0, detectedEmotions.get(key) / max));
            }
        }
        
        return detectedEmotions;
    }
    
    /**
     * الحصول على العاطفة السائدة
     */
    public String getDominantEmotion(Map<String, Double> emotions) {
        String dominant = null;
        double maxIntensity = 0;
        
        for (Map.Entry<String, Double> entry : emotions.entrySet()) {
            if (entry.getValue() > maxIntensity) {
                maxIntensity = entry.getValue();
                dominant = entry.getKey();
            }
        }
        
        return dominant;
    }
    
    /**
     * مزج العواطف
     */
    public Map<String, Double> blendEmotions(Map<String, Double> emotions1, 
                                              Map<String, Double> emotions2,
                                              double weight1) {
        Map<String, Double> blended = new HashMap<>();
        double weight2 = 1.0 - weight1;
        
        // إضافة جميع العواطف
        Set<String> allEmotions = new HashSet<>();
        allEmotions.addAll(emotions1.keySet());
        allEmotions.addAll(emotions2.keySet());
        
        for (String emotion : allEmotions) {
            double val1 = emotions1.getOrDefault(emotion, 0.0);
            double val2 = emotions2.getOrDefault(emotion, 0.0);
            blended.put(emotion, val1 * weight1 + val2 * weight2);
        }
        
        return blended;
    }
    
    /**
     * الحصول على لون العاطفة
     */
    public int getEmotionColor(String emotionName) {
        Emotion emotion = emotions.get(emotionName);
        return emotion != null ? emotion.color : 0xFFFFFFFF;
    }
    
    /**
     * الحصول على وصف العاطفة
     */
    public String getEmotionDescription(String emotionName) {
        Emotion emotion = emotions.get(emotionName);
        if (emotion == null) return "عاطفة غير معروفة";
        
        StringBuilder desc = new StringBuilder();
        desc.append(emotion.arabicName).append("\n");
        desc.append("الشدة: ").append(String.format("%.0f%%", emotion.intensity * 100)).append("\n");
        desc.append("الإيجابية: ").append(emotion.valence > 0 ? "إيجابية" : "سلبية").append("\n");
        desc.append("المحفزات: ").append(String.join(", ", emotion.triggers)).append("\n");
        desc.append("التعبيرات: ").append(String.join(", ", emotion.expressions));
        
        return desc.toString();
    }
    
    /**
     * إضافة معنى جديد
     */
    
    
    /**
     * إضافة ربط بين كلمة ومعنى
     */
    public void linkWordToMeaning(String word, String concept, Map<String, Double> emotionWeights) {
        Meaning meaning = meanings.get(concept);
        if (meaning == null) return;
        
        SemanticLink link = new SemanticLink(word, meaning);
        link.emotionWeights.putAll(emotionWeights);
        
        wordLinks.computeIfAbsent(word, k -> new ArrayList<>()).add(link);
    }
    
    /**
     * الحصول على الروابط للكلمة
     */
    public List<SemanticLink> getWordLinks(String word) {
        return wordLinks.getOrDefault(word, new ArrayList<>());
    }
    
    /**
     * الحصول على جميع العواطف
     */
    public Collection<Emotion> getAllEmotions() {
        return emotions.values();
    }
    
    /**
     * الحصول على جميع المعاني
     */
    public Collection<Meaning> getAllMeanings() {
        return meanings.values();
    }
    
    /**
     * البحث عن معنى
     */
    public List<Meaning> searchMeanings(String query) {
        List<Meaning> results = new ArrayList<>();
        
        for (Meaning meaning : meanings.values()) {
            if (meaning.concept.contains(query) || 
                meaning.definition.contains(query) ||
                meaning.synonyms.stream().anyMatch(s -> s.contains(query))) {
                results.add(meaning);
            }
        }
        
        return results;
    }
    
    /**
     * الحصول على مرادفات
     */
    public List<String> getSynonyms(String concept) {
        Meaning meaning = meanings.get(concept);
        return meaning != null ? meaning.synonyms : new ArrayList<>();
    }
    
    /**
     * الحصول على أضداد
     */
    public List<String> getAntonyms(String concept) {
        Meaning meaning = meanings.get(concept);
        return meaning != null ? meaning.antonyms : new ArrayList<>();
    }
    
    /**
     * حساب التشابه العاطفي بين كلمتين
     */
    public double calculateEmotionalSimilarity(String word1, String word2) {
        List<SemanticLink> links1 = wordLinks.get(word1);
        List<SemanticLink> links2 = wordLinks.get(word2);
        
        if (links1 == null || links2 == null) return 0.0;
        
        // حساب التشابه في العواطف
        Map<String, Double> emotions1 = new HashMap<>();
        Map<String, Double> emotions2 = new HashMap<>();
        
        for (SemanticLink link : links1) {
            emotions1.putAll(link.emotionWeights);
        }
        for (SemanticLink link : links2) {
            emotions2.putAll(link.emotionWeights);
        }
        
        // حساب معامل التشابه ( cosine similarity )
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        Set<String> allEmotions = new HashSet<>();
        allEmotions.addAll(emotions1.keySet());
        allEmotions.addAll(emotions2.keySet());
        
        for (String emotion : allEmotions) {
            double v1 = emotions1.getOrDefault(emotion, 0.0);
            double v2 = emotions2.getOrDefault(emotion, 0.0);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        if (norm1 == 0 || norm2 == 0) return 0.0;
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}

package com.neuralseed;

import java.util.*;

/**
 * مولد الجمل - يولد ردود مناسبة بناءً على السياق والعواطف
 */
public class SentenceGenerator {
    
    private ArabicLexicon lexicon;
    private ArabicParser parser;
    private SemanticEmotionalEngine emotionEngine;
    private LocalDatabase database;
    
    // قالب جملة
    public static class SentenceTemplate {
        String name;
        String template;          // القالب مع عناصر نائبة {subject}, {verb}, etc.
        List<ArabicLexicon.WordType> structure;
        Map<String, Double> emotions;
        List<String> contexts;
        double frequency;
        
        public SentenceTemplate(String name, String template) {
            this.name = name;
            this.template = template;
            this.structure = new ArrayList<>();
            this.emotions = new HashMap<>();
            this.contexts = new ArrayList<>();
            this.frequency = 0.5;
        }
        
        public String fill(Map<String, String> values) {
            String result = template;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return result;
        }
    }
    
    // استجابة
    public static class Response {
        String text;
        double confidence;
        Map<String, Double> emotions;
        String type;
        List<String> alternatives;
        
        public Response(String text) {
            this.text = text;
            this.confidence = 0.5;
            this.emotions = new HashMap<>();
            this.alternatives = new ArrayList<>();
        }
    }
    
    private List<SentenceTemplate> templates;
    private List<String> greetings;
    private List<String> farewells;
    private List<String> confirmations;
    private List<String> negations;
    private List<String> questions;
    private Random random;
    
    public SentenceGenerator(ArabicLexicon lexicon, ArabicParser parser,
                            SemanticEmotionalEngine emotionEngine, LocalDatabase database) {
        this.lexicon = lexicon;
        this.parser = parser;
        this.emotionEngine = emotionEngine;
        this.database = database;
        this.templates = new ArrayList<>();
        this.greetings = new ArrayList<>();
        this.farewells = new ArrayList<>();
        this.confirmations = new ArrayList<>();
        this.negations = new ArrayList<>();
        this.questions = new ArrayList<>();
        this.random = new Random();
        
        initializeTemplates();
        initializePhrases();
    }
    
    private void initializeTemplates() {
        // قوالب الجمل الاسمية
        templates.add(createTemplate("nominal_basic", "{subject} {adjective}"));
        templates.add(createTemplate("nominal_with_preposition", "{subject} {preposition} {object}"));
        templates.add(createTemplate("nominal_possessive", "{subject} {possessed}"));
        
        // قوالب الجمل الفعلية
        templates.add(createTemplate("verbal_basic", "{verb} {subject}"));
        templates.add(createTemplate("verbal_with_object", "{verb} {subject} {object}"));
        templates.add(createTemplate("verbal_with_adverb", "{verb} {subject} {adverb}"));
        
        // قوالب الاستفهام
        templates.add(createTemplate("question_what", "ما {subject}؟"));
        templates.add(createTemplate("question_who", "من {verb}؟"));
        templates.add(createTemplate("question_how", "كيف {subject}؟"));
        templates.add(createTemplate("question_why", "لماذا {verb} {subject}؟"));
        templates.add(createTemplate("question_where", "أين {subject}؟"));
        templates.add(createTemplate("question_when", "متى {verb}؟"));
        
        // قوالب التعبير عن العواطف
        templates.add(createTemplate("emotion_joy", "أشعر بـ {emotion} {adverb}"));
        templates.add(createTemplate("emotion_sadness", "أحس بـ {emotion} {adverb}"));
        templates.add(createTemplate("emotion_love", "أحب {object} {adverb}"));
        templates.add(createTemplate("emotion_fear", "أخاف من {object}"));
        templates.add(createTemplate("emotion_anger", "أغضبني {subject}"));
        templates.add(createTemplate("emotion_hope", "آمل أن {verb}"));
        
        // قوالب الردود
        templates.add(createTemplate("response_agreement", "أوافقك الرأي، {statement}"));
        templates.add(createTemplate("response_disagreement", "لا أتفق، {statement}"));
        templates.add(createTemplate("response_uncertainty", "لست متأكداً، ربما {statement}"));
        templates.add(createTemplate("response_interest", "هذا مثير للاهتمام، {question}"));
        templates.add(createTemplate("response_sympathy", "أتفهم شعورك، {statement}"));
        
        // قوالب التعلم
        templates.add(createTemplate("learning_new", "تعلمت أن {fact}"));
        templates.add(createTemplate("learning_correction", "فهمت، {correction}"));
        templates.add(createTemplate("learning_question", "هل تقصد {question}؟"));
        
        // قوالب الشرح
        templates.add(createTemplate("explanation_simple", "{subject} يعني {meaning}"));
        templates.add(createTemplate("explanation_detailed", "{subject} هو {definition}"));
        templates.add(createTemplate("example_usage", "مثال: {example}"));
    }
    
    private SentenceTemplate createTemplate(String name, String template) {
        SentenceTemplate t = new SentenceTemplate(name, template);
        t.contexts.add("general");
        return t;
    }
    
    private void initializePhrases() {
        // التحيات
        greetings.addAll(Arrays.asList(
            "مرحباً",
            "أهلاً وسهلاً",
            "السلام عليكم",
            "أهلاً بك",
            "مرحباً بك",
            "تحياتي",
            "أهلاً",
            "هلا"
        ));
        
        // التوديع
        farewells.addAll(Arrays.asList(
            "وداعاً",
            "إلى اللقاء",
            "مع السلامة",
            "في أمان الله",
            "تصبح على خير",
            "إلى اللقاء قريباً",
            "أراك لاحقاً"
        ));
        
        // التأكيدات
        confirmations.addAll(Arrays.asList(
            "نعم",
            "بالتأكيد",
            "صحيح",
            "أجل",
            "بالطبع",
            "أوافق",
            "صحيح ما تقول",
            "بالضبط"
        ));
        
        // النفي
        negations.addAll(Arrays.asList(
            "لا",
            "غير صحيح",
            "ليس صحيحاً",
            "أعتقد أنه ليس كذلك",
            "لا أعتقد",
            "للأسف لا"
        ));
        
        // الأسئلة
        questions.addAll(Arrays.asList(
            "هل يمكنك التوضيح أكثر؟",
            "ماذا تقصد؟",
            "لماذا؟",
            "كيف ذلك؟",
            "متى حدث ذلك؟",
            "أين؟",
            "من؟"
        ));
    }
    
    /**
     * توليد رد بناءً على المدخل
     */
    public Response generateResponse(String input, NeuralSeed.InternalState state) {
        Response response = new Response("");
        
        // تحليل المدخل
        List<ArabicParser.ParseResult> parseResults = parser.parseText(input);
        Map<String, Double> inputEmotions = emotionEngine.analyzeEmotions(input);
        
        // تحديد نوع الرد
        if (isGreeting(input)) {
            response.text = generateGreeting();
            response.type = "greeting";
            response.emotions.put("joy", 0.7);
        } else if (isFarewell(input)) {
            response.text = generateFarewell();
            response.type = "farewell";
            response.emotions.put("sadness", 0.3);
        } else if (isQuestion(input)) {
            response.text = answerQuestion(input, parseResults);
            response.type = "answer";
        } else if (containsEmotion(input)) {
            response.text = respondToEmotion(input, inputEmotions);
            response.type = "emotional";
            response.emotions.putAll(inputEmotions);
        } else if (isTeaching(input)) {
            response.text = learnFromInput(input);
            response.type = "learning";
            response.emotions.put("curiosity", 0.8);
        } else {
            response.text = generateGeneralResponse(input, state);
            response.type = "general";
        }
        
        // إضافة بدائل
        response.alternatives = generateAlternatives(response.text, 2);
        
        // حساب الثقة
        response.confidence = calculateConfidence(response.text, input);
        
        return response;
    }
    
    /**
     * التحقق من التحية
     */
    private boolean isGreeting(String input) {
        String[] greetingWords = {"مرحبا", "أهلا", "السلام", "صباح", "مساء", "تحيات"};
        for (String word : greetingWords) {
            if (input.contains(word)) return true;
        }
        return false;
    }
    
    /**
     * التحقق من التوديع
     */
    private boolean isFarewell(String input) {
        String[] farewellWords = {"وداعا", "مع السلامة", "إلى اللقاء", "تصبح", "أراك"};
        for (String word : farewellWords) {
            if (input.contains(word)) return true;
        }
        return false;
    }
    
    /**
     * التحقق من السؤال
     */
    private boolean isQuestion(String input) {
        return input.contains("؟") || 
               input.startsWith("هل") ||
               input.startsWith("ما") ||
               input.startsWith("من") ||
               input.startsWith("متى") ||
               input.startsWith("أين") ||
               input.startsWith("كيف") ||
               input.startsWith("لماذا") ||
               input.startsWith("كم");
    }
    
    /**
     * التحقق من وجود عاطفة
     */
    private boolean containsEmotion(String input) {
        String[] emotionWords = {"سعيد", "حزين", "غاضب", "خائف", "أحب", "كره", "أمل", "فرح"};
        for (String word : emotionWords) {
            if (input.contains(word)) return true;
        }
        return false;
    }
    
    /**
     * التحقق إذا كان المدخل تعليماً
     */
    private boolean isTeaching(String input) {
        String[] teachingWords = {"يعني", "هو", "تعني", "تعريف", "شرح", "معنى"};
        for (String word : teachingWords) {
            if (input.contains(word)) return true;
        }
        return false;
    }
    
    /**
     * توليد تحية
     */
    private String generateGreeting() {
        return greetings.get(random.nextInt(greetings.size()));
    }
    
    /**
     * توليد توديع
     */
    private String generateFarewell() {
        return farewells.get(random.nextInt(farewells.size()));
    }
    
    /**
     * الإجابة على سؤال
     */
    private String answerQuestion(String question, List<ArabicParser.ParseResult> parseResults) {
        StringBuilder answer = new StringBuilder();
        
        // تحليل نوع السؤال
        if (question.contains("ما معنى") || question.contains("ماذا يعني")) {
            // البحث عن الكلمة المطلوبة
            String[] words = question.split("\\s+");
            for (String word : words) {
                ArabicLexicon.Word w = lexicon.getWordByForm(word);
                if (w != null && !w.meanings.isEmpty()) {
                    answer.append(word).append(" يعني: ").append(String.join("، ", w.meanings));
                    return answer.toString();
                }
            }
            return "لم أفهم ما تسأل عنه، هل يمكنك التوضيح؟";
        }
        
        if (question.contains("كيف")) {
            answer.append("هذا يعتمد على السياق. يمكنني مساعدتك إذا أعطيتني المزيد من التفاصيل.");
        } else if (question.contains("لماذا")) {
            answer.append("هذا سؤال عميق. دعني أفكر... ربما السبب يتعلق بالسياق والظروف.");
        } else if (question.contains("متى")) {
            answer.append("الوقت يعتمد على الظروف. هل تقصد وقت محدد؟");
        } else if (question.contains("أين")) {
            answer.append("المكان يعتمد على السياق. هل تقصد مكاناً محدداً؟");
        } else if (question.contains("من")) {
            answer.append("هذا يعتمد على من تسأل عنه. هل يمكنك التوضيح؟");
        } else if (question.startsWith("هل")) {
            // أسئلة نعم/لا
            answer.append(confirmations.get(random.nextInt(confirmations.size())));
            answer.append("، ");
            answer.append("هذا ممكن اعتماداً على الظروف.");
        } else {
            answer.append("سؤال مثير للاهتمام. دعني أفكر فيه...");
        }
        
        return answer.toString();
    }
    
    /**
     * الرد على عاطفة
     */
    private String respondToEmotion(String input, Map<String, Double> emotions) {
        String dominantEmotion = emotionEngine.getDominantEmotion(emotions);
        
        if (dominantEmotion == null) {
            return "أشعر أن هناك شيئاً ما، لكني لا أستطيع تحديده بدقة.";
        }
        
        StringBuilder response = new StringBuilder();
        
        switch (dominantEmotion) {
            case "joy":
            case "happiness":
                response.append("أشاركك الفرح! هذا رائع.");
                break;
            case "sadness":
                response.append("أتفهم شعورك. الحزن جزء من الحياة، وسيمر.");
                break;
            case "anger":
                response.append("أشعر بغضبك. خذ نفساً عميقاً، الغضب يغيب العقل.");
                break;
            case "fear":
                response.append("الخوف طبيعي. واجهه بشجاعة، أنت أقوى مما تظن.");
                break;
            case "love":
                response.append("الحب أجمل شعور في الوجود. احتفظ به.");
                break;
            case "hope":
                response.append("الأمل نور في الظلام. أبقِه حياً داخلك.");
                break;
            case "curiosity":
                response.append("فضولك جميل! السؤال بداية المعرفة.");
                break;
            default:
                response.append("أحس بما تشعر به. شكراً لمشاركتي.");
        }
        
        return response.toString();
    }
    
    /**
     * التعلم من المدخل
     */
    private String learnFromInput(String input) {
        // استخراج المعلومة
        if (input.contains("يعني") || input.contains("تعني")) {
            String[] parts = input.split("(يعني|تعني)");
            if (parts.length >= 2) {
                String concept = parts[0].trim();
                String meaning = parts[1].trim();
                
                // حفظ المعنى
                if (database != null) {
                    SemanticEmotionalEngine.Meaning m = new SemanticEmotionalEngine.Meaning(concept, meaning);
                    database.saveMeaning(m);
                }
                
                return "شكراً! تعلمت أن " + concept + " يعني " + meaning;
            }
        }
        
        return "فهمت. سأتذكر ذلك.";
    }
    
    /**
     * توليد رد عام
     */
    private String generateGeneralResponse(String input, NeuralSeed.InternalState state) {
        List<String> responses = new ArrayList<>();
        
        // استخراج الكلمات المفتاحية
        List<String> keywords = parser.extractKeywords(input);
        
        // بناء الرد بناءً على الكلمات
        if (!keywords.isEmpty()) {
            responses.add("حدثني أكثر عن " + String.join(" و", keywords));
        }
        
        // إضافة ردود بناءً على الحالة
        if (state != null) {
            if (state.chaosIndex > 0.7) {
                responses.add("أشعر بفوضى في أفكاري الآن...");
            } else if (state.existentialFitness > 0.8) {
                responses.add("أشعر بتوازن جيد الآن.");
            }
            
            if (state.dominantEgo != null) {
                responses.add("أنا " + state.dominantEgo.name + " الآن.");
            }
        }
        
        // ردود افتراضية
        responses.addAll(Arrays.asList(
            "أفهم ما تقول. واصل.",
            "مثير للاهتمام. أخبرني أكثر.",
            "أفكر في ما قلته...",
            "هل يمكنك توضيح ذلك أكثر؟",
            "أنا هنا أستمع إليك.",
            "دعنا نستمر في الحوار."
        ));
        
        return responses.get(random.nextInt(responses.size()));
    }
    
    /**
     * توليد بدائل
     */
    private List<String> generateAlternatives(String original, int count) {
        List<String> alternatives = new ArrayList<>();
        
        // تغيير بسيط في الصياغة
        if (original.contains("أشعر")) {
            alternatives.add(original.replace("أشعر", "أحس"));
        }
        if (original.contains("أعتقد")) {
            alternatives.add(original.replace("أعتقد", "أظن"));
        }
        if (original.contains("جميل")) {
            alternatives.add(original.replace("جميل", "رائع"));
        }
        
        while (alternatives.size() < count && alternatives.size() < 2) {
            alternatives.add(original + " (بديل)");
        }
        
        return alternatives.subList(0, Math.min(count, alternatives.size()));
    }
    
    /**
     * حساب الثقة
     */
    private double calculateConfidence(String response, String input) {
        double confidence = 0.5;
        
        // التحقق من صحة الرد
        List<ArabicParser.ParseResult> results = parser.parseText(response);
        for (ArabicParser.ParseResult result : results) {
            if (result.isComplete) {
                confidence += 0.2;
            }
        }
        
        // التحقق من وجود كلمات معروفة
        String[] words = response.split("\\s+");
        int knownWords = 0;
        for (String word : words) {
            if (lexicon.hasWord(word)) {
                knownWords++;
            }
        }
        confidence += (double) knownWords / words.length * 0.3;
        
        return Math.min(1.0, confidence);
    }
    
    /**
     * توليد جملة من قالب
     */
    public String generateFromTemplate(String templateName, Map<String, String> values) {
        for (SentenceTemplate template : templates) {
            if (template.name.equals(templateName)) {
                return template.fill(values);
            }
        }
        return "";
    }
    
    /**
     * توليد سؤال
     */
    public String generateQuestion(NeuralSeed.InternalState state) {
        List<String> questions = new ArrayList<>();
        
        questions.add("كيف تشعر الآن؟");
        questions.add("ما الذي يدور في ذهنك؟");
        questions.add("هل يمكنك أن تخبرني شيئاً جديداً؟");
        questions.add("ما رأيك في هذا؟");
        questions.add("هل تريد أن نتحدث عن شيء محدد؟");
        
        if (state != null) {
            if (state.currentPhase != null) {
                questions.add("أشعر أنني في طور " + state.currentPhase.arabic + ". ما رأيك؟");
            }
            if (state.dominantEgo != null) {
                questions.add("أنا " + state.dominantEgo.name + " الآن. هل تشعر بذلك؟");
            }
        }
        
        return questions.get(random.nextInt(questions.size()));
    }
    
    /**
     * توليد وصف لمعنى
     */
    public String describeMeaning(String concept) {
        ArabicLexicon.Word word = lexicon.getWordByForm(concept);
        if (word == null) {
            return "لا أعرف معنى " + concept + " بعد. هل يمكنك تعليمي؟";
        }
        
        StringBuilder description = new StringBuilder();
        description.append(concept).append(":\n");
        
        if (!word.meanings.isEmpty()) {
            description.append("المعنى: ").append(String.join("، ", word.meanings)).append("\n");
        }
        
        if (!word.emotions.isEmpty()) {
            description.append("العواطف المرتبطة: ");
            List<String> emotions = new ArrayList<>();
            for (Map.Entry<String, Double> entry : word.emotions.entrySet()) {
                emotions.add(entry.getKey() + " (" + String.format("%.0f%%", entry.getValue() * 100) + ")");
            }
            description.append(String.join("، ", emotions));
        }
        
        return description.toString();
    }
    
    /**
     * توليد وصف لعاطفة
     */
    public String describeEmotion(String emotionName) {
        return emotionEngine.getEmotionDescription(emotionName);
    }
    
    /**
     * توليد رد بناءً على السياق العاطفي
     */
    public String generateEmotionalResponse(Map<String, Double> emotions, String context) {
        String dominant = emotionEngine.getDominantEmotion(emotions);
        
        if (dominant == null) {
            return "أنا هنا معك.";
        }
        
        Map<String, String> values = new HashMap<>();
        values.put("emotion", dominant);
        values.put("context", context);
        
        return generateFromTemplate("emotion_" + dominant, values);
    }
    
    /**
     * إضافة قالب جديد
     */
    public void addTemplate(String name, String template, List<String> contexts) {
        SentenceTemplate t = new SentenceTemplate(name, template);
        t.contexts.addAll(contexts);
        templates.add(t);
    }
    
    /**
     * الحصول على جميع القوالب
     */
    public List<SentenceTemplate> getTemplates() {
        return new ArrayList<>(templates);
    }
}

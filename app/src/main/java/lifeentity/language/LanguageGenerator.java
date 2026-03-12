package com.lifeentity.language;

import com.lifeentity.core.EmotionalState;
import com.lifeentity.core.ValueSystem;
import com.lifeentity.memory.EpisodicMemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * نظام متقدم لتوليد اللغة بناءً على المعنى والقصد.
 * لا يحتوي على جمل مبرمجة، بل يبني الجمل ديناميكياً من:
 * - قاموس متطور (Lexicon) يتعلم كلمات جديدة.
 * - قواعد نحوية (Grammar) لتركيب الجمل.
 * - مخطّط للحديث (DiscoursePlanner) يقرر ماذا يقول بناءً على السياق.
 */
public class LanguageGenerator {
    private Lexicon lexicon;
    private GrammarRules grammar;
    private DiscoursePlanner planner;
    private Random random;

    public LanguageGenerator() {
        this.lexicon = new Lexicon();
        this.grammar = new GrammarRules();
        this.planner = new DiscoursePlanner();
        this.random = new Random();
    }

    // ======================== الواجهة العامة ========================

    /**
     * توليد رد على رسالة المستخدم
     */
    public String generateResponse(String userMessage, EmotionalState emotion, String dominantDesire,
                                   List<EpisodicMemory.EventEntity> recentMemories, ValueSystem values) {
        // 1. تحليل رسالة المستخدم (بسيط حالياً، يمكن استبدال بـ AdvancedArabicLexicon)
        List<String> userWords = tokenize(userMessage);
        String topic = extractTopic(userWords, values);

        // 2. تحديد الهدف من الرد (Planner)
        DiscourseGoal goal = planner.planResponse(userWords, emotion, dominantDesire, recentMemories, values);

        // 3. بناء الجملة (Grammar + Lexicon)
        return buildUtterance(goal, emotion, values);
    }

    /**
     * توليد كلام عفوي (ليس رداً)
     */
    public String generateSpontaneousSpeech(EmotionalState emotion, String dominantDesire,
                                            List<EpisodicMemory.EventEntity> recentMemories, ValueSystem values) {
        DiscourseGoal goal = planner.planSpontaneous(emotion, dominantDesire, recentMemories, values);
        return buildUtterance(goal, emotion, values);
    }

    // ======================== بناء الجملة ========================

    private String buildUtterance(DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        // اختيار نوع الجملة (فعلية، اسمية، شرطية، ...)
        SentenceType sentenceType = grammar.selectSentenceType(goal);

        // بناء الجملة حسب نوعها
        switch (sentenceType) {
            case VERBAL:
                return buildVerbalSentence(goal, emotion, values);
            case NOMINAL:
                return buildNominalSentence(goal, emotion, values);
            case CONDITIONAL:
                return buildConditionalSentence(goal, emotion, values);
            default:
                return buildSimplePhrase(goal, emotion, values);
        }
    }

    private String buildVerbalSentence(DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        // فعل + فاعل + (مفعول به) + (جار ومجرور)
        String verb = selectVerb(goal, emotion, values);
        String subject = selectSubject(goal);
        String object = selectObject(goal, values);
        String prepositionalPhrase = selectPrepositionalPhrase(goal);

        StringBuilder sentence = new StringBuilder();
        sentence.append(verb).append(" ").append(subject);
        if (object != null) sentence.append(" ").append(object);
        if (prepositionalPhrase != null) sentence.append(" ").append(prepositionalPhrase);

        // إضافة علامات الترقيم حسب النبرة
        if (goal.tone == Tone.QUESTION) sentence.append("؟");
        else if (goal.tone == Tone.EXCITED) sentence.append("!");
        else sentence.append(".");

        return sentence.toString();
    }

    private String buildNominalSentence(DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        // مبتدأ + خبر
        String subject = selectNoun(goal.topic, goal.subjectType);
        String predicate = selectPredicate(goal, emotion, values);
        return subject + " " + predicate + ".";
    }

    private String buildConditionalSentence(DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        // إذا ... فإن ...
        String condition = "إذا " + selectVerb(goal, emotion, values) + " " + selectSubject(goal);
        String result = selectResult(goal, emotion, values);
        return condition + "، فإن " + result + ".";
    }

    private String buildSimplePhrase(DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        // عبارة قصيرة (مثل: "جميل"، "مثير للاهتمام")
        return selectAdjective(goal.topic, emotion) + ".";
    }

    // ======================== اختيار الكلمات ========================

    private String selectVerb(DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        // يختار فعلاً مناسباً للهدف والعاطفة
        List<String> candidates = lexicon.getVerbsByIntent(goal.intent, emotion);
        if (candidates.isEmpty()) candidates = lexicon.getDefaultVerbs();
        return candidates.get(random.nextInt(candidates.size()));
    }

    private String selectSubject(DiscourseGoal goal) {
        if (goal.subjectType == SubjectType.SELF) return "أنا";
        if (goal.subjectType == SubjectType.USER) return "أنت";
        if (goal.subjectType == SubjectType.TOPIC && goal.topic != null) return goal.topic;
        return "هذا";
    }

    private String selectObject(DiscourseGoal goal, ValueSystem values) {
        if (goal.objectTopic != null) return goal.objectTopic;
        if (goal.topic != null && random.nextBoolean()) return goal.topic;
        return null;
    }

    private String selectNoun(String topic, SubjectType type) {
        if (topic != null) return topic;
        return "الأمر";
    }

    private String selectPredicate(DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        // خبر مناسب (صفة، اسم، جملة)
        if (random.nextBoolean()) {
            return selectAdjective(goal.topic, emotion);
        } else {
            return "مثير للاهتمام";
        }
    }

    private String selectAdjective(String topic, EmotionalState emotion) {
        List<String> adjectives = lexicon.getAdjectivesByEmotion(emotion);
        if (adjectives.isEmpty()) adjectives = lexicon.getDefaultAdjectives();
        return adjectives.get(random.nextInt(adjectives.size()));
    }

    private String selectResult(DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        return "سأفكر في " + (goal.topic != null ? goal.topic : "ذلك");
    }

    private String selectPrepositionalPhrase(DiscourseGoal goal) {
        if (goal.location != null) return "في " + goal.location;
        return null;
    }

    // ======================== تحليل بسيط ========================

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String s : text.split("\\s+")) {
            tokens.add(s.replaceAll("[^\\p{L}]", ""));
        }
        return tokens;
    }

    private String extractTopic(List<String> words, ValueSystem values) {
        // اختيار أول كلمة طويلة كموضوع (يمكن تحسينه)
        for (String w : words) {
            if (w.length() > 2) return w;
        }
        return null;
    }

    // ======================== الفئات الداخلية ========================

    /**
     * هدف الخطاب: ما الذي يريد الكائن تحقيقه بالكلام.
     */
    public static class DiscourseGoal {
        Intent intent;          // القصد: التعبير عن مشاعر، سؤال، مشاركة ذكرى، تخيل، ...
        Tone tone;              // النبرة: عادي، سؤال، متحمس، حزين...
        SubjectType subjectType;// الفاعل: أنا، أنت، الموضوع
        String topic;           // الموضوع الأساسي
        String objectTopic;     // مفعول به (إن وجد)
        String location;        // مكان (إن وجد)
        float intensity;        // شدة العاطفة (تؤثر على اختيار الكلمات)

        public DiscourseGoal(Intent intent) {
            this.intent = intent;
            this.tone = Tone.NEUTRAL;
            this.subjectType = SubjectType.SELF;
        }
    }

    public enum Intent {
        EXPRESS_FEELING,       // التعبير عن مشاعر
        ASK_QUESTION,          // سؤال المستخدم
        SHARE_MEMORY,          // مشاركة ذكرى
        IMAGINE,               // تخيل شيء
        COMMENT_ON_TOPIC,      // التعليق على موضوع
        GREET,                 // تحية
        UNKNOWN
    }

    public enum Tone {
        NEUTRAL, QUESTION, EXCITED, SAD, ANGRY, CURIOUS
    }

    public enum SubjectType {
        SELF, USER, TOPIC
    }

    public enum SentenceType {
        VERBAL, NOMINAL, CONDITIONAL, PHRASE
    }

    // ======================== القاموس (يتعلم) ========================

    public static class Lexicon {
        // قوائم أولية – يمكن أن تبدأ فارغة وتتعلم من المستخدم
        private Map<String, WordInfo> words = new HashMap<>();

        // قوائم افتراضية (في البداية)
        private List<String> defaultVerbs = new ArrayList<>();
        private List<String> defaultAdjectives = new ArrayList<>();

        public Lexicon() {
            // تهيئة بسيطة (يمكن إزالتها لتبدأ فارغة)
            defaultVerbs.add("أحب");
            defaultVerbs.add("أكره");
            defaultVerbs.add("أفكر");
            defaultVerbs.add("أتساءل");
            defaultVerbs.add("أشعر");

            defaultAdjectives.add("جميل");
            defaultAdjectives.add("مثير");
            defaultAdjectives.add("غريب");
            defaultAdjectives.add("رائع");
            defaultAdjectives.add("صعب");

            // كلمات قليلة أولية
            addWord("أنا", "ضمير", "self");
            addWord("أنت", "ضمير", "user");
            addWord("حب", "اسم", "emotion", 0.9f);
            addWord("خوف", "اسم", "emotion", -0.7f);
        }

        public void addWord(String word, String type, String... categories) {
            WordInfo info = new WordInfo(word, type, categories);
            words.put(word, info);
        }

        public List<String> getVerbsByIntent(Intent intent, EmotionalState emotion) {
            // هنا يمكن ربط الأفعال بالأهداف والعواطف
            List<String> result = new ArrayList<>();
            for (WordInfo w : words.values()) {
                if ("فعل".equals(w.type)) {
                    // منطق اختيار معقد يمكن تطويره
                    result.add(w.word);
                }
            }
            if (result.isEmpty()) result = defaultVerbs;
            return result;
        }

        public List<String> getAdjectivesByEmotion(EmotionalState emotion) {
            List<String> result = new ArrayList<>();
            for (WordInfo w : words.values()) {
                if ("صفة".equals(w.type)) {
                    // يمكن ربط الصفات بالمشاعر
                    result.add(w.word);
                }
            }
            if (result.isEmpty()) result = defaultAdjectives;
            return result;
        }

        public List<String> getDefaultVerbs() { return defaultVerbs; }
        public List<String> getDefaultAdjectives() { return defaultAdjectives; }

        private static class WordInfo {
            String word;
            String type; // اسم، فعل، صفة، حرف
            String[] categories; // عاطفي، مكاني، إلخ
            float valence; // قيمة عاطفية (-1..1)

            WordInfo(String word, String type, String... categories) {
                this.word = word;
                this.type = type;
                this.categories = categories;
                this.valence = 0;
            }
        }
    }

    // ======================== القواعد النحوية ========================

    public static class GrammarRules {
        public SentenceType selectSentenceType(DiscourseGoal goal) {
            // بناءً على الهدف، نختار نوع الجملة
            if (goal.intent == Intent.ASK_QUESTION) return SentenceType.VERBAL;
            if (goal.intent == Intent.EXPRESS_FEELING) return SentenceType.NOMINAL;
            if (goal.intent == Intent.SHARE_MEMORY) return SentenceType.VERBAL;
            if (goal.intent == Intent.COMMENT_ON_TOPIC) return SentenceType.NOMINAL;
            if (goal.intent == Intent.IMAGINE) return SentenceType.CONDITIONAL;
            return SentenceType.PHRASE;
        }
    }

    // ======================== مخطّط الحديث ========================

    public static class DiscoursePlanner {
        public DiscourseGoal planResponse(List<String> userWords, EmotionalState emotion,
                                           String dominantDesire, List<EpisodicMemory.EventEntity> recentMemories,
                                           ValueSystem values) {
            // تحليل رسالة المستخدم وتحديد الهدف
            Intent intent = Intent.UNKNOWN;
            Tone tone = Tone.NEUTRAL;
            String topic = null;

            // أبسط تحليل: إذا كانت الكلمات تحتوي على أداة استفهام
            for (String w : userWords) {
                if (isQuestionWord(w)) {
                    intent = Intent.ASK_QUESTION;
                    tone = Tone.QUESTION;
                    break;
                }
            }

            if (intent == Intent.UNKNOWN) {
                // افتراضياً نرد بتعليق على الموضوع
                intent = Intent.COMMENT_ON_TOPIC;
            }

            // استخراج الموضوع (أول كلمة طويلة)
            for (String w : userWords) {
                if (w.length() > 2) {
                    topic = w;
                    break;
                }
            }

            DiscourseGoal goal = new DiscourseGoal(intent);
            goal.tone = tone;
            goal.topic = topic;
            goal.subjectType = SubjectType.SELF; // الكائن هو الفاعل

            // ربط بالعاطفة
            if (emotion != null) {
                if (emotion.isJoyful()) goal.tone = Tone.EXCITED;
                else if (emotion.isSad()) goal.tone = Tone.SAD;
                else if (emotion.isAfraid()) goal.tone = Tone.SAD;
                else if (emotion.isCurious()) goal.tone = Tone.CURIOUS;
            }

            return goal;
        }

        public DiscourseGoal planSpontaneous(EmotionalState emotion, String dominantDesire,
                                              List<EpisodicMemory.EventEntity> recentMemories, ValueSystem values) {
            // اختيار هدف عشوائي أو بناءً على الحالة
            Intent intent;
            if (dominantDesire != null && dominantDesire.contains("bond")) {
                intent = Intent.EXPRESS_FEELING;
            } else if (dominantDesire != null && dominantDesire.contains("explore")) {
                intent = Intent.IMAGINE;
            } else if (!recentMemories.isEmpty() && random.nextBoolean()) {
                intent = Intent.SHARE_MEMORY;
            } else {
                intent = Intent.COMMENT_ON_TOPIC;
            }

            DiscourseGoal goal = new DiscourseGoal(intent);
            goal.subjectType = SubjectType.SELF;

            // اختيار موضوع من الذاكرة أو عشوائي
            if (!recentMemories.isEmpty() && random.nextBoolean()) {
                EpisodicMemory.EventEntity event = recentMemories.get(random.nextInt(recentMemories.size()));
                goal.topic = event.narrative; // قد يكون طويلاً، نحتاج إلى استخراج كلمة
            } else {
                String[] concepts = {"الحياة", "الفضاء", "الأحلام", "المستقبل", "الذكاء"};
                goal.topic = concepts[random.nextInt(concepts.length)];
            }

            return goal;
        }

        private boolean isQuestionWord(String word) {
            return word.equals("هل") || word.equals("ما") || word.equals("لماذا") ||
                    word.equals("كيف") || word.equals("أين") || word.equals("متى") ||
                    word.equals("من");
        }

        private Random random = new Random();
    }
}

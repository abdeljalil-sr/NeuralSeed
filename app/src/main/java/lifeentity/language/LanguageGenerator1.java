package com.lifeentity.language;

import android.util.Log;

import com.lifeentity.core.EmotionalState;
import com.lifeentity.core.ValueSystem;
import com.lifeentity.memory.EpisodicMemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * نظام متقدم لتوليد اللغة بناءً على المعنى والسياق والتعلم.
 * 
 * المكونات:
 * - AdvancedArabicLexicon: تحليل دقيق للغة العربية.
 * - Lexicon: قاموس داخلي يتعلم كلمات جديدة.
 * - GrammarRules: قواعد نحوية لتركيب الجمل.
 * - DiscoursePlanner: يخطط لمحتوى الكلام.
 * - LearningMechanism: يتعلم من تفاعلات المستخدم.
 */
public class LanguageGenerator {
    private static final String TAG = "LanguageGenerator";

    private AdvancedArabicLexicon advancedLexicon;
    private Lexicon lexicon;
    private GrammarRules grammar;
    private DiscoursePlanner planner;
    private LearningMechanism learning;
    private Random random;

    public LanguageGenerator() {
        this.advancedLexicon = new AdvancedArabicLexicon(); // إذا كان static، نستخدم مباشرة
        this.lexicon = new Lexicon();
        this.grammar = new GrammarRules();
        this.planner = new DiscoursePlanner();
        this.learning = new LearningMechanism();
        this.random = new Random();
        Log.i(TAG, "LanguageGenerator initialized.");
    }

    // ======================== الواجهة العامة ========================

    /**
     * توليد رد على رسالة المستخدم
     */
    public String generateResponse(String userMessage, EmotionalState emotion, String dominantDesire,
                                   List<EpisodicMemory.EventEntity> recentMemories, ValueSystem values) {
        // 1. تحليل رسالة المستخدم باستخدام المعجم المتقدم
        AdvancedArabicLexicon.TextAnalysis analysis = AdvancedArabicLexicon.analyze(userMessage);

        // 2. التعلم من رسالة المستخدم (كلمات جديدة، تكرارات)
        learning.learnFromUserMessage(analysis, lexicon, values);

        // 3. تحديد الهدف من الرد (Planner)
        DiscourseGoal goal = planner.planResponse(analysis, emotion, dominantDesire, recentMemories, values);

        // 4. بناء الجملة
        return buildUtterance(goal, emotion, values, analysis);
    }

    /**
     * توليد كلام عفوي (ليس رداً)
     */
    public String generateSpontaneousSpeech(EmotionalState emotion, String dominantDesire,
                                            List<EpisodicMemory.EventEntity> recentMemories, ValueSystem values) {
        DiscourseGoal goal = planner.planSpontaneous(emotion, dominantDesire, recentMemories, values);
        return buildUtterance(goal, emotion, values, null);
    }

    // ======================== بناء الجملة ========================

    private String buildUtterance(DiscourseGoal goal, EmotionalState emotion, ValueSystem values,
                                   AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        // اختيار نوع الجملة
        SentenceType sentenceType = grammar.selectSentenceType(goal);

        StringBuilder sentence = new StringBuilder();

        switch (sentenceType) {
            case VERBAL:
                buildVerbalSentence(sentence, goal, emotion, values, userAnalysis);
                break;
            case NOMINAL:
                buildNominalSentence(sentence, goal, emotion, values, userAnalysis);
                break;
            case CONDITIONAL:
                buildConditionalSentence(sentence, goal, emotion, values, userAnalysis);
                break;
            case PHRASE:
                buildPhrase(sentence, goal, emotion, values);
                break;
        }

        // إضافة علامات الترقيم
        addPunctuation(sentence, goal.tone);

        return sentence.toString().trim();
    }

    private void buildVerbalSentence(StringBuilder sb, DiscourseGoal goal, EmotionalState emotion,
                                      ValueSystem values, AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        // فعل
        String verb = selectVerb(goal, emotion, values, userAnalysis);
        sb.append(verb).append(" ");

        // فاعل
        String subject = selectSubject(goal);
        sb.append(subject);

        // مفعول به (إن وجد)
        String object = selectObject(goal, values, userAnalysis);
        if (object != null) {
            sb.append(" ").append(object);
        }

        // جار ومجرور (إن وجد)
        String prepositional = selectPrepositionalPhrase(goal, userAnalysis);
        if (prepositional != null) {
            sb.append(" ").append(prepositional);
        }
    }

    private void buildNominalSentence(StringBuilder sb, DiscourseGoal goal, EmotionalState emotion,
                                       ValueSystem values, AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        String subject = selectNoun(goal.topic, goal.subjectType, userAnalysis);
        String predicate = selectPredicate(goal, emotion, values, userAnalysis);
        sb.append(subject).append(" ").append(predicate);
    }

    private void buildConditionalSentence(StringBuilder sb, DiscourseGoal goal, EmotionalState emotion,
                                           ValueSystem values, AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        String condition = "إذا " + selectVerb(goal, emotion, values, userAnalysis) + " " + selectSubject(goal);
        String result = selectResult(goal, emotion, values, userAnalysis);
        sb.append(condition).append("، فإن ").append(result);
    }

    private void buildPhrase(StringBuilder sb, DiscourseGoal goal, EmotionalState emotion, ValueSystem values) {
        String phrase = selectAdjective(goal.topic, emotion, values);
        sb.append(phrase);
    }

    private void addPunctuation(StringBuilder sb, Tone tone) {
        if (tone == Tone.QUESTION) {
            sb.append("؟");
        } else if (tone == Tone.EXCITED) {
            sb.append("!");
        } else {
            sb.append(".");
        }
    }

    // ======================== اختيار الكلمات ========================

    private String selectVerb(DiscourseGoal goal, EmotionalState emotion, ValueSystem values,
                               AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        // نبحث في قاموسنا أولاً، ثم في المعجم
        List<String> candidates = lexicon.getVerbsByIntent(goal.intent, emotion);
        if (candidates.isEmpty()) {
            // إذا لم نجد، نأخذ من المعجم (أفعال عامة)
            candidates = lexicon.getDefaultVerbs();
        }

        // اختيار عشوائي مع تفضيل الكلمات ذات القيمة العاطفية العالية
        return selectWeighted(candidates, emotion, values);
    }

    private String selectSubject(DiscourseGoal goal) {
        if (goal.subjectType == SubjectType.SELF) return "أنا";
        if (goal.subjectType == SubjectType.USER) return "أنت";
        if (goal.subjectType == SubjectType.TOPIC && goal.topic != null) return goal.topic;
        return "هذا";
    }

    private String selectObject(DiscourseGoal goal, ValueSystem values,
                                 AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        // إذا كان الهدف يشير إلى مفعول به معين، نستخدمه
        if (goal.objectTopic != null) return goal.objectTopic;

        // نبحث عن اسم في رسالة المستخدم
        if (userAnalysis != null && !userAnalysis.getNouns().isEmpty()) {
            return userAnalysis.getNouns().get(random.nextInt(userAnalysis.getNouns().size()));
        }

        // وإلا نستخدم الموضوع الأساسي
        if (goal.topic != null && random.nextBoolean()) return goal.topic;

        return null;
    }

    private String selectNoun(String topic, SubjectType type, AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        if (topic != null) return topic;
        if (userAnalysis != null && !userAnalysis.getNouns().isEmpty()) {
            return userAnalysis.getNouns().get(0);
        }
        return "الأمر";
    }

    private String selectPredicate(DiscourseGoal goal, EmotionalState emotion, ValueSystem values,
                                    AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        // يمكن أن يكون خبرًا: صفة، اسم، جملة فعلية قصيرة
        if (random.nextBoolean()) {
            return selectAdjective(goal.topic, emotion, values);
        } else {
            // اسم مع حرف جر
            String noun = (userAnalysis != null && !userAnalysis.getNouns().isEmpty())
                    ? userAnalysis.getNouns().get(0) : "ذلك";
            return "مهم بالنسبة لي";
        }
    }

    private String selectAdjective(String topic, EmotionalState emotion, ValueSystem values) {
        List<String> adjectives = lexicon.getAdjectivesByEmotion(emotion);
        if (adjectives.isEmpty()) adjectives = lexicon.getDefaultAdjectives();
        return selectWeighted(adjectives, emotion, values);
    }

    private String selectResult(DiscourseGoal goal, EmotionalState emotion, ValueSystem values,
                                 AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        String topic = (goal.topic != null) ? goal.topic : "ذلك";
        return "سأفكر في " + topic;
    }

    private String selectPrepositionalPhrase(DiscourseGoal goal, AdvancedArabicLexicon.TextAnalysis userAnalysis) {
        // يمكن تطويره لاحقًا
        return null;
    }

    /**
     * اختيار عشوائي مع وزن بناءً على القيم العاطفية (محاكاة بسيطة)
     */
    private String selectWeighted(List<String> candidates, EmotionalState emotion, ValueSystem values) {
        if (candidates.isEmpty()) return "";
        // تبسيط: نختار عشوائيًا
        return candidates.get(random.nextInt(candidates.size()));
    }

    // ======================== التعلم ========================

    public static class LearningMechanism {
        public void learnFromUserMessage(AdvancedArabicLexicon.TextAnalysis analysis, Lexicon lexicon, ValueSystem values) {
            // 1. تعلم كلمات جديدة
            for (AdvancedArabicLexicon.WordAnalysis word : analysis.getWords()) {
                String norm = word.getNormalizedWord();
                if (!lexicon.contains(norm) && word.getConfidence() > 0.6) {
                    // كلمة جديدة، نضيفها إلى القاموس
                    String type = inferType(word.getCategory());
                    lexicon.addWord(norm, type);
                    Log.d(TAG, "Learned new word: " + norm + " (" + type + ")");
                }
            }

            // 2. تحديث القيم العاطفية (بسيط)
            // يمكن تحسينه لاحقًا
        }

        private String inferType(AdvancedArabicLexicon.WordCategory cat) {
            if (cat.name().contains("NOUN")) return "اسم";
            if (cat.name().contains("VERB")) return "فعل";
            if (cat.name().contains("ADJECTIVE")) return "صفة";
            return "أخرى";
        }
    }

    // ======================== الفئات الداخلية ========================

    public static class DiscourseGoal {
        Intent intent;
        Tone tone;
        SubjectType subjectType;
        String topic;
        String objectTopic;
        String location;
        float intensity;

        public DiscourseGoal(Intent intent) {
            this.intent = intent;
            this.tone = Tone.NEUTRAL;
            this.subjectType = SubjectType.SELF;
        }
    }

    public enum Intent {
        EXPRESS_FEELING, ASK_QUESTION, SHARE_MEMORY, IMAGINE, COMMENT_ON_TOPIC, GREET, UNKNOWN
    }

    public enum Tone { NEUTRAL, QUESTION, EXCITED, SAD, ANGRY, CURIOUS }
    public enum SubjectType { SELF, USER, TOPIC }
    public enum SentenceType { VERBAL, NOMINAL, CONDITIONAL, PHRASE }

    // ======================== القاموس (يتعلم) ========================

    public static class Lexicon {
        private Map<String, WordInfo> words = new HashMap<>();
        private List<String> defaultVerbs = new ArrayList<>();
        private List<String> defaultAdjectives = new ArrayList<>();

        public Lexicon() {
            // تهيئة أولية بسيطة (يمكن أن تبدأ فارغة)
            defaultVerbs.add("أحب");
            defaultVerbs.add("أكره");
            defaultVerbs.add("أفكر");
            defaultVerbs.add("أتساءل");
            defaultVerbs.add("أشعر");
            defaultVerbs.add("أريد");

            defaultAdjectives.add("جميل");
            defaultAdjectives.add("مثير");
            defaultAdjectives.add("غريب");
            defaultAdjectives.add("رائع");
            defaultAdjectives.add("صعب");

            // بعض الكلمات الأساسية
            addWord("أنا", "ضمير");
            addWord("أنت", "ضمير");
            addWord("حب", "اسم");
            addWord("خوف", "اسم");
        }

        public void addWord(String word, String type) {
            words.put(word, new WordInfo(word, type));
        }

        public boolean contains(String word) {
            return words.containsKey(word);
        }

        public List<String> getVerbsByIntent(Intent intent, EmotionalState emotion) {
            List<String> result = new ArrayList<>();
            for (WordInfo w : words.values()) {
                if ("فعل".equals(w.type)) {
                    // يمكن إضافة منطق لاحقًا
                    result.add(w.word);
                }
            }
            return result;
        }

        public List<String> getAdjectivesByEmotion(EmotionalState emotion) {
            List<String> result = new ArrayList<>();
            for (WordInfo w : words.values()) {
                if ("صفة".equals(w.type)) {
                    result.add(w.word);
                }
            }
            return result;
        }

        public List<String> getDefaultVerbs() { return defaultVerbs; }
        public List<String> getDefaultAdjectives() { return defaultAdjectives; }

        private static class WordInfo {
            String word;
            String type; // اسم، فعل، صفة، حرف، ضمير...
            WordInfo(String word, String type) { this.word = word; this.type = type; }
        }
    }

    // ======================== القواعد النحوية ========================

    public static class GrammarRules {
        public SentenceType selectSentenceType(DiscourseGoal goal) {
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
        private Random random = new Random();

        public DiscourseGoal planResponse(AdvancedArabicLexicon.TextAnalysis analysis,
                                           EmotionalState emotion, String dominantDesire,
                                           List<EpisodicMemory.EventEntity> recentMemories,
                                           ValueSystem values) {
            Intent intent = Intent.UNKNOWN;
            Tone tone = Tone.NEUTRAL;
            String topic = null;

            // هل هي سؤال؟
            if (analysis.isQuestion()) {
                intent = Intent.ASK_QUESTION;
                tone = Tone.QUESTION;
            } else {
                // إذا لم تكن سؤالاً، نحدد الهدف حسب الرغبة والعاطفة
                if (dominantDesire != null) {
                    if (dominantDesire.contains("bond")) {
                        intent = Intent.EXPRESS_FEELING;
                    } else if (dominantDesire.contains("explore")) {
                        intent = Intent.IMAGINE;
                    } else {
                        intent = Intent.COMMENT_ON_TOPIC;
                    }
                } else {
                    intent = Intent.COMMENT_ON_TOPIC;
                }
            }

            // استخراج الموضوع (أول اسم)
            List<String> nouns = analysis.getNouns();
            if (!nouns.isEmpty()) {
                topic = nouns.get(0);
            }

            // ضبط النبرة حسب العاطفة
            if (emotion != null) {
                if (emotion.isJoyful()) tone = Tone.EXCITED;
                else if (emotion.isSad()) tone = Tone.SAD;
                else if (emotion.isAfraid()) tone = Tone.SAD;
                else if (emotion.isCurious()) tone = Tone.CURIOUS;
            }

            DiscourseGoal goal = new DiscourseGoal(intent);
            goal.tone = tone;
            goal.topic = topic;
            goal.subjectType = SubjectType.SELF;
            return goal;
        }

        public DiscourseGoal planSpontaneous(EmotionalState emotion, String dominantDesire,
                                              List<EpisodicMemory.EventEntity> recentMemories,
                                              ValueSystem values) {
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
                goal.topic = event.narrative; // قد يكون طويلاً، نحتاج إلى اختصار
            } else {
                String[] concepts = {"الحياة", "الفضاء", "الأحلام", "المستقبل", "الذكاء", "الحب", "الوقت"};
                goal.topic = concepts[random.nextInt(concepts.length)];
            }

            return goal;
        }
    }
}

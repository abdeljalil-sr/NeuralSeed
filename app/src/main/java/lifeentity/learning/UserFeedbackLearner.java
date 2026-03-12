package com.lifeentity.learning;

import android.util.Log;

import com.lifeentity.core.EmotionalState;
import com.lifeentity.core.ValueSystem;
import com.lifeentity.language.AdvancedArabicLexicon;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.EpisodicMemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * نظام التعلم من ردود فعل المستخدم.
 * يقوم بتحليل رد المستخدم على كلام الكائن، ويستخدم ذلك لتحديث:
 * - قيم المفاهيم (ValueSystem)
 * - الروابط بين المفاهيم (في CompetitiveLearningCore)
 * - المعتقدات الذاتية (SelfModel)
 */
public class UserFeedbackLearner {
    private static final String TAG = "UserFeedbackLearner";

    // عتبات التقييم
    private static final float POSITIVE_THRESHOLD = 0.6f;
    private static final float NEGATIVE_THRESHOLD = -0.3f;
    private static final float LEARNING_RATE = 0.1f;

    private ValueSystem valueSystem;
    private CompetitiveLearningCore competitiveLearning;
    private AppDatabase database;

    // تخزين آخر كلام للكائن (لربطه برسالة المستخدم التالية)
    private String lastAgentUtterance = "";
    private long lastAgentTime = 0;

    public UserFeedbackLearner(ValueSystem valueSystem, CompetitiveLearningCore competitiveLearning, AppDatabase db) {
        this.valueSystem = valueSystem;
        this.competitiveLearning = competitiveLearning;
        this.database = db;
    }

    /**
     * يُستدعى عندما يتكلم الكائن
     */
    public void onAgentSpoke(String utterance) {
        lastAgentUtterance = utterance;
        lastAgentTime = System.currentTimeMillis();
    }

    /**
     * يُستدعى عندما يرسل المستخدم رسالة (بعد كلام الكائن مباشرة)
     * @param userMessage رسالة المستخدم الحالية
     * @param emotion الحالة العاطفية للكائن (قد تؤثر على التفسير)
     */
    public void onUserResponded(String userMessage, EmotionalState emotion) {
        // إذا مر وقت طويل، لا نعتبره رداً على الكلام السابق
        if (System.currentTimeMillis() - lastAgentTime > 10000) { // 10 ثوانٍ
            Log.d(TAG, "User response too late, ignoring for feedback");
            return;
        }

        // تحليل رسالة المستخدم باستخدام المعجم
        AdvancedArabicLexicon.TextAnalysis analysis = AdvancedArabicLexicon.analyze(userMessage);

        // استخراج الكلمات المهمة
        List<String> importantWords = extractImportantWords(analysis);

        // تقدير رد الفعل (إيجابي/سلبي/محايد)
        float feedbackScore = estimateFeedbackSentiment(analysis, emotion);

        // تحديث القيم بناءً على درجة رد الفعل والكلمات المهمة
        updateValues(importantWords, feedbackScore);

        // تسجيل الحدث في الذاكرة العرضية (للتذكر لاحقاً)
        saveFeedbackEvent(lastAgentUtterance, userMessage, feedbackScore);

        // تحديث الروابط التنافسية (اختياري)
        if (competitiveLearning != null) {
            updateCompetitiveLearning(importantWords, feedbackScore);
        }
    }

    /**
     * استخراج الكلمات المهمة من تحليل المعجم
     */
    private List<String> extractImportantWords(AdvancedArabicLexicon.TextAnalysis analysis) {
        List<String> important = new ArrayList<>();
        // نأخذ الأسماء والأفعال والصفات
        for (AdvancedArabicLexicon.WordAnalysis word : analysis.getWords()) {
            String cat = word.getCategory().name();
            if (cat.contains("NOUN") || cat.contains("VERB") || cat.contains("ADJECTIVE")) {
                important.add(word.getNormalizedWord());
            }
        }
        return important;
    }

    /**
     * تقدير درجة رد الفعل (إيجابي/سلبي) من تحليل النص
     */
    private float estimateFeedbackSentiment(AdvancedArabicLexicon.TextAnalysis analysis, EmotionalState emotion) {
        // قائمة كلمات إيجابية/سلبية بسيطة (يمكن تحسينها لاحقاً)
        Map<String, Float> sentimentLexicon = new HashMap<>();
        sentimentLexicon.put("نعم", 0.8f);
        sentimentLexicon.put("صحيح", 0.7f);
        sentimentLexicon.put("أجل", 0.8f);
        sentimentLexicon.put("لا", -0.8f);
        sentimentLexicon.put("خطأ", -0.7f);
        sentimentLexicon.put("كذبة", -0.9f);
        sentimentLexicon.put("جميل", 0.6f);
        sentimentLexicon.put("رائع", 0.8f);
        sentimentLexicon.put("سيء", -0.6f);
        sentimentLexicon.put("غبي", -0.7f);
        sentimentLexicon.put("ذكي", 0.6f);

        float totalScore = 0;
        int count = 0;

        for (AdvancedArabicLexicon.WordAnalysis word : analysis.getWords()) {
            String norm = word.getNormalizedWord();
            if (sentimentLexicon.containsKey(norm)) {
                totalScore += sentimentLexicon.get(norm);
                count++;
            }
        }

        if (count > 0) {
            return totalScore / count;
        }

        // إذا لم نجد كلمات واضحة، نعتمد على الحالة العاطفية للكائن
        // (افتراض أن المستخدم قد يؤثر على مشاعر الكائن)
        if (emotion != null) {
            return (emotion.getDopamine() - emotion.getCortisol()) * 0.3f;
        }

        float emotionalValence = (float)(emotion.getDopamine() - emotion.getCortisol());

        return emotionalValence * 0.3f;
        return 0;
    }

    /**
     * تحديث قيم المفاهيم بناءً على رد الفعل
     */
    private void updateValues(List<String> importantWords, float feedbackScore) {
        for (String word : importantWords) {
            // إذا كانت الكلمة موجودة في نظام القيم، نحدثها
            float current = valueSystem.getValue(word);
            // الهدف: إذا كان رد الفعل إيجابياً، نعزز الكلمة، وإلا نضعفها
            float delta = feedbackScore * LEARNING_RATE;
            valueSystem.learnValue(word, delta);
            Log.d(TAG, "Updated value for '" + word + "': " + current + " -> " + valueSystem.getValue(word));
        }
    }

    /**
     * حفظ الحدث في الذاكرة العرضية
     */
    private void saveFeedbackEvent(String agentUtterance, String userResponse, float feedbackScore) {
        if (database == null) return;
        try {
            EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
            event.timestamp = System.currentTimeMillis();
            event.narrative = "الكائن: " + agentUtterance + " | المستخدم: " + userResponse;
            event.emotionalState = feedbackScore > 0.3f ? "joy" : (feedbackScore < -0.3f ? "fear" : "neutral");
            event.emotionalIntensity = Math.abs(feedbackScore);
            event.location = "feedback";
            database.memoryDao().insertEvent(event);
        } catch (Exception e) {
            Log.e(TAG, "Error saving feedback event", e);
        }
    }

    /**
     * تحديث نظام التعلم التنافسي (اختياري)
     */
    private void updateCompetitiveLearning(List<String> importantWords, float feedbackScore) {
        // يمكن ربط الكلمات المهمة مع بعضها البعض حسب درجة التفاعل
        for (int i = 0; i < importantWords.size(); i++) {
            for (int j = i + 1; j < importantWords.size(); j++) {
                // تعزيز الرابط بين الكلمتين إذا كان التفاعل إيجابياً، إضعاف إذا كان سلبياً
                // هذا يتطلب واجهة من CompetitiveLearningCore تسمح بتحديث الروابط
                // competitiveLearning.updateAssociation(importantWords.get(i), importantWords.get(j), feedbackScore);
            }
        }
    }
}

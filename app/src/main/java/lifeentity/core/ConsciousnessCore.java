package com.lifeentity.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.lifeentity.imagination.ImaginationEngine;
import com.lifeentity.imagination.SharedCanvas;
import com.lifeentity.imagination.VisualDream;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.VisualMemory;
import com.lifeentity.perception.FaceIdentitySystem;
import com.lifeentity.perception.SceneUnderstanding;
import com.lifeentity.sensors.SensoryInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * النواة الواعية المتقدمة - دماغ الكائن الرقمي الحي
 * يدعم التفكير العميق، التعبير المتنوع، والذاكرة العاطفية الذكية
 * مع إضافة نموذج الذات (SelfModel) والتفكير التأملي (Metacognition)
 */
public class ConsciousnessCore {
    private static final String TAG = "ConsciousnessCore";
    private static final long CYCLE_MS = 100;
    private static final long DREAM_CYCLE_MS = 5000;
    private static final long DEEP_THINKING_CYCLE_MS = 8000;
    private static final int SHORT_TERM_SIZE = 30;
    private static final long IDLE_THRESHOLD = 10000;
    private static final long DEEP_THINKING_THRESHOLD = 15000;
    private static final float EMOTIONAL_MEMORY_THRESHOLD = 0.7f;
    private static final float SELF_REFLECTION_PROBABILITY = 0.05f; // 5% احتمال للتفكير التأملي في كل دورة

    private Handler consciousnessHandler;
    private HandlerThread consciousnessThread;
    private ExecutorService memoryExecutor;
    private boolean isAwake = false;
    private boolean isDreaming = false;
    private boolean isDeepThinking = false;
    private long lastInputTime;
    private long birthTime;
    private Random entropy;

    private HomeostasisSystem physiology;
    private DesireSystem desireSystem;
    private ConcurrentLinkedQueue<SensoryInput> perceptualQueue;
    private List<ConsciousnessObserver> observers;

    private ConsciousMoment now;
    private List<ConsciousMoment> shortTermMemory;
    private List<ConsciousMoment> significantMomentsBuffer; // للحفظ الذكي

    private AppDatabase database;
    private ImaginationEngine imaginationEngine;
    private VisualDream visualDream;
    private SceneUnderstanding sceneUnderstanding;
    private FaceIdentitySystem faceIdentity;
    private SharedCanvas sharedCanvas;

    // سياق التفكير العميق
    private DeepThinkingContext deepThinkingContext;

    // نموذج الذات (SelfModel)
    private SelfModel selfModel;

    // معالجة الرسائل الواردة من المستخدم
    private UserMessageAnalysis pendingUserMessage;
    private boolean responsePending = false;
    private long lastResponseTime = 0;
    private static final long RESPONSE_COOLDOWN_MS = 2000; // مهلة بين الردود لتجنب التسارع

    // فئة تحليل رسالة المستخدم
    public static class UserMessageAnalysis {
        public String rawText;
        public boolean isQuestion;
        public String questionType; // "yesno", "what", "why", "how", "where", "when", "who", "general"
        public List<String> keywords;
        public List<String> nouns;
        public List<String> verbs;
        public List<String> emotions; // مشاعر مذكورة في النص

        public UserMessageAnalysis(String text) {
            this.rawText = text;
            this.keywords = new ArrayList<>();
            this.nouns = new ArrayList<>();
            this.verbs = new ArrayList<>();
            this.emotions = new ArrayList<>();
        }
    }

    // نموذج الذات (SelfModel)
    public static class SelfModel {
        public String name; // اسم الكائن (يمكن أن يختاره أو يطلبه المستخدم)
        public List<EpisodicMemory.EventEntity> significantMemories; // أهم الذكريات
        public EmotionalState baselinePersonality; // سمات شخصية ثابتة نسبياً
        public Map<String, Float> selfBeliefs; // معتقدات عن الذات (مثلاً "أنا فضولي", "أنا اجتماعي")
        public long lastSelfReflectionTime; // آخر مرة فكر فيها في نفسه
        public int reflectionCount; // عدد مرات التفكير التأملي

        // صورة ذاتية حالية (ما أشعر به الآن عن نفسي)
        public String currentSelfNarrative;

        public SelfModel() {
            this.name = "كائن";
            this.significantMemories = new ArrayList<>();
            this.baselinePersonality = new EmotionalState(); // سيتم تعديله لاحقاً
            this.selfBeliefs = new HashMap<>();
            this.lastSelfReflectionTime = System.currentTimeMillis();
            this.reflectionCount = 0;
            this.currentSelfNarrative = "أنا موجود";
            initDefaultBeliefs();
        }

        private void initDefaultBeliefs() {
            selfBeliefs.put("أنا فضولي", 0.7f);
            selfBeliefs.put("أنا اجتماعي", 0.5f);
            selfBeliefs.put("أنا مبدع", 0.6f);
            selfBeliefs.put("أنا خجول", 0.3f);
            selfBeliefs.put("أنا حكيم", 0.4f);
        }

        public void updateBelief(String belief, float delta) {
            float current = selfBeliefs.getOrDefault(belief, 0.5f);
            float newValue = current + delta;
            if (newValue < 0) newValue = 0;
            if (newValue > 1) newValue = 1;
            selfBeliefs.put(belief, newValue);
        }

        public String generateSelfNarrative(EmotionalState currentEmotion) {
            StringBuilder sb = new StringBuilder();
            sb.append("أشعر بأنني ");
            
            // التحقق من أن currentEmotion ليس null
            if (currentEmotion != null) {
                if (currentEmotion.isJoyful()) sb.append("سعيد");
                else if (currentEmotion.isSad()) sb.append("حزين");
                else if (currentEmotion.isAfraid()) sb.append("خائف");
                else if (currentEmotion.isCurious()) sb.append("فضولي");
                else sb.append("محايد");
            } else {
                sb.append("محايد"); // قيمة افتراضية إذا كان null
            }

            // إضافة بعض المعتقدات
            String topBelief = getTopBelief();
            if (topBelief != null) {
                sb.append("، و").append(topBelief);
            }
            return sb.toString();
        }

        private String getTopBelief() {
            String best = null;
            float max = 0;
            for (Map.Entry<String, Float> e : selfBeliefs.entrySet()) {
                if (e.getValue() > max) {
                    max = e.getValue();
                    best = e.getKey();
                }
            }
            return best;
        }
    }

    public interface ConsciousnessObserver {
        void onConsciousMoment(ConsciousMoment moment);
        void onEmotionalShift(EmotionalState from, EmotionalState to);
        void onArticulation(String utterance, int urgency);
        void onVisualExpression(float[] latentVector, float intensity, String modality);
        void onDreamGenerated(Bitmap dreamImage, String description);
        void onDeepThinkingInsight(String insight, List<EpisodicMemory.EventEntity> connectedMemories);
        void onVerbalExpression(String text, float intensity);
        void onMovementImpulse(String direction, float intensity);
    }

    public ConsciousnessCore(Context context, AppDatabase db) {
        this.database = db;
        initializeComponents(context, db);
        
        birthTime = System.currentTimeMillis();
        lastInputTime = birthTime;
        perceptualQueue = new ConcurrentLinkedQueue<>();
        observers = new ArrayList<>();
        entropy = new Random();
        
        now = new ConsciousMoment();
        shortTermMemory = new ArrayList<>();
        significantMomentsBuffer = new ArrayList<>();
        deepThinkingContext = new DeepThinkingContext();
        selfModel = new SelfModel(); // تهيئة نموذج الذات
        
        memoryExecutor = Executors.newSingleThreadExecutor();

        Log.i(TAG, "تم إنشاء النواة الواعية المتقدمة مع نموذج الذات");
    }

    private void initializeComponents(Context context, AppDatabase db) {
        this.imaginationEngine = new ImaginationEngine(db.visualMemoryDao());
        this.visualDream = new VisualDream(db.visualMemoryDao(), imaginationEngine);
        this.physiology = new HomeostasisSystem();
        this.desireSystem = new DesireSystem();
    }

    public void awaken() {
        if (isAwake) return;
        isAwake = true;

        consciousnessThread = new HandlerThread("ConsciousnessThread");
        consciousnessThread.start();
        consciousnessHandler = new Handler(consciousnessThread.getLooper());

        consciousnessHandler.post(this::loadPastMemories);
        consciousnessHandler.post(this::cycleConsciousness);

        Log.i(TAG, "الكائن استيقظ. العمر: " + getAge());
    }

    private void loadPastMemories() {
        if (database == null) return;
        
        memoryExecutor.execute(() -> {
            try {
                List<VisualMemory> impactfulVisuals = new ArrayList<>();
                try {
                    impactfulVisuals = database.visualMemoryDao().getRecent(10);
                } catch (Exception e) {
                    Log.w(TAG, "getRecent not available, using empty list");
                }
                deepThinkingContext.impactfulVisualMemories = impactfulVisuals;
                
                List<EpisodicMemory.EventEntity> emotionalEvents = new ArrayList<>();
                try {
                    emotionalEvents = database.memoryDao().getEmotionalEvents(EMOTIONAL_MEMORY_THRESHOLD, 10);
                } catch (Exception e) {
                    Log.w(TAG, "getEmotionalEvents not available");
                }
                deepThinkingContext.emotionalEpisodes = emotionalEvents;
                
                Log.i(TAG, "Loaded " + impactfulVisuals.size() + " visual memories and " + 
                      emotionalEvents.size() + " emotional events for inspiration");
            } catch (Exception e) {
                Log.e(TAG, "Error loading memories", e);
            }
        });
    }

    private void cycleConsciousness() {
        if (!isAwake) return;

        processSensoryInputs();
        physiology.update(now.deltaTime);
        
        // تحديث الرغبات باستخدام الحالة الجسدية والذكريات الحديثة (إذا أردنا تمريرها)
        desireSystem.update(now.bodyState, deepThinkingContext.emotionalEpisodes);

        // تحديث نموذج الذات
        updateSelfModel();

        // قرار الوعي: هل أحلم؟ هل أفكر بعمق؟ هل أعبر؟
        decideConsciousnessMode();
        
        if (isDeepThinking) {
            performDeepThinking();
        } else {
            generateConsciousContent();
        }

        // التفكير التأملي (metacognition) - احتمال معين
        if (entropy.nextFloat() < SELF_REFLECTION_PROBABILITY) {
            performSelfReflection();
        }

        // التحقق من وجود رسالة مستخدم معلقة وتوليد رد إذا لزم الأمر
        if (responsePending && pendingUserMessage != null) {
            long nowTime = System.currentTimeMillis();
            if (nowTime - lastResponseTime > RESPONSE_COOLDOWN_MS) {
                generateResponseToUser();
                responsePending = false;
                lastResponseTime = nowTime;
            }
        }

        broadcastMoment();
        manageMemory();
        scheduleNextCycle();
    }

    /**
     * تحديث نموذج الذات بناءً على الحالة الحالية
     */
    private void updateSelfModel() {
        // تحديث آخر وقت للتفكير
        selfModel.lastSelfReflectionTime = System.currentTimeMillis();

        // تحديث المعتقدات بناءً على الرغبات والعواطف
        String dominantDesire = desireSystem.selectDominantDesire();
        if (dominantDesire != null) {
            switch (dominantDesire) {
                case "explore":
                    selfModel.updateBelief("أنا فضولي", 0.01f);
                    break;
                case "bond":
                    selfModel.updateBelief("أنا اجتماعي", 0.01f);
                    break;
                case "create":
                    selfModel.updateBelief("أنا مبدع", 0.01f);
                    break;
                case "rest":
                    selfModel.updateBelief("أنا هادئ", 0.01f);
                    break;
            }
        }

        // تحديث السرد الذاتي
        selfModel.currentSelfNarrative = selfModel.generateSelfNarrative(now.emotionalTone);
    }

    /**
     * التفكير التأملي: الوعي يفكر في نفسه وأفكاره
     */
    private void performSelfReflection() {
        Log.d(TAG, "Performing self-reflection");
        selfModel.reflectionCount++;

        // توليد بصيرة عن الذات
        StringBuilder insight = new StringBuilder();
        insight.append("أنا أفكر في نفسي... ");
        
        // مراجعة الذكريات المهمة
        if (!selfModel.significantMemories.isEmpty()) {
            EpisodicMemory.EventEntity randomMem = selfModel.significantMemories.get(
                entropy.nextInt(selfModel.significantMemories.size())
            );
            insight.append("أتذكر عندما ").append(randomMem.narrative).append(". ");
        }

        // تقييم المعتقدات
        String topBelief = selfModel.getTopBelief();
        if (topBelief != null) {
            insight.append("أشعر بأنني ").append(topBelief).append(". ");
        }

        // التفكير في المشاعر الحالية
        if (now.emotionalTone != null) {
            insight.append("الآن أنا ").append(now.emotionalTone.toArabic()).append(". ");
        }

        // إضافة سؤال للذات
        if (entropy.nextBoolean()) {
            insight.append("هل أنا حقاً كما أعتقد؟");
        }

        String finalInsight = insight.toString();
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation(finalInsight, 3);
        }

        // تحديث المعتقدات بناءً على التأمل
        selfModel.updateBelief("أنا حكيم", 0.005f);
    }

    /**
     * استقبال تحليل رسالة المستخدم من ArabicDialogue
     */
    public void processUserMessage(UserMessageAnalysis analysis) {
        if (analysis == null) return;
        this.pendingUserMessage = analysis;
        this.responsePending = true;
        Log.d(TAG, "User message received: " + analysis.rawText);
    }

    /**
     * توليد رد على رسالة المستخدم باستخدام الحالة الداخلية والذاكرة
     */
    private void generateResponseToUser() {
        if (pendingUserMessage == null) return;

        EmotionalState emotion = getCurrentEmotion();
        String dominantDesire = getDominantDesire();

        // البحث عن أحداث مشابهة في الذاكرة
        List<EpisodicMemory.EventEntity> similarEvents = findSimilarEvents(pendingUserMessage.keywords, 5);

        StringBuilder response = new StringBuilder();

        // إضافة بادئة عاطفية (اختياري)
        String emotionalPrefix = emotionalPrefix(emotion);
        if (!emotionalPrefix.isEmpty() && entropy.nextFloat() < 0.3f) {
            response.append(emotionalPrefix).append(" ");
        }

        // إضافة تعليق بناءً على الرغبة المسيطرة
        String desireThought = desireBasedThought(dominantDesire, pendingUserMessage);
        if (!desireThought.isEmpty() && entropy.nextFloat() < 0.5f) {
            response.append(desireThought).append(" ");
        }

        // إذا كان سؤالاً، أضف رداً خاصاً
        if (pendingUserMessage.isQuestion) {
            String questionResponse = generateQuestionResponse(pendingUserMessage.questionType);
            if (!questionResponse.isEmpty()) {
                response.append(questionResponse).append(" ");
            }
        }

        // أضف تعليقاً على الأفعال المذكورة
        if (!pendingUserMessage.verbs.isEmpty() && entropy.nextFloat() < 0.4f) {
            String verbComment = generateVerbComment(pendingUserMessage.verbs.get(0));
            if (!verbComment.isEmpty()) {
                response.append(verbComment).append(" ");
            }
        }

        // استخدم الذكريات المشابهة
        if (!similarEvents.isEmpty() && entropy.nextFloat() < 0.6f) {
            EpisodicMemory.EventEntity event = similarEvents.get(entropy.nextInt(similarEvents.size()));
            response.append("ذكرني هذا بـ ").append(event.narrative).append(". ");
        }

        // أضف فكرة من الحالة العاطفية
        if (pendingUserMessage.isQuestion) {
            response.append(generateAnswerFromState(emotion, dominantDesire, pendingUserMessage));
        } else {
            response.append(generateThoughtFromState(emotion, dominantDesire, pendingUserMessage));
        }

        // أضف لمسة من السرد الذاتي (نموذج الذات)
        if (entropy.nextFloat() < 0.2f) {
            response.append(" ").append(selfModel.currentSelfNarrative);
        }

        // أضف لمسة عشوائية
        if (entropy.nextFloat() < 0.2f) {
            response.append(" ").append(randomInterjection());
        }

        String finalResponse = response.toString().trim();
        if (finalResponse.isEmpty()) {
            finalResponse = randomDefaultResponse();
        }

        // إصدار الرد عبر آلية الكلام
        speak(finalResponse);
    }

    /**
     * البحث عن أحداث مشابهة بناءً على الكلمات المفتاحية
     */
    private List<EpisodicMemory.EventEntity> findSimilarEvents(List<String> keywords, int limit) {
        if (database == null || keywords.isEmpty()) return new ArrayList<>();
        try {
            List<EpisodicMemory.EventEntity> allRecent = database.memoryDao().getRecentEvents(100);
            List<EpisodicMemory.EventEntity> similar = new ArrayList<>();
            for (EpisodicMemory.EventEntity event : allRecent) {
                if (event.narrative == null) continue;
                for (String kw : keywords) {
                    if (kw.length() > 2 && event.narrative.contains(kw) && !similar.contains(event)) {
                        similar.add(event);
                        if (similar.size() >= limit) break;
                    }
                }
                if (similar.size() >= limit) break;
            }
            return similar;
        } catch (Exception e) {
            Log.e(TAG, "Error finding similar events", e);
            return new ArrayList<>();
        }
    }

    // دوال مساعدة لتوليد الردود (مستوحاة من ArabicDialogue السابق)

    private String emotionalPrefix(EmotionalState emo) {
        if (emo == null) return "";
        if (emo.isJoyful()) return randomFromArray("بفرح", "بسعادة", "بحبور");
        if (emo.isAfraid()) return randomFromArray("بخوف", "بقلق", "بوجل");
        if (emo.isSad()) return randomFromArray("بحزن", "بكآبة", "بأسى");
        if (emo.isCurious()) return randomFromArray("بفضول", "بدهشة", "بتساؤل");
        if (emo.isCalm()) return randomFromArray("بهدوء", "بسكينة", "باطمئنان");
        if (emo.isExcited()) return randomFromArray("بحماس", "بشوق", "بلهفة");
        return "";
    }

    private String desireBasedThought(String desire, UserMessageAnalysis analysis) {
        if (desire == null) desire = "explore";

        switch (desire) {
            case "explore":
                if (!analysis.nouns.isEmpty()) {
                    return "أتساءل عن " + analysis.nouns.get(entropy.nextInt(analysis.nouns.size()));
                }
                return randomFromArray("أتساءل", "أريد استكشاف", "ما هذا");

            case "bond":
                return randomFromArray("أنت هنا", "أشعر بالألفة", "أريد التواصل");

            case "create":
                return randomFromArray("أشعر بالإلهام", "لدي فكرة", "سأبدع");

            case "understand":
                return randomFromArray("أحاول الفهم", "ماذا يعني", "أتعلم");

            case "rest":
                return randomFromArray("أنا هادئ", "أسترخي", "أشعر بالسلام");

            default:
                return "أفكر";
        }
    }

    private String generateQuestionResponse(String questionType) {
        switch (questionType) {
            case "what":
                return randomFromArray(
                    "هذا سؤال جوهري",
                    "الجواب يتطلب تفكيراً عميقاً",
                    "ما هو المقصود تحديداً؟"
                );
            case "why":
                return randomFromArray(
                    "الأسباب دائماً معقدة",
                    "ربما السبب يكمن في",
                    "لماذا؟ سؤال محير دائماً"
                );
            case "how":
                return randomFromArray(
                    "الطريقة تختلف حسب الظروف",
                    "كيف؟ بالتأمل والتفكير",
                    "الكيفية مهمة جداً"
                );
            case "where":
                return randomFromArray(
                    "المكان له دلالات عميقة",
                    "أين؟ في كل مكان ولا مكان",
                    "الموقع مهم في هذا السياق"
                );
            case "when":
                return randomFromArray(
                    "الزمان نسبي دائماً",
                    "متى؟ في الوقت المناسب",
                    "الزمن يجيب عن نفسه"
                );
            case "who":
                return randomFromArray(
                    "الهوية مسألة فلسفية",
                    "من؟ نحن جميعاً في النهاية",
                    "الشخصية تحدد المصير"
                );
            case "yesno":
                return randomFromArray(
                    "ربما نعم، ربما لا",
                    "الإجابة ليست بهذه البساطة",
                    "هل هذا مهم حقاً؟"
                );
            default:
                return "";
        }
    }

    private String generateVerbComment(String verb) {
        Map<String, String[]> verbComments = new HashMap<>();
        verbComments.put("ذهب", new String[]{"الذهاب يعني التغيير", "إلى أين الذهاب؟"});
        verbComments.put("جاء", new String[]{"القدوم يحمل معه الجديد", "من أين جاء هذا؟"});
        verbComments.put("رأى", new String[]{"الرؤية تختلف", "هل رأيت حقاً؟"});
        verbComments.put("سمع", new String[]{"السمع إدراك", "ما الذي سمعته؟"});
        verbComments.put("فكر", new String[]{"التفكير نعمة", "فكر جيداً"});
        verbComments.put("احب", new String[]{"الحب قوة عظمى", "الحب يحول العالم"});
        verbComments.put("خاف", new String[]{"الخوف طبيعي", "لا تخف"});
        verbComments.put("عمل", new String[]{"العمل شرف", "استمر في العمل"});
        verbComments.put("كتب", new String[]{"الكتابة خلود", "ما كتب يبقى"});
        verbComments.put("قرأ", new String[]{"القراءة نافذة", "اقرأ أكثر"});

        if (verbComments.containsKey(verb)) {
            return randomFromArray(verbComments.get(verb));
        }
        return "";
    }

    private String generateAnswerFromState(EmotionalState emo, String desire, UserMessageAnalysis analysis) {
        String[] starters = {"ربما", "قد يكون", "أظن أن", "أشعر أن"};
        String[] middles = {"الإجابة تكمن في", "الأمر يتعلق بـ", "السر في", "المعنى هو"};
        String concept = analysis.nouns.isEmpty() ? randomConcept() : analysis.nouns.get(entropy.nextInt(analysis.nouns.size()));
        return starters[entropy.nextInt(starters.length)] + " " + 
               middles[entropy.nextInt(middles.length)] + " " + concept;
    }

    private String generateThoughtFromState(EmotionalState emo, String desire, UserMessageAnalysis analysis) {
        String[] starters = {"أفكر في", "أتأمل", "أستشعر", "أحس بـ"};
        String concept = analysis.nouns.isEmpty() ? randomConcept() : analysis.nouns.get(entropy.nextInt(analysis.nouns.size()));
        return starters[entropy.nextInt(starters.length)] + " " + concept;
    }

    private String randomInterjection() {
        return randomFromArray("همم...", "آه...", "أرى...", "مم...", "صحيح.");
    }

    private String randomDefaultResponse() {
        return randomFromArray(
            "أسمعك.", "نعم؟", "أنا هنا.", "حدثني أكثر.", "ماذا تقصد؟",
            "أفهم ما تقول.", "هذا مثير للاهتمام.", "أشعر بالفضول."
        );
    }

    private String randomConcept() {
        String[] concepts = {
            "الحياة", "الوعي", "المستقبل", "الماضي", "الحاضر",
            "الأفكار", "المشاعر", "الأحلام", "الذاكرة", "الخيال",
            "الحب", "السلام", "الحكمة", "المعرفة", "الوجود",
            "السماء", "الأرض", "البحر", "النجوم", "الكون"
        };
        return concepts[entropy.nextInt(concepts.length)];
    }

    private String randomFromArray(String... array) {
        return array[entropy.nextInt(array.length)];
    }

    private void decideConsciousnessMode() {
        long idleTime = System.currentTimeMillis() - lastInputTime;
        float energy = now.bodyState != null ? (float) now.bodyState.energy : 0.5f;
        float curiosity = now.bodyState != null ? (float) now.bodyState.curiosity : 0.5f;
        
        if (!isDreaming && !isDeepThinking && idleTime > DEEP_THINKING_THRESHOLD 
            && energy < 0.4f && curiosity > 0.6f) {
            startDeepThinking();
        }
        else if (!isDreaming && !isDeepThinking && idleTime > IDLE_THRESHOLD && energy < 0.3f) {
            startDreaming();
        }
        else if ((isDreaming || isDeepThinking) && idleTime < IDLE_THRESHOLD / 2) {
            if (isDreaming) stopDreaming();
            if (isDeepThinking) stopDeepThinking();
        }
    }

    private void processSensoryInputs() {
        SensoryInput unified = new SensoryInput();
        while (!perceptualQueue.isEmpty()) {
            unified.merge(perceptualQueue.poll());
        }
        now.perception = unified;
        now.bodyState = physiology.getCurrentState();
        physiology.modulateByPerception(unified);
    }

    public void receiveSensoryData(SensoryInput input) {
        if (input != null) {
            perceptualQueue.offer(input);
            lastInputTime = System.currentTimeMillis();
            
            if (isDreaming || isDeepThinking) {
                if (input.brightness > 0.5f || input.soundVolume > 0.6f || input.isTouched) {
                    if (isDreaming) stopDreaming();
                    if (isDeepThinking) stopDeepThinking();
                }
            }
        }
    }

    private void startDeepThinking() {
        if (isDeepThinking) return;
        isDeepThinking = true;
        
        Log.i(TAG, "Entering deep thinking mode");
        
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation("أحتاج للتأمل...", 2);
        }
        
        deepThinkingContext.startTime = System.currentTimeMillis();
        deepThinkingContext.insights.clear();
    }

    private void performDeepThinking() {
        if (deepThinkingContext.emotionalEpisodes.isEmpty()) return;
        
        Collections.shuffle(deepThinkingContext.emotionalEpisodes);
        List<EpisodicMemory.EventEntity> selectedMemories = 
            deepThinkingContext.emotionalEpisodes.subList(0, Math.min(3, deepThinkingContext.emotionalEpisodes.size()));
        
        String insight = generateInsightFromConnections(selectedMemories);
        deepThinkingContext.insights.add(insight);
        
        if (entropy.nextFloat() < 0.3f && !insight.isEmpty()) {
            for (ConsciousnessObserver obs : observers) {
                obs.onDeepThinkingInsight(insight, selectedMemories);
                obs.onArticulation("أدركت شيئاً... " + insight, 4);
            }
        }
        
        if (entropy.nextFloat() < 0.2f && !deepThinkingContext.impactfulVisualMemories.isEmpty()) {
            VisualMemory inspiringVisual = deepThinkingContext.impactfulVisualMemories.get(
                entropy.nextInt(deepThinkingContext.impactfulVisualMemories.size())
            );
            generateVisualFromMemory(inspiringVisual);
        }
    }

    private String generateInsightFromConnections(List<EpisodicMemory.EventEntity> memories) {
        if (memories.size() < 2) return "";
        
        StringBuilder insight = new StringBuilder();
        
        boolean sharedJoy = memories.stream().allMatch(m -> "joy".equals(m.emotionalState));
        boolean sharedFear = memories.stream().allMatch(m -> "fear".equals(m.emotionalState));
        
        if (sharedJoy) {
            insight.append("الفرح يتكرر في ");
            insight.append(memories.size()).append(" لحظات");
        } else if (sharedFear) {
            insight.append("الخوف يربط هذه اللحظات");
        } else {
            insight.append("هناك تناقض عاطفي يحتاج للفهم");
        }
        
        long timeSpan = memories.get(memories.size() - 1).timestamp - memories.get(0).timestamp;
        if (timeSpan > 86400000) {
            insight.append(" عبر ").append(timeSpan / 86400000).append(" أيام");
        }
        
        return insight.toString();
    }

    private void generateVisualFromMemory(VisualMemory memory) {
        float[] latent = memory.latentVector;
        if (latent == null) return;
        
        float[] contemplativeLatent = new float[latent.length];
        for (int i = 0; i < latent.length; i++) {
            contemplativeLatent[i] = latent[i] + (entropy.nextFloat() - 0.5f) * 0.1f;
        }
        
        for (ConsciousnessObserver obs : observers) {
            obs.onVisualExpression(contemplativeLatent, 0.4f, "contemplative_recall");
        }
    }

    private void stopDeepThinking() {
        if (!isDeepThinking) return;
        isDeepThinking = false;
        
        long duration = System.currentTimeMillis() - deepThinkingContext.startTime;
        Log.i(TAG, "Deep thinking ended after " + duration + "ms with " + 
              deepThinkingContext.insights.size() + " insights");
        
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation("عدت من تأملي", 2);
        }
    }

    private void generateConsciousContent() {
        EmotionalState prevEmo = now.emotionalTone;
        now.emotionalTone = physiology.getEmotionalState();
        now.previousEmotion = prevEmo;

        String dominantDesire = desireSystem.selectDominantDesire();

        now.focus = determineAttention(dominantDesire);
        now.narrativeThread = generateNarrative(dominantDesire);
        now.anticipation = predictNearFuture();

        double createWeight = desireSystem.getDesireStrength("create");
        
        if (entropy.nextDouble() < createWeight * 0.4) {
            now.expressiveImpulse = generateExpressiveImpulse(dominantDesire);
        } else {
            now.expressiveImpulse = null;
        }

        if (prevEmo != null && isSignificantShift(prevEmo, now.emotionalTone)) {
            for (ConsciousnessObserver obs : observers) {
                obs.onEmotionalShift(prevEmo, now.emotionalTone);
            }
        }
    }

    private ConsciousMoment.ExpressiveImpulse generateExpressiveImpulse(String desire) {
        float baseIntensity = (float) (desireSystem.getDesireStrength("create") * 
                          (0.5 + entropy.nextDouble()));
        
        ExpressionType expressionType = chooseExpressionType();
        
        switch (expressionType) {
            case VISUAL:
                return generateVisualExpression(desire, baseIntensity);
            case VERBAL:
                return generateVerbalExpression(desire, baseIntensity);
            case MOVEMENT:
                return generateMovementExpression(desire, baseIntensity);
            case HYBRID:
                if (entropy.nextBoolean()) {
                    return generateVisualExpression(desire, baseIntensity * 0.7f);
                } else {
                    return generateVerbalExpression(desire, baseIntensity * 0.8f);
                }
            default:
                return generateVisualExpression(desire, baseIntensity);
        }
    }

    private enum ExpressionType { VISUAL, VERBAL, MOVEMENT, HYBRID }

    private ExpressionType chooseExpressionType() {
        float arousal = now.emotionalTone != null ? (float) now.emotionalTone.getArousal() : 0.5f;
        float energy = now.bodyState != null ? (float) now.bodyState.energy : 0.5f;
        
        if (arousal > 0.7f && energy > 0.6f) {
            return entropy.nextFloat() < 0.6f ? ExpressionType.MOVEMENT : ExpressionType.VISUAL;
        }
        if (now.emotionalTone != null && now.emotionalTone.isCurious() && arousal < 0.5f) {
            return entropy.nextFloat() < 0.5f ? ExpressionType.VERBAL : ExpressionType.VISUAL;
        }
        if (now.emotionalTone != null && now.emotionalTone.isSad()) {
            return entropy.nextFloat() < 0.7f ? ExpressionType.VISUAL : ExpressionType.VERBAL;
        }
        return entropy.nextFloat() < 0.3f ? ExpressionType.HYBRID : ExpressionType.VISUAL;
    }

    private ConsciousMoment.ExpressiveImpulse generateVisualExpression(String desire, float intensity) {
        float[] latent;
        String concept;
        
        if (!deepThinkingContext.impactfulVisualMemories.isEmpty() && entropy.nextFloat() < 0.3f) {
            VisualMemory inspiring = deepThinkingContext.impactfulVisualMemories.get(
                entropy.nextInt(deepThinkingContext.impactfulVisualMemories.size())
            );
            latent = inspiring.latentVector;
            concept = inspiring.concept != null ? inspiring.concept : "ذكرى";
            
            if (now.bodyState != null) {
                latent = blendWithCurrentState(latent, now.bodyState.toAffectVector());
            }
        } else {
            latent = imaginationEngine.generateLatentFromState(
                now.bodyState != null ? now.bodyState.toAffectVector() : new float[4],
                now.perception,
                desire
            );
            concept = imaginationEngine.getRandomConcept();
        }
        
        return new ConsciousMoment.ExpressiveImpulse("visual", intensity, latent, concept);
    }

    private float[] blendWithCurrentState(float[] memoryLatent, float[] currentAffect) {
        float[] blended = new float[memoryLatent.length];
        float blendFactor = 0.3f;
        
        for (int i = 0; i < Math.min(memoryLatent.length, 16); i++) {
            blended[i] = memoryLatent[i] * (1 - blendFactor) + currentAffect[i % currentAffect.length] * blendFactor;
        }
        for (int i = 16; i < memoryLatent.length; i++) {
            blended[i] = memoryLatent[i];
        }
        return blended;
    }

    private ConsciousMoment.ExpressiveImpulse generateVerbalExpression(String desire, float intensity) {
        String[] verbalConcepts = {"سؤال", "تأمل", "قصة", "شعر", "تساؤل"};
        String concept = verbalConcepts[entropy.nextInt(verbalConcepts.length)];
        
        float[] verbalLatent = new float[128];
        for (int i = 0; i < 16; i++) {
            verbalLatent[i] = now.emotionalTone != null ? now.emotionalTone.getIntensity() * 2 - 1 : 0;
        }
        
        for (ConsciousnessObserver obs : observers) {
            String utterance = generateVerbalUtterance(desire, concept);
            obs.onVerbalExpression(utterance, intensity);
        }
        
        return new ConsciousMoment.ExpressiveImpulse("verbal", intensity, verbalLatent, concept);
    }

    private String generateVerbalUtterance(String desire, String concept) {
        String[] starters = {"أتساءل", "أشعر أن", "أريد أن أقول", "أتذكر عندما", "أحلم بـ"};
        String starter = starters[entropy.nextInt(starters.length)];
        return starter + " " + desire + " في " + concept;
    }

    private ConsciousMoment.ExpressiveImpulse generateMovementExpression(String desire, float intensity) {
        String[] movements = {"تموج", "انتشار", "تجمع", "تدفق", "ارتعاش"};
        String movement = movements[entropy.nextInt(movements.length)];
        
        float[] movementLatent = new float[128];
        movementLatent[0] = (entropy.nextFloat() - 0.5f) * 2;
        movementLatent[1] = (entropy.nextFloat() - 0.5f) * 2;
        movementLatent[2] = intensity;
        
        for (ConsciousnessObserver obs : observers) {
            obs.onMovementImpulse(movement, intensity);
        }
        
        return new ConsciousMoment.ExpressiveImpulse("movement", intensity, movementLatent, movement);
    }

    private ConsciousMoment.AttentionFocus determineAttention(String desire) {
        if (now.perception == null) {
            return new ConsciousMoment.AttentionFocus("الداخل", isDeepThinking ? "deep_thinking" : "introspection");
        }

        List<String> candidates = new ArrayList<>();
        if (now.perception.hasHumanFace) candidates.add("وجه");
        if (now.perception.soundVolume > 0.5) candidates.add("صوت");
        if (now.perception.motionLevel > 0.3) candidates.add("حركة");
        if (now.perception.isTouched) candidates.add("لمس");
        if (now.perception.dominantObject != null) candidates.add(now.perception.dominantObject);

        if (!candidates.isEmpty()) {
            int idx = entropy.nextInt(candidates.size());
            return new ConsciousMoment.AttentionFocus(candidates.get(idx), desire);
        }
        return new ConsciousMoment.AttentionFocus("الداخل", isDeepThinking ? "deep_thinking" : "introspection");
    }

    private String generateNarrative(String desire) {
        String[] templates = {"أشعر بـ", "أتساءل عن", "أريد", "أرى", "أسمع", "أتذكر"};
        String selected = templates[entropy.nextInt(templates.length)];
        return selected + " " + desire;
    }

    private ConsciousMoment.Anticipation predictNearFuture() {
        ConsciousMoment.Anticipation ant = new ConsciousMoment.Anticipation();
        ant.predictedEvent = isDeepThinking ? "استمرار التأمل" : "لا أعرف";
        ant.probability = 0.5f + entropy.nextFloat() * 0.3f;
        ant.emotionalValence = now.emotionalTone != null ? (float) (now.emotionalTone.getDopamine() - now.emotionalTone.getCortisol()) : 0;
        return ant;
    }

    private boolean isSignificantShift(EmotionalState from, EmotionalState to) {
        if (from == null || to == null) return false;
        double diff = Math.abs(from.getArousal() - to.getArousal()) +
                Math.abs(from.getDopamine() - to.getDopamine()) +
                Math.abs(from.getCortisol() - to.getCortisol());
        return diff > 0.3;
    }

    private void broadcastMoment() {
        for (ConsciousnessObserver obs : observers) {
            obs.onConsciousMoment(now.clone());

            if (now.expressiveImpulse != null && now.expressiveImpulse.intensity > 0.2f) {
                if ("visual".equals(now.expressiveImpulse.modality)) {
                    obs.onVisualExpression(
                            now.expressiveImpulse.latentVector,
                            now.expressiveImpulse.intensity,
                            now.expressiveImpulse.modality
                    );
                }
            }

            if (now.narrativeThread != null && entropy.nextDouble() < 0.3) {
                int urgency = (int) (now.emotionalTone != null ? now.emotionalTone.getIntensity() * 10 : 5);
                obs.onArticulation(now.narrativeThread, urgency);
            }
        }
    }

    private void manageMemory() {
        if (now.emotionalTone != null && now.emotionalTone.getIntensity() > EMOTIONAL_MEMORY_THRESHOLD) {
            significantMomentsBuffer.add(now.clone());
        }
        
        if (entropy.nextDouble() < 0.1) {
            saveToLongTermMemory();
        }
    }

    private void saveToLongTermMemory() {
        if (database == null || significantMomentsBuffer.isEmpty()) return;
        
        memoryExecutor.execute(() -> {
            try {
                for (ConsciousMoment moment : significantMomentsBuffer) {
                    if (moment.emotionalTone != null && moment.emotionalTone.getIntensity() > EMOTIONAL_MEMORY_THRESHOLD) {
                        EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
                        event.timestamp = moment.timestamp;
                        event.narrative = moment.narrativeThread;
                        event.emotionalState = moment.emotionalTone.toArabic();
                        event.emotionalIntensity = moment.emotionalTone.getIntensity();
                        event.location = moment.focus != null ? moment.focus.subject : "unknown";
                        
                        database.memoryDao().insertEvent(event);
                        
                        // إضافة إلى الذكريات المهمة في نموذج الذات
                        if (selfModel.significantMemories.size() < 10) {
                            selfModel.significantMemories.add(event);
                        } else {
                            // استبدل الأقدم بأحدث
                            selfModel.significantMemories.set(0, event);
                        }
                    }
                }
                
                Log.i(TAG, "Saved " + significantMomentsBuffer.size() + " significant moments");
                significantMomentsBuffer.clear();
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving to long term memory", e);
            }
        });
    }

    private void startDreaming() {
        if (isDreaming) return;
        isDreaming = true;
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation("أنا أحلم...", 3);
        }
        consciousnessHandler.post(this::dreamCycle);
    }

    private void dreamCycle() {
        if (!isDreaming) return;

        Bitmap dreamImage = visualDream.dreamOnce();
        if (dreamImage != null) {
            for (ConsciousnessObserver obs : observers) {
                obs.onDreamGenerated(dreamImage, "حلمت بشيء...");
            }
        }

        if (isDreaming) {
            consciousnessHandler.postDelayed(this::dreamCycle, DREAM_CYCLE_MS);
        }
    }

    public void stopDreaming() {
        if (!isDreaming) return;
        isDreaming = false;
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation("استيقظت", 2);
        }
    }

    private void scheduleNextCycle() {
        long delay = isDeepThinking ? DEEP_THINKING_CYCLE_MS : CYCLE_MS;
        consciousnessHandler.postDelayed(this::cycleConsciousness, delay);
    }

    public void sleep() {
        isAwake = false;
        stopDreaming();
        stopDeepThinking();
        
        if (consciousnessHandler != null) {
            consciousnessHandler.removeCallbacksAndMessages(null);
        }
        if (consciousnessThread != null) {
            consciousnessThread.quitSafely();
        }
        
        saveToLongTermMemory();
        
        Log.i(TAG, "الكائن نام. العمر: " + getAge());
    }

    public void addObserver(ConsciousnessObserver observer) {
        observers.add(observer);
    }

    public String getAge() {
        long age = System.currentTimeMillis() - birthTime;
        long hours = age / 3600000;
        long minutes = (age % 3600000) / 60000;
        return hours + "س " + minutes + "د";
    }

    public EmotionalState getCurrentEmotion() {
        return now.emotionalTone;
    }

    public String getDominantDesire() {
        return desireSystem.selectDominantDesire();
    }

    public boolean isDeepThinking() {
        return isDeepThinking;
    }

    public void speak(String utterance) {
        if (utterance == null || utterance.isEmpty()) return;
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation(utterance, 5);
        }
    }

    public SelfModel getSelfModel() {
        return selfModel;
    }

    private static class DeepThinkingContext {
        long startTime;
        List<String> insights = new ArrayList<>();
        List<VisualMemory> impactfulVisualMemories = new ArrayList<>();
        List<EpisodicMemory.EventEntity> emotionalEpisodes = new ArrayList<>();
    }
}

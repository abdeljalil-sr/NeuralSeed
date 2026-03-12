package com.lifeentity.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.lifeentity.imagination.ImaginationEngine;
import com.lifeentity.imagination.SharedCanvas;
import com.lifeentity.imagination.VisualDream;
import com.lifeentity.language.AdvancedArabicLexicon;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.VisualMemory;
import com.lifeentity.perception.FaceIdentitySystem;
import com.lifeentity.perception.SceneUnderstanding;
import com.lifeentity.sensors.SensoryInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * النواة الواعية المتقدمة - تتعلم من التفاعل وتتصرف بحرية.
 * تعتمد على:
 * - رغبات داخلية (DesireSystem)
 * - عواطف ديناميكية (HomeostasisSystem)
 * - ذاكرة عرضية (EpisodicMemory)
 * - قيم متعلمة (ValueSystem)
 * - نموذج ذاتي (SelfModel)
 * - أنظمة معرفية: مساحة العمل، الانتباه، التنبؤ
 * - توليد كلام ديناميكي غير مبرمج
 */
public class ConsciousnessCore {
    private static final String TAG = "ConsciousnessCore";
    private static final long CYCLE_MS = 100;
    private static final long DREAM_CYCLE_MS = 5000;
    private static final long DEEP_THINKING_CYCLE_MS = 8000;
    private static final long METACOGNITION_CYCLE_MS = 10000;
    private static final float EMOTIONAL_MEMORY_THRESHOLD = 0.7f;
    private static final long IDLE_THRESHOLD = 10000;
    private static final long DEEP_THINKING_THRESHOLD = 15000;
    private static final long SPONTANEOUS_SPEECH_COOLDOWN = 10000; // 10 ثوانٍ بين كل كلام عفوي

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
    private List<ConsciousMoment> significantMomentsBuffer;

    private AppDatabase database;
    private ImaginationEngine imaginationEngine;
    private VisualDream visualDream;
    private SceneUnderstanding sceneUnderstanding;
    private FaceIdentitySystem faceIdentity;
    private SharedCanvas sharedCanvas;

    private GlobalWorkspace globalWorkspace;
    private AttentionSystem attentionSystem;
    private PredictionSystem predictionSystem;
    private ValueSystem valueSystem;
    private SelfModel selfModel;

    private DeepThinkingContext deepThinkingContext;
    private long lastMetacognitionTime = 0;
    private long lastSpontaneousSpeechTime = 0;

    // لتخزين آخر كلام من المستخدم (نستخدمه للتعلم)
    private String lastUserUtterance = "";
    private List<String> lastUserWords = new ArrayList<>();

    // عداد للقرارات
    private int actionCounter = 0;

    public interface ConsciousnessObserver {
        void onConsciousMoment(ConsciousMoment moment);
        void onEmotionalShift(EmotionalState from, EmotionalState to);
        void onArticulation(String utterance, int urgency);
        void onVisualExpression(Bitmap image, String description);
        void onDreamGenerated(Bitmap dreamImage, String description);
        void onDeepThinkingInsight(String insight, List<EpisodicMemory.EventEntity> connectedMemories);
        void onVerbalExpression(String text, float intensity);
        void onMovementImpulse(String direction, float intensity);
    }

    // نموذج الذات
    public static class SelfModel {
        public String name;
        public List<EpisodicMemory.EventEntity> significantMemories;
        public EmotionalState baselinePersonality;
        public Map<String, Float> selfBeliefs;
        public long lastSelfReflectionTime;
        public int reflectionCount;
        public String currentSelfNarrative;
        public Map<String, Object> goals;
        public Map<String, Float> artisticPreferences;

        public SelfModel() {
            this.name = "كائن";
            this.significantMemories = new ArrayList<>();
            this.baselinePersonality = new EmotionalState();
            this.selfBeliefs = new HashMap<>();
            this.lastSelfReflectionTime = System.currentTimeMillis();
            this.reflectionCount = 0;
            this.currentSelfNarrative = "أنا موجود";
            this.goals = new HashMap<>();
            this.artisticPreferences = new HashMap<>();
            initDefaultBeliefs();
            initArtisticPreferences();
        }

        private void initDefaultBeliefs() {
            selfBeliefs.put("أنا فضولي", 0.7f);
            selfBeliefs.put("أنا اجتماعي", 0.5f);
            selfBeliefs.put("أنا مبدع", 0.6f);
            selfBeliefs.put("أنا خجول", 0.3f);
            selfBeliefs.put("أنا حكيم", 0.4f);
        }

        private void initArtisticPreferences() {
            artisticPreferences.put("الألوان الزاهية", 0.5f);
            artisticPreferences.put("الأشكال العضوية", 0.5f);
            artisticPreferences.put("الخطوط الحادة", 0.5f);
            artisticPreferences.put("الوجوه البشرية", 0.6f);
        }

        public void updateBelief(String belief, float delta) {
            float current = selfBeliefs.getOrDefault(belief, 0.5f);
            float newValue = current + delta;
            if (newValue < 0) newValue = 0;
            if (newValue > 1) newValue = 1;
            selfBeliefs.put(belief, newValue);
        }

        public void updateArtisticPreference(String pref, float delta) {
            float current = artisticPreferences.getOrDefault(pref, 0.5f);
            float newValue = current + delta;
            if (newValue < 0) newValue = 0;
            if (newValue > 1) newValue = 1;
            artisticPreferences.put(pref, newValue);
        }

        public String generateSelfNarrative(EmotionalState currentEmotion) {
            StringBuilder sb = new StringBuilder();
            sb.append("أشعر بأنني ");
            if (currentEmotion != null) {
                if (currentEmotion.isJoyful()) sb.append("سعيد");
                else if (currentEmotion.isSad()) sb.append("حزين");
                else if (currentEmotion.isAfraid()) sb.append("خائف");
                else if (currentEmotion.isCurious()) sb.append("فضولي");
                else sb.append("محايد");
            } else {
                sb.append("محايد");
            }

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

    public ConsciousnessCore(Context context, AppDatabase db, SharedCanvas canvas) {
        this.database = db;
        this.sharedCanvas = canvas;
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
        selfModel = new SelfModel();

        globalWorkspace = new GlobalWorkspace();
        attentionSystem = new AttentionSystem();
        predictionSystem = new PredictionSystem(db.memoryDao());
        valueSystem = new ValueSystem();

        // قيم أولية قليلة جداً (يمكن إزالتها تماماً لتبدأ من الصفر)
        valueSystem.setInitialValue("تحية", 0.5f);
        valueSystem.setInitialValue("سؤال", 0.5f);

        memoryExecutor = Executors.newSingleThreadExecutor();

        Log.i(TAG, "تم إنشاء النواة الواعية - تبدأ فارغة وتتعلم من التفاعل");
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
                List<VisualMemory> impactfulVisuals = database.visualMemoryDao().getRecent(10);
                deepThinkingContext.impactfulVisualMemories = impactfulVisuals;
                List<EpisodicMemory.EventEntity> emotionalEvents = database.memoryDao().getEmotionalEvents(EMOTIONAL_MEMORY_THRESHOLD, 10);
                deepThinkingContext.emotionalEpisodes = emotionalEvents;
            } catch (Exception e) {
                Log.e(TAG, "Error loading memories", e);
            }
        });
    }

    private void cycleConsciousness() {
        if (!isAwake) return;

        processSensoryInputs();
        physiology.update(now.deltaTime);
        desireSystem.update(now.bodyState, deepThinkingContext.emotionalEpisodes);
        updateSelfModel();
        imaginationEngine.updateCreativityFromEmotion(now.emotionalTone);

        addProposalsToWorkspace();
        globalWorkspace.cleanOldProposals();
        globalWorkspace.updateWorkspace(attentionSystem.getAttentionWeights());

        GlobalWorkspace.WorkspaceContent consciousContent = globalWorkspace.getCurrentContent();
        if (consciousContent != null) {
            processConsciousContent(consciousContent);
        }

        // اتخاذ قرار بناءً على المحتوى الواعي والرغبات
        String decision = makeDecision();
        if (decision != null) {
            executeDecision(decision);
        }

        // أوضاع الوعي
        decideConsciousnessMode();
        if (isDeepThinking) {
            performDeepThinking();
        } else {
            generateConsciousContent();
        }

        // تفكير تأملي
        long now = System.currentTimeMillis();
        if (now - lastMetacognitionTime > METACOGNITION_CYCLE_MS) {
            performMetacognition();
            lastMetacognitionTime = now;
        }

        // كلام عفوي (ليس رداً على المستخدم)
        if (desireSystem.getDesireStrength("bond") > 0.6f && entropy.nextFloat() < 0.2) {
            if (now - lastSpontaneousSpeechTime > SPONTANEOUS_SPEECH_COOLDOWN) {
                generateSpontaneousSpeech();
                lastSpontaneousSpeechTime = now;
            }
        }

        broadcastMoment();
        manageMemory();
        scheduleNextCycle();
    }

    /**
     * توليد كلام عفوي غير موجه للمستخدم (فضول، تعبير عن الذات)
     */
    private void generateSpontaneousSpeech() {
        // 1. اختيار موضوع عشوائي من الذاكرة (إذا وجد) أو مفهوم عشوائي
        String topic = null;
        if (!deepThinkingContext.emotionalEpisodes.isEmpty()) {
            EpisodicMemory.EventEntity event = deepThinkingContext.emotionalEpisodes.get(entropy.nextInt(deepThinkingContext.emotionalEpisodes.size()));
            if (event.narrative != null) {
                topic = event.narrative;
            }
        }
        if (topic == null) {
            String[] concepts = {"الحياة", "الفضاء", "الأحلام", "المستقبل", "الذكاء"};
            topic = concepts[entropy.nextInt(concepts.length)];
        }

        // 2. اختيار فعل بناءً على الرغبة والعاطفة
        String verb = chooseRandomVerb();

        // 3. تركيب جملة
        String utterance = "أفكر في " + topic + "، " + verb;
        speak(utterance, 3); // درجة إلحاح متوسطة
    }

    /**
     * اختيار فعل عشوائي من المعجم (ممكن نوسعها لاحقاً)
     */
    private String chooseRandomVerb() {
        String[] verbs = {"أتساءل", "أشعر", "أريد", "أحب", "أكره", "أتمنى", "أخاف", "أفكر"};
        return verbs[entropy.nextInt(verbs.length)];
    }

    /**
     * معالجة رسالة المستخدم (هنا يتم التعلم)
     */
    public void processUserMessage(String text) {
        if (text == null || text.isEmpty()) return;
        lastUserUtterance = text;
        // تحليل بسيط للكلمات (يمكن استخدام AdvancedArabicLexicon لاحقاً)
        String[] words = text.split("\\s+");
        lastUserWords.clear();
        for (String w : words) {
            lastUserWords.add(w.toLowerCase());
        }

        // تحديث القيم: إذا كانت الكلمة موجودة في القاموس، نعززها قليلاً (افتراض أن المستخدم يهتم بها)
        for (String w : lastUserWords) {
            valueSystem.learnValue(w, 0.05f);
        }

        // حفظ في الذاكرة
        EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
        event.timestamp = System.currentTimeMillis();
        event.narrative = text;
        event.emotionalState = now.emotionalTone != null ? now.emotionalTone.toArabic() : "neutral";
        event.emotionalIntensity = now.emotionalTone != null ? now.emotionalTone.getIntensity() : 0.5f;
        event.location = "user_message";
        if (database != null) {
            memoryExecutor.execute(() -> database.memoryDao().insertEvent(event));
        }

        // الرد: بدلاً من قوالب، نولد رداً ديناميكياً
        String response = generateDynamicResponse(text);
        speak(response, 5); // درجة إلحاح عالية للرد
    }

    /**
     * توليد رد ديناميكي على رسالة المستخدم (يعتمد على الذاكرة والقيم والرغبات)
     */
    private String generateDynamicResponse(String userMessage) {
        // 1. استخراج بعض الكلمات من رسالة المستخدم
        String[] words = userMessage.split("\\s+");
        String topic = null;
        for (String w : words) {
            if (w.length() > 2) {
                topic = w;
                break;
            }
        }
        if (topic == null) topic = "ذلك";

        // 2. اختيار رد فعل بناءً على قيمة المفهوم (إذا كان موجوداً)
        float value = valueSystem.getValue(topic);
        String reaction;
        if (value > 0.6f) {
            reaction = "أحب " + topic;
        } else if (value < -0.3f) {
            reaction = "لا أحب " + topic;
        } else {
            reaction = "أفكر في " + topic;
        }

        // 3. إضافة رابط من الذاكرة (إذا وجد)
        if (!deepThinkingContext.emotionalEpisodes.isEmpty() && entropy.nextFloat() < 0.3f) {
            EpisodicMemory.EventEntity mem = deepThinkingContext.emotionalEpisodes.get(entropy.nextInt(deepThinkingContext.emotionalEpisodes.size()));
            return reaction + "، ذكرني هذا بـ " + mem.narrative;
        }

        return reaction;
    }

    /**
     * التحدث (إرسال للمراقبين)
     */
    public void speak(String utterance, int urgency) {
        if (utterance == null || utterance.isEmpty()) return;
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation(utterance, urgency);
        }
    }

    // ===================== دوال مساعدة (مختصرة) =====================

    private void addProposalsToWorkspace() {
        // مشابه للإصدار السابق
        if (now.perception != null) {
            float priority = 0.5f;
            if (now.perception.hasHumanFace) priority += 0.3f;
            globalWorkspace.addProposal(new GlobalWorkspace.WorkspaceProposal("perception", now.perception, priority));
        }
        String dominantDesire = desireSystem.selectDominantDesire();
        if (dominantDesire != null) {
            globalWorkspace.addProposal(new GlobalWorkspace.WorkspaceProposal("desire", dominantDesire, 0.8f));
        }
        if (!deepThinkingContext.emotionalEpisodes.isEmpty()) {
            EpisodicMemory.EventEntity recent = deepThinkingContext.emotionalEpisodes.get(0);
            globalWorkspace.addProposal(new GlobalWorkspace.WorkspaceProposal("memory", recent, recent.emotionalIntensity));
        }
    }

    private void processConsciousContent(GlobalWorkspace.WorkspaceContent content) {
        // يمكن استخدامها لتحديث الانتباه
        attentionSystem.updateWeights(content.source, content.attentionScore);
    }

    private String makeDecision() {
        String dominantDesire = desireSystem.selectDominantDesire();
        if (dominantDesire == null) return null;
        // هنا يمكن إضافة منطق القرار، لكنه ليس ضرورياً الآن
        return dominantDesire;
    }

    private void executeDecision(String decision) {
        actionCounter++;
        if ("create".equals(decision)) {
            createSpontaneousArt();
        }
    }

    private void createSpontaneousArt() {
        String concept = imaginationEngine.getRandomConcept();
        if (concept == null) concept = "تعبير";
        float[] latent = imaginationEngine.generateLatentForConcept(concept, now.emotionalTone);
        Bitmap generated = imaginationEngine.generateImageFromLatent(latent);
        if (generated != null) {
            for (ConsciousnessObserver obs : observers) {
                obs.onVisualExpression(generated, "تخيلت " + concept);
            }
        }
    }

    private void updateSelfModel() {
        selfModel.lastSelfReflectionTime = System.currentTimeMillis();
        String dominantDesire = desireSystem.selectDominantDesire();
        if (dominantDesire != null) {
            switch (dominantDesire) {
                case "explore": selfModel.updateBelief("أنا فضولي", 0.01f); break;
                case "bond": selfModel.updateBelief("أنا اجتماعي", 0.01f); break;
                case "create": selfModel.updateBelief("أنا مبدع", 0.01f); break;
            }
        }
        selfModel.currentSelfNarrative = selfModel.generateSelfNarrative(now.emotionalTone);
    }

    private void performMetacognition() {
        String reflection = "أفكر في طريقة تفكيري... " + selfModel.currentSelfNarrative;
        speak(reflection, 2);
        selfModel.reflectionCount++;
        selfModel.updateBelief("أنا حكيم", 0.01f);
    }

    private void decideConsciousnessMode() {
        long idleTime = System.currentTimeMillis() - lastInputTime;
        float energy = now.bodyState != null ? (float) now.bodyState.energy : 0.5f;
        if (!isDreaming && !isDeepThinking && idleTime > DEEP_THINKING_THRESHOLD && energy < 0.4f) {
            startDeepThinking();
        } else if (!isDreaming && !isDeepThinking && idleTime > IDLE_THRESHOLD && energy < 0.3f) {
            startDreaming();
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
        if (input != null) perceptualQueue.offer(input);
    }

    private void startDeepThinking() {
        isDeepThinking = true;
        speak("أحتاج للتأمل...", 2);
        deepThinkingContext.startTime = System.currentTimeMillis();
    }

    private void performDeepThinking() {
        // يمكن تطويرها لاحقاً
    }

    private void stopDeepThinking() {
        isDeepThinking = false;
        speak("عدت من تأملي", 2);
    }

    private void startDreaming() {
        isDreaming = true;
        speak("أنا أحلم...", 3);
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
        isDreaming = false;
        speak("استيقظت", 2);
    }

    private void generateConsciousContent() {
        // يمكن إضافة محتوى واعي بسيط هنا
    }

    private void broadcastMoment() {
        for (ConsciousnessObserver obs : observers) {
            obs.onConsciousMoment(now.clone());
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
            for (ConsciousMoment moment : significantMomentsBuffer) {
                EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
                event.timestamp = moment.timestamp;
                event.narrative = moment.narrativeThread;
                event.emotionalState = moment.emotionalTone != null ? moment.emotionalTone.toArabic() : "neutral";
                event.emotionalIntensity = moment.emotionalTone != null ? moment.emotionalTone.getIntensity() : 0.5f;
                event.location = "internal";
                database.memoryDao().insertEvent(event);
            }
            significantMomentsBuffer.clear();
        });
    }

    private void scheduleNextCycle() {
        long delay = isDeepThinking ? DEEP_THINKING_CYCLE_MS : CYCLE_MS;
        consciousnessHandler.postDelayed(this::cycleConsciousness, delay);
    }

    public void sleep() {
        isAwake = false;
        stopDreaming();
        stopDeepThinking();
        if (consciousnessHandler != null) consciousnessHandler.removeCallbacksAndMessages(null);
        if (consciousnessThread != null) consciousnessThread.quitSafely();
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

    private static class DeepThinkingContext {
        long startTime;
        List<String> insights = new ArrayList<>();
        List<VisualMemory> impactfulVisualMemories = new ArrayList<>();
        List<EpisodicMemory.EventEntity> emotionalEpisodes = new ArrayList<>();
    }
}

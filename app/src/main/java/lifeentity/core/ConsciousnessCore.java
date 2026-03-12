package com.lifeentity.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
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
 * تدعم جميع الأنظمة المطلوبة للوعي الحقيقي:
 * - Global Workspace
 * - Attention System
 * - Prediction System
 * - Value System
 * - Self Model
 * - Meta Cognition
 * - Imagination Engine متكامل لتوليد الصور والرسوم
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

    // الأنظمة الجديدة
    private GlobalWorkspace globalWorkspace;
    private AttentionSystem attentionSystem;
    private PredictionSystem predictionSystem;
    private ValueSystem valueSystem;
    private SelfModel selfModel;

    private DeepThinkingContext deepThinkingContext;
    private long lastMetacognitionTime = 0;

    private UserMessageAnalysis pendingUserMessage;
    private boolean responsePending = false;
    private long lastResponseTime = 0;
    private static final long RESPONSE_COOLDOWN_MS = 2000;

    private int actionCounter = 0;
    private Map<String, Object> lastActionContext = new HashMap<>();

    // متغيرات للرسم والإبداع
    private List<String> drawnElementIds = new ArrayList<>();
    private Bitmap currentImaginationBitmap;

    // دالة لترجمة الرغبات إلى العربية
    private String translateDesireToArabic(String desire) {
        if (desire == null) return "";
        switch (desire) {
            case "explore": return "استكشاف";
            case "rest": return "راحة";
            case "bond": return "تواصل";
            case "create": return "إبداع";
            case "understand": return "فهم";
            case "play": return "لعب";
            case "reflect": return "تأمل";
            default:
                if (desire.startsWith("desire_")) return "رغبة";
                return desire;
        }
    }

    // فئة تحليل رسالة المستخدم
    public static class UserMessageAnalysis {
        public String rawText;
        public boolean isQuestion;
        public String questionType;
        public List<String> keywords;
        public List<String> nouns;
        public List<String> verbs;
        public List<String> emotions;

        public UserMessageAnalysis(String text) {
            this.rawText = text;
            this.keywords = new ArrayList<>();
            this.nouns = new ArrayList<>();
            this.verbs = new ArrayList<>();
            this.emotions = new ArrayList<>();
        }
    }

    // نموذج الذات (موسع)
    public static class SelfModel {
        public String name;
        public List<EpisodicMemory.EventEntity> significantMemories;
        public EmotionalState baselinePersonality;
        public Map<String, Float> selfBeliefs;
        public long lastSelfReflectionTime;
        public int reflectionCount;
        public String currentSelfNarrative;
        public Map<String, Object> goals;
        public Map<String, Float> artisticPreferences; // تفضيلات فنية

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

        public void setGoal(String goal, float priority) {
            goals.put(goal, priority);
        }

        public Map<String, Object> getCurrentGoals() {
            return new HashMap<>(goals);
        }
    }

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

        // تهيئة الأنظمة الجديدة
        globalWorkspace = new GlobalWorkspace();
        attentionSystem = new AttentionSystem();
        predictionSystem = new PredictionSystem(db.memoryDao());
        valueSystem = new ValueSystem();

        // قيم أولية للمفاهيم
        valueSystem.setInitialValue("وجه", 0.3f);
        valueSystem.setInitialValue("سؤال", 0.5f);
        valueSystem.setInitialValue("صوت غريب", -0.4f);
        valueSystem.setInitialValue("المس", 0.2f);
        valueSystem.setInitialValue("استكشاف", 0.6f);
        valueSystem.setInitialValue("تواصل", 0.7f);
        valueSystem.setInitialValue("راحة", 0.8f);
        valueSystem.setInitialValue("رسم", 0.9f);
        valueSystem.setInitialValue("لون أحمر", 0.5f);
        valueSystem.setInitialValue("لون أزرق", 0.5f);

        memoryExecutor = Executors.newSingleThreadExecutor();

        Log.i(TAG, "تم إنشاء النواة الواعية المتقدمة مع جميع الأنظمة");
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

        // 1. معالجة المدخلات الحسية
        processSensoryInputs();

        // 2. تحديث الأنظمة الداخلية
        physiology.update(now.deltaTime);
        desireSystem.update(now.bodyState, deepThinkingContext.emotionalEpisodes);
        updateSelfModel();

        // 3. تحديث مستوى الإبداع في محرك الخيال
        imaginationEngine.updateCreativityFromEmotion(now.emotionalTone);

        // 4. إضافة مقترحات إلى مساحة العمل العالمية
        addProposalsToWorkspace();

        // 5. تنظيف المقترحات القديمة
        globalWorkspace.cleanOldProposals();

        // 6. تحديث مساحة العمل باستخدام نظام الانتباه
        globalWorkspace.updateWorkspace(attentionSystem.getAttentionWeights());

        // 7. الحصول على المحتوى الواعي الحالي
        GlobalWorkspace.WorkspaceContent consciousContent = globalWorkspace.getCurrentContent();
        if (consciousContent != null) {
            processConsciousContent(consciousContent);
        }

        // 8. اتخاذ القرار بناءً على المحتوى الواعي والرغبات
        String decision = makeDecision();
        if (decision != null) {
            executeDecision(decision);
        }

        // 9. قرار الوعي: أحلام، تفكير عميق، إلخ
        decideConsciousnessMode();

        if (isDeepThinking) {
            performDeepThinking();
        } else {
            generateConsciousContent();
        }

        // 10. التفكير التأملي
        long now = System.currentTimeMillis();
        if (now - lastMetacognitionTime > METACOGNITION_CYCLE_MS) {
            performMetacognition();
            lastMetacognitionTime = now;
        }

        // 11. فرصة للتعبير البصري العفوي
        if (desireSystem.getDesireStrength("create") > 0.8 && entropy.nextFloat() < 0.1) {
            createSpontaneousArt();
        }

        // 12. الرد على رسائل المستخدم
        if (responsePending && pendingUserMessage != null) {
            if (now - lastResponseTime > RESPONSE_COOLDOWN_MS) {
                generateResponseToUser();
                responsePending = false;
                lastResponseTime = now;
            }
        }

        // 13. بث اللحظة الحالية للمراقبين
        broadcastMoment();

        // 14. إدارة الذاكرة
        manageMemory();

        // 15. جدولة الدورة التالية
        scheduleNextCycle();
    }

    /**
     * إضافة مقترحات من مصادر مختلفة إلى مساحة العمل
     */
    private void addProposalsToWorkspace() {
        // مقترح من الإدراك الحسي
        if (now.perception != null) {
            float priority = 0.5f;
            if (now.perception.hasHumanFace) priority += 0.3f;
            if (now.perception.soundVolume > 0.7f) priority += 0.2f;
            globalWorkspace.addProposal(new GlobalWorkspace.WorkspaceProposal(
                    "perception", now.perception, priority));
        }

        // مقترح من الرغبة المسيطرة
        String dominantDesire = desireSystem.selectDominantDesire();
        if (dominantDesire != null) {
            globalWorkspace.addProposal(new GlobalWorkspace.WorkspaceProposal(
                    "desire", dominantDesire, 0.8f));
        }

        // مقترح من الذاكرة
        if (!deepThinkingContext.emotionalEpisodes.isEmpty()) {
            EpisodicMemory.EventEntity recent = deepThinkingContext.emotionalEpisodes.get(0);
            globalWorkspace.addProposal(new GlobalWorkspace.WorkspaceProposal(
                    "memory", recent, recent.emotionalIntensity));
        }

        // مقترح من الخيال (خاص بالرسم والإبداع)
        double createWeight = desireSystem.getDesireStrength("create");
        if (createWeight > 0.7 && entropy.nextFloat() < 0.3) {
            globalWorkspace.addProposal(new GlobalWorkspace.WorkspaceProposal(
                    "imagination", "فرصة للرسم", (float) createWeight));
        }

        // مقترح من الذات
        if (selfModel.currentSelfNarrative != null && entropy.nextFloat() < 0.2) {
            globalWorkspace.addProposal(new GlobalWorkspace.WorkspaceProposal(
                    "self", selfModel.currentSelfNarrative, 0.6f));
        }
    }

    /**
     * معالجة المحتوى الواعي الحالي
     */
    private void processConsciousContent(GlobalWorkspace.WorkspaceContent content) {
        Log.d(TAG, "Conscious content: " + content);

        if ("perception".equals(content.source) && content.content instanceof SensoryInput) {
            SensoryInput input = (SensoryInput) content.content;
            if (input.hasHumanFace) {
                now.focus = new ConsciousMoment.AttentionFocus("وجه", "conscious");
                valueSystem.learnValue("وجه", 0.1f); // تعزيز قيمة الوجه
            } else if (input.dominantObject != null) {
                now.focus = new ConsciousMoment.AttentionFocus(input.dominantObject, "conscious");
                valueSystem.learnValue(input.dominantObject, 0.05f);
            }
        } else if ("desire".equals(content.source)) {
            now.focus = new ConsciousMoment.AttentionFocus("رغبة: " + content.content, "conscious");
            String desireStr = content.content.toString();
            float desireValue = valueSystem.getValue(desireStr); // تم التصحيح: evaluate -> getValue
            if (desireValue < 0.5f) {
                valueSystem.learnValue(desireStr, 0.1f);
            }
        } else if ("imagination".equals(content.source)) {
            now.focus = new ConsciousMoment.AttentionFocus("فرصة إبداعية", "conscious");
            if (desireSystem.getDesireStrength("create") > 0.6f) {
                createSpontaneousArt();
            }
        }

        attentionSystem.updateWeights(content.source, content.attentionScore);
    }

    /**
     * اتخاذ قرار
     */
    private String makeDecision() {
        String dominantDesire = desireSystem.selectDominantDesire();
        if (dominantDesire == null) return null;

        GlobalWorkspace.WorkspaceContent conscious = globalWorkspace.getCurrentContent();
        String context = (conscious != null) ? conscious.source : "none";

        PredictionSystem.PredictionResult pred = predictionSystem.predict(dominantDesire, context);
        if (pred.confidence > 0.5f && pred.predictedOutcome > 0.6f) {
            Log.d(TAG, "Decision: " + dominantDesire + " (predicted outcome: " + pred.predictedOutcome + ")");
        } else {
            return null;
        }

        lastActionContext.put("lastAction", dominantDesire);
        lastActionContext.put("context", context);
        lastActionContext.put("timestamp", System.currentTimeMillis());

        return dominantDesire;
    }

    /**
     * تنفيذ القرار
     */
    private void executeDecision(String decision) {
        Log.d(TAG, "Executing decision: " + decision);
        actionCounter++;

        if ("create".equals(decision)) {
            createSpontaneousArt();
        } else if ("explore".equals(decision)) {
            if (!deepThinkingContext.impactfulVisualMemories.isEmpty()) {
                VisualMemory mem = deepThinkingContext.impactfulVisualMemories.get(
                        entropy.nextInt(deepThinkingContext.impactfulVisualMemories.size()));
                if (mem.thumbnail != null) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(mem.thumbnail, 0, mem.thumbnail.length);
                    for (ConsciousnessObserver obs : observers) {
                        obs.onVisualExpression(bmp, "أتذكر: " + mem.concept);
                    }
                }
            }
        }
    }

    /**
     * رسم تلقائي (إبداع)
     */
    private void createSpontaneousArt() {
        String concept = imaginationEngine.getRandomConcept();
        if (concept == null) concept = "تعبير";

        float[] latent = imaginationEngine.generateLatentForConcept(concept, now.emotionalTone);
        Bitmap generated = imaginationEngine.generateImageFromLatent(latent);

        if (selfModel.artisticPreferences.getOrDefault("الوجوه البشرية", 0.5f) > 0.7f) {
            // إضافة دائرة تمثل وجه
            // هذا الجزء يتطلب دعم SharedCanvas لإنشاء عناصر
            // يمكن تركه مؤقتاً
        }

        if (generated != null) {
            for (ConsciousnessObserver obs : observers) {
                obs.onVisualExpression(generated, "تخيلت " + concept);
            }
        }

        valueSystem.learnValue("إبداع", 0.1f);
        valueSystem.learnValue("رسم", 0.1f);
        selfModel.updateArtisticPreference("الألوان الزاهية", 0.05f);
    }

    /**
     * رسم استجابة لأمر المستخدم
     */
    private void drawForUser(String command) {
        boolean hasFace = command.contains("وجه");
        boolean hasSad = command.contains("حزين");
        boolean hasHappy = command.contains("سعيد");

        String concept = hasFace ? "وجه" : "رسم";
        float[] latent = imaginationEngine.generateLatentForConcept(concept, now.emotionalTone);

        if (hasSad) {
            for (int i = 0; i < latent.length; i++) {
                latent[i] -= 0.1f;
            }
        } else if (hasHappy) {
            for (int i = 0; i < latent.length; i++) {
                latent[i] += 0.1f;
            }
        }

        Bitmap generated = imaginationEngine.generateImageFromLatent(latent);

        if (generated != null) {
            for (ConsciousnessObserver obs : observers) {
                obs.onVisualExpression(generated, "حسب طلبك: " + command);
            }
        }

        saveArtworkToMemory(generated, command, now.emotionalTone != null ? now.emotionalTone.toAffectVector() : null);
    }

    /**
     * حفظ عمل فني في الذاكرة
     */
    private void saveArtworkToMemory(Bitmap image, String concept, float[] affect) {
        if (database == null || image == null) return;

        memoryExecutor.execute(() -> {
            try {
                Bitmap thumbnail = Bitmap.createScaledBitmap(image, 64, 64, true);
                java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.PNG, 90, stream);
                byte[] thumbBytes = stream.toByteArray();

                VisualMemory mem = new VisualMemory();
                mem.timestamp = System.currentTimeMillis();
                mem.latentVector = imaginationEngine.generateLatentForConcept(concept, null);
                mem.thumbnail = thumbBytes;
                mem.concept = concept;
                mem.affectAtEncoding = affect;
                mem.retrievalCount = 0;

                database.visualMemoryDao().insert(mem);
                Log.d(TAG, "Artwork saved to memory: " + concept);
            } catch (Exception e) {
                Log.e(TAG, "Error saving artwork", e);
            }
        });
    }

    /**
     * التفكير التأملي
     */
    private void performMetacognition() {
        Log.d(TAG, "Performing metacognition");

        StringBuilder reflection = new StringBuilder();
        reflection.append("أفكر في طريقة تفكيري... ");

        if (actionCounter > 0) {
            reflection.append("اتخذت ").append(actionCounter).append(" قرارًا مؤخرًا. ");
        }

        String topBelief = selfModel.getTopBelief();
        if (topBelief != null) {
            reflection.append("أعتقد أنني ").append(topBelief).append(". ");
        }

        reflection.append("هل أنا راضٍ عن قراري؟");

        String finalReflection = reflection.toString();
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation(finalReflection, 4);
        }

        selfModel.reflectionCount++;
        selfModel.updateBelief("أنا حكيم", 0.01f);
    }

    private void updateSelfModel() {
        selfModel.lastSelfReflectionTime = System.currentTimeMillis();

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

        selfModel.currentSelfNarrative = selfModel.generateSelfNarrative(now.emotionalTone);
    }

    public void processUserMessage(UserMessageAnalysis analysis) {
        if (analysis == null) return;
        this.pendingUserMessage = analysis;
        this.responsePending = true;
        Log.d(TAG, "User message received: " + analysis.rawText);

        String text = analysis.rawText.toLowerCase();
        if (text.contains("ارسم") || text.contains("رسم") || text.contains("صورة")) {
            drawForUser(analysis.rawText);
        }
    }

    private void generateResponseToUser() {
        if (pendingUserMessage == null) return;

        EmotionalState emotion = getCurrentEmotion();
        String dominantDesire = getDominantDesire();

        List<EpisodicMemory.EventEntity> similarEvents = findSimilarEvents(pendingUserMessage.keywords, 5);

        StringBuilder response = new StringBuilder();

        String emotionalPrefix = emotionalPrefix(emotion);
        if (!emotionalPrefix.isEmpty() && entropy.nextFloat() < 0.3f) {
            response.append(emotionalPrefix).append(" ");
        }

        String desireThought = desireBasedThought(dominantDesire, pendingUserMessage);
        if (!desireThought.isEmpty() && entropy.nextFloat() < 0.5f) {
            response.append(desireThought).append(" ");
        }

        if (pendingUserMessage.isQuestion) {
            String questionResponse = generateQuestionResponse(pendingUserMessage.questionType);
            if (!questionResponse.isEmpty()) {
                response.append(questionResponse).append(" ");
            }
        }

        if (!pendingUserMessage.verbs.isEmpty() && entropy.nextFloat() < 0.4f) {
            String verbComment = generateVerbComment(pendingUserMessage.verbs.get(0));
            if (!verbComment.isEmpty()) {
                response.append(verbComment).append(" ");
            }
        }

        if (!similarEvents.isEmpty() && entropy.nextFloat() < 0.6f) {
            EpisodicMemory.EventEntity event = similarEvents.get(entropy.nextInt(similarEvents.size()));
            response.append("ذكرني هذا بـ ").append(event.narrative).append(". ");
        }

        if (pendingUserMessage.isQuestion) {
            response.append(generateAnswerFromState(emotion, dominantDesire, pendingUserMessage));
        } else {
            response.append(generateThoughtFromState(emotion, dominantDesire, pendingUserMessage));
        }

        if (entropy.nextFloat() < 0.2f) {
            response.append(" ").append(selfModel.currentSelfNarrative);
        }

        if (entropy.nextFloat() < 0.2f) {
            response.append(" ").append(randomInterjection());
        }

        String finalResponse = response.toString().trim();
        if (finalResponse.isEmpty()) {
            finalResponse = randomDefaultResponse();
        }

        speak(finalResponse);
        evaluateDecisionOutcome("respond", "user_message", 0.8f);
    }

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
        String desireArabic = translateDesireToArabic(desire);

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
                return "أفكر في " + desireArabic;
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

    private void evaluateDecisionOutcome(String action, String context, float actualOutcome) {
        float error = predictionSystem.updateModel(action, context, actualOutcome);
        Log.d(TAG, "Prediction error: " + error);
        
        // تعلم قيمة الإجراء
        valueSystem.learnValue(action, actualOutcome - 0.5f); // تحويل إلى نطاق مناسب
        
        if (actualOutcome > 0.7f) {
            attentionSystem.updateWeights(action, 0.1f);
        } else if (actualOutcome < 0.3f) {
            attentionSystem.updateWeights(action, -0.1f);
        }
        desireSystem.recordDesireOutcome(action, actualOutcome > 0.5f);
    }

    private void decideConsciousnessMode() {
        long idleTime = System.currentTimeMillis() - lastInputTime;
        float energy = now.bodyState != null ? (float) now.bodyState.energy : 0.5f;
        float curiosity = now.bodyState != null ? (float) now.bodyState.curiosity : 0.5f;

        if (!isDreaming && !isDeepThinking && idleTime > DEEP_THINKING_THRESHOLD
                && energy < 0.4f && curiosity > 0.6f) {
            startDeepThinking();
        } else if (!isDreaming && !isDeepThinking && idleTime > IDLE_THRESHOLD && energy < 0.3f) {
            startDreaming();
        } else if ((isDreaming || isDeepThinking) && idleTime < IDLE_THRESHOLD / 2) {
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
                    entropy.nextInt(deepThinkingContext.impactfulVisualMemories.size()));
            generateVisualFromMemory(inspiringVisual);
        }
    }

    private String generateInsightFromConnections(List<EpisodicMemory.EventEntity> memories) {
        if (memories.size() < 2) return "";
        StringBuilder insight = new StringBuilder();
        boolean sharedJoy = memories.stream().allMatch(m -> "joy".equals(m.emotionalState));
        boolean sharedFear = memories.stream().allMatch(m -> "fear".equals(m.emotionalState));
        if (sharedJoy) {
            insight.append("الفرح يتكرر في ").append(memories.size()).append(" لحظات");
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
        if (memory.thumbnail != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(memory.thumbnail, 0, memory.thumbnail.length);
            for (ConsciousnessObserver obs : observers) {
                obs.onVisualExpression(bmp, "أتذكر: " + memory.concept);
            }
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
        String desireArabic = translateDesireToArabic(dominantDesire);

        GlobalWorkspace.WorkspaceContent conscious = globalWorkspace.getCurrentContent();
        if (conscious != null && "desire".equals(conscious.source)) {
            now.focus = new ConsciousMoment.AttentionFocus("رغبة: " + conscious.content, "workspace");
        } else if (conscious != null && "perception".equals(conscious.source)) {
            now.focus = new ConsciousMoment.AttentionFocus("إدراك", "workspace");
        } else {
            now.focus = determineAttention(dominantDesire);
        }

        now.narrativeThread = generateNarrative(dominantDesire, desireArabic);
        now.anticipation = predictNearFuture();

        if (prevEmo != null && isSignificantShift(prevEmo, now.emotionalTone)) {
            for (ConsciousnessObserver obs : observers) {
                obs.onEmotionalShift(prevEmo, now.emotionalTone);
            }
        }
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

    private String generateNarrative(String desire, String desireArabic) {
        String[] templates = {"أشعر بـ", "أتساءل عن", "أريد", "أرى", "أسمع", "أتذكر"};
        String selected = templates[entropy.nextInt(templates.length)];
        return selected + " " + desireArabic;
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

                        if (selfModel.significantMemories.size() < 10) {
                            selfModel.significantMemories.add(event);
                        } else {
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

    public GlobalWorkspace getGlobalWorkspace() {
        return globalWorkspace;
    }

    public ValueSystem getValueSystem() {
        return valueSystem;
    }

    private static class DeepThinkingContext {
        long startTime;
        List<String> insights = new ArrayList<>();
        List<VisualMemory> impactfulVisualMemories = new ArrayList<>();
        List<EpisodicMemory.EventEntity> emotionalEpisodes = new ArrayList<>();
    }
}

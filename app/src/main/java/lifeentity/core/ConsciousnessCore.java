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
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * النواة الواعية المتقدمة - دماغ الكائن الرقمي الحي
 * يدعم التفكير العميق، التعبير المتنوع، والذاكرة العاطفية الذكية
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
        
        memoryExecutor = Executors.newSingleThreadExecutor();

        Log.i(TAG, "تم إنشاء النواة الواعية المتقدمة");
    }

    private void initializeComponents(Context context, AppDatabase db) {
        this.imaginationEngine = new ImaginationEngine(db.visualMemoryDao());
        this.visualDream = new VisualDream(db.visualMemoryDao(), imaginationEngine);
        this.physiology = new HomeostasisSystem(); // ✅ التصحيح: منشئ بدون معاملات
        this.desireSystem = new DesireSystem();
        
        // هذه يمكن تفعيلها لاحقاً
        // this.sceneUnderstanding = new SceneUnderstanding(context, db.visualMemoryDao());
        // this.faceIdentity = new FaceIdentitySystem(db.memoryDao());
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
                // ✅ تم التصحيح: استخدام getRecent بدلاً من getRecentVisualMemories
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
        desireSystem.update(now.bodyState);

        // قرار الوعي: هل أحلم؟ هل أفكر بعمق؟ هل أعبر؟
        decideConsciousnessMode();
        
        if (isDeepThinking) {
            performDeepThinking();
        } else {
            generateConsciousContent();
        }

        broadcastMoment();
        manageMemory();
        scheduleNextCycle();
    }

    /**
     * الوعي يقرر أي وضعية وعي يناسب حالته
     */
    private void decideConsciousnessMode() {
        long idleTime = System.currentTimeMillis() - lastInputTime;
        float energy = now.bodyState != null ? (float) now.bodyState.energy : 0.5f;      // تم التصحيح: casting
        float curiosity = now.bodyState != null ? (float) now.bodyState.curiosity : 0.5f; // تم التصحيح: casting
        
        // التفكير العميق: طاقة منخفضة + فضول مرتفع + خمول
        if (!isDreaming && !isDeepThinking && idleTime > DEEP_THINKING_THRESHOLD 
            && energy < 0.4f && curiosity > 0.6f) {
            startDeepThinking();
        }
        // الأحلام: خمول طويل
        else if (!isDreaming && !isDeepThinking && idleTime > IDLE_THRESHOLD && energy < 0.3f) {
            startDreaming();
        }
        // الاستيقاظ من الأحلام أو التفكير
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
            
            // الخروج من الأوضاع الداخلية إذا كان الإدخال قوياً
            if (isDreaming || isDeepThinking) {
                if (input.brightness > 0.5f || input.soundVolume > 0.6f || input.isTouched) {
                    if (isDreaming) stopDreaming();
                    if (isDeepThinking) stopDeepThinking();
                }
            }
        }
    }

    // ==================== التفكير العميق ====================

    private void startDeepThinking() {
        if (isDeepThinking) return;
        isDeepThinking = true;
        
        Log.i(TAG, "Entering deep thinking mode");
        
        // الوعي يعلن بدء التأمل
        for (ConsciousnessObserver obs : observers) {
            obs.onArticulation("أحتاج للتأمل...", 2);
        }
        
        deepThinkingContext.startTime = System.currentTimeMillis();
        deepThinkingContext.insights.clear();
    }

    private void performDeepThinking() {
        // الوعي يراجع ذكريات قديمة ويربطها
        if (deepThinkingContext.emotionalEpisodes.isEmpty()) return;
        
        // اختيار ذكريات عشوائية للربط
        Collections.shuffle(deepThinkingContext.emotionalEpisodes);
        List<EpisodicMemory.EventEntity> selectedMemories = 
            deepThinkingContext.emotionalEpisodes.subList(0, Math.min(3, deepThinkingContext.emotionalEpisodes.size()));
        
        // محاولة إيجاد روابط
        String insight = generateInsightFromConnections(selectedMemories);
        deepThinkingContext.insights.add(insight);
        
        // إذا وجد رابط عميق، أعلن عنه
        if (entropy.nextFloat() < 0.3f && !insight.isEmpty()) {
            for (ConsciousnessObserver obs : observers) {
                obs.onDeepThinkingInsight(insight, selectedMemories);
                obs.onArticulation("أدركت شيئاً... " + insight, 4);
            }
        }
        
        // أحياناً يولد الوعي أفكاراً بصرية من التأمل
        if (entropy.nextFloat() < 0.2f && !deepThinkingContext.impactfulVisualMemories.isEmpty()) {
            VisualMemory inspiringVisual = deepThinkingContext.impactfulVisualMemories.get(
                entropy.nextInt(deepThinkingContext.impactfulVisualMemories.size())
            );
            generateVisualFromMemory(inspiringVisual);
        }
    }

    private String generateInsightFromConnections(List<EpisodicMemory.EventEntity> memories) {
        if (memories.size() < 2) return "";
        
        // الوعي يحاول إيجاد نمط
        StringBuilder insight = new StringBuilder();
        
        // تحليل المشاعر المشتركة
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
        
        // ربط بالزمان
        long timeSpan = memories.get(memories.size() - 1).timestamp - memories.get(0).timestamp;
        if (timeSpan > 86400000) { // أكثر من يوم
            insight.append(" عبر ").append(timeSpan / 86400000).append(" أيام");
        }
        
        return insight.toString();
    }

    private void generateVisualFromMemory(VisualMemory memory) {
        // الوعي يستلهم من ذاكرة بصرية قديمة
        float[] latent = memory.latentVector;
        if (latent == null) return;
        
        // إضافة تغيير طفيف للتأمل
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

    // ==================== توليد المحتوى الواعي المتنوع ====================

    private void generateConsciousContent() {
        EmotionalState prevEmo = now.emotionalTone;
        now.emotionalTone = physiology.getEmotionalState();
        now.previousEmotion = prevEmo;

        String dominantDesire = desireSystem.selectDominantDesire();

        now.focus = determineAttention(dominantDesire);
        now.narrativeThread = generateNarrative(dominantDesire);
        now.anticipation = predictNearFuture();

        // الوعي يقرر كيف يعبر - ليس دائماً بصرياً
        double createWeight = desireSystem.getDesireStrength("create");
        double expressWeight = desireSystem.getDesireStrength("express");
        
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

    /**
     * توليد دافع تعبيري متنوع: بصري، لفظي، أو حركي
     */
    private ConsciousMoment.ExpressiveImpulse generateExpressiveImpulse(String desire) {
        float baseIntensity = (float) (desireSystem.getDesireStrength("create") * 
                          (0.5 + entropy.nextDouble()));
        
        // الوعي يختار نوع التعبير بناءً على حالته
        ExpressionType expressionType = chooseExpressionType();
        
        switch (expressionType) {
            case VISUAL:
                return generateVisualExpression(desire, baseIntensity);
                
            case VERBAL:
                return generateVerbalExpression(desire, baseIntensity);
                
            case MOVEMENT:
                return generateMovementExpression(desire, baseIntensity);
                
            case HYBRID:
                // مزيج من أكثر من نوع
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

    /**
     * الوعي يختار كيف يعبر بناءً على مشاعره والسياق
     */
    private ExpressionType chooseExpressionType() {
        float arousal = now.emotionalTone != null ? (float) now.emotionalTone.getArousal() : 0.5f;  // تم التصحيح: casting
        float energy = now.bodyState != null ? (float) now.bodyState.energy : 0.5f;                 // تم التصحيح: casting
        
        // إثارة عالية + طاقة = حركة
        if (arousal > 0.7f && energy > 0.6f) {
            return entropy.nextFloat() < 0.6f ? ExpressionType.MOVEMENT : ExpressionType.VISUAL;
        }
        
        // فضول + هدوء = لفظي
        if (now.emotionalTone != null && now.emotionalTone.isCurious() && arousal < 0.5f) {
            return entropy.nextFloat() < 0.5f ? ExpressionType.VERBAL : ExpressionType.VISUAL;
        }
        
        // حزن = بصري غالباً
        if (now.emotionalTone != null && now.emotionalTone.isSad()) {
            return entropy.nextFloat() < 0.7f ? ExpressionType.VISUAL : ExpressionType.VERBAL;
        }
        
        // افتراضي: بصري أو هجين
        return entropy.nextFloat() < 0.3f ? ExpressionType.HYBRID : ExpressionType.VISUAL;
    }

    private ConsciousMoment.ExpressiveImpulse generateVisualExpression(String desire, float intensity) {
        // الربط بالذاكرة البصرية إذا وجدت
        float[] latent;
        String concept;
        
        if (!deepThinkingContext.impactfulVisualMemories.isEmpty() && entropy.nextFloat() < 0.3f) {
            // استلهام من ذاكرة بصرية قوية
            VisualMemory inspiring = deepThinkingContext.impactfulVisualMemories.get(
                entropy.nextInt(deepThinkingContext.impactfulVisualMemories.size())
            );
            latent = inspiring.latentVector;
            concept = inspiring.concept != null ? inspiring.concept : "ذكرى";
            
            // إضافة طابع الشخصية الحالية
            if (now.bodyState != null) {
                latent = blendWithCurrentState(latent, now.bodyState.toAffectVector());
            }
        } else {
            // توليد جديد
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
        float blendFactor = 0.3f; // 30% من الحالة الحالية
        
        for (int i = 0; i < Math.min(memoryLatent.length, 16); i++) { // الأبعاد المهمة فقط
            blended[i] = memoryLatent[i] * (1 - blendFactor) + currentAffect[i % currentAffect.length] * blendFactor;
        }
        
        // بقية الأبعاد من الذاكرة
        for (int i = 16; i < memoryLatent.length; i++) {
            blended[i] = memoryLatent[i];
        }
        
        return blended;
    }

    private ConsciousMoment.ExpressiveImpulse generateVerbalExpression(String desire, float intensity) {
        // الوعي يريد التحدث كتعبير
        String[] verbalConcepts = {"سؤال", "تأمل", "قصة", "شعر", "تساؤل"};
        String concept = verbalConcepts[entropy.nextInt(verbalConcepts.length)];
        
        // المتجه "اللفظي" يمثل نمط الكلام (يمكن تطويره لاحقاً)
        float[] verbalLatent = new float[128];
        for (int i = 0; i < 16; i++) {
            verbalLatent[i] = now.emotionalTone != null ? now.emotionalTone.getIntensity() * 2 - 1 : 0;
        }
        
        // إعلان للمراقبين
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
        // الوعي يريد الحركة كتعبير (اهتزاز، تموج، انتقال)
        String[] movements = {"تموج", "انتشار", "تجمع", "تدفق", "ارتعاش"};
        String movement = movements[entropy.nextInt(movements.length)];
        
        // المتجه "الحركي"
        float[] movementLatent = new float[128];
        // الأبعاد الأولى تمثل سرعة واتجاه
        movementLatent[0] = (entropy.nextFloat() - 0.5f) * 2; // X velocity
        movementLatent[1] = (entropy.nextFloat() - 0.5f) * 2; // Y velocity
        movementLatent[2] = intensity; // speed magnitude
        
        for (ConsciousnessObserver obs : observers) {
            obs.onMovementImpulse(movement, intensity);
        }
        
        return new ConsciousMoment.ExpressiveImpulse("movement", intensity, movementLatent, movement);
    }

    // ==================== باقي الوظائف ====================

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
        ant.emotionalValence = (float) (now.emotionalTone.getDopamine() - now.emotionalTone.getCortisol());
        return ant;
    }

    private boolean isSignificantShift(EmotionalState from, EmotionalState to) {
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
                int urgency = (int) (now.emotionalTone.getIntensity() * 10);
                obs.onArticulation(now.narrativeThread, urgency);
            }
        }
    }

    // ==================== إدارة الذاكرة الذكية ====================

    private void manageMemory() {
        // جمع اللحظات المهمة
        if (now.emotionalTone != null && now.emotionalTone.getIntensity() > EMOTIONAL_MEMORY_THRESHOLD) {
            significantMomentsBuffer.add(now.clone());
        }
        
        // الحفظ الدوري
        if (entropy.nextDouble() < 0.1) {
            saveToLongTermMemory();
        }
    }

    private void saveToLongTermMemory() {
        if (database == null || significantMomentsBuffer.isEmpty()) return;
        
        memoryExecutor.execute(() -> {
            try {
                // حفظ اللحظات المهمة فقط
                for (ConsciousMoment moment : significantMomentsBuffer) {
                    if (moment.emotionalTone != null && moment.emotionalTone.getIntensity() > EMOTIONAL_MEMORY_THRESHOLD) {
                        EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
                        event.timestamp = moment.timestamp;
                        event.narrative = moment.narrativeThread;
                        event.emotionalState = moment.emotionalTone.toArabic(); // ✅ التصحيح
                        event.emotionalIntensity = moment.emotionalTone.getIntensity();
                        // تم التصحيح: استخدام subject بدلاً من target
                        event.location = moment.focus != null ? moment.focus.subject : "unknown";
                        
                        database.memoryDao().insertEvent(event);
                    }
                }
                
                Log.i(TAG, "Saved " + significantMomentsBuffer.size() + " significant moments");
                significantMomentsBuffer.clear();
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving to long term memory", e);
            }
        });
    }

    // ==================== الأحلام ====================

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

    // ==================== Lifecycle ====================

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
        
        // حفظ نهائي
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

    // ==================== الفئات الداخلية ====================

    private static class DeepThinkingContext {
        long startTime;
        List<String> insights = new ArrayList<>();
        List<VisualMemory> impactfulVisualMemories = new ArrayList<>();
        List<EpisodicMemory.EventEntity> emotionalEpisodes = new ArrayList<>();
    }
}

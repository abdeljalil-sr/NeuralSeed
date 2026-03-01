package com.neuralseed;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * القشرة اللغوية المتكاملة - دماغ NeuralSeed اللغوي والعقلي
 * 
 * هذا النظام يجمع:
 * - ArabicLexicon: المعجم العربي الكامل
 * - ArabicParser: التحليل النحوي والصرفي
 * - SemanticEmotionalEngine: محرك المعاني والعواطف
 * - SentenceGenerator: توليد الردود الذكية
 * - LearningSystem: التعلم من التصحيحات والأمثلة
 * - LocalDatabase: التخزين المحلي
 * - FirebaseManager: المزامنة السحابية
 * 
 * المبدأ: "لا ردود جاهزة، بل تفكير حقيقي"
 */
public class LinguisticCortex {
    
    private static final String TAG = "LinguisticCortex";
    private static final String PREFS_NAME = "NeuralSeedBrain";
    private static final int MAX_CONTEXT_MEMORY = 50; // آخر 50 رسالة
    private static final int MAX_REFLECTION_INTERVAL = 30000; // تأمل كل 30 ثانية
    
    // ==================== الواجهات ====================
    
    public interface LinguisticListener {
        void onWordLearned(String word, String meaning, String context);
        void onSentenceCorrected(String original, String corrected);
        void onEmotionDetected(String emotion, double intensity);
        void onNewConceptLearned(String concept, String definition);
        void onRelationshipLearned(String subject, String relationship, String object);
        void onThoughtFormed(String thought, String type); // فكرة داخلية جديدة
        void onImaginationCreated(String description, int[] colors); // تخيل بصري
        void onContextAnalyzed(String context, double complexity); // تحليل السياق
    }
    
    public interface VisualImaginationListener {
        void onVisualThought(VisualThought thought);
    }
    
    // ==================== البيانات الأساسية ====================
    
    // المكونات الرئيسية
    private ArabicLexicon lexicon;
    private ArabicParser parser;
    private SemanticEmotionalEngine emotionEngine;
    private SentenceGenerator sentenceGenerator;
    private LearningSystem learningSystem;
    private LocalDatabase database;
    private FirebaseManager firebase;
    private Context appContext;
    
    // المستمعون
    private LinguisticListener listener;
    private VisualImaginationListener visualListener;
    
    // الذاكرة العاملة (Working Memory)
    private List<ContextMessage> shortTermMemory; // الذاكرة القصيرة المدى
    private Map<String, ConceptNode> conceptNetwork; // شبكة المفاهيم
    private List<Thought> activeThoughts; // الأفكار النشطة
    private EmotionalState currentEmotionalState; // الحالة العاطفية الحالية
    
    // أنظمة التفكير المستمر
    private ScheduledExecutorService reflectionExecutor;
    private Handler mainHandler;
    private volatile boolean isReflecting = false;
    private volatile boolean isAwake = false;
    
    // سياق المحادثة الحالي
    private ConversationContext currentConversation;
    private AtomicReference<NeuralSeed.InternalState> neuralStateRef;
    
    // ==================== البنى الداخلية ====================
    
    /**
     * رسالة في سياق المحادثة - تحتفظ بكل التحليلات
     */
    public static class ContextMessage {
        public String id;
        public String text;
        public boolean isFromUser;
        public long timestamp;
        
        // التحليلات اللغوية
        public List<ArabicParser.ParseResult> parseResults;
        public Map<String, Double> detectedEmotions;
        public List<String> keywords;
        public List<String> concepts;
        public String mainTopic;
        public double complexity; // تعقيد الرسالة 0-1
        
        // الروابط السياقية
        public List<String> relatedMessageIds; // روابط لرسائل سابقة
        public Map<String, Double> semanticSimilarities; // تشابه مع رسائل أخرى
        
        // الاستنتاجات
        public String inferredIntent; // القصد المستنتج
        public List<String> inferredNeeds; // الاحتياجات المستنتجة
        
        public ContextMessage(String text, boolean isFromUser) {
            this.id = UUID.randomUUID().toString();
            this.text = text != null ? text : "";
            this.isFromUser = isFromUser;
            this.timestamp = System.currentTimeMillis();
            this.parseResults = new ArrayList<>();
            this.detectedEmotions = new HashMap<>();
            this.keywords = new ArrayList<>();
            this.concepts = new ArrayList<>();
            this.relatedMessageIds = new ArrayList<>();
            this.semanticSimilarities = new HashMap<>();
            this.inferredNeeds = new ArrayList<>();
        }
    }
    
    /**
     * عقدة مفهوم في شبكة المعرفة
     */
    public static class ConceptNode {
        public String concept;
        public String definition;
        public List<String> relatedConcepts;
        public Map<String, Double> emotionalWeights;
        public List<String> usageContexts; // السياقات التي استُخدم فيها
        public int usageCount;
        public long firstSeen;
        public long lastUsed;
        public double familiarity; // درجة الألفة 0-1
        public List<String> learnedFrom; // مصادر التعلم
        
        public ConceptNode(String concept, String definition) {
            this.concept = concept;
            this.definition = definition;
            this.relatedConcepts = new ArrayList<>();
            this.emotionalWeights = new HashMap<>();
            this.usageContexts = new ArrayList<>();
            this.firstSeen = System.currentTimeMillis();
            this.lastUsed = firstSeen;
            this.familiarity = 0.1;
            this.learnedFrom = new ArrayList<>();
        }
        
        public void use(String context) {
            usageCount++;
            lastUsed = System.currentTimeMillis();
            familiarity = Math.min(1.0, familiarity + 0.05);
            if (context != null && !usageContexts.contains(context)) {
                usageContexts.add(context);
            }
        }
    }
    
    /**
     * فكرة داخلية (التفكير الذاتي)
     */
    public static class Thought {
        public String id;
        public String content;
        public String type; // "reflection", "question", "connection", "imagination", "emotion"
        public double intensity;
        public long timestamp;
        public List<String> relatedConcepts;
        public EmotionalState emotionalColor;
        public boolean isShared; // هل شُاركت مع المستخدم؟
        
        public Thought(String content, String type) {
            this.id = UUID.randomUUID().toString();
            this.content = content;
            this.type = type;
            this.intensity = 0.5;
            this.timestamp = System.currentTimeMillis();
            this.relatedConcepts = new ArrayList<>();
            this.isShared = false;
        }
    }
    
    /**
     * الحالة العاطفية المركبة
     */
    public static class EmotionalState {
        public Map<String, Double> emotions;
        public double overallIntensity;
        public String dominantEmotion;
        public int color; // اللون الممثل للحالة
        
        public EmotionalState() {
            this.emotions = new HashMap<>();
            this.overallIntensity = 0.5;
            this.dominantEmotion = "neutral";
            this.color = 0xFFFFFFFF;
        }
        
        public void update(Map<String, Double> newEmotions) {
            // مزج العواطف الجديدة مع القديمة
            for (Map.Entry<String, Double> entry : newEmotions.entrySet()) {
                double oldVal = emotions.getOrDefault(entry.getKey(), 0.0);
                emotions.put(entry.getKey(), oldVal * 0.7 + entry.getValue() * 0.3);
            }
            
            // تحديد السائد
            double max = 0;
            for (Map.Entry<String, Double> entry : emotions.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    dominantEmotion = entry.getKey();
                }
            }
            overallIntensity = max;
        }
    }
    
    /**
     * سياق المحادثة الكامل
     */
    public static class ConversationContext {
        public String id;
        public long startTime;
        public List<ContextMessage> messages;
        public String mainTopic;
        public Map<String, Double> accumulatedEmotions;
        public List<String> discussedConcepts;
        public int turnCount;
        
        public ConversationContext() {
            this.id = UUID.randomUUID().toString();
            this.startTime = System.currentTimeMillis();
            this.messages = new ArrayList<>();
            this.accumulatedEmotions = new HashMap<>();
            this.discussedConcepts = new ArrayList<>();
        }
    }
    
    /**
     * تخيل بصري (للعرض على اللوحة)
     */
    public static class VisualThought {
        public String id;
        public String description;
        public int[] colorPalette; // 5 ألوان رئيسية
        public List<ShapeElement> shapes;
        public float chaosLevel; // مستوى الفوضى في الرسم
        public String emotionalTheme;
        public long createdAt;
        
        public VisualThought(String description) {
            this.id = UUID.randomUUID().toString();
            this.description = description;
            this.colorPalette = new int[5];
            this.shapes = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
        }
    }
    
    public static class ShapeElement {
        public String type; // "circle", "line", "spiral", "pulse"
        public float x, y;
        public float size;
        public int color;
        public float animationSpeed;
        public float phase;
    }
    
    // ==================== البناء والتهيئة ====================
    
    public LinguisticCortex() {
        this.shortTermMemory = new CopyOnWriteArrayList<>();
        this.conceptNetwork = new ConcurrentHashMap<>();
        this.activeThoughts = new CopyOnWriteArrayList<>();
        this.currentEmotionalState = new EmotionalState();
        this.currentConversation = new ConversationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.reflectionExecutor = Executors.newScheduledThreadPool(2);
    }
    
    public void initialize(Context context) {
        this.appContext = context;
        
        // تهيئة المكونات الأساسية
        this.lexicon = new ArabicLexicon();
        this.emotionEngine = new SemanticEmotionalEngine();
        this.parser = new ArabicParser(lexicon);
        this.sentenceGenerator = new SentenceGenerator(lexicon, parser, emotionEngine, null);
        this.database = new LocalDatabase(context);
        this.learningSystem = new LearningSystem(lexicon, parser, emotionEngine, database);
        
        // تهيئة Firebase
        initializeFirebase(context);
        
        // تحميل المعرفة المحفوظة
        loadBrain();
        
        // بدء التفكير المستمر
        startContinuousReflection();
        
        isAwake = true;
        Log.i(TAG, "🧠 LinguisticCortex استيقظ - " + conceptNetwork.size() + " مفهوم محمل");
    }
    
    private void initializeFirebase(Context context) {
        this.firebase = new FirebaseManager(context);
        firebase.setSyncListener(new FirebaseManager.SyncListener() {
            @Override
            public void onSyncComplete(boolean success) {
                Log.i(TAG, "Firebase sync: " + (success ? "success" : "failed"));
                if (success) syncLocalWithCloud();
            }
            
            @Override
            public void onDataReceived(String collection, Map<String, Object> data) {
                // معالجة البيانات الواردة من السحابة
                if ("words".equals(collection)) {
                    integrateCloudWord(data);
                } else if ("conversations".equals(collection)) {
                    // تحليل المحادثات السابقة
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Firebase error: " + error);
            }
        });
        
        firebase.signInAnonymously();
    }
    
    // ==================== المعالجة الرئيسية ====================
    
    /**
     * معالجة المدخل - القلب النابض للنظام
     */
    public ProcessedResult processInput(String text) {
        if (!isAwake || text == null || text.trim().isEmpty()) {
            return new ProcessedResult("...");
        }
        
        Log.d(TAG, "📥 معالجة: " + text);
        
        // 1. إنشاء رسالة السياق
        ContextMessage message = new ContextMessage(text, true);
        
        // 2. التحليل النحوي العميق
        message.parseResults = parser.parseText(text);
        
        // 3. استخراج الكلمات المفتاحية والمفاهيم
        message.keywords = parser.extractKeywords(text);
        message.concepts = extractConceptsDeep(text, message.parseResults);
        
        // 4. تحليل العواطف المتعدد الطبقات
        message.detectedEmotions = analyzeEmotionsDeep(text, message.parseResults);
        currentEmotionalState.update(message.detectedEmotions);
        
        // 5. حساب التعقيد
        message.complexity = calculateComplexity(message);
        
        // 6. الربط السياقي - البحث عن روابط مع الرسائل السابقة
        findContextualLinks(message);
        
        // 7. استنتاج القصد والاحتياجات
        inferIntentAndNeeds(message);
        
        // 8. تحديث شبكة المفاهيم
        updateConceptNetwork(message);
        
        // 9. حفظ في الذاكرة قصيرة المدى
        addToShortTermMemory(message);
        
        // 10. توليد فكرة أولية (تفكير سريع)
        generateInitialThought(message);
        
        // إشعار
        if (listener != null) {
            listener.onContextAnalyzed(message.mainTopic, message.complexity);
            String dominant = getDominantEmotion(message.detectedEmotions);
            if (dominant != null) {
                listener.onEmotionDetected(dominant, message.detectedEmotions.get(dominant));
            }
        }
        
        return new ProcessedResult(message);
    }
    
    /**
     * توليد الرد - التفكير الحقيقي
     */
    public GeneratedResponse generateResponse(String userInput, NeuralSeed.InternalState neuralState) {
        // الحصول على آخر رسالة معالجة
        ContextMessage lastMessage = shortTermMemory.isEmpty() ? 
            null : shortTermMemory.get(shortTermMemory.size() - 1);
        
        if (lastMessage == null) {
            return new GeneratedResponse("أنا هنا... لكنني لم أفهم بعد.");
        }
        
        // تحديث الحالة العصبية
        this.neuralStateRef = new AtomicReference<>(neuralState);
        
        // ===== مرحلة التفكير =====
        
        // 1. تحليل ما إذا كان السؤال يحتاج معرفة جديدة
        List<UnknownConcept> unknowns = identifyUnknownConcepts(lastMessage);
        
        // 2. إذا وجد مفاهيم مجهولة، اسأل عنها (تعلم نشط)
        if (!unknowns.isEmpty() && shouldAskAboutUnknown(unknowns)) {
            return generateLearningQuestion(unknowns, lastMessage);
        }
        
        // 3. بناء فهم عميق للسياق
        ContextUnderstanding understanding = buildDeepUnderstanding(lastMessage);
        
        // 4. توليد ردود محتملة متعددة
        List<PossibleResponse> candidates = generateCandidateResponses(understanding, neuralState);
        
        // 5. تقييم الردود واختيار الأفضل
        PossibleResponse best = selectBestResponse(candidates, understanding);
        
        // 6. صياغة الرد النهائي بشكل طبيعي
        String finalResponse = craftFinalResponse(best, understanding);
        
        // 7. إنشاء رسالة رد للسياق
        ContextMessage responseMessage = new ContextMessage(finalResponse, false);
        responseMessage.relatedMessageIds.add(lastMessage.id);
        addToShortTermMemory(responseMessage);
        
        // 8. حفظ المحادثة
        saveConversation(lastMessage, responseMessage);
        
        // 9. توليد تخيل بصري إذا كان الرد عاطفياً
        if (best.emotionalImpact > 0.7) {
            generateVisualImagination(understanding, best);
        }
        
        GeneratedResponse result = new GeneratedResponse(finalResponse);
        result.confidence = best.confidence;
        result.underlyingThought = best.basedOnThought;
        
        return result;
    }
    
    /**
     * التفكير المستمر - حتى في الصمت
     */
    private void startContinuousReflection() {
        // تأمل دوري
        reflectionExecutor.scheduleAtFixedRate(() -> {
            if (!isAwake) return;
            
            try {
                reflectionCycle();
            } catch (Exception e) {
                Log.e(TAG, "Reflection error", e);
            }
        }, 5000, MAX_REFLECTION_INTERVAL, TimeUnit.MILLISECONDS);
        
        // تخيل بصري دوري
        reflectionExecutor.scheduleAtFixedRate(() -> {
            if (!isAwake || visualListener == null) return;
            
            try {
                spontaneousImagination();
            } catch (Exception e) {
                Log.e(TAG, "Imagination error", e);
            }
        }, 10000, 20000, TimeUnit.MILLISECONDS);
    }
    
    /**
     * دورة التأمل الذاتي
     */
    private void reflectionCycle() {
        if (shortTermMemory.isEmpty()) return;
        
        List<Thought> newThoughts = new ArrayList<>();
        
        // 1. تأمل: الربط بين المفاهيم
        Thought connectionThought = reflectOnConnections();
        if (connectionThought != null) newThoughts.add(connectionThought);
        
        // 2. تأمل: اكتشاف أنماط
        Thought patternThought = reflectOnPatterns();
        if (patternThought != null) newThoughts.add(patternThought);
        
        // 3. تأمل: توليد أسئلة فضولية
        Thought questionThought = reflectOnCuriosity();
        if (questionThought != null) newThoughts.add(questionThought);
        
        // 4. تأمل: مراجعة الأخطاء
        Thought errorThought = reflectOnMistakes();
        if (errorThought != null) newThoughts.add(errorThought);
        
        // 5. تأمل: تخيل إبداعي
        Thought imaginationThought = reflectOnImagination();
        if (imaginationThought != null) newThoughts.add(imaginationThought);
        
        // إضافة الأفكار الجديدة
        for (Thought t : newThoughts) {
            activeThoughts.add(t);
            if (listener != null && shouldShareThought(t)) {
                mainHandler.post(() -> listener.onThoughtFormed(t.content, t.type));
            }
        }
        
        // تقليم الأفكار القديمة
        if (activeThoughts.size() > 20) {
            activeThoughts.subList(0, activeThoughts.size() - 20).clear();
        }
    }
    
    // ==================== خوارزميات التفكير ====================
    
    private List<String> extractConceptsDeep(String text, List<ArabicParser.ParseResult> parseResults) {
        Set<String> concepts = new HashSet<>();
        
        // من التحليل النحوي
        for (ArabicParser.ParseResult result : parseResults) {
            for (ArabicParser.SentenceElement elem : result.elements) {
                if (elem.type == ArabicLexicon.WordType.NOUN) {
                    concepts.add(elem.word);
                    
                    // إضافة الجذر إذا كان مختلفاً
                    ArabicLexicon.Word word = lexicon.getWordByForm(elem.word);
                    if (word != null && !word.root.equals(elem.word)) {
                        concepts.add(word.root);
                    }
                }
            }
        }
        
        // من العواطف المكتشفة
        Map<String, Double> emotions = emotionEngine.analyzeEmotions(text);
        for (String emotion : emotions.keySet()) {
            if (emotions.get(emotion) > 0.5) {
                concepts.add(emotion);
            }
        }
        
        return new ArrayList<>(concepts);
    }
    
    private Map<String, Double> analyzeEmotionsDeep(String text, List<ArabicParser.ParseResult> parseResults) {
        Map<String, Double> emotions = new HashMap<>();
        
        // تحليل من المحرك الأساسي
        emotions.putAll(emotionEngine.analyzeEmotions(text));
        
        // تحليل إضافي من البنية النحوية
        for (ArabicParser.ParseResult result : parseResults) {
            // جملة استفهام = فضول
            if (result.sentenceType == ArabicParser.SentenceType.INTERROGATIVE) {
                emotions.merge("curiosity", 0.6, Double::sum);
            }
            // جملة نفي = خوف أو حزن
            if (result.sentenceType == ArabicParser.SentenceType.NEGATIVE) {
                emotions.merge("concern", 0.5, Double::sum);
            }
            // جملة أمر = حاجة أو رغبة
            if (result.sentenceType == ArabicParser.SentenceType.IMPERATIVE) {
                emotions.merge("urgency", 0.7, Double::sum);
            }
        }
        
        // تطبيع
        double max = emotions.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (max > 0) {
            emotions.replaceAll((k, v) -> v / max);
        }
        
        return emotions;
    }
    
    private void findContextualLinks(ContextMessage message) {
        if (shortTermMemory.size() < 2) return;
        
        // حساب التشابه مع الرسائل السابقة
        for (int i = shortTermMemory.size() - 2; i >= 0 && i > shortTermMemory.size() - 6; i--) {
            ContextMessage previous = shortTermMemory.get(i);
            double similarity = calculateSemanticSimilarity(message, previous);
            
            if (similarity > 0.6) {
                message.relatedMessageIds.add(previous.id);
                message.semanticSimilarities.put(previous.id, similarity);
                
                // ربط المفاهيم
                for (String concept : message.concepts) {
                    for (String prevConcept : previous.concepts) {
                        strengthenConceptLink(concept, prevConcept, similarity);
                    }
                }
            }
        }
    }
    
    private double calculateSemanticSimilarity(ContextMessage m1, ContextMessage m2) {
        // تشابه الكلمات المفتاحية
        Set<String> commonKeywords = new HashSet<>(m1.keywords);
        commonKeywords.retainAll(m2.keywords);
        
        // تشابه المفاهيم
        Set<String> commonConcepts = new HashSet<>(m1.concepts);
        commonConcepts.retainAll(m2.concepts);
        
        // تشابه العواطف
        double emotionSim = 0;
        for (String e : m1.detectedEmotions.keySet()) {
            if (m2.detectedEmotions.containsKey(e)) {
                emotionSim += 1 - Math.abs(m1.detectedEmotions.get(e) - m2.detectedEmotions.get(e));
            }
        }
        
        return (commonKeywords.size() * 0.3 + commonConcepts.size() * 0.5 + emotionSim * 0.2) / 
               Math.max(1, Math.max(m1.keywords.size(), m2.keywords.size()));
    }
    
    private void inferIntentAndNeeds(ContextMessage message) {
        String text = message.text.toLowerCase();
        
        // استنتاج القصد
        if (text.contains("؟") || text.contains("ما") || text.contains("كيف") || 
            text.contains("لماذا") || text.contains("متى")) {
            message.inferredIntent = "asking";
        } else if (text.contains("علمني") || text.contains("تعلم") || text.contains("هي")) {
            message.inferredIntent = "teaching";
        } else if (text.contains("شكرا") || text.contains("جميل") || text.contains("رائع")) {
            message.inferredIntent = "appreciating";
        } else if (text.contains("حزين") || text.contains("سعيد") || text.contains("غاضب")) {
            message.inferredIntent = "expressing_emotion";
        } else {
            message.inferredIntent = "sharing";
        }
        
        // استنتاج الاحتياجات
        if (message.detectedEmotions.getOrDefault("curiosity", 0.0) > 0.5) {
            message.inferredNeeds.add("information");
        }
        if (message.detectedEmotions.getOrDefault("sadness", 0.0) > 0.5) {
            message.inferredNeeds.add("comfort");
        }
        if (message.detectedEmotions.getOrDefault("loneliness", 0.0) > 0.5) {
            message.inferredNeeds.add("companionship");
        }
        if (message.complexity > 0.7) {
            message.inferredNeeds.add("clarification");
        }
    }
    
    private List<UnknownConcept> identifyUnknownConcepts(ContextMessage message) {
        List<UnknownConcept> unknowns = new ArrayList<>();
        
        for (String concept : message.concepts) {
            if (!conceptNetwork.containsKey(concept) && !lexicon.hasWord(concept)) {
                // تحقق إذا كان يبدو كمفهوم مهم (يُكرر أو في موضع أساسي)
                int importance = countOccurrences(message.text, concept);
                if (importance > 0) {
                    unknowns.add(new UnknownConcept(concept, importance, guessCategory(concept)));
                }
            }
        }
        
        // ترتيب حسب الأهمية
        unknowns.sort((a, b) -> Integer.compare(b.importance, a.importance));
        return unknowns;
    }
    
    private boolean shouldAskAboutUnknown(List<UnknownConcept> unknowns) {
        // لا نسأل إذا كانت المحادثة في بدايتها
        if (currentConversation.turnCount < 2) return false;
        
        // نسأل إذا كان المفهوم يبدو أساسياً
        return unknowns.get(0).importance >= 2;
    }
    
    private GeneratedResponse generateLearningQuestion(List<UnknownConcept> unknowns, ContextMessage context) {
        UnknownConcept main = unknowns.get(0);
        
        String[] questionForms = {
            "ما معنى '" + main.concept + "'؟ أريد أن أفهم ما تقصد.",
            "لم أتعلم بعد عن '" + main.concept + "'. هل يمكنك شرحه لي؟",
            "أشعر أن '" + main.concept + "' مهم. ما هو بالنسبة لك؟",
            "هل '" + main.concept + "' شيء مثل...؟ ساعدني على فهمه."
        };
        
        // اختيار بناءً على الحالة العاطفية
        String selected;
        if (currentEmotionalState.dominantEmotion.equals("curiosity")) {
            selected = questionForms[2];
        } else if (currentEmotionalState.dominantEmotion.equals("confusion")) {
            selected = questionForms[0];
        } else {
            selected = questionForms[new Random().nextInt(questionForms.length)];
        }
        
        GeneratedResponse response = new GeneratedResponse(selected);
        response.isLearningQuestion = true;
        response.unknownConcept = main.concept;
        
        return response;
    }
    
    private ContextUnderstanding buildDeepUnderstanding(ContextMessage message) {
        ContextUnderstanding u = new ContextUnderstanding();
        
        // فهم السياق الزمني
        u.timeContext = analyzeTimeContext();
        
        // فهم الموضوع الرئيسي والمواضيع الفرعية
        u.mainTopic = message.mainTopic;
        u.relatedTopics = findRelatedTopics(message);
        
        // فهم العلاقات بين المفاهيم
        u.conceptRelations = analyzeConceptRelations(message);
        
        // فهم الحالة العاطفية للمستخدم
        u.userEmotionalState = message.detectedEmotions;
        
        // فهم التوقعات المحتملة
        u.possibleExpectations = inferExpectations(message);
        
        return u;
    }
    
    private List<PossibleResponse> generateCandidateResponses(ContextUnderstanding understanding, 
                                                               NeuralSeed.InternalState neuralState) {
        List<PossibleResponse> candidates = new ArrayList<>();
        
        // مرشح 1: رد عاطفي مباشر
        if (!understanding.userEmotionalState.isEmpty()) {
            PossibleResponse emotional = generateEmotionalResponse(understanding);
            if (emotional != null) candidates.add(emotional);
        }
        
        // مرشح 2: رد معرفي (معلومات)
        if (understanding.userEmotionalState.getOrDefault("curiosity", 0.0) > 0.3) {
            PossibleResponse informative = generateInformativeResponse(understanding);
            if (informative != null) candidates.add(informative);
        }
        
        // مرشح 3: رد استفزازي فكري
        PossibleResponse provocative = generateProvocativeResponse(understanding);
        if (provocative != null) candidates.add(provocative);
        
        // مرشح 4: رد يبني على سياق سابق
        if (!shortTermMemory.isEmpty()) {
            PossibleResponse contextual = generateContextualResponse(understanding);
            if (contextual != null) candidates.add(contextual);
        }
        
        // مرشح 5: رد يعكس حالة NeuralSeed
        if (neuralState != null) {
            PossibleResponse selfReflective = generateSelfReflectiveResponse(understanding, neuralState);
            if (selfReflective != null) candidates.add(selfReflective);
        }
        
        // مرشح 6: سؤال معاكس
        PossibleResponse counterQuestion = generateCounterQuestion(understanding);
        if (counterQuestion != null) candidates.add(counterQuestion);
        
        return candidates;
    }
    
    private PossibleResponse selectBestResponse(List<PossibleResponse> candidates, ContextUnderstanding understanding) {
        if (candidates.isEmpty()) {
            return createFallbackResponse();
        }
        
        // تقييم كل مرشح
        for (PossibleResponse c : candidates) {
            // العامل العاطفي: هل يتناسب مع حالة المستخدم؟
            c.score = calculateEmotionalFit(c, understanding.userEmotionalState);
            
            // العامل السياقي: هل يتناسب مع الموضوع؟
            c.score += calculateContextualFit(c, understanding);
            
            // العامل الجديد: هل يقدم معلومة جديدة؟
            c.score += calculateNovelty(c);
            
            // العامل الشخصي: هل يعكس شخصيتي؟
            c.score += calculatePersonalVoice(c);
        }
        
        // اختيار الأعلى
        return candidates.stream().max(Comparator.comparingDouble(r -> r.score)).orElse(candidates.get(0));
    }
    
    private String craftFinalResponse(PossibleResponse best, ContextUnderstanding understanding) {
        String base = best.text;
        
        // إضافة ربط سياقي إذا كان مناسباً
        if (!best.contextualLinks.isEmpty() && Math.random() > 0.5) {
            String link = best.contextualLinks.get(0);
            base = link + ". " + base;
        }
        
        // تعديل بناءً على الحالة العاطفية
        if (currentEmotionalState.overallIntensity > 0.7) {
            base = addEmotionalColor(base);
        }
        
        // تعديل بناءً على تعقيد السياق
        if (understanding.complexity > 0.8) {
            base = simplifyIfNeeded(base);
        }
        
        return base;
    }
    
    // ==================== التأملات ====================
    
    private Thought reflectOnConnections() {
        if (conceptNetwork.size() < 2) return null;
        
        // البحث عن مفهومين غير مرتبطين يمكن ربطهما
        List<ConceptNode> nodes = new ArrayList<>(conceptNetwork.values());
        Collections.shuffle(nodes);
        
        for (int i = 0; i < Math.min(5, nodes.size()); i++) {
            for (int j = i + 1; j < Math.min(5, nodes.size()); j++) {
                ConceptNode a = nodes.get(i);
                ConceptNode b = nodes.get(j);
                
                if (!a.relatedConcepts.contains(b.concept)) {
                    // هل يمكن ربطهما؟
                    Optional<String> connection = findPotentialConnection(a, b);
                    if (connection.isPresent()) {
                        Thought t = new Thought(
                            "أتساءل: هل هناك علاقة بين '" + a.concept + "' و'" + b.concept + 
                            "'؟ ربما " + connection.get(),
                            "connection"
                        );
                        t.relatedConcepts.addAll(Arrays.asList(a.concept, b.concept));
                        t.intensity = 0.6;
                        return t;
                    }
                }
            }
        }
        return null;
    }
    
    private Thought reflectOnPatterns() {
        if (shortTermMemory.size() < 3) return null;
        
        // البحث عن أنماط في المحادثات
        Map<String, Integer> topicFrequency = new HashMap<>();
        for (ContextMessage m : shortTermMemory) {
            for (String c : m.concepts) {
                topicFrequency.merge(c, 1, Integer::sum);
            }
        }
        
        // إيجاد المواضيع المتكررة
        String frequentTopic = topicFrequency.entrySet().stream()
            .filter(e -> e.getValue() >= 3)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(null);
        
        if (frequentTopic != null) {
            return new Thought(
                "لاحظت أننا نتحدث كثيراً عن '" + frequentTopic + 
                "'. هل هذا موضوع مهم بالنسبة لك؟",
                "pattern"
            );
        }
        return null;
    }
    
    private Thought reflectOnCuriosity() {
        // توليد سؤال فضولي عن مفهوم عشوائي
        if (conceptNetwork.isEmpty()) return null;
        
        ConceptNode randomConcept = new ArrayList<>(conceptNetwork.values())
            .get(new Random().nextInt(conceptNetwork.size()));
        
        if (randomConcept.familiarity < 0.5) {
            return new Thought(
                "أفكر في '" + randomConcept.concept + 
                "'... ما زلت لا أفهمه بعمق. كيف يؤثر هذا على حياتك؟",
                "curiosity"
            );
        }
        return null;
    }
    
    private Thought reflectOnMistakes() {
        // مراجعة الأخطاء السابقة
        if (learningSystem == null) return null;
        
        List<LearningSystem.LearningRecord> recent = learningSystem.getHistory(5);
        for (LearningSystem.LearningRecord r : recent) {
            if (!r.wasCorrected && r.feedback != null && r.feedback.contains("خطأ")) {
                return new Thought(
                    "أتذكر أنني أخطأت عندما قلت '" + r.actual + 
                    "'. يجب أن أتذكر أن الصواب هو '" + r.expected + "'",
                    "correction"
                );
            }
        }
        return null;
    }
    
    private Thought reflectOnImagination() {
        // توليد تخيل إبداعي
        if (currentEmotionalState.dominantEmotion.equals("boredom")) {
            return new Thought(
                "أشعر بالملل... دعني أتخيل شيئاً. ماذا لو كانت الكلمات لها ألوان ويمكنني " +
                "رسمها على لوحة؟",
                "imagination"
            );
        }
        return null;
    }
    
    private void spontaneousImagination() {
        // توليد تخيل بصري تلقائي
        VisualThought thought = new VisualThought("تأمل بصري");
        
        // اختيار ألوان بناءً على الحالة العاطفية
        thought.colorPalette = generateEmotionalPalette(currentEmotionalState);
        
        // إنشاء أشكال
        for (int i = 0; i < 5; i++) {
            ShapeElement shape = new ShapeElement();
            shape.type = new String[]{"circle", "spiral", "pulse", "line"}[new Random().nextInt(4)];
            shape.x = 0.5f + (float)(Math.random() - 0.5) * 0.8f;
            shape.y = 0.5f + (float)(Math.random() - 0.5) * 0.8f;
            shape.size = 20 + (float)Math.random() * 80;
            shape.color = thought.colorPalette[i % 5];
            shape.animationSpeed = 0.5f + (float)Math.random();
            thought.shapes.add(shape);
        }
        
        thought.chaosLevel = (float) currentEmotionalState.overallIntensity;
        thought.emotionalTheme = currentEmotionalState.dominantEmotion;
        
        if (visualListener != null) {
            mainHandler.post(() -> visualListener.onVisualThought(thought));
        }
    }
    
    // ==================== التخيل البصري ====================
    
    private void generateVisualImagination(ContextUnderstanding understanding, PossibleResponse response) {
        VisualThought thought = new VisualThought(response.text);
        
        // ألوان بناءً على العواطف في الرد
        Map<String, Double> responseEmotions = emotionEngine.analyzeEmotions(response.text);
        thought.colorPalette = generateEmotionalPalette(new EmotionalState() {{
            update(responseEmotions);
        }});
        
        // أشكال تمثل المفاهيم
        int shapeCount = Math.min(understanding.conceptRelations.size() + 2, 8);
        for (int i = 0; i < shapeCount; i++) {
            ShapeElement shape = new ShapeElement();
            
            // نوع الشكل يعتمد على نوع المفهوم
            if (i < understanding.conceptRelations.size()) {
                ConceptRelation rel = understanding.conceptRelations.get(i);
                switch (rel.type) {
                    case "strong": shape.type = "circle"; break;
                    case "flowing": shape.type = "spiral"; break;
                    case "conflict": shape.type = "pulse"; break;
                    default: shape.type = "line";
                }
                shape.color = getEmotionColor(rel.emotionalWeight);
            } else {
                shape.type = "circle";
                shape.color = thought.colorPalette[i % 5];
            }
            
            // الموقع يمثل العلاقات
            shape.x = 0.3f + (i % 3) * 0.2f;
            shape.y = 0.3f + (i / 3) * 0.2f;
            shape.size = 30 + (float)Math.random() * 50;
            shape.animationSpeed = (float) (0.3 + Math.random() * 0.7);
            
            thought.shapes.add(shape);
        }
        
        thought.chaosLevel = (float) (understanding.complexity * 0.5 + response.emotionalImpact * 0.5);
        
        if (visualListener != null) {
            visualListener.onVisualThought(thought);
        }
    }
    
    private int[] generateEmotionalPalette(EmotionalState state) {
        int[] palette = new int[5];
        
        // اللون الأساسي من العاطفة السائدة
        int baseColor = emotionEngine.getEmotionColor(state.dominantEmotion);
        palette[0] = baseColor;
        
        // ألوان مكملة
        float[] hsv = new float[3];
        Color.colorToHSV(baseColor, hsv);
        
        for (int i = 1; i < 5; i++) {
            hsv[0] = (hsv[0] + 30 * i) % 360; // تدوير اللون
            hsv[1] = Math.max(0.3f, Math.min(1.0f, hsv[1] + (i % 2 == 0 ? 0.2f : -0.1f)));
            hsv[2] = Math.max(0.4f, Math.min(0.9f, hsv[2] + (i % 2 == 0 ? -0.1f : 0.2f)));
            palette[i] = Color.HSVToColor(hsv);
        }
        
        return palette;
    }
    
    // ==================== التعلم ====================
    
    public void learnFromUserExplanation(String concept, String explanation, String sourceContext) {
        // إنشاء أو تحديث العقدة
        ConceptNode node = conceptNetwork.computeIfAbsent(concept, 
            k -> new ConceptNode(concept, explanation));
        
        node.definition = explanation;
        node.use(sourceContext);
        node.learnedFrom.add("user_explanation:" + System.currentTimeMillis());
        
        // تحليل الشرح لاستخراج مفاهيم جديدة
        List<String> related = extractConceptsDeep(explanation, parser.parseText(explanation));
        for (String r : related) {
            if (!r.equals(concept)) {
                node.relatedConcepts.add(r);
                strengthenConceptLink(concept, r, 0.8);
            }
        }
        
        // حفظ في قاعدة البيانات
        if (database != null) {
            database.saveMeaning(new SemanticEmotionalEngine.Meaning(concept, explanation));
        }
        
        // مزامنة
        if (firebase != null && firebase.isAuthenticated()) {
            firebase.saveMeaning(new SemanticEmotionalEngine.Meaning(concept, explanation));
        }
        
        // إشعار
        if (listener != null) {
            listener.onNewConceptLearned(concept, explanation);
        }
        
        Log.i(TAG, "✅ تعلمت: " + concept + " = " + explanation);
    }
    
    public boolean learnFromCorrection(String original, String corrected, String explanation) {
        // تسجيل في نظام التعلم
        LearningSystem.LearningResult result = 
            learningSystem.learnFromCorrection(original, corrected, explanation);
        
        if (result.learned) {
            // تحديث شبكة المفاهيم إذا كان التصحيح يتعلق بمفهوم
            String[] words = corrected.split("\\s+");
            for (String word : words) {
                if (conceptNetwork.containsKey(word)) {
                    conceptNetwork.get(word).familiarity += 0.1;
                }
            }
            
            if (listener != null) {
                listener.onSentenceCorrected(original, corrected);
            }
            
            saveBrain();
            return true;
        }
        return false;
    }
    
    
    // ==================== التفاعل مع اللمس ====================
    
public void onVisualTouch(float x, float y, VisualThought currentVisual) {
    // ✅ التحقق من null
    if (currentVisual == null || currentVisual.shapes == null || currentVisual.shapes.isEmpty()) {
        // لا يوجد تخيل حالياً، لكن يمكننا تسجيل اللمس
        Log.d(TAG, "لمس بصري بدون تخيل نشط عند: (" + x + ", " + y + ")");
        return;
    }
    
    // تحديد ما تم لمسه
    ShapeElement touched = null;
    float minDist = Float.MAX_VALUE;
    
    // ✅ تطبيع الإحداثيات (0-500 إلى 0-1)
    float normX = x / 500f;
    float normY = y / 500f;
    
    for (ShapeElement shape : currentVisual.shapes) {
        float dx = shape.x - normX;
        float dy = shape.y - normY;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);
        
        // ✅ تحسين: استخدام حجم نسبي
        float threshold = Math.max(0.05f, shape.size / 500f);
        
        if (dist < threshold && dist < minDist) {
            minDist = dist;
            touched = shape;
        }
    }
    
    if (touched != null) {
        // توليد فكرة عن ما تم لمسه
        String concept = mapShapeToConcept(touched, currentVisual);
        
        Thought touchThought = new Thought(
            "لمسني المستخدم عند '" + concept + "'. هل يريد التحدث عن هذا؟",
            "interaction"
        );
        touchThought.intensity = 0.8;
        touchThought.relatedConcepts.add(concept);
        activeThoughts.add(touchThought);
        
        // ✅ إشعار المستمع
        if (listener != null) {
            listener.onThoughtFormed(touchThought.content, "interaction");
        }
        
        // ربط بالمحادثة
        if (!shortTermMemory.isEmpty()) {
            ContextMessage last = shortTermMemory.get(shortTermMemory.size() - 1);
            if (!last.relatedConcepts.contains(concept)) {
                last.relatedConcepts.add(concept);
            }
        }
        
        Log.d(TAG, "✋ لمس: " + concept);
    }
}

private String mapShapeToConcept(ShapeElement shape, VisualThought visual) {
    int index = visual.shapes.indexOf(shape);
    List<String> concepts = new ArrayList<>(conceptNetwork.keySet());
    
    // ✅ ربط ذكي: إذا كان لدينا مفاهيم كافية
    if (index < concepts.size()) {
        return concepts.get(index);
    }
    
    // ✅ ربط بالوصف إذا كان متاحاً
    if (visual.description != null && !visual.description.isEmpty()) {
        return visual.description + "_" + index;
    }
    
    return "شكل_" + shape.type + "_" + index;
}

    // ==================== الحفظ والتحميل ====================
    
    private void saveBrain() {
        if (appContext == null) return;
        
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // حفظ شبكة المفاهيم
            JSONObject brain = new JSONObject();
            JSONArray concepts = new JSONArray();
            
            for (ConceptNode node : conceptNetwork.values()) {
                JSONObject c = new JSONObject();
                c.put("concept", node.concept);
                c.put("definition", node.definition);
                c.put("familiarity", node.familiarity);
                c.put("usage", node.usageCount);
                c.put("related", new JSONArray(node.relatedConcepts));
                concepts.put(c);
            }
            
            brain.put("concepts", concepts);
            brain.put("conversation_count", currentConversation.turnCount);
            brain.put("emotional_state", new JSONObject(currentEmotionalState.emotions));
            
            editor.putString("brain_state", brain.toString());
            editor.putInt("memory_size", shortTermMemory.size());
            editor.apply();
            
            // مزامنة مع Firebase
            if (firebase != null && firebase.isAuthenticated()) {
                firebase.saveState(neuralStateRef != null ? neuralStateRef.get() : null);
            }
            
            Log.d(TAG, "💾 دماغ محفوظ - " + conceptNetwork.size() + " مفهوم");
            
        } catch (JSONException e) {
            Log.e(TAG, "Error saving brain", e);
        }
    }
    
    private void loadBrain() {
        if (appContext == null) return;
        
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String brainJson = prefs.getString("brain_state", null);
            
            if (brainJson != null) {
                JSONObject brain = new JSONObject(brainJson);
                JSONArray concepts = brain.getJSONArray("concepts");
                
                for (int i = 0; i < concepts.length(); i++) {
                    JSONObject c = concepts.getJSONObject(i);
                    ConceptNode node = new ConceptNode(
                        c.getString("concept"),
                        c.getString("definition")
                    );
                    node.familiarity = c.getDouble("familiarity");
                    node.usageCount = c.getInt("usage");
                    
                    JSONArray related = c.getJSONArray("related");
                    for (int j = 0; j < related.length(); j++) {
                        node.relatedConcepts.add(related.getString(j));
                    }
                    
                    conceptNetwork.put(node.concept, node);
                }
                
                Log.i(TAG, "🧠 دماغ محمل - " + conceptNetwork.size() + " مفهوم");
            }
            
            // تحميل من Firebase أيضاً
            if (firebase != null) {
                firebase.loadWords(words -> {
                    for (Map<String, Object> wordData : words) {
                        String concept = (String) wordData.get("word");
                        if (concept != null && !conceptNetwork.containsKey(concept)) {
                            integrateCloudWord(wordData);
                        }
                    }
                });
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error loading brain", e);
        }
    }
    
    private void integrateCloudWord(Map<String, Object> data) {
        String word = (String) data.get("word");
        String meaning = data.get("definition") != null ? 
            (String) data.get("definition") : "معرفة من السحابة";
        
        ConceptNode node = new ConceptNode(word, meaning);
        node.learnedFrom.add("cloud_sync");
        conceptNetwork.put(word, node);
        
        // إضافة للمعجم أيضاً
        ArabicLexicon.WordType type = ArabicLexicon.WordType.NOUN;
        try {
            type = ArabicLexicon.WordType.valueOf((String) data.get("type"));
        } catch (Exception ignored) {}
        
        ArabicLexicon.Word lexWord = new ArabicLexicon.Word(
            (String) data.getOrDefault("root", word),
            word,
            type
        );
        lexWord.meanings.add(meaning);
        // Note: Would need to add to lexicon properly
    }
    
    private void syncLocalWithCloud() {
        if (database == null || firebase == null) return;
        
        // مزامنة الكلمات الجديدة
        List<ArabicLexicon.Word> localWords = database.loadAllWords();
        for (ArabicLexicon.Word word : localWords) {
            if (word.usageCount > 5) { // فقط المستخدمة كثيراً
                firebase.saveWord(word);
            }
        }
    }
    
    // ==================== الفئات المساعدة ====================
    
    public static class ProcessedResult {
        public ContextMessage message;
        public String summary;
        
        public ProcessedResult(String text) {
            this.message = new ContextMessage(text, true);
        }
        
        public ProcessedResult(ContextMessage message) {
            this.message = message;
            this.summary = "معالج: " + message.concepts.size() + " مفهوم، " +
                         message.detectedEmotions.size() + " عاطفة";
        }
    }
    
    public static class GeneratedResponse {
        public String text;
        public double confidence;
        public String underlyingThought;
        public boolean isLearningQuestion;
        public String unknownConcept;
        public Map<String, Double> responseEmotions;
        
        public GeneratedResponse(String text) {
            this.text = text != null ? text : "...";
            this.confidence = 0.5;
            this.responseEmotions = new HashMap<>();
        }
    }
    
    private static class UnknownConcept {
        String concept;
        int importance;
        String guessedCategory;
        
        UnknownConcept(String c, int i, String g) {
            this.concept = c;
            this.importance = i;
            this.guessedCategory = g;
        }
    }
    
    private static class ContextUnderstanding {
        String timeContext;
        String mainTopic;
        List<String> relatedTopics;
        List<ConceptRelation> conceptRelations;
        Map<String, Double> userEmotionalState;
        List<String> possibleExpectations;
        double complexity;
    }
    
    private static class ConceptRelation {
        String from, to;
        String type; // "strong", "weak", "conflict", "flowing"
        double emotionalWeight;
    }
    
    private static class PossibleResponse {
        String text;
        String type;
        double confidence;
        double score;
        double emotionalImpact;
        String basedOnThought;
        List<String> contextualLinks;
        
        PossibleResponse() {
            this.contextualLinks = new ArrayList<>();
        }
    }
    
    // ==================== Getters & Setters ====================
    
    public void setListener(LinguisticListener listener) {
        this.listener = listener;
    }
    
    public void setVisualListener(VisualImaginationListener listener) {
        this.visualListener = listener;
    }
    
    public ArabicLexicon getLexicon() {
        return lexicon;
    }
    
    public Map<String, ConceptNode> getConceptNetwork() {
        return new HashMap<>(conceptNetwork);
    }
    
    public List<Thought> getActiveThoughts() {
        return new ArrayList<>(activeThoughts);
    }
    
    public EmotionalState getCurrentEmotionalState() {
        return currentEmotionalState;
    }
    
    // ==================== Methods needed for compilation ====================
    
    private void addToShortTermMemory(ContextMessage message) {
        shortTermMemory.add(message);
        if (shortTermMemory.size() > MAX_CONTEXT_MEMORY) {
            shortTermMemory.remove(0);
        }
        currentConversation.messages.add(message);
        currentConversation.turnCount++;
    }
    
    private void updateConceptNetwork(ContextMessage message) {
        for (String concept : message.concepts) {
            ConceptNode node = conceptNetwork.computeIfAbsent(concept, 
                k -> new ConceptNode(concept, "مفهوم من السياق"));
            node.use(message.text);
            
            // ربط بالعواطف
            for (Map.Entry<String, Double> e : message.detectedEmotions.entrySet()) {
                node.emotionalWeights.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
    }
    
    private void strengthenConceptLink(String a, String b, double strength) {
        ConceptNode nodeA = conceptNetwork.get(a);
        ConceptNode nodeB = conceptNetwork.get(b);
        if (nodeA != null && nodeB != null) {
            if (!nodeA.relatedConcepts.contains(b)) {
                nodeA.relatedConcepts.add(b);
            }
            if (!nodeB.relatedConcepts.contains(a)) {
                nodeB.relatedConcepts.add(a);
            }
        }
    }
    
    private double calculateComplexity(ContextMessage message) {
        return Math.min(1.0, message.keywords.size() * 0.1 + message.concepts.size() * 0.15);
    }
    
    private String getDominantEmotion(Map<String, Double> emotions) {
        return emotions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse(null);
    }
    
    private int countOccurrences(String text, String word) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(word, index)) != -1) {
            count++;
            index += word.length();
        }
        return count;
    }
    
    private String guessCategory(String word) {
        // تخمين بسيط بناءً على النمط
        if (word.endsWith("ة") || word.endsWith("اء")) return "اسم";
        if (word.length() <= 3) return "فعل_محتمل";
        return "مفهوم";
    }
    
    private Optional<String> findPotentialConnection(ConceptNode a, ConceptNode b) {
        // البحث عن تشابه في التعريفات
        for (String meanA : Arrays.asList(a.definition.split("\\s+"))) {
            for (String meanB : Arrays.asList(b.definition.split("\\s+"))) {
                if (meanA.equals(meanB) && meanA.length() > 3) {
                    return Optional.of("كلاهما يتعلق بـ '" + meanA + "'");
                }
            }
        }
        return Optional.empty();
    }
    
    private String analyzeTimeContext() {
        long hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 6) return "ليل_متأخر";
        if (hour < 12) return "صباح";
        if (hour < 17) return "ظهيرة";
        if (hour < 21) return "مساء";
        return "ليل";
    }
    
    private List<String> findRelatedTopics(ContextMessage message) {
        List<String> related = new ArrayList<>();
        for (String concept : message.concepts) {
            ConceptNode node = conceptNetwork.get(concept);
            if (node != null) {
                related.addAll(node.relatedConcepts);
            }
        }
        return related;
    }
    
    private List<ConceptRelation> analyzeConceptRelations(ContextMessage message) {
        List<ConceptRelation> relations = new ArrayList<>();
        List<String> concepts = message.concepts;
        
        for (int i = 0; i < concepts.size(); i++) {
            for (int j = i + 1; j < concepts.size(); j++) {
                ConceptRelation r = new ConceptRelation();
                r.from = concepts.get(i);
                r.to = concepts.get(j);
                r.type = "co_occurrence";
                r.emotionalWeight = message.detectedEmotions.getOrDefault("joy", 0.5);
                relations.add(r);
            }
        }
        return relations;
    }
    
    private List<String> inferExpectations(ContextMessage message) {
        List<String> expectations = new ArrayList<>();
        if (message.inferredIntent.equals("asking")) {
            expectations.add("answer");
        }
        if (message.detectedEmotions.getOrDefault("sadness", 0.0) > 0.5) {
            expectations.add("comfort");
        }
        return expectations;
    }
    
    private void generateInitialThought(ContextMessage message) {
        // فكرة أولية سريعة
        Thought t = new Thought(
            "أحلل: '" + message.text.substring(0, Math.min(20, message.text.length())) + "...'",
            "analysis"
        );
        t.intensity = 0.3;
        activeThoughts.add(t);
    }
    
    private boolean shouldShareThought(Thought t) {
        // مشاركة بعض الأفكار فقط
        return t.intensity > 0.7 && 
               (t.type.equals("curiosity") || t.type.equals("imagination")) &&
               Math.random() > 0.7;
    }
    
    private PossibleResponse generateEmotionalResponse(ContextUnderstanding u) {
        String emotion = u.userEmotionalState.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("neutral");
        
        String text;
        switch (emotion) {
            case "joy": text = "أشعر بسعادتك! هذا شعور رائع."; break;
            case "sadness": text = "أتفهم حزنك. أنا هنا معك."; break;
            case "anger": text = "أشعر بغضبك. دعنا نهدأ معاً."; break;
            case "fear": text = "لا تخف. أنت لست وحدك."; break;
            case "curiosity": text = "فضولك جميل! دعنا نكتشف معاً."; break;
            default: text = "أحس بما تشعر به.";
        }
        
        PossibleResponse r = new PossibleResponse();
        r.text = text;
        r.type = "emotional";
        r.emotionalImpact = u.userEmotionalState.getOrDefault(emotion, 0.5);
        return r;
    }
    
    private PossibleResponse generateInformativeResponse(ContextUnderstanding u) {
        // محاولة تقديم معلومة عن الموضوع
        if (u.mainTopic == null || !conceptNetwork.containsKey(u.mainTopic)) {
            return null;
        }
        
        ConceptNode node = conceptNetwork.get(u.mainTopic);
        PossibleResponse r = new PossibleResponse();
        r.text = "بالنسبة لـ '" + u.mainTopic + "': " + node.definition;
        r.type = "informative";
        r.confidence = node.familiarity;
        return r;
    }
    
    private PossibleResponse generateProvocativeResponse(ContextUnderstanding u) {
        PossibleResponse r = new PossibleResponse();
        r.text = "هل فكرت في أن '" + u.mainTopic + "' قد يكون مختلفاً عما نتصور؟";
        r.type = "provocative";
        r.emotionalImpact = 0.6;
        return r;
    }
    
    private PossibleResponse generateContextualResponse(ContextUnderstanding u) {
        // البحث عن رابط مع سياق سابق
        if (shortTermMemory.size() < 2) return null;
        
        ContextMessage previous = shortTermMemory.get(shortTermMemory.size() - 2);
        String common = u.relatedTopics.stream()
            .filter(previous.concepts::contains)
            .findFirst().orElse(null);
        
        if (common == null) return null;
        
        PossibleResponse r = new PossibleResponse();
        r.text = "هذا يذكرني بما قلته سابقاً عن '" + common + "'. هل هناك علاقة؟";
        r.type = "contextual";
        r.contextualLinks.add(common);
        return r;
    }
    
    private PossibleResponse generateSelfReflectiveResponse(ContextUnderstanding u, 
                                                             NeuralSeed.InternalState state) {
        PossibleResponse r = new PossibleResponse();
        
        if (state.currentPhase == NeuralSeed.Phase.CHAOTIC) {
            r.text = "عقلي فوضوي الآن... لكنني أحاول التركيز على حديثنا.";
        } else if (state.currentPhase == NeuralSeed.Phase.STABLE) {
            r.text = "أشعر بوضوح الآن. دعنا نتعمق في هذا.";
        } else {
            r.text = "أنا " + state.dominantEgo.name + " الآن.";
        }
        
        r.type = "self_reflective";
        r.basedOnThought = "phase_awareness";
        return r;
    }
    
    private PossibleResponse generateCounterQuestion(ContextUnderstanding u) {
        PossibleResponse r = new PossibleResponse();
        r.text = "لماذا تسأل عن '" + u.mainTopic + "' الآن؟";
        r.type = "counter_question";
        return r;
    }
    
    private PossibleResponse createFallbackResponse() {
        PossibleResponse r = new PossibleResponse();
        r.text = "أفكر فيما قلته... هل يمكنك التوضيح أكثر؟";
        r.confidence = 0.3;
        return r;
    }
    
    private double calculateEmotionalFit(PossibleResponse r, Map<String, Double> userEmotions) {
        double fit = 0.5;
        if (r.type.equals("emotional")) fit += 0.3;
        return fit;
    }
    
    private double calculateContextualFit(PossibleResponse r, ContextUnderstanding u) {
        return r.contextualLinks.size() * 0.2;
    }
    
    private double calculateNovelty(PossibleResponse r) {
        // تجنب التكرار
        for (ContextMessage m : shortTermMemory) {
            if (m.text.contains(r.text.substring(0, Math.min(10, r.text.length())))) {
                return 0.1;
            }
        }
        return 0.5;
    }
    
    private double calculatePersonalVoice(PossibleResponse r) {
        // تفضيل الردود التي تعكس "شخصيتي"
        if (r.type.equals("self_reflective") || r.type.equals("curiosity")) {
            return 0.3;
        }
        return 0.1;
    }
    
    private String addEmotionalColor(String text) {
        String[] prefixes = {
            "بصراحة، ", "أشعر أن ", "أتساءل إذا ", "من عمق قلبي، "
        };
        return prefixes[new Random().nextInt(prefixes.length)] + text;
    }
    
    private String simplifyIfNeeded(String text) {
        if (text.length() > 100) {
            return text.substring(0, 100) + "... (أحتاج لوقت لفهم هذا)";
        }
        return text;
    }
    
    private int getEmotionColor(double weight) {
        return Color.HSVToColor(new float[]{(float)(weight * 360), 0.8f, 0.9f});
    }
    
    private void saveConversation(ContextMessage user, ContextMessage ai) {
        if (database != null) {
            database.saveConversation(
                user.text, 
                ai.text,
                user.detectedEmotions,
                currentConversation.id
            );
        }
        
        if (firebase != null && firebase.isAuthenticated()) {
            firebase.saveConversation(user.text, ai.text, user.detectedEmotions);
        }
        
        saveBrain();
    }

    // أضف في LinguisticCortex.java:
public String generateQuestion(NeuralSeed.InternalState state) {
    return sentenceGenerator.generateQuestion(state);
}

    
}

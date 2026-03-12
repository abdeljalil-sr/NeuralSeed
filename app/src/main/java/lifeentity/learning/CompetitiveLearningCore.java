package com.lifeentity.learning;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.lifeentity.core.ConsciousnessCore;
import com.lifeentity.core.EmotionalState;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.MemoryDao;
import com.lifeentity.memory.SemanticEmbeddings;
import com.lifeentity.memory.VisualMemory;
import com.lifeentity.memory.VisualMemoryDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * نظام تعلم تنافسي متطور يتكامل مع بنية LifeEntity الحالية.
 * 
 * المميزات:
 * - خلايا مفاهيم تنافسية (Concept Cells) مع تثبيط جانبي (Lateral Inhibition)
 * - التعلم عبر الفائز يأخذ الكل (Winner-Take-All) مع تعلم الوصيف
 * - خريطة انتباه ديناميكية (Attention Map)
 * - تكيف تلقائي لعدد الخلايا بناءً على حداثة المدخلات
 * - ربط متعدد الوسائط (بصري-نصي-عاطفي) باستخدام MemoryDao الموجود
 * - يعمل بالكامل في خيط خلفي، آمن للتزامن
 */
public class CompetitiveLearningCore {
    private static final String TAG = "CompetitiveLearning";
    
    // ثوابت أساسية
    private static final int EMBEDDING_SIZE = 128;                    // يجب أن يطابق EmbeddingsEngine
    private static final float SIMILARITY_THRESHOLD = 0.65f;          // عتبة التشابه للمفاهيم
    private static final float LEARNING_RATE = 0.1f;                  // معدل التعلم الأساسي
    private static final float PLASTICITY_DECAY = 0.998f;             // تضاؤل اللدونة مع الوقت
    private static final float MIN_PLASTICITY = 0.05f;                // الحد الأدنى للدونة
    private static final float NOVELTY_THRESHOLD = 0.55f;             // عتبة الاستحداث (إنشاء خلية جديدة)
    private static final int MAX_CELLS = 100;                         // الحد الأقصى لعدد الخلايا
    private static final int INITIAL_CELLS = 15;                       // عدد الخلايا الأولية
    private static final int TOP_K_WINNERS = 3;                        // عدد الفائزين (للتعلم الجزئي)
    private static final float INHIBITION_STRENGTH = 0.2f;             // قوة التثبيط بين الخلايا
    private static final long CELL_LIFETIME_MS = 7L * 24 * 60 * 60 * 1000; // أسبوع
    
    // مكونات قاعدة البيانات
    private AppDatabase database;
    private MemoryDao memoryDao;
    private VisualMemoryDao visualMemoryDao;
    
    // خيط الخلفية
    private ExecutorService learningExecutor;
    
    // هياكل التعلم التنافسي
    private ConcurrentHashMap<String, ConceptCell> conceptCells;       // جميع الخلايا
    private ConcurrentHashMap<String, Float> attentionScores;          // خريطة الانتباه
    private ConcurrentHashMap<String, Long> lastActivationTime;        // آخر وقت نشاط
    private AtomicInteger totalCompetitions = new AtomicInteger(0);
    private AtomicInteger cellsCreated = new AtomicInteger(INITIAL_CELLS);
    
    // مرجع للوعي (اختياري)
    private ConsciousnessCore consciousness;
    
    // المستمع
    private LearningListener listener;
    
    // عداد عشوائي للمتجهات (في انتظار النماذج الحقيقية)
    private Random entropy = new Random();
    
    public interface LearningListener {
        void onConceptStrengthened(String conceptId, String conceptName, int occurrences, float confidence);
        void onNewAssociation(String concept1, String concept2, String relationType, float strength);
        void onWinnerSelected(String winnerId, float activation, List<String> runnersUp);
        void onNewCellCreated(String cellId, String triggerInput);
        void onAttentionShift(String oldFocus, String newFocus, float intensity);
        void onPrediction(String predictedConcept, float confidence);
    }
    
    /**
     * خلية مفهوم تنافسية - تمثل تمثيلاً داخلياً لمفهوم أو فئة.
     */
    private static class ConceptCell {
        final String id;                       // معرف فريد
        float[] prototype;                      // المتجه النموذجي (في الفضاء الكامن)
        String label;                            // تسمية نصية (مثل "قطة" أو "joy")
        String modality;                         // الطريقة المسيطرة (visual, textual, emotional)
        float plasticity;                        // قابلية التعلم (تتناقص مع الوقت)
        int winCount;                            // عدد مرات الفوز بالمنافسة
        float energy;                            // الطاقة التراكمية (نشاط)
        long lastWinTime;                        // آخر وقت فوز
        long birthTime;                           // وقت الإنشاء
        List<String> associatedTexts;             // النصوص المرتبطة (من المستخدم)
        
        ConceptCell(String id, float[] prototype, String modality) {
            this.id = id;
            this.prototype = prototype.clone();
            this.modality = modality;
            this.plasticity = 1.0f;
            this.winCount = 0;
            this.energy = 0.5f;
            this.lastWinTime = System.currentTimeMillis();
            this.birthTime = lastWinTime;
            this.associatedTexts = new ArrayList<>();
            this.label = id; // مؤقتاً
        }
        
        /**
         * حساب التنشيط (كلما كان أقرب للمدخل، زاد التنشيط)
         */
        float computeActivation(float[] input) {
            float sim = cosineSimilarity(prototype, input);
            // تطبيع مع مراعاة الطاقة والحداثة
            float timeFactor = (float) Math.exp(-(System.currentTimeMillis() - lastWinTime) / 3600000.0); // ساعة
            return sim * (0.7f + 0.3f * energy) * (0.5f + 0.5f * plasticity) * timeFactor;
        }
        
        /**
         * تعلم من المدخل (إذا كان الفائز أو وصيفاً)
         */
        void learn(float[] input, boolean isWinner, float learningRate) {
            float rate = isWinner ? learningRate * plasticity : learningRate * plasticity * 0.2f;
            for (int i = 0; i < EMBEDDING_SIZE; i++) {
                prototype[i] += rate * (input[i] - prototype[i]);
            }
            normalizeVector(prototype);
            
            plasticity *= PLASTICITY_DECAY;
            plasticity = Math.max(plasticity, MIN_PLASTICITY);
            
            if (isWinner) {
                winCount++;
                lastWinTime = System.currentTimeMillis();
                energy += 0.1f;
                energy = Math.min(1.0f, energy);
            }
        }
        
        /**
         * إضافة نص مرتبط
         */
        void associateText(String text) {
            if (!associatedTexts.contains(text)) {
                associatedTexts.add(text);
            }
        }
        
        float getConfidence() {
            return Math.min(1.0f, winCount / 20.0f) * plasticity;
        }
    }
    
    public CompetitiveLearningCore(Context context, AppDatabase db) {
        this.database = db;
        this.memoryDao = db.memoryDao();
        this.visualMemoryDao = db.visualMemoryDao();
        this.learningExecutor = Executors.newSingleThreadExecutor();
        this.conceptCells = new ConcurrentHashMap<>();
        this.attentionScores = new ConcurrentHashMap<>();
        this.lastActivationTime = new ConcurrentHashMap<>();
        
        // تهيئة الخلايا الأولية من قاعدة البيانات (إذا وجدت) أو إنشائها عشوائياً
        initializeCells();
        
        Log.i(TAG, "CompetitiveLearningCore initialized with " + conceptCells.size() + " cells.");
    }
    
    /**
     * تهيئة الخلايا من التضمينات المخزنة أو عشوائياً
     */
    private void initializeCells() {
        learningExecutor.execute(() -> {
            try {
                // محاولة استرجاع embeddings من قاعدة البيانات لاستخدامها كبذور
                List<SemanticEmbeddings.EmbeddingEntity> embeddings = memoryDao.getRecentEmbeddings(100);
                if (!embeddings.isEmpty()) {
                    int count = 0;
                    for (SemanticEmbeddings.EmbeddingEntity e : embeddings) {
                        if (e.vector != null && count < INITIAL_CELLS) {
                            String cellId = "cell_emb_" + e.concept;
                            ConceptCell cell = new ConceptCell(cellId, e.vector, "mixed");
                            cell.label = e.concept;
                            conceptCells.put(cellId, cell);
                            count++;
                        }
                    }
                    Log.i(TAG, "Initialized " + count + " cells from embeddings.");
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not load embeddings, using random cells.");
            }
            
            // إذا لم نحصل على العدد الكافي، ننشئ خلايا عشوائية
            if (conceptCells.size() < INITIAL_CELLS) {
                int needed = INITIAL_CELLS - conceptCells.size();
                for (int i = 0; i < needed; i++) {
                    float[] vec = generateRandomVector();
                    String cellId = "cell_init_" + i;
                    ConceptCell cell = new ConceptCell(cellId, vec, "generic");
                    conceptCells.put(cellId, cell);
                }
                Log.i(TAG, "Added " + needed + " random cells to reach initial count.");
            }
        });
    }
    
    // ==================== الواجهة الرئيسية ====================
    
    /**
     * معالجة مشهد بصري مع نص مصاحب
     * @param thumbnail الصورة المصغرة (قد تكون null)
     * @param visualConcept اسم المفهوم البصري (من SceneUnderstanding أو null)
     * @param spokenText النص المنطوق (من المستخدم)
     * @param affect الحالة العاطفية الحالية
     */
    public void processVisualWithText(Bitmap thumbnail, String visualConcept, String spokenText, float[] affect) {
        learningExecutor.execute(() -> {
            try {
                // تطبيع النصوص
                String visNorm = normalizeConcept(visualConcept);
                String txtNorm = normalizeConcept(spokenText);
                
                // توليد متجه الإدخال من المفهوم البصري (أو عشوائي إذا لم يوجد)
                float[] inputVector;
                if (visNorm != null && !visNorm.isEmpty()) {
                    inputVector = getVectorForConcept(visNorm);
                } else {
                    inputVector = generateRandomVector(); // أو يمكن استخدام لون الصورة لاحقاً
                }
                
                // دمج العاطفة مع المتجه (تأثير بسيط)
                if (affect != null && affect.length >= 5) {
                    for (int i = 0; i < Math.min(5, EMBEDDING_SIZE); i++) {
                        inputVector[i] += affect[i] * 0.1f;
                    }
                    normalizeVector(inputVector);
                }
                
                // 1. تنفيذ المنافسة
                CompetitionResult result = compete(inputVector, visNorm, txtNorm);
                
                // 2. تحديث خريطة الانتباه
                updateAttention(result.winner.id, result.activation);
                
                // 3. ربط النص إذا وجد
                if (txtNorm != null && !txtNorm.isEmpty()) {
                    associateTextWithWinner(result.winner, txtNorm, inputVector);
                }
                
                // 4. حفظ في الذاكرة البصرية إذا توفرت الصورة
                if (thumbnail != null) {
                    saveVisualMemory(thumbnail, result.winner, inputVector, affect);
                }
                
                // 5. تحديث إحصائيات التعلم
                totalCompetitions.incrementAndGet();
                
                // 6. إشعار المستمع
                notifyListeners(result, visNorm, txtNorm);
                
            } catch (Exception e) {
                Log.e(TAG, "Error in processVisualWithText: " + e.getMessage());
            }
        });
    }
    
    /**
     * المنافسة الرئيسية: تحديد الخلية الفائزة وتحديث الخلايا
     */
    private CompetitionResult compete(float[] input, String visConcept, String txtConcept) {
        // حساب التنشيط لكل الخلايا
        List<CellActivation> activations = new ArrayList<>();
        for (ConceptCell cell : conceptCells.values()) {
            float act = cell.computeActivation(input);
            activations.add(new CellActivation(cell, act));
        }
        
        // ترتيب تنازلي حسب التنشيط
        activations.sort((a, b) -> Float.compare(b.activation, a.activation));
        
        // تطبيق التثبيط الجانبي على المراتب العليا
        for (int i = 0; i < Math.min(activations.size(), TOP_K_WINNERS + 3); i++) {
            for (int j = i + 1; j < Math.min(activations.size(), TOP_K_WINNERS + 3); j++) {
                float inhibition = INHIBITION_STRENGTH * (1.0f - (float)j / (TOP_K_WINNERS + 3));
                activations.get(j).cell.energy *= (1 - inhibition);
            }
        }
        
        // إعادة حساب التنشيط بعد التثبيط (بسيط: نأخذ التنشيط الأصلي مضروباً بالطاقة الجديدة)
        for (CellActivation ca : activations) {
            ca.finalActivation = ca.activation * ca.cell.energy;
        }
        activations.sort((a, b) -> Float.compare(b.finalActivation, a.finalActivation));
        
        // الفائز والوصيف
        ConceptCell winner = activations.get(0).cell;
        float winnerAct = activations.get(0).finalActivation;
        
        List<String> runnersUp = new ArrayList<>();
        for (int i = 1; i < Math.min(activations.size(), 4); i++) {
            runnersUp.add(activations.get(i).cell.id);
        }
        
        // تعلم الفائز والوصيف
        winner.learn(input, true, LEARNING_RATE);
        for (int i = 1; i < Math.min(activations.size(), TOP_K_WINNERS); i++) {
            activations.get(i).cell.learn(input, false, LEARNING_RATE * 0.3f);
        }
        
        // إذا كان التنشيط منخفضاً جداً والعدد لم يصل للحد الأقصى، ننشئ خلية جديدة
        if (winnerAct < NOVELTY_THRESHOLD && conceptCells.size() < MAX_CELLS) {
            createNewCell(input, visConcept != null ? visConcept : "novel", winner);
        }
        
        return new CompetitionResult(winner, winnerAct, runnersUp);
    }
    
    /**
     * إنشاء خلية جديدة من مدخل جديد (استحداث)
     */
    private void createNewCell(float[] input, String trigger, ConceptCell nearest) {
        String cellId = "cell_" + cellsCreated.incrementAndGet() + "_" + trigger;
        // مزج المدخل مع أقرب خلية لإنشاء تمثيل جديد
        float[] newProto = new float[EMBEDDING_SIZE];
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            newProto[i] = 0.6f * input[i] + 0.4f * nearest.prototype[i];
        }
        normalizeVector(newProto);
        
        ConceptCell newCell = new ConceptCell(cellId, newProto, "learned");
        newCell.plasticity = 1.0f; // لدونة عالية للخلية الجديدة
        conceptCells.put(cellId, newCell);
        
        if (listener != null) {
            listener.onNewCellCreated(cellId, trigger);
        }
        Log.i(TAG, "Created new cell: " + cellId);
    }
    
    /**
     * ربط نص بالخلية الفائزة وتحديث التضمينات
     */
    private void associateTextWithWinner(ConceptCell winner, String text, float[] inputVector) {
        winner.associateText(text);
        
        // تحديث أو إنشاء embedding للنص في قاعدة البيانات
        float[] textVec = getVectorForConcept(text);
        // دمج متجه النص مع متجه الفائز (لتعزيز الارتباط)
        float[] fused = new float[EMBEDDING_SIZE];
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            fused[i] = (inputVector[i] + textVec[i]) / 2;
        }
        normalizeVector(fused);
        
        // حفظ في قاعدة البيانات
        SemanticEmbeddings.EmbeddingEntity entity = new SemanticEmbeddings.EmbeddingEntity(text, fused);
        memoryDao.saveEmbedding(entity);
        
        // أيضاً حفظ العلاقة في الذاكرة العرضية (اختياري)
        EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
        event.timestamp = System.currentTimeMillis();
        event.narrative = "ربط بصري-نصي: " + winner.label + " مع " + text;
        event.emotionalState = "neutral";
        event.emotionalIntensity = 0.5f;
        event.location = "learning";
        memoryDao.insertEvent(event);
        
        if (listener != null) {
            listener.onNewAssociation(winner.id, text, "visual-text", 0.8f);
        }
    }
    
    /**
     * تحديث خريطة الانتباه (تضاؤل القديم وإضافة الجديد)
     */
    private void updateAttention(String cellId, float activation) {
        // تضاؤل كل القيم
        for (String key : attentionScores.keySet()) {
            attentionScores.put(key, attentionScores.get(key) * 0.95f);
        }
        // إضافة/تحديث للفائز
        attentionScores.merge(cellId, activation, Math::max);
        
        // تنظيف القيم المنخفضة جداً
        attentionScores.entrySet().removeIf(e -> e.getValue() < 0.01f);
    }
    
    private void notifyListeners(CompetitionResult result, String visConcept, String txtConcept) {
        if (listener == null) return;
        
        listener.onWinnerSelected(result.winner.id, result.activation, result.runnersUp);
        
        String oldFocus = getMostAttended();
        if (oldFocus != null && !oldFocus.equals(result.winner.id)) {
            float intensity = result.activation - attentionScores.getOrDefault(oldFocus, 0f);
            listener.onAttentionShift(oldFocus, result.winner.id, intensity);
        }
        
        float confidence = result.winner.getConfidence();
        listener.onConceptStrengthened(result.winner.id, result.winner.label, result.winner.winCount, confidence);
    }
    
    // ==================== أدوات مساعدة ====================
    
    /**
     * الحصول على متجه لمفهوم (نصي) – إما من قاعدة البيانات أو توليد متجه ثابت
     */
    private float[] getVectorForConcept(String concept) {
        if (concept == null || concept.isEmpty()) return generateRandomVector();
        
        // البحث في قاعدة البيانات أولاً
        SemanticEmbeddings.EmbeddingEntity emb = memoryDao.getEmbedding(concept);
        if (emb != null && emb.vector != null) {
            return emb.vector;
        }
        
        // توليد متجه ثابت يعتمد على هاش الكلمة (لتكرارية معقولة)
        Random r = new Random(concept.hashCode());
        float[] vec = new float[EMBEDDING_SIZE];
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            vec[i] = r.nextFloat() * 2 - 1;
        }
        normalizeVector(vec);
        return vec;
    }
    
    private float[] generateRandomVector() {
        float[] vec = new float[EMBEDDING_SIZE];
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            vec[i] = entropy.nextFloat() * 2 - 1;
        }
        normalizeVector(vec);
        return vec;
    }
    
    private static void normalizeVector(float[] v) {
        float sum = 0;
        for (float f : v) sum += f * f;
        float norm = (float) Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
    }
    
    private static float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, na = 0, nb = 0;
        for (int i = 0; i < EMBEDDING_SIZE; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (float) (Math.sqrt(na) * Math.sqrt(nb) + 1e-8f);
    }
    
    private String normalizeConcept(String c) {
        return c == null ? null : c.trim().toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .replaceAll("[\\u064B-\\u065F]", "");
    }
    
    private void saveVisualMemory(Bitmap bmp, ConceptCell cell, float[] vector, float[] affect) {
        if (database == null || bmp == null) return;
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 80, baos);
            VisualMemory vm = new VisualMemory();
            vm.timestamp = System.currentTimeMillis();
            vm.latentVector = vector.clone();
            vm.thumbnail = baos.toByteArray();
            vm.concept = cell.label;
            vm.affectAtEncoding = affect != null ? affect.clone() : new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};
            visualMemoryDao.insert(vm);
        } catch (Exception e) {
            Log.e(TAG, "Error saving visual memory: " + e.getMessage());
        }
    }
    
    // ==================== استعلامات ====================
    
    public String getMostAttended() {
        String best = null;
        float max = 0;
        for (Map.Entry<String, Float> e : attentionScores.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }
    
    public List<String> getTopConcepts(int k) {
        List<ConceptCell> cells = new ArrayList<>(conceptCells.values());
        cells.sort((a, b) -> Float.compare(b.energy, a.energy));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(k, cells.size()); i++) {
            result.add(cells.get(i).id + " (" + cells.get(i).label + ")");
        }
        return result;
    }
    
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCells", conceptCells.size());
        stats.put("totalCompetitions", totalCompetitions.get());
        stats.put("cellsCreated", cellsCreated.get());
        stats.put("mostAttended", getMostAttended());
        stats.put("averageEnergy", conceptCells.values().stream().mapToDouble(c -> c.energy).average().orElse(0));
        return stats;
    }
    
    // ==================== إعدادات ====================
    
    public void setListener(LearningListener l) { this.listener = l; }
    public void setConsciousnessCore(ConsciousnessCore c) { this.consciousness = c; }
    
    public void shutdown() {
        learningExecutor.shutdown();
        Log.i(TAG, "CompetitiveLearningCore shut down.");
    }
    
    // ==================== فئات داخلية ====================
    
    private static class CellActivation {
        ConceptCell cell;
        float activation;
        float finalActivation;
        CellActivation(ConceptCell cell, float activation) {
            this.cell = cell;
            this.activation = activation;
            this.finalActivation = activation;
        }
    }
    
    private static class CompetitionResult {
        ConceptCell winner;
        float activation;
        List<String> runnersUp;
        CompetitionResult(ConceptCell w, float a, List<String> r) {
            winner = w; activation = a; runnersUp = r;
        }
    }
}

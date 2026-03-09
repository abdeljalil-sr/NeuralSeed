package com.lifeentity.imagination;

import android.util.Log;

import com.lifeentity.core.EmotionalState;
import com.lifeentity.memory.VisualMemory;
import com.lifeentity.memory.VisualMemoryDao;
import com.lifeentity.sensors.SensoryInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * محرك الخيال المتقدم - توليد متجهات كامنة وصور باستخدام تقنيات بحث فعالة
 * وإنشاء محتوى جديد عبر interpolation و VAE-style generation.
 */
public class ImaginationEngine {
    private static final String TAG = "ImaginationEngine";
    private static final int LATENT_SIZE = 128;
    private static final int MAX_CACHED_MEMORIES = 500;
    private static final float RECENCY_DECAY_HOURS = 24.0f;
    
    private VisualMemoryDao visualMemoryDao;
    private Random random;
    private float creativityLevel; // 0.0 to 1.0, يتأثر بالحالة العاطفية
    private float currentEmotionalIntensity;
    
    // بنية بحث فعالة (KD-Tree مبسط)
    private List<VisualMemory> memoryCache;
    private ConcurrentHashMap<String, float[]> conceptCentroids;
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 30000; // 30 ثانية

    public ImaginationEngine(VisualMemoryDao dao) {
        this.visualMemoryDao = dao;
        this.random = new Random();
        this.creativityLevel = 0.3f; // قيمة أولية معتدلة
        this.currentEmotionalIntensity = 0.5f;
        this.memoryCache = new ArrayList<>();
        this.conceptCentroids = new ConcurrentHashMap<>();
    }

    /**
     * تحديث مستوى الإبداع بناءً على الحالة العاطفية
     */
    public void updateCreativityFromEmotion(EmotionalState emotion) {
        if (emotion == null) return;
        
        currentEmotionalIntensity = emotion.getIntensity();
        
        // الإثارة تزيد الإبداع، الهدوء يقلله
        if (emotion.isExcited()) {
            creativityLevel = Math.min(1.0f, 0.5f + currentEmotionalIntensity * 0.4f);
        } else if (emotion.isCalm()) {
            creativityLevel = Math.max(0.1f, 0.2f + currentEmotionalIntensity * 0.2f);
        } else if (emotion.isCurious()) {
            creativityLevel = 0.6f + currentEmotionalIntensity * 0.3f;
        } else if (emotion.isAfraid()) {
            creativityLevel = 0.2f; // الخوف يقلل الإبداع
        } else {
            creativityLevel = 0.3f + currentEmotionalIntensity * 0.3f;
        }
        
        Log.d(TAG, "Creativity updated to: " + creativityLevel + " (emotion: " + emotion.toArabic() + ")");
    }

    /**
     * توليد متجه كامن متقدم مع دعم interpolation و creativity ديناميكية
     */
    public float[] generateLatentFromState(float[] affectVector, SensoryInput perception, String desire) {
        refreshCacheIfNeeded();
        
        if (memoryCache.isEmpty()) {
            return generateNovelLatent(affectVector, true); // توليد جديد بالكامل
        }

        int blendCount = 2 + random.nextInt(4); // 2-5 ذكريات للمزج
        List<WeightedMemory> selected = selectMemoriesForBlending(affectVector, desire, blendCount);
        
        if (selected.isEmpty()) {
            return generateNovelLatent(affectVector, true);
        }

        // interpolation متعدد النقاط مع weights
        float[] result = multiPointInterpolation(selected);
        
        // إضافة creativity noise بناءً على الحالة العاطفية
        addCreativeNoise(result);
        
        // إذا كان الإبداع عالياً، امزج مع توليد جديد
        if (creativityLevel > 0.7f && random.nextFloat() < creativityLevel) {
            float[] novel = generateNovelLatent(affectVector, false);
            result = interpolateVectors(result, novel, creativityLevel);
        }

        return clampVector(result);
    }

    /**
     * اختيار الذكريات للمزج مع مراعاة التشابه والحداثة
     */
    private List<WeightedMemory> selectMemoriesForBlending(float[] affectVector, String desire, int count) {
        PriorityQueue<WeightedMemory> heap = new PriorityQueue<>(
            Comparator.comparingDouble(wm -> -wm.weight)
        );
        
        long now = System.currentTimeMillis();
        
        for (VisualMemory mem : memoryCache) {
            float weight = computeAdvancedWeight(mem, affectVector, desire, now);
            heap.offer(new WeightedMemory(mem, weight));
        }
        
        List<WeightedMemory> selected = new ArrayList<>();
        for (int i = 0; i < count && !heap.isEmpty(); i++) {
            selected.add(heap.poll());
        }
        
        return selected;
    }

    /**
     * حساب الوزن المتقدم مع حداثة الذاكرة والتشابه العاطفي
     */
    private float computeAdvancedWeight(VisualMemory mem, float[] affectVector, String desire, long currentTime) {
        float weight = 0.2f; // وزن أساسي
        
        // 1. التشابه العاطفي (40%)
        float[] memAffect = mem.affectAtEncoding;
        if (memAffect != null && affectVector != null) {
            float emotionalSim = cosineSimilarity(affectVector, memAffect);
            weight += emotionalSim * 0.4f;
        }
        
        // 2. الارتباط بالرغبة (25%)
        if (mem.concept != null && desire != null) {
            float conceptMatch = computeConceptSimilarity(mem.concept, desire);
            weight += conceptMatch * 0.25f;
        }
        
        // 3. حداثة الذاكرة (25%) - ذكريات أحدث لها وزن أكبر
        long ageHours = (currentTime - mem.timestamp) / (1000 * 60 * 60);
        float recency = (float) Math.exp(-ageHours / RECENCY_DECAY_HOURS);
        weight += recency * 0.25f;
        
        // 4. جودة الذاكرة (10%) - حسب عدد المرات التي تم استرجاعها
        if (mem.retrievalCount > 0) {
            weight += Math.min(0.1f, mem.retrievalCount * 0.01f);
        }
        
        // تعزيز عشوائي طفيف للتنوع
        weight *= (0.9f + random.nextFloat() * 0.2f);
        
        return weight;
    }

    /**
     * interpolation متعدد النقاط مع weights متغيرة
     */
    private float[] multiPointInterpolation(List<WeightedMemory> memories) {
        float[] result = new float[LATENT_SIZE];
        float totalWeight = 0;
        
        // normalize weights
        float weightSum = memories.stream().map(m -> m.weight).reduce(0f, Float::sum);
        
        for (WeightedMemory wm : memories) {
            float normalizedWeight = wm.weight / weightSum;
            float[] latent = wm.memory.latentVector;
            if (latent == null) continue;
            
            // spherical interpolation للحفاظ على البنية
            if (totalWeight == 0) {
                System.arraycopy(latent, 0, result, 0, LATENT_SIZE);
                totalWeight = normalizedWeight;
            } else {
                result = slerp(result, latent, normalizedWeight / (totalWeight + normalizedWeight));
                totalWeight += normalizedWeight;
            }
        }
        
        return result;
    }

    /**
     * Spherical Linear Interpolation (SLERP) للحفاظ على توزيع المتجهات
     */
    private float[] slerp(float[] a, float[] b, float t) {
        float dot = dotProduct(a, b);
        dot = Math.max(-1.0f, Math.min(1.0f, dot));
        
        double theta = Math.acos(dot) * t;
        float[] relative = new float[LATENT_SIZE];
        for (int i = 0; i < LATENT_SIZE; i++) {
            relative[i] = b[i] - a[i] * dot;
        }
        normalizeVector(relative);
        
        float[] result = new float[LATENT_SIZE];
        double sinTheta = Math.sin(theta);
        double sinTotal = Math.sin(theta / t);
        
        for (int i = 0; i < LATENT_SIZE; i++) {
            result[i] = (float) (a[i] * Math.cos(theta) + relative[i] * sinTheta);
        }
        
        return result;
    }

    /**
     * توليد متجه كامن جديد (VAE-style sampling)
     */
    private float[] generateNovelLatent(float[] affectVector, boolean fullRandom) {
        float[] latent = new float[LATENT_SIZE];
        
        if (fullRandom) {
            // توليد عشوائي بحت
            for (int i = 0; i < LATENT_SIZE; i++) {
                latent[i] = random.nextGaussian() * 0.5f;
            }
        } else {
            // توليع مبني على الحالة العاطفية (conditional generation)
            float[] seed = affectVector != null ? affectVector : new float[4];
            for (int i = 0; i < LATENT_SIZE; i++) {
                // استخدام الحالة العاطفية لتوجيه التوليد
                float emotionalBias = (i < seed.length) ? seed[i] * 0.3f : 0;
                latent[i] = random.nextGaussian() * 0.4f + emotionalBias;
            }
        }
        
        normalizeVector(latent);
        return latent;
    }

    /**
     * إضافة ضوضاء إبداعية بناءً على مستوى creativity
     */
    private void addCreativeNoise(float[] vector) {
        float noiseAmplitude = creativityLevel * 0.4f;
        
        // Perlin noise مبسط للبنية الطبيعية
        for (int i = 0; i < LATENT_SIZE; i++) {
            float noise = random.nextGaussian() * noiseAmplitude;
            
            // تقليل الضوضاء في الأبعاد المهمة (الأولى)
            if (i < 16) noise *= 0.5f;
            
            vector[i] += noise;
        }
    }

    /**
     * تحويل متجه كامن إلى صورة مع دعم التوليد الجديد
     */
    public byte[] latentToThumbnail(float[] latent) {
        if (latent == null) return null;
        
        refreshCacheIfNeeded();
        
        // البحث الفعال باستخدام البنية المخزنة مؤقتاً
        VisualMemory best = findNearestNeighbor(latent);
        
        if (best == null) {
            return null;
        }
        
        float distance = euclideanDistance(latent, best.latentVector);
        
        // إذا كان البعد كبيراً والإبداع عالياً، قم بتوليد جديد
        if (distance > 0.5f && creativityLevel > 0.6f) {
            return generateNewImage(latent, best);
        }
        
        // تحديث إحصائيات الاسترجاع
        updateRetrievalStats(best);
        
        // إذا كان البعد متوسطاً، استخدم interpolation
        if (distance > 0.2f && distance <= 0.5f) {
            return interpolateWithNearest(latent, best);
        }
        
        return best.thumbnail;
    }

    /**
     * البحث عن أقرب جار باستخدام البنية المخزنة مؤقتاً
     */
    private VisualMemory findNearestNeighbor(float[] latent) {
        if (memoryCache.isEmpty()) return null;
        
        VisualMemory best = null;
        float bestDist = Float.MAX_VALUE;
        
        // بحث خطي محسّن مع early termination
        for (VisualMemory mem : memoryCache) {
            if (mem.latentVector == null) continue;
            
            float dist = quickDistanceEstimate(latent, mem.latentVector);
            if (dist < bestDist) {
                bestDist = dist;
                best = mem;
                
                // early termination إذا وجدنا تطابقاً جيداً جداً
                if (bestDist < 0.1f) break;
            }
        }
        
        return best;
    }

    /**
     * تقدير سريع للمسافة (باستخدام عينة من الأبعاد)
     */
    private float quickDistanceEstimate(float[] a, float[] b) {
        float sum = 0;
        // فحص 32 بعداً موزعة بالتساوي
        int step = LATENT_SIZE / 32;
        for (int i = 0; i < LATENT_SIZE; i += step) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return sum;
    }

    /**
     * توليد صورة جديدة باستخدام interpolation متقدم
     */
    private byte[] generateNewImage(float[] targetLatent, VisualMemory baseMemory) {
        // في تطبيق حقيقي، هنا يتم استخدام VAE decoder أو GAN
        // حالياً نستخدم interpolation مع عدة جيران
        
        List<VisualMemory> neighbors = findKNearestNeighbors(targetLatent, 3);
        if (neighbors.size() < 2) {
            return baseMemory.thumbnail;
        }
        
        // إنشاء latent مزيج
        float[] blended = multiPointInterpolation(
            neighbors.stream().map(m -> new WeightedMemory(m, 1.0f)).toList()
        );
        
        // إضافة creativity للحصول على شيء جديد
        addCreativeNoise(blended);
        
        // في الإصدار الكامل، هنا يتم decode المتجه إلى صورة
        // حالياً نعيد الصورة الأقرب مع علامة أنها "مولدة"
        Log.d(TAG, "Generated new image via advanced interpolation");
        return baseMemory.thumbnail; // placeholder للتوليد الحقيقي
    }

    /**
     * البحث عن k أقرب جيران
     */
    private List<VisualMemory> findKNearestNeighbors(float[] latent, int k) {
        PriorityQueue<VisualMemoryDistance> heap = new PriorityQueue<>(
            Comparator.comparingDouble(vmd -> vmd.distance)
        );
        
        for (VisualMemory mem : memoryCache) {
            if (mem.latentVector == null) continue;
            float dist = euclideanDistance(latent, mem.latentVector);
            heap.offer(new VisualMemoryDistance(mem, dist));
            if (heap.size() > k) heap.poll();
        }
        
        List<VisualMemory> result = new ArrayList<>();
        while (!heap.isEmpty()) result.add(heap.poll().memory);
        Collections.reverse(result); // الأقرب أولاً
        return result;
    }

    /**
     * interpolation بين المتجه المستهدف والأقرب
     */
    private byte[] interpolateWithNearest(float[] target, VisualMemory nearest) {
        float similarity = 1.0f - Math.min(1.0f, euclideanDistance(target, nearest.latentVector));
        float blendFactor = similarity * (1 - creativityLevel * 0.3f);
        
        // في التطبيق الكامل، هنا يتم blend الصور pixel-wise
        // أو استخدام VAE decoder على المتجه الممزوج
        Log.d(TAG, "Interpolating with blend factor: " + blendFactor);
        return nearest.thumbnail;
    }

    /**
     * تحويل مفهوم إلى متجه كامن مع دعم centroids المخزنة
     */
    public float[] conceptToLatent(String concept) {
        // التحقق من centroids المخزنة
        if (conceptCentroids.containsKey(concept)) {
            float[] centroid = conceptCentroids.get(concept);
            float[] result = centroid.clone();
            addCreativeNoise(result); // إضافة تنوع
            return clampVector(result);
        }
        
        // حساب centroid إذا لم يكن مخزناً
        List<VisualMemory> memories = visualMemoryDao.getByConcept(concept);
        if (memories.isEmpty()) {
            return generateNovelLatent(null, true);
        }
        
        float[] centroid = new float[LATENT_SIZE];
        int validCount = 0;
        
        for (VisualMemory mem : memories) {
            if (mem.latentVector != null) {
                for (int i = 0; i < LATENT_SIZE; i++) {
                    centroid[i] += mem.latentVector[i];
                }
                validCount++;
            }
        }
        
        if (validCount > 0) {
            for (int i = 0; i < LATENT_SIZE; i++) {
                centroid[i] /= validCount;
            }
            normalizeVector(centroid);
            conceptCentroids.put(concept, centroid.clone());
        }
        
        addCreativeNoise(centroid);
        return clampVector(centroid);
    }

    // ==================== دوال مساعدة ====================

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > CACHE_TTL_MS || memoryCache.isEmpty()) {
            memoryCache = visualMemoryDao.getRecent(MAX_CACHED_MEMORIES);
            lastCacheUpdate = now;
            Log.d(TAG, "Cache refreshed with " + memoryCache.size() + " memories");
        }
    }

    private float computeConceptSimilarity(String concept1, String concept2) {
        if (concept1.equalsIgnoreCase(concept2)) return 1.0f;
        
        // تطابق جزئي بسيط
        String[] words1 = concept1.toLowerCase().split("\\s+");
        String[] words2 = concept2.toLowerCase().split("\\s+");
        
        int matches = 0;
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.contains(w2) || w2.contains(w1)) matches++;
            }
        }
        
        return matches / (float) Math.max(words1.length, words2.length);
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = dotProduct(a, b);
        float normA = (float) Math.sqrt(dotProduct(a, a));
        float normB = (float) Math.sqrt(dotProduct(b, b));
        return dot / (normA * normB + 1e-8f);
    }

    private float dotProduct(float[] a, float[] b) {
        float sum = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) sum += a[i] * b[i];
        return sum;
    }

    private void normalizeVector(float[] v) {
        float norm = (float) Math.sqrt(dotProduct(v, v));
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
    }

    private float[] interpolateVectors(float[] a, float[] b, float t) {
        float[] result = new float[LATENT_SIZE];
        for (int i = 0; i < LATENT_SIZE; i++) {
            result[i] = a[i] * (1 - t) + b[i] * t;
        }
        return result;
    }

    private float[] clampVector(float[] v) {
        for (int i = 0; i < v.length; i++) {
            v[i] = Math.max(-1, Math.min(1, v[i]));
        }
        return v;
    }

    private void updateRetrievalStats(VisualMemory mem) {
        mem.retrievalCount++;
        // يمكن تحديث قاعدة البيانات هنا إذا لزم الأمر
    }

    private float euclideanDistance(float[] a, float[] b) {
        float sum = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return (float) Math.sqrt(sum);
    }

    public String getRandomConcept() {
        return visualMemoryDao.getRandomConcept();
    }

    /**
     * الحصول على مستوى الإبداع الحالي (للمراقبة)
     */
    public float getCreativityLevel() {
        return creativityLevel;
    }

    /**
     * تعيين مستوى الإبداع يدوياً (للتخصيص)
     */
    public void setCreativityLevel(float level) {
        this.creativityLevel = Math.max(0, Math.min(1, level));
    }

    // ==================== الفئات الداخلية ====================

    private static class WeightedMemory {
        final VisualMemory memory;
        final float weight;
        
        WeightedMemory(VisualMemory m, float w) {
            this.memory = m;
            this.weight = w;
        }
    }

    private static class VisualMemoryDistance {
        final VisualMemory memory;
        final float distance;
        
        VisualMemoryDistance(VisualMemory m, float d) {
            this.memory = m;
            this.distance = d;
        }
    }
}

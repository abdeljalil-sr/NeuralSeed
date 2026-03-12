package com.lifeentity.imagination;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
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
    
    private List<VisualMemory> memoryCache;
    private ConcurrentHashMap<String, float[]> conceptCentroids;
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 30000;

    public ImaginationEngine(VisualMemoryDao dao) {
        this.visualMemoryDao = dao;
        this.random = new Random();
        this.creativityLevel = 0.3f;
        this.currentEmotionalIntensity = 0.5f;
        this.memoryCache = new ArrayList<>();
        this.conceptCentroids = new ConcurrentHashMap<>();
    }

    public void updateCreativityFromEmotion(EmotionalState emotion) {
        if (emotion == null) return;
        currentEmotionalIntensity = emotion.getIntensity();
        if (emotion.isExcited()) {
            creativityLevel = Math.min(1.0f, 0.5f + currentEmotionalIntensity * 0.4f);
        } else if (emotion.isCalm()) {
            creativityLevel = Math.max(0.1f, 0.2f + currentEmotionalIntensity * 0.2f);
        } else if (emotion.isCurious()) {
            creativityLevel = 0.6f + currentEmotionalIntensity * 0.3f;
        } else if (emotion.isAfraid()) {
            creativityLevel = 0.2f;
        } else {
            creativityLevel = 0.3f + currentEmotionalIntensity * 0.3f;
        }
        Log.d(TAG, "Creativity updated to: " + creativityLevel);
    }

    public float[] generateLatentFromState(float[] affectVector, SensoryInput perception, String desire) {
        refreshCacheIfNeeded();
        if (memoryCache.isEmpty()) {
            return generateNovelLatent(affectVector, true);
        }
        int blendCount = 2 + random.nextInt(4);
        List<WeightedMemory> selected = selectMemoriesForBlending(affectVector, desire, blendCount);
        if (selected.isEmpty()) {
            return generateNovelLatent(affectVector, true);
        }
        float[] result = multiPointInterpolation(selected);
        addCreativeNoise(result);
        if (creativityLevel > 0.7f && random.nextFloat() < creativityLevel) {
            float[] novel = generateNovelLatent(affectVector, false);
            result = interpolateVectors(result, novel, creativityLevel);
        }
        return clampVector(result);
    }

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

    private float computeAdvancedWeight(VisualMemory mem, float[] affectVector, String desire, long currentTime) {
        float weight = 0.2f;
        float[] memAffect = mem.affectAtEncoding;
        if (memAffect != null && affectVector != null) {
            float emotionalSim = cosineSimilarity(affectVector, memAffect);
            weight += emotionalSim * 0.4f;
        }
        if (mem.concept != null && desire != null) {
            float conceptMatch = computeConceptSimilarity(mem.concept, desire);
            weight += conceptMatch * 0.25f;
        }
        long ageHours = (currentTime - mem.timestamp) / (1000 * 60 * 60);
        float recency = (float) Math.exp(-ageHours / RECENCY_DECAY_HOURS);
        weight += recency * 0.25f;
        if (mem.retrievalCount > 0) {
            weight += Math.min(0.1f, mem.retrievalCount * 0.01f);
        }
        weight *= (0.9f + random.nextFloat() * 0.2f);
        return weight;
    }

    private float[] multiPointInterpolation(List<WeightedMemory> memories) {
        float[] result = new float[LATENT_SIZE];
        float totalWeight = 0;
        float weightSum = 0;
        for (WeightedMemory wm : memories) weightSum += wm.weight;
        for (WeightedMemory wm : memories) {
            float normalizedWeight = wm.weight / weightSum;
            float[] latent = wm.memory.latentVector;
            if (latent == null) continue;
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
        for (int i = 0; i < LATENT_SIZE; i++) {
            result[i] = (float) (a[i] * Math.cos(theta) + relative[i] * sinTheta);
        }
        return result;
    }

    private float[] generateNovelLatent(float[] affectVector, boolean fullRandom) {
        float[] latent = new float[LATENT_SIZE];
        if (fullRandom) {
            for (int i = 0; i < LATENT_SIZE; i++) {
                latent[i] = (float) random.nextGaussian() * 0.5f;
            }
        } else {
            float[] seed = affectVector != null ? affectVector : new float[4];
            for (int i = 0; i < LATENT_SIZE; i++) {
                float emotionalBias = (i < seed.length) ? seed[i] * 0.3f : 0;
                latent[i] = (float) random.nextGaussian() * 0.4f + emotionalBias;
            }
        }
        normalizeVector(latent);
        return latent;
    }

    private void addCreativeNoise(float[] vector) {
        float noiseAmplitude = creativityLevel * 0.4f;
        for (int i = 0; i < LATENT_SIZE; i++) {
            float noise = (float) random.nextGaussian() * noiseAmplitude;
            if (i < 16) noise *= 0.5f;
            vector[i] += noise;
        }
    }

    /**
     * تحويل متجه كامن إلى صورة (إصدار قديم للتوافق مع الكود الحالي)
     */
    public byte[] latentToThumbnail(float[] latent) {
        if (latent == null) return null;
        refreshCacheIfNeeded();
        VisualMemory best = findNearestNeighbor(latent);
        if (best == null) return null;
        if (best.retrievalCount >= 0) best.retrievalCount++;
        return best.thumbnail;
    }

    /**
     * الحصول على متجه كامن لمفهوم معين
     */
    public float[] conceptToLatent(String concept) {
        if (concept == null) return generateNovelLatent(null, true);
        if (conceptCentroids.containsKey(concept)) {
            float[] centroid = conceptCentroids.get(concept);
            float[] result = centroid.clone();
            addCreativeNoise(result);
            return clampVector(result);
        }
        List<VisualMemory> memories = visualMemoryDao.getByConcept(concept);
        if (memories.isEmpty()) {
            return generateNovelLatent(null, true);
        }
        float[] centroid = new float[LATENT_SIZE];
        int validCount = 0;
        for (VisualMemory mem : memories) {
            if (mem.latentVector != null) {
                for (int i = 0; i < LATENT_SIZE; i++) centroid[i] += mem.latentVector[i];
                validCount++;
            }
        }
        if (validCount > 0) {
            for (int i = 0; i < LATENT_SIZE; i++) centroid[i] /= validCount;
            normalizeVector(centroid);
            conceptCentroids.put(concept, centroid.clone());
        }
        addCreativeNoise(centroid);
        return clampVector(centroid);
    }

    /**
     * توليد متجه كامن لمفهوم مع الحالة العاطفية
     */
    public float[] generateLatentForConcept(String concept, EmotionalState emotion) {
        float[] latent = conceptToLatent(concept);
        if (emotion != null) {
            float[] affect = emotion.toAffectVector();
            for (int i = 0; i < Math.min(5, LATENT_SIZE); i++) {
                latent[i] += affect[i] * 0.2f;
            }
            normalizeVector(latent);
        }
        return latent;
    }

    /**
     * توليد صورة من متجه كامن (محاكاة - ترجع صورة عشوائية)
     */
    public Bitmap generateImageFromLatent(float[] latent) {
        // هذه دالة محاكاة إلى أن يتم دمج VAE decoder حقيقي
        byte[] thumb = latentToThumbnail(latent);
        if (thumb != null) {
            return BitmapFactory.decodeByteArray(thumb, 0, thumb.length);
        }
        // إنشاء صورة عشوائية
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                int r = (int) (random.nextFloat() * 255);
                int g = (int) (random.nextFloat() * 255);
                int b = (int) (random.nextFloat() * 255);
                bitmap.setPixel(x, y, Color.rgb(r, g, b));
            }
        }
        return bitmap;
    }

    /**
     * إنشاء مسار (Path) من نقاط (لـ SharedCanvas)
     */
    public Path createPath(List<PointF> points, boolean close) {
        if (points == null || points.isEmpty()) return null;
        Path path = new Path();
        path.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).x, points.get(i).y);
        }
        if (close) path.close();
        return path;
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

    private VisualMemory findNearestNeighbor(float[] latent) {
        if (memoryCache.isEmpty()) return null;
        VisualMemory best = null;
        float bestDist = Float.MAX_VALUE;
        for (VisualMemory mem : memoryCache) {
            if (mem.latentVector == null) continue;
            float dist = quickDistanceEstimate(latent, mem.latentVector);
            if (dist < bestDist) {
                bestDist = dist;
                best = mem;
                if (bestDist < 0.1f) break;
            }
        }
        return best;
    }

    private float quickDistanceEstimate(float[] a, float[] b) {
        float sum = 0;
        int step = LATENT_SIZE / 32;
        for (int i = 0; i < LATENT_SIZE; i += step) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return sum;
    }

    private float computeConceptSimilarity(String concept1, String concept2) {
        if (concept1.equalsIgnoreCase(concept2)) return 1.0f;
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

    public String getRandomConcept() {
        return visualMemoryDao.getRandomConcept();
    }

    public float getCreativityLevel() {
        return creativityLevel;
    }

    public void setCreativityLevel(float level) {
        this.creativityLevel = Math.max(0, Math.min(1, level));
    }

    // ==================== الفئات الداخلية ====================

    public static class PointF {
        public float x, y;
        public PointF(float x, float y) { this.x = x; this.y = y; }
    }

    private static class WeightedMemory {
        final VisualMemory memory;
        final float weight;
        WeightedMemory(VisualMemory m, float w) { this.memory = m; this.weight = w; }
    }

    private static class VisualMemoryDistance {
        final VisualMemory memory;
        final float distance;
        VisualMemoryDistance(VisualMemory m, float d) { this.memory = m; this.distance = d; }
    }
}

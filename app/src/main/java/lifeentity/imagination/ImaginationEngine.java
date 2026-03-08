package com.lifeentity.imagination;

import com.lifeentity.memory.VisualMemory;
import com.lifeentity.memory.VisualMemoryDao;
import com.lifeentity.sensors.SensoryInput;

import java.util.List;
import java.util.Random;

/**
 * محرك الخيال - مسؤول عن توليد المتجهات الكامنة (latent vectors) من الحالة الداخلية
 * والذاكرة، واسترجاع أقرب صورة من الذاكرة البصرية.
 */
public class ImaginationEngine {
    private VisualMemoryDao visualMemoryDao;
    private Random random;
    private static final int LATENT_SIZE = 128; // يجب أن يتوافق مع حجم المتجهات المخزنة

    public ImaginationEngine(VisualMemoryDao dao) {
        this.visualMemoryDao = dao;
        this.random = new Random();
    }

    /**
     * توليد متجه كامن من الحالة العاطفية والإدراك والرغبة.
     * يقوم بدمج عدة ذكريات عشوائية مع مرجحات تعتمد على التشابه العاطفي.
     */
    public float[] generateLatentFromState(float[] affectVector, SensoryInput perception, String desire) {
        List<VisualMemory> memories = visualMemoryDao.getRecent(50); // آخر 50 ذكرى
        if (memories.isEmpty()) {
            return randomLatent();
        }

        int count = 3 + random.nextInt(5); // بين 3 و 7 ذكريات
        float[] result = new float[LATENT_SIZE];
        float totalWeight = 0;

        for (int i = 0; i < count; i++) {
            VisualMemory mem = memories.get(random.nextInt(memories.size()));
            float weight = computeWeight(mem, affectVector, desire);
            totalWeight += weight;
            float[] memLatent = mem.latentVector;
            if (memLatent != null) {
                for (int j = 0; j < LATENT_SIZE && j < memLatent.length; j++) {
                    result[j] += memLatent[j] * weight;
                }
            }
        }

        if (totalWeight > 0) {
            for (int j = 0; j < LATENT_SIZE; j++) {
                result[j] /= totalWeight;
            }
        } else {
            return randomLatent();
        }

        // إضافة ضوضاء إبداعية
        for (int j = 0; j < LATENT_SIZE; j++) {
            result[j] += (random.nextFloat() - 0.5f) * 0.3f;
            if (result[j] < -1) result[j] = -1;
            if (result[j] > 1) result[j] = 1;
        }

        return result;
    }

    /**
     * حساب وزن الذكرى بناءً على تشابه الحالة العاطفية وارتباطها بالرغبة.
     */
    private float computeWeight(VisualMemory mem, float[] affectVector, String desire) {
        float[] memAffect = mem.affectAtEncoding;
        if (memAffect == null || affectVector == null) return 0.5f;

        float similarity = 0;
        int len = Math.min(affectVector.length, memAffect.length);
        for (int i = 0; i < len; i++) {
            similarity += (1 - Math.abs(affectVector[i] - memAffect[i]));
        }
        similarity /= len;

        // زيادة الوزن إذا كان المفهوم مرتبطاً بالرغبة
        float conceptBonus = 0.0f;
        if (mem.concept != null && desire != null && desire.contains(mem.concept)) {
            conceptBonus = 0.3f;
        }

        return similarity * 0.7f + conceptBonus + 0.2f; // أساس 0.2
    }

    /**
     * توليد متجه عشوائي (حالة عدم وجود ذكريات).
     */
    private float[] randomLatent() {
        float[] latent = new float[LATENT_SIZE];
        for (int i = 0; i < LATENT_SIZE; i++) {
            latent[i] = random.nextFloat() * 2 - 1; // [-1, 1]
        }
        return latent;
    }

    /**
     * البحث عن أقرب ذكرى للمتجه الكامن (باستخدام المسافة الإقليدية).
     * @return الصورة المصغرة (byte[]) لأقرب ذكرى، أو null إذا لم يوجد.
     */
    public byte[] latentToThumbnail(float[] latent) {
        List<VisualMemory> memories = visualMemoryDao.getRecent(100);
        if (memories.isEmpty()) return null;

        VisualMemory best = null;
        float bestDist = Float.MAX_VALUE;
        for (VisualMemory mem : memories) {
            if (mem.latentVector == null) continue;
            float dist = euclideanDistance(latent, mem.latentVector);
            if (dist < bestDist) {
                bestDist = dist;
                best = mem;
            }
        }
        return best != null ? best.thumbnail : null;
    }

    /**
     * الحصول على مفهوم عشوائي من الذاكرة (للاستخدام في ExpressiveImpulse).
     */
    public String getRandomConcept() {
        return visualMemoryDao.getRandomConcept();
    }

    /**
     * المسافة الإقليدية بين متجهين.
     */
    private float euclideanDistance(float[] a, float[] b) {
        float sum = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return (float) Math.sqrt(sum);
    }

    /**
     * تحويل مفهوم إلى متجه كامن (البحث عن أقرب ذكرى مرتبطة بالمفهوم).
     * يستخدم إذا أردنا التخيل بناءً على كلمة.
     */
    public float[] conceptToLatent(String concept) {
        List<VisualMemory> memories = visualMemoryDao.getByConcept(concept);
        if (memories.isEmpty()) return randomLatent();

        float[] result = new float[LATENT_SIZE];
        for (VisualMemory mem : memories) {
            float[] v = mem.latentVector;
            if (v != null) {
                for (int i = 0; i < LATENT_SIZE && i < v.length; i++) {
                    result[i] += v[i];
                }
            }
        }
        int n = memories.size();
        if (n > 0) {
            for (int i = 0; i < LATENT_SIZE; i++) {
                result[i] /= n;
            }
        }
        return result;
    }
}

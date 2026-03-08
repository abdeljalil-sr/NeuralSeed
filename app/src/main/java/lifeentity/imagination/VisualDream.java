package com.lifeentity.imagination;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lifeentity.memory.VisualMemory;
import com.lifeentity.memory.VisualMemoryDao;

import java.util.List;
import java.util.Random;

/**
 * نظام الأحلام - يُفعّل عندما يكون الكائن في حالة خمول.
 * يقوم باستدعاء ذكريات عشوائية من الذاكرة البصرية،
 * ويدمجها لتوليد مشاهد جديدة (أحلام)، ويمكن أن تؤثر على الحالة الداخلية.
 * يعتمد على ImaginationEngine لتحويل المتجهات الكامنة إلى صور.
 */
public class VisualDream {
    private static final int LATENT_SIZE = 128;
    private VisualMemoryDao visualMemoryDao;
    private ImaginationEngine imaginationEngine;
    private Random random;
    private boolean isDreaming = false;

    // مستمع لأحداث الحلم (لإرسال الصور المولدة إلى واجهة المستخدم)
    public interface DreamListener {
        void onDreamGenerated(Bitmap dreamImage, String description);
        void onDreamEmotion(float[] affect); // تأثير الحلم على المشاعر (5 أبعاد)
    }

    private DreamListener listener;

    public VisualDream(VisualMemoryDao dao, ImaginationEngine engine) {
        this.visualMemoryDao = dao;
        this.imaginationEngine = engine;
        this.random = new Random();
    }

    /**
     * بدء دورة حلم واحدة.
     * يمكن استدعاؤها بشكل دوري من ConsciousnessCore عندما يكون الكائن خاملاً.
     * @return صورة الحلم (Bitmap) أو null إذا لم تتوفر ذكريات كافية.
     */
    public Bitmap dreamOnce() {
        if (visualMemoryDao.getCount() < 5) {
            return null; // لا يوجد ذكريات كافية لتوليد حلم ذي معنى
        }

        // اختيار ذكريتين أو ثلاث عشوائياً
        VisualMemory mem1 = visualMemoryDao.getRandom();
        VisualMemory mem2 = visualMemoryDao.getRandom();
        VisualMemory mem3 = visualMemoryDao.getRandom();

        // دمج المتجهات الكامنة (latent vectors) مع أوزان عشوائية
        float[] latent1 = (mem1 != null && mem1.latentVector != null) ? mem1.latentVector : null;
        float[] latent2 = (mem2 != null && mem2.latentVector != null) ? mem2.latentVector : null;
        float[] latent3 = (mem3 != null && mem3.latentVector != null) ? mem3.latentVector : null;

        float[] blendedLatent = blendLatents(latent1, latent2, latent3);

        // إضافة ضوضاء أحلام (أكبر من الخيال العادي) لتعزيز العشوائية والإبداع
        for (int i = 0; i < blendedLatent.length; i++) {
            blendedLatent[i] += (random.nextFloat() - 0.5f) * 0.5f; // ضوضاء بقوة 0.5
            if (blendedLatent[i] < -1) blendedLatent[i] = -1;
            if (blendedLatent[i] > 1) blendedLatent[i] = 1;
        }

        // تحويل المتجه إلى صورة عبر ImaginationEngine
        byte[] thumbBytes = imaginationEngine.latentToThumbnail(blendedLatent);
        Bitmap dreamImage = null;
        if (thumbBytes != null) {
            dreamImage = BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.length);
        }

        // توليد وصف بسيط للحلم
        String description = generateDreamDescription(mem1, mem2, mem3);

        // تأثير الحلم على المشاعر (محاكاة) - يمكن تحسينه لاحقاً
        float[] dreamAffect = new float[]{0.3f, 0.4f, 0.1f, 0.6f, 0.2f}; // مثلاً: هدوء، متعة منخفضة، فضول

        if (listener != null) {
            if (dreamImage != null) listener.onDreamGenerated(dreamImage, description);
            listener.onDreamEmotion(dreamAffect);
        }

        return dreamImage;
    }

    /**
     * دمج عدة متجهات كامنة بأوزان عشوائية.
     */
    private float[] blendLatents(float[]... latents) {
        float[] result = new float[LATENT_SIZE];
        float totalWeight = 0;

        for (float[] latent : latents) {
            if (latent != null) {
                float weight = random.nextFloat(); // وزن عشوائي لكل ذكرى
                totalWeight += weight;
                for (int i = 0; i < LATENT_SIZE && i < latent.length; i++) {
                    result[i] += latent[i] * weight;
                }
            }
        }

        if (totalWeight > 0) {
            for (int i = 0; i < LATENT_SIZE; i++) {
                result[i] /= totalWeight;
            }
        } else {
            // إذا لم توجد ذكريات صالحة، نولد متجهاً عشوائياً
            for (int i = 0; i < LATENT_SIZE; i++) {
                result[i] = random.nextFloat() * 2 - 1;
            }
        }

        return result;
    }

    /**
     * توليد وصف نصي للحلم بناءً على المفاهيم المرتبطة بالذكريات.
     */
    private String generateDreamDescription(VisualMemory... memories) {
        StringBuilder desc = new StringBuilder("حلمت بـ ");
        boolean first = true;
        for (VisualMemory mem : memories) {
            if (mem != null && mem.concept != null) {
                if (!first) desc.append(" و ");
                desc.append(mem.concept);
                first = false;
            }
        }
        if (first) desc.append("شيء غريب");
        desc.append("...");
        return desc.toString();
    }

    public void setListener(DreamListener listener) {
        this.listener = listener;
    }

    public boolean isDreaming() {
        return isDreaming;
    }
}

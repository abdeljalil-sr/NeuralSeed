package com.lifeentity.imagination;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.lifeentity.memory.VisualMemoryDao;

import java.util.Random;

/**
 * الخيال البصري – مسؤول عن تحويل الدوافع التعبيرية (latent vectors) إلى صور ملموسة.
 * يعتمد على ImaginationEngine لاسترجاع أقرب صورة من الذاكرة البصرية،
 * مع إمكانية إضافة تأثيرات أو توليد صور عشوائية في حالة عدم وجود ذكريات.
 */
public class VisualImagination {
    private Bitmap canvas;
    private Paint paint;
    private Random random;
    private ImaginationEngine engine;       // للتعامل مع الذاكرة البصرية

    public VisualImagination(int w, int h, VisualMemoryDao visualMemoryDao) {
        canvas = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        random = new Random();
        engine = new ImaginationEngine(visualMemoryDao); // نمرر الـ Dao مباشرة
    }

    /**
     * تعلم من الإدراك البصري – يمكن استخدامها لتخزين الصور في الذاكرة.
     * هذه الدالة تستدعيها VisionCortex لحفظ ما يراه الكائن.
     */
    public void learnFromPerception(String label, byte[] imageBytes, float[] affectVector) {
        if (label == null || imageBytes == null) return;
        // هنا يمكن تحويل imageBytes إلى thumbnail وتخزينه في قاعدة البيانات
        // لكن هذه المسؤولية ربما تكون في VisionCortex نفسه.
        // سنتركها فارغة أو ننقلها لاحقاً.
    }

    /**
     * تخيل صورة بناءً على متجه كامن (latent vector) من ConsciousnessCore.
     * @param latentVector المتجه الكامن (يمثل الفكرة)
     * @param intensity شدة الدافع (تؤثر على التعديلات)
     * @param mode وضع التخيل (PERCEPTUAL, MEMORY, CREATIVE, DREAM)
     * @return صورة Bitmap تمثل التخيل
     */
    public Bitmap imagine(float[] latentVector, float intensity, ImaginationMode mode) {
        Canvas c = new Canvas(canvas);
        c.drawColor(Color.BLACK); // مسح الرسم السابق

        if (latentVector == null) {
            // إذا لم يوجد متجه، نستخدم التوليد العشوائي القديم
            return generateRandomImage(c, intensity, "random");
        }

        // 1. استرجاع أقرب صورة من الذاكرة (thumbnail)
        byte[] thumbBytes = engine.latentToThumbnail(latentVector);
        Bitmap thumbnail = null;
        if (thumbBytes != null) {
            thumbnail = BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.length);
        }

        // 2. إذا وجدت صورة، نعرضها مع تأثيرات حسب الوضع
        if (thumbnail != null) {
            // تكبير/تصغير الصورة لتناسب حجم اللوحة (اختياري)
            Bitmap scaled = Bitmap.createScaledBitmap(thumbnail, canvas.getWidth(), canvas.getHeight(), true);
            c.drawBitmap(scaled, 0, 0, null);

            // إضافة تأثيرات حسب الوضع والشدة
            if (mode == ImaginationMode.CREATIVE || mode == ImaginationMode.DREAM) {
                // تعديل الألوان أو إضافة ضوضاء إبداعية
                applyCreativeEffects(c, intensity);
            }

            return canvas;
        } else {
            // إذا لم توجد ذاكرة، نولد صورة عشوائية
            return generateRandomImage(c, intensity, "unknown");
        }
    }

    /**
     * توليد صورة عشوائية (الطريقة القديمة) كحل بديل.
     */
    private Bitmap generateRandomImage(Canvas c, float intensity, String seed) {
        paint.setAlpha((int)(255 * intensity));
        random.setSeed(seed.hashCode() + System.currentTimeMillis());

        for (int i = 0; i < 20; i++) {
            float x = random.nextFloat() * canvas.getWidth();
            float y = random.nextFloat() * canvas.getHeight();
            float r = 20 + random.nextFloat() * 100 * intensity;

            int rc = 100 + random.nextInt(155);
            int gc = 100 + random.nextInt(155);
            int bc = 100 + random.nextInt(155);
            paint.setColor(Color.argb(150, rc, gc, bc));

            if (random.nextBoolean()) {
                c.drawCircle(x, y, r, paint);
            } else {
                Path p = new Path();
                p.moveTo(x, y - r);
                p.lineTo(x + r, y + r);
                p.lineTo(x - r, y + r);
                p.close();
                c.drawPath(p, paint);
            }
        }
        return canvas;
    }

    /**
     * تطبيق تأثيرات إبداعية على الصورة الحالية (المرسومة بالفعل على الـ Canvas).
     */
    private void applyCreativeEffects(Canvas c, float intensity) {
        // مثال: رسم بعض الخطوط أو الدوائر الشفافة فوق الصورة
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5 * intensity);
        paint.setColor(Color.argb(100, 255, 255, 255));
        for (int i = 0; i < 5; i++) {
            float x1 = random.nextFloat() * canvas.getWidth();
            float y1 = random.nextFloat() * canvas.getHeight();
            float x2 = random.nextFloat() * canvas.getWidth();
            float y2 = random.nextFloat() * canvas.getHeight();
            c.drawLine(x1, y1, x2, y2, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * نسخة احتياطية للتوافق مع الاستدعاءات القديمة (تستخدم concept بدلاً من latent)
     */
    public Bitmap imagine(String concept, ImaginationMode mode, float intensity) {
        // إذا كان المفهوم معروفاً، يمكن البحث عن صورة مرتبطة به
        byte[] thumbBytes = engine.getThumbnailForConcept(concept);
        if (thumbBytes != null) {
            return imagine(engine.conceptToLatent(concept), intensity, mode);
        } else {
            return generateRandomImage(new Canvas(canvas), intensity, concept);
        }
    }

    public enum ImaginationMode {
        PERCEPTUAL,    // مستوحى من الإدراك الحالي
        MEMORY,        // استدعاء ذاكرة محددة
        COUNTERFACTUAL,// تخيل بديل (ماذا لو)
        CREATIVE,      // إبداع حر (يمزج ذكريات)
        DREAM          // أحلام (عشوائية موجهة)
    }
}

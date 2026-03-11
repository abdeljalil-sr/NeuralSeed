package com.lifeentity.imagination;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import com.lifeentity.core.EmotionalState;
import com.lifeentity.memory.VisualMemory;
import com.lifeentity.memory.VisualMemoryDao;
import com.lifeentity.sensors.SensoryInput;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * محرك الخيال المتقدم - يدعم التوليد الكامل للصور عبر نموذج VAE/GAN
 * يمكنه إنشاء صور جديدة تماماً، وتعديلها، ودمجها مع عناصر مرسومة.
 */
public class ImaginationEngine {
    private static final String TAG = "ImaginationEngine";
    private static final int LATENT_SIZE = 128;
    private static final int IMAGE_SIZE = 256; // حجم الصورة المولدة
    private static final int MAX_CACHED_MEMORIES = 500;

    private VisualMemoryDao visualMemoryDao;
    private Random random;
    private float creativityLevel;
    private float currentEmotionalIntensity;

    // النموذج التوليدي (VAE Decoder)
    private Interpreter tflite;
    private boolean modelLoaded = false;

    // ذاكرة مؤقتة للبحث
    private List<VisualMemory> memoryCache;
    private ConcurrentHashMap<String, float[]> conceptCentroids;
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 30000;

    // عناصر الرسم الحر (يستخدمها SharedCanvas)
    private List<DrawingElement> drawingElements;

    // فئة لعنصر رسم حر
    public static class DrawingElement {
        public enum Type { PATH, CIRCLE, RECTANGLE, TEXT, IMAGE }
        public Type type;
        public float x, y;
        public float width, height;
        public int color;
        public int alpha;
        public Path path;
        public String text;
        public Bitmap image;
        public float rotation;
        public float scale = 1.0f;
    }

    public ImaginationEngine(VisualMemoryDao dao) {
        this.visualMemoryDao = dao;
        this.random = new Random();
        this.creativityLevel = 0.5f;
        this.currentEmotionalIntensity = 0.5f;
        this.memoryCache = new ArrayList<>();
        this.conceptCentroids = new ConcurrentHashMap<>();
        this.drawingElements = new ArrayList<>();

        // محاولة تحميل نموذج VAE من assets
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(2);
            tflite = new Interpreter(FileUtil.loadMappedFile(null, "vae_decoder.tflite"), options);
            modelLoaded = true;
            Log.i(TAG, "VAE model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load VAE model", e);
            modelLoaded = false;
        }
    }

    // ==================== توليد الصور من المتجهات ====================

    /**
     * توليد صورة جديدة من متجه كامن باستخدام النموذج
     */
    public Bitmap generateImageFromLatent(float[] latent) {
        if (!modelLoaded || tflite == null) {
            return generateFallbackImage(latent);
        }

        // تحويل المتجه إلى المدخلات المناسبة للنموذج
        ByteBuffer input = ByteBuffer.allocateDirect(4 * LATENT_SIZE);
        input.order(ByteOrder.nativeOrder());
        for (float f : latent) {
            input.putFloat(f);
        }

        // مصفوفة الخرج (مثلاً 256x256x3)
        float[][][][] output = new float[1][IMAGE_SIZE][IMAGE_SIZE][3];
        tflite.run(input, output);

        // تحويل الخرج إلى Bitmap
        return floatArrayToBitmap(output[0], IMAGE_SIZE, IMAGE_SIZE);
    }

    /**
     * توليد صورة احتياطية إذا لم يتوفر النموذج
     */
    private Bitmap generateFallbackImage(float[] latent) {
        Bitmap bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        // استخدام المتجه لتحديد الألوان والأشكال (محاكاة بسيطة)
        int r = (int) (Math.abs(latent[0]) * 255);
        int g = (int) (Math.abs(latent[1]) * 255);
        int b = (int) (Math.abs(latent[2]) * 255);
        paint.setColor(Color.rgb(r, g, b));

        // رسم بعض الأشكال العشوائية
        for (int i = 0; i < 10; i++) {
            float x = (latent[i * 3] * 0.5f + 0.5f) * IMAGE_SIZE;
            float y = (latent[i * 3 + 1] * 0.5f + 0.5f) * IMAGE_SIZE;
            float radius = (Math.abs(latent[i * 3 + 2]) * 0.5f + 0.5f) * 50;
            canvas.drawCircle(x, y, radius, paint);
        }

        return bitmap;
    }

    /**
     * تحويل مصفوفة float إلى Bitmap
     */
    private Bitmap floatArrayToBitmap(float[][][] array, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = (int) (Math.min(1.0f, Math.max(0, array[y][x][0])) * 255);
                int g = (int) (Math.min(1.0f, Math.max(0, array[y][x][1])) * 255);
                int b = (int) (Math.min(1.0f, Math.max(0, array[y][x][2])) * 255);
                pixels[y * width + x] = Color.rgb(r, g, b);
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    // ==================== توليد متجهات كامنة من المفاهيم ====================

    /**
     * توليد متجه كامن يصف مفهوماً معيناً
     */
    public float[] generateLatentForConcept(String concept, EmotionalState emotion) {
        refreshCacheIfNeeded();

        // البحث عن ذكريات مرتبطة بالمفهوم
        List<VisualMemory> relevant = new ArrayList<>();
        for (VisualMemory mem : memoryCache) {
            if (mem.concept != null && mem.concept.contains(concept)) {
                relevant.add(mem);
            }
        }

        if (relevant.isEmpty()) {
            // توليد متجه عشوائي مع توجيه عاطفي
            return generateRandomLatentWithEmotion(emotion);
        }

        // مزج الذكريات المرتبطة بالمفهوم
        List<WeightedMemory> weighted = new ArrayList<>();
        for (VisualMemory mem : relevant) {
            float weight = 1.0f;
            if (emotion != null) {
                // تعزيز الذكريات المتوافقة عاطفياً
                if (mem.affectAtEncoding != null) {
                    float[] affect = emotion.toAffectVector();
                    float sim = cosineSimilarity(affect, mem.affectAtEncoding);
                    weight += sim * 0.5f;
                }
            }
            weighted.add(new WeightedMemory(mem, weight));
        }

        return multiPointInterpolation(weighted);
    }

    /**
     * توليد متجه عشوائي متأثر بالحالة العاطفية
     */
    private float[] generateRandomLatentWithEmotion(EmotionalState emotion) {
        float[] latent = new float[LATENT_SIZE];
        float[] affect = emotion != null ? emotion.toAffectVector() : new float[]{0.5f, 0.5f, 0.5f, 0.5f, 0.5f};

        for (int i = 0; i < LATENT_SIZE; i++) {
            // استخدام الحالة العاطفية كمتوسط للمتجه
            float emotionalBias = (i < affect.length) ? affect[i] : 0.5f;
            latent[i] = (float) random.nextGaussian() * 0.3f + emotionalBias * 0.7f;
            if (latent[i] < -1) latent[i] = -1;
            if (latent[i] > 1) latent[i] = 1;
        }
        return latent;
    }

    // ==================== التعديل على الصور ====================

    /**
     * تعديل صورة موجودة (مثلاً: تغيير تعبير الوجه، إضافة عناصر)
     */
    public Bitmap modifyImage(Bitmap source, String modification, float intensity) {
        // هنا يمكن تطبيق تحولات مثل تغيير الألوان، إضافة ضوضاء، تشويه
        // باستخدام النموذج أو معالجة الصور التقليدية

        Bitmap result = source.copy(source.getConfig(), true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        // مثال: إضافة دائرة شفافة
        paint.setColor(Color.argb((int)(intensity * 100), 255, 0, 0));
        canvas.drawCircle(source.getWidth()/2, source.getHeight()/2, 
                          source.getWidth()/4 * intensity, paint);

        return result;
    }

    // ==================== عناصر الرسم الحر ====================

    /**
     * إنشاء عنصر رسم جديد (خط، دائرة، نص، إلخ)
     */
    public DrawingElement createDrawingElement(DrawingElement.Type type, float x, float y, 
                                                float width, float height, int color, int alpha) {
        DrawingElement element = new DrawingElement();
        element.type = type;
        element.x = x;
        element.y = y;
        element.width = width;
        element.height = height;
        element.color = color;
        element.alpha = alpha;
        return element;
    }

    /**
     * إنشاء مسار معقد
     */
    public Path createPath(List<PointF> points, boolean closed) {
        Path path = new Path();
        if (points.isEmpty()) return path;

        path.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).x, points.get(i).y);
        }
        if (closed) path.close();
        return path;
    }

    /**
     * إضافة عنصر رسم إلى القائمة
     */
    public void addDrawingElement(DrawingElement element) {
        drawingElements.add(element);
    }

    /**
     * مسح جميع عناصر الرسم
     */
    public void clearDrawingElements() {
        drawingElements.clear();
    }

    /**
     * رسم جميع العناصر على Canvas
     */
    public void drawAllElements(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (DrawingElement element : drawingElements) {
            paint.setColor(element.color);
            paint.setAlpha(element.alpha);
            canvas.save();
            canvas.translate(element.x, element.y);
            canvas.rotate(element.rotation);
            canvas.scale(element.scale, element.scale);

            switch (element.type) {
                case PATH:
                    if (element.path != null) canvas.drawPath(element.path, paint);
                    break;
                case CIRCLE:
                    canvas.drawCircle(0, 0, element.width/2, paint);
                    break;
                case RECTANGLE:
                    canvas.drawRect(-element.width/2, -element.height/2, 
                                     element.width/2, element.height/2, paint);
                    break;
                case TEXT:
                    paint.setTextSize(element.height);
                    canvas.drawText(element.text, 0, 0, paint);
                    break;
                case IMAGE:
                    if (element.image != null) {
                        canvas.drawBitmap(element.image, -element.width/2, -element.height/2, paint);
                    }
                    break;
            }
            canvas.restore();
        }
    }

    // ==================== وظائف الذاكرة والبحث ====================

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > CACHE_TTL_MS || memoryCache.isEmpty()) {
            memoryCache = visualMemoryDao.getRecent(MAX_CACHED_MEMORIES);
            lastCacheUpdate = now;
            Log.d(TAG, "Cache refreshed with " + memoryCache.size() + " memories");
        }
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

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = dotProduct(a, b);
        float normA = (float) Math.sqrt(dotProduct(a, a));
        float normB = (float) Math.sqrt(dotProduct(b, b));
        return dot / (normA * normB + 1e-8f);
    }

    // ==================== واجهة للوعي ====================

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
    }

    public String getRandomConcept() {
        return visualMemoryDao.getRandomConcept();
    }

    public float getCreativityLevel() {
        return creativityLevel;
    }

    // ==================== الفئات الداخلية ====================

    private static class WeightedMemory {
        final VisualMemory memory;
        final float weight;
        WeightedMemory(VisualMemory m, float w) { memory = m; weight = w; }
    }

    public static class PointF {
        public float x, y;
        public PointF(float x, float y) { this.x = x; this.y = y; }
    }
}

package com.lifeentity.perception;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.lifeentity.memory.VisualMemory;
import com.lifeentity.memory.VisualMemoryDao;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * فهم المشاهد - يستخدم نموذج تعلم عميق (TensorFlow Lite) لتحليل الصور
 * واستخراج المفاهيم والعلاقات. يقوم بتخزين النتائج في الذاكرة البصرية
 * ويمكن استخدامها لتغذية EmbeddingsEngine.
 */
public class SceneUnderstanding {
    private static final String TAG = "SceneUnderstanding";
    // اسم ملف النموذج في مجلد assets (يمكن استبداله بأي نموذج تصنيف صور)
    private static final String MODEL_FILE = "mobilenet_v1_1.0_224_quant.tflite";
    private static final String LABEL_FILE = "labels.txt";
    private static final int INPUT_SIZE = 224; // حجم الإدخال للنموذج
    private static final int LATENT_SIZE = 128;

    private Interpreter tflite;
    private List<String> labels;
    private boolean modelLoaded = false;

    private VisualMemoryDao visualMemoryDao;

    public SceneUnderstanding(Context context, VisualMemoryDao dao) {
        this.visualMemoryDao = dao;
        try {
            // تحميل النموذج من مجلد assets
            tflite = new Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE));
            // تحميل labels (افتراضياً موجودة في assets/labels.txt)
            labels = FileUtil.loadLabels(context, LABEL_FILE);
            modelLoaded = true;
            Log.i(TAG, "Model loaded successfully. Labels: " + labels.size());
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model", e);
        }
    }

    /**
     * تحليل صورة واستخراج قائمة بالمفاهيم الموجودة مع نسب الثقة.
     * @param image الصورة المدخلة (Bitmap)
     * @return قائمة من كائنات ConceptResult تحتوي على المفهوم والثقة (أعلى 5 نتائج).
     */
    public List<ConceptResult> analyzeScene(Bitmap image) {
        List<ConceptResult> results = new ArrayList<>();
        if (!modelLoaded || tflite == null || image == null) {
            Log.w(TAG, "Model not loaded or image null, returning empty list");
            return results;
        }

        // تحضير الصورة للإدخال (تصغير وتطبيع)
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(image, INPUT_SIZE, INPUT_SIZE, true);
        TensorImage tensorImage = TensorImage.fromBitmap(resizedBitmap);
        ByteBuffer inputBuffer = tensorImage.getBuffer();

        // تجهيز المخزن المؤقت للمخرجات (بافتراض أن النموذج يخرج float[1][num_labels])
        int numLabels = labels.size();
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, numLabels}, org.tensorflow.lite.support.tensorbuffer.TensorBuffer.DataType.FLOAT32);

        // تشغيل النموذج
        tflite.run(inputBuffer, outputBuffer.getBuffer().rewind());

        // الحصول على الخريطة (التسمية -> الثقة)
        Map<String, Float> labeledResults = new TensorLabel(labels, outputBuffer).getMapWithFloatValue();

        // ترتيب وتصفية النتائج (أعلى 5)
        labeledResults.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> results.add(new ConceptResult(entry.getKey(), entry.getValue())));

        return results;
    }

    /**
     * تحليل صورة وحفظ النتائج في الذاكرة البصرية مع المتجه الكامن.
     * @param image الصورة (كاملة الدقة)
     * @param affect المتجه العاطفي الحالي (5 أبعاد)
     */
    public void learnScene(Bitmap image, float[] affect) {
        if (image == null) return;

        List<ConceptResult> concepts = analyzeScene(image);
        if (concepts.isEmpty()) return;

        // أخذ أفضل مفهوم
        ConceptResult top = concepts.get(0);
        String mainConcept = top.label;
        float confidence = top.confidence;

        // توليد متجه كامن من الصورة والنتائج
        float[] latent = extractLatentFromImage(image, concepts);

        // تصغير الصورة لحفظها في الذاكرة
        Bitmap thumbnail = Bitmap.createScaledBitmap(image, 64, 64, true);
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.PNG, 90, stream);
        byte[] thumbBytes = stream.toByteArray();

        // تخزين في VisualMemory
        VisualMemory visualMemory = new VisualMemory();
        visualMemory.timestamp = System.currentTimeMillis();
        visualMemory.latentVector = latent;
        visualMemory.thumbnail = thumbBytes;
        visualMemory.concept = mainConcept;
        visualMemory.affectAtEncoding = affect;

        // حفظ في الخلفية
        new Thread(() -> visualMemoryDao.insert(visualMemory)).start();

        Log.i(TAG, "Learned scene: " + mainConcept + " with confidence " + confidence);
    }

    /**
     * استخراج متجه كامن من الصورة ونتائج التصنيف.
     * يستخدم متوسط RGB وأعلى 5 مفاهيم لملء المتجه.
     */
    private float[] extractLatentFromImage(Bitmap image, List<ConceptResult> concepts) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getPixels(pixels, 0, w, 0, 0, w, h);

        float rAvg = 0, gAvg = 0, bAvg = 0;
        for (int p : pixels) {
            rAvg += (p >> 16) & 0xFF;
            gAvg += (p >> 8) & 0xFF;
            bAvg += p & 0xFF;
        }
        int n = pixels.length;
        rAvg = rAvg / n / 255f;
        gAvg = gAvg / n / 255f;
        bAvg = bAvg / n / 255f;

        float[] latent = new float[LATENT_SIZE];
        latent[0] = rAvg;
        latent[1] = gAvg;
        latent[2] = bAvg;

        // نضع نسب الثقة لأول 10 مفاهيم (أو أقل)
        for (int i = 0; i < Math.min(concepts.size(), 10); i++) {
            latent[3 + i] = concepts.get(i).confidence;
        }

        // الأبعاد المتبقية تُملأ عشوائياً بشكل متناسق (يمكن تحسينها)
        for (int i = 3 + Math.min(concepts.size(), 10); i < LATENT_SIZE; i++) {
            latent[i] = (float) Math.random() * 2 - 1;
        }

        return latent;
    }

    /**
     * فئة تمثل نتيجة مفهوم مع ثقة.
     */
    public static class ConceptResult {
        public final String label;
        public final float confidence;

        public ConceptResult(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }

    /**
     * إغلاق النموذج لتحرير الموارد.
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}

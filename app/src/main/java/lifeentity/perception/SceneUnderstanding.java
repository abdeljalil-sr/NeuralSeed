// ====================== perception/SceneUnderstanding.java (معدل بالكامل) ======================
package com.lifeentity.perception;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.lifeentity.memory.VisualMemory;
import com.lifeentity.memory.VisualMemoryDao;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SceneUnderstanding {
    private static final String TAG = "SceneUnderstanding";
    private static final String MODEL_FILE = "mobilenet_v1_1.0_224_quant.tflite";
    private static final String LABEL_FILE = "labels.txt";
    private static final int INPUT_SIZE = 224;
    private static final int LATENT_SIZE = 128;

    private Interpreter tflite;
    private List<String> labels;
    private boolean modelLoaded = false;

    private VisualMemoryDao visualMemoryDao;

    public SceneUnderstanding(Context context, VisualMemoryDao dao) {
        this.visualMemoryDao = dao;
        try {
            tflite = new Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE));
            labels = FileUtil.loadLabels(context, LABEL_FILE);
            modelLoaded = true;
            Log.i(TAG, "Model loaded successfully. Labels: " + labels.size());
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model", e);
        }
    }

    public List<ConceptResult> analyzeScene(Bitmap image) {
        List<ConceptResult> results = new ArrayList<>();
        if (!modelLoaded || tflite == null || image == null) {
            Log.w(TAG, "Model not loaded or image null");
            return results;
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(image, INPUT_SIZE, INPUT_SIZE, true);
        TensorImage tensorImage = TensorImage.fromBitmap(resizedBitmap);
        ByteBuffer inputBuffer = tensorImage.getBuffer();

        int numLabels = labels.size();
        // استخدام المسار الكامل لـ DataType (أو يمكن استيراده)
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, numLabels}, TensorBuffer.DataType.FLOAT32);

        tflite.run(inputBuffer, outputBuffer.getBuffer().rewind());

        Map<String, Float> labeledResults = new TensorLabel(labels, outputBuffer).getMapWithFloatValue();

        labeledResults.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> results.add(new ConceptResult(entry.getKey(), entry.getValue())));

        return results;
    }

    public void learnScene(Bitmap image, float[] affect) {
        if (image == null) return;

        List<ConceptResult> concepts = analyzeScene(image);
        if (concepts.isEmpty()) return;

        ConceptResult top = concepts.get(0);
        String mainConcept = top.label;
        float confidence = top.confidence;

        float[] latent = extractLatentFromImage(image, concepts);

        Bitmap thumbnail = Bitmap.createScaledBitmap(image, 64, 64, true);
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.PNG, 90, stream);
        byte[] thumbBytes = stream.toByteArray();

        VisualMemory visualMemory = new VisualMemory();
        visualMemory.timestamp = System.currentTimeMillis();
        visualMemory.latentVector = latent;
        visualMemory.thumbnail = thumbBytes;
        visualMemory.concept = mainConcept;
        visualMemory.affectAtEncoding = affect;

        new Thread(() -> visualMemoryDao.insert(visualMemory)).start();

        Log.i(TAG, "Learned scene: " + mainConcept + " with confidence " + confidence);
    }

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

        for (int i = 0; i < Math.min(concepts.size(), 10); i++) {
            latent[3 + i] = concepts.get(i).confidence;
        }

        for (int i = 3 + Math.min(concepts.size(), 10); i < LATENT_SIZE; i++) {
            latent[i] = (float) Math.random() * 2 - 1;
        }

        return latent;
    }

    public static class ConceptResult {
        public final String label;
        public final float confidence;
        public ConceptResult(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}

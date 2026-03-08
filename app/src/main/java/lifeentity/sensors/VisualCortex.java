package com.lifeentity.sensors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitFactory;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.VisualMemory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * القشرة البصرية (Visual Cortex) – مسؤولة عن التقاط الصور من الكاميرا،
 * اكتشاف الوجوه والأجسام، واستخراج المتجهات الكامنة (latent vectors)
 * لتخزينها في الذاكرة البصرية (VisualMemory).
 */
public class VisualCortex {
    private static final String TAG = "VisualCortex";
    private static final int THUMBNAIL_SIZE = 64; // حجم الصورة المصغرة

    private FaceDetector faceDetector;
    private ObjectDetector objectDetector;
    private ExecutorService analysisExecutor;
    private OnVisualPerceptionListener listener;
    private List<Face> lastFaces = new ArrayList<>();

    private AppDatabase database; // لحفظ الصور في VisualMemory

    public interface OnVisualPerceptionListener {
        void onPerception(VisualPerception perception);
        void onFacesDetected(List<Face> faces);
    }

    public VisualCortex(Context context, AppDatabase db) {
        this.database = db;

        faceDetector = FaceDetection.getClient();

        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();
        objectDetector = ObjectDetection.getClient(options);

        analysisExecutor = Executors.newSingleThreadExecutor();
    }

    public void start(LifecycleOwner lifecycleOwner, Context context) {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(analysisExecutor, this::analyzeFrame);

                // يمكن اختيار الكاميرا الأمامية أو الخلفية، هنا نستخدم الأمامية للوجوه
                CameraSelector selector = CameraSelector.DEFAULT_FRONT_CAMERA;

                provider.unbindAll();
                provider.bindToLifecycle(lifecycleOwner, selector, analysis);

            } catch (Exception e) {
                Log.e(TAG, "Camera failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void analyzeFrame(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    lastFaces = faces;
                    if (listener != null) {
                        listener.onFacesDetected(faces);
                    }

                    objectDetector.process(image)
                            .addOnSuccessListener(objects -> {
                                VisualPerception perception = createPerception(faces, objects);

                                // استخراج الصورة المصغرة وتخزينها في الذاكرة البصرية
                                Bitmap thumbnail = extractThumbnail(imageProxy);
                                if (thumbnail != null) {
                                    saveToVisualMemory(thumbnail, perception, faces);
                                }

                                if (listener != null) {
                                    listener.onPerception(perception);
                                }
                                imageProxy.close();
                            })
                            .addOnFailureListener(e -> imageProxy.close());
                })
                .addOnFailureListener(e -> imageProxy.close());
    }

    /**
     * إنشاء كائن VisualPerception من نتائج ML Kit.
     */
    private VisualPerception createPerception(List<Face> faces, List<com.google.mlkit.vision.objects.DetectedObject> objects) {
        VisualPerception p = new VisualPerception();
        p.faceCount = faces.size();

        if (!faces.isEmpty()) {
            Face mainFace = faces.get(0);
            p.mainFace = mainFace;
            p.smileProbability = mainFace.getSmilingProbability() != null ? mainFace.getSmilingProbability() : 0.5f;
            p.eyeOpenProbability = (mainFace.getLeftEyeOpenProbability() != null ? mainFace.getLeftEyeOpenProbability() : 1f)
                    * (mainFace.getRightEyeOpenProbability() != null ? mainFace.getRightEyeOpenProbability() : 1f);
            p.faceBounds = mainFace.getBoundingBox();

            // استخراج متجه الوجه (face embedding) – في الإصدار الحالي نستخدم دالة بسيطة
            p.faceEmbedding = extractFaceEmbedding(mainFace);
        }

        p.objects = new ArrayList<>();
        for (com.google.mlkit.vision.objects.DetectedObject obj : objects) {
            VisualObject vo = new VisualObject();
            if (!obj.getLabels().isEmpty()) {
                vo.label = obj.getLabels().get(0).getText();
                vo.confidence = obj.getLabels().get(0).getConfidence();
            } else {
                vo.label = "unknown";
                vo.confidence = 0.5f;
            }
            vo.bounds = obj.getBoundingBox();
            p.objects.add(vo);
        }

        return p;
    }

    /**
     * استخراج متجه وجه بسيط (مكان لحين استخدام نموذج حقيقي).
     */
    private float[] extractFaceEmbedding(Face face) {
        // يمكن استبدال هذا باستخدام FaceNet أو ML Kit face embedding إذا كان متاحاً
        // حالياً نستخدم مزيجاً من معالم الوجه لتوليد متجه 128 بعداً
        float[] embedding = new float[128];
        int i = 0;
        embedding[i++] = face.getBoundingBox().width() / 1000f;
        embedding[i++] = face.getBoundingBox().height() / 1000f;
        embedding[i++] = (face.getHeadEulerAngleY() + 90) / 180f;
        embedding[i++] = (face.getHeadEulerAngleZ() + 90) / 180f;
        embedding[i++] = face.getSmilingProbability() != null ? face.getSmilingProbability() : 0.5f;
        embedding[i++] = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0.5f;
        embedding[i++] = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0.5f;
        // الباقي عشوائي متناسق (يمكن تحسينه)
        for (; i < 128; i++) {
            embedding[i] = (float) Math.random(); // مؤقت
        }
        return embedding;
    }

    /**
     * استخراج صورة مصغرة من ImageProxy.
     */
    private Bitmap extractThumbnail(ImageProxy image) {
        // تحويل ImageProxy إلى Bitmap (هذه دالة مبسطة، قد تحتاج إلى تنفيذ حقيقي)
        // الافتراض أن الصورة بصيغة YUV_420_888
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int width = image.getWidth();
        int height = image.getHeight();

        // تحويل YUV إلى RGB (مبسط جداً – في التطبيق الحقيقي استخدم مكتبة)
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // ... هنا كود التحويل ...

        // تصغير الصورة
        return Bitmap.createScaledBitmap(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true);
    }

    /**
     * تحويل Bitmap إلى مصفوفة بايتات (PNG).
     */
    private byte[] bitmapToBytes(Bitmap bmp) {
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 90, stream);
        return stream.toByteArray();
    }

    /**
     * توليد متجه كامن بسيط من الصورة (يستخدم ImaginationEngine لاحقاً).
     */
    private float[] computeSimpleLatent(Bitmap bmp) {
        // يمكن تحسينها باستخدام نموذج encoder حقيقي
        float[] latent = new float[128];
        int[] pixels = new int[THUMBNAIL_SIZE * THUMBNAIL_SIZE];
        bmp.getPixels(pixels, 0, THUMBNAIL_SIZE, 0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        float rSum = 0, gSum = 0, bSum = 0;
        for (int p : pixels) {
            rSum += (p >> 16) & 0xFF;
            gSum += (p >> 8) & 0xFF;
            bSum += p & 0xFF;
        }
        int n = pixels.length;
        latent[0] = rSum / n / 255f;
        latent[1] = gSum / n / 255f;
        latent[2] = bSum / n / 255f;
        for (int i = 3; i < 128; i++) {
            latent[i] = (float) Math.random(); // مؤقت
        }
        return latent;
    }

    /**
     * حفظ الصورة في الذاكرة البصرية (VisualMemory).
     */
    private void saveToVisualMemory(Bitmap thumbnail, VisualPerception perception, List<Face> faces) {
        if (database == null) return;

        byte[] thumbBytes = bitmapToBytes(thumbnail);
        float[] latent = computeSimpleLatent(thumbnail);
        String concept = perception.objects.isEmpty() ? "scene" : perception.objects.get(0).label;

        VisualMemory mem = new VisualMemory();
        mem.timestamp = System.currentTimeMillis();
        mem.latentVector = latent;
        mem.thumbnail = thumbBytes;
        mem.concept = concept;
        // affectVector يمكن تمريره لاحقاً من ConsciousnessCore، نضعه مؤقتاً صفر
        mem.affectAtEncoding = new float[]{0,0,0,0,0};

        // تخزين في خلفية
        new Thread(() -> database.visualMemoryDao().insert(mem)).start();
    }

    public List<Face> getLastFaces() {
        return lastFaces;
    }

    public void setListener(OnVisualPerceptionListener l) {
        this.listener = l;
    }

    // ======================= الفئات الداخلية =======================

    public static class VisualPerception {
        public int faceCount;
        public Face mainFace;
        public float smileProbability;
        public float eyeOpenProbability;
        public android.graphics.Rect faceBounds;
        public float[] faceEmbedding; // متجه الوجه
        public List<VisualObject> objects;

        // يمكن إضافة دوال مساعدة مثل getAverageBrightness إذا أردت
    }

    public static class VisualObject {
        public String label;
        public float confidence;
        public android.graphics.Rect bounds;

        public int getArea() {
            return bounds != null ? bounds.width() * bounds.height() : 0;
        }
    }
}

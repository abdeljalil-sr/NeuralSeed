package com.lifeentity.sensors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.lifeentity.utils.YuvToRgbConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VisualCortex {
    private static final String TAG = "VisualCortex";
    private static final int THUMBNAIL_SIZE = 64;

    private FaceDetector faceDetector;
    private ObjectDetector objectDetector;
    private ExecutorService analysisExecutor;
    private OnVisualPerceptionListener listener;
    private List<Face> lastFaces = new ArrayList<>();

    private AppDatabase database;

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
                                Bitmap frameBitmap = YuvToRgbConverter.imageProxyToBitmap(imageProxy);
                                VisualPerception perception = createPerception(faces, objects, frameBitmap);
                                if (listener != null) {
                                    listener.onPerception(perception);
                                }
                                imageProxy.close();
                            })
                            .addOnFailureListener(e -> imageProxy.close());
                })
                .addOnFailureListener(e -> imageProxy.close());
    }

    private VisualPerception createPerception(List<Face> faces, List<com.google.mlkit.vision.objects.DetectedObject> objects, Bitmap frameBitmap) {
        VisualPerception p = new VisualPerception();
        p.faceCount = faces.size();
        p.frame = frameBitmap;

        if (!faces.isEmpty()) {
            Face mainFace = faces.get(0);
            p.mainFace = mainFace;
            p.smileProbability = mainFace.getSmilingProbability() != null ? mainFace.getSmilingProbability() : 0.5f;
            p.eyeOpenProbability = (mainFace.getLeftEyeOpenProbability() != null ? mainFace.getLeftEyeOpenProbability() : 1f)
                    * (mainFace.getRightEyeOpenProbability() != null ? mainFace.getRightEyeOpenProbability() : 1f);
            p.faceBounds = mainFace.getBoundingBox();
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

        if (frameBitmap != null) {
            saveToVisualMemory(frameBitmap, p, faces);
        }

        return p;
    }

    private float[] extractFaceEmbedding(Face face) {
        float[] embedding = new float[128];
        int i = 0;
        embedding[i++] = face.getBoundingBox().width() / 1000f;
        embedding[i++] = face.getBoundingBox().height() / 1000f;
        embedding[i++] = (face.getHeadEulerAngleY() + 90) / 180f;
        embedding[i++] = (face.getHeadEulerAngleZ() + 90) / 180f;
        embedding[i++] = face.getSmilingProbability() != null ? face.getSmilingProbability() : 0.5f;
        embedding[i++] = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0.5f;
        embedding[i++] = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0.5f;
        for (; i < 128; i++) {
            embedding[i] = (float) Math.random();
        }
        return embedding;
    }

    private byte[] bitmapToBytes(Bitmap bmp) {
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 90, stream);
        return stream.toByteArray();
    }

    private float[] computeSimpleLatent(Bitmap bmp) {
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
            latent[i] = (float) Math.random();
        }
        return latent;
    }

    private void saveToVisualMemory(Bitmap frame, VisualPerception perception, List<Face> faces) {
        if (database == null || frame == null) return;

        Bitmap thumbnail = Bitmap.createScaledBitmap(frame, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true);
        byte[] thumbBytes = bitmapToBytes(thumbnail);
        float[] latent = computeSimpleLatent(thumbnail);
        String concept = perception.objects.isEmpty() ? "scene" : perception.objects.get(0).label;

        VisualMemory mem = new VisualMemory();
        mem.timestamp = System.currentTimeMillis();
        mem.latentVector = latent;
        mem.thumbnail = thumbBytes;
        mem.concept = concept;
        mem.affectAtEncoding = new float[]{0,0,0,0,0};

        new Thread(() -> database.visualMemoryDao().insert(mem)).start();
    }

    public List<Face> getLastFaces() {
        return lastFaces;
    }

    public void setListener(OnVisualPerceptionListener l) {
        this.listener = l;
    }

    public static class VisualPerception {
        public int faceCount;
        public Face mainFace;
        public float smileProbability;
        public float eyeOpenProbability;
        public android.graphics.Rect faceBounds;
        public float[] faceEmbedding;
        public List<VisualObject> objects;
        public Bitmap frame;
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

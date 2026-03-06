package com.lifeentity.sensors;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VisualCortex {
    private static final String TAG = "VisualCortex";
    
    private FaceDetector faceDetector;
    private ObjectDetector objectDetector;
    private ExecutorService analysisExecutor;
    private OnVisualPerceptionListener listener;
    private List<Face> lastFaces = new ArrayList<>();
    
    public interface OnVisualPerceptionListener {
        void onPerception(VisualPerception perception);
        void onFacesDetected(List<Face> faces);
    }
    
    public VisualCortex(Context context) {
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
                        VisualPerception perception = createPerception(faces, objects);
                        if (listener != null) {
                            listener.onPerception(perception);
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> imageProxy.close());
            })
            .addOnFailureListener(e -> imageProxy.close());
    }
    
    private VisualPerception createPerception(List<Face> faces, List<com.google.mlkit.vision.objects.DetectedObject> objects) {
        VisualPerception p = new VisualPerception();
        p.faceCount = faces.size();
        
        if (!faces.isEmpty()) {
            p.mainFace = faces.get(0);
            Float smile = p.mainFace.getSmilingProbability();
            Float leftEye = p.mainFace.getLeftEyeOpenProbability();
            Float rightEye = p.mainFace.getRightEyeOpenProbability();
            
            p.smileProbability = smile != null ? smile : 0.5f;
            p.eyeOpenProbability = (leftEye != null ? leftEye : 1f) * 
                                  (rightEye != null ? rightEye : 1f);
            p.faceBounds = p.mainFace.getBoundingBox();
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
        public List<VisualObject> objects;
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

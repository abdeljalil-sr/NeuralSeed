package com.lifeentity.core;

import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.MemoryDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * نظام التنبؤ (Prediction System)
 * 
 * يقوم بالتنبؤ بنتائج الأفعال بناءً على الذاكرة والتجارب السابقة.
 * ويحسب خطأ التنبؤ (Prediction Error) الذي يستخدم في التعلم.
 */
public class PredictionSystem {
    
    private MemoryDao memoryDao;
    private Map<String, PredictionModel> models = new HashMap<>();
    
    // بنية لنموذج تنبؤ بسيط (يمكن توسيعها)
    private static class PredictionModel {
        float averageOutcome;      // متوسط النتائج السابقة
        int count;                 // عدد العينات
        float confidence;          // الثقة في النموذج (0-1)
        
        PredictionModel() {
            averageOutcome = 0.5f;
            count = 0;
            confidence = 0.1f;
        }
        
        float predict() {
            return averageOutcome;
        }
        
        void update(float outcome) {
            averageOutcome = (averageOutcome * count + outcome) / (count + 1);
            count++;
            confidence = Math.min(1.0f, count / 10.0f); // تزداد الثقة مع العينات
        }
    }
    
    public PredictionSystem(MemoryDao dao) {
        this.memoryDao = dao;
    }
    
    /**
     * التنبؤ بنتيجة إجراء معين في سياق معين
     * @param action الإجراء (مثل "explore", "speak", إلخ)
     * @param context سياق (مثل "face_detected", "silence")
     * @return القيمة المتوقعة (0-1) وخطأ التنبؤ (سيتم حسابه لاحقاً)
     */
    public PredictionResult predict(String action, String context) {
        String key = action + ":" + context;
        PredictionModel model = models.get(key);
        if (model == null) {
            model = new PredictionModel();
            models.put(key, model);
        }
        
        float predicted = model.predict();
        float confidence = model.confidence;
        
        return new PredictionResult(predicted, confidence, model);
    }
    
    /**
     * تحديث النموذج بناءً على النتيجة الفعلية
     * @param action الإجراء
     * @param context السياق
     * @param actualOutcome النتيجة الفعلية (0-1)
     * @return خطأ التنبؤ (prediction error)
     */
    public float updateModel(String action, String context, float actualOutcome) {
        String key = action + ":" + context;
        PredictionModel model = models.get(key);
        if (model == null) {
            model = new PredictionModel();
            models.put(key, model);
        }
        
        float predicted = model.predict();
        float error = Math.abs(actualOutcome - predicted);
        
        model.update(actualOutcome);
        
        return error;
    }
    
    /**
     * نتيجة التنبؤ
     */
    public static class PredictionResult {
        public final float predictedOutcome;
        public final float confidence;
        private final PredictionModel model;
        
        PredictionResult(float predicted, float confidence, PredictionModel model) {
            this.predictedOutcome = predicted;
            this.confidence = confidence;
            this.model = model;
        }
    }
}

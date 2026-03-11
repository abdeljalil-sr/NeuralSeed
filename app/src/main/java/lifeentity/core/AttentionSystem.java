package com.lifeentity.core;

import java.util.HashMap;
import java.util.Map;

/**
 * نظام الانتباه (Attention System)
 * 
 * المسؤول عن تحديد أولوية المصادر المختلفة بناءً على:
 * - الجدة (novelty)
 * - الأهمية العاطفية (emotional importance)
 * - الأهداف الحالية (current goals/desires)
 * - السياق (context)
 */
public class AttentionSystem {
    
    // أوزان الانتباه لكل مصدر (يتم تحديثها ديناميكياً)
    private Map<String, Float> attentionWeights = new HashMap<>();
    
    // ذاكرة للمدخلات السابقة لحساب الجدة
    private Map<String, Object> lastInputs = new HashMap<>();
    
    // معاملات
    private static final float NOVELTY_FACTOR = 0.4f;
    private static final float EMOTION_FACTOR = 0.3f;
    private static final float GOAL_FACTOR = 0.3f;
    
    public AttentionSystem() {
        // تهيئة الأوزان الافتراضية
        attentionWeights.put("perception", 1.0f);
        attentionWeights.put("memory", 0.8f);
        attentionWeights.put("desire", 0.9f);
        attentionWeights.put("imagination", 0.7f);
        attentionWeights.put("prediction", 0.6f);
        attentionWeights.put("self", 0.5f);
    }
    
    /**
     * حساب درجة الانتباه لمقترح معين
     */
    public float computeAttentionScore(GlobalWorkspace.WorkspaceProposal proposal, 
                                        EmotionalState currentEmotion, 
                                        String dominantDesire) {
        float baseWeight = attentionWeights.getOrDefault(proposal.source, 0.5f);
        
        // عامل الجدة (إذا كان المحتوى جديداً)
        float novelty = computeNovelty(proposal);
        
        // عامل العاطفة (إذا كان المحتوى مرتبطاً بالعاطفة الحالية)
        float emotionalRelevance = computeEmotionalRelevance(proposal, currentEmotion);
        
        // عامل الهدف (إذا كان المحتوى مرتبطاً بالرغبة الحالية)
        float goalRelevance = computeGoalRelevance(proposal, dominantDesire);
        
        // الدرجة النهائية
        return baseWeight * (NOVELTY_FACTOR * novelty + EMOTION_FACTOR * emotionalRelevance + GOAL_FACTOR * goalRelevance);
    }
    
    /**
     * تحديث أوزان الانتباه بناءً على النجاحات والإخفاقات
     */
    public void updateWeights(String source, float outcome) {
        float current = attentionWeights.getOrDefault(source, 0.5f);
        // إذا كان المصدر مفيداً (outcome موجب)، نزيد وزنه، والعكس
        float newWeight = current + outcome * 0.1f;
        newWeight = Math.max(0.1f, Math.min(2.0f, newWeight));
        attentionWeights.put(source, newWeight);
    }
    
    private float computeNovelty(GlobalWorkspace.WorkspaceProposal proposal) {
        Object last = lastInputs.get(proposal.source);
        if (last == null) return 1.0f; // جديد تماماً
        
        // هنا يمكن أن نقارن المحتوى بطريقة ذكية (مثل التشابه)
        // حالياً نستخدم مقارنة بسيطة: إذا تغير المحتوى، فهو جديد
        if (!last.equals(proposal.content)) {
            // تحديث آخر مدخل
            lastInputs.put(proposal.source, proposal.content);
            return 0.8f;
        }
        return 0.2f;
    }
    
    private float computeEmotionalRelevance(GlobalWorkspace.WorkspaceProposal proposal, EmotionalState emotion) {
        // هذا يتطلب ربط المحتوى بالعواطف، يمكن تطويره لاحقاً
        // حالياً نعيد قيمة افتراضية
        return 0.5f;
    }
    
    private float computeGoalRelevance(GlobalWorkspace.WorkspaceProposal proposal, String dominantDesire) {
        // إذا كان المصدر هو "desire" نفسه، فله أهمية عالية
        if ("desire".equals(proposal.source)) return 1.0f;
        // يمكن ربط المحتوى بالرغبة لاحقاً
        return 0.5f;
    }
    
    public Map<String, Float> getAttentionWeights() {
        return attentionWeights;
    }
}

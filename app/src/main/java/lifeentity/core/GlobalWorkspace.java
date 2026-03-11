package com.lifeentity.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * مساحة العمل العالمية (Global Workspace)
 * 
 * الفكرة: كل الأنظمة (الحواس، الذاكرة، الرغبات، الخيال) ترسل "مقترحات" إلى هنا.
 * ثم يختار نظام الانتباه (Attention) أهم مقترح ليصبح محتوى الوعي الحالي.
 * هذا يحاكي نظرية Global Workspace للوعي.
 */
public class GlobalWorkspace {
    
    // قائمة المقترحات الواردة من الأنظمة المختلفة
    private ConcurrentLinkedQueue<WorkspaceProposal> proposals = new ConcurrentLinkedQueue<>();
    
    // المحتوى الحالي في مساحة العمل (ما هو واعٍ الآن)
    private WorkspaceContent currentContent;
    
    // الحد الأقصى لعدد المقترحات التي نحتفظ بها
    private static final int MAX_PROPOSALS = 20;
    
    // مستمع للتغييرات في المحتوى الواعي
    public interface WorkspaceListener {
        void onContentChanged(WorkspaceContent newContent);
    }
    
    private List<WorkspaceListener> listeners = new ArrayList<>();
    
    /**
     * مقترح من أحد الأنظمة للمنافسة على الدخول إلى الوعي
     */
    public static class WorkspaceProposal {
        public final String source;      // "perception", "memory", "desire", "imagination", إلخ
        public final Object content;      // المحتوى المقترح (يمكن أن يكون أي شيء)
        public final float priority;      // أولية مبدئية (يحسبها المصدر)
        public final long timestamp;      // وقت الاقتراح
        
        public WorkspaceProposal(String source, Object content, float priority) {
            this.source = source;
            this.content = content;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * المحتوى الواعي الحالي
     */
    public static class WorkspaceContent {
        public final String source;       // المصدر الفائز
        public final Object content;       // المحتوى
        public final float attentionScore; // درجة الانتباه النهائية
        public final long timestamp;       // وقت الدخول إلى الوعي
        
        public WorkspaceContent(String source, Object content, float attentionScore) {
            this.source = source;
            this.content = content;
            this.attentionScore = attentionScore;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return "WorkspaceContent{source='" + source + "', content=" + content + ", score=" + attentionScore + "}";
        }
    }
    
    /**
     * إضافة مقترح جديد من أحد الأنظمة
     */
    public void addProposal(WorkspaceProposal proposal) {
        proposals.offer(proposal);
        // الحفاظ على حجم معقول
        while (proposals.size() > MAX_PROPOSALS) {
            proposals.poll();
        }
    }
    
    /**
     * تحديث مساحة العمل: اختيار أفضل مقترح ليصبح المحتوى الواعي
     * @param attentionScores خريطة تحتوي على أوزان الانتباه لكل مصدر (يمكن تحديثها ديناميكياً)
     */
    public void updateWorkspace(Map<String, Float> attentionScores) {
        if (proposals.isEmpty()) return;
        
        WorkspaceProposal best = null;
        float bestScore = -Float.MAX_VALUE;
        
        for (WorkspaceProposal prop : proposals) {
            // حساب النتيجة النهائية = أولية المصدر * وزن الانتباه لذلك المصدر + عامل الحداثة
            float sourceWeight = attentionScores.getOrDefault(prop.source, 1.0f);
            float recency = (System.currentTimeMillis() - prop.timestamp) / 1000.0f; // كلما كان أحدث، كان أفضل (نعكس)
            float recencyFactor = 1.0f / (1.0f + recency); // 1 للأحدث، يتناقص مع الزمن
            
            float totalScore = prop.priority * sourceWeight * recencyFactor;
            
            if (totalScore > bestScore) {
                bestScore = totalScore;
                best = prop;
            }
        }
        
        if (best != null) {
            // إزالة هذا المقترح من القائمة (لأنه دخل الوعي)
            proposals.remove(best);
            
            // إنشاء محتوى واعٍ جديد
            WorkspaceContent newContent = new WorkspaceContent(best.source, best.content, bestScore);
            
            // إذا تغير المحتوى، نبلغ المستمعين
            if (currentContent == null || !currentContent.content.equals(newContent.content)) {
                currentContent = newContent;
                notifyListeners(newContent);
            }
        }
    }
    
    /**
     * الحصول على المحتوى الواعي الحالي
     */
    public WorkspaceContent getCurrentContent() {
        return currentContent;
    }
    
    /**
     * إضافة مستمع للتغييرات في المحتوى الواعي
     */
    public void addListener(WorkspaceListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners(WorkspaceContent newContent) {
        for (WorkspaceListener l : listeners) {
            l.onContentChanged(newContent);
        }
    }
    
    /**
     * تنظيف المقترحات القديمة جداً (أكثر من 10 ثوانٍ)
     */
    public void cleanOldProposals() {
        long now = System.currentTimeMillis();
        proposals.removeIf(p -> (now - p.timestamp) > 10000);
    }
}

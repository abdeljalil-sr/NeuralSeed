package com.lifeentity.imagination;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.MotionEvent;

import com.lifeentity.core.ConsciousnessCore;
import com.lifeentity.core.EmotionalState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * لوحة مشتركة - منصة عرض للتعبير الحر للوعي.
 * الكائن هو الفنان، وهذه اللوحة مجرد وسيلة لعرض إبداعه.
 * لا تحتوي على أي منطق لتوليد الأشكال أو الألوان؛ الوعي هو من يقرر كل شيء.
 */
public class SharedCanvas {
    
    private Bitmap canvasBitmap;
    private Canvas canvas;
    private Paint paint;
    private Random random;
    
    // عناصر اللوحة الحالية
    private CopyOnWriteArrayList<CanvasElement> elements;
    
    // إعدادات العرض
    private int width, height;
    private float scaleX = 1f, scaleY = 1f;
    
    // الوعي المرتبط (للقراءة فقط)
    private ConsciousnessCore connectedMind;
    
    // مستمع لأحداث التفاعل (يستخدمه LifeActivity)
    private OnCanvasInteraction listener;
    
    public interface OnCanvasInteraction {
        void onObjectCreated(String id, String concept, float x, float y);
        void onObjectSelected(String id, String concept);
        void onObjectMoved(String id, float x, float y);
        void onGestureDrawn(String gesture, float x, float y);
        void onCanvasQuestion(String question);
    }
    
    public SharedCanvas(int width, int height) {
        this.width = width;
        this.height = height;
        initializeCanvas();
        this.elements = new CopyOnWriteArrayList<>();
        this.random = new Random();
    }
    
    private void initializeCanvas() {
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(canvasBitmap);
        canvas.drawColor(Color.BLACK);
        
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
    }
    
    public void setListener(OnCanvasInteraction listener) {
        this.listener = listener;
    }
    
    public void setBackground(Bitmap background) {
        if (background == null) return;
        canvas.drawBitmap(background, 0, 0, null);
    }
    
    /**
     * الوعي يطلب رسم عنصر جديد. يجب أن يزودنا بكل خصائصه.
     * هذه الدالة لا تولد أي شيء، فقط ترسم ما يطلبه الوعي.
     */
    public String createElement(float x, float y, float size, int color, int alpha, Path path, String concept) {
        CanvasElement element = new CanvasElement();
        element.id = generateElementId();
        element.x = x;
        element.y = y;
        element.baseSize = size;
        element.color = color;
        element.alpha = alpha;
        element.path = path;
        element.concept = concept;
        element.creationTime = System.currentTimeMillis();
        
        elements.add(element);
        renderElement(element);
        
        if (listener != null) {
            listener.onObjectCreated(element.id, concept, x / width, y / height);
        }
        
        return element.id;
    }
    
    /**
     * الوعي يعدل عنصراً موجوداً
     */
    public void updateElement(String id, float x, float y, float size, int color, int alpha, Path path) {
        for (CanvasElement element : elements) {
            if (element.id.equals(id)) {
                if (x >= 0) element.x = x;
                if (y >= 0) element.y = y;
                if (size > 0) element.baseSize = size;
                if (color != 0) element.color = color;
                if (alpha >= 0) element.alpha = alpha;
                if (path != null) element.path = path;
                
                redrawCanvas();
                
                if (listener != null) {
                    listener.onObjectMoved(id, x, y);
                }
                break;
            }
        }
    }
    
    /**
     * الوعي يمسح عنصراً
     */
    public void removeElement(String id) {
        Iterator<CanvasElement> it = elements.iterator();
        while (it.hasNext()) {
            CanvasElement element = it.next();
            if (element.id.equals(id)) {
                animateFadeOut(element, () -> {
                    elements.remove(element);
                    redrawCanvas();
                });
                break;
            }
        }
    }
    
    /**
     * الوعي يمسح كل شيء
     */
    public void clearAll() {
        for (CanvasElement element : elements) {
            animateFadeOut(element, null);
        }
        elements.clear();
        
        ValueAnimator clearAnim = ValueAnimator.ofFloat(1, 0);
        clearAnim.setDuration(2000);
        clearAnim.addUpdateListener(anim -> {
            float alpha = (float) anim.getAnimatedValue();
            Paint fadePaint = new Paint();
            fadePaint.setColor(Color.BLACK);
            fadePaint.setAlpha((int) ((1 - alpha) * 255));
            canvas.drawRect(0, 0, width, height, fadePaint);
        });
        clearAnim.start();
    }
    
    // ==================== تفاعل المستخدم ====================
    
    public boolean onTouch(MotionEvent event) {
        float screenX = event.getX();
        float screenY = event.getY();
        float canvasX = screenX * scaleX;
        float canvasY = screenY * scaleY;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (listener != null) {
                    listener.onGestureDrawn("touch", canvasX / width, canvasY / height);
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (listener != null) {
                    listener.onGestureDrawn("drag", canvasX / width, canvasY / height);
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                if (listener != null && event.getEventTime() - event.getDownTime() < 200) {
                    listener.onCanvasQuestion("ما هذا؟");
                }
                return true;
        }
        return false;
    }
    
    // ==================== العرض والرسم ====================
    
    private void renderElement(CanvasElement element) {
        paint.setColor(element.color);
        paint.setAlpha(element.alpha);
        
        // تأثير مضيء (إذا أراد الوعي ذلك، يمكن التحكم به عبر alpha)
        if (element.alpha > 200) {
            RadialGradient glow = new RadialGradient(
                element.x, element.y, element.baseSize * 1.5f,
                element.color, Color.TRANSPARENT, Shader.TileMode.CLAMP
            );
            Paint glowPaint = new Paint();
            glowPaint.setShader(glow);
            canvas.drawCircle(element.x, element.y, element.baseSize * 1.5f, glowPaint);
        }
        
        // الرسم الرئيسي
        if (element.path != null) {
            canvas.drawPath(element.path, paint);
        } else {
            // إذا لم يكن هناك مسار، ارسم دائرة افتراضية
            canvas.drawCircle(element.x, element.y, element.baseSize, paint);
        }
        
        // إضافة نص المفهوم إذا كان موجوداً
        if (element.concept != null && !element.concept.isEmpty()) {
            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setAlpha(element.alpha / 2);
            textPaint.setTextSize(20);
            canvas.drawText(element.concept, element.x - 30, element.y + element.baseSize + 25, textPaint);
        }
    }
    
    private void redrawCanvas() {
        canvas.drawColor(Color.BLACK);
        for (CanvasElement element : elements) {
            renderElement(element);
        }
    }
    
    private void animateFadeOut(CanvasElement element, Runnable onComplete) {
        ValueAnimator fade = ValueAnimator.ofInt(element.alpha, 0);
        fade.setDuration(1500);
        fade.addUpdateListener(a -> {
            element.alpha = (int) a.getAnimatedValue();
            redrawCanvas();
        });
        fade.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onComplete != null) onComplete.run();
            }
        });
        fade.start();
    }
    
    // ==================== أدوات مساعدة ====================
    
    private String generateElementId() {
        return "element_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
    }
    
    public void setViewSize(int viewWidth, int viewHeight) {
        this.scaleX = (float) width / viewWidth;
        this.scaleY = (float) height / viewHeight;
    }
    
    public Bitmap getBitmap() {
        return canvasBitmap;
    }
    
    public void connectToMind(ConsciousnessCore mind) {
        this.connectedMind = mind;
    }
    
    /**
     * البحث عن أقرب مفهوم لإحداثيات الشاشة (يستخدم للاستعلام)
     */
    public String findNearestConcept(float screenX, float screenY) {
        float canvasX = screenX * scaleX;
        float canvasY = screenY * scaleY;
        CanvasElement nearest = null;
        float minDist = Float.MAX_VALUE;
        for (CanvasElement element : elements) {
            float dx = canvasX - element.x;
            float dy = canvasY - element.y;
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            if (dist < minDist) {
                minDist = dist;
                nearest = element;
            }
        }
        return nearest != null ? nearest.concept : "الفراغ";
    }
    
    // ==================== الفئات الداخلية ====================
    
    public static class CanvasElement {
        public String id;
        public String concept;
        public float x, y;
        public float baseSize;
        public int color;
        public int alpha;
        public Path path;
        public long creationTime;
        
        // للرسوم المتحركة المستقبلية
        public float currentScale = 1f;
        public float rotation = 0f;
    }
}

package com.lifeentity.imagination;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * لوحة مشتركة بين الكائن والمستخدم.
 * يمكن للكائن أن يتخيل كائنات (ImaginedObject) ويرسمها، ويمكن للمستخدم التفاعل عبر اللمس.
 * الخلفية يمكن تعيينها من ImaginationEngine (صورة مولدة من الذاكرة).
 */
public class SharedCanvas {
    
    private Bitmap bitmap;                 // اللوحة النهائية (الخلفية + الكائنات)
    private Canvas canvas;                  // لرسم اللوحة
    private Paint paint;                     // للرسم
    
    private Bitmap backgroundBitmap;        // صورة الخلفية (يمكن أن تأتي من ImaginationEngine)
    private Canvas backgroundCanvas;        // لرسم الخلفية (اختياري)
    
    private List<ImaginedObject> objects;   // كائنات متخيلة
    private ImaginedObject selected;         // الكائن المحدد حالياً
    private OnCanvasInteraction listener;    // مستمع للتفاعلات
    
    private float lastX, lastY;              // آخر إحداثيات لمس (بالـ canvas pixels)
    private long touchStartTime;              // وقت بدء اللمس
    
    // تحويل إحداثيات الشاشة إلى إحداثيات اللوحة
    private int viewWidth, viewHeight;
    private float scaleX = 1f, scaleY = 1f;
    
    // عشوائية موجهة (لرسم الكائنات)
    private Random random;
    // داخل SharedCanvas.java، أضف الدالة التالية:
    public String findNearestConcept(float screenX, float screenY) {
    float canvasX = screenX * scaleX;
    float canvasY = screenY * scaleY;
    ImaginedObject nearest = null;
    float minDist = Float.MAX_VALUE;
    for (ImaginedObject obj : objects) {
        float dx = canvasX - obj.x;
        float dy = canvasY - obj.y;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);
        if (dist < minDist) {
            minDist = dist;
            nearest = obj;
        }
    }
    return nearest != null ? nearest.concept : "الفراغ";
        }
    public interface OnCanvasInteraction {
        void onObjectCreated(String concept, float x, float y);
        void onObjectSelected(String id, String concept);
        void onObjectMoved(String id, float x, float y);
        void onGestureDrawn(String gesture, float x, float y);
        void onCanvasQuestion(String question);
    }
    
    public SharedCanvas(int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        clear();
        
        // إعداد الفرشاة
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(8);
        
        objects = new ArrayList<>();
        random = new Random();
        
        backgroundBitmap = null;
    }
    
    /**
     * تعيين صورة الخلفية (تأتي من ImaginationEngine مثلاً)
     */
    public void setBackground(Bitmap bg) {
        if (bg == null) return;
        // تغيير حجم الصورة لتناسب اللوحة إذا لزم الأمر
        backgroundBitmap = Bitmap.createScaledBitmap(bg, bitmap.getWidth(), bitmap.getHeight(), true);
        redraw(); // إعادة رسم اللوحة بالخلفية الجديدة
    }
    
    /**
     * مسح الخلفية وإعادة تعيينها للون الأسود
     */
    public void clearBackground() {
        backgroundBitmap = null;
        redraw();
    }
    
    /**
     * مسح اللوحة بالكامل (الخلفية والكائنات)
     */
    public void clear() {
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
    }
    
    /**
     * تحديث أبعاد العرض (لتطبيق scale)
     */
    public void setViewSize(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;
        this.scaleX = (float) bitmap.getWidth() / width;
        this.scaleY = (float) bitmap.getHeight() / height;
    }
    
    /**
     * إضافة كائن متخيل (يناديها ConsciousnessCore عند وجود دافع تعبيري)
     * @param concept مفهوم الكائن (يمكن أن يكون عشوائياً أو من الذاكرة)
     * @param x إحداثي x (0..1 نسبة إلى عرض اللوحة)
     * @param y إحداثي y
     * @param intensity شدة الدافع (تؤثر على الحجم والشفافية)
     * @param hue لون مقترح (ثلاثي RGB)
     */
    public void imagineObject(String concept, float x, float y, float intensity, int[] hue) {
        if (concept == null) concept = "شيء";
        
        ImaginedObject obj = new ImaginedObject();
        obj.id = "obj_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
        obj.concept = concept;
        obj.x = x * bitmap.getWidth();
        obj.y = y * bitmap.getHeight();
        obj.size = 50 + intensity * 100; // الحجم يتناسب مع الشدة
        if (hue == null || hue.length < 3) {
            obj.colors = new int[]{
                100 + random.nextInt(155),
                100 + random.nextInt(155),
                100 + random.nextInt(155)
            };
        } else {
            obj.colors = hue.clone();
        }
        
        // توليد مسار عشوائي فريد لكل كائن بناءً على مفهومه
        obj.path = generateRandomPath(obj.x, obj.y, obj.size, concept);
        
        objects.add(obj);
        redraw();
        
        if (listener != null) {
            listener.onObjectCreated(concept, obj.x / bitmap.getWidth(), obj.y / bitmap.getHeight());
        }
    }
    
    /**
     * توليد مسار عشوائي (شكل عضوي) يعتمد على concept
     */
    private Path generateRandomPath(float cx, float cy, float size, String seed) {
        random.setSeed(seed.hashCode() + System.currentTimeMillis() % 10000);
        Path path = new Path();
        
        // عدد النقاط: بين 5 و 12
        int numPoints = 5 + random.nextInt(8);
        float[] angles = new float[numPoints];
        float[] radii = new float[numPoints];
        
        // توليد زوايا متباعدة بشكل متساوٍ مع بعض العشوائية
        for (int i = 0; i < numPoints; i++) {
            angles[i] = (float) (2 * Math.PI * i / numPoints + random.nextFloat() * 0.5 - 0.25);
            radii[i] = size * (0.7f + random.nextFloat() * 0.6f);
        }
        
        // بناء المسار (نقاط متصلة)
        float startX = cx + (float) Math.cos(angles[0]) * radii[0];
        float startY = cy + (float) Math.sin(angles[0]) * radii[0];
        path.moveTo(startX, startY);
        
        for (int i = 1; i < numPoints; i++) {
            float x = cx + (float) Math.cos(angles[i]) * radii[i];
            float y = cy + (float) Math.sin(angles[i]) * radii[i];
            path.lineTo(x, y);
        }
        path.close();
        
        // إضافة بعض التموجات (Bezier curves) لجعل الشكل أقل خشونة
        // (يمكن تطويرها لاحقاً)
        
        return path;
    }
    
    /**
     * معالجة حدث اللمس (من LifeActivity)
     */
    public boolean onTouch(MotionEvent event) {
        if (viewWidth == 0 || viewHeight == 0) return false;
        
        float rawX = event.getX();
        float rawY = event.getY();
        float canvasX = rawX * scaleX;
        float canvasY = rawY * scaleY;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartTime = System.currentTimeMillis();
                lastX = canvasX;
                lastY = canvasY;
                selected = findObjectAt(canvasX, canvasY);
                
                if (selected != null && listener != null) {
                    listener.onObjectSelected(selected.id, selected.concept);
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (selected != null) {
                    // تحريك الكائن المحدد
                    selected.x += (canvasX - lastX);
                    selected.y += (canvasY - lastY);
                    // إعادة توليد المسار مع الحفاظ على الشكل (نفس البذرة)
                    selected.path = generateRandomPath(selected.x, selected.y, selected.size, selected.concept);
                    redraw();
                    if (listener != null) {
                        listener.onObjectMoved(selected.id, selected.x / bitmap.getWidth(), selected.y / bitmap.getHeight());
                    }
                } else {
                    // رسم خط (المستخدم يرسم)
                    paint.setColor(Color.argb(200, 100, 200, 255)); // لون أزرق فاتح
                    paint.setStrokeWidth(8);
                    canvas.drawLine(lastX, lastY, canvasX, canvasY, paint);
                    detectGesture(canvasX - lastX, canvasY - lastY);
                }
                lastX = canvasX;
                lastY = canvasY;
                return true;
                
            case MotionEvent.ACTION_UP:
                long duration = System.currentTimeMillis() - touchStartTime;
                if (duration < 200 && selected == null && listener != null) {
                    listener.onCanvasQuestion("ما هذا؟");
                }
                selected = null;
                return true;
        }
        return false;
    }
    
    /**
     * الكشف عن إيماءة بسيطة (سحب، رسم)
     */
    private void detectGesture(float dx, float dy) {
        if (Math.abs(dx) < 10 && Math.abs(dy) < 10) return;
        
        String gesture;
        if (Math.abs(dx) > Math.abs(dy) * 3) {
            gesture = dx > 0 ? "swipe_right" : "swipe_left";
        } else if (Math.abs(dy) > Math.abs(dx) * 3) {
            gesture = dy > 0 ? "swipe_down" : "swipe_up";
        } else {
            gesture = "draw";
        }
        
        if (listener != null) {
            listener.onGestureDrawn(gesture, lastX / bitmap.getWidth(), lastY / bitmap.getHeight());
        }
    }
    
    /**
     * البحث عن كائن تحت إحداثيات معينة
     */
    private ImaginedObject findObjectAt(float x, float y) {
        for (int i = objects.size() - 1; i >= 0; i--) {
            ImaginedObject obj = objects.get(i);
            // تقريب: نعتبر الكائن دائرة قطره size
            float dx = x - obj.x;
            float dy = y - obj.y;
            if (Math.sqrt(dx*dx + dy*dy) < obj.size) {
                return obj;
            }
        }
        return null;
    }
    
    /**
     * إعادة رسم اللوحة بالكامل (الخلفية + الكائنات)
     */
    private void redraw() {
        // مسح اللوحة
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
        
        // رسم الخلفية إن وجدت
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }
        
        // رسم الكائنات
        for (ImaginedObject obj : objects) {
            drawObject(obj);
        }
    }
    
    /**
     * رسم كائن متخيل على اللوحة
     */
    private void drawObject(ImaginedObject obj) {
        paint.setColor(Color.rgb(
            Math.min(255, obj.colors[0]),
            Math.min(255, obj.colors[1]),
            Math.min(255, obj.colors[2])
        ));
        paint.setAlpha(200);
        paint.setStyle(Paint.Style.FILL);
        
        // رسم المسار الخاص بالكائن
        if (obj.path != null) {
            canvas.drawPath(obj.path, paint);
        }
        
        // رسم إطار خفيف حول الكائن إذا كان مختاراً
        if (obj == selected) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.WHITE);
            paint.setAlpha(255);
            if (obj.path != null) {
                canvas.drawPath(obj.path, paint);
            }
        }
        
        // رسم التسمية (concept)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        canvas.drawText(obj.concept, obj.x - obj.size, obj.y + obj.size + 30, paint);
    }
    
    public Bitmap getBitmap() {
        return bitmap;
    }
    
    public void setListener(OnCanvasInteraction l) {
        this.listener = l;
    }
    
    /**
     * الكائن المتخيل (تمثيل داخلي)
     */
    public static class ImaginedObject {
        public String id;
        public String concept;
        public float x, y;          // إحداثيات المركز بالبكسل
        public float size;           // نصف قطر تقريبي
        public int[] colors;         // RGB
        public Path path;            // المسار العضوي الفريد
    }
}

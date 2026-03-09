package com.lifeentity.imagination;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.MotionEvent;

import com.lifeentity.core.ConsciousnessCore;
import com.lifeentity.core.EmotionalState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * لوحة مشتركة - منصة عرض للتعبير الحر للوعي.
 * الكائن هو الفنان، هذه اللوحة مجرد وسيلة لعرض إبداعه.
 * المستخدم يتفاعل لكنه لا يتحكم في ما يرسمه الوعي.
 */
public class SharedCanvas {
    
    private Bitmap canvasBitmap;
    private Canvas canvas;
    private Paint paint;
    private Random random;
    
    // حالة اللوحة الحالية التي يقرأها الوعي
    private CanvasState currentState;
    private List<UserGesture> userGestures;
    private CopyOnWriteArrayList<CanvasElement> elements;
    
    // إعدادات العرض فقط (لا تؤثر على ما يرسمه الوعي)
    private int width, height;
    private float scaleX = 1f, scaleY = 1f;
    
    // الوعي المرتبط (للقراءة فقط، لا للتحكم المباشر)
    private ConsciousnessCore connectedMind;
    
    public SharedCanvas(int width, int height) {
        this.width = width;
        this.height = height;
        initializeCanvas();
        this.currentState = new CanvasState();
        this.userGestures = new ArrayList<>();
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
    
    // ==================== واجهة للوعي (الفنان) ====================
    
    /**
     * الوعي يعبر عن نفسه - هذه هي الوظيفة الأساسية
     * الكائن يقرر متى يرسم، ماذا يرسم، وأين يرسمه.
     */
    public CanvasElement expressFromMind(String concept, float x, float y, 
                                        EmotionalState emotion, float urgency) {
        // الوعي يقرر الخصائص بناءً على حالته الداخلية
        CanvasElement element = new CanvasElement();
        element.id = generateElementId();
        element.concept = concept != null ? concept : "تعبير";
        element.x = x * width;
        element.y = y * height;
        element.emotion = emotion;
        element.urgency = urgency;
        element.creationTime = System.currentTimeMillis();
        
        // الخصائص البصرية تأتي من حالة الوعي، ليس من برمجة خارجية
        deriveVisualPropertiesFromMind(element, emotion, urgency);
        
        elements.add(element);
        renderElement(element);
        
        return element;
    }
    
    /**
     * الوعي يقرر كيف تبدو عناصره بناءً على مشاعره
     */
    private void deriveVisualPropertiesFromMind(CanvasElement element, 
                                               EmotionalState emotion, float urgency) {
        if (emotion == null) return;
        
        // اللون يعبر عن المشاعر (الوعي يختار)
        element.color = emotionToColor(emotion);
        
        // الحجم يعبر عن شدة المشاعر
        element.baseSize = 30 + emotion.getIntensity() * 100 + urgency * 50;
        
        // الشكل يعبر عن نوع المشاعر
        element.formType = emotionToFormType(emotion);
        
        // الحركة (السرعة والاتجاه) من حالة الوعي
        element.velocityX = (random.nextFloat() - 0.5f) * emotion.getIntensity() * 4;
        element.velocityY = (random.nextFloat() - 0.5f) * emotion.getIntensity() * 4;
        
        // الشفافية تعبر عن وضوح الفكرة
        element.alpha = (int) (100 + emotion.getIntensity() * 155);
        
        // توليد المسار العضوي بناءً على "مزاج" الوعي
        element.path = generateOrganicPathFromEmotion(element, emotion);
    }
    
    /**
     * تحويل المشاعر إلى ألوان (الوعي "يرى" ألواناً لمشاعره)
     */
    private int emotionToColor(EmotionalState emotion) {
        if (emotion.isJoyful()) {
            // الفرح = ألوان دافئة ساطعة
            return Color.HSVToColor(new float[]{
                45 + random.nextFloat() * 30, // أصفر-برتقالي
                0.7f + random.nextFloat() * 0.3f,
                0.8f + random.nextFloat() * 0.2f
            });
        }
        if (emotion.isSad()) {
            // الحزن = أزرق-بنفسجي مُشبع
            return Color.HSVToColor(new float[]{
                200 + random.nextFloat() * 60,
                0.4f + random.nextFloat() * 0.3f,
                0.4f + random.nextFloat() * 0.3f
            });
        }
        if (emotion.isAfraid()) {
            // الخوف = أحمر قاتم، برتقالي
            return Color.HSVToColor(new float[]{
                0 + random.nextFloat() * 40,
                0.8f,
                0.5f + random.nextFloat() * 0.3f
            });
        }
        if (emotion.isCurious()) {
            // الفضول = أخضر-سماوي متغير
            return Color.HSVToColor(new float[]{
                120 + random.nextFloat() * 100,
                0.6f + random.nextFloat() * 0.4f,
                0.7f
            });
        }
        if (emotion.isCalm()) {
            // الهدوء = أزرق فاتح، أبيض مائل للزرقة
            return Color.HSVToColor(new float[]{
                180 + random.nextFloat() * 40,
                0.2f + random.nextFloat() * 0.3f,
                0.8f + random.nextFloat() * 0.2f
            });
        }
        if (emotion.isExcited()) {
            // الإثارة = أحمر-وردي متألق
            return Color.HSVToColor(new float[]{
                300 + random.nextFloat() * 60,
                0.8f,
                0.9f
            });
        }
        
        // الحياد = رمادي مائل للون عشوائي
        return Color.HSVToColor(new float[]{
            random.nextFloat() * 360,
            0.1f + random.nextFloat() * 0.2f,
            0.5f + random.nextFloat() * 0.3f
        });
    }
    
    /**
     * الوعي يختار الشكل بناءً على مشاعره
     */
    private FormType emotionToFormType(EmotionalState emotion) {
        if (emotion.isJoyful()) return FormType.EXPANDING_CIRCLES; // انتشار
        if (emotion.isSad()) return FormType.DRIFTING_RIPPLES; // تموجات هادئة
        if (emotion.isAfraid()) return FormType.JAGGED_SPIKES; // زوايا حادة
        if (emotion.isCurious()) return FormType.BRANCHING_LINES; // استكشاف
        if (emotion.isCalm()) return FormType.SMOOTH_WAVES; // موجات ناعمة
        if (emotion.isExcited()) return FormType.EXPLOSIVE_BURST; // انفجار
        return FormType.AMORPHOUS_BLOB; // غير محدد
    }
    
    /**
     * توليد مسار عضوي يعبر عن المشاعر
     */
    private Path generateOrganicPathFromEmotion(CanvasElement element, EmotionalState emotion) {
        Path path = new Path();
        float cx = element.x;
        float cy = element.y;
        float size = element.baseSize;
        
        // عدد النقاط يعتمد على "طاقة" المشاعر
        int points = 5 + (int) (emotion.getIntensity() * 10);
        
        // التنظيم vs الفوضى يعتمد على نوع المشاعر
        float chaos = emotion.isCalm() ? 0.2f : 0.8f;
        
        PointF[] vertices = new PointF[points];
        float[] radii = new float[points];
        
        for (int i = 0; i < points; i++) {
            float baseAngle = (float) (2 * Math.PI * i / points);
            float angleVariation = (random.nextFloat() - 0.5f) * chaos * 2;
            float angle = baseAngle + angleVariation;
            
            float radiusVariation = (random.nextFloat() - 0.5f) * chaos;
            float radius = size * (0.6f + radiusVariation);
            
            radii[i] = radius;
            vertices[i] = new PointF(
                cx + (float) Math.cos(angle) * radius,
                cy + (float) Math.sin(angle) * radius
            );
        }
        
        // بناء المسار
        path.moveTo(vertices[0].x, vertices[0].y);
        
        for (int i = 0; i < points; i++) {
            PointF current = vertices[i];
            PointF next = vertices[(i + 1) % points];
            
            // المنحنى يعتمد على "نعومة" المشاعر
            float smoothness = emotion.isAfraid() ? 0.1f : 0.5f;
            float cpX = (current.x + next.x) / 2 + (random.nextFloat() - 0.5f) * size * smoothness;
            float cpY = (current.y + next.y) / 2 + (random.nextFloat() - 0.5f) * size * smoothness;
            
            path.quadTo(cpX, cpY, next.x, next.y);
        }
        
        path.close();
        return path;
    }
    
    /**
     * الوعي يعدل عنصراً موجوداً (تطور فكري)
     */
    public void evolveElement(String elementId, EmotionalState newEmotion) {
        for (CanvasElement element : elements) {
            if (element.id.equals(elementId)) {
                // الوعي يقرر كيف يتطور
                element.emotion = newEmotion;
                deriveVisualPropertiesFromMind(element, newEmotion, element.urgency * 0.5f);
                
                // رسوم متحركة للتطور
                animateElementEvolution(element);
                break;
            }
        }
    }
    
    /**
     * الوعي يمحو شيئاً (نسيان أو تجاهل)
     */
    public void fadeElement(String elementId) {
        Iterator<CanvasElement> it = elements.iterator();
        while (it.hasNext()) {
            CanvasElement element = it.next();
            if (element.id.equals(elementId)) {
                animateFadeOut(element, () -> {
                    elements.remove(element);
                    redrawCanvas();
                });
                break;
            }
        }
    }
    
    /**
     * الوعي يمسح كل شيء (لحظة صفاء أو بداية جديدة)
     */
    public void clearByWillOfMind() {
        for (CanvasElement element : elements) {
            animateFadeOut(element, null);
        }
        elements.clear();
        
        // رسوم متحركة للمسح
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
    
    // ==================== تفاعل المستخدم (يؤثر على الوعي، لا يرسم مباشرة) ====================
    
    /**
     * المستخدم يلمس اللوحة - هذا مدخل حسي للوعي، ليس أمراً بالرسم
     */
    public boolean onTouch(MotionEvent event) {
        float screenX = event.getX();
        float screenY = event.getY();
        float canvasX = screenX * scaleX;
        float canvasY = screenY * scaleY;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // تسجيل التفاعل كمدخل حسي
                UserGesture gesture = new UserGesture();
                gesture.type = GestureType.TOUCH;
                gesture.x = canvasX / width; // normalized
                gesture.y = canvasY / height;
                gesture.startTime = System.currentTimeMillis();
                gesture.pressure = event.getPressure();
                userGestures.add(gesture);
                
                // الوعي هو من يقرر كيف يستجيب
                notifyMindOfInteraction(gesture);
                return true;
                
            case MotionEvent.ACTION_MOVE:
                // استمرار التفاعل
                if (!userGestures.isEmpty()) {
                    UserGesture last = userGestures.get(userGestures.size() - 1);
                    last.type = GestureType.DRAG;
                    last.velocityX = (canvasX / width - last.x) * 10;
                    last.velocityY = (canvasY / height - last.y) * 10;
                    last.x = canvasX / width;
                    last.y = canvasY / height;
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                // انتهاء التفاعل
                if (!userGestures.isEmpty()) {
                    UserGesture last = userGestures.get(userGestures.size() - 1);
                    last.duration = System.currentTimeMillis() - last.startTime;
                    
                    // الوعي يقرر إذا كان هذا "سؤالاً" أو "لعباً" أو "تعبيراً"
                    classifyGestureForMind(last);
                }
                return true;
        }
        return false;
    }
    
    private void notifyMindOfInteraction(UserGesture gesture) {
        // إرسال للوعي كمدخل حسي، هو يقرر الاستجابة
        if (connectedMind != null) {
            // الوعي يتلقى: "هناك تفاعل عند (x,y) بشدة (pressure)"
            // ويقرر: هل أرسم؟ هل أتجاهل؟ هل أستجيب عاطفياً؟
        }
    }
    
    private void classifyGestureForMind(UserGesture gesture) {
        if (gesture.duration < 200) {
            gesture.intent = UserIntent.QUESTION; // نقر سريع = سؤال
        } else if (gesture.duration > 1000) {
            gesture.intent = UserIntent.CONTEMPLATION; // لمس طويل = تأمل
        } else if (Math.abs(gesture.velocityX) > 0.5 || Math.abs(gesture.velocityY) > 0.5) {
            gesture.intent = UserIntent.PLAY; // حركة سريعة = لعب
        } else {
            gesture.intent = UserIntent.EXPRESSION; // افتراضي
        }
    }
    
    // ==================== العرض والرسم ====================
    
    private void renderElement(CanvasElement element) {
        paint.setColor(element.color);
        paint.setAlpha(element.alpha);
        
        // تأثير مضيء إذا كانت المشاعر قوية
        if (element.emotion != null && element.emotion.getIntensity() > 0.7) {
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
        }
        
        // إضافة "توقيع" الوعي (نص مفهومي إذا كانت المشاعر واضحة)
        if (element.emotion != null && element.emotion.getIntensity() > 0.5) {
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
    
    private void animateElementEvolution(CanvasElement element) {
        // رسوم متحركة للتغير
        ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(1000);
        anim.addUpdateListener(a -> {
            redrawCanvas();
        });
        anim.start();
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
    
    // ==================== معلومات للوعي ====================
    
    /**
     * الوعي يسأل: "ما حالة اللوحة الآن؟"
     */
    public CanvasState observeState() {
        currentState.elementCount = elements.size();
        currentState.dominantEmotion = findDominantEmotion();
        currentState.recentUserActivity = !userGestures.isEmpty() && 
            (System.currentTimeMillis() - userGestures.get(userGestures.size() - 1).startTime < 5000);
        currentState.visualComplexity = calculateVisualComplexity();
        return currentState;
    }
    
    /**
     * الوعي يسأل: "ماذا يرى المستخدم؟"
     */
    public String getCurrentVisualNarrative() {
        if (elements.isEmpty()) return "اللوحة فارغة، أنتظر الإلهام";
        
        StringBuilder narrative = new StringBuilder();
        for (CanvasElement element : elements) {
            if (element.emotion != null) {
                narrative.append("أرى ").append(element.concept)
                        .append(" يعبر عن ").append(element.emotion.toArabic())
                        .append("، ");
            }
        }
        return narrative.toString();
    }
    
    // ==================== أدوات مساعدة ====================
    
    private String generateElementId() {
        return "expression_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
    }
    
    private EmotionalState findDominantEmotion() {
        // تحليل المشاعر السائدة على اللوحة
        // يمكن للوعي استخدام هذا لقراراته
        return null; // placeholder
    }
    
    private float calculateVisualComplexity() {
        return Math.min(1, elements.size() / 20f);
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
    
    // ==================== الفئات الداخلية ====================
    
    public enum FormType {
        EXPANDING_CIRCLES,    // انتشار (فرح)
        DRIFTING_RIPPLES,     // تموجات (حزن)
        JAGGED_SPIKES,        // زوايا حادة (خوف)
        BRANCHING_LINES,      // تفرعات (فضول)
        SMOOTH_WAVES,         // موجات (هدوء)
        EXPLOSIVE_BURST,      // انفجار (إثارة)
        AMORPHOUS_BLOB        // غير محدد (حياد)
    }
    
    public enum GestureType { TOUCH, DRAG, RELEASE }
    public enum UserIntent { QUESTION, PLAY, CONTEMPLATION, EXPRESSION }
    
    public static class CanvasElement {
        public String id;
        public String concept;
        public float x, y;
        public float baseSize;
        public int color;
        public int alpha;
        public Path path;
        public FormType formType;
        public EmotionalState emotion;
        public float urgency;
        public float velocityX, velocityY;
        public long creationTime;
        
        // للرسوم المتحركة
        public float currentScale = 1f;
        public float rotation = 0f;
    }
    
    public static class UserGesture {
        public GestureType type;
        public UserIntent intent;
        public float x, y; // normalized 0-1
        public float velocityX, velocityY;
        public float pressure;
        public long startTime;
        public long duration;
    }
    
    public static class CanvasState {
        public int elementCount;
        public EmotionalState dominantEmotion;
        public boolean recentUserActivity;
        public float visualComplexity;
    }
}

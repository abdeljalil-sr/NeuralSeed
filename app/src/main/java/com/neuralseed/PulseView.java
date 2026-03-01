package com.neuralseed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * PulseView المتقدم - مرآة الوعي البصرية
 * يجمع بين نبض الأنا والتخيل الإبداعي
 */
public class PulseView extends View {
    
    // ===== النبض الأساسي =====
    private Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private NeuralSeed.EgoType currentEgoType = NeuralSeed.EgoType.STABLE;
    private float pulsePhase = 0;
    
    // ===== التخيل الإبداعي =====
    private Paint imaginationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private VisualThought currentThought;
    private List<FloatingShape> floatingShapes = new ArrayList<>();
    private float imaginationPhase = 0;
    private boolean showImagination = false;
    
    // ===== البيانات الداخلية =====
    private static class FloatingShape {
        float x, y, baseX, baseY;
        float size;
        int color;
        String type;
        float speed;
        float phase;
        float chaosInfluence;
        
        FloatingShape(float x, float y, float size, int color, String type) {
            this.baseX = x;
            this.baseY = y;
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
            this.type = type;
            this.speed = 0.02f + (float)Math.random() * 0.03f;
            this.phase = (float)(Math.random() * Math.PI * 2);
            this.chaosInfluence = (float)Math.random();
        }
        
        void update(float globalPhase, float chaosLevel) {
            phase += speed;
            
            // حركة الفوضى
            float chaosX = (float)(Math.sin(phase * 2) * chaosLevel * chaosInfluence * 30);
            float chaosY = (float)(Math.cos(phase * 1.5f) * chaosLevel * chaosInfluence * 30);
            
            // حركة النبض
            float pulse = (float)(Math.sin(globalPhase + phase) * 10);
            
            x = baseX + chaosX + pulse;
            y = baseY + chaosY + pulse;
        }
    }
    
    // ===== البناء =====
    public PulseView(Context context) {
        super(context);
        init();
    }

    public PulseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        basePaint.setStyle(Paint.Style.STROKE);
        basePaint.setStrokeWidth(5);
        
        imaginationPaint.setStyle(Paint.Style.STROKE);
        imaginationPaint.setStrokeWidth(3);
        
        startAnimation();
    }

    // ===== التحكم بالأنا =====
    public void setEgoType(NeuralSeed.EgoType type) {
        currentEgoType = type;
        invalidate();
    }

    // ===== التحكم بالتخيل =====
    public void setVisualThought(VisualThought thought) {
        this.currentThought = thought;
        this.showImagination = (thought != null);
        rebuildShapes();
        invalidate();
    }
    
    public void clearImagination() {
        this.showImagination = false;
        this.currentThought = null;
        this.floatingShapes.clear();
        invalidate();
    }
    
    private void rebuildShapes() {
        floatingShapes.clear();
        if (currentThought == null || currentThought.shapes == null) return;
        
        float width = getWidth();
        float height = getHeight();
        
        if (width == 0 || height == 0) return;
        
        for (ShapeElement elem : currentThought.shapes) {
            FloatingShape shape = new FloatingShape(
                elem.x * width,
                elem.y * height,
                elem.size,
                elem.color,
                elem.type
            );
            shape.speed = elem.animationSpeed * 0.02f;
            floatingShapes.add(shape);
        }
    }

    // ===== الرسومات =====
    private void startAnimation() {
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                pulsePhase += 0.1f;
                imaginationPhase += 0.05f;
                invalidate();
                postOnAnimation(this);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // رسم النبض الأساسي (دائماً)
        drawBasePulse(canvas);
        
        // رسم التخيل إذا كان متاحاً
        if (showImagination && !floatingShapes.isEmpty()) {
            drawImagination(canvas);
        }
    }
    
    private void drawBasePulse(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        int color;
        float speed;

        switch (currentEgoType) {
            case STABLE: color = Color.parseColor("#90EE90"); speed = 0.5f; break;
            case CHAOTIC: color = Color.parseColor("#FF6347"); speed = 2.0f; break;
            case ADAPTIVE: color = Color.parseColor("#FFD700"); speed = 1.0f; break;
            case SURVIVAL: color = Color.parseColor("#FF0000"); speed = 3.0f; break;
            default: color = Color.WHITE; speed = 1.0f;
        }

        basePaint.setColor(color);
        
        // دوائر النبض الأساسية
        for (int i = 0; i < 3; i++) {
            float radius = (float) (50 + i * 30 + Math.sin(pulsePhase * speed + i) * 10);
            int alpha = (int) (150 - i * 40 + Math.sin(pulsePhase * speed) * 50);
            basePaint.setAlpha(Math.max(0, alpha));
            canvas.drawCircle(centerX, centerY, radius, basePaint);
        }
    }
    
    private void drawImagination(Canvas canvas) {
        // تحديث الأشكال
        float chaosLevel = currentThought != null ? currentThought.chaosLevel : 0.5f;
        for (FloatingShape shape : floatingShapes) {
            shape.update(imaginationPhase, chaosLevel);
            drawShape(canvas, shape);
        }
        
        // رسم الروابط
        drawConnections(canvas);
    }
    
    private void drawShape(Canvas canvas, FloatingShape shape) {
        imaginationPaint.setColor(shape.color);
        imaginationPaint.setAlpha(200);
        
        switch (shape.type) {
            case "circle":
                imaginationPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(shape.x, shape.y, shape.size * 0.5f, imaginationPaint);
                // هالة خارجية
                imaginationPaint.setStyle(Paint.Style.STROKE);
                imaginationPaint.setAlpha(100);
                float haloSize = shape.size * (0.7f + (float)Math.sin(shape.phase) * 0.1f);
                canvas.drawCircle(shape.x, shape.y, haloSize, imaginationPaint);
                break;
                
            case "spiral":
                Path spiral = new Path();
                for (float i = 0; i < 15; i += 0.5f) {
                    float angle = i * 0.4f + shape.phase;
                    float r = i * shape.size / 15;
                    float x = shape.x + (float)Math.cos(angle) * r;
                    float y = shape.y + (float)Math.sin(angle) * r;
                    if (i == 0) spiral.moveTo(x, y);
                    else spiral.lineTo(x, y);
                }
                imaginationPaint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(spiral, imaginationPaint);
                break;
                
            case "pulse":
                imaginationPaint.setStyle(Paint.Style.STROKE);
                imaginationPaint.setStrokeWidth(4);
                float pulseSize = shape.size * (1 + (float)Math.sin(shape.phase * 2) * 0.3f);
                canvas.drawCircle(shape.x, shape.y, pulseSize, imaginationPaint);
                imaginationPaint.setAlpha(150);
                canvas.drawCircle(shape.x, shape.y, pulseSize * 0.6f, imaginationPaint);
                break;
                
            case "line":
                imaginationPaint.setStrokeWidth(5);
                float len = shape.size;
                float angle = shape.phase;
                canvas.drawLine(
                    shape.x - (float)Math.cos(angle) * len/2,
                    shape.y - (float)Math.sin(angle) * len/2,
                    shape.x + (float)Math.cos(angle) * len/2,
                    shape.y + (float)Math.sin(angle) * len/2,
                    imaginationPaint
                );
                break;
                
            default:
                // شكل افتراضي
                imaginationPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(shape.x, shape.y, shape.size * 0.3f, imaginationPaint);
        }
    }
    
    private void drawConnections(Canvas canvas) {
        if (floatingShapes.size() < 2) return;
        
        imaginationPaint.setStyle(Paint.Style.STROKE);
        imaginationPaint.setStrokeWidth(1);
        
        for (int i = 0; i < floatingShapes.size(); i++) {
            for (int j = i + 1; j < floatingShapes.size(); j++) {
                FloatingShape a = floatingShapes.get(i);
                FloatingShape b = floatingShapes.get(j);
                
                float dx = a.x - b.x;
                float dy = a.y - b.y;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);
                
                if (dist < 150) {
                    int alpha = (int)(255 * (1 - dist/150) * 0.3);
                    imaginationPaint.setColor(Color.WHITE);
                    imaginationPaint.setAlpha(alpha);
                    canvas.drawLine(a.x, a.y, b.x, b.y, imaginationPaint);
                }
            }
        }
    }

    // ===== التفاعل =====
    public String onTouch(float x, float y) {
        if (!showImagination || floatingShapes.isEmpty()) return null;
        
        FloatingShape closest = null;
        float minDist = 100;
        
        for (FloatingShape shape : floatingShapes) {
            float dist = (float)Math.sqrt((shape.x - x) * (shape.x - x) + 
                                         (shape.y - y) * (shape.y - y));
            if (dist < minDist) {
                minDist = dist;
                closest = shape;
            }
        }
        
        if (closest != null) {
            // تأثير بصري
            closest.phase += Math.PI;
            closest.size *= 1.2f;
            invalidate();
            
            return mapShapeToConcept(closest);
        }
        
        return null;
    }
    
    private String mapShapeToConcept(FloatingShape shape) {
        int index = floatingShapes.indexOf(shape);
        if (currentThought != null && currentThought.description != null) {
            return currentThought.description + "_" + index;
        }
        return "شكل_" + shape.type;
    }
    
    // ===== الفئات العامة =====
    
    public static class VisualThought {
        public String id;
        public String description;
        public int[] colorPalette;
        public List<ShapeElement> shapes;
        public float chaosLevel;
        public String emotionalTheme;
        public long createdAt;
        
        public VisualThought(String description) {
            this.id = String.valueOf(System.currentTimeMillis());
            this.description = description;
            this.colorPalette = new int[5];
            this.shapes = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
        }
    }
    
    public static class ShapeElement {
        public String type; // "circle", "line", "spiral", "pulse"
        public float x, y;
        public float size;
        public int color;
        public float animationSpeed;
        public float phase;
    }
}

package com.neuralseed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import java.util.ArrayList;
import java.util.List;

public class EnhancedPulseView extends PulseView {
    
    private Paint imaginationPaint;
    private LinguisticCortex.VisualThought currentThought;
    private List<FloatingShape> floatingShapes;
    private float globalPhase = 0;
    
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
            
            // حركة عشوائية مؤثرة بالفوضى
            float chaosX = (float)(Math.sin(phase * 2) * chaosLevel * chaosInfluence * 30);
            float chaosY = (float)(Math.cos(phase * 1.5f) * chaosLevel * chaosInfluence * 30);
            
            // حركة نبضية
            float pulse = (float)(Math.sin(globalPhase + phase) * 10);
            
            x = baseX + chaosX + pulse;
            y = baseY + chaosY + pulse;
        }
    }
    
    public EnhancedPulseView(Context context) {
        super(context);
        init();
    }
    
    public EnhancedPulseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        imaginationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        imaginationPaint.setStyle(Paint.Style.STROKE);
        imaginationPaint.setStrokeWidth(3);
        floatingShapes = new ArrayList<>();
    }
    
    public void setVisualThought(LinguisticCortex.VisualThought thought) {
        this.currentThought = thought;
        rebuildShapes();
        invalidate();
    }
    
    private void rebuildShapes() {
        floatingShapes.clear();
        if (currentThought == null) return;
        
        for (LinguisticCortex.ShapeElement elem : currentThought.shapes) {
            FloatingShape shape = new FloatingShape(
                elem.x * getWidth(),
                elem.y * getHeight(),
                elem.size,
                elem.color,
                elem.type
            );
            shape.speed = elem.animationSpeed * 0.02f;
            floatingShapes.add(shape);
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (currentThought == null || floatingShapes.isEmpty()) return;
        
        globalPhase += 0.05f;
        
        // تحديث و رسم الأشكال
        for (FloatingShape shape : floatingShapes) {
            shape.update(globalPhase, currentThought.chaosLevel);
            drawShape(canvas, shape);
        }
        
        // رسم الروابط بين الأشكال المتقاربة
        drawConnections(canvas);
        
        // إعادة الرسم
        postInvalidateDelayed(50);
    }
    
    private void drawShape(Canvas canvas, FloatingShape shape) {
        imaginationPaint.setColor(shape.color);
        imaginationPaint.setAlpha(200);
        
        switch (shape.type) {
            case "circle":
                imaginationPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(shape.x, shape.y, shape.size * 0.5f, imaginationPaint);
                // هالة
                imaginationPaint.setStyle(Paint.Style.STROKE);
                imaginationPaint.setAlpha(100);
                canvas.drawCircle(shape.x, shape.y, shape.size * (0.7f + (float)Math.sin(shape.phase) * 0.1f), imaginationPaint);
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
                // داخلي
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
    
    // التفاعل مع اللمس
    public String onTouch(float x, float y) {
        if (currentThought == null) return null;
        
        // البحث عن الشكل الأقرب
        FloatingShape closest = null;
        float minDist = 100;
        
        for (FloatingShape shape : floatingShapes) {
            float dist = (float)Math.sqrt((shape.x - x) * (shape.x - x) + (shape.y - y) * (shape.y - y));
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
            
            // إرجاع نوع الشكل للمعالجة
            return mapShapeToConcept(closest);
        }
        
        return null;
    }
    
    private String mapShapeToConcept(FloatingShape shape) {
        int index = floatingShapes.indexOf(shape);
        if (currentThought != null && index < currentThought.shapes.size()) {
            return currentThought.description + "_" + index;
        }
        return "شكل_" + shape.type;
    }
}

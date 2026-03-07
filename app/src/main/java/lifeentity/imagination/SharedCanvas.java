package com.lifeentity.imagination;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

public class SharedCanvas {
    
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;
    private List<ImaginedObject> objects;
    private ImaginedObject selected;
    private OnCanvasInteraction listener;
    private float lastX, lastY;
    private long touchStartTime;
    
    public interface OnCanvasInteraction {
        void onObjectCreated(String desc, float x, float y);
        void onObjectSelected(String id, String concept);
        void onObjectMoved(String id, float x, float y);
        void onGestureDrawn(String gesture, float x, float y);
        void onCanvasQuestion(String question);
    }
    
    public SharedCanvas(int w, int h) {
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        clear();
        
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        
        objects = new ArrayList<>();
    }
    
    public void clear() {
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
    }
    
    public void imagineObject(String concept, float x, float y, float size, int[] colors) {
        ImaginedObject obj = new ImaginedObject();
        obj.id = "obj_" + System.currentTimeMillis();
        obj.concept = concept;
        obj.x = x;
        obj.y = y;
        obj.size = Math.max(20, size);
        obj.colors = colors != null ? colors : new int[] { 200, 200, 200 };
        obj.shape = generateShape(concept);
        
        objects.add(obj);
        drawObject(obj);
        
        if (listener != null) {
            listener.onObjectCreated(concept, x, y);
        }
    }
    
    public boolean onTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartTime = System.currentTimeMillis();
                lastX = x;
                lastY = y;
                selected = findObjectAt(x, y);
                
                if (selected != null && listener != null) {
                    listener.onObjectSelected(selected.id, selected.concept);
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (selected != null) {
                    selected.x += (x - lastX);
                    selected.y += (y - lastY);
                    redraw();
                    if (listener != null) {
                        listener.onObjectMoved(selected.id, selected.x, selected.y);
                    }
                } else {
                    canvas.drawLine(lastX, lastY, x, y, paint);
                    detectGesture(x - lastX, y - lastY);
                }
                lastX = x;
                lastY = y;
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
            listener.onGestureDrawn(gesture, lastX, lastY);
        }
    }
    
    private ImaginedObject findObjectAt(float x, float y) {
        for (int i = objects.size() - 1; i >= 0; i--) {
            ImaginedObject obj = objects.get(i);
            float dx = x - obj.x;
            float dy = y - obj.y;
            if (Math.sqrt(dx*dx + dy*dy) < obj.size) {
                return obj;
            }
        }
        return null;
    }
    
    public ImaginedObject getLastSelectedObject() {
        return selected;
    }

    public enum ImaginationMode {
        PERCEPTUAL, MEMORY, COUNTERFACTUAL, CREATIVE, DREAM
    }

    
    public String findNearestConcept(float x, float y) {
        ImaginedObject nearest = null;
        float minDist = Float.MAX_VALUE;
        
        for (ImaginedObject obj : objects) {
            float dx = x - obj.x;
            float dy = y - obj.y;
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            if (dist < minDist) {
                minDist = dist;
                nearest = obj;
            }
        }
        
        return nearest != null ? nearest.concept : "الفراغ";
    }
    
    private void drawObject(ImaginedObject obj) {
        paint.setColor(Color.rgb(
            Math.min(255, obj.colors[0]),
            Math.min(255, obj.colors[1]),
            Math.min(255, obj.colors[2])
        ));
        paint.setAlpha(200);
        paint.setStyle(Paint.Style.FILL);
        
        switch (obj.shape) {
            case "circle":
                canvas.drawCircle(obj.x, obj.y, obj.size, paint);
                break;
            case "square":
                canvas.drawRect(
                    obj.x - obj.size, obj.y - obj.size,
                    obj.x + obj.size, obj.y + obj.size,
                    paint
                );
                break;
            default:
                Path p = new Path();
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3;
                    float px = obj.x + (float)(Math.cos(angle) * obj.size);
                    float py = obj.y + (float)(Math.sin(angle) * obj.size);
                    if (i == 0) p.moveTo(px, py);
                    else p.lineTo(px, py);
                }
                p.close();
                canvas.drawPath(p, paint);
        }
        
        // Label
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        canvas.drawText(obj.concept, obj.x - obj.size, obj.y + obj.size + 30, paint);
    }
    
    private void redraw() {
        clear();
        for (ImaginedObject obj : objects) {
            drawObject(obj);
        }
    }
    
    private String generateShape(String concept) {
        int hash = concept.hashCode();
        String[] shapes = {"circle", "square", "organic"};
        return shapes[Math.abs(hash) % shapes.length];
    }
    
    public Bitmap getBitmap() {
        return bitmap;
    }
    
    public void setListener(OnCanvasInteraction l) {
        this.listener = l;
    }
    
    public static class ImaginedObject {
        public String id;
        public String concept;
        public float x, y, size;
        public int[] colors;
        public String shape;
    }
}

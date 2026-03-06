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
        canvas.drawColor(Color.BLACK);
        
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        
        objects = new ArrayList<>();
    }
    
    public void imagineObject(String concept, float x, float y, float size, int[] colors) {
        ImaginedObject obj = new ImaginedObject();
        obj.id = "obj_" + System.currentTimeMillis();
        obj.concept = concept;
        obj.x = x;
        obj.y = y;
        obj.size = size;
        obj.colors = colors;
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
                lastX = x;
                lastY = y;
                selected = findObjectAt(x, y);
                
                if (selected != null && listener != null) {
                    listener.onObjectSelected(selected.id, selected.concept);
                }
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (selected != null) {
                    selected.x += x - lastX;
                    selected.y += y - lastY;
                    redraw();
                    if (listener != null) {
                        listener.onObjectMoved(selected.id, selected.x, selected.y);
                    }
                } else {
                    canvas.drawLine(lastX, lastY, x, y, paint);
                    detectGesture(x, y);
                }
                lastX = x;
                lastY = y;
                return true;
                
            case MotionEvent.ACTION_UP:
                if (selected == null && listener != null) {
                    listener.onCanvasQuestion("ما هذا؟");
                }
                selected = null;
                return true;
        }
        return false;
    }
    
    private void detectGesture(float x, float y) {
        float dx = x - lastX;
        float dy = y - lastY;
        
        String gesture;
        if (Math.abs(dx) > Math.abs(dy) * 3) {
            gesture = dx > 0 ? "swipe_right" : "swipe_left";
        } else if (Math.abs(dy) > Math.abs(dx) * 3) {
            gesture = dy > 0 ? "swipe_down" : "swipe_up";
        } else {
            gesture = "draw";
        }
        
        if (listener != null) {
            listener.onGestureDrawn(gesture, x, y);
        }
    }
    
    private ImaginedObject findObjectAt(float x, float y) {
        for (ImaginedObject obj : objects) {
            float d = (float) Math.sqrt((x-obj.x)*(x-obj.x) + (y-obj.y)*(y-obj.y));
            if (d < obj.size) return obj;
        }
        return null;
    }
    
    public ImaginedObject getLastSelectedObject() {
        return selected;
    }
    
    public String findNearestConcept(float x, float y) {
        ImaginedObject nearest = null;
        float minD = Float.MAX_VALUE;
        
        for (ImaginedObject obj : objects) {
            float d = (float) Math.sqrt((x-obj.x)*(x-obj.x) + (y-obj.y)*(y-obj.y));
            if (d < minD) {
                minD = d;
                nearest = obj;
            }
        }
        
        return nearest != null ? nearest.concept : "empty";
    }
    
    private void drawObject(ImaginedObject obj) {
        paint.setColor(Color.rgb(obj.colors[0], obj.colors[1], obj.colors[2]));
        paint.setAlpha(200);
        
        switch (obj.shape) {
            case "circle":
                canvas.drawCircle(obj.x, obj.y, obj.size, paint);
                break;
            case "square":
                canvas.drawRect(obj.x-obj.size, obj.y-obj.size, 
                    obj.x+obj.size, obj.y+obj.size, paint);
                break;
            default:
                Path p = new Path();
                for (int i = 0; i < 6; i++) {
                    float a = (float) (i * Math.PI / 3);
                    float px = obj.x + (float) Math.cos(a) * obj.size;
                    float py = obj.y + (float) Math.sin(a) * obj.size;
                    if (i == 0) p.moveTo(px, py);
                    else p.lineTo(px, py);
                }
                p.close();
                canvas.drawPath(p, paint);
        }
        
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        canvas.drawText(obj.concept, obj.x-obj.size, obj.y+obj.size+30, paint);
    }
    
    private void redraw() {
        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
        for (ImaginedObject obj : objects) {
            drawObject(obj);
        }
    }
    
    private String generateShape(String concept) {
        int h = concept.hashCode();
        String[] shapes = {"circle", "square", "organic"};
        return shapes[Math.abs(h) % shapes.length];
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

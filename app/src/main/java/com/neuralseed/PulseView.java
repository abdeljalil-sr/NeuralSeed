package com.neuralseed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PulseView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private NeuralSeed.EgoType currentEgoType = NeuralSeed.EgoType.STABLE;
    private float pulsePhase = 0;

    public PulseView(Context context) {
        super(context);
        init();
    }

    public PulseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        startAnimation();
    }

    public void setEgoType(NeuralSeed.EgoType type) {
        currentEgoType = type;
        invalidate();
    }

    private void startAnimation() {
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                pulsePhase += 0.1f;
                invalidate();
                postOnAnimation(this);
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
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

        paint.setColor(color);
        for (int i = 0; i < 3; i++) {
            float radius = (float) (50 + i * 30 + Math.sin(pulsePhase * speed + i) * 10);
            int alpha = (int) (150 - i * 40 + Math.sin(pulsePhase * speed) * 50);
            paint.setAlpha(Math.max(0, alpha));
            canvas.drawCircle(centerX, centerY, radius, paint);
        }
    }
}

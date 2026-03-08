package com.lifeentity.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * الحس الحركي (Kinesthetic Sense) – يستقبل بيانات التسارع والدوران من مستشعرات الجهاز.
 * يحولها إلى بيانات رقمية خام ويرسلها عبر المستمع (OnMotionSensed).
 * لا يقوم بأي تفسير أو تصنيف (مثل "خوف" أو "انتباه")، فهذه مسؤولية الوعي.
 */
public class KinestheticSense implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private OnMotionSensed listener;
    private MotionState currentMotion;
    private float[] gravity = new float[3];
    private float[] linearAccel = new float[3];

    public interface OnMotionSensed {
        void onMotionDetected(MotionState state);
        void onShakeDetected(float intensity);
        void onOrientationChanged(String newOrientation);
        void onFallDetected();
    }

    public KinestheticSense(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        currentMotion = new MotionState();
    }

    public void activate() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void deactivate() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                processAccel(event.values);
                break;
            case Sensor.TYPE_GYROSCOPE:
                processGyro(event.values);
                break;
        }
    }

    private void processAccel(float[] values) {
        final float alpha = 0.8f;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2];

        linearAccel[0] = values[0] - gravity[0];
        linearAccel[1] = values[1] - gravity[1];
        linearAccel[2] = values[2] - gravity[2];

        float magnitude = (float) Math.sqrt(
            linearAccel[0] * linearAccel[0] +
            linearAccel[1] * linearAccel[1] +
            linearAccel[2] * linearAccel[2]
        );

        currentMotion.accelerationMagnitude = magnitude;

        // الكشف عن الاهتزاز (هزات قوية) – لا يحدد المشاعر، فقط يرسل حدثاً
        if (magnitude > 15) {
            if (listener != null) {
                listener.onShakeDetected(Math.min(1, magnitude / 25));
            }
            if (magnitude > 25 && listener != null) {
                listener.onFallDetected();
            }
        }

        detectPosture();

        if (listener != null) {
            listener.onMotionDetected(currentMotion);
        }
    }

    private void processGyro(float[] values) {
        currentMotion.rotationSpeed = (float) Math.sqrt(
            values[0] * values[0] + values[1] * values[1] + values[2] * values[2]
        );
    }

    /**
     * تقدير الوضعية (upright, face_down, face_up, tilted) بناءً على الجاذبية.
     */
    private void detectPosture() {
        float pitch = (float) Math.atan2(gravity[1],
                Math.sqrt(gravity[0] * gravity[0] + gravity[2] * gravity[2]));

        String newOrient;
        if (Math.abs(pitch) < 0.3) newOrient = "upright";
        else if (pitch > 1.0) newOrient = "face_down";
        else if (pitch < -1.0) newOrient = "face_up";
        else newOrient = "tilted";

        if (!newOrient.equals(currentMotion.orientation)) {
            currentMotion.orientation = newOrient;
            if (listener != null) {
                listener.onOrientationChanged(newOrient);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // يمكن تجاهله
    }

    public MotionState getCurrentMotion() {
        return currentMotion;
    }

    public void setListener(OnMotionSensed l) {
        this.listener = l;
    }

    /**
     * كائن يمثل الحالة الحركية الحالية – بيانات خام فقط، لا تفسير.
     */
    public static class MotionState {
        public float accelerationMagnitude; // شدة التسارع الخطي (m/s² تقريباً)
        public float rotationSpeed;          // سرعة الدوران (rad/s)
        public String orientation = "unknown"; // الوضعية التقديرية

        // يمكن إضافة المزيد من الحقول الخام لاحقاً (مثل قيم المحاور الفردية)
    }
}

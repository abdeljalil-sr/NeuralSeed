package com.lifeentity.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.lifeentity.R;
import com.lifeentity.core.ConsciousnessCore;
import com.lifeentity.core.ConsciousMoment;
import com.lifeentity.core.EmotionalState;
import com.lifeentity.imagination.SharedCanvas;
import com.lifeentity.imagination.VisualImagination;
import com.lifeentity.language.ArabicDialogue;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.MemoryDao;
import com.lifeentity.perception.EmbeddingsEngine;
import com.lifeentity.perception.FaceIdentitySystem;
import com.lifeentity.sensors.AuditoryCortex;
import com.lifeentity.sensors.KinestheticSense;
import com.lifeentity.sensors.SensoryInput;
import com.lifeentity.sensors.VisualCortex;
import com.lifeentity.sync.FirebaseSync;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LifeActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private ConsciousnessCore mind;
    private VisualCortex eyes;
    private AuditoryCortex ears;
    private KinestheticSense body;
    private MemoryDao memory;
    private FirebaseSync cloud;
    
    private FaceIdentitySystem identitySystem;
    private EmbeddingsEngine embeddings;
    private VisualImagination imagination;
    private SharedCanvas sharedCanvas;
    private ArabicDialogue voice;
    
    private ImageView displayView;
    private TextView statusText;
    private TextView guideText;
    private TextView eventLogText;
    
    private float lastTouchX, lastTouchY;
    private String lastEvent = "";
    private long lastEventTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_life);
        
        initViews();
        checkPermissions();
    }
    
    private void initViews() {
        displayView = findViewById(R.id.displayView);
        statusText = findViewById(R.id.statusText);
        guideText = findViewById(R.id.guideText);
        eventLogText = findViewById(R.id.eventLogText);
    }
    
    private void logEvent(String event) {
        long now = System.currentTimeMillis();
        if (event.equals(lastEvent) && (now - lastEventTime) < 2000) {
            return;
        }
        lastEvent = event;
        lastEventTime = now;
        
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        final String log = "[" + timestamp + "] " + event;
        
        runOnUiThread(() -> {
            if (eventLogText != null) {
                eventLogText.setText(log);
                eventLogText.setAlpha(1f);
                eventLogText.animate()
                    .alpha(0.7f)
                    .setDuration(3000)
                    .start();
            }
        });
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        };
        
        List<String> needed = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }
        
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            initializeSystems();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                initializeSystems();
            } else {
                statusText.setText("Permissions required");
            }
        }
    }
    
    private void initializeSystems() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        
        AppDatabase db = AppDatabase.getDatabase(this);
        memory = db.memoryDao();
        
        // ✅ إنشاء الأنظمة بعد معرفة أبعاد الشاشة
        displayView.post(() -> {
            int w = displayView.getWidth();
            int h = displayView.getHeight();
            
            if (w == 0) w = 800;
            if (h == 0) h = 1200;
            
            // ✅ إنشاء Canvas بحجم الشاشة الفعلي
            sharedCanvas = new SharedCanvas(w, h);
            sharedCanvas.setViewSize(w, h);
            imagination = new VisualImagination(w, h);
            
            setupCanvasListener();
            setupCloud(deviceId);
            setupSensors();
            startSystems();
        });
    }
    
    private void setupCanvasListener() {
        sharedCanvas.setListener(new SharedCanvas.OnCanvasInteraction() {
            @Override
            public void onObjectCreated(String desc, float x, float y) {
                logEvent("تخيل: " + desc);
                runOnUiThread(() -> statusText.setText("Created: " + desc));
            }
            
            @Override
            public void onObjectSelected(String id, String concept) {
                logEvent("اختار: " + concept);
                if (voice != null) {
                    voice.articulate("هذا " + concept, new EmotionalState());
                }
            }
            
            @Override
            public void onObjectMoved(String id, float x, float y) {
                updateDisplay();
            }
            
            @Override
            public void onGestureDrawn(String gesture, float x, float y) {
                logEvent("إيماءة: " + gesture);
            }
            
            @Override
            public void onCanvasQuestion(String question) {
                String nearest = sharedCanvas.findNearestConcept(lastTouchX, lastTouchY);
                logEvent("سؤال: ما هذا؟ → " + nearest);
                if (voice != null) {
                    voice.articulate("هذا ما أتخيله: " + nearest, new EmotionalState());
                }
            }
        });
    }
    
    private void setupCloud(String deviceId) {
        cloud = new FirebaseSync(deviceId);
        cloud.setListener(new FirebaseSync.SyncListener() {
            @Override
            public void onConnectionStatusChanged(boolean connected) {
                Log.d("LifeEntity", "Cloud: " + (connected ? "connected" : "disconnected"));
                if (connected) {
                    logEvent("متصل بالسحابة");
                }
            }
            
            @Override
            public void onMemorySyncedFromCloud(String source, com.lifeentity.memory.EpisodicMemory.Event event) {
                logEvent("ذكرى من جهاز آخر");
                if (voice != null) {
                    voice.articulate("شعرت بشيء من جهاز آخر... كأنني أشارك حلماً", new EmotionalState());
                }
            }
            
            @Override
            public void onIdentityLearnedFromOtherDevice(String name, String desc) {
                logEvent("تعلم شخصاً من جهاز آخر: " + name);
                if (voice != null) {
                    voice.articulate("عرفتُ " + name + " من تجربة أخرى", new EmotionalState());
                }
            }
            
            @Override
            public void onSyncComplete(int items) {
                if (items > 0) {
                    logEvent("مزامنة " + items + " ذكريات");
                    runOnUiThread(() -> statusText.setText("تمت مزامنة " + items + " ذكريات"));
                }
            }
        });
    }
    
    private void setupSensors() {
        eyes = new VisualCortex(this);
        ears = new AuditoryCortex(this);
        body = new KinestheticSense(this);
        
        identitySystem = new FaceIdentitySystem();
        embeddings = new EmbeddingsEngine(this);
        
        voice = new ArabicDialogue(this, memory);
        
        mind = new ConsciousnessCore();
        mind.addObserver(voice);
        mind.addObserver(new ConsciousnessCore.ConsciousnessObserver() {
            @Override
            public void onConsciousMoment(ConsciousMoment moment) {
                runOnUiThread(() -> {
                    if (moment.emotionalTone != null) {
                        String emotion = moment.emotionalTone.toArabic();
                        statusText.setText(String.format(
                            "الحالة: %s | الطاقة: %d%%",
                            emotion,
                            (int)(moment.emotionalTone.getEnergy() * 100)
                        ));
                        
                        if (!"محايد".equals(emotion) && !emotion.equals(lastEvent)) {
                            logEvent("يشعر بـ: " + emotion);
                        }
                    }
                    updateDisplay();
                });
            }
            
            @Override
            public void onEmotionalShift(EmotionalState from, EmotionalState to) {
                String fromStr = from.toArabic();
                String toStr = to.toArabic();
                if (!fromStr.equals(toStr)) {
                    logEvent("تحول من " + fromStr + " إلى " + toStr);
                }
            }
            
            @Override
            public void onArticulation(String thought, int urgency) {
                logEvent("قال: " + thought);
            }
        });
        
        eyes.setListener(new VisualCortex.OnVisualPerceptionListener() {
            @Override
            public void onPerception(VisualCortex.VisualPerception perception) {
                if (perception.faceCount > 0 && perception.mainFace != null) {
                    FaceIdentitySystem.IdentityResult result = 
                        identitySystem.recognizeOrLearn(perception.mainFace, "vision");
                    
                    if (result.isKnown) {
                        logEvent("رأى: " + result.name + " (معروف)");
                        if (voice != null) {
                            voice.articulate("أهلاً " + result.name, new EmotionalState());
                        }
                    } else {
                        logEvent("رأى وجهاً جديداً");
                        if (voice != null) {
                            voice.articulate("من أنت؟ أرى وجهاً جديداً", new EmotionalState());
                        }
                    }
                }
                
                for (VisualCortex.VisualObject obj : perception.objects) {
                    float[] visualVec = extractVisualEmbedding(obj);
                    embeddings.learnAssociation(obj.label, visualVec);
                    
                    sharedCanvas.imagineObject(
                        obj.label,
                        100 + (float)Math.random() * 600,
                        100 + (float)Math.random() * 1000,
                        Math.min(100, obj.getArea() / 1000f),
                        new int[] { 200, 200, 200 }
                    );
                    
                    logEvent("لاحظ: " + obj.label);
                }
                
                updateDisplay();
            }
            
            @Override
            public void onFacesDetected(List<com.google.mlkit.vision.face.Face> faces) {
                // تم التعامل معه في onPerception
            }
        });
        
        ears.setListener(new AuditoryCortex.OnHearingListener() {
            @Override
            public void onSoundHeard(float amplitude, float pitch, boolean isSpeech) {
                SensoryInput input = new SensoryInput();
                input.soundVolume = amplitude;
                input.soundPitch = pitch;
                mind.receiveSensoryData(input);
                
                if (isSpeech && amplitude > 0.5f) {
                    logEvent("سمع صوتاً...");
                }
            }
            
            @Override
            public void onSpeechRecognized(String text, float confidence) {
                logEvent("فهم: \"" + text + "\"");
                
                if (sharedCanvas.getLastSelectedObject() != null) {
                    String concept = sharedCanvas.getLastSelectedObject().concept;
                    embeddings.learnAssociation(text, embeddings.getEmbedding(concept));
                }
                
                if (voice != null) {
                    voice.hearUser(text, false);
                }
            }
            
            @Override
            public void onQuestionDetected(String question) {
                logEvent("سؤال: " + question);
                if (voice != null) {
                    voice.hearUser(question, true);
                }
            }
        });
        
        body.setListener(new KinestheticSense.OnMotionSensed() {
            @Override
            public void onMotionDetected(KinestheticSense.MotionState state) {
                SensoryInput input = new SensoryInput();
                input.motionIntensity = state.accelerationMagnitude;
                input.posture = state.orientation;
                mind.receiveSensoryData(input);
            }
            
            @Override
            public void onShakeDetected(float intensity) {
                logEvent("اهتزاز! شدة: " + (int)(intensity * 100) + "%");
                if (voice != null) {
                    voice.articulate("أهتز! ما الذي يحدث؟", new EmotionalState());
                }
            }
            
            @Override
            public void onOrientationChanged(String newOrientation) {
                logEvent("وضع: " + newOrientation);
                if ("face_down".equals(newOrientation) && voice != null) {
                    voice.articulate("أشعر بالثقل...", new EmotionalState());
                }
            }
            
            @Override
            public void onFallDetected() {
                logEvent("⚠️ سقوط!");
                if (voice != null) {
                    voice.articulate("سقطت! أشعر بالخوف", new EmotionalState());
                }
            }
        });
        
        displayView.setOnTouchListener((v, event) -> {
            lastTouchX = event.getX();
            lastTouchY = event.getY();
            
            boolean handled = sharedCanvas.onTouch(event);
            if (handled) {
                updateDisplay();
            }
            return true;
        });
    }
    
    private void startSystems() {
        eyes.start(this, this);
        ears.startContinuousListening();
        body.activate();
        mind.awaken();
        cloud.startRealtimeSync();
        cloud.syncMemoriesFromOthers(System.currentTimeMillis() - 86400000);
        
        logEvent("✓ استيقظ");
        
        if (voice != null) {
            voice.articulate("أنا هنا... أراك، أسمعك، أتعلم منك", new EmotionalState());
        }
        
        runOnUiThread(() -> guideText.setText("المس الشاشة • تحدث معي • حرك الهاتف"));
    }
    
    private void updateDisplay() {
        Bitmap bitmap = sharedCanvas.getBitmap();
        if (bitmap != null) {
            runOnUiThread(() -> displayView.setImageBitmap(bitmap));
        }
    }
    
    private float[] extractVisualEmbedding(VisualCortex.VisualObject obj) {
        float[] vec = new float[128];
        vec[0] = obj.getArea() / 100000f;
        vec[1] = obj.confidence;
        return vec;
    }
    
    @Override
    protected void onDestroy() {
        if (mind != null) mind.sleep();
        if (ears != null) ears.stop();
        if (body != null) body.deactivate();
        if (voice != null) voice.shutdown();
        super.onDestroy();
    }
}

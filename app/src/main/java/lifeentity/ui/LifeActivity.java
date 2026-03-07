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

import java.util.ArrayList;
import java.util.List;

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
    
    private float lastTouchX, lastTouchY;

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
        
        cloud = new FirebaseSync(deviceId);
        cloud.setListener(new FirebaseSync.SyncListener() {
            
            // ✅ هذه الدالة المضافة:
            @Override
            public void onConnectionStatusChanged(boolean connected) {
                Log.d("LifeEntity", "Cloud: " + (connected ? "connected" : "disconnected"));
            }
            
            @Override
            public void onMemorySyncedFromCloud(String source, com.lifeentity.memory.EpisodicMemory.Event event) {
                if (voice != null) {
                    voice.articulate("شعرت بشيء من جهاز آخر... كأنني أشارك حلماً", new EmotionalState());
                }
            }
            
            @Override
            public void onIdentityLearnedFromOtherDevice(String name, String desc) {
                if (voice != null) {
                    voice.articulate("عرفتُ " + name + " من تجربة أخرى", new EmotionalState());
                }
            }
            
            @Override
            public void onSyncComplete(int items) {
                if (items > 0) {
                    runOnUiThread(() -> statusText.setText("تمت مزامنة " + items + " ذكريات"));
                }
            }
        });
        
        eyes = new VisualCortex(this);
        ears = new AuditoryCortex(this);
        body = new KinestheticSense(this);
        
        identitySystem = new FaceIdentitySystem();
        embeddings = new EmbeddingsEngine(this);
        imagination = new VisualImagination(800, 1200);
        
        sharedCanvas = new SharedCanvas(800, 1200);
        sharedCanvas.setListener(new SharedCanvas.OnCanvasInteraction() {
            @Override
            public void onObjectCreated(String desc, float x, float y) {
                runOnUiThread(() -> statusText.setText("Created: " + desc));
            }
            
            @Override
            public void onObjectSelected(String id, String concept) {
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
                // Handle gesture
            }
            
            @Override
            public void onCanvasQuestion(String question) {
                String nearest = sharedCanvas.findNearestConcept(lastTouchX, lastTouchY);
                if (voice != null) {
                    voice.articulate("هذا ما أتخيله: " + nearest, new EmotionalState());
                }
            }
        });
        
        voice = new ArabicDialogue(this, memory);
        
        mind = new ConsciousnessCore();
        mind.addObserver(voice);
        mind.addObserver(new ConsciousnessCore.ConsciousnessObserver() {
            @Override
            public void onConsciousMoment(ConsciousMoment moment) {
                runOnUiThread(() -> {
                    if (moment.emotionalTone != null) {
                        statusText.setText(String.format(
                            "الحالة: %s | الطاقة: %d%%",
                            moment.emotionalTone.toArabic(),
                            (int)(moment.emotionalTone.getEnergy() * 100)
                        ));
                    }
                    updateDisplay();
                });
            }
            
            @Override
            public void onEmotionalShift(EmotionalState from, EmotionalState to) {
                // Handle shift
            }
            
            @Override
            public void onArticulation(String thought, int urgency) {
                // Handle articulation
            }
        });
        
        eyes.setListener(new VisualCortex.OnVisualPerceptionListener() {
            @Override
            public void onPerception(VisualCortex.VisualPerception perception) {
                if (perception.faceCount > 0 && perception.mainFace != null) {
                    FaceIdentitySystem.IdentityResult result = 
                        identitySystem.recognizeOrLearn(perception.mainFace, "vision");
                    
                    if (result.isKnown && voice != null) {
                        voice.articulate("أهلاً " + result.name, new EmotionalState());
                    } else if (voice != null) {
                        voice.articulate("من أنت؟ أرى وجهاً جديداً", new EmotionalState());
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
                }
                
                updateDisplay();
            }
            
            @Override
            public void onFacesDetected(List<com.google.mlkit.vision.face.Face> faces) {
                // Handle faces
            }
        });
        
        ears.setListener(new AuditoryCortex.OnHearingListener() {
            @Override
            public void onSoundHeard(float amplitude, float pitch, boolean isSpeech) {
                SensoryInput input = new SensoryInput();
                input.soundVolume = amplitude;
                input.soundPitch = pitch;
                mind.receiveSensoryData(input);
            }
            
            @Override
            public void onSpeechRecognized(String text, float confidence) {
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
                if (voice != null) {
                    voice.articulate("أهتز! ما الذي يحدث؟", new EmotionalState());
                }
            }
            
            @Override
            public void onOrientationChanged(String newOrientation) {
                if ("face_down".equals(newOrientation) && voice != null) {
                    voice.articulate("أشعر بالثقل...", new EmotionalState());
                }
            }
            
            @Override
            public void onFallDetected() {
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
        
        startSystems();
    }
    
    private void startSystems() {
        eyes.start(this, this);
        ears.startContinuousListening();
        body.activate();
        mind.awaken();
        cloud.startRealtimeSync();
        cloud.syncMemoriesFromOthers(System.currentTimeMillis() - 86400000);
        
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

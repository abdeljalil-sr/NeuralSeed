package com.lifeentity.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.lifeentity.R;
import com.lifeentity.core.ConsciousnessCore;
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

public class LifeActivity extends AppCompatActivity {
    
    // الأنظمة الأساسية
    private ConsciousnessCore mind;
    private VisualCortex eyes;
    private AuditoryCortex ears;
    private KinestheticSense body;
    private MemoryDao memory;
    private FirebaseSync cloud;
    
    // الأنظمة المعرفية
    private FaceIdentitySystem identitySystem;
    private EmbeddingsEngine embeddings;
    private VisualImagination imagination;
    private SharedCanvas sharedCanvas;
    private ArabicDialogue voice;
    
    // الواجهة
    private ImageView displayView;
    private TextView statusText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_life);
        
        requestPermissions();
        initializeSystems();
        setupInteractions();
        awakenEntity();
    }
    
    private void initializeSystems() {
        // الهوية الفريدة للجهاز
        String deviceId = Settings.Secure.getString(getContentResolver(), 
            Settings.Secure.ANDROID_ID);
        
        // الذاكرة
        memory = AppDatabase.getDatabase(this).memoryDao();
        
        // السحابة
        cloud = new FirebaseSync(deviceId);
        cloud.setListener(new FirebaseSync.SyncListener() {
            @Override
            public void onMemorySyncedFromCloud(String source, com.lifeentity.memory.EpisodicMemory.Event event) {
                voice.articulate("شعرت بشيء من جهاز آخر... كأنني أشارك حلماً", 
                    new com.lifeentity.core.EmotionalState());
            }
            
            @Override
            public void onIdentityLearnedFromOtherDevice(String name, String desc) {
                voice.articulate("عرفتُ " + name + " من تجربة أخرى", 
                    new com.lifeentity.core.EmotionalState());
            }
            
            @Override
            public void onSyncComplete(int items) {
                if (items > 0) {
                    statusText.setText("تمت مزامنة " + items + " ذكريات");
                }
            }
        });
        
        // الحواس
        eyes = new VisualCortex(this);
        ears = new AuditoryCortex(this);
        body = new KinestheticSense(this);
        
        // المعرفة
        identitySystem = new FaceIdentitySystem();
        embeddings = new EmbeddingsEngine(this);
        imagination = new VisualImagination(800, 1200);
        
        // اللوحة المشتركة
        sharedCanvas = new SharedCanvas(800, 1200);
        
        // الصوت
        voice = new ArabicDialogue(this, memory);
        
        // الوعي
        mind = new ConsciousnessCore();
        mind.addObserver(voice);
        
        // الواجهة
        displayView = findViewById(R.id.displayView);
        statusText = findViewById(R.id.statusText);
    }
    
    private void setupInteractions() {
        // الكاميرا → الهوية والخيال
        eyes.setListener(perception -> {
            // التعرف على الهوية
            if (perception.faceCount > 0) {
                FaceIdentitySystem.IdentityResult result = 
                    identitySystem.recognizeOrLearn(perception.faces.get(0), "direct_vision");
                
                if (result.isKnown) {
                    // شخص معروف!
                    voice.articulate("أهلاً " + result.name, 
                        new com.lifeentity.core.EmotionalState());
                } else {
                    // سؤال عن الاسم
                    voice.articulate("من أنت؟ أرى وجهاً جديداً", 
                        new com.lifeentity.core.EmotionalState());
                }
            }
            
            // تعلم الأشياء المرئية
            for (VisualCortex.VisualObject obj : perception.objects) {
                float[] visualVec = extractVisualEmbedding(obj);
                embeddings.learnAssociation(obj.label, visualVec);
                
                // رسم في الخيال
                sharedCanvas.imagineObject(
                    obj.label,
                    100 + (float)Math.random() * 600,
                    100 + (float)Math.random() * 1000,
                    obj.area / 1000f,
                    new int[]{200, 200, 200}
                );
            }
            
            updateDisplay();
        });
        
        // السمع → الحوار
        ears.setListener(new AuditoryCortex.OnHearingListener() {
            @Override
            public void onSoundHeard(float amp, float pitch, boolean speech) {
                // تمرير للوعي
            }
            
            @Override
            public void onSpeechRecognized(String text, float conf) {
                // تعلم الارتباطات
                if (sharedCanvas.getLastSelectedObject() != null) {
                    String concept = sharedCanvas.getLastSelectedObject().concept;
                    embeddings.learnAssociation(text, embeddings.getEmbedding(concept));
                }
                
                voice.hearUser(text, false);
            }
            
            @Override
            public void onQuestionDetected(String q) {
                voice.hearUser(q, true);
            }
        });
        
        // الجيروسكوب → الحالة الجسدية
        body.setListener(new KinestheticSense.OnMotionSensed() {
            @Override
            public void onMotionDetected(KinestheticSense.MotionState state) {
                SensoryInput motion = new SensoryInput();
                motion.motionIntensity = state.accelerationMagnitude;
                motion.posture = state.orientation;
                mind.receiveSensoryData(motion);
            }
            
            @Override
            public void onShakeDetected(float intensity) {
                voice.articulate("أهتز! ما الذي يحدث؟", 
                    new com.lifeentity.core.EmotionalState());
            }
            
            @Override
            public void onOrientationChanged(String orient) {
                if (orient.equals("face_down")) {
                    voice.articulate("أشعر بالثقل... كأنني أغمض عيني", 
                        new com.lifeentity.core.EmotionalState());
                }
            }
            
            @Override
            public void onFallDetected() {
                voice.articulate("سقطت! أشعر بالخوف", 
                    new com.lifeentity.core.EmotionalState());
            }
        });
        
        // اللوحة التفاعلية
        sharedCanvas.setListener(new SharedCanvas.OnCanvasInteraction() {
            @Override
            public void onObjectCreated(String desc, float x, float y) {
                // الكائن خلق شيئاً
            }
            
            @Override
            public void onObjectSelected(String id, String concept) {
                voice.articulate("هذا " + concept + ". يمكنك تحريكه", 
                    new com.lifeentity.core.EmotionalState());
            }
            
            @Override
            public void onObjectMoved(String id, float x, float y) {
                // المستخدم حرك شيئاً في عالم الكائن
                voice.articulate("أرى أنك تغير عالمي", 
                    new com.lifeentity.core.EmotionalState());
            }
            
            @Override
            public void onGestureDrawn(String gesture, float x, float y) {
                if (gesture.equals("swipe_up")) {
                    voice.articulate("إلى الأعلى!", 
                        new com.lifeentity.core.EmotionalState());
                }
            }
            
            @Override
            public void onCanvasQuestion(String q) {
                String nearest = sharedCanvas.findNearestConcept(lastTouchX, lastTouchY);
                voice.articulate("هذا ما أتخيله: " + nearest, 
                    new com.lifeentity.core.EmotionalState());
            }
        });
        
        displayView.setOnTouchListener((v, event) -> {
            sharedCanvas.onTouch(event);
            updateDisplay();
            return true;
        });
    }
    
    private void awakenEntity() {
        eyes.start(this, this);
        ears.startContinuousListening();
        body.activate();
        mind.awaken();
        cloud.startRealtimeSync();
        
        // مزامنة أولية
        cloud.syncMemoriesFromOthers(System.currentTimeMillis() - 86400000); // أمس
        
        voice.start();
    }
    
    private void updateDisplay() {
        runOnUiThread(() -> {
            displayView.setImageBitmap(sharedCanvas.getBitmap());
        });
    }
    
    private float[] extractVisualEmbedding(VisualCortex.VisualObject obj) {
        // تبسيط: تحويل خصائص بصرية إلى vector
        float[] vec = new float[128];
        vec[0] = obj.area / 100000f;
        vec[1] = obj.confidence;
        // ... إكمال التعبئة
        return vec;
    }
    
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        }, 100);
    }
    
    @Override
    protected void onDestroy() {
        mind.sleep();
        ears.stop();
        body.deactivate();
        super.onDestroy();
    }
}

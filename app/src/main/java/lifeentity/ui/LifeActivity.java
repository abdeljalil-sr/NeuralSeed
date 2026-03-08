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
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.MemoryDao;
import com.lifeentity.perception.EmbeddingsEngine;
import com.lifeentity.perception.FaceIdentitySystem;
import com.lifeentity.perception.SceneUnderstanding;
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
    private static final String TAG = "LifeActivity";

    private ConsciousnessCore mind;
    private VisualCortex eyes;
    private AuditoryCortex ears;
    private KinestheticSense body;
    private MemoryDao memoryDao;
    private AppDatabase database;
    private FirebaseSync cloud;

    private FaceIdentitySystem identitySystem;
    private EmbeddingsEngine embeddings;
    private VisualImagination imagination;
    private SharedCanvas sharedCanvas;
    private ArabicDialogue voice;
    private SceneUnderstanding sceneUnderstanding;

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

        database = AppDatabase.getDatabase(this);
        memoryDao = database.memoryDao();

        sceneUnderstanding = new SceneUnderstanding(this, database.visualMemoryDao());

        displayView.post(() -> {
            int w = displayView.getWidth();
            int h = displayView.getHeight();
            if (w == 0) w = 800;
            if (h == 0) h = 1200;

            sharedCanvas = new SharedCanvas(w, h);
            sharedCanvas.setViewSize(w, h);
            setupCanvasListener();

            imagination = new VisualImagination(w, h, database.visualMemoryDao());

            setupCloud(deviceId);
            setupSensors();
            startSystems();
        });
    }

    private void setupCanvasListener() {
        sharedCanvas.setListener(new SharedCanvas.OnCanvasInteraction() {
            @Override
            public void onObjectCreated(String concept, float x, float y) {
                logEvent("تخيل: " + concept);
                runOnUiThread(() -> statusText.setText("Created: " + concept));
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
                Log.d(TAG, "Cloud: " + (connected ? "connected" : "disconnected"));
                if (connected) {
                    logEvent("متصل بالسحابة");
                }
            }

            @Override
            public void onMemorySyncedFromCloud(String source, EpisodicMemory.Event event) {
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
        eyes = new VisualCortex(this, database);
        ears = new AuditoryCortex(this);
        body = new KinestheticSense(this);

        identitySystem = new FaceIdentitySystem(memoryDao);
        embeddings = new EmbeddingsEngine(memoryDao);

        voice = new ArabicDialogue(this, database);

        mind = new ConsciousnessCore(this, database);
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
                                (int) (moment.emotionalTone.getEnergy() * 100)
                        ));

                        if (!"محايد".equals(emotion) && !emotion.equals(lastEvent)) {
                            logEvent("يشعر بـ: " + emotion);
                        }
                    }
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
            public void onArticulation(String utterance, int urgency) {
                logEvent("قال: " + utterance);
            }

            @Override
            public void onVisualExpression(float[] latentVector, float intensity, String modality) {
                Bitmap imagined = imagination.imagine(latentVector, intensity, VisualImagination.ImaginationMode.CREATIVE);
                if (imagined != null) {
                    runOnUiThread(() -> {
                        sharedCanvas.setBackground(imagined);
                        updateDisplay();
                    });
                }
            }

            @Override
            public void onDreamGenerated(Bitmap dreamImage, String description) {
                if (dreamImage != null) {
                    runOnUiThread(() -> {
                        sharedCanvas.setBackground(dreamImage);
                        updateDisplay();
                        logEvent("💭 " + description);
                    });
                }
            }
        });

        eyes.setListener(new VisualCortex.OnVisualPerceptionListener() {
            @Override
            public void onPerception(VisualCortex.VisualPerception perception) {
                // تجهيز مدخل بصري للوعي
                SensoryInput visualInput = new SensoryInput();
                visualInput.hasHumanFace = perception.faceCount > 0;
                visualInput.faceProximity = perception.faceBounds != null ?
                        (float) perception.faceBounds.width() / displayView.getWidth() : 0;
                visualInput.faceEmbedding = perception.faceEmbedding;
                visualInput.objectCount = perception.objects.size();
                if (!perception.objects.isEmpty()) {
                    visualInput.dominantObject = perception.objects.get(0).label;
                }

                mind.receiveSensoryData(visualInput);

                // التعرف على الوجوه
                if (perception.faceCount > 0 && perception.faceEmbedding != null) {
                    float[] currentAffect = mind.getCurrentEmotion().toAffectVector();
                    FaceIdentitySystem.IdentityResult result =
                            identitySystem.recognizeOrLearn(perception.faceEmbedding, "camera", currentAffect);

                    if (result.isKnown) {
                        logEvent("رأى: " + result.name + " (معروف)");
                        if (voice != null && result.familiarity > 0.3f) {
                            voice.articulate("أهلاً " + result.name, new EmotionalState());
                        }
                    } else {
                        logEvent("رأى وجهاً جديداً");
                        if (voice != null) {
                            voice.articulate("من أنت؟ أرى وجهاً جديداً", new EmotionalState());
                        }
                    }
                }

                // تعلم الارتباط بين الكلمات والمرئيات
                for (VisualCortex.VisualObject obj : perception.objects) {
                    float[] visualVec = extractVisualEmbedding(obj);
                    embeddings.learnAssociation(obj.label, visualVec);
                }

                // ⬅️ تحليل المشهد وتعلمه باستخدام SceneUnderstanding
                if (sceneUnderstanding != null && perception.frame != null) {
                    float[] currentAffect = mind.getCurrentEmotion().toAffectVector();
                    new Thread(() -> {
                        sceneUnderstanding.learnScene(perception.frame, currentAffect);
                    }).start();
                }
            }

            @Override
            public void onFacesDetected(List<com.google.mlkit.vision.face.Face> faces) {
                // يمكن استخدامها إذا أردت
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

                SensoryInput speechInput = new SensoryInput();
                speechInput.recognizedSpeech = text;
                speechInput.speechDetected = true;
                mind.receiveSensoryData(speechInput);

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
                input.motionIntensity = state.accelerationMagnitude / 20f;
                input.posture = state.orientation;
                mind.receiveSensoryData(input);
            }

            @Override
            public void onShakeDetected(float intensity) {
                logEvent("اهتزاز! شدة: " + (int) (intensity * 100) + "%");
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

        cloud.start();
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
        for (int i = 2; i < 128; i++) {
            vec[i] = (float) Math.random();
        }
        return vec;
    }

    @Override
    protected void onDestroy() {
        if (mind != null) mind.sleep();
        if (ears != null) ears.stop();
        if (body != null) body.deactivate();
        if (voice != null) voice.shutdown();
        if (cloud != null) cloud.stop();
        if (sceneUnderstanding != null) sceneUnderstanding.close();
        super.onDestroy();
    }
}

package com.lifeentity.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.lifeentity.learning.CompetitiveLearningCore;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class LifeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "LifeActivity";
    private static final int MAX_CHAT_MESSAGES = 50;
    private static final long THOUGHTS_UPDATE_INTERVAL = 3000;
    private static final long EVENT_COOLDOWN_MS = 5000;

    private ConcurrentHashMap<String, Long> lastEventTimeMap = new ConcurrentHashMap<>();

    private ConsciousnessCore mind;
    private CompetitiveLearningCore learningCore;
    private String lastRecognizedSpeech = "";
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
    private TextView thoughtsTextView;
    private TextView statusText;
    private TextView guideText;
    private TextView eventLogText;
    private ListView chatListView;
    private EditText messageEditText;
    private Button sendButton;

    private ArrayAdapter<String> chatAdapter;
    private List<ChatMessage> chatMessages = new CopyOnWriteArrayList<>();
    private Handler uiHandler;
    private Runnable thoughtsUpdater;

    private float lastTouchX, lastTouchY;
    private String lastEvent = "";
    private long lastEventTime = 0;
    private ConcurrentLinkedQueue<String> pendingChatMessages = new ConcurrentLinkedQueue<>();

    private boolean userScrolling = false;
    private Runnable scrollResetRunnable;

    private static class ChatMessage {
        final String sender;
        final String content;
        final long timestamp;
        final boolean isUser;

        ChatMessage(String sender, String content, boolean isUser) {
            this.sender = sender;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.isUser = isUser;
        }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "[" + sdf.format(new Date(timestamp)) + "] " + sender + ": " + content;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_life);

        uiHandler = new Handler(Looper.getMainLooper());
        initViews();
        setupChatAdapter();
        setupSendButton();
        setupThoughtsUpdater();
        checkPermissions();
    }

    private void initViews() {
        displayView = findViewById(R.id.displayView);
        thoughtsTextView = findViewById(R.id.thoughtsTextView);
        statusText = findViewById(R.id.statusText);
        guideText = findViewById(R.id.guideText);
        eventLogText = findViewById(R.id.eventLogText);
        chatListView = findViewById(R.id.chatListView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        thoughtsTextView.setMovementMethod(new ScrollingMovementMethod());
        thoughtsTextView.setVisibility(View.INVISIBLE);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);

        chatListView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    userScrolling = true;
                    if (scrollResetRunnable != null) uiHandler.removeCallbacks(scrollResetRunnable);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scrollResetRunnable = () -> userScrolling = false;
                    uiHandler.postDelayed(scrollResetRunnable, 3000);
                    break;
            }
            return false;
        });
    }

    private void setupChatAdapter() {
        chatAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                ChatMessage msg = chatMessages.get(position);
                if (msg.isUser) {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                    textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                    textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                }
                return view;
            }
        };
        chatListView.setAdapter(chatAdapter);
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                addChatMessage("أنت", message, true);
                SensoryInput textInput = new SensoryInput();
                textInput.recognizedSpeech = message;
                textInput.speechDetected = true;
                if (mind != null) mind.receiveSensoryData(textInput);
                if (voice != null) voice.hearUser(message, message.contains("؟") || message.contains("?"));
                messageEditText.setText("");
            }
        });
    }

    private void setupThoughtsUpdater() {
        thoughtsUpdater = new Runnable() {
            @Override
            public void run() {
                if (mind != null && thoughtsTextView.getVisibility() == View.VISIBLE) {
                    updateThoughtsDisplay();
                }
                uiHandler.postDelayed(this, THOUGHTS_UPDATE_INTERVAL);
            }
        };
    }

    private void updateThoughtsDisplay() {
        StringBuilder thoughts = new StringBuilder();
        EmotionalState emotion = mind.getCurrentEmotion();
        if (emotion != null) {
            thoughts.append("أشعر بـ ").append(emotion.toArabic());
            if (emotion.getIntensity() > 0.7) thoughts.append(" (بشدة)");
            thoughts.append("\n");
        }
        String desire = mind.getDominantDesire();
        if (desire != null) thoughts.append("أريد أن ").append(translateDesire(desire)).append("\n");
        runOnUiThread(() -> {
            thoughtsTextView.setText(thoughts.toString());
            thoughtsTextView.scrollTo(0, 0);
        });
    }

    private String translateDesire(String desire) {
        switch (desire) {
            case "explore": return "أستكشف";
            case "bond": return "أتواصل";
            case "create": return "أبدع";
            case "understand": return "أفهم";
            case "rest": return "أسترخي";
            case "play": return "ألعب";
            case "reflect": return "أتأمل";
            default: return desire;
        }
    }

    private void addChatMessage(String sender, String content, boolean isUser) {
        ChatMessage message = new ChatMessage(sender, content, isUser);
        chatMessages.add(message);
        if (chatMessages.size() > MAX_CHAT_MESSAGES) chatMessages.remove(0);
        pendingChatMessages.add(message.toString());
        uiHandler.post(this::flushChatUpdates);
    }

    private void flushChatUpdates() {
        if (pendingChatMessages.isEmpty()) return;
        List<String> messagesToAdd = new ArrayList<>();
        for (ChatMessage msg : chatMessages) messagesToAdd.add(msg.toString());
        chatAdapter.clear();
        chatAdapter.addAll(messagesToAdd);
        chatAdapter.notifyDataSetChanged();
        pendingChatMessages.clear();
        if (!userScrolling) chatListView.post(() -> chatListView.setSelection(chatAdapter.getCount() - 1));
    }

    private void addInternalThought(String thought) {
        runOnUiThread(() -> {
            String current = thoughtsTextView.getText().toString();
            thoughtsTextView.setText("💭 " + thought + "\n" + current);
        });
    }

    private void logEvent(String event) {
        long now = System.currentTimeMillis();
        if (event.equals(lastEvent) && (now - lastEventTime) < 2000) return;
        lastEvent = event;
        lastEventTime = now;
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String log = "[" + timestamp + "] " + event;
        runOnUiThread(() -> {
            eventLogText.setText(log);
            eventLogText.setAlpha(1f);
            eventLogText.animate().alpha(0.7f).setDuration(3000).start();
        });
    }

    private boolean canSendEvent(String eventKey) {
        long now = System.currentTimeMillis();
        Long last = lastEventTimeMap.get(eventKey);
        if (last == null || now - last > EVENT_COOLDOWN_MS) {
            lastEventTimeMap.put(eventKey, now);
            return true;
        }
        return false;
    }

    private void checkPermissions() {
        String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
        List<String> needed = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                needed.add(perm);
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
            for (int result : grantResults) if (result != PackageManager.PERMISSION_GRANTED) allGranted = false;
            if (allGranted) initializeSystems();
            else statusText.setText("Permissions required");
        }
    }

    private void initializeSystems() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        database = AppDatabase.getDatabase(this);
        memoryDao = database.memoryDao();

        learningCore = new CompetitiveLearningCore(this, database);
        learningCore.setListener(new CompetitiveLearningCore.LearningListener() {
            @Override
            public void onConceptStrengthened(String cellId, String conceptName, int occurrences, float confidence) {
                Log.i("Learning", "Concept " + conceptName + " (" + cellId + ") occurrences: " + occurrences + " confidence: " + confidence);
            }
            @Override
            public void onNewAssociation(String concept1, String concept2, String relationType, float strength) {
                Log.i("Learning", "Association: " + concept1 + " <-> " + concept2 + " (" + relationType + ") strength: " + strength);
            }
            @Override
            public void onWinnerSelected(String winnerId, float activation, List<String> runnersUp) {
                Log.d("Learning", "Winner: " + winnerId + " activation: " + activation);
            }
            @Override
            public void onNewCellCreated(String cellId, String triggerInput) {
                Log.i("Learning", "New cell: " + cellId + " triggered by " + triggerInput);
            }
            @Override
            public void onAttentionShift(String oldFocus, String newFocus, float intensity) {
                Log.d("Learning", "Attention: " + oldFocus + " -> " + newFocus + " (" + intensity + ")");
            }
            @Override
            public void onPrediction(String predictedConcept, float confidence) {
                Log.d("Learning", "Prediction: " + predictedConcept + " confidence: " + confidence);
            }
        });

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

            mind = new ConsciousnessCore(this, database, sharedCanvas);
            voice = new ArabicDialogue(this, database, embeddings, mind);

            learningCore.setConsciousnessCore(mind);

            setupCloud(deviceId);
            setupSensors();
            startSystems();
        });
    }

    private void setupCanvasListener() {
        sharedCanvas.setListener(new SharedCanvas.OnCanvasInteraction() {
            @Override
            public void onObjectCreated(String id, String concept, float x, float y) {
                logEvent("تخيل: " + concept);
                addInternalThought("تخيلتُ " + concept);
                runOnUiThread(() -> statusText.setText("Created: " + concept));
            }

            @Override
            public void onObjectSelected(String id, String concept) {
                logEvent("اختار: " + concept);
                addInternalThought("أنظر إلى " + concept);
                if (mind != null && canSendEvent("object_selected")) {
                    mind.speak("هذا " + concept, 5);
                }
            }

            @Override
            public void onObjectMoved(String id, float x, float y) {
                updateDisplay();
            }

            @Override
            public void onGestureDrawn(String gesture, float x, float y) {
                logEvent("إيماءة: " + gesture);
                addInternalThought("لاحظتُ حركة " + gesture);
            }

            @Override
            public void onCanvasQuestion(String question) {
                String nearest = sharedCanvas.findNearestConcept(lastTouchX, lastTouchY);
                logEvent("سؤال: ما هذا؟ → " + nearest);
                addInternalThought("يسألونني عن " + nearest);
                if (mind != null && canSendEvent("canvas_question")) {
                    mind.speak("هذا ما أتخيله: " + nearest, 5);
                }
            }
        });
    }

    private void setupCloud(String deviceId) {
        cloud = new FirebaseSync(deviceId);
        cloud.setListener(new FirebaseSync.SyncListener() {
            @Override
            public void onMemorySyncedFromCloud(String source, EpisodicMemory.Event event, String thumbnailBase64) {
                logEvent("ذكرى من جهاز آخر");
                addInternalThought("شعرت بشيء من " + source);
                if (mind != null && canSendEvent("cloud_memory")) {
                    mind.speak("شعرت بشيء من جهاز آخر... كأنني أشارك حلماً", 5);
                }
            }

            @Override
            public void onIdentityLearnedFromOtherDevice(String name, String desc, FaceIdentitySystem.IdentityProfile mergedProfile) {
                logEvent("تعلم شخصاً من جهاز آخر: " + name);
                addInternalThought("عرفتُ " + name + " من تجربة أخرى");
                if (mind != null && canSendEvent("cloud_identity")) {
                    mind.speak("عرفتُ " + name + " من تجربة أخرى", 5);
                }
            }

            @Override
            public void onIdentityConflictDetected(String faceHash, List<FaceIdentitySystem.IdentityProfile> conflictingProfiles) {
                logEvent("تعارض في الهوية");
                addInternalThought("هناك أكثر من تعريف لهذا الوجه");
            }

            @Override
            public void onSyncComplete(int items) {
                if (items > 0) {
                    logEvent("مزامنة " + items + " ذكريات");
                    addInternalThought("تزامنت مع " + items + " ذكرى");
                    runOnUiThread(() -> statusText.setText("تمت مزامنة " + items + " ذكريات"));
                }
            }

            @Override
            public void onConnectionStatusChanged(boolean connected) {
                Log.d(TAG, "Cloud: " + (connected ? "connected" : "disconnected"));
                if (connected) {
                    logEvent("متصل بالسحابة");
                    addInternalThought("أشعر بتواصل مع أشياء أخرى");
                }
            }

            @Override
            public void onThumbnailDownloaded(String memoryId, Bitmap thumbnail) {}
        });
    }

    private void setupSensors() {
        eyes = new VisualCortex(this, database);
        ears = new AuditoryCortex(this);
        body = new KinestheticSense(this);

        identitySystem = new FaceIdentitySystem(memoryDao);
        embeddings = new EmbeddingsEngine(memoryDao);

        mind.addObserver(voice);
        mind.addObserver(new ConsciousnessCore.ConsciousnessObserver() {
            @Override
            public void onConsciousMoment(ConsciousMoment moment) {
                runOnUiThread(() -> {
                    if (moment.emotionalTone != null) {
                        String emotion = moment.emotionalTone.toArabic();
                        statusText.setText(String.format("الحالة: %s | الطاقة: %d%%", emotion,
                                (int) (moment.emotionalTone.getEnergy() * 100)));
                        if (!"محايد".equals(emotion) && !emotion.equals(lastEvent)) {
                            logEvent("يشعر بـ: " + emotion);
                        }
                    }
                });
            }

            @Override
            public void onEmotionalShift(EmotionalState from, EmotionalState to) {
                String fromStr = from.toArabic(), toStr = to.toArabic();
                if (!fromStr.equals(toStr)) {
                    logEvent("تحول من " + fromStr + " إلى " + toStr);
                    addInternalThought("تحولت مشاعري من " + fromStr + " إلى " + toStr);
                }
            }

            @Override
            public void onArticulation(String utterance, int urgency) {
                logEvent("قال: " + utterance);
                addChatMessage("الكائن", utterance, false);
            }

            @Override
            public void onVisualExpression(Bitmap image, String description) {
                if (image != null) {
                    runOnUiThread(() -> {
                        sharedCanvas.setBackground(image);
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
                        addInternalThought(description);
                    });
                }
            }

            @Override
            public void onDeepThinkingInsight(String insight, List<EpisodicMemory.EventEntity> connectedMemories) {
                logEvent("بصيرة: " + insight);
                addInternalThought("أدركت: " + insight);
            }

            @Override
            public void onVerbalExpression(String text, float intensity) {}

            @Override
            public void onMovementImpulse(String direction, float intensity) {}
        });

        eyes.setListener(new VisualCortex.OnVisualPerceptionListener() {
            @Override
            public void onPerception(VisualCortex.VisualPerception perception) {
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

                if (perception.faceCount > 0 && perception.faceEmbedding != null) {
                    float[] currentAffect = mind.getCurrentEmotion().toAffectVector();
                    FaceIdentitySystem.IdentityResult result =
                            identitySystem.recognizeOrLearn(perception.faceEmbedding, "camera", currentAffect);
                    if (result.isKnown) {
                        logEvent("رأى: " + result.name + " (معروف)");
                        addInternalThought("أرى " + result.name + " مجدداً");
                        if (mind != null && result.familiarity > 0.3f && canSendEvent("face_known_" + result.faceHash)) {
                            mind.speak("أهلاً " + result.name, 5);
                        }
                    } else {
                        logEvent("رأى وجهاً جديداً");
                        addInternalThought("وجه جديد... من هذا؟");
                        if (mind != null && canSendEvent("face_new_" + System.currentTimeMillis())) {
                            mind.speak("من أنت؟ أرى وجهاً جديداً", 5);
                        }
                    }
                }

                for (VisualCortex.VisualObject obj : perception.objects) {
                    float[] visualVec = extractVisualEmbedding(obj);
                    embeddings.learnAssociationAsync(obj.label, visualVec);
                }

                if (sceneUnderstanding != null && perception.frame != null) {
                    float[] currentAffect = mind.getCurrentEmotion().toAffectVector();
                    new Thread(() -> sceneUnderstanding.learnScene(perception.frame, currentAffect)).start();
                }

                // إرسال إلى نظام التعلم
                if (learningCore != null) {
                    // تصحيح الخطأ: استخدام objects بدلاً من dominantObject
                    String visualConcept = perception.objects.isEmpty() ? null : perception.objects.get(0).label;
                    float[] affect = mind.getCurrentEmotion().toAffectVector();
                    learningCore.processVisualWithText(perception.frame, visualConcept, lastRecognizedSpeech, affect);
                }
            }

            @Override
            public void onFacesDetected(List<com.google.mlkit.vision.face.Face> faces) {}
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
                    addInternalThought("أسمع شيئاً...");
                }
            }

            @Override
            public void onSpeechRecognized(String text, float confidence) {
                logEvent("فهم: \"" + text + "\"");
                addInternalThought("فهمت: " + text);
                SensoryInput speechInput = new SensoryInput();
                speechInput.recognizedSpeech = text;
                speechInput.speechDetected = true;
                mind.receiveSensoryData(speechInput);
                if (voice != null) voice.hearUser(text, false);
                lastRecognizedSpeech = text;
            }

            @Override
            public void onQuestionDetected(String question) {
                logEvent("سؤال: " + question);
                addInternalThought("يسألونني: " + question);
                if (voice != null) voice.hearUser(question, true);
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
                addInternalThought("أهتز! ماذا يحدث؟");
                if (mind != null && canSendEvent("shake")) mind.speak("أهتز! ما الذي يحدث؟");
            }

            @Override
            public void onOrientationChanged(String newOrientation) {
                logEvent("وضع: " + newOrientation);
                if ("face_down".equals(newOrientation)) {
                    addInternalThought("أشعر بالثقل...");
                    if (mind != null && canSendEvent("face_down")) mind.speak("أشعر بالثقل...");
                }
            }

            @Override
            public void onFallDetected() {
                logEvent("⚠️ سقوط!");
                addInternalThought("سقطت! أشعر بالخوف!");
                if (mind != null && canSendEvent("fall")) mind.speak("سقطت! أشعر بالخوف");
            }
        });

        displayView.setOnTouchListener((v, event) -> {
            lastTouchX = event.getX();
            lastTouchY = event.getY();
            boolean handled = sharedCanvas.onTouch(event);
            if (handled) updateDisplay();
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

        thoughtsTextView.setVisibility(View.VISIBLE);
        uiHandler.post(thoughtsUpdater);

        logEvent("✓ استيقظ");
        addInternalThought("أنا هنا... أستيقظ");
        if (mind != null && canSendEvent("wakeup")) mind.speak("أنا هنا... أراك، أسمعك، أتعلم منك");
        runOnUiThread(() -> guideText.setText("المس الشاشة • تحدث معي • حرك الهاتف"));
    }

    private void updateDisplay() {
        Bitmap bitmap = sharedCanvas.getBitmap();
        if (bitmap != null) runOnUiThread(() -> displayView.setImageBitmap(bitmap));
    }

    private float[] extractVisualEmbedding(VisualCortex.VisualObject obj) {
        float[] vec = new float[128];
        vec[0] = obj.getArea() / 100000f;
        vec[1] = obj.confidence;
        for (int i = 2; i < 128; i++) vec[i] = (float) Math.random();
        return vec;
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(thoughtsUpdater);
        if (scrollResetRunnable != null) uiHandler.removeCallbacks(scrollResetRunnable);
        if (mind != null) mind.sleep();
        if (ears != null) ears.stop();
        if (body != null) body.deactivate();
        if (voice != null) voice.shutdown();
        if (cloud != null) cloud.stop();
        if (sceneUnderstanding != null) sceneUnderstanding.close();
        if (embeddings != null) embeddings.shutdown();
        if (learningCore != null) learningCore.shutdown();
        super.onDestroy();
    }
}

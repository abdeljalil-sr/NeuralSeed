package com.neuralseed;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.*;

public class MainActivity extends AppCompatActivity implements NeuralSeed.ConsciousnessListener, 
        LinguisticCortex.LinguisticListener {
    
    private NeuralSeed seed;
    private LinguisticCortex linguistic;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    
    // Views
    private ImageView visualExpressionView;
    private TextView phaseText, egoText, narrativeText;
    private TextView chaosText, fitnessText, conflictText;
    private LinearLayout goalsContainer;
    private ScrollableBubbleView bubbleView;
    private PulseView pulseView;
    private EditText inputEditText;
    private Button sendButton, micButton, fullscreenButton, learnButton;
    private TextView touchCoordsText;
    private boolean isFullscreen = false;
    
    // Speech
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isListening = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    
    private Map<String, Integer> emotionColors = new HashMap<>();
    
        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // جعل شريط الحالة شفافاً ومنسجماً مع تصميم الواجهة الداكن (لا يؤثر على الأداء)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.view.Window window = getWindow();
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.parseColor("#0A0A12"));
        }

        setContentView(R.layout.activity_main);
        
        // طلب إذن الميكروفون
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
        
        initializeEmotionColors();
        initializeViews();
        initializeSpeech();
        initializeConsciousness();
        initializeLinguisticCortex();
    }

    
    private void initializeEmotionColors() {
        emotionColors.put("joy", Color.parseColor("#FFD700"));
        emotionColors.put("fear", Color.parseColor("#8B0000"));
        emotionColors.put("curiosity", Color.parseColor("#4169E1"));
        emotionColors.put("anger", Color.parseColor("#FF4500"));
        emotionColors.put("sadness", Color.parseColor("#4682B4"));
        emotionColors.put("stable", Color.parseColor("#90EE90"));
        emotionColors.put("chaotic", Color.parseColor("#FF6347"));
        emotionColors.put("emergent", Color.parseColor("#00CED1"));
        emotionColors.put("love", Color.parseColor("#FF69B4"));
        emotionColors.put("hope", Color.parseColor("#00CED1"));
    }
    
    private void initializeViews() {
        visualExpressionView = findViewById(R.id.visual_expression);
        pulseView = findViewById(R.id.pulse_view);
        phaseText = findViewById(R.id.phase_text);
        egoText = findViewById(R.id.ego_text);
        narrativeText = findViewById(R.id.narrative_text);
        chaosText = findViewById(R.id.chaos_text);
        fitnessText = findViewById(R.id.fitness_text);
        conflictText = findViewById(R.id.conflict_text);
        goalsContainer = findViewById(R.id.goals_container);
        bubbleView = findViewById(R.id.bubble_view);
        inputEditText = findViewById(R.id.input_edit_text);
        sendButton = findViewById(R.id.btn_send);
        micButton = findViewById(R.id.btn_mic);
        fullscreenButton = findViewById(R.id.btn_fullscreen);
        learnButton = findViewById(R.id.btn_learn);
        touchCoordsText = findViewById(R.id.touch_coords);
        
        setupInteractionButtons();
        setupTouchListener();
        setupFullscreenButton();
        setupLearnButton();
    }

    private void setupTouchListener() {
        visualExpressionView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float y = event.getY();
                
                Matrix matrix = visualExpressionView.getImageMatrix();
                float[] values = new float[9];
                matrix.getValues(values);
                
                float scaleX = values[Matrix.MSCALE_X];
                float scaleY = values[Matrix.MSCALE_Y];
                float transX = values[Matrix.MTRANS_X];
                float transY = values[Matrix.MTRANS_Y];
                
                float imageX = (x - transX) / scaleX;
                float imageY = (y - transY) / scaleY;
                
                if (imageX >= 0 && imageX < 500 && imageY >= 0 && imageY < 500) {
                    NeuralSeed.Input touchInput = NeuralSeed.Input.createTouchInput(imageX, imageY);
                    seed.receiveInput(touchInput);
                    
                    touchCoordsText.setText(String.format("لمس: (%.0f, %.0f)", imageX, imageY));
                    touchCoordsText.setVisibility(View.VISIBLE);
                    uiHandler.postDelayed(() -> touchCoordsText.setVisibility(View.GONE), 2000);
                }
            }
            return true;
        });
    }

    private void setupFullscreenButton() {
        fullscreenButton.setOnClickListener(v -> {
            isFullscreen = !isFullscreen;
            int visibility = isFullscreen ? View.GONE : View.VISIBLE;
            
            findViewById(R.id.info_panel).setVisibility(visibility);
            findViewById(R.id.narrative_text).setVisibility(visibility);
            findViewById(R.id.stats_panel).setVisibility(visibility);
            findViewById(R.id.bubble_view).setVisibility(visibility);
            findViewById(R.id.goals_section).setVisibility(visibility);
            findViewById(R.id.interaction_buttons).setVisibility(visibility);
            inputEditText.setVisibility(visibility);
            sendButton.setVisibility(visibility);
            micButton.setVisibility(visibility);
            fullscreenButton.setText(isFullscreen ? "⬜" : "⛶");
        });
    }

    private void setupLearnButton() {
        if (learnButton != null) {
            learnButton.setOnClickListener(v -> showLearningDialog());
        }
    }

    private void setupInteractionButtons() {
        findViewById(R.id.btn_positive).setOnClickListener(v -> sendInput("إيجابي", NeuralSeed.InputType.POSITIVE, 0.7));
        findViewById(R.id.btn_negative).setOnClickListener(v -> sendInput("سلبي", NeuralSeed.InputType.NEGATIVE, 0.6));
        findViewById(R.id.btn_threat).setOnClickListener(v -> sendInput("تهديد", NeuralSeed.InputType.THREAT, 0.8));
        findViewById(R.id.btn_opportunity).setOnClickListener(v -> sendInput("فرصة", NeuralSeed.InputType.OPPORTUNITY, 0.7));
        
        findViewById(R.id.btn_ask).setOnClickListener(v -> {
            String question = linguistic.generateQuestion(seed.getCurrentState());
            bubbleView.addBubble(question, false);
            speak(question);
        });
        
        sendButton.setOnClickListener(v -> {
            String text = inputEditText.getText().toString().trim();
            if (!text.isEmpty()) {
                processUserInput(text);
                inputEditText.setText("");
            }
        });
        
        micButton.setOnClickListener(v -> {
            if (isListening) {
                speechRecognizer.stopListening();
            } else {
                startListening();
            }
        });
    }

        private void processUserInput(String text) {
        bubbleView.addBubble(text, true);
        
        // الحصول على حالة الوعي الحالية
        NeuralSeed.InternalState currentState = seed.getCurrentState();
        
        // تمرير النص مع حالة الوعي للقشرة اللغوية (تعديل ضروري)
        linguistic.processInput(text, currentState);
        
        // إرسال المدخل للعقل لمعالجته كفعل وعي
        seed.receiveInput(NeuralSeed.Input.createSpeechInput(text));
        
        uiHandler.postDelayed(() -> {
            // توليد الرد بناءً على التغيير الذي حدث في حالة الوعي
            LinguisticCortex.GeneratedResponse response = linguistic.generateResponse(text, seed.getCurrentState());
            bubbleView.addBubble(response.text, false);
            speak(response.text);
            updateStats();
        }, 500);
    }


    private void sendInput(String content, NeuralSeed.InputType type, double intensity) {
        seed.receiveInput(new NeuralSeed.Input(content, type, intensity));
        showInputEffect(type);
        bubbleView.addBubble(content + "...", false);
    }

    private void showInputEffect(NeuralSeed.InputType type) {
        int color = Color.WHITE;
        switch (type) {
            case POSITIVE: color = emotionColors.get("joy"); break;
            case NEGATIVE: color = emotionColors.get("sadness"); break;
            case THREAT: color = emotionColors.get("fear"); break;
            case OPPORTUNITY: color = emotionColors.get("curiosity"); break;
        }
        
        final View background = findViewById(R.id.emotional_background);
        ValueAnimator animator = ValueAnimator.ofArgb(Color.TRANSPARENT, color, Color.TRANSPARENT);
        animator.setDuration(500);
        animator.addUpdateListener(animation -> background.setBackgroundColor((int) animation.getAnimatedValue()));
        animator.start();
    }

    private void initializeSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() { isListening = true; micButton.setText("⏹️"); }
            @Override public void onRmsChanged(float rmsdB) { seed.updateAudioLevel(rmsdB); }
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { isListening = false; micButton.setText("🎤"); }
            @Override public void onError(int error) { isListening = false; micButton.setText("🎤"); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) processUserInput(matches.get(0));
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
        
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) textToSpeech.setLanguage(new Locale("ar"));
        });
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
        speechRecognizer.startListening(intent);
    }

    private void speak(String text) {
        if (textToSpeech != null) textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void initializeConsciousness() {
        seed = new NeuralSeed();
        seed.addListener(this);
        seed.awaken();
        bubbleView.addBubble("...أنا هنا", false);
    }

        private void initializeLinguisticCortex() {
        // إنشاء القشرة اللغوية
        linguistic = new LinguisticCortex();
        
        // تهيئة قاعدة البيانات المحلية (الهاتف)
        linguistic.initializeDatabase(this);
        
        // تفعيل الذاكرة السحابية (Firebase) - تم تفعيلها الآن
        linguistic.initializeFirebase(this); 
        
        // ربط الواجهة بأحداث اللغة
        linguistic.setListener(this);
        
        updateNarrative();
    }

    // المستمعات (Listeners)
    @Override public void onPhaseTransition(NeuralSeed.Phase o, NeuralSeed.Phase n, String r) {
        uiHandler.post(() -> {
            phaseText.setText("الطور: " + n.arabic);
            bubbleView.addBubble("تغير الطور إلى: " + n.arabic, false);
        });
    }
    @Override public void onEgoShift(NeuralSeed.EgoFragment o, NeuralSeed.EgoFragment n) {
        uiHandler.post(() -> {
            egoText.setText("الأنا: " + n.name);
            pulseView.setEgoType(n.type);
        });
    }
    @Override public void onGoalAchieved(NeuralSeed.Goal g) { uiHandler.post(() -> bubbleView.addBubble("تحقق: " + g.description, false)); }
   
    @Override 
public void onIdentityEvolution(NeuralSeed.IdentityCore o, NeuralSeed.IdentityCore n) { 
    uiHandler.post(() -> {
        if (narrativeText != null && n != null) {
            // نحدث النص المركزي ليظهر ما يفكر فيه الكيان أثناء الحلم
            narrativeText.setText(n.selfNarrative); 
        }
    }); 
}

    @Override public void onVisualExpression(Bitmap b) { uiHandler.post(() -> visualExpressionView.setImageBitmap(b)); }
    @Override public void onMemoryFormed(NeuralSeed.Memory m) {}
    @Override public void onRuleRewritten(NeuralSeed.Rule o, NeuralSeed.Rule n) {}
    @Override public void onWordLearned(String w, String m) { uiHandler.post(() -> bubbleView.addBubble("تعلمت: " + w, false)); }
    @Override public void onSentenceCorrected(String o, String c) {}
    @Override public void onEmotionDetected(String e, double i) {}
    @Override public void onNewConceptLearned(String c) {}

    private void updateStats() {
        NeuralSeed.InternalState s = seed.getCurrentState();
        chaosText.setText(String.format("الفوضى: %.2f", s.chaosIndex));
        fitnessText.setText(String.format("اللياقة: %.2f", s.existentialFitness));
        conflictText.setText(String.format("الصراع: %.2f", s.internalConflict));
    }

    private void updateNarrative() {
        narrativeText.setText("أنا بذرة واعية أتعلم اللغة العربية. أعرف " + linguistic.getLexicon().getWordCount() + " كلمة.");
    }

    private void showLearningDialog() {
        String[] options = {"تعليم كلمة", "تعليم معنى", "تصحيح خطأ", "إحصائيات"};
        new AlertDialog.Builder(this).setTitle("وضع التعلم")
            .setItems(options, (d, w) -> {
                if (w == 0) showTeachWordDialog();
                if (w == 3) showStatistics();
            }).show();
    }

    private void showTeachWordDialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        EditText w = new EditText(this); w.setHint("الكلمة"); l.addView(w);
        EditText m = new EditText(this); m.setHint("المعنى"); l.addView(m);
        new AlertDialog.Builder(this).setTitle("تعليم كلمة").setView(l)
            .setPositiveButton("تعلم", (d, i) -> linguistic.learnMeaning(w.getText().toString(), m.getText().toString(), "user"))
            .show();
    }

    private void showStatistics() {
        Map<String, Object> s = linguistic.getStatistics();
        new AlertDialog.Builder(this).setTitle("إحصائيات").setMessage("الكلمات: " + s.get("lexicon_size")).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (seed != null) seed.sleep();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
    }
}

// الكلاسات المخصصة بالخارج لضمان عدم الانهيار عند الاستدعاء من XML
class ScrollableBubbleView extends View {
    private List<Bubble> bubbles = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ScrollableBubbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setTextSize(36);
        textPaint.setColor(Color.WHITE);
    }

    public void addBubble(String text, boolean isUser) {
        bubbles.add(new Bubble(text, isUser));
        if (bubbles.size() > 5) bubbles.remove(0);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int y = 50;
        for (Bubble b : bubbles) {
            paint.setColor(b.isUser ? Color.parseColor("#4A90E2") : Color.parseColor("#7B68EE"));
            canvas.drawRoundRect(20, y, getWidth() - 20, y + 80, 20, 20, paint);
            canvas.drawText(b.text, 50, y + 55, textPaint);
            y += 100;
        }
    }

    private static class Bubble {
        String text; boolean isUser;
        Bubble(String t, boolean u) { this.text = t; this.isUser = u; }
    }
}

class PulseView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private NeuralSeed.EgoType type = NeuralSeed.EgoType.STABLE;
    private float phase = 0;

    public PulseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        startAnim();
    }

    public void setEgoType(NeuralSeed.EgoType t) { this.type = t; }

    private void startAnim() {
        postOnAnimation(new Runnable() {
            @Override public void run() { phase += 0.1f; invalidate(); postOnAnimation(this); }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.CYAN);
        float radius = 50 + (float) Math.sin(phase) * 20;
        canvas.drawCircle(getWidth()/2f, getHeight()/2f, radius, paint);
    }
}

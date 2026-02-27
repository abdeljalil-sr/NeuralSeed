package com.neuralseed;


import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.animation.ValueAnimator;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.*;
import android.widget.*;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.*;

public class MainActivity extends Activity implements NeuralSeed.ConsciousnessListener, 
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
    
    // Emotion colors
    private Map<String, Integer> emotionColors = new HashMap<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Check permissions
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
        View emotionalBackground = findViewById(R.id.emotional_background);
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
            if (isFullscreen) {
                findViewById(R.id.info_panel).setVisibility(View.GONE);
                findViewById(R.id.narrative_text).setVisibility(View.GONE);
                findViewById(R.id.stats_panel).setVisibility(View.GONE);
                findViewById(R.id.bubble_view).setVisibility(View.GONE);
                findViewById(R.id.goals_section).setVisibility(View.GONE);
                findViewById(R.id.interaction_buttons).setVisibility(View.GONE);
                inputEditText.setVisibility(View.GONE);
                sendButton.setVisibility(View.GONE);
                micButton.setVisibility(View.GONE);
                fullscreenButton.setText("⬜");
            } else {
                findViewById(R.id.info_panel).setVisibility(View.VISIBLE);
                findViewById(R.id.narrative_text).setVisibility(View.VISIBLE);
                findViewById(R.id.stats_panel).setVisibility(View.VISIBLE);
                findViewById(R.id.bubble_view).setVisibility(View.VISIBLE);
                findViewById(R.id.goals_section).setVisibility(View.VISIBLE);
                findViewById(R.id.interaction_buttons).setVisibility(View.VISIBLE);
                inputEditText.setVisibility(View.VISIBLE);
                sendButton.setVisibility(View.VISIBLE);
                micButton.setVisibility(View.VISIBLE);
                fullscreenButton.setText("⛶");
            }
        });
    }
    
    private void setupLearnButton() {
        if (learnButton != null) {
            learnButton.setOnClickListener(v -> showLearningDialog());
        }
    }
    
    private void setupInteractionButtons() {
        Button positiveBtn = findViewById(R.id.btn_positive);
        Button negativeBtn = findViewById(R.id.btn_negative);
        Button threatBtn = findViewById(R.id.btn_threat);
        Button opportunityBtn = findViewById(R.id.btn_opportunity);
        Button askBtn = findViewById(R.id.btn_ask);
        
        positiveBtn.setOnClickListener(v -> sendInput("تجربة إيجابية", NeuralSeed.InputType.POSITIVE, 0.7));
        negativeBtn.setOnClickListener(v -> sendInput("تجربة سلبية", NeuralSeed.InputType.NEGATIVE, 0.6));
        threatBtn.setOnClickListener(v -> sendInput("تهديد محتمل", NeuralSeed.InputType.THREAT, 0.8));
        opportunityBtn.setOnClickListener(v -> sendInput("فرصة جديدة", NeuralSeed.InputType.OPPORTUNITY, 0.7));
        
        askBtn.setOnClickListener(v -> {
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
                isListening = false;
                micButton.setText("🎤");
            } else {
                startListening();
                micButton.setText("⏹️");
            }
        });
    }
    
    private void processUserInput(String text) {
        // عرض رسالة المستخدم
        bubbleView.addBubble(text, true);
        
        // معالجة المدخل اللغوي
        LinguisticCortex.ProcessedInput processed = linguistic.processInput(text);
        
        // إرسال كمدخل للوعي
        NeuralSeed.Input input = NeuralSeed.Input.createSpeechInput(text);
        seed.receiveInput(input);
        
        // توليد رد
        uiHandler.postDelayed(() -> {
            LinguisticCortex.GeneratedResponse response = 
                linguistic.generateResponse(text, seed.getCurrentState());
            
            bubbleView.addBubble(response.text, false);
            speak(response.text);
            
            // تحديث الإحصائيات
            updateStats();
        }, 500);
    }
    
    private void sendInput(String content, NeuralSeed.InputType type, double intensity) {
        NeuralSeed.Input input = new NeuralSeed.Input(content, type, intensity);
        seed.receiveInput(input);
        showInputEffect(type);
        
        String message = "";
        switch (type) {
            case POSITIVE: message = "شكراً... أشعر بشيء إيجابي"; break;
            case NEGATIVE: message = "هذا صعب... لكنني أتعلم"; break;
            case THREAT: message = "أنا متأهب..."; break;
            case OPPORTUNITY: message = "مثير للاهتمام!"; break;
            case SPEECH: message = "سمعتك..."; break;
            default: message = "...";
        }
        
        bubbleView.addBubble(message, false);
    }
    
    private void showInputEffect(NeuralSeed.InputType type) {
        int color;
        switch (type) {
            case POSITIVE: color = emotionColors.get("joy"); break;
            case NEGATIVE: color = emotionColors.get("sadness"); break;
            case THREAT: color = emotionColors.get("fear"); break;
            case OPPORTUNITY: color = emotionColors.get("curiosity"); break;
            default: color = Color.WHITE;
        }
        
        final View emotionalBackground = findViewById(R.id.emotional_background);
        ValueAnimator animator = ValueAnimator.ofArgb(Color.TRANSPARENT, color, Color.TRANSPARENT);
        animator.setDuration(500);
        animator.addUpdateListener(animation -> {
            emotionalBackground.setBackgroundColor((int) animation.getAnimatedValue());
        });
        animator.start();
    }
    
    // ===== Speech =====
    
    private void initializeSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}
            
            @Override
            public void onBeginningOfSpeech() {}
            
            @Override
            public void onRmsChanged(float rmsdB) {
                seed.updateAudioLevel(rmsdB);
            }
            
            @Override
            public void onBufferReceived(byte[] buffer) {}
            
            @Override
            public void onEndOfSpeech() {
                isListening = false;
                micButton.setText("🎤");
            }
            
            @Override
            public void onError(int error) {
                isListening = false;
                micButton.setText("🎤");
            }
            
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && matches.size() > 0) {
                    String spokenText = matches.get(0);
                    inputEditText.setText(spokenText);
                    processUserInput(spokenText);
                }
            }
            
            @Override
            public void onPartialResults(Bundle partialResults) {}
            
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
        
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("ar"));
            }
        });
    }
    
    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث إلى كيانك...");
        speechRecognizer.startListening(intent);
        isListening = true;
    }
    
    private void speak(String text) {
        if (textToSpeech != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    
    // ===== Consciousness =====
    
    private void initializeConsciousness() {
        seed = new NeuralSeed();
        seed.addListener(this);
        seed.awaken();
        bubbleView.addBubble("...أنا هنا", false);
    }
    
    private void initializeLinguisticCortex() {
        linguistic = new LinguisticCortex();
        linguistic.initializeDatabase(this);
        linguistic.initializeFirebase(this);
        linguistic.setListener(this);
        
        // تحديث السرد الذاتي
        updateNarrative();
    }
    
    // ===== ConsciousnessListener =====
    
    @Override
    public void onPhaseTransition(NeuralSeed.Phase oldPhase, NeuralSeed.Phase newPhase, String reason) {
        uiHandler.post(() -> {
            phaseText.setText("الطور: " + newPhase.arabic);
            phaseText.setTextColor(getPhaseTextColor(newPhase));
            
            String message = "أشعر بشيء يتغير... " + newPhase.arabic;
            bubbleView.addBubble(message, false);
            speak(message);
        });
    }
    
    @Override
    public void onEgoShift(NeuralSeed.EgoFragment oldDominant, NeuralSeed.EgoFragment newDominant) {
        uiHandler.post(() -> {
            egoText.setText("الأنا: " + newDominant.name);
            String message = "أصبحت " + newDominant.name + " الآن";
            bubbleView.addBubble(message, false);
            pulseView.setEgoType(newDominant.type);
            speak(message);
        });
    }
    
    @Override
    public void onGoalAchieved(NeuralSeed.Goal goal) {
        uiHandler.post(() -> {
            String message = "حققت هدفي: " + goal.description;
            bubbleView.addBubble(message, false);
            speak(message);
            updateGoalsDisplay();
        });
    }
    
    @Override
    public void onIdentityEvolution(NeuralSeed.IdentityCore oldIdentity, NeuralSeed.IdentityCore newIdentity) {
        uiHandler.post(() -> {
            narrativeText.setText(newIdentity.selfNarrative);
            String message = "أشعر أنني أتغير... " + newIdentity.selfNarrative;
            bubbleView.addBubble(message, false);
            speak(message);
        });
    }
    
    @Override
    public void onVisualExpression(Bitmap expression) {
        uiHandler.post(() -> {
            visualExpressionView.setImageBitmap(expression);
        });
    }
    
    @Override
    public void onMemoryFormed(NeuralSeed.Memory memory) {
        uiHandler.post(() -> {
            // تحديث الذاكرة
        });
    }
    
    @Override
    public void onRuleRewritten(NeuralSeed.Rule oldRule, NeuralSeed.Rule newRule) {
        uiHandler.post(() -> {
            bubbleView.addBubble("تعلمت قاعدة جديدة...", false);
        });
    }
    
    // ===== LinguisticListener =====
    
    @Override
    public void onWordLearned(String word, String meaning) {
        uiHandler.post(() -> {
            String message = "تعلمت كلمة جديدة: " + word + " = " + meaning;
            bubbleView.addBubble(message, false);
        });
    }
    
    @Override
    public void onSentenceCorrected(String original, String corrected) {
        uiHandler.post(() -> {
            String message = "فهمت التصحيح: " + original + " -> " + corrected;
            bubbleView.addBubble(message, false);
        });
    }
    
    @Override
    public void onEmotionDetected(String emotion, double intensity) {
        uiHandler.post(() -> {
            // تحديث اللون بناءً على العاطفة
            Integer color = emotionColors.get(emotion);
            if (color != null) {
                final View emotionalBackground = findViewById(R.id.emotional_background);
                emotionalBackground.setBackgroundColor(color);
            }
        });
    }
    
    @Override
    public void onNewConceptLearned(String concept) {
        uiHandler.post(() -> {
            String message = "تعلمت مفهوماً جديداً: " + concept;
            bubbleView.addBubble(message, false);
        });
    }
    
    // ===== UI Updates =====
    
    private void updateStats() {
        NeuralSeed.InternalState state = seed.getCurrentState();
        
        chaosText.setText(String.format("الفوضى: %.2f", state.chaosIndex));
        fitnessText.setText(String.format("اللياقة: %.2f", state.existentialFitness));
        conflictText.setText(String.format("الصراع: %.2f", state.internalConflict));
        
        updateGoalsDisplay();
    }
    
    private void updateGoalsDisplay() {
        goalsContainer.removeAllViews();
        List<NeuralSeed.Goal> goals = seed.getGoals();
        
        for (NeuralSeed.Goal goal : goals) {
            View goalView = createGoalView(goal);
            goalsContainer.addView(goalView);
        }
    }
    
    private View createGoalView(NeuralSeed.Goal goal) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 5, 10, 5);
        
        TextView textView = new TextView(this);
        textView.setText(goal.description);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(14);
        layout.addView(textView);
        
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setProgress((int) (goal.progress * 100));
        layout.addView(progressBar);
        
        return layout;
    }
    
    private void updateNarrative() {
        // تحديث السرد بناءً على المعجم
        int wordCount = linguistic.getLexicon().getWordCount();
        narrativeText.setText("أنا بذرة واعية أتعلم اللغة العربية. أعرف " + wordCount + " كلمة حتى الآن.");
    }
    
    private int getPhaseTextColor(NeuralSeed.Phase phase) {
        switch (phase) {
            case CHAOTIC: return emotionColors.get("chaotic");
            case STABLE: return emotionColors.get("stable");
            case EMERGENT: return emotionColors.get("emergent");
            case COLLAPSING: return Color.RED;
            default: return Color.WHITE;
        }
    }
    
    // ===== Learning Dialog =====
    
    private void showLearningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("وضع التعلم");
        
        String[] options = {
            "تعليم كلمة جديدة",
            "تعليم معنى",
            "تعليم عاطفة",
            "عرض الإحصائيات",
            "تصحيح خطأ",
            "مزامنة مع السحابة"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: showTeachWordDialog(); break;
                case 1: showTeachMeaningDialog(); break;
                case 2: showTeachEmotionDialog(); break;
                case 3: showStatistics(); break;
                case 4: showCorrectionDialog(); break;
                case 5: syncWithCloud(); break;
            }
        });
        
        builder.show();
    }
    
    private void showTeachWordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تعليم كلمة");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        EditText wordInput = new EditText(this);
        wordInput.setHint("الكلمة");
        layout.addView(wordInput);
        
        EditText meaningInput = new EditText(this);
        meaningInput.setHint("المعنى");
        layout.addView(meaningInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("تعلم", (dialog, which) -> {
            String word = wordInput.getText().toString().trim();
            String meaning = meaningInput.getText().toString().trim();
            
            if (!word.isEmpty() && !meaning.isEmpty()) {
                linguistic.learnMeaning(word, meaning, "user_taught");
                Toast.makeText(this, "تم التعلم!", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private void showTeachMeaningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تعليم معنى");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        EditText conceptInput = new EditText(this);
        conceptInput.setHint("المفهوم");
        layout.addView(conceptInput);
        
        EditText definitionInput = new EditText(this);
        definitionInput.setHint("التعريف");
        layout.addView(definitionInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("تعلم", (dialog, which) -> {
            String concept = conceptInput.getText().toString().trim();
            String definition = definitionInput.getText().toString().trim();
            
            if (!concept.isEmpty() && !definition.isEmpty()) {
                SemanticEmotionalEngine.Meaning meaning = 
                    new SemanticEmotionalEngine.Meaning(concept, definition);
                linguistic.getEmotionEngine();
                Toast.makeText(this, "تم التعلم!", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private void showTeachEmotionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تعليم عاطفة");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        EditText wordInput = new EditText(this);
        wordInput.setHint("الكلمة");
        layout.addView(wordInput);
        
        EditText emotionInput = new EditText(this);
        emotionInput.setHint("العاطفة (مثال: joy, sadness)");
        layout.addView(emotionInput);
        
        SeekBar intensityBar = new SeekBar(this);
        intensityBar.setMax(100);
        intensityBar.setProgress(50);
        layout.addView(intensityBar);
        
        builder.setView(layout);
        
        builder.setPositiveButton("تعلم", (dialog, which) -> {
            String word = wordInput.getText().toString().trim();
            String emotion = emotionInput.getText().toString().trim();
            double intensity = intensityBar.getProgress() / 100.0;
            
            if (!word.isEmpty() && !emotion.isEmpty()) {
                linguistic.learnWordEmotion(word, emotion, intensity);
                Toast.makeText(this, "تم التعلم!", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private void showStatistics() {
        Map<String, Object> stats = linguistic.getStatistics();
        
        StringBuilder message = new StringBuilder();
        message.append("إحصائيات التعلم:\n\n");
        message.append("حجم المعجم: ").append(stats.get("lexicon_size")).append(" كلمة\n");
        message.append("مستوى التعلم: ").append(stats.get("learning_level")).append("\n");
        
        if (stats.containsKey("word_count")) {
            message.append("الكلمات المحفوظة: ").append(stats.get("word_count")).append("\n");
        }
        if (stats.containsKey("conversation_count")) {
            message.append("المحادثات: ").append(stats.get("conversation_count")).append("\n");
        }
        if (stats.containsKey("pending_corrections")) {
            message.append("التصحيحات المعلقة: ").append(stats.get("pending_corrections")).append("\n");
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("الإحصائيات");
        builder.setMessage(message.toString());
        builder.setPositiveButton("موافق", null);
        builder.show();
    }
    
    private void showCorrectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تصحيح");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        EditText originalInput = new EditText(this);
        originalInput.setHint("النص الخاطئ");
        layout.addView(originalInput);
        
        EditText correctedInput = new EditText(this);
        correctedInput.setHint("التصحيح");
        layout.addView(correctedInput);
        
        EditText explanationInput = new EditText(this);
        explanationInput.setHint("الشرح (اختياري)");
        layout.addView(explanationInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("تعلم", (dialog, which) -> {
            String original = originalInput.getText().toString().trim();
            String corrected = correctedInput.getText().toString().trim();
            String explanation = explanationInput.getText().toString().trim();
            
            if (!original.isEmpty() && !corrected.isEmpty()) {
                boolean learned = linguistic.learnFromCorrection(original, corrected, explanation);
                if (learned) {
                    Toast.makeText(this, "تم التعلم من التصحيح!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private void syncWithCloud() {
        if (linguistic.getFirebaseManager() != null) {
            linguistic.getFirebaseManager().syncWithLocal(linguistic.getDatabase());
            Toast.makeText(this, "جاري المزامنة...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Firebase غير مهيأ", Toast.LENGTH_SHORT).show();
        }
    }
    
    // ===== Lifecycle =====
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (seed != null) {
            seed.sleep();
        }
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // تم منح الإذن
            } else {
                Toast.makeText(this, "الإذن مطلوب للتسجيل الصوتي", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // ===== Custom Views =====
    
    public static class ScrollableBubbleView extends View {
        private List<Bubble> bubbles = new ArrayList<>();
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int maxBubbles = 10;
        
        public ScrollableBubbleView(Context context) {
            super(context);
            init();
        }
        
        public ScrollableBubbleView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }
        
        private void init() {
            textPaint.setTextSize(36);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.WHITE);
        }
        
        public void addBubble(String text, boolean isUser) {
            Bubble bubble = new Bubble(text, isUser, System.currentTimeMillis());
            bubbles.add(bubble);
            if (bubbles.size() > maxBubbles) {
                bubbles.remove(0);
            }
            invalidate();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            int y = 20;
            int padding = 20;
            int bubblePadding = 30;
            
            for (int i = bubbles.size() - 1; i >= 0; i--) {
                Bubble bubble = bubbles.get(i);
                
                Rect textBounds = new Rect();
                textPaint.getTextBounds(bubble.text, 0, bubble.text.length(), textBounds);
                
                int bubbleWidth = textBounds.width() + bubblePadding * 2;
                int bubbleHeight = textBounds.height() + bubblePadding * 2;
                
                float x = bubble.isUser ? getWidth() - bubbleWidth - padding : padding;
                int bgColor = bubble.isUser ? Color.parseColor("#4A90E2") : Color.parseColor("#7B68EE");
                
                paint.setColor(bgColor);
                canvas.drawRoundRect(x, y, x + bubbleWidth, y + bubbleHeight, 20, 20, paint);
                canvas.drawText(bubble.text, x + bubblePadding, y + bubbleHeight - bubblePadding, textPaint);
                
                y += bubbleHeight + 15;
            }
        }
        
        private static class Bubble {
            String text;
            boolean isUser;
            long timestamp;
            
            Bubble(String text, boolean isUser, long timestamp) {
                this.text = text;
                this.isUser = isUser;
                this.timestamp = timestamp;
            }
        }
    }
    
    public static class PulseView extends View {
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
                case STABLE:
                    color = Color.parseColor("#90EE90");
                    speed = 0.5f;
                    break;
                case CHAOTIC:
                    color = Color.parseColor("#FF6347");
                    speed = 2.0f;
                    break;
                case ADAPTIVE:
                    color = Color.parseColor("#FFD700");
                    speed = 1.0f;
                    break;
                case SURVIVAL:
                    color = Color.parseColor("#FF0000");
                    speed = 3.0f;
                    break;
                default:
                    color = Color.WHITE;
                    speed = 1.0f;
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
}

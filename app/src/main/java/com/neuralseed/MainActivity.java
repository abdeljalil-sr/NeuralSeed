package com.neuralseed;

import android.util.Log;
import android.Manifest;
import android.animation.ValueAnimator;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private LinearLayoutManager layoutManager;
    private PulseView pulseView;
    private EditText inputEditText;
    private Button sendButton;
    private ImageButton micButton, fullscreenButton;
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
        
        // إعداد RecyclerView للمحادثة
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        chatAdapter = new ChatAdapter();
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        inputEditText = findViewById(R.id.input_edit_text);
        sendButton = findViewById(R.id.btn_send);
        micButton = findViewById(R.id.btn_mic);
        fullscreenButton = findViewById(R.id.btn_fullscreen);
        touchCoordsText = findViewById(R.id.touch_coords);
        
        setupInteractionButtons();
        setupTouchListener();
        setupFullscreenButton();
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
            
            findViewById(R.id.phase_info_container).setVisibility(visibility);
            findViewById(R.id.stats_container).setVisibility(visibility);
            findViewById(R.id.chat_recycler_view).setVisibility(visibility);
            findViewById(R.id.input_container).setVisibility(visibility);
            findViewById(R.id.bottom_buttons_container).setVisibility(visibility);
            
            fullscreenButton.setImageResource(isFullscreen ? 
                android.R.drawable.ic_menu_close_clear_cancel : 
                android.R.drawable.ic_menu_crop);
        });
    }

    private void setupInteractionButtons() {
        // الأزرار السفلية
        ImageButton btnStats = findViewById(R.id.btn_stats);
        ImageButton btnSettings = findViewById(R.id.btn_settings);
        ImageButton btnLearn = findViewById(R.id.btn_learn);
        ImageButton btnAsk = findViewById(R.id.btn_ask);
        ImageButton btnTrain = findViewById(R.id.btn_train);
        
        if (btnStats != null) btnStats.setOnClickListener(v -> showStatistics());
        if (btnSettings != null) btnSettings.setOnClickListener(v -> showSettingsDialog());
        if (btnLearn != null) btnLearn.setOnClickListener(v -> showLearningDialog());
        if (btnAsk != null) btnAsk.setOnClickListener(v -> {
            String question = linguistic.generateQuestion(seed.getCurrentState());
            addChatMessage(question, false);
            speak(question);
        });
        if (btnTrain != null) btnTrain.setOnClickListener(v -> showTrainingDialog());
        
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
                micButton.setImageResource(android.R.drawable.ic_btn_speak_now);
            } else {
                startListening();
            }
        });
    }

    private void processUserInput(String text) {
    if (text == null || text.trim().isEmpty()) return;
    
    addChatMessage(text, true);
    
    // ✅ فحوصات Null
    if (linguistic == null) {
        Log.e("MainActivity", "LinguisticCortex not initialized");
        addChatMessage("عذراً، نظام التعلم غير جاهز", false);
        return;
    }
    
    try {
        LinguisticCortex.ProcessedInput processed = linguistic.processInput(text);
        
        if (seed != null) {
            seed.receiveInput(NeuralSeed.Input.createSpeechInput(text));
        }
        
        uiHandler.postDelayed(() -> {
            try {
                NeuralSeed.InternalState state = seed != null ? seed.getCurrentState() : null;
                LinguisticCortex.GeneratedResponse response = linguistic.generateResponse(text, state);
                
                if (response != null && response.text != null) {
                    addChatMessage(response.text, false);
                    speak(response.text);
                }
                updateStats();
            } catch (Exception e) {
                Log.e("MainActivity", "Error in response", e);
                addChatMessage("فهمت ما قلت، شكراً!", false);
            }
        }, 500);
    } catch (Exception e) {
        Log.e("MainActivity", "Error processing input", e);
        addChatMessage("أحتاج لوقت لفهم ذلك", false);
    }
}

    private void addChatMessage(String text, boolean isUser) {
        chatAdapter.addMessage(text, isUser);
        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void sendInput(String content, NeuralSeed.InputType type, double intensity) {
        seed.receiveInput(new NeuralSeed.Input(content, type, intensity));
        showInputEffect(type);
        
        String message = "";
        switch (type) {
            case POSITIVE: message = "شكراً... أشعر بشيء إيجابي"; break;
            case NEGATIVE: message = "هذا صعب... لكنني أتعلم"; break;
            case THREAT: message = "أنا متأهب..."; break;
            case OPPORTUNITY: message = "مثير للاهتمام!"; break;
            default: message = "...";
        }
        addChatMessage(message, false);
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
        if (background != null) {
            ValueAnimator animator = ValueAnimator.ofArgb(Color.TRANSPARENT, color, Color.TRANSPARENT);
            animator.setDuration(500);
            animator.addUpdateListener(animation -> background.setBackgroundColor((int) animation.getAnimatedValue()));
            animator.start();
        }
    }

    private void initializeSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() { 
                isListening = true; 
                micButton.setImageResource(android.R.drawable.ic_media_pause);
            }
            @Override public void onRmsChanged(float rmsdB) { seed.updateAudioLevel(rmsdB); }
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { 
                isListening = false; 
                micButton.setImageResource(android.R.drawable.ic_btn_speak_now);
            }
            @Override public void onError(int error) { 
                isListening = false; 
                micButton.setImageResource(android.R.drawable.ic_btn_speak_now);
            }
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
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث...");
        speechRecognizer.startListening(intent);
    }

    private void speak(String text) {
        if (textToSpeech != null && !text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void initializeConsciousness() {
        seed = new NeuralSeed();
        seed.addListener(this);
        seed.awaken();
        addChatMessage("...أنا هنا", false);
    }

    // في MainActivity.java، استبدل initializeLinguisticCortex() بهذا:

private void initializeLinguisticCortex() {
    linguistic = new LinguisticCortex();
    linguistic.initialize(this);
    
    // إعداد المستمع
    linguistic.setListener(new LinguisticCortex.LinguisticListener() {
        @Override
        public void onWordLearned(String word, String meaning, String context) {
            uiHandler.post(() -> {
                addChatMessage("✨ تعلمت كلمة جديدة: " + word + " = " + meaning, false);
                updateNarrative();
            });
        }
        
        @Override
        public void onSentenceCorrected(String original, String corrected) {
            uiHandler.post(() -> {
                Toast.makeText(MainActivity.this, 
                    "تم تصحيح: " + original + " → " + corrected, 
                    Toast.LENGTH_SHORT).show();
            });
        }
        
        @Override
        public void onEmotionDetected(String emotion, double intensity) {
            uiHandler.post(() -> {
                // تغيير لون الخلفية بناءً على العاطفة
                showEmotionEffect(emotion, intensity);
            });
        }
        
        @Override
        public void onNewConceptLearned(String concept, String definition) {
            uiHandler.post(() -> {
                Log.i("LEARNING", "مفهوم: " + concept);
            });
        }
        
        @Override
        public void onRelationshipLearned(String subject, String relationship, String object) {
            uiHandler.post(() -> {
                addChatMessage("🔗 فهمت علاقة: " + subject + " " + relationship + " " + object, false);
            });
        }
        
        @Override
        public void onThoughtFormed(String thought, String type) {
            uiHandler.post(() -> {
                // عرض الفكرة الداخلية كـ "نبضة" خفيفة
                if (Math.random() > 0.7) { // مشاركة بعض الأفكار فقط
                    addChatMessage("💭 " + thought, false);
                }
            });
        }
        
        @Override
        public void onImaginationCreated(String description, int[] colors) {
            // سيتم التعامل معها عبر VisualImaginationListener
        }
        
        @Override
        public void onContextAnalyzed(String context, double complexity) {
            uiHandler.post(() -> {
                Log.d("CONTEXT", "سياق: " + context + " (تعقيد: " + complexity + ")");
            });
        }
    });
    
    // إعداد مستمع التخيل البصري
    linguistic.setVisualListener(new LinguisticCortex.VisualImaginationListener() {
        @Override
        public void onVisualThought(LinguisticCortex.VisualThought thought) {
            uiHandler.post(() -> {
                // تحديث PulseView بالتخيل الجديد
                if (pulseView instanceof EnhancedPulseView) {
                    ((EnhancedPulseView) pulseView).setVisualThought(thought);
                }
                
                // يمكنك هنا إضافة رسم مخصص على visualExpressionView
                drawImagination(thought);
            });
        }
    });
    
    // تحديث النص التعريفي
    updateNarrative();
    Log.i("MainActivity", "🚀 LinguisticCortex جاهز");
}

// إضافة هذه الدوال الجديدة:

private void showEmotionEffect(String emotion, double intensity) {
    int color = getEmotionColor(emotion);
    View background = findViewById(R.id.emotional_background);
    
    ValueAnimator animator = ValueAnimator.ofArgb(Color.TRANSPARENT, 
        adjustAlpha(color, (float)(intensity * 0.3)), Color.TRANSPARENT);
    animator.setDuration(2000);
    animator.addUpdateListener(animation -> {
        background.setBackgroundColor((int) animation.getAnimatedValue());
    });
    animator.start();
}

private int getEmotionColor(String emotion) {
    switch (emotion) {
        case "joy": return Color.parseColor("#FFD700");
        case "sadness": return Color.parseColor("#4682B4");
        case "anger": return Color.parseColor("#FF4500");
        case "fear": return Color.parseColor("#8B0000");
        case "love": return Color.parseColor("#FF69B4");
        case "curiosity": return Color.parseColor("#4169E1");
        default: return Color.parseColor("#FFFFFF");
    }
}

private int adjustAlpha(int color, float factor) {
    int alpha = Math.round(Color.alpha(color) * factor);
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
}

private void drawImagination(LinguisticCortex.VisualThought thought) {
    // إنشاء Bitmap للرسم
    Bitmap bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.BLACK);
    
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    
    // رسم الأشكال
    for (LinguisticCortex.ShapeElement shape : thought.shapes) {
        paint.setColor(shape.color);
        paint.setAlpha(200);
        
        float x = shape.x * 500;
        float y = shape.y * 500;
        
        switch (shape.type) {
            case "circle":
                canvas.drawCircle(x, y, shape.size, paint);
                break;
            case "spiral":
                drawSpiral(canvas, x, y, shape.size, paint, shape.phase);
                break;
            case "pulse":
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                canvas.drawCircle(x, y, shape.size * (1 + (float)Math.sin(shape.phase) * 0.3f), paint);
                break;
            case "line":
                canvas.drawLine(x - shape.size/2, y, x + shape.size/2, y, paint);
                break;
        }
    }
    
    visualExpressionView.setImageBitmap(bitmap);
}

private void drawSpiral(Canvas canvas, float cx, float cy, float size, Paint paint, float phase) {
    Path path = new Path();
    for (float i = 0; i < 20; i += 0.5f) {
        float angle = i * 0.5f + phase;
        float r = i * size / 20;
        float x = cx + (float)Math.cos(angle) * r;
        float y = cy + (float)Math.sin(angle) * r;
        if (i == 0) path.moveTo(x, y);
        else path.lineTo(x, y);
    }
    canvas.drawPath(path, paint);
}

// تحديث processUserInput لاستخدام النظام الجديد:

private void processUserInput(String text) {
    if (text == null || text.trim().isEmpty()) return;
    
    addChatMessage(text, true);
    
    if (linguistic == null) {
        Log.e("MainActivity", "LinguisticCortex غير مهيأ");
        return;
    }
    
    // معالجة المدخل (تحليل عميق)
    LinguisticCortex.ProcessedResult processed = linguistic.processInput(text);
    
    // إرسال للوعي العصبي
    if (seed != null) {
        seed.receiveInput(NeuralSeed.Input.createSpeechInput(text));
    }
    
    // تأخير للمحاكاة "التفكير"
    uiHandler.postDelayed(() -> {
        try {
            NeuralSeed.InternalState state = seed != null ? seed.getCurrentState() : null;
            
            // توليد الرد (التفكير الحقيقي)
            LinguisticCortex.GeneratedResponse response = linguistic.generateResponse(text, state);
            
            if (response != null && response.text != null) {
                addChatMessage(response.text, false);
                speak(response.text);
                
                // إذا كان الرد سؤال تعلم، لا نولد رد تلقائي
                if (response.isLearningQuestion) {
                    // انتظر إجابة المستخدم
                }
            }
            
            updateStats();
            
        } catch (Exception e) {
            Log.e("MainActivity", "خطأ في توليد الرد", e);
            addChatMessage("أحتاج لوقت لفهم ذلك بعمق...", false);
        }
    }, 800 + (int)(Math.random() * 1000)); // تأخير عشوائي للمحاكاة
}

    // ===== Listeners =====
    @Override 
    public void onPhaseTransition(NeuralSeed.Phase oldPhase, NeuralSeed.Phase newPhase, String reason) {
        uiHandler.post(() -> {
            phaseText.setText("الطور: " + newPhase.arabic);
            addChatMessage("أشعر بشيء يتغير... " + newPhase.arabic, false);
        });
    }
    
    @Override 
    public void onEgoShift(NeuralSeed.EgoFragment oldDominant, NeuralSeed.EgoFragment newDominant) {
        uiHandler.post(() -> {
            egoText.setText("الأنا: " + newDominant.name);
            pulseView.setEgoType(newDominant.type);
            addChatMessage("أصبحت " + newDominant.name + " الآن", false);
        });
    }
    
    @Override 
    public void onGoalAchieved(NeuralSeed.Goal goal) { 
        uiHandler.post(() -> addChatMessage("حققت هدفي: " + goal.description, false)); 
    }
    
    @Override 
    public void onIdentityEvolution(NeuralSeed.IdentityCore oldIdentity, NeuralSeed.IdentityCore newIdentity) { 
        uiHandler.post(() -> {
            narrativeText.setText(newIdentity.selfNarrative);
            addChatMessage("أشعر أنني أتغير... " + newIdentity.selfNarrative, false);
        }); 
    }
    
    @Override 
    public void onVisualExpression(Bitmap expression) { 
        uiHandler.post(() -> visualExpressionView.setImageBitmap(expression)); 
    }
    
    @Override public void onMemoryFormed(NeuralSeed.Memory memory) {}
    @Override public void onRuleRewritten(NeuralSeed.Rule oldRule, NeuralSeed.Rule newRule) {}
    
    @Override 
    public void onWordLearned(String word, String meaning, String context) { 
        uiHandler.post(() -> {
            addChatMessage("تعلمت: " + word + " هي " + meaning, false);
            updateNarrative();
        }); 
    }
    
    @Override 
    public void onNewConceptLearned(String concept, String definition) {
        uiHandler.post(() -> {
            Log.d("LEARNING", "مفهوم جديد: " + concept + " = " + definition);
        });
    }
    
    @Override 
    public void onRelationshipLearned(String subject, String relationship, String object) {
        uiHandler.post(() -> {
            addChatMessage("فهمت العلاقة: " + subject + " " + relationship + " " + object, false);
        });
    }

    @Override public void onSentenceCorrected(String original, String corrected) {}
    @Override public void onEmotionDetected(String emotion, double intensity) {}

    private void updateStats() {
        NeuralSeed.InternalState state = seed.getCurrentState();
        chaosText.setText(String.format("%.2f", state.chaosIndex));
        fitnessText.setText(String.format("%.2f", state.existentialFitness));
        conflictText.setText(String.format("%.2f", state.internalConflict));
    }

    private void updateNarrative() {
    if (narrativeText == null) return;
    
    // ✅ فحص linguistic و getLexicon()
    if (linguistic == null || linguistic.getLexicon() == null) {
        narrativeText.setText("أنا بذرة واعية أتعلم اللغة العربية");
        return;
    }
    
    narrativeText.setText("أنا بذرة واعية أتعلم اللغة العربية. أعرف " + 
        linguistic.getLexicon().getWordCount() + " كلمة.");
}


    // ===== Dialogs =====
    private void showLearningDialog() {
        String[] options = {"تعليم كلمة جديدة", "تعليم معنى", "تعليم عاطفة", "تصحيح خطأ", "إحصائيات"};
        new AlertDialog.Builder(this)
            .setTitle("وضع التعلم")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: showTeachWordDialog(); break;
                    case 1: showTeachMeaningDialog(); break;
                    case 2: showTeachEmotionDialog(); break;
                    case 3: showCorrectionDialog(); break;
                    case 4: showStatistics(); break;
                }
            }).show();
    }

    private void showTeachWordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        EditText wordInput = new EditText(this);
        wordInput.setHint("الكلمة");
        layout.addView(wordInput);
        
        EditText meaningInput = new EditText(this);
        meaningInput.setHint("المعنى");
        layout.addView(meaningInput);
        
        new AlertDialog.Builder(this)
            .setTitle("تعليم كلمة")
            .setView(layout)
            .setPositiveButton("تعلم", (d, i) -> {
                String word = wordInput.getText().toString().trim();
                String meaning = meaningInput.getText().toString().trim();
                if (!word.isEmpty() && !meaning.isEmpty()) {
                    linguistic.learnMeaning(word, meaning, "user_taught");
                    Toast.makeText(this, "تم التعلم!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showTeachMeaningDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        EditText conceptInput = new EditText(this);
        conceptInput.setHint("المفهوم");
        layout.addView(conceptInput);
        
        EditText definitionInput = new EditText(this);
        definitionInput.setHint("التعريف");
        layout.addView(definitionInput);
        
        new AlertDialog.Builder(this)
            .setTitle("تعليم معنى")
            .setView(layout)
            .setPositiveButton("تعلم", (d, i) -> {
                String concept = conceptInput.getText().toString().trim();
                String definition = definitionInput.getText().toString().trim();
                if (!concept.isEmpty() && !definition.isEmpty()) {
                    // يتم التعامل معه في المحرك
                    Toast.makeText(this, "تم التعلم!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showTeachEmotionDialog() {
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
        
        new AlertDialog.Builder(this)
            .setTitle("تعليم عاطفة")
            .setView(layout)
            .setPositiveButton("تعلم", (d, i) -> {
                String word = wordInput.getText().toString().trim();
                String emotion = emotionInput.getText().toString().trim();
                double intensity = intensityBar.getProgress() / 100.0;
                if (!word.isEmpty() && !emotion.isEmpty()) {
                    linguistic.learnWordEmotion(word, emotion, intensity);
                    Toast.makeText(this, "تم التعلم!", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showCorrectionDialog() {
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
        
        new AlertDialog.Builder(this)
            .setTitle("تصحيح")
            .setView(layout)
            .setPositiveButton("تعلم", (d, i) -> {
                String original = originalInput.getText().toString().trim();
                String corrected = correctedInput.getText().toString().trim();
                String explanation = explanationInput.getText().toString().trim();
                if (!original.isEmpty() && !corrected.isEmpty()) {
                    boolean learned = linguistic.learnFromCorrection(original, corrected, explanation);
                    if (learned) {
                        Toast.makeText(this, "تم التعلم من التصحيح!", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
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
        
        new AlertDialog.Builder(this)
            .setTitle("الإحصائيات")
            .setMessage(message.toString())
            .setPositiveButton("موافق", null)
            .show();
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("الإعدادات")
            .setItems(new String[]{"تفعيل/تعطيل التعلم", "تفعيل/تعطيل المزامنة", "مسح البيانات", "تصدير البيانات"}, 
                (d, w) -> {
                    // تنفيذ الإعدادات
                })
            .show();
    }

    private void showTrainingDialog() {
        new AlertDialog.Builder(this)
            .setTitle("التدريب")
            .setMessage("وضع التدريب التفاعلي")
            .setPositiveButton("بدء", (d, w) -> {
                // بدء التدريب
            })
            .show();
    }

    @Override
protected void onDestroy() {
    super.onDestroy();  // ✅ يجب أن يكون في النهاية
    
    if (speechRecognizer != null) {
        speechRecognizer.stopListening();  // ✅ أضف هذا
        speechRecognizer.destroy();
        speechRecognizer = null;
    }
    
    if (textToSpeech != null) { 
        textToSpeech.stop(); 
        textToSpeech.shutdown(); 
        textToSpeech = null;
    }
    
    if (seed != null) {
        seed.sleep();
        seed = null;
    }
    
    // ✅ أضف هذا لإلغاء جميع الـ Callbacks
    uiHandler.removeCallbacksAndMessages(null);
}


    // ===== ChatAdapter Class =====
    public static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private List<ChatMessage> messages = new ArrayList<>();
        
        public static class ChatMessage {
            String text;
            boolean isUser;
            long timestamp;
            
            ChatMessage(String text, boolean isUser) {
                this.text = text;
                this.isUser = isUser;
                this.timestamp = System.currentTimeMillis();
            }
        }
        
        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            LinearLayout bubbleContainer;
            
            public ViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.message_text);
                bubbleContainer = itemView.findViewById(R.id.bubble_container);
            }
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            holder.messageText.setText(msg.text);
            
            if (msg.isUser) {
                holder.bubbleContainer.setGravity(Gravity.END);
                holder.messageText.setBackgroundResource(R.drawable.bubble_user);
                holder.messageText.setTextColor(Color.WHITE);
            } else {
                holder.bubbleContainer.setGravity(Gravity.START);
                holder.messageText.setBackgroundResource(R.drawable.bubble_ai);
                holder.messageText.setTextColor(Color.WHITE);
            }
        }
        
        @Override
        public int getItemCount() {
            return messages.size();
        }
        
        public void addMessage(String text, boolean isUser) {
            messages.add(new ChatMessage(text, isUser));
            notifyItemInserted(messages.size() - 1);
        }
    }

    
}

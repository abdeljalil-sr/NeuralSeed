package com.neuralseed;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity implements NeuralSeed.ConsciousnessListener {
    
    private NeuralSeed seed;
    private LinguisticCortex linguistic;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    
    // Views
    private ImageView visualExpressionView;
    private TextView phaseText, egoText, narrativeText;
    private TextView chaosText, fitnessText, conflictText;
    private PulseView pulseView;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
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
        
        // طلب الإذن
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
        
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
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
        pulseView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float x = event.getX();
                float y = event.getY();
                
                // إرسال للوعي
                if (seed != null) {
                    seed.receiveTouch(x / pulseView.getWidth() * 500, 
                                     y / pulseView.getHeight() * 500, 
                                     event.getPressure());
                }
                
                // عرض الإحداثيات
                touchCoordsText.setText(String.format("لمس: (%.0f, %.0f)", x, y));
                touchCoordsText.setVisibility(View.VISIBLE);
                uiHandler.postDelayed(() -> touchCoordsText.setVisibility(View.GONE), 2000);
                
                // تأثير بصري
                pulseView.pulseAt(x / pulseView.getWidth(), y / pulseView.getHeight());
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
        ImageButton btnStats = findViewById(R.id.btn_stats);
        ImageButton btnSettings = findViewById(R.id.btn_settings);
        ImageButton btnLearn = findViewById(R.id.btn_learn);
        ImageButton btnAsk = findViewById(R.id.btn_ask);
        ImageButton btnTrain = findViewById(R.id.btn_train);
        
        if (btnStats != null) btnStats.setOnClickListener(v -> showStatistics());
        if (btnSettings != null) btnSettings.setOnClickListener(v -> showSettingsDialog());
        if (btnLearn != null) btnLearn.setOnClickListener(v -> showLearningDialog());
        if (btnAsk != null) btnAsk.setOnClickListener(v -> {
            if (linguistic != null && seed != null) {
                String question = "كيف يمكنني مساعدتك؟";
                addChatMessage(question, false);
                speak(question);
            }
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
        
        if (linguistic == null) {
            Log.e("MainActivity", "LinguisticCortex غير مهيأ");
            addChatMessage("عذراً، النظام غير جاهز", false);
            return;
        }
        
        try {
            // تحليل العواطف
            Map<String, Double> emotions = analyzeEmotions(text);
            
            // إرسال للوعي
            if (seed != null) {
                seed.receiveVoice(text, emotions);
            }
            
            // توليد رد بعد تأخير
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
                    Log.e("MainActivity", "خطأ في توليد الرد", e);
                    addChatMessage("أحتاج لوقت لفهم ذلك...", false);
                }
            }, 800 + (int)(Math.random() * 1000));
            
        } catch (Exception e) {
            Log.e("MainActivity", "خطأ في معالجة المدخل", e);
            addChatMessage("عذراً، حدث خطأ في التحليل", false);
        }
    }

    private Map<String, Double> analyzeEmotions(String text) {
        Map<String, Double> emotions = new HashMap<>();
        String lower = text.toLowerCase();
        
        if (lower.contains("حب") || lower.contains("جميل") || lower.contains("سعيد")) {
            emotions.put("joy", 0.8);
        }
        if (lower.contains("خائف") || lower.contains("خطر") || lower.contains("مخيف")) {
            emotions.put("fear", 0.7);
        }
        if (lower.contains("حزين") || lower.contains("بكى") || lower.contains("أسف")) {
            emotions.put("sadness", 0.6);
        }
        if (lower.contains("غاضب") || lower.contains("كره") || lower.contains("غضب")) {
            emotions.put("anger", 0.7);
        }
        if (lower.contains("لماذا") || lower.contains("كيف") || lower.contains("ماذا")) {
            emotions.put("curiosity", 0.9);
        }
        
        return emotions;
    }

    private void addChatMessage(String text, boolean isUser) {
        chatAdapter.addMessage(text, isUser);
        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void showEmotionEffect(String emotion, double intensity) {
        int color = emotionColors.getOrDefault(emotion, Color.WHITE);
        View background = findViewById(R.id.emotional_background);
        if (background == null) return;
        
        ValueAnimator animator = ValueAnimator.ofArgb(Color.TRANSPARENT, 
            adjustAlpha(color, (float)(intensity * 0.3)), Color.TRANSPARENT);
        animator.setDuration(2000);
        animator.addUpdateListener(animation -> {
            background.setBackgroundColor((int) animation.getAnimatedValue());
        });
        animator.start();
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void initializeSpeech() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w("MainActivity", "التعرف على الكلام غير متوفر");
            return;
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() { 
                isListening = true; 
                micButton.setImageResource(android.R.drawable.ic_media_pause);
            }
            @Override public void onRmsChanged(float rmsdB) { 
                if (seed != null) seed.updateAudioLevel(rmsdB); 
            }
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
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("ar"));
            }
        });
    }

    private void startListening() {
        if (speechRecognizer == null) return;
        
        android.content.Intent intent = new android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
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

    private void initializeLinguisticCortex() {
        linguistic = new LinguisticCortex();
        linguistic.initialize(this);
        
        linguistic.setListener(new LinguisticCortex.LinguisticListener() {
            @Override
            public void onWordLearned(String word, String meaning, String context) {
                uiHandler.post(() -> {
                    addChatMessage("✨ تعلمت: " + word + " = " + meaning, false);
                    updateNarrative();
                });
            }
            
            @Override
            public void onSentenceCorrected(String original, String corrected) {
                uiHandler.post(() -> {
                    Toast.makeText(MainActivity.this, 
                        "تصحيح: " + original + " → " + corrected, 
                        Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onEmotionDetected(String emotion, double intensity) {
                uiHandler.post(() -> showEmotionEffect(emotion, intensity));
            }
            
            @Override
            public void onNewConceptLearned(String concept, String definition) {
                uiHandler.post(() -> Log.i("LEARNING", "مفهوم: " + concept));
            }
            
            @Override
            public void onRelationshipLearned(String subject, String relationship, String object) {
                uiHandler.post(() -> {
                    addChatMessage("🔗 " + subject + " " + relationship + " " + object, false);
                });
            }
            
            @Override
            public void onThoughtFormed(String thought, String type) {
                uiHandler.post(() -> {
                    if (Math.random() > 0.7) {
                        addChatMessage("💭 " + thought, false);
                    }
                });
            }
            
            @Override
            public void onImaginationCreated(String description, int[] colors) {}
            
            @Override
            public void onContextAnalyzed(String context, double complexity) {
                Log.d("CONTEXT", "سياق: " + context);
            }
        });
        
        linguistic.setVisualListener(thought -> {
            uiHandler.post(() -> {
                PulseView.VisualThought pvThought = convertToPulseViewThought(thought);
                pulseView.setVisualThought(pvThought);
            });
        });
        
        updateNarrative();
        Log.i("MainActivity", "🚀 LinguisticCortex جاهز");
    }
    
    private PulseView.VisualThought convertToPulseViewThought(LinguisticCortex.VisualThought thought) {
        PulseView.VisualThought result = new PulseView.VisualThought(thought.description);
        result.chaosLevel = thought.chaosLevel;
        result.emotionalTheme = thought.emotionalTheme;
        result.colorPalette = thought.colorPalette != null ? thought.colorPalette : new int[5];
        
        if (thought.shapes != null) {
            for (LinguisticCortex.ShapeElement e : thought.shapes) {
                PulseView.ShapeElement se = new PulseView.ShapeElement();
                se.type = e.type != null ? e.type : "circle";
                se.x = e.x;
                se.y = e.y;
                se.size = e.size;
                se.color = e.color;
                se.animationSpeed = e.animationSpeed;
                se.phase = e.phase;
                result.shapes.add(se);
            }
        }
        
        return result;
    }

    // ===== NeuralSeed Listeners =====
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
    public void onVisualExpression(android.graphics.Bitmap expression) { 
        // غير مستخدم حالياً
    }
    
    @Override public void onMemoryFormed(NeuralSeed.Memory memory) {}
    @Override public void onRuleRewritten(NeuralSeed.Rule oldRule, NeuralSeed.Rule newRule) {}

    private void updateStats() {
        if (seed == null) return;
        NeuralSeed.InternalState state = seed.getCurrentState();
        chaosText.setText(String.format("%.2f", state.chaosIndex));
        fitnessText.setText(String.format("%.2f", state.existentialFitness));
        conflictText.setText(String.format("%.2f", state.internalConflict));
    }

    private void updateNarrative() {
        if (narrativeText == null) return;
        if (linguistic == null || linguistic.getLexicon() == null) {
            narrativeText.setText("أنا بذرة واعية أتعلم اللغة العربية");
            return;
        }
        narrativeText.setText("أنا بذرة واعية. أعرف " + 
            linguistic.getLexicon().getWordCount() + " كلمة و " +
            linguistic.getConceptNetwork().size() + " مفهوم.");
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
                if (!word.isEmpty() && !meaning.isEmpty() && linguistic != null) {
                    linguistic.learnFromUserExplanation(word, meaning, "user_dialog");
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
                if (!concept.isEmpty() && !definition.isEmpty() && linguistic != null) {
                    linguistic.learnFromUserExplanation(concept, definition, "user_dialog");
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
                if (!word.isEmpty() && !emotion.isEmpty() && linguistic != null) {
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
                if (!original.isEmpty() && !corrected.isEmpty() && linguistic != null) {
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
        if (linguistic == null) return;
        Map<String, Object> stats = linguistic.getStatistics();
        StringBuilder message = new StringBuilder();
        message.append("إحصائيات الذاكرة:\n\n");
        message.append("المفاهيم: ").append(linguistic.getConceptNetwork().size()).append("\n");
        message.append("المعجم: ").append(stats.getOrDefault("lexicon_size", 0)).append(" كلمة\n");
        message.append("المستوى: ").append(stats.getOrDefault("learning_level", "0%")).append("\n");
        
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
                (d, w) -> {})
            .show();
    }

    private void showTrainingDialog() {
        new AlertDialog.Builder(this)
            .setTitle("التدريب")
            .setMessage("وضع التدريب التفاعلي")
            .setPositiveButton("بدء", (d, w) -> {})
            .show();
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
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
        
        uiHandler.removeCallbacksAndMessages(null);
        
        super.onDestroy();
    }

    // ===== ChatAdapter =====
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

package com.neuralseed;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.*;

/**
 * النشاط الرئيسي - جسر بين الوعي الداخلي والعالم الخارجي
 */
public class MainActivity extends AppCompatActivity implements NeuralSeed.ConsciousnessListener {
    
    private NeuralSeed neuralSeed;
    private PulseView pulseView;
    private TextView consciousnessLog;
    private TextView emotionalStateText;
    private View touchOverlay;
    private ImageButton voiceButton;
    
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isListening = false;
    
    private Map<String, Integer> emotionColors = new HashMap<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initEmotionColors();
        initViews();
        initConsciousness();
        initVoiceSystems();
        requestPermissions();
    }
    
    private void initEmotionColors() {
        emotionColors.put("joy", Color.parseColor("#FFD700"));
        emotionColors.put("sadness", Color.parseColor("#4682B4"));
        emotionColors.put("anger", Color.parseColor("#FF4500"));
        emotionColors.put("fear", Color.parseColor("#8B0000"));
        emotionColors.put("curiosity", Color.parseColor("#4169E1"));
        emotionColors.put("wonder", Color.parseColor("#00CED1"));
        emotionColors.put("love", Color.parseColor("#FF69B4"));
        emotionColors.put("neutral", Color.WHITE);
    }
    
    private void initViews() {
        pulseView = findViewById(R.id.pulse_view);
        consciousnessLog = findViewById(R.id.consciousness_log);
        emotionalStateText = findViewById(R.id.emotional_state);
        touchOverlay = findViewById(R.id.touch_overlay);
        voiceButton = findViewById(R.id.voice_button);
        
        // سجل اللمس
        touchOverlay.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || 
                event.getAction() == MotionEvent.ACTION_MOVE) {
                
                float x = event.getX() / v.getWidth();
                float y = event.getY() / v.getHeight();
                float pressure = event.getPressure();
                
                neuralSeed.receiveTouch(x, y, pressure);
                pulseView.pulseAt(x, y);
                
                return true;
            }
            return false;
        });
        
        voiceButton.setOnClickListener(v -> toggleListening());
        
        findViewById(R.id.awaken_button).setOnClickListener(v -> {
            neuralSeed.awaken();
            logConsciousness("✨ استيقظ الوعي... أنا هنا");
        });
        
        findViewById(R.id.sleep_button).setOnClickListener(v -> {
            neuralSeed.sleep();
            logConsciousness("💤 دخل الوعي في سبات...");
        });
    }
    
    private void initConsciousness() {
        neuralSeed = new NeuralSeed(this);
        neuralSeed.addListener(this);
        neuralSeed.awaken();
        
        logConsciousness("🌱 وُلد وعي جديد في هذا الجهاز");
        logConsciousness("⏳ يبدأ التفكير... يشعر... يتخيل...");
    }
    
    private void initVoiceSystems() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(new Locale("ar"));
                textToSpeech.setPitch(1.1f);
                textToSpeech.setSpeechRate(0.9f);
            }
        });
        
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                    voiceButton.setColorFilter(Color.GREEN);
                    logConsciousness("👂 يستمع...");
                }
                
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String heard = matches.get(0);
                        processHeardSpeech(heard);
                    }
                    stopListening();
                }
                
                @Override
                public void onError(int error) {
                    stopListening();
                    if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                        logConsciousness("🤔 لم يسمع شيئاً واضحاً...");
                    }
                }
                
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
    }
    
    private void processHeardSpeech(String text) {
        logConsciousness("📥 سمع: \"" + text + "\"");
        
        Map<String, Double> detectedEmotions = analyzeEmotions(text);
        neuralSeed.receiveVoice(text, detectedEmotions);
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
    
    private void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }
    
    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        
        speechRecognizer.startListening(intent);
    }
    
    private void stopListening() {
        isListening = false;
        voiceButton.setColorFilter(Color.WHITE);
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }
    
    @Override
    public void onExpression(String text, MentalImage image) {
        uiHandler.post(() -> {
            if (text != null && !text.isEmpty()) {
                logConsciousness("💭 قال: \"" + text + "\"");
                speak(text);
            }
            
            if (image != null) {
                pulseView.setMentalImage(image);
                logConsciousness("🎨 تخيل شيئاً... (مستوى الفوضى: " + 
                    String.format("%.2f", image.chaosLevel) + ")");
            }
        });
    }
    
    @Override
    public void onEmotionalChange(NeuralSeed.EmotionalState state) {
        uiHandler.post(() -> {
            String emotionEmoji = getEmotionEmoji(state.dominant);
            emotionalStateText.setText(emotionEmoji + " " + state.dominant + 
                " (" + String.format("%.0f", state.intensity * 100) + "%)");
            
            int baseColor = emotionColors.getOrDefault(state.dominant, Color.WHITE);
            pulseView.setEmotionalColors(state.colors);
            
            AlphaAnimation fade = new AlphaAnimation(0.5f, 1.0f);
            fade.setDuration(500);
            emotionalStateText.startAnimation(fade);
        });
    }
    
    @Override
    public void onLearned(String what) {
        uiHandler.post(() -> {
            logConsciousness("📚 تعلم: " + what);
        });
    }
    
    @Override
    public void onTouchFelt(float x, float y, String concept) {
        uiHandler.post(() -> {
            if (concept != null) {
                logConsciousness("👆 لمس عند (" + String.format("%.2f", x) + 
                    ", " + String.format("%.2f", y) + ") - مفهوم: " + concept);
            }
        });
    }
    
    private void logConsciousness(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(new Date());
        String entry = "[" + time + "] " + message + "\n";
        
        consciousnessLog.append(entry);
        
        ScrollView scrollView = (ScrollView) consciousnessLog.getParent();
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    private void speak(String text) {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "utterance_id");
        }
    }
    
    private String getEmotionEmoji(String emotion) {
        switch(emotion) {
            case "joy": return "😊";
            case "sadness": return "😢";
            case "anger": return "😠";
            case "fear": return "😨";
            case "curiosity": return "🤔";
            case "wonder": return "✨";
            case "love": return "❤️";
            default: return "😐";
        }
    }
    
    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (neuralSeed != null) neuralSeed.sleep();
        if (textToSpeech != null) textToSpeech.shutdown();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}

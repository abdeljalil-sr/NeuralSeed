package com.lifeentity.language;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.lifeentity.core.ConsciousMoment;
import com.lifeentity.core.ConsciousnessCore;
import com.lifeentity.core.EmotionalState;
import com.lifeentity.memory.MemoryDao;

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class ArabicDialogue implements ConsciousnessCore.ConsciousnessObserver {
    
    private static final String TAG = "ArabicDialogue";
    
    private TextToSpeech tts;
    private MemoryDao memory;
    private Random random;
    private boolean isSpeaking = false;
    private Context context;
    
    private String[][] phrases = {
        {"أشعر بالدفء", "هذا جميل", "أحب هذا"},
        {"أشعر بالوحدة", "أحتاج لراحة"},
        {"ما هذا؟", "أريد أن أعرف"},
        {"أشعر بخطر", "أريد أن أختبئ"},
        {"أنت هنا", "أشعر بالأمان معك"}
    };
    
    public ArabicDialogue(Context context, MemoryDao dao) {
        this.context = context.getApplicationContext();
        this.memory = dao;
        this.random = new Random();
        
        initTts();
    }
    
    private void initTts() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ar"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Arabic not supported");
                } else {
                    tts.setPitch(1.0f);
                    tts.setSpeechRate(0.9f);
                }
            }
        });
    }
    
    @Override
    public void onConsciousMoment(ConsciousMoment moment) {
        if (random.nextFloat() > 0.98 && !isSpeaking && moment.emotionalTone != null) {
            String text = generateSpeech(moment.emotionalTone);
            articulate(text, moment.emotionalTone);
        }
    }
    
    @Override
    public void onEmotionalShift(EmotionalState from, EmotionalState to) {
        if (to.getIntensity() > 0.7 && !isSpeaking) {
            String text = verbalize(to);
            articulate(text, to);
        }
    }
    
    @Override
    public void onArticulation(String thought, int urgency) {
        // Already handled
    }
    
    public void articulate(String text, EmotionalState emo) {
        if (isSpeaking || text == null || text.isEmpty() || tts == null) {
            return;
        }
        
        float pitch = 1.0f;
        float rate = 0.9f;
        
        if (emo.isExcited()) {
            pitch = 1.2f;
            rate = 1.1f;
        } else if (emo.isCalm()) {
            pitch = 0.9f;
            rate = 0.7f;
        } else if (emo.isAfraid()) {
            pitch = 1.3f;
            rate = 1.2f;
        }
        
        tts.setPitch(pitch);
        tts.setSpeechRate(rate);
        
        isSpeaking = true;
        
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "speech");
        
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}
            
            @Override
            public void onDone(String utteranceId) {
                isSpeaking = false;
            }
            
            @Override
            public void onError(String utteranceId) {
                isSpeaking = false;
            }
        });
        
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    }
    
    public void hearUser(String text, boolean isQuestion) {
        // Process user input
        Log.d(TAG, "Heard: " + text + (isQuestion ? " (question)" : ""));
        
        // Respond to question
        if (isQuestion) {
            String response = generateAnswer(text);
            articulate(response, new EmotionalState());
        }
    }
    
    private String generateSpeech(EmotionalState emo) {
        int category;
        if (emo.isJoyful()) category = 0;
        else if (emo.isSad()) category = 1;
        else if (emo.isCurious()) category = 2;
        else if (emo.isAfraid()) category = 3;
        else category = 4;
        
        String[] catPhrases = phrases[category];
        return catPhrases[random.nextInt(catPhrases.length)];
    }
    
    private String verbalize(EmotionalState emo) {
        if (emo.isJoyful()) return "أشعر بالفرح!";
        if (emo.isAfraid()) return "أشعر بالخوف...";
        if (emo.isCurious()) return "ما هذا؟";
        if (emo.isSad()) return "أشعر بالحزن";
        return "أنا هنا";
    }
    
    private String generateAnswer(String question) {
        String[] answers = {
            "لا أعرف بالضبط، لكنني أفكر",
            "هذا سؤال عميق",
            "دعني أتأمل في ذلك",
            "أشعر أن الإجابة تتعلق بما نعيشه"
        };
        return answers[random.nextInt(answers.length)];
    }
    
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}

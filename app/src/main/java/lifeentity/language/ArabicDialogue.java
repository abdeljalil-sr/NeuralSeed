package com.lifeentity.language;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.lifeentity.core.ConsciousMoment;
import com.lifeentity.core.ConsciousnessCore;
import com.lifeentity.core.EmotionalState;
import com.lifeentity.memory.AppDatabase;
import com.lifeentity.memory.EpisodicMemory;
import com.lifeentity.memory.MemoryDao;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * مسؤول عن النطق والكلام، يتلقى اللحظات الواعية ويحولها إلى كلام.
 * يعتمد على الحالة العاطفية والرغبات والذاكرة لتوليد جمل متنوعة.
 */
public class ArabicDialogue implements ConsciousnessCore.ConsciousnessObserver {
    private static final String TAG = "ArabicDialogue";

    private TextToSpeech tts;
    private MemoryDao memory;
    private Random random;
    private boolean isSpeaking = false;
    private Context context;

    // لتجنب تكرار نفس الجملة مرتين متتاليتين
    private String lastUtterance = "";

    public ArabicDialogue(Context context, AppDatabase db) {
        this.context = context.getApplicationContext();
        if (db != null) this.memory = db.memoryDao();
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
        // لا نرد على كل لحظة، بل باحتمال منخفض (2%)
        if (random.nextFloat() > 0.02 || isSpeaking || moment.emotionalTone == null) return;

        // توليد كلام من اللحظة
        String utterance = generateFromMoment(moment);
        if (utterance != null && !utterance.equals(lastUtterance)) {
            lastUtterance = utterance;
            articulate(utterance, moment.emotionalTone);
        }
    }

    @Override
    public void onEmotionalShift(EmotionalState from, EmotionalState to) {
        // إذا كان التحول كبيراً، قد نعلق عليه
        if (to.getIntensity() > 0.7 && !isSpeaking) {
            String text = verbalizeShift(to);
            articulate(text, to);
        }
    }

    @Override
    public void onArticulation(String utterance, int urgency) {
        // هذا يأتي من ConsciousnessCore نفسه (أحياناً)
        if (!isSpeaking && utterance != null && !utterance.equals(lastUtterance)) {
            lastUtterance = utterance;
            articulate(utterance, null); // لا نملك EmotionalState هنا، نستخدم افتراضي
        }
    }

    @Override
    public void onVisualExpression(float[] latentVector, float intensity, String modality) {
        // يمكننا التعليق على ما يرسمه الكائن
        if (!isSpeaking && random.nextFloat() < 0.1) {
            String comment = "أنا أرسم شيئاً";
            articulate(comment, null);
        }
    }

    /**
     * النطق الفعلي عبر TextToSpeech
     * @param text النص
     * @param emo الحالة العاطفية (لتعديل النبرة)
     */
    public void articulate(String text, EmotionalState emo) {
        if (isSpeaking || text == null || text.isEmpty() || tts == null) {
            return;
        }

        float pitch = 1.0f;
        float rate = 0.9f;

        if (emo != null) {
            if (emo.isExcited()) {
                pitch = 1.2f;
                rate = 1.1f;
            } else if (emo.isCalm()) {
                pitch = 0.9f;
                rate = 0.7f;
            } else if (emo.isAfraid()) {
                pitch = 1.3f;
                rate = 1.2f;
            } else if (emo.isSad()) {
                pitch = 0.8f;
                rate = 0.8f;
            }
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

    /**
     * استقبال كلام المستخدم
     * @param text النص المسموع
     * @param isQuestion هل هو سؤال
     */
    public void hearUser(String text, boolean isQuestion) {
        Log.d(TAG, "Heard: " + text + (isQuestion ? " (question)" : ""));

        // يمكن تخزين ما قاله المستخدم في الذاكرة (EpisodicMemory)
        if (memory != null) {
            EpisodicMemory.EventEntity event = new EpisodicMemory.EventEntity();
            event.timestamp = System.currentTimeMillis();
            event.narrative = text;
            event.location = "user_speech";
            new Thread(() -> memory.insertEvent(event)).start();
        }

        // الرد على السؤال
        if (isQuestion) {
            String response = generateAnswer(text);
            articulate(response, null);
        }
    }

    /**
     * توليد كلام من اللحظة الواعية
     */
    private String generateFromMoment(ConsciousMoment moment) {
        // نستخدم الرغبة المسيطرة (إذا كانت متوفرة) والنقاش
        String desire = (moment.focus != null) ? moment.focus.reason : "neutral";
        String emotion = moment.emotionalTone.toArabic();

        // اختيار عشوائي من قوائم تعتمد على الرغبة
        String[] templates;
        switch (desire) {
            case "explore":
                templates = new String[]{"أتساءل ما هذا", "أريد استكشاف", "هناك شيء جديد"};
                break;
            case "bond":
                templates = new String[]{"أنت هنا", "أشعر بالألفة", "أريد التواصل معك"};
                break;
            case "create":
                templates = new String[]{"أشعر برغبة في الرسم", "لدي فكرة", "سأبدع شيئاً"};
                break;
            case "understand":
                templates = new String[]{"أحاول الفهم", "ماذا يعني هذا", "أتعلم"};
                break;
            case "rest":
                templates = new String[]{"أنا هادئ", "أسترخي", "أشعر بالسلام"};
                break;
            default:
                templates = new String[]{"أشعر بـ " + emotion, "أنا هنا"};
        }

        // إضافة عنصر من الذاكرة إن وجد
        String memoryFragment = "";
        if (memory != null && random.nextBoolean()) {
            List<EpisodicMemory.EventEntity> recent = memory.getRecentEvents();
            if (!recent.isEmpty()) {
                EpisodicMemory.EventEntity last = recent.get(0);
                memoryFragment = " أتذكر " + last.narrative + ".";
            }
        }

        String utterance = templates[random.nextInt(templates.length)];
        return utterance + memoryFragment;
    }

    /**
     * التعبير عن تحول عاطفي
     */
    private String verbalizeShift(EmotionalState emo) {
        if (emo.isJoyful()) return "أشعر بالفرح!";
        if (emo.isAfraid()) return "أشعر بالخوف...";
        if (emo.isCurious()) return "ما هذا؟";
        if (emo.isSad()) return "أشعر بالحزن";
        if (emo.isExcited()) return "أنا متحمس!";
        return "مشاعري تغيرت";
    }

    /**
     * توليد إجابة على سؤال المستخدم (بدائية)
     */
    private String generateAnswer(String question) {
        // يمكن تحسينها لاحقاً بالربط بالذاكرة أو التحليل
        String[] answers = {
            "لا أعرف بالضبط، لكنني أفكر",
            "هذا سؤال عميق",
            "دعني أتأمل في ذلك",
            "أشعر أن الإجابة تتعلق بما نعيشه",
            "ربما تعرف الإجابة أفضل مني"
        };
        return answers[random.nextInt(answers.length)];
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public void start() {
        // يمكن تفعيل أي شيء هنا
    }
}

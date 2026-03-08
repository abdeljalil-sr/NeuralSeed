package com.lifeentity.sensors;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * القشرة السمعية (Auditory Cortex) - مسؤولة عن استقبال الأصوات وتحويلها إلى بيانات.
 * تعمل بشكل مستمر في الخلفية وتبلغ المستمع (OnHearingListener) عند حدوث أصوات أو كلام.
 * لا تحتوي على قواعد سلوكية، فقط تحويل المدخلات الصوتية إلى معلومات رقمية.
 */
public class AuditoryCortex {
    private static final String TAG = "AuditoryCortex";

    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private OnHearingListener listener;
    private boolean isListening = false;

    private float currentAmplitude = 0;      // شدة الصوت الحالية (0..1 تقريباً)
    private float currentPitch = 0;          // طبقة الصوت (غير مستخدمة حالياً)
    private boolean speechDetected = false;

    public interface OnHearingListener {
        void onSoundHeard(float amplitude, float pitch, boolean isSpeech);
        void onSpeechRecognized(String text, float confidence);
        void onQuestionDetected(String question);
    }

    public AuditoryCortex(Context context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(new EarListener());

            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        } else {
            Log.e(TAG, "Speech recognition not available");
        }
    }

    public void startContinuousListening() {
        if (recognizer == null || isListening) return;
        isListening = true;
        listen();
    }

    private void listen() {
        if (!isListening) return;
        try {
            recognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            Log.e(TAG, "Listen failed: " + e.getMessage());
            retryListen();
        }
    }

    private void retryListen() {
        new android.os.Handler().postDelayed(this::listen, 1000);
    }

    private class EarListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for speech");
        }

        @Override
        public void onBeginningOfSpeech() {
            speechDetected = true;
            if (listener != null) {
                listener.onSoundHeard(currentAmplitude, currentPitch, true);
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // تحويل dB إلى مقياس 0-1 (تقريبي)
            currentAmplitude = Math.min(1.0f, rmsdB / 20f); // 0-20dB -> 0-1
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            // يمكن استخدامه لتحليل pitch لاحقاً
        }

        @Override
        public void onEndOfSpeech() {
            speechDetected = false;
        }

        @Override
        public void onError(int error) {
            String errorMsg = "Unknown error";
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO: errorMsg = "Audio error"; break;
                case SpeechRecognizer.ERROR_CLIENT: errorMsg = "Client error"; break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: errorMsg = "Insufficient permissions"; break;
                case SpeechRecognizer.ERROR_NETWORK: errorMsg = "Network error"; break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: errorMsg = "Network timeout"; break;
                case SpeechRecognizer.ERROR_NO_MATCH: errorMsg = "No match"; break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: errorMsg = "Recognizer busy"; break;
                case SpeechRecognizer.ERROR_SERVER: errorMsg = "Server error"; break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: errorMsg = "Speech timeout"; break;
            }
            Log.e(TAG, "Recognition error: " + errorMsg);
            if (isListening) retryListen();
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            float[] confidences = results.getFloatArray(
                    SpeechRecognizer.CONFIDENCE_SCORES);

            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                float conf = (confidences != null && confidences.length > 0) ?
                        confidences[0] : 0.5f;

                if (listener != null) {
                    listener.onSpeechRecognized(text, conf);
                    if (isQuestion(text)) {
                        listener.onQuestionDetected(text);
                    }
                }
            }

            if (isListening) listen();
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            // يمكن معالجة النتائج الجزئية إذا أردنا
        }

        @Override
        public void onEvent(int eventType, Bundle params) {}
    }

    /**
     * كشف بسيط للأسئلة بناءً على الكلمات المفتاحية (يمكن نقل هذه المسؤولية للمستوى الأعلى).
     */
    private boolean isQuestion(String text) {
        String normalized = text.trim();
        return normalized.contains("؟") ||
                normalized.startsWith("ما") ||
                normalized.startsWith("من") ||
                normalized.startsWith("كم") ||
                normalized.startsWith("أين") ||
                normalized.startsWith("متى") ||
                normalized.startsWith("كيف") ||
                normalized.startsWith("لماذا") ||
                normalized.startsWith("هل");
    }

    public void stop() {
        isListening = false;
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
        }
    }

    public void setListener(OnHearingListener l) {
        this.listener = l;
    }
}

package com.lifeentity.sensors;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class AuditoryCortex {
    private static final String TAG = "AuditoryCortex";
    
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private OnHearingListener listener;
    private boolean isListening = false;
    
    private float currentAmplitude = 0;
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
            Log.d(TAG, "Ready");
        }
        
        @Override
        public void onBeginningOfSpeech() {
            speechDetected = true;
            if (listener != null) {
                listener.onSoundHeard(currentAmplitude, 0, true);
            }
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
            currentAmplitude = rmsdB / 10f;
        }
        
        @Override
        public void onBufferReceived(byte[] buffer) {}
        
        @Override
        public void onEndOfSpeech() {
            speechDetected = false;
        }
        
        @Override
        public void onError(int error) {
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
                float conf = confidences != null && confidences.length > 0 ? 
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
        public void onPartialResults(Bundle partialResults) {}
        
        @Override
        public void onEvent(int eventType, Bundle params) {}
    }
    
    private boolean isQuestion(String text) {
        return text.contains("؟") || 
               text.startsWith("ما") || 
               text.startsWith("من") ||
               text.startsWith("كم") ||
               text.startsWith("أين") ||
               text.startsWith("متى") ||
               text.startsWith("كيف") ||
               text.startsWith("لماذا") ||
               text.startsWith("هل");
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

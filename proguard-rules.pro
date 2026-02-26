# ProGuard rules for NeuralSeed

# Keep public classes and methods
-keep public class com.neuralseed.** {
    public *;
}

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep SQLite database classes
-keep class android.database.sqlite.** { *; }

# Keep JSON classes
-keep class org.json.** { *; }

# Keep Android support library
-keep class androidx.** { *; }

# Keep speech recognition
-keep class android.speech.** { *; }

# Keep TextToSpeech
-keep class android.speech.tts.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

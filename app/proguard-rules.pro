# قواعد ProGuard لتطبيق LifeEntity

# الاحتفاظ بكل الكلاسات العامة والأساليب
-keep public class com.lifeentity.** {
    public *;
}

# الاحتفاظ بكلاسات Firebase (إذا استخدمتها)
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# الاحتفاظ بكلاسات SQLite
-keep class android.database.sqlite.** { *; }

# الاحتفاظ بكلاسات JSON
-keep class org.json.** { *; }

# الاحتفاظ بدعم مكتبات AndroidX
-keep class androidx.** { *; }

# الاحتفاظ بالتعرف على الكلام
-keep class android.speech.** { *; }

# الاحتفاظ بتحويل النص إلى كلام
-keep class android.speech.tts.** { *; }

# إزالة السجلات في نسخة الإصدار (release)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

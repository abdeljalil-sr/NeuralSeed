package com.lifeentity.memory;

import androidx.room.TypeConverter;
import java.util.Date;

/**
 * محولات النوع (TypeConverters) لقاعدة بيانات Room.
 * تسمح بتخزين أنواع معقدة مثل float[] و Date في جداول SQLite.
 */
public class Converters {

    // -------------------------- Date <-> Long --------------------------
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // -------------------------- float[] <-> String --------------------------
    // تُستخدم لتخزين المتجهات الكامنة (latent vectors) في VisualMemory،
    // ومتجهات التضمين (embeddings) في FaceIdentitySystem،
    // ومتجهات العاطفة (affect vectors) في EmotionalState.
    @TypeConverter
    public static String floatArrayToString(float[] array) {
        if (array == null || array.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(array[i]);
        }
        return sb.toString();
    }

    @TypeConverter
    public static float[] stringToFloatArray(String value) {
        if (value == null || value.isEmpty()) return new float[0];
        String[] parts = value.split(",");
        float[] array = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                array[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                // في حالة خطأ، نعطي قيمة صفر (يمكن تعديلها لتناسب الحاجة)
                array[i] = 0f;
            }
        }
        return array;
    }

    // -------------------------- محولات إضافية (إذا احتيج إليها مستقبلاً) --------------------------
    // يمكن إضافة محولات لـ int[], List<String>, إلخ.
}

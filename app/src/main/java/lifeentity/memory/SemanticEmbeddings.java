package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

public class SemanticEmbeddings {
    
    @androidx.room.Entity(tableName = "embeddings")
    public static class Entity {
        @androidx.room.PrimaryKey
        @NonNull  // ← إضافة فقط
        public String concept;
        
        // تغيير float[] إلى String لتجنب TypeConverter
        public String vectorData;  // ← تغيير الاسم والنوع
        
        public long learnedAt;
        
        // دوال مساعدة للتحويل (اختيارية للاستخدام)
        public void setVector(float[] array) {
            if (array == null) {
                vectorData = "";
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(array[i]);
            }
            vectorData = sb.toString();
        }
        
        public float[] getVector() {
            if (vectorData == null || vectorData.isEmpty()) return new float[0];
            String[] parts = vectorData.split(",");
            float[] array = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    array[i] = Float.parseFloat(parts[i]);
                } catch (NumberFormatException e) {
                    array[i] = 0f;
                }
            }
            return array;
        }
    }
}

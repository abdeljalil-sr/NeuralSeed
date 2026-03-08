package com.lifeentity.memory;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {
    EpisodicMemory.EventEntity.class,
    IdentityMemory.class,
    SemanticEmbeddings.Entity.class,
    VisualMemory.class          // إضافة كيان الذاكرة البصرية الجديد
}, version = 2, exportSchema = false)  // زيادة رقم الإصدار إلى 2
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    // Dao الموجود سابقاً
    public abstract MemoryDao memoryDao();
    
    // Dao جديد للذاكرة البصرية
    public abstract VisualMemoryDao visualMemoryDao();
    
    private static volatile AppDatabase INSTANCE;
    
    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "life_entity_brain.db"
                    )
                    .fallbackToDestructiveMigration() // يسمح بإعادة بناء الجداول أثناء التطوير
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

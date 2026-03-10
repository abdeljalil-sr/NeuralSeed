package com.lifeentity.memory;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {
    EpisodicMemory.EventEntity.class,
    IdentityMemory.class,
    SemanticEmbeddings.EmbeddingEntity.class,
    VisualMemory.class,
    ConceptEmbedding.class      // ✅ تمت إضافة ConceptEmbedding للتوافق (يمكن دمجه لاحقاً)
}, version = 3, exportSchema = false)  // تم رفع الإصدار إلى 3
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract MemoryDao memoryDao();
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
                    .fallbackToDestructiveMigration() // ✅ يسمح بالترحيل التلقائي مع حذف البيانات القديمة
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

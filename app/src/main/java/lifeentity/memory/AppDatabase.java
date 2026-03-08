// ====================== memory/AppDatabase.java (مع تحديث قائمة entities) ======================
package com.lifeentity.memory;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {
    EpisodicMemory.EventEntity.class,
    IdentityMemory.class,
    SemanticEmbeddings.EmbeddingEntity.class,  // ✅ تم التعديل
    VisualMemory.class
}, version = 2, exportSchema = false)
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
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

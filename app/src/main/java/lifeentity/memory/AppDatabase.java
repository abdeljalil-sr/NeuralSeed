package com.lifeentity.memory;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {
    EpisodicMemory.EventEntity.class,
    IdentityMemory.class,
    SemanticEmbeddings.Entity.class
}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    public abstract MemoryDao memoryDao();
    
    private static volatile AppDatabase INSTANCE;
    
    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "life_entity_brain.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}

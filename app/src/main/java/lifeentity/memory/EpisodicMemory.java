package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EpisodicMemory {
    
    @Entity(tableName = "events")
    public static class EventEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;
        public long timestamp;
        public String sensoryHash;
        public String emotionalState;
        public double emotionalIntensity;
        public String narrative;
        public String location;
    }
    
    public static class Event {
        public long timestamp;
        public String sensoryHash;
        public String emotionalState;
        public double emotionalIntensity;
        public String narrative;
        public String location;
    }
}

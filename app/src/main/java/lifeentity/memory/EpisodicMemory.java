package com.lifeentity.memory;

import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Relation;
import androidx.room.TypeConverters;

public class EpisodicMemory {

    @Entity(tableName = "events")
    @TypeConverters(Converters.class)
    public static class EventEntity {
        @PrimaryKey(autoGenerate = true)
        public long id;
        public long timestamp;
        public String narrative;
        public String emotionalState;
        public float[] affectVector;
        public float emotionalIntensity;
        public String location;
        public Long visualMemoryId;
        public String faceId;
        public float importance;
    }

    public static class Event {
        public long timestamp;
        public String sensoryHash;
        public String emotionalState;
        public double emotionalIntensity;
        public String narrative;
        public String location;
        public float[] affectVector;

        public Event() {}

        public Event(EventEntity entity) {
            this.timestamp = entity.timestamp;
            this.emotionalState = entity.emotionalState;
            this.emotionalIntensity = entity.emotionalIntensity;
            this.narrative = entity.narrative;
            this.location = entity.location;
            this.affectVector = entity.affectVector;
        }
    }

    public static class EventWithVisual {
        @Embedded
        public EventEntity event;

        @Relation(
            parentColumn = "visualMemoryId",
            entityColumn = "id"
        )
        public VisualMemory visual;
    }
}

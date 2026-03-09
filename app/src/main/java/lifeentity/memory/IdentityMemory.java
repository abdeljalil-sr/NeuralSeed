package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "identities")
@TypeConverters(Converters.class)
public class IdentityMemory {

    @PrimaryKey
    @NonNull
    public String faceHash;

    public String name;
    public String relationship;
    public float[] faceEmbedding;
    public float familiarity;
    public float trust;
    public String emotionalAssociation;
    public int encounterCount;
    public long firstSeen;
    public long lastSeen;
    public String lastContext;

    public IdentityMemory() {}

    @Ignore
    public IdentityMemory(@NonNull String faceHash, String name, String relationship, float[] faceEmbedding) {
        this.faceHash = faceHash;
        this.name = name;
        this.relationship = relationship;
        this.faceEmbedding = faceEmbedding;
        this.familiarity = 0f;
        this.trust = 0.5f;
        this.emotionalAssociation = "neutral";
        this.encounterCount = 0;
        this.firstSeen = System.currentTimeMillis();
        this.lastSeen = this.firstSeen;
    }
}

package com.lifeentity.memory;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "identities")
public class IdentityMemory {
    @PrimaryKey
    public String faceHash;
    public String name;
    public String relationship;
    public float familiarity;
    public String emotionalAssociation;
    public int encounterCount;
    public long firstSeen;
    public long lastSeen;
}

package com.lifeentity.memory;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "identities")
public class IdentityMemory {
    @PrimaryKey
    @NonNull  // ← إضافة واحدة فقط
    public String faceHash;
    public String name;
    public String relationship;
    public float familiarity;
    public String emotionalAssociation;
    public int encounterCount;
    public long firstSeen;
    public long lastSeen;
}

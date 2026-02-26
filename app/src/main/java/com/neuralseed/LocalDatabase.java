package com.neuralseed;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

/**
 * قاعدة البيانات المحلية - تخزين الذاكرة والمعرفة والتعلم
 */
public class LocalDatabase extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "NeuralSeedMemory.db";
    private static final int DATABASE_VERSION = 1;
    
    // جداول قاعدة البيانات
    private static final String TABLE_WORDS = "words";
    private static final String TABLE_SENTENCES = "sentences";
    private static final String TABLE_MEANINGS = "meanings";
    private static final String TABLE_EMOTIONS = "emotions";
    private static final String TABLE_LEARNED = "learned";
    private static final String TABLE_CORRECTIONS = "corrections";
    private static final String TABLE_CONVERSATIONS = "conversations";
    private static final String TABLE_EXPERIENCES = "experiences";
    
    public LocalDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // جدول الكلمات
        db.execSQL("CREATE TABLE " + TABLE_WORDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "word TEXT UNIQUE," +
                "root TEXT," +
                "type TEXT," +
                "meanings TEXT," +  // JSON array
                "emotions TEXT," +  // JSON object
                "familiarity REAL DEFAULT 0.5," +
                "usage_count INTEGER DEFAULT 0," +
                "last_used INTEGER," +
                "created_at INTEGER DEFAULT (strftime('%s','now') * 1000)" +
                ")");
        
        // جدول الجمل
        db.execSQL("CREATE TABLE " + TABLE_SENTENCES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sentence TEXT UNIQUE," +
                "type TEXT," +
                "structure TEXT," +  // JSON
                "emotions TEXT," +
                "is_valid INTEGER DEFAULT 1," +
                "confidence REAL DEFAULT 0.5," +
                "usage_count INTEGER DEFAULT 0," +
                "created_at INTEGER DEFAULT (strftime('%s','now') * 1000)" +
                ")");
        
        // جدول المعاني
        db.execSQL("CREATE TABLE " + TABLE_MEANINGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "concept TEXT UNIQUE," +
                "definition TEXT," +
                "synonyms TEXT," +  // JSON array
                "antonyms TEXT," +  // JSON array
                "related_concepts TEXT," +  // JSON object
                "created_at INTEGER DEFAULT (strftime('%s','now') * 1000)" +
                ")");
        
        // جدول العواطف المرتبطة بالكلمات
        db.execSQL("CREATE TABLE " + TABLE_EMOTIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "word TEXT," +
                "emotion TEXT," +
                "intensity REAL," +
                "context TEXT," +
                "created_at INTEGER DEFAULT (strftime('%s','now') * 1000)," +
                "UNIQUE(word, emotion, context)" +
                ")");
        
        // جدول ما تم تعلمه
        db.execSQL("CREATE TABLE " + TABLE_LEARNED + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "input TEXT," +
                "output TEXT," +
                "context TEXT," +
                "success INTEGER DEFAULT 1," +
                "feedback TEXT," +
                "learned_at INTEGER DEFAULT (strftime('%s','now') * 1000)" +
                ")");
        
        // جدول التصحيحات
        db.execSQL("CREATE TABLE " + TABLE_CORRECTIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "original TEXT," +
                "corrected TEXT," +
                "explanation TEXT," +
                "learned INTEGER DEFAULT 0," +
                "corrected_at INTEGER DEFAULT (strftime('%s','now') * 1000)" +
                ")");
        
        // جدول المحادثات
        db.execSQL("CREATE TABLE " + TABLE_CONVERSATIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_message TEXT," +
                "ai_response TEXT," +
                "emotions TEXT," +  // JSON
                "context TEXT," +
                "timestamp INTEGER DEFAULT (strftime('%s','now') * 1000)" +
                ")");
        
        // جدول التجارب
        db.execSQL("CREATE TABLE " + TABLE_EXPERIENCES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "description TEXT," +
                "emotions TEXT," +
                "outcome TEXT," +
                "lesson TEXT," +
                "importance REAL DEFAULT 0.5," +
                "timestamp INTEGER DEFAULT (strftime('%s','now') * 1000)" +
                ")");
        
        // إنشاء الفهارس
        db.execSQL("CREATE INDEX idx_words_word ON " + TABLE_WORDS + "(word)");
        db.execSQL("CREATE INDEX idx_words_root ON " + TABLE_WORDS + "(root)");
        db.execSQL("CREATE INDEX idx_words_type ON " + TABLE_WORDS + "(type)");
        db.execSQL("CREATE INDEX idx_sentences_type ON " + TABLE_SENTENCES + "(type)");
        db.execSQL("CREATE INDEX idx_emotions_word ON " + TABLE_EMOTIONS + "(word)");
        db.execSQL("CREATE INDEX idx_conversations_time ON " + TABLE_CONVERSATIONS + "(timestamp)");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENTENCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEANINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EMOTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEARNED);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CORRECTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONVERSATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPERIENCES);
        onCreate(db);
    }
    
    // ===== إدارة الكلمات =====
    
    public void saveWord(ArabicLexicon.Word word) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("word", word.word);
        values.put("root", word.root);
        values.put("type", word.type.name());
        
        try {
            values.put("meanings", new JSONArray(word.meanings).toString());
            values.put("emotions", new JSONObject(word.emotions).toString());
        } catch (Exception e) {
            values.put("meanings", "[]");
            values.put("emotions", "{}");
        }
        
        values.put("familiarity", word.familiarity);
        values.put("usage_count", word.usageCount);
        values.put("last_used", word.lastUsed);
        
        db.insertWithOnConflict(TABLE_WORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    public ArabicLexicon.Word loadWord(String wordText) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WORDS, null, "word = ?", 
                                 new String[]{wordText}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            ArabicLexicon.WordType type = ArabicLexicon.WordType.valueOf(
                cursor.getString(cursor.getColumnIndexOrThrow("type")));
            
            ArabicLexicon.Word word = new ArabicLexicon.Word(
                cursor.getString(cursor.getColumnIndexOrThrow("root")),
                wordText,
                type
            );
            
            try {
                String meaningsJson = cursor.getString(cursor.getColumnIndexOrThrow("meanings"));
                JSONArray meaningsArray = new JSONArray(meaningsJson);
                for (int i = 0; i < meaningsArray.length(); i++) {
                    word.meanings.add(meaningsArray.getString(i));
                }
                
                String emotionsJson = cursor.getString(cursor.getColumnIndexOrThrow("emotions"));
                JSONObject emotionsObj = new JSONObject(emotionsJson);
                Iterator<String> keys = emotionsObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    word.emotions.put(key, emotionsObj.getDouble(key));
                }
            } catch (JSONException e) {
                // تجاهل خطأ JSON
            }
            
            word.familiarity = cursor.getDouble(cursor.getColumnIndexOrThrow("familiarity"));
            word.usageCount = cursor.getInt(cursor.getColumnIndexOrThrow("usage_count"));
            word.lastUsed = cursor.getLong(cursor.getColumnIndexOrThrow("last_used"));
            
            cursor.close();
            return word;
        }
        
        if (cursor != null) cursor.close();
        return null;
    }
    
    public List<ArabicLexicon.Word> loadAllWords() {
        List<ArabicLexicon.Word> words = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_WORDS, null, null, null, null, null, "usage_count DESC");
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String wordText = cursor.getString(cursor.getColumnIndexOrThrow("word"));
                ArabicLexicon.Word word = loadWord(wordText);
                if (word != null) words.add(word);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return words;
    }
    
    public void updateWordUsage(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_WORDS + 
                   " SET usage_count = usage_count + 1, last_used = ? WHERE word = ?",
                   new Object[]{System.currentTimeMillis(), word});
    }
    
    // ===== إدارة الجمل =====
    
    public void saveSentence(String sentence, ArabicParser.SentenceType type, 
                            String structure, Map<String, Double> emotions, 
                            boolean isValid, double confidence) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("sentence", sentence);
        values.put("type", type.name());
        values.put("structure", structure);
        
        try {
            values.put("emotions", new JSONObject(emotions).toString());
        } catch (Exception e) {
            values.put("emotions", "{}");
        }
        
        values.put("is_valid", isValid ? 1 : 0);
        values.put("confidence", confidence);
        
        db.insertWithOnConflict(TABLE_SENTENCES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    public List<String> getSimilarSentences(String pattern, int limit) {
        List<String> sentences = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_SENTENCES, new String[]{"sentence"},
                                 "sentence LIKE ?", new String[]{"%" + pattern + "%"},
                                 null, null, "usage_count DESC", String.valueOf(limit));
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                sentences.add(cursor.getString(0));
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return sentences;
    }
    
    // ===== إدارة المعاني =====
    
    public void saveMeaning(SemanticEmotionalEngine.Meaning meaning) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("concept", meaning.concept);
        values.put("definition", meaning.definition);
        
        try {
            values.put("synonyms", new JSONArray(meaning.synonyms).toString());
            values.put("antonyms", new JSONArray(meaning.antonyms).toString());
            values.put("related_concepts", new JSONObject(meaning.relatedConcepts).toString());
        } catch (Exception e) {
            values.put("synonyms", "[]");
            values.put("antonyms", "[]");
            values.put("related_concepts", "{}");
        }
        
        db.insertWithOnConflict(TABLE_MEANINGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    public SemanticEmotionalEngine.Meaning loadMeaning(String concept) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEANINGS, null, "concept = ?",
                                 new String[]{concept}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            SemanticEmotionalEngine.Meaning meaning = new SemanticEmotionalEngine.Meaning(
                concept,
                cursor.getString(cursor.getColumnIndexOrThrow("definition"))
            );
            
            try {
                String synonymsJson = cursor.getString(cursor.getColumnIndexOrThrow("synonyms"));
                JSONArray synonymsArray = new JSONArray(synonymsJson);
                for (int i = 0; i < synonymsArray.length(); i++) {
                    meaning.synonyms.add(synonymsArray.getString(i));
                }
                
                String antonymsJson = cursor.getString(cursor.getColumnIndexOrThrow("antonyms"));
                JSONArray antonymsArray = new JSONArray(antonymsJson);
                for (int i = 0; i < antonymsArray.length(); i++) {
                    meaning.antonyms.add(antonymsArray.getString(i));
                }
                
                String relatedJson = cursor.getString(cursor.getColumnIndexOrThrow("related_concepts"));
                JSONObject relatedObj = new JSONObject(relatedJson);
                Iterator<String> keys = relatedObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    meaning.relatedConcepts.put(key, relatedObj.getDouble(key));
                }
            } catch (JSONException e) {
                // تجاهل خطأ JSON
            }
            
            cursor.close();
            return meaning;
        }
        
        if (cursor != null) cursor.close();
        return null;
    }
    
    // ===== إدارة العواطف =====
    
    public void saveEmotionLink(String word, String emotion, double intensity, String context) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("word", word);
        values.put("emotion", emotion);
        values.put("intensity", intensity);
        values.put("context", context);
        
        db.insertWithOnConflict(TABLE_EMOTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    public Map<String, Double> getWordEmotions(String word) {
        Map<String, Double> emotions = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_EMOTIONS, new String[]{"emotion", "intensity"},
                                 "word = ?", new String[]{word},
                                 null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String emotion = cursor.getString(0);
                double intensity = cursor.getDouble(1);
                emotions.merge(emotion, intensity, Double::sum);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return emotions;
    }
    
    // ===== إدارة التعلم =====
    
    public void recordLearning(String input, String output, String context, 
                               boolean success, String feedback) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("input", input);
        values.put("output", output);
        values.put("context", context);
        values.put("success", success ? 1 : 0);
        values.put("feedback", feedback);
        
        db.insert(TABLE_LEARNED, null, values);
    }
    
    public List<Map<String, Object>> getLearningHistory(String context, int limit) {
        List<Map<String, Object>> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = context != null ? "context = ?" : null;
        String[] selectionArgs = context != null ? new String[]{context} : null;
        
        Cursor cursor = db.query(TABLE_LEARNED, null, selection, selectionArgs,
                                 null, null, "learned_at DESC", String.valueOf(limit));
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> record = new HashMap<>();
                record.put("input", cursor.getString(cursor.getColumnIndexOrThrow("input")));
                record.put("output", cursor.getString(cursor.getColumnIndexOrThrow("output")));
                record.put("context", cursor.getString(cursor.getColumnIndexOrThrow("context")));
                record.put("success", cursor.getInt(cursor.getColumnIndexOrThrow("success")) == 1);
                record.put("feedback", cursor.getString(cursor.getColumnIndexOrThrow("feedback")));
                record.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("learned_at")));
                history.add(record);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return history;
    }
    
    // ===== إدارة التصحيحات =====
    
    public void saveCorrection(String original, String corrected, String explanation) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("original", original);
        values.put("corrected", corrected);
        values.put("explanation", explanation);
        values.put("learned", 0);
        
        db.insert(TABLE_CORRECTIONS, null, values);
    }
    
    public void markCorrectionLearned(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("learned", 1);
        db.update(TABLE_CORRECTIONS, values, "id = ?", new String[]{String.valueOf(id)});
    }
    
    public List<Map<String, Object>> getPendingCorrections() {
        List<Map<String, Object>> corrections = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_CORRECTIONS, null, "learned = 0",
                                 null, null, null, "corrected_at DESC");
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> correction = new HashMap<>();
                correction.put("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                correction.put("original", cursor.getString(cursor.getColumnIndexOrThrow("original")));
                correction.put("corrected", cursor.getString(cursor.getColumnIndexOrThrow("corrected")));
                correction.put("explanation", cursor.getString(cursor.getColumnIndexOrThrow("explanation")));
                corrections.add(correction);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return corrections;
    }
    
    public String findCorrection(String text) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_CORRECTIONS, 
                                 new String[]{"corrected", "explanation"},
                                 "original = ? AND learned = 1",
                                 new String[]{text}, null, null, null, "1");
        
        if (cursor != null && cursor.moveToFirst()) {
            String corrected = cursor.getString(0);
            cursor.close();
            return corrected;
        }
        
        if (cursor != null) cursor.close();
        return null;
    }
    
    // ===== إدارة المحادثات =====
    
    public void saveConversation(String userMessage, String aiResponse, 
                                 Map<String, Double> emotions, String context) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("user_message", userMessage);
        values.put("ai_response", aiResponse);
        
        try {
            values.put("emotions", new JSONObject(emotions).toString());
        } catch (Exception e) {
            values.put("emotions", "{}");
        }
        
        values.put("context", context);
        
        db.insert(TABLE_CONVERSATIONS, null, values);
    }
    
    public List<Map<String, Object>> getConversationHistory(int limit) {
        List<Map<String, Object>> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_CONVERSATIONS, null, null, null, null, null,
                                 "timestamp DESC", String.valueOf(limit));
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> record = new HashMap<>();
                record.put("user_message", cursor.getString(cursor.getColumnIndexOrThrow("user_message")));
                record.put("ai_response", cursor.getString(cursor.getColumnIndexOrThrow("ai_response")));
                record.put("context", cursor.getString(cursor.getColumnIndexOrThrow("context")));
                record.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                
                try {
                    String emotionsJson = cursor.getString(cursor.getColumnIndexOrThrow("emotions"));
                    record.put("emotions", new JSONObject(emotionsJson));
                } catch (JSONException e) {
                    record.put("emotions", new JSONObject());
                }
                
                history.add(record);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return history;
    }
    
    // ===== إدارة التجارب =====
    
    public void saveExperience(String description, Map<String, Double> emotions,
                               String outcome, String lesson, double importance) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put("description", description);
        
        try {
            values.put("emotions", new JSONObject(emotions).toString());
        } catch (Exception e) {
            values.put("emotions", "{}");
        }
        
        values.put("outcome", outcome);
        values.put("lesson", lesson);
        values.put("importance", importance);
        
        db.insert(TABLE_EXPERIENCES, null, values);
    }
    
    public List<Map<String, Object>> getImportantExperiences(double minImportance, int limit) {
        List<Map<String, Object>> experiences = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_EXPERIENCES, null, "importance >= ?",
                                 new String[]{String.valueOf(minImportance)},
                                 null, null, "importance DESC, timestamp DESC",
                                 String.valueOf(limit));
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Map<String, Object> exp = new HashMap<>();
                exp.put("description", cursor.getString(cursor.getColumnIndexOrThrow("description")));
                exp.put("outcome", cursor.getString(cursor.getColumnIndexOrThrow("outcome")));
                exp.put("lesson", cursor.getString(cursor.getColumnIndexOrThrow("lesson")));
                exp.put("importance", cursor.getDouble(cursor.getColumnIndexOrThrow("importance")));
                exp.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")));
                experiences.add(exp);
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return experiences;
    }
    
    // ===== إحصائيات =====
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // عدد الكلمات
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORDS, null);
        if (cursor != null && cursor.moveToFirst()) {
            stats.put("word_count", cursor.getInt(0));
            cursor.close();
        }
        
        // عدد الجمل
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SENTENCES, null);
        if (cursor != null && cursor.moveToFirst()) {
            stats.put("sentence_count", cursor.getInt(0));
            cursor.close();
        }
        
        // عدد المحادثات
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CONVERSATIONS, null);
        if (cursor != null && cursor.moveToFirst()) {
            stats.put("conversation_count", cursor.getInt(0));
            cursor.close();
        }
        
        // عدد التصحيحات المعلمة
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CORRECTIONS + " WHERE learned = 1", null);
        if (cursor != null && cursor.moveToFirst()) {
            stats.put("learned_corrections", cursor.getInt(0));
            cursor.close();
        }
        
        // عدد التصحيحات المعلقة
        cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CORRECTIONS + " WHERE learned = 0", null);
        if (cursor != null && cursor.moveToFirst()) {
            stats.put("pending_corrections", cursor.getInt(0));
            cursor.close();
        }
        
        return stats;
    }
    
    // ===== البحث =====
    
    public List<String> searchWords(String query) {
        List<String> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_WORDS, new String[]{"word"},
                                 "word LIKE ? OR meanings LIKE ?",
                                 new String[]{"%" + query + "%", "%" + query + "%"},
                                 null, null, "familiarity DESC", "20");
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                results.add(cursor.getString(0));
            } while (cursor.moveToNext());
            cursor.close();
        }
        
        return results;
    }
    
    // ===== النسخ الاحتياطي والاستعادة =====
    
    public String exportToJson() {
        JSONObject export = new JSONObject();
        SQLiteDatabase db = this.getReadableDatabase();
        
        try {
            // تصدير الكلمات
            JSONArray wordsArray = new JSONArray();
            Cursor cursor = db.query(TABLE_WORDS, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    JSONObject word = new JSONObject();
                    word.put("word", cursor.getString(cursor.getColumnIndexOrThrow("word")));
                    word.put("root", cursor.getString(cursor.getColumnIndexOrThrow("root")));
                    word.put("type", cursor.getString(cursor.getColumnIndexOrThrow("type")));
                    word.put("meanings", cursor.getString(cursor.getColumnIndexOrThrow("meanings")));
                    word.put("emotions", cursor.getString(cursor.getColumnIndexOrThrow("emotions")));
                    word.put("familiarity", cursor.getDouble(cursor.getColumnIndexOrThrow("familiarity")));
                    word.put("usage_count", cursor.getInt(cursor.getColumnIndexOrThrow("usage_count")));
                    wordsArray.put(word);
                } while (cursor.moveToNext());
                cursor.close();
            }
            export.put("words", wordsArray);
            
            // تصدير المعاني
            JSONArray meaningsArray = new JSONArray();
            cursor = db.query(TABLE_MEANINGS, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    JSONObject meaning = new JSONObject();
                    meaning.put("concept", cursor.getString(cursor.getColumnIndexOrThrow("concept")));
                    meaning.put("definition", cursor.getString(cursor.getColumnIndexOrThrow("definition")));
                    meaning.put("synonyms", cursor.getString(cursor.getColumnIndexOrThrow("synonyms")));
                    meaning.put("antonyms", cursor.getString(cursor.getColumnIndexOrThrow("antonyms")));
                    meaningsArray.put(meaning);
                } while (cursor.moveToNext());
                cursor.close();
            }
            export.put("meanings", meaningsArray);
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return export.toString();
    }
    
    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_WORDS);
        db.execSQL("DELETE FROM " + TABLE_SENTENCES);
        db.execSQL("DELETE FROM " + TABLE_MEANINGS);
        db.execSQL("DELETE FROM " + TABLE_EMOTIONS);
        db.execSQL("DELETE FROM " + TABLE_LEARNED);
        db.execSQL("DELETE FROM " + TABLE_CORRECTIONS);
        db.execSQL("DELETE FROM " + TABLE_CONVERSATIONS);
        db.execSQL("DELETE FROM " + TABLE_EXPERIENCES);
    }
}

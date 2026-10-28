package com.omnisys.pricechallenge;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "omnisys_game.db";
    private static final int DB_VERSION = 4;

    public LocalDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS products (" +
                        "name TEXT PRIMARY KEY NOT NULL, " +
                        "category TEXT, " +
                        "price REAL NOT NULL, " +
                        "image_name TEXT)"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS game_stats (" +
                        "id INTEGER PRIMARY KEY CHECK(id=1), " +
                        "points INTEGER NOT NULL, " +
                        "rounds INTEGER NOT NULL)"
        );

        db.execSQL(
                "INSERT OR IGNORE INTO game_stats(id, points, rounds) " +
                        "VALUES(1, 0, 0)"
        );

        createUserTables(db);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 3) {
            createUserTables(db);
        }

        if (oldVersion < 4) {
            db.execSQL(
                    "ALTER TABLE game_results " +
                            "ADD COLUMN result_id TEXT"
            );

            db.execSQL(
                    "ALTER TABLE game_results " +
                            "ADD COLUMN synced INTEGER NOT NULL DEFAULT 0"
            );

            db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_results_result_id " +
                            "ON game_results(result_id)"
            );
        }
    }

    private void createUserTables(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "username TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                        "created_at INTEGER NOT NULL)"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS game_results (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "result_id TEXT UNIQUE, " +
                        "user_id INTEGER NOT NULL, " +
                        "score INTEGER NOT NULL, " +
                        "correct_answers INTEGER NOT NULL, " +
                        "total_questions INTEGER NOT NULL, " +
                        "played_at INTEGER NOT NULL, " +
                        "synced INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(user_id) REFERENCES users(id) " +
                        "ON DELETE CASCADE)"
        );

        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_results_ranking " +
                        "ON game_results(score DESC, played_at ASC)"
        );

        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_results_user " +
                        "ON game_results(user_id)"
        );

        db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_results_result_id " +
                        "ON game_results(result_id)"
        );
    }

    public void saveProduct(Product product) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", product.name);
        values.put("category", product.category);
        values.put("price", product.price);
        values.put("image_name", product.imageName);

        db.insertWithOnConflict(
                "products",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public List<Product> getProducts() {
        List<Product> products = new ArrayList<>();

        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT name, category, price, image_name " +
                        "FROM products ORDER BY name",
                null
        );

        try {
            while (cursor.moveToNext()) {
                products.add(new Product(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getDouble(2),
                        cursor.getString(3)
                ));
            }
        } finally {
            cursor.close();
        }

        return products;
    }

    public int getProductCount() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM products",
                null
        );

        int count = 0;
        try {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            cursor.close();
        }

        return count;
    }

    public void saveStats(int points, int rounds) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("points", points);
        values.put("rounds", rounds);

        db.insertWithOnConflict(
                "game_stats",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
    }

    public int[] getStats() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT points, rounds FROM game_stats WHERE id=1",
                null
        );

        int[] result = {0, 0};
        try {
            if (cursor.moveToFirst()) {
                result[0] = cursor.getInt(0);
                result[1] = cursor.getInt(1);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public User getOrCreateUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String cleanName = username.trim();
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("username", cleanName);
        values.put("created_at", System.currentTimeMillis());

        db.insertWithOnConflict(
                "users",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
        );

        Cursor cursor = db.query(
                "users",
                new String[]{"id", "username", "created_at"},
                "username = ?",
                new String[]{cleanName},
                null,
                null,
                null,
                "1"
        );

        try {
            if (cursor.moveToFirst()) {
                return new User(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2)
                );
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    public String saveGameResult(
            long userId,
            int score,
            int correctAnswers,
            int totalQuestions) {

        SQLiteDatabase db = getWritableDatabase();
        String resultId = java.util.UUID.randomUUID().toString();

        ContentValues values = new ContentValues();
        values.put("result_id", resultId);
        values.put("user_id", userId);
        values.put("score", score);
        values.put("correct_answers", correctAnswers);
        values.put("total_questions", totalQuestions);
        values.put("played_at", System.currentTimeMillis());
        values.put("synced", 0);

        db.insertOrThrow("game_results", null, values);
        return resultId;
    }

    /**
     * Bidali gabeko emaitzak itzultzen ditu, zerbitzariarekin sinkronizatzeko.
     * Emaitza bakoitzak APIak behar dituen eremuak ditu.
     */
    public JSONArray getPendingResults() {
        JSONArray pending = new JSONArray();

        String sql =
                "SELECT gr.result_id, u.username, gr.score, " +
                        "gr.correct_answers, gr.total_questions, gr.played_at " +
                        "FROM game_results gr " +
                        "INNER JOIN users u ON u.id = gr.user_id " +
                        "WHERE gr.synced = 0 " +
                        "ORDER BY gr.played_at ASC";

        Cursor cursor = getReadableDatabase().rawQuery(sql, null);

        try {
            while (cursor.moveToNext()) {
                JSONObject item = new JSONObject();
                item.put("resultId", cursor.getString(0));
                item.put("username", cursor.getString(1));
                item.put("score", cursor.getInt(2));
                item.put("correctAnswers", cursor.getInt(3));
                item.put("totalQuestions", cursor.getInt(4));
                item.put("playedAt", cursor.getLong(5));
                pending.put(item);
            }
        } catch (JSONException e) {
            android.util.Log.e("OmniSysRanking",
                    "Ezin izan dira bidaltzeko emaitzak prestatu", e);
        } finally {
            cursor.close();
        }

        return pending;
    }

    /**
     * Emaitza zerbitzariak onartu ondoren bakarrik markatu synced=1.
     */
    public void markResultSynced(String resultId) {
        if (resultId == null || resultId.trim().isEmpty()) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("synced", 1);

        getWritableDatabase().update(
                "game_results",
                values,
                "result_id = ?",
                new String[]{resultId}
        );
    }

    public List<LeaderboardEntry> getTopFive() {
        List<LeaderboardEntry> ranking = new ArrayList<>();

        String sql =
                "SELECT u.id, u.username, MAX(gr.score) AS best_score, " +
                        "(SELECT MIN(gr2.played_at) " +
                        " FROM game_results gr2 " +
                        " WHERE gr2.user_id = u.id " +
                        " AND gr2.score = (" +
                        "     SELECT MAX(gr3.score) " +
                        "     FROM game_results gr3 " +
                        "     WHERE gr3.user_id = u.id" +
                        " )) AS best_time " +
                        "FROM users u " +
                        "INNER JOIN game_results gr ON gr.user_id = u.id " +
                        "GROUP BY u.id, u.username " +
                        "ORDER BY best_score DESC, best_time ASC, " +
                        "u.username COLLATE NOCASE ASC " +
                        "LIMIT 5";

        Cursor cursor = getReadableDatabase().rawQuery(sql, null);

        try {
            while (cursor.moveToNext()) {
                ranking.add(new LeaderboardEntry(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getLong(3)
                ));
            }
        } finally {
            cursor.close();
        }

        return ranking;
    }
}

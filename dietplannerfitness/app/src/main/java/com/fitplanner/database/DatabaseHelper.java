package com.fitplanner.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "fitplanner.db";
    private static final int DATABASE_VERSION = 3;

    // Users Table
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_NAME = "name";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_PASSWORD = "password";
    public static final String COL_USER_AGE = "age";
    public static final String COL_USER_GENDER = "gender";
    public static final String COL_USER_HEIGHT = "height";
    public static final String COL_USER_CURRENT_WEIGHT = "currentWeight";
    public static final String COL_USER_TARGET_WEIGHT = "targetWeight";
    public static final String COL_USER_STARTING_WEIGHT = "startingWeight";
    public static final String COL_USER_ACTIVITY_LEVEL = "activityLevel";
    public static final String COL_USER_FITNESS_GOAL = "fitnessGoal";
    public static final String COL_USER_DIET_TYPE = "dietType";

    // WeightLogs Table
    public static final String TABLE_WEIGHT_LOGS = "weight_logs";
    public static final String COL_LOG_ID = "id";
    public static final String COL_LOG_USER_ID = "userId";
    public static final String COL_LOG_WEIGHT = "weight";
    public static final String COL_LOG_DATE = "date";

    // Create Users Table Query
    private static final String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " ("
            + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_USER_NAME + " TEXT, "
            + COL_USER_EMAIL + " TEXT UNIQUE, "
            + COL_USER_PASSWORD + " TEXT, "
            + COL_USER_AGE + " INTEGER, "
            + COL_USER_GENDER + " TEXT, "
            + COL_USER_HEIGHT + " REAL, "
            + COL_USER_CURRENT_WEIGHT + " REAL, "
            + COL_USER_TARGET_WEIGHT + " REAL, "
            + COL_USER_STARTING_WEIGHT + " REAL, "
            + COL_USER_ACTIVITY_LEVEL + " TEXT, "
            + COL_USER_FITNESS_GOAL + " TEXT, "
            + COL_USER_DIET_TYPE + " TEXT)";

    // Create WeightLogs Table Query
    private static final String CREATE_WEIGHT_LOGS_TABLE = "CREATE TABLE " + TABLE_WEIGHT_LOGS + " ("
            + COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_LOG_USER_ID + " INTEGER, "
            + COL_LOG_WEIGHT + " REAL, "
            + COL_LOG_DATE + " TEXT, "
            + "FOREIGN KEY(" + COL_LOG_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_USERS_TABLE);
            db.execSQL(CREATE_WEIGHT_LOGS_TABLE);
            Log.d(TAG, "Tables created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating tables: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_USER_STARTING_WEIGHT + " REAL");
                Log.d(TAG, "Added startingWeight column to users table");
            } catch (Exception e) {
                Log.e(TAG, "Error adding column: " + e.getMessage());
                // Fallback: recreate tables if migration fails
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHT_LOGS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
                onCreate(db);
            }
        } else {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHT_LOGS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
    }

    // --- User Operations ---

    /**
     * Inserts a new user into the database.
     */
    public long insertUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_NAME, name);
        values.put(COL_USER_EMAIL, email);
        values.put(COL_USER_PASSWORD, password);

        try {
            return db.insert(TABLE_USERS, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting user: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Retrieves a user by their email.
     */
    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_USERS, null, COL_USER_EMAIL + "=?",
                new String[]{email}, null, null, null);
    }

    /**
     * Updates user profile information.
     */
    public int updateUserProfile(int userId, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.update(TABLE_USERS, values, COL_USER_ID + "=?", new String[]{String.valueOf(userId)});
        } catch (Exception e) {
            Log.e(TAG, "Error updating user profile: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Deletes a user and their associated logs.
     */
    public void deleteUser(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_WEIGHT_LOGS, COL_LOG_USER_ID + "=?", new String[]{String.valueOf(userId)});
            db.delete(TABLE_USERS, COL_USER_ID + "=?", new String[]{String.valueOf(userId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting user: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Checks if a user exists and verifies password for login.
     */
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USER_EMAIL + "=? AND " + COL_USER_PASSWORD + "=?",
                new String[]{email, password}, null, null, null);
        
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }
    
    /**
     * Checks if email already exists in database.
     */
    public boolean isEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USER_EMAIL + "=?", new String[]{email}, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    /**
     * Retrieves User ID by email.
     */
    public int getUserIdByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USER_EMAIL + "=?", new String[]{email}, null, null, null);
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    /**
     * Checks if a user's profile is complete (e.g., age and height are set).
     */
    public boolean isProfileComplete(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COL_USER_EMAIL + "=?",
                new String[]{email}, null, null, null);

        boolean isComplete = false;
        if (cursor != null && cursor.moveToFirst()) {
            int age = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_AGE));
            double height = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_USER_HEIGHT));
            // Consider profile complete if age and height are greater than 0
            if (age > 0 && height > 0) {
                isComplete = true;
            }
            cursor.close();
        }
        return isComplete;
    }

    // --- Weight Log Operations ---

    /**
     * Inserts a new weight entry for a user.
     */
    public long insertWeightLog(int userId, double weight, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LOG_USER_ID, userId);
        values.put(COL_LOG_WEIGHT, weight);
        values.put(COL_LOG_DATE, date);

        try {
            return db.insert(TABLE_WEIGHT_LOGS, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting weight log: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Gets weight history for a specific user.
     */
    public Cursor getWeightHistory(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_WEIGHT_LOGS, null, COL_LOG_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, COL_LOG_DATE + " DESC");
    }

    /**
     * Deletes a specific weight log.
     */
    public int deleteWeightLog(int logId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WEIGHT_LOGS, COL_LOG_ID + "=?", new String[]{String.valueOf(logId)});
    }

    /**
     * Deletes all weight history for a specific user.
     */
    public int deleteAllWeightLogs(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WEIGHT_LOGS, COL_LOG_USER_ID + "=?", new String[]{String.valueOf(userId)});
    }
}

package com.asb.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "LocationDB";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "locations";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_SHORT = "short";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LATITUDE + " REAL, " +
                COLUMN_LONGITUDE + " REAL, " +
                COLUMN_TIMESTAMP + " TEXT, " +
                COLUMN_USER_ID + " TEXT, " +
                COLUMN_SHORT + " TEXT)";
        db.execSQL(CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert location data
    public boolean insertLocation(double latitude, double longitude, String timestamp, String userId, String softName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_SHORT, softName);

        long result = db.insert(TABLE_NAME, null, values);

        db.close();

        return result != -1; // Return true if insertion was successful
    }

    // Retrieve all location data
    public Cursor getAllLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }
    public void delete(String sql) {
        SQLiteDatabase db = this.getWritableDatabase();  // Get a writable database instance
        db.execSQL(sql);  // Execute the SQL query (in this case, a DELETE query)
        db.close();  // Close the database to avoid memory leaks
    }

}

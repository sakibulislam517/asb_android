package com.asb.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefHelper {
    private static final String PREF_NAME = "user_data";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // Constructor
    public SharedPrefHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // ✅ Add/Update Data
    public void addData(String key, String value) {
        editor.putString(key, value);
        editor.apply();  // Apply changes asynchronously
    }

    // ✅ Get Data
    public String getData(String key) {
        return sharedPreferences.getString(key, "");  // Default empty string if key not found
    }

    // ✅ Delete Specific Data
    public void deleteData(String key) {
        editor.remove(key);
        editor.apply();
    }

    // ✅ Clear All Data
    public void clearAllData() {
        editor.clear();
        editor.apply();
    }
}

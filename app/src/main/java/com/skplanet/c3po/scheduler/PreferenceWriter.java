package com.skplanet.c3po.scheduler;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by 1002407 on 15. 6. 24..
 */
public class PreferenceWriter {
    private static String PREF_NAME = "log";

    public static void remove(Context con, String variable) {
        SharedPreferences prefs = con.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(variable).commit();
    }

    public static void write(Context con, String variable, String data) {
        SharedPreferences prefs = con.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(variable, data).commit();
    }

    public static String getString(Context con, String variable, String defaultValue) {
        SharedPreferences prefs = con.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(variable, defaultValue);
        return data;
    }
}

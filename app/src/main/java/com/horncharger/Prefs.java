package com.horncharger;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

public final class Prefs {

    private static final String NAME = "horncharger";
    private static final String K_VOL = "volume";
    private static final String K_CHARGER = "charger_enabled";
    private static final String K_HEADPHONE = "headphone_enabled";
    private static final String SOUND_FILE = "charge_sound";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static File getSoundFile(Context c) {
        return new File(c.getFilesDir(), SOUND_FILE);
    }

    public static int getVolume(Context c) {
        return sp(c).getInt(K_VOL, 100);
    }

    public static void setVolume(Context c, int v) {
        sp(c).edit().putInt(K_VOL, v).apply();
    }

    public static boolean isChargerEnabled(Context c) {
        return sp(c).getBoolean(K_CHARGER, true);
    }

    public static void setChargerEnabled(Context c, boolean enabled) {
        sp(c).edit().putBoolean(K_CHARGER, enabled).apply();
    }

    public static boolean isHeadphoneEnabled(Context c) {
        return sp(c).getBoolean(K_HEADPHONE, true);
    }

    public static void setHeadphoneEnabled(Context c, boolean enabled) {
        sp(c).edit().putBoolean(K_HEADPHONE, enabled).apply();
    }

    public static boolean isAnyEnabled(Context c) {
        return isChargerEnabled(c) || isHeadphoneEnabled(c);
    }
}

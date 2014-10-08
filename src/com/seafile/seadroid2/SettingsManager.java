package com.seafile.seadroid2;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Access the app settings
 */
public final class SettingsManager {

    // Gesture Lock
	public static final String GESTURE_LOCK_SWITCH_KEY = "gesture_lock_switch_key";
	public static final String LOCK_KEY = "gesture_lock_key";

    // Camera upload
    public static final String ALLOW_MOBILE_CONNECTIONS_SWITCH_KEY = "allow_mobile_connections_switch_key";
	public static final String CAMERA_UPLOAD_SWITCH_KEY = "camera_upload_switch_key";

    private SharedPreferences sharedPref = SeadroidApplication
        .getAppContext()
        .getSharedPreferences(AccountsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);

    private static SettingsManager instance;

    public static synchronized SettingsManager instance() {
        if (instance == null) {
            instance = new SettingsManager();
        }

        return instance;
    }

    public boolean isGestureLockEnabled() {
        return sharedPref.getBoolean(GESTURE_LOCK_SWITCH_KEY, false);
    }
}

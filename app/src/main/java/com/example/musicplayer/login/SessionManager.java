package com.example.musicplayer.login;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "DeezerSession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, String refreshToken, int expiresIn) {
        // expiresIn is in seconds, convert to milliseconds for expiration time
        long expiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putLong(KEY_EXPIRES_AT, expiresAt);
        editor.apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public boolean isTokenValid() {
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
        // Check if token exists and is not expired
        return getAccessToken() != null && System.currentTimeMillis() < expiresAt;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}

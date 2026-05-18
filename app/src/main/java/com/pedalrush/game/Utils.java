package com.pedalrush.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

/**
 * Utils - Static helper methods used across the project.
 */
public final class Utils {

    private static final String PREFS_NAME  = "pedalrush_prefs";
    private static final String KEY_BEST    = "best_score";

    // Prevent instantiation
    private Utils() {}

    // ── Score persistence ─────────────────────────────────────────────────────

    public static int getBestScore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_BEST, 0);
    }

    public static void saveBestScore(Context context, int score) {
        SharedPreferences.Editor editor = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_BEST, score);
        editor.apply();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /**
     * Enables full immersive sticky mode (hides status bar and nav bar).
     */
    public static void setImmersiveMode(View decorView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    /**
     * Clamps a float value between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Linear interpolation.
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}

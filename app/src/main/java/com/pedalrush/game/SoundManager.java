package com.pedalrush.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * SoundManager - Singleton that manages all game sounds.
 * Uses ToneGenerator for lightweight sound effects (no audio files needed).
 * Settings persisted via SharedPreferences.
 */
public class SoundManager {

    private static final String TAG   = "SoundManager";
    private static final String PREFS = "pedalrush_prefs";
    private static final String KEY_SOUND = "sound_enabled";

    private static SoundManager instance;

    private final Context context;
    private boolean soundEnabled;

    // ToneGenerator for SFX
    private ToneGenerator toneGen;

    // Background music (simple beep sequence via Handler)
    private Handler musicHandler;
    private Runnable musicRunnable;
    private boolean musicPlaying = false;
    private int musicBeat = 0;

    // Simple music note sequence (DTMF tones are coarse but work without assets)
    private static final int[] MUSIC_TONES = {
        ToneGenerator.TONE_DTMF_1,
        ToneGenerator.TONE_DTMF_3,
        ToneGenerator.TONE_DTMF_5,
        ToneGenerator.TONE_DTMF_3,
        ToneGenerator.TONE_DTMF_1,
        ToneGenerator.TONE_DTMF_5,
        ToneGenerator.TONE_DTMF_8,
        ToneGenerator.TONE_DTMF_5,
    };

    private SoundManager(Context context) {
        this.context = context.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        soundEnabled = prefs.getBoolean(KEY_SOUND, true);

        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
        } catch (Exception e) {
            Log.w(TAG, "ToneGenerator init failed: " + e.getMessage());
            toneGen = null;
        }

        musicHandler = new Handler(Looper.getMainLooper());
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    // ── Sound Effects ─────────────────────────────────────────────────────────

    /** Short blip for jump. */
    public void playJump() {
        if (!soundEnabled || toneGen == null) return;
        try {
            toneGen.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 80);
        } catch (Exception e) { /* ignore */ }
    }

    /** Bright ding for coin. */
    public void playCoin() {
        if (!soundEnabled || toneGen == null) return;
        try {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 60);
        } catch (Exception e) { /* ignore */ }
    }

    /** Harsh buzz for crash. */
    public void playCrash() {
        if (!soundEnabled || toneGen == null) return;
        try {
            toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 400);
        } catch (Exception e) { /* ignore */ }
    }

    /** Short click for buttons. */
    public void playButtonClick() {
        if (!soundEnabled || toneGen == null) return;
        try {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 40);
        } catch (Exception e) { /* ignore */ }
    }

    // ── Background Music ──────────────────────────────────────────────────────

    public void playGameMusic() {
        if (!soundEnabled || musicPlaying) return;
        musicPlaying = true;
        musicBeat = 0;
        scheduleNextBeat();
    }

    public void stopGameMusic() {
        musicPlaying = false;
        if (musicHandler != null && musicRunnable != null) {
            musicHandler.removeCallbacks(musicRunnable);
        }
    }

    public void playMenuMusic() {
        // No menu music to keep it lightweight
    }

    public void stopMenuMusic() {
        stopGameMusic();
    }

    private void scheduleNextBeat() {
        if (!musicPlaying) return;
        musicRunnable = () -> {
            if (!musicPlaying || !soundEnabled || toneGen == null) return;
            try {
                toneGen.startTone(MUSIC_TONES[musicBeat % MUSIC_TONES.length], 90);
            } catch (Exception e) { /* ignore */ }
            musicBeat++;
            musicHandler.postDelayed(this::scheduleNextBeat, 380);
        };
        musicHandler.postDelayed(musicRunnable, 380);
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    public void toggleSound() {
        soundEnabled = !soundEnabled;
        SharedPreferences.Editor editor = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KEY_SOUND, soundEnabled);
        editor.apply();

        if (!soundEnabled) {
            stopGameMusic();
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public void release() {
        stopGameMusic();
        if (toneGen != null) {
            try { toneGen.release(); } catch (Exception e) { /* ignore */ }
            toneGen = null;
        }
        instance = null;
    }
}

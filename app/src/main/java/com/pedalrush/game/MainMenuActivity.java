package com.pedalrush.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainMenuActivity - Shows Play, Sound toggle, and Exit buttons.
 * Also displays the all-time best score.
 */
public class MainMenuActivity extends AppCompatActivity {

    private SoundManager soundManager;
    private TextView bestScoreText;
    private ImageButton soundButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main_menu);

        soundManager = SoundManager.getInstance(this);

        bestScoreText = findViewById(R.id.best_score_text);
        soundButton = findViewById(R.id.btn_sound);

        updateBestScore();
        updateSoundButton();

        // Play button
        findViewById(R.id.btn_play).setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, GameActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Sound toggle button
        soundButton.setOnClickListener(v -> {
            soundManager.toggleSound();
            updateSoundButton();
        });

        // Exit button
        findViewById(R.id.btn_exit).setOnClickListener(v -> {
            soundManager.playButtonClick();
            finishAffinity();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBestScore();
        soundManager.playMenuMusic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        soundManager.stopMenuMusic();
    }

    private void updateBestScore() {
        int best = Utils.getBestScore(this);
        bestScoreText.setText(best > 0 ? "Best: " + best : "");
    }

    private void updateSoundButton() {
        if (soundManager.isSoundEnabled()) {
            soundButton.setImageResource(R.drawable.ic_sound_on);
            soundButton.setContentDescription("Sound On");
        } else {
            soundButton.setImageResource(R.drawable.ic_sound_off);
            soundButton.setContentDescription("Sound Off");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Utils.setImmersiveMode(getWindow().getDecorView());
        }
    }
}

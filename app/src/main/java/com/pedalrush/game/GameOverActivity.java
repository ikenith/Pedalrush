package com.pedalrush.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * GameOverActivity - Displays final score, best score, and navigation buttons.
 */
public class GameOverActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "extra_score";
    public static final String EXTRA_COINS = "extra_coins";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_game_over);

        SoundManager soundManager = SoundManager.getInstance(this);

        int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        int coins = getIntent().getIntExtra(EXTRA_COINS, 0);
        int best = Utils.getBestScore(this);

        // Save best score if this run beats it
        if (score > best) {
            Utils.saveBestScore(this, score);
            best = score;
        }

        TextView scoreText = findViewById(R.id.gameover_score);
        TextView bestText = findViewById(R.id.gameover_best);
        TextView coinsText = findViewById(R.id.gameover_coins);
        TextView newBestLabel = findViewById(R.id.gameover_newbest);

        scoreText.setText(String.valueOf(score));
        bestText.setText(String.valueOf(best));
        coinsText.setText(String.valueOf(coins));

        // Show "NEW BEST!" label if applicable
        if (score >= best && score > 0) {
            newBestLabel.setVisibility(android.view.View.VISIBLE);
        } else {
            newBestLabel.setVisibility(android.view.View.INVISIBLE);
        }

        // Play crash sound
        soundManager.playCrash();

        // Restart button
        findViewById(R.id.btn_restart).setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, GameActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        // Main menu button
        findViewById(R.id.btn_main_menu).setOnClickListener(v -> {
            soundManager.playButtonClick();
            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Utils.setImmersiveMode(getWindow().getDecorView());
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}

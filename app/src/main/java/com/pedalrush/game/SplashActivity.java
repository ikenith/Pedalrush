package com.pedalrush.game;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity - Entry point. Shows logo for ~2 seconds then navigates to MainMenu.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2200; // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen immersive
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        // Fade in the title
        TextView titleView = findViewById(R.id.splash_title);
        TextView subtitleView = findViewById(R.id.splash_subtitle);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setFillAfter(true);
        titleView.startAnimation(fadeIn);

        AlphaAnimation fadeInSub = new AlphaAnimation(0f, 1f);
        fadeInSub.setDuration(800);
        fadeInSub.setStartOffset(400);
        fadeInSub.setFillAfter(true);
        subtitleView.startAnimation(fadeInSub);

        // Navigate to main menu after splash duration
        new Handler(getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainMenuActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            Utils.setImmersiveMode(getWindow().getDecorView());
        }
    }
}

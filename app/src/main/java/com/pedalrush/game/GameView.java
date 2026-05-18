package com.pedalrush.game;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * GameView - Main game surface. Handles rendering, touch input, and game state.
 * Uses a dedicated game thread for the loop.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    // ── Game States ──────────────────────────────────────────────────────────
    private static final int STATE_COUNTDOWN = 0;
    private static final int STATE_PLAYING   = 1;
    private static final int STATE_DEAD      = 2;

    // ── Target frame rate ────────────────────────────────────────────────────
    private static final int TARGET_FPS      = 60;

    // ── Spawn timing (ms) ────────────────────────────────────────────────────
    private static final long OBSTACLE_SPAWN_INTERVAL_MIN = 1800;
    private static final long OBSTACLE_SPAWN_INTERVAL_MAX = 3200;
    private static final long COIN_SPAWN_INTERVAL          = 900;

    // ── Difficulty ramp ──────────────────────────────────────────────────────
    private static final float BASE_SPEED     = 8f;   // dp/frame equiv
    private static final float MAX_SPEED      = 22f;
    private static final float SPEED_INCREMENT = 0.0012f; // per frame

    // ── Fields ───────────────────────────────────────────────────────────────
    private final Context   context;
    private GameLoop         gameLoop;
    private final SoundManager soundManager;

    // Screen dimensions (set once surface is created)
    private int screenW, screenH;
    private float density;

    // Game state
    private int  gameState  = STATE_COUNTDOWN;
    private int  score      = 0;
    private int  coinsCount = 0;
    private float speed;

    // Countdown
    private int    countdownValue   = 3;
    private long   countdownTimer   = 0;
    private static final long COUNTDOWN_INTERVAL = 900; // ms

    // Entity lists
    private Player              player;
    private Background          background;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Coin>     coins     = new ArrayList<>();

    // Spawn timers
    private long lastObstacleTime = 0;
    private long nextObstacleInterval;
    private long lastCoinTime     = 0;

    // Touch state
    private boolean isPressed = false;

    // Random generator (reused, never recreated inside loop)
    private final Random random = new Random();

    // Paints (reused)
    private Paint hudPaint;
    private Paint hudShadowPaint;
    private Paint countdownPaint;
    private Paint overlayPaint;

    // Death animation
    private float deathTimer = 0;

    // Score tick accumulator
    private float scoreAccumulator = 0;

    public GameView(Context context) {
        super(context);
        this.context = context;
        this.soundManager = SoundManager.getInstance(context);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    // ── SurfaceHolder.Callback ───────────────────────────────────────────────

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        density  = getResources().getDisplayMetrics().density;
        screenW  = getWidth();
        screenH  = getHeight();

        initGame();

        gameLoop = new GameLoop(this, holder);
        gameLoop.setRunning(true);
        gameLoop.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenW = width;
        screenH = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopLoop();
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    private void initGame() {
        speed     = dp(BASE_SPEED);
        score     = 0;
        coinsCount = 0;
        gameState  = STATE_COUNTDOWN;
        countdownValue = 3;
        countdownTimer = System.currentTimeMillis();
        deathTimer = 0;
        scoreAccumulator = 0;

        obstacles.clear();
        coins.clear();

        background = new Background(screenW, screenH, density);
        player     = new Player(screenW, screenH, density);

        nextObstacleInterval = randBetween(OBSTACLE_SPAWN_INTERVAL_MIN, OBSTACLE_SPAWN_INTERVAL_MAX);
        lastObstacleTime = System.currentTimeMillis();
        lastCoinTime     = System.currentTimeMillis();

        // Paints
        hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudPaint.setColor(Color.WHITE);
        hudPaint.setTextSize(dp(22));
        hudPaint.setFakeBoldText(true);

        hudShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudShadowPaint.setColor(Color.parseColor("#66000000"));
        hudShadowPaint.setTextSize(dp(22));
        hudShadowPaint.setFakeBoldText(true);

        countdownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        countdownPaint.setColor(Color.WHITE);
        countdownPaint.setTextSize(dp(80));
        countdownPaint.setFakeBoldText(true);
        countdownPaint.setTextAlign(Paint.Align.CENTER);

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.parseColor("#AA000000"));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Called by the game loop every frame. Advances game logic.
     */
    public void update() {
        long now = System.currentTimeMillis();

        switch (gameState) {
            case STATE_COUNTDOWN:
                updateCountdown(now);
                background.update(speed * 0.3f);
                break;

            case STATE_PLAYING:
                updatePlaying(now);
                break;

            case STATE_DEAD:
                updateDeath();
                break;
        }
    }

    private void updateCountdown(long now) {
        if (now - countdownTimer >= COUNTDOWN_INTERVAL) {
            countdownValue--;
            countdownTimer = now;
            if (countdownValue <= 0) {
                gameState = STATE_PLAYING;
                soundManager.playGameMusic();
            }
        }
    }

    private void updatePlaying(long now) {
        // Increase speed over time
        speed = Math.min(dp(MAX_SPEED), speed + dp(SPEED_INCREMENT));

        // Scroll background
        background.update(speed);

        // Update player (jump physics)
        player.update(isPressed);

        // Accumulate score (distance-based)
        scoreAccumulator += speed * 0.015f;
        if (scoreAccumulator >= 1f) {
            score += (int) scoreAccumulator;
            scoreAccumulator -= (int) scoreAccumulator;
        }

        // Spawn obstacles
        if (now - lastObstacleTime >= nextObstacleInterval) {
            obstacles.add(new Obstacle(screenW, screenH, density, random));
            lastObstacleTime = now;
            nextObstacleInterval = randBetween(
                Math.max(800, OBSTACLE_SPAWN_INTERVAL_MIN - (long)(score * 0.4f)),
                Math.max(1200, OBSTACLE_SPAWN_INTERVAL_MAX - (long)(score * 0.5f))
            );
        }

        // Spawn coins
        if (now - lastCoinTime >= COIN_SPAWN_INTERVAL) {
            coins.add(new Coin(screenW, screenH, density, random));
            lastCoinTime = now;
        }

        // Update obstacles, check collision
        Iterator<Obstacle> obsIt = obstacles.iterator();
        while (obsIt.hasNext()) {
            Obstacle obs = obsIt.next();
            obs.update(speed);
            if (obs.isOffScreen()) {
                obsIt.remove();
                continue;
            }
            if (obs.collidesWith(player.getHitbox())) {
                triggerDeath();
                return;
            }
        }

        // Update coins, check collection
        Iterator<Coin> coinIt = coins.iterator();
        while (coinIt.hasNext()) {
            Coin coin = coinIt.next();
            coin.update(speed);
            if (coin.isOffScreen()) {
                coinIt.remove();
                continue;
            }
            if (coin.collidesWith(player.getHitbox())) {
                coinsCount++;
                score += 10;
                soundManager.playCoin();
                coinIt.remove();
            }
        }
    }

    private void updateDeath() {
        deathTimer += 0.05f;
        // Gradually slow scroll after death
        background.update(speed * Math.max(0f, 1f - deathTimer * 0.3f));
        player.updateCrash(deathTimer);

        // After death animation, launch GameOver screen
        if (deathTimer >= 1.8f) {
            launchGameOver();
        }
    }

    private void triggerDeath() {
        gameState  = STATE_DEAD;
        deathTimer = 0;
        soundManager.stopGameMusic();
        soundManager.playCrash();
    }

    private void launchGameOver() {
        if (gameLoop != null) {
            gameLoop.setRunning(false);
        }
        Intent intent = new Intent(context, GameOverActivity.class);
        intent.putExtra(GameOverActivity.EXTRA_SCORE, score);
        intent.putExtra(GameOverActivity.EXTRA_COINS, coinsCount);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        // Save best score
        int best = Utils.getBestScore(context);
        if (score > best) {
            Utils.saveBestScore(context, score);
        }

        // Finish the host activity
        if (context instanceof GameActivity) {
            ((GameActivity) context).finish();
        }
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    /**
     * Renders the current frame onto the given canvas.
     */
    public void draw(Canvas canvas) {
        if (canvas == null) return;

        // Background layer
        background.draw(canvas);

        // Game entities
        for (Coin coin : coins)         coin.draw(canvas);
        for (Obstacle obs : obstacles)  obs.draw(canvas);
        player.draw(canvas);

        // HUD overlay
        drawHUD(canvas);

        // Countdown overlay
        if (gameState == STATE_COUNTDOWN) {
            drawCountdown(canvas);
        }

        // Death flash
        if (gameState == STATE_DEAD && deathTimer < 0.3f) {
            overlayPaint.setAlpha((int)(255 * (0.3f - deathTimer) / 0.3f * 0.6f));
            canvas.drawRect(0, 0, screenW, screenH, overlayPaint);
        }
    }

    private void drawHUD(Canvas canvas) {
        // Score
        String scoreStr = "Score: " + score;
        float x = dp(16), y = dp(48);
        canvas.drawText(scoreStr, x + 2, y + 2, hudShadowPaint);
        canvas.drawText(scoreStr, x, y, hudPaint);

        // Coins
        String coinStr = "⬤ " + coinsCount;
        hudPaint.setColor(Color.parseColor("#FFD700"));
        hudShadowPaint.setColor(Color.parseColor("#88000000"));
        canvas.drawText(coinStr, x + 2, y + dp(28) + 2, hudShadowPaint);
        canvas.drawText(coinStr, x, y + dp(28), hudPaint);
        hudPaint.setColor(Color.WHITE);

        // Best score (top right)
        int best = Utils.getBestScore(context);
        if (best > 0) {
            String bestStr = "Best: " + best;
            hudPaint.setTextAlign(Paint.Align.RIGHT);
            hudShadowPaint.setTextAlign(Paint.Align.RIGHT);
            float rx = screenW - dp(16);
            canvas.drawText(bestStr, rx + 2, y + 2, hudShadowPaint);
            canvas.drawText(bestStr, rx, y, hudPaint);
            hudPaint.setTextAlign(Paint.Align.LEFT);
            hudShadowPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawCountdown(Canvas canvas) {
        // Semi-transparent dark overlay
        overlayPaint.setAlpha(120);
        canvas.drawRect(0, 0, screenW, screenH, overlayPaint);

        String text = countdownValue > 0 ? String.valueOf(countdownValue) : "GO!";
        countdownPaint.setColor(Color.WHITE);
        canvas.drawText(text, screenW / 2f, screenH / 2f + dp(30), countdownPaint);
    }

    // ── Touch Input ──────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                isPressed = true;
                if (gameState == STATE_PLAYING) {
                    player.jump();
                    soundManager.playJump();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    public void resume() {
        if (gameLoop != null && !gameLoop.isAlive()) {
            gameLoop.setRunning(true);
            gameLoop.start();
        }
    }

    public void pause() {
        soundManager.stopGameMusic();
        stopLoop();
    }

    private void stopLoop() {
        if (gameLoop != null) {
            gameLoop.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    gameLoop.join();
                    retry = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Convert dp to pixels using screen density. */
    float dp(float val) {
        return val * density;
    }

    private long randBetween(long min, long max) {
        return min + (long)(random.nextFloat() * (max - min));
    }
}

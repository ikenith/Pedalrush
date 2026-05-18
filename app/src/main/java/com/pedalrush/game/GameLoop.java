package com.pedalrush.game;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * GameLoop - Dedicated game thread that drives update/draw at a fixed target FPS.
 * Uses a sleep-based approach to cap frame rate while staying smooth.
 */
public class GameLoop extends Thread {

    private static final int    TARGET_FPS       = 60;
    private static final long   TARGET_FRAME_TIME = 1000L / TARGET_FPS; // ~16ms

    private final GameView      gameView;
    private final SurfaceHolder surfaceHolder;

    private volatile boolean running = false;

    public GameLoop(GameView gameView, SurfaceHolder surfaceHolder) {
        super("GameLoop");
        this.gameView      = gameView;
        this.surfaceHolder = surfaceHolder;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        long frameStart;
        long frameTime;

        while (running) {
            frameStart = System.currentTimeMillis();

            // 1. Update game logic
            gameView.update();

            // 2. Draw frame
            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    synchronized (surfaceHolder) {
                        gameView.draw(canvas);
                    }
                }
            } catch (Exception e) {
                // Surface may have been destroyed; exit gracefully
                running = false;
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        running = false;
                    }
                }
            }

            // 3. Sleep to maintain target FPS
            frameTime = System.currentTimeMillis() - frameStart;
            long sleepTime = TARGET_FRAME_TIME - frameTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}

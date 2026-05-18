package com.pedalrush.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Random;

/**
 * Coin - A spinning gold coin that the player collects for score and coins count.
 * Coin arc pattern: random height on the road lane or just above ground.
 */
public class Coin {

    private float x, y;
    private final float baseY;   // rest position
    private float floatOffset;   // gentle bob animation

    private float spinAngle = 0f;

    private final float radius;
    private final float density;

    // Squish factor for spin illusion (0 = edge-on, 1 = full circle)
    private float squishX = 1f;

    // Paints
    private final Paint coinPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coinDarkPaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Coin(int screenW, int screenH, float density, Random random) {
        this.density = density;
        this.radius  = dp(12);

        float roadTop    = screenH * 0.78f;
        float roadBottom = screenH * 0.97f;
        float groundMid  = roadTop + (roadBottom - roadTop) * 0.4f;

        // Position: random height—on road or mid-air
        float lane = random.nextFloat();
        if (lane < 0.5f) {
            // Road level
            baseY = groundMid - radius;
        } else if (lane < 0.8f) {
            // Low jump height
            baseY = roadTop - dp(40) - random.nextFloat() * dp(30);
        } else {
            // High jump height
            baseY = roadTop - dp(90) - random.nextFloat() * dp(40);
        }

        x = screenW + dp(20);
        y = baseY;
        floatOffset = random.nextFloat() * (float)(Math.PI * 2); // phase offset

        initPaints();
    }

    private void initPaints() {
        coinPaint.setColor(Color.parseColor("#FFD700")); // gold
        coinPaint.setStyle(Paint.Style.FILL);

        coinDarkPaint.setColor(Color.parseColor("#FF8F00")); // dark gold rim
        coinDarkPaint.setStyle(Paint.Style.FILL);

        shinePaint.setColor(Color.parseColor("#FFFDE7")); // shine highlight
        shinePaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(Color.parseColor("#44000000"));
        shadowPaint.setStyle(Paint.Style.FILL);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public void update(float speed) {
        x -= speed;

        // Bob gently
        floatOffset += 0.08f;
        y = baseY + (float)(Math.sin(floatOffset) * dp(3));

        // Spin (affects squishX 1 → 0 → -1 → 0 → 1)
        spinAngle += 4f;
        if (spinAngle > 360f) spinAngle -= 360f;
        squishX = Math.abs((float)(Math.cos(Math.toRadians(spinAngle))));
    }

    public boolean isOffScreen() {
        return x + radius < 0;
    }

    // ── Hitbox ───────────────────────────────────────────────────────────────

    public RectF getHitbox() {
        return new RectF(x - radius * 0.8f, y - radius * 0.8f,
                         x + radius * 0.8f, y + radius * 0.8f);
    }

    public boolean collidesWith(RectF playerBox) {
        return RectF.intersects(getHitbox(), playerBox);
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    public void draw(Canvas canvas) {
        // Shadow on ground
        float groundY = y + radius + dp(4);
        float shadowW = radius * squishX * 1.4f;
        if (shadowW > dp(2)) {
            canvas.drawOval(new RectF(
                x - shadowW, groundY,
                x + shadowW, groundY + dp(5)
            ), shadowPaint);
        }

        // Coin body (squished oval for spin effect)
        float rx = radius * squishX;
        float ry = radius;

        if (rx < dp(1)) return; // Edge-on, skip drawing

        // Dark rim
        canvas.drawOval(new RectF(x - rx - dp(2), y - ry - dp(2),
                                   x + rx + dp(2), y + ry + dp(2)), coinDarkPaint);

        // Gold face
        canvas.drawOval(new RectF(x - rx, y - ry, x + rx, y + ry), coinPaint);

        // Shine glint (top-left quadrant)
        if (rx > dp(4)) {
            canvas.drawOval(new RectF(
                x - rx * 0.5f, y - ry * 0.6f,
                x + rx * 0.1f, y + ry * 0.05f
            ), shinePaint);
        }

        // "₿" symbol in centre (just a thick vertical bar as decoration)
        if (rx > dp(6)) {
            Paint symbolPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            symbolPaint.setColor(Color.parseColor("#FF8F00"));
            symbolPaint.setStyle(Paint.Style.STROKE);
            symbolPaint.setStrokeWidth(dp(2));
            symbolPaint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(x, y - ry * 0.5f, x, y + ry * 0.5f, symbolPaint);
            canvas.drawLine(x - rx * 0.3f, y - ry * 0.15f, x + rx * 0.3f, y - ry * 0.15f, symbolPaint);
            canvas.drawLine(x - rx * 0.3f, y + ry * 0.15f, x + rx * 0.3f, y + ry * 0.15f, symbolPaint);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private float dp(float v) { return v * density; }
}

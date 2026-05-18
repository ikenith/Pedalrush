package com.pedalrush.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.Random;

/**
 * Obstacle - Spawns at the right edge, scrolls left, and kills the player on contact.
 * Four types: CONE, ROCK, BOX, POTHOLE.
 */
public class Obstacle {

    // Obstacle types
    private static final int TYPE_CONE    = 0;
    private static final int TYPE_ROCK    = 1;
    private static final int TYPE_BOX     = 2;
    private static final int TYPE_POTHOLE = 3;

    private float x, y;
    private final int type;

    // Dimensions (pixels)
    private final float w, h;

    private final float density;

    // Paints
    private final Paint mainPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Obstacle(int screenW, int screenH, float density, Random random) {
        this.density = density;

        type = random.nextInt(4);

        // Determine size by type
        switch (type) {
            case TYPE_CONE:    w = dp(22); h = dp(34); break;
            case TYPE_ROCK:    w = dp(32); h = dp(24); break;
            case TYPE_BOX:     w = dp(30); h = dp(30); break;
            case TYPE_POTHOLE: w = dp(46); h = dp(18); break;
            default:           w = dp(24); h = dp(28); break;
        }

        // Road surface top is at ~78% screen height
        float roadTop = screenH * 0.78f;
        float groundY = roadTop + (screenH * 0.97f - roadTop) * 0.5f;

        // Y: base of obstacle sits on ground
        y = groundY - h;
        x = screenW + dp(20);

        initPaints();
    }

    private void initPaints() {
        shadowPaint.setColor(Color.parseColor("#55000000"));
        shadowPaint.setStyle(Paint.Style.FILL);
        outlinePaint.setColor(Color.parseColor("#22000000"));
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dp(1.5f));

        switch (type) {
            case TYPE_CONE:
                mainPaint.setColor(Color.parseColor("#FF6F00"));  // orange cone
                accentPaint.setColor(Color.parseColor("#FFFFFF")); // white stripe
                break;
            case TYPE_ROCK:
                mainPaint.setColor(Color.parseColor("#78909C"));   // grey rock
                accentPaint.setColor(Color.parseColor("#90A4AE")); // highlight
                break;
            case TYPE_BOX:
                mainPaint.setColor(Color.parseColor("#8D6E63"));   // brown wood
                accentPaint.setColor(Color.parseColor("#6D4C41")); // dark grain
                break;
            case TYPE_POTHOLE:
                mainPaint.setColor(Color.parseColor("#212121"));   // asphalt dark
                accentPaint.setColor(Color.parseColor("#37474F")); // rim
                break;
        }
        mainPaint.setStyle(Paint.Style.FILL);
        accentPaint.setStyle(Paint.Style.FILL);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public void update(float speed) {
        x -= speed;
    }

    public boolean isOffScreen() {
        return x + w < 0;
    }

    // ── Hitbox ───────────────────────────────────────────────────────────────

    /** Slightly inset for forgiveness. */
    public RectF getHitbox() {
        float margin = dp(4);
        if (type == TYPE_POTHOLE) {
            // Pothole is flush on ground; only hitbox the top third
            return new RectF(x + margin, y + margin, x + w - margin, y + h * 0.5f);
        }
        return new RectF(x + margin, y + margin, x + w - margin, y + h - margin);
    }

    public boolean collidesWith(RectF playerBox) {
        return RectF.intersects(getHitbox(), playerBox);
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    public void draw(Canvas canvas) {
        // Shadow
        float shadowW = w * 1.2f;
        float shadowH = dp(6);
        canvas.drawOval(new RectF(
            x + w/2f - shadowW/2f, y + h,
            x + w/2f + shadowW/2f, y + h + shadowH
        ), shadowPaint);

        switch (type) {
            case TYPE_CONE:    drawCone(canvas);    break;
            case TYPE_ROCK:    drawRock(canvas);    break;
            case TYPE_BOX:     drawBox(canvas);     break;
            case TYPE_POTHOLE: drawPothole(canvas); break;
        }
    }

    private void drawCone(Canvas canvas) {
        // Orange triangle cone
        Path cone = new Path();
        cone.moveTo(x + w / 2f, y);               // tip
        cone.lineTo(x + w,      y + h);            // bottom-right
        cone.lineTo(x,          y + h);            // bottom-left
        cone.close();
        canvas.drawPath(cone, mainPaint);

        // White stripe across middle
        float sy = y + h * 0.55f;
        Path stripe = new Path();
        float stripeWidthFraction = 0.6f;
        float sw = w * stripeWidthFraction;
        float leftX  = x + (w - sw * ((h - sy + y) / h)) / 2f;
        float rightX = leftX + sw * ((h - (sy - y)) / h);
        stripe.moveTo(x + w * 0.2f, sy);
        stripe.lineTo(x + w * 0.8f, sy);
        stripe.lineTo(x + w * 0.7f, sy + dp(8));
        stripe.lineTo(x + w * 0.3f, sy + dp(8));
        stripe.close();
        canvas.drawPath(stripe, accentPaint);

        // Base plate
        RectF base = new RectF(x + w * 0.1f, y + h - dp(5), x + w * 0.9f, y + h);
        canvas.drawRect(base, accentPaint);
    }

    private void drawRock(Canvas canvas) {
        // Irregular rock shape
        Path rock = new Path();
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        rock.moveTo(cx - w * 0.5f, cy + h * 0.35f);
        rock.cubicTo(cx - w * 0.6f, cy - h * 0.1f, cx - w * 0.3f, cy - h * 0.5f, cx, cy - h * 0.5f);
        rock.cubicTo(cx + w * 0.35f, cy - h * 0.55f, cx + w * 0.55f, cy - h * 0.1f, cx + w * 0.5f, cy + h * 0.35f);
        rock.cubicTo(cx + w * 0.2f, cy + h * 0.5f, cx - w * 0.2f, cy + h * 0.5f, cx - w * 0.5f, cy + h * 0.35f);
        rock.close();
        canvas.drawPath(rock, mainPaint);

        // Highlight patch
        accentPaint.setAlpha(180);
        canvas.drawCircle(cx - w * 0.15f, cy - h * 0.2f, w * 0.18f, accentPaint);
        accentPaint.setAlpha(255);
    }

    private void drawBox(Canvas canvas) {
        RectF boxRect = new RectF(x, y, x + w, y + h);

        // Main face
        canvas.drawRect(boxRect, mainPaint);

        // Top face (lighter)
        Paint topFace = new Paint(Paint.ANTI_ALIAS_FLAG);
        topFace.setColor(Color.parseColor("#A1887F"));
        topFace.setStyle(Paint.Style.FILL);
        Path top = new Path();
        top.moveTo(x,         y);
        top.lineTo(x + dp(6), y - dp(8));
        top.lineTo(x + w + dp(6), y - dp(8));
        top.lineTo(x + w,    y);
        top.close();
        canvas.drawPath(top, topFace);

        // Side face (darker)
        Paint sideFace = new Paint(Paint.ANTI_ALIAS_FLAG);
        sideFace.setColor(Color.parseColor("#5D4037"));
        sideFace.setStyle(Paint.Style.FILL);
        Path side = new Path();
        side.moveTo(x + w,         y);
        side.lineTo(x + w + dp(6), y - dp(8));
        side.lineTo(x + w + dp(6), y + h - dp(8));
        side.lineTo(x + w,         y + h);
        side.close();
        canvas.drawPath(side, sideFace);

        // Cross planks
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(dp(2));
        canvas.drawLine(x, y, x + w, y + h, accentPaint);
        canvas.drawLine(x + w, y, x, y + h, accentPaint);
        accentPaint.setStyle(Paint.Style.FILL);
    }

    private void drawPothole(Canvas canvas) {
        // Dark oval sunken into road
        RectF hole = new RectF(x, y, x + w, y + h);
        canvas.drawOval(hole, mainPaint);

        // Inner shadow ring
        RectF inner = new RectF(x + dp(5), y + dp(3), x + w - dp(5), y + h - dp(3));
        canvas.drawOval(inner, accentPaint);

        // Dark centre
        Paint centre = new Paint(Paint.ANTI_ALIAS_FLAG);
        centre.setColor(Color.parseColor("#000000"));
        centre.setStyle(Paint.Style.FILL);
        canvas.drawOval(new RectF(x + dp(10), y + dp(5), x + w - dp(10), y + h - dp(5)), centre);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private float dp(float v) { return v * density; }
}

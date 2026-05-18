package com.pedalrush.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Player - The cyclist character.
 * Drawn entirely with Canvas shapes (no bitmaps needed).
 * Supports smooth jump/fall physics and a crash animation.
 */
public class Player {

    // ── Physics constants ────────────────────────────────────────────────────
    private static final float GRAVITY        = 1.5f;   // acceleration per frame (in raw pixels / density)
    private static final float JUMP_VELOCITY  = -18f;   // initial jump impulse
    private static final float HOLD_BOOST     = -0.6f;  // extra upward force while holding screen
    private static final float MAX_FALL_SPEED = 22f;

    // ── Sizing (dp) ──────────────────────────────────────────────────────────
    private static final float BODY_W   = 34f;
    private static final float BODY_H   = 40f;
    private static final float WHEEL_R  = 14f;
    private static final float HEAD_R   = 11f;

    // ── State ────────────────────────────────────────────────────────────────
    private float x, y;            // top-left of bounding box
    private float velY = 0;
    private boolean onGround = true;
    private boolean jumping  = false;

    // Wheel rotation angle
    private float wheelAngle = 0f;

    // Crash animation
    private float crashRotation = 0f;
    private float crashVelX     = 0f;

    // Ground Y position (where feet rest)
    private float groundY;

    // Screen dimensions
    private final int screenW, screenH;
    private final float density;

    // Body dimensions in pixels
    private final float bw, bh, wr, hr;

    // Paints (allocated once)
    private final Paint bodyPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wheelPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spokePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint helmetPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint framePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Player(int screenW, int screenH, float density) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.density  = density;

        bw = dp(BODY_W);
        bh = dp(BODY_H);
        wr = dp(WHEEL_R);
        hr = dp(HEAD_R);

        // Ground: just above the road surface (road top is at 78% screen height)
        groundY = screenH * 0.78f - wr * 2f;

        // Horizontal position: fixed at 18% from left
        x = screenW * 0.18f;
        y = groundY;

        initPaints();
    }

    private void initPaints() {
        bodyPaint.setColor(Color.parseColor("#E53935"));    // red jersey
        bodyPaint.setStyle(Paint.Style.FILL);

        wheelPaint.setColor(Color.parseColor("#212121"));
        wheelPaint.setStyle(Paint.Style.STROKE);
        wheelPaint.setStrokeWidth(dp(4));

        spokePaint.setColor(Color.parseColor("#616161"));
        spokePaint.setStyle(Paint.Style.STROKE);
        spokePaint.setStrokeWidth(dp(1.5f));

        headPaint.setColor(Color.parseColor("#FFCCBC"));   // skin tone
        headPaint.setStyle(Paint.Style.FILL);

        helmetPaint.setColor(Color.parseColor("#1565C0")); // blue helmet
        helmetPaint.setStyle(Paint.Style.FILL);

        framePaint.setColor(Color.parseColor("#F57F17"));  // orange bike frame
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(dp(3));
        framePaint.setStrokeCap(Paint.Cap.ROUND);

        shadowPaint.setColor(Color.parseColor("#44000000"));
        shadowPaint.setStyle(Paint.Style.FILL);
    }

    // ── Input / Physics ──────────────────────────────────────────────────────

    /** Called when player taps (initial jump). */
    public void jump() {
        if (onGround) {
            velY    = dp(JUMP_VELOCITY);
            onGround = false;
            jumping  = true;
        }
    }

    /** Main update called every frame. isHeld = finger still down. */
    public void update(boolean isHeld) {
        // Apply hold-boost (floaty jump) while airborne and finger held
        if (!onGround && isHeld && velY < 0) {
            velY += dp(HOLD_BOOST);
        }

        // Gravity
        velY += dp(GRAVITY);
        velY  = Math.min(velY, dp(MAX_FALL_SPEED));

        y += velY;

        // Land on ground
        if (y >= groundY) {
            y        = groundY;
            velY     = 0;
            onGround  = true;
            jumping   = false;
        }

        // Rotate wheels (faster when moving faster, but speed is handled by GameView)
        wheelAngle += 8f;
        if (wheelAngle > 360f) wheelAngle -= 360f;
    }

    /** Crash animation update; timer goes 0 → ~2. */
    public void updateCrash(float timer) {
        if (timer < 0.05f) {
            crashVelX = -dp(3f);
        }
        x       += crashVelX;
        crashVelX *= 0.92f;
        crashRotation += 12f;
        velY += dp(GRAVITY);
        y    = Math.min(y + velY, groundY + dp(30));
    }

    // ── Drawing ──────────────────────────────────────────────────────────────

    public void draw(Canvas canvas) {
        canvas.save();

        // Center of the cyclist (x,y is top of rear wheel)
        float cx = x + bw * 0.5f;
        float cy = y + wr;  // center of rear wheel

        // Apply crash rotation around center
        if (crashRotation != 0) {
            canvas.rotate(crashRotation, cx, cy);
        }

        // Gentle lean forward when airborne
        float lean = onGround ? 0f : -8f;
        canvas.rotate(lean, cx, cy);

        // Shadow
        float shadowY = groundY + wr + dp(4);
        float shadowScale = onGround ? 1f : Math.max(0.3f, 1f - (groundY - y) / (screenH * 0.3f));
        float shadowW = bw * 1.4f * shadowScale;
        canvas.drawOval(new RectF(cx - shadowW/2f, shadowY, cx + shadowW/2f, shadowY + dp(6)), shadowPaint);

        // ── Bike frame ──────────────────────────────────────────────────────
        // Wheel positions (rear and front)
        float rearWheelX  = cx - bw * 0.28f;
        float frontWheelX = cx + bw * 0.32f;
        float wheelCY     = cy;

        drawWheel(canvas, rearWheelX,  wheelCY);
        drawWheel(canvas, frontWheelX, wheelCY);

        // Frame: seat tube, top tube, down tube, chain stay
        // Seat stay (rear dropout to saddle)
        float saddleX = rearWheelX + dp(6);
        float saddleY = wheelCY - wr * 1.6f;
        framePaint.setColor(Color.parseColor("#F57F17"));

        canvas.drawLine(rearWheelX,  wheelCY, saddleX,      saddleY,         framePaint);  // seat tube
        canvas.drawLine(saddleX,     saddleY, frontWheelX - dp(2), wheelCY - wr * 0.9f, framePaint); // top tube
        canvas.drawLine(saddleX,     saddleY, frontWheelX - dp(4), wheelCY,             framePaint); // down tube
        canvas.drawLine(rearWheelX,  wheelCY, frontWheelX - dp(4), wheelCY,             framePaint); // chain stay

        // Handlebars
        float hbarX = frontWheelX - dp(4);
        float hbarBaseY = wheelCY - wr * 0.9f;
        framePaint.setColor(Color.parseColor("#795548"));
        canvas.drawLine(hbarX, hbarBaseY, hbarX, hbarBaseY - dp(10), framePaint);
        canvas.drawLine(hbarX - dp(7), hbarBaseY - dp(8), hbarX + dp(3), hbarBaseY - dp(8), framePaint);

        // Saddle
        framePaint.setColor(Color.parseColor("#37474F"));
        framePaint.setStrokeWidth(dp(5));
        canvas.drawLine(saddleX - dp(7), saddleY - dp(2), saddleX + dp(7), saddleY - dp(2), framePaint);
        framePaint.setStrokeWidth(dp(3));

        // ── Rider body ──────────────────────────────────────────────────────
        // Torso (leaning forward)
        float torsoBaseX = saddleX;
        float torsoBaseY = saddleY - dp(3);
        float torsoTopX  = hbarX - dp(3);
        float torsoTopY  = torsoBaseY - dp(18);

        bodyPaint.setColor(Color.parseColor("#E53935"));
        Path torsoPath = new Path();
        torsoPath.moveTo(torsoBaseX - dp(5), torsoBaseY);
        torsoPath.lineTo(torsoBaseX + dp(5), torsoBaseY);
        torsoPath.lineTo(torsoTopX + dp(5),  torsoTopY);
        torsoPath.lineTo(torsoTopX - dp(5),  torsoTopY);
        torsoPath.close();
        canvas.drawPath(torsoPath, bodyPaint);

        // Arms
        Paint armPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        armPaint.setColor(Color.parseColor("#E53935"));
        armPaint.setStyle(Paint.Style.STROKE);
        armPaint.setStrokeWidth(dp(6));
        armPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(torsoTopX, torsoTopY + dp(4), hbarX - dp(2), hbarBaseY - dp(9), armPaint);

        // Legs (crank animation)
        double crank = Math.toRadians(wheelAngle);
        float pedalR = dp(10f);
        float crankX = rearWheelX;
        float crankY = wheelCY;
        float knee1X = crankX + (float)(Math.cos(crank) * pedalR * 0.7f);
        float knee1Y = crankY + (float)(Math.sin(crank) * pedalR * 0.7f) - dp(5);
        float pedal1X = crankX + (float)(Math.cos(crank) * pedalR);
        float pedal1Y = crankY + (float)(Math.sin(crank) * pedalR);

        Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legPaint.setStyle(Paint.Style.STROKE);
        legPaint.setStrokeWidth(dp(6));
        legPaint.setStrokeCap(Paint.Cap.ROUND);
        legPaint.setColor(Color.parseColor("#1565C0")); // dark blue shorts
        canvas.drawLine(saddleX, saddleY + dp(2), knee1X, knee1Y, legPaint);
        legPaint.setColor(Color.parseColor("#FFCCBC")); // skin tone lower leg
        canvas.drawLine(knee1X, knee1Y, pedal1X, pedal1Y, legPaint);

        double crank2 = crank + Math.PI;
        float knee2X = crankX + (float)(Math.cos(crank2) * pedalR * 0.7f);
        float knee2Y = crankY + (float)(Math.sin(crank2) * pedalR * 0.7f) - dp(5);
        float pedal2X = crankX + (float)(Math.cos(crank2) * pedalR);
        float pedal2Y = crankY + (float)(Math.sin(crank2) * pedalR);

        legPaint.setColor(Color.parseColor("#1565C0"));
        canvas.drawLine(saddleX, saddleY + dp(2), knee2X, knee2Y, legPaint);
        legPaint.setColor(Color.parseColor("#FFCCBC"));
        canvas.drawLine(knee2X, knee2Y, pedal2X, pedal2Y, legPaint);

        // Shoes
        Paint shoePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shoePaint.setColor(Color.parseColor("#212121"));
        shoePaint.setStyle(Paint.Style.FILL);
        canvas.drawOval(new RectF(pedal1X - dp(6), pedal1Y - dp(3), pedal1X + dp(6), pedal1Y + dp(3)), shoePaint);
        canvas.drawOval(new RectF(pedal2X - dp(6), pedal2Y - dp(3), pedal2X + dp(6), pedal2Y + dp(3)), shoePaint);

        // Head
        float headX = torsoTopX;
        float headY = torsoTopY - hr;
        canvas.drawCircle(headX, headY, hr, headPaint);

        // Helmet
        RectF helmetRect = new RectF(headX - hr * 1.1f, headY - hr, headX + hr * 1.1f, headY + hr * 0.3f);
        canvas.drawArc(helmetRect, 180, 180, false, helmetPaint);
        // Helmet visor
        helmetPaint.setColor(Color.parseColor("#0D47A1"));
        canvas.drawRect(headX - hr * 1.1f, headY + hr * 0.1f, headX + hr * 1.1f, headY + hr * 0.4f, helmetPaint);
        helmetPaint.setColor(Color.parseColor("#1565C0"));

        canvas.restore();
    }

    private void drawWheel(Canvas canvas, float cx, float cy) {
        // Tire
        canvas.drawCircle(cx, cy, wr, wheelPaint);

        // Hub
        Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setColor(Color.parseColor("#9E9E9E"));
        hubPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, dp(3), hubPaint);

        // Spokes (4 spokes, rotated by wheelAngle)
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(wheelAngle + i * 45f);
            float sx = cx + (float)(Math.cos(angle) * wr);
            float sy = cy + (float)(Math.sin(angle) * wr);
            float ex = cx - (float)(Math.cos(angle) * wr);
            float ey = cy - (float)(Math.sin(angle) * wr);
            canvas.drawLine(sx, sy, ex, ey, spokePaint);
        }
    }

    // ── Hitbox ───────────────────────────────────────────────────────────────

    /** Slightly inset hitbox for forgiving collision. */
    public RectF getHitbox() {
        float margin = dp(6);
        return new RectF(
            x  + margin,
            y  + margin,
            x  + bw - margin,
            y  + bh + wr * 2f - margin
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private float dp(float v) { return v * density; }
}

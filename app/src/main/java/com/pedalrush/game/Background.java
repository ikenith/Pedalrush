package com.pedalrush.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Background - Renders a layered parallax background:
 *   Layer 0 (slowest): sky gradient
 *   Layer 1: distant mountains / hills
 *   Layer 2: mid-ground trees
 *   Layer 3: near ground / road
 *   Layer 4: road markings
 *   Layer 5: clouds
 */
public class Background {

    private final int screenW, screenH;
    private final float density;

    // ── Sky ──────────────────────────────────────────────────────────────────
    private final Paint skyPaint = new Paint();

    // ── Clouds ───────────────────────────────────────────────────────────────
    private final List<Cloud> clouds = new ArrayList<>();
    private final Paint cloudPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Hills ────────────────────────────────────────────────────────────────
    private float hillOffsetFar  = 0f; // distant hills
    private float hillOffsetNear = 0f; // near hills
    private final Paint hillFarPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hillNearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Trees ────────────────────────────────────────────────────────────────
    private final List<Tree> trees = new ArrayList<>();
    private final Paint treeTrunkPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint treeTopPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Ground / Road ────────────────────────────────────────────────────────
    private final Paint groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint roadPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint curbPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markingPaint= new Paint(Paint.ANTI_ALIAS_FLAG);

    // Road dashes
    private float dashOffset = 0f;
    private static final float DASH_W = 60f; // dp
    private static final float DASH_GAP = 40f;

    private final Random random = new Random(42);

    // ── Layout ───────────────────────────────────────────────────────────────
    private float skyBottom;     // where sky ends
    private float hillFarTop;    // top of far hills
    private float hillNearTop;   // top of near hills
    private float groundTop;     // grass/dirt strip
    private float roadTop;       // road surface top
    private float roadBottom;    // road surface bottom

    public Background(int screenW, int screenH, float density) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.density  = density;

        // Layout fractions of screen height
        skyBottom   = screenH * 0.60f;
        hillFarTop  = screenH * 0.52f;
        hillNearTop = screenH * 0.62f;
        groundTop   = screenH * 0.73f;
        roadTop     = screenH * 0.78f;
        roadBottom  = screenH * 0.97f;

        setupPaints();
        spawnInitialClouds();
        spawnInitialTrees();
    }

    private void setupPaints() {
        // Sky gradient (top = deep blue, bottom = light azure)
        LinearGradient skyGradient = new LinearGradient(
            0, 0, 0, skyBottom,
            new int[]{ Color.parseColor("#1565C0"), Color.parseColor("#42A5F5"), Color.parseColor("#90CAF9") },
            new float[]{ 0f, 0.6f, 1f },
            Shader.TileMode.CLAMP
        );
        skyPaint.setShader(skyGradient);

        cloudPaint.setColor(Color.parseColor("#FAFAFA"));
        cloudPaint.setStyle(Paint.Style.FILL);

        hillFarPaint.setColor(Color.parseColor("#7986CB"));   // purple-ish distant
        hillFarPaint.setStyle(Paint.Style.FILL);

        hillNearPaint.setColor(Color.parseColor("#66BB6A"));  // green near hills
        hillNearPaint.setStyle(Paint.Style.FILL);

        groundPaint.setColor(Color.parseColor("#558B2F"));    // grass
        groundPaint.setStyle(Paint.Style.FILL);

        roadPaint.setColor(Color.parseColor("#455A64"));      // dark asphalt
        roadPaint.setStyle(Paint.Style.FILL);

        curbPaint.setColor(Color.parseColor("#B0BEC5"));      // curb line
        curbPaint.setStyle(Paint.Style.FILL);

        markingPaint.setColor(Color.parseColor("#FFEE58"));   // yellow dash
        markingPaint.setStyle(Paint.Style.FILL);

        treeTrunkPaint.setColor(Color.parseColor("#5D4037"));
        treeTrunkPaint.setStyle(Paint.Style.FILL);

        treeTopPaint.setColor(Color.parseColor("#2E7D32"));
        treeTopPaint.setStyle(Paint.Style.FILL);
    }

    private void spawnInitialClouds() {
        for (int i = 0; i < 5; i++) {
            float cx = random.nextFloat() * screenW;
            float cy = dp(30) + random.nextFloat() * dp(80);
            float r  = dp(20) + random.nextFloat() * dp(30);
            clouds.add(new Cloud(cx, cy, r));
        }
    }

    private void spawnInitialTrees() {
        for (int i = 0; i < 8; i++) {
            float tx = i * screenW / 7f;
            float ty = hillNearTop + dp(5) + random.nextFloat() * dp(20);
            float th = dp(35) + random.nextFloat() * dp(25);
            trees.add(new Tree(tx, ty, th));
        }
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public void update(float speed) {
        // Parallax multipliers
        float cloudSpeed  = speed * 0.12f;
        float hillFarSpeed = speed * 0.20f;
        float hillNearSpeed= speed * 0.35f;
        float treeSpeed   = speed * 0.55f;

        hillOffsetFar  = (hillOffsetFar  + hillFarSpeed)  % (screenW * 2f);
        hillOffsetNear = (hillOffsetNear + hillNearSpeed) % (screenW * 2f);
        dashOffset     = (dashOffset     + speed)         % dp(DASH_W + DASH_GAP);

        // Clouds
        for (Cloud c : clouds) {
            c.x -= cloudSpeed;
            if (c.x + c.r < 0) {
                c.x = screenW + c.r;
                c.y = dp(30) + random.nextFloat() * dp(80);
                c.r = dp(20) + random.nextFloat() * dp(30);
            }
        }

        // Trees
        for (Tree t : trees) {
            t.x -= treeSpeed;
            if (t.x + t.h * 0.5f < 0) {
                t.x = screenW + dp(20);
                t.y = hillNearTop + dp(5) + random.nextFloat() * dp(20);
                t.h = dp(35) + random.nextFloat() * dp(25);
            }
        }
    }

    // ── Draw ─────────────────────────────────────────────────────────────────

    public void draw(Canvas canvas) {
        // 1. Sky
        canvas.drawRect(0, 0, screenW, screenH, skyPaint);

        // 2. Clouds
        for (Cloud c : clouds) {
            drawCloud(canvas, c.x, c.y, c.r);
        }

        // 3. Far hills (repeating sine wave path)
        drawHills(canvas, hillOffsetFar,  hillFarTop,  hillFarPaint,  dp(60), dp(120));

        // 4. Near hills
        drawHills(canvas, hillOffsetNear, hillNearTop, hillNearPaint, dp(40), dp(90));

        // 5. Trees
        for (Tree t : trees) {
            drawTree(canvas, t.x, t.y, t.h);
        }

        // 6. Grass strip
        canvas.drawRect(0, groundTop, screenW, roadTop, groundPaint);

        // 7. Road surface
        canvas.drawRect(0, roadTop, screenW, roadBottom, roadPaint);

        // 8. Curb line (top edge of road)
        canvas.drawRect(0, roadTop, screenW, roadTop + dp(5), curbPaint);

        // 9. Centre dashes
        float dashY     = roadTop + (roadBottom - roadTop) * 0.5f - dp(3);
        float dashH     = dp(6);
        float dashPxW   = dp(DASH_W);
        float dashPxGap = dp(DASH_GAP);
        float startX    = -dashOffset;
        while (startX < screenW) {
            canvas.drawRect(startX, dashY, startX + dashPxW, dashY + dashH, markingPaint);
            startX += dashPxW + dashPxGap;
        }
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float r) {
        canvas.drawCircle(cx,       cy,       r,        cloudPaint);
        canvas.drawCircle(cx + r * 0.8f, cy - r * 0.2f, r * 0.7f, cloudPaint);
        canvas.drawCircle(cx - r * 0.8f, cy + r * 0.1f, r * 0.65f, cloudPaint);
        canvas.drawCircle(cx + r * 0.4f, cy + r * 0.1f, r * 0.55f, cloudPaint);
    }

    private void drawHills(Canvas canvas, float offset, float topY, Paint paint,
                           float amplitude, float wavelength) {
        Path path = new Path();
        path.moveTo(0, screenH);

        int steps = (int)(screenW / 4f) + 2;
        for (int i = 0; i <= steps; i++) {
            float px = i * 4f;
            float phase = (px + offset) / wavelength * (float)(Math.PI * 2);
            float py = topY - (float)(Math.sin(phase) * amplitude);
            if (i == 0) path.moveTo(px, py);
            else        path.lineTo(px, py);
        }
        path.lineTo(screenW, screenH);
        path.lineTo(0, screenH);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawTree(Canvas canvas, float x, float baseY, float h) {
        float trunkW = dp(5);
        float trunkH = h * 0.35f;
        // Trunk
        canvas.drawRect(x - trunkW/2f, baseY - trunkH, x + trunkW/2f, baseY, treeTrunkPaint);
        // Foliage (triangle approximated with 3-circle cluster)
        float topR = h * 0.35f;
        canvas.drawCircle(x, baseY - trunkH - topR * 0.8f, topR, treeTopPaint);
        canvas.drawCircle(x - topR * 0.5f, baseY - trunkH - topR * 0.4f, topR * 0.7f, treeTopPaint);
        canvas.drawCircle(x + topR * 0.5f, baseY - trunkH - topR * 0.4f, topR * 0.7f, treeTopPaint);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private float dp(float v) { return v * density; }

    // ── Inner data classes ───────────────────────────────────────────────────

    private static class Cloud {
        float x, y, r;
        Cloud(float x, float y, float r) { this.x = x; this.y = y; this.r = r; }
    }

    private static class Tree {
        float x, y, h;
        Tree(float x, float y, float h) { this.x = x; this.y = y; this.h = h; }
    }
}

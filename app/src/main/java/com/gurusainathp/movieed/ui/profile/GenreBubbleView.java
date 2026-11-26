package com.gurusainathp.movieed.ui.profile;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.gurusainathp.movieed.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Bubble chart: one circle per genre. Circle AREA is proportional to the value,
 * so a 40% genre looks twice as big (by area) as a 20% one.
 * <p>
 * Usage:
 * bubbleView.setData(list of new GenreBubbleView.Bubble("Drama", 40));
 */
public class GenreBubbleView extends View {

    /**
     * Plain data holder.
     */
    public static final class Bubble {
        public final String label;
        public final float value;

        public Bubble(@NonNull String label, float value) {
            this.label = label;
            this.value = value;
        }
    }

    // ---- Tunables -------------------------------------------------------
    private static final int MAX_BUBBLES = 6;          // cap shown on the profile
    private static final float MIN_RADIUS_RATIO = 0.30f; // smallest bubble vs biggest (keeps tiny ones visible)
    private static final float GAP_UNITS = 4f;         // gap between bubbles (biggest radius = 100 units)
    private static final int ANGLE_STEPS = 72;         // placement search resolution
    private static final float STAGGER = 0.12f;        // delay between bubbles in the intro animation
    private static final long ANIM_MS = 800L;
    private static final float PADDING_DP = 8f;

    // ---- State ----------------------------------------------------------
    private static final class Placed {
        Bubble src;
        float ux, uy, ur;       // position/radius in "unit" space
        float px, py, pr;       // position/radius in pixels
        int color;
        int textColor;
        boolean showName;
        float nameSize;
        boolean showPct;
        float pctSize;
        String pctText;
    }

    private final List<Placed> placed = new ArrayList<>();
    private final List<Bubble> data = new ArrayList<>();
    private int[] palette;

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pctPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final OvershootInterpolator overshoot = new OvershootInterpolator(1.4f);
    private ValueAnimator animator;
    private float animProgress = 1f;

    @ColorInt
    private int darkText;
    @ColorInt
    private int lightText;

    // Intro animation plays once the view is fully in view and not clipped
    private final Rect visibleRect = new Rect();
    private boolean animationPending = false;
    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        if (animationPending && !data.isEmpty() && isFullyVisible()) {
            animationPending = false;
            startIntroAnimation();
        }
        return true;
    };

    public GenreBubbleView(Context context) {
        this(context, null);
    }

    public GenreBubbleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GenreBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        darkText = ContextCompat.getColor(context, R.color.movieed_background);
        lightText = ContextCompat.getColor(context, R.color.movieed_text_primary);

        // Palette tuned to the Movieed theme: muted, earthy tones.
        // Defined in colors.xml so you can tweak them without touching code.
        palette = new int[]{
                ContextCompat.getColor(context, R.color.movieed_primary),       // sage
                ContextCompat.getColor(context, R.color.movieed_accent),        // sand
                ContextCompat.getColor(context, R.color.movieed_bubble_clay),   // terracotta
                ContextCompat.getColor(context, R.color.movieed_bubble_slate),  // dusty blue
                ContextCompat.getColor(context, R.color.movieed_bubble_mauve),  // mauve
                ContextCompat.getColor(context, R.color.movieed_bubble_glass)   // sea glass
        };

        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pctPaint.setTextAlign(Paint.Align.CENTER);
        pctPaint.setTypeface(Typeface.DEFAULT_BOLD);

        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    // ---- Public API -----------------------------------------------------

    public void setPalette(@NonNull @ColorInt int[] colors) {
        if (colors.length == 0) return;
        this.palette = colors;
        computeLayout();
        invalidate();
    }

    public void setData(@Nullable List<Bubble> bubbles) {
        List<Bubble> next = new ArrayList<>();
        if (bubbles != null) {
            List<Bubble> sorted = new ArrayList<>(bubbles);
            Collections.sort(sorted, (a, b) -> Float.compare(b.value, a.value));
            for (Bubble b : sorted) {
                if (b.value <= 0f) continue;
                next.add(b);
                if (next.size() == MAX_BUBBLES) break;
            }
        }

        data.clear();
        data.addAll(next);
        updateContentDescription();
        computeLayout();

        if (!data.isEmpty()) {
            animationPending = true;
            animProgress = 0f;
            invalidate();
        } else {
            animationPending = false;
            animProgress = 1f;
            invalidate();
        }
    }

    // ---- Measure / layout ----------------------------------------------

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int desiredH = (int) dp(220);
        int w = MeasureSpec.getSize(widthSpec);
        int h = resolveSize(desiredH, heightSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeLayout();
    }

    /**
     * Greedy circle packing in unit space, then scale to fit the view.
     * 1) Biggest bubble goes in the middle.
     * 2) Each next bubble is pushed outward along 72 directions until it no longer
     * overlaps; the direction giving the most compact spot wins. Distance is
     * measured with the x-axis squeezed by the view's aspect ratio, so the
     * cluster spreads sideways on a wide card instead of growing tall.
     * 3) The whole cluster is scaled and centered inside the view.
     */
    private void computeLayout() {
        placed.clear();
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || data.isEmpty()) return;

        float maxValue = data.get(0).value;
        float aspect = (float) w / (float) h;

        for (int i = 0; i < data.size(); i++) {
            Bubble b = data.get(i);
            Placed p = new Placed();
            p.src = b;
            float rel = (float) Math.sqrt(b.value / maxValue);   // area ∝ value
            rel = Math.max(rel, MIN_RADIUS_RATIO);
            p.ur = rel * 100f;
            p.color = palette[i % palette.length];
            // Pick whichever text color has the higher contrast against this bubble
            p.textColor = ColorUtils.calculateContrast(darkText, p.color)
                    >= ColorUtils.calculateContrast(lightText, p.color) ? darkText : lightText;
            p.pctText = formatPct(b.value);

            if (i == 0) {
                p.ux = 0f;
                p.uy = 0f;
            } else {
                float bestScore = Float.MAX_VALUE;
                float bestX = 0f, bestY = 0f;
                for (int a = 0; a < ANGLE_STEPS; a++) {
                    double ang = (Math.PI * 2.0 * a) / ANGLE_STEPS;
                    float dx = (float) Math.cos(ang);
                    float dy = (float) Math.sin(ang);
                    float d = 0f;
                    float x, y;
                    while (true) {
                        x = dx * d;
                        y = dy * d;
                        if (!overlaps(x, y, p.ur)) break;
                        d += 2f;
                    }
                    float score = (float) Math.hypot(x / aspect, y);
                    if (score < bestScore) {
                        bestScore = score;
                        bestX = x;
                        bestY = y;
                    }
                }
                p.ux = bestX;
                p.uy = bestY;
            }
            placed.add(p);
        }

        // Bounding box in unit space
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Placed p : placed) {
            minX = Math.min(minX, p.ux - p.ur);
            maxX = Math.max(maxX, p.ux + p.ur);
            minY = Math.min(minY, p.uy - p.ur);
            maxY = Math.max(maxY, p.uy + p.ur);
        }
        float bw = maxX - minX;
        float bh = maxY - minY;
        float pad = dp(PADDING_DP);
        float scale = Math.min((w - 2 * pad) / bw, (h - 2 * pad) / bh);
        float offX = w / 2f - ((minX + maxX) / 2f) * scale;
        float offY = h / 2f - ((minY + maxY) / 2f) * scale;

        for (Placed p : placed) {
            p.px = offX + p.ux * scale;
            p.py = offY + p.uy * scale;
            p.pr = p.ur * scale;
            fitLabels(p);
        }
    }

    private boolean overlaps(float x, float y, float r) {
        for (Placed o : placed) {
            float dist = (float) Math.hypot(x - o.ux, y - o.uy);
            if (dist < r + o.ur + GAP_UNITS) return true;
        }
        return false;
    }

    /**
     * Decide which labels fit inside the circle and at what text size.
     */
    private void fitLabels(Placed p) {
        float availW = p.pr * 2f * 0.80f;
        float availH = p.pr * 2f * 0.70f;
        float minSize = sp(9);

        // Name + percentage stacked
        p.showName = false;
        p.showPct = false;

        float nameSize = sp(13);
        namePaint.setTextSize(nameSize);
        while (nameSize > minSize && namePaint.measureText(p.src.label) > availW) {
            nameSize -= sp(1);
            namePaint.setTextSize(nameSize);
        }
        boolean nameFits = namePaint.measureText(p.src.label) <= availW;

        float pctSize = nameFits ? sp(11) : sp(14);
        pctPaint.setTextSize(pctSize);
        while (pctSize > minSize && pctPaint.measureText(p.pctText) > availW) {
            pctSize -= sp(1);
            pctPaint.setTextSize(pctSize);
        }
        boolean pctFits = pctPaint.measureText(p.pctText) <= availW;

        if (nameFits && pctFits && (nameSize + pctSize + dp(2)) <= availH) {
            p.showName = true;
            p.nameSize = nameSize;
            p.showPct = true;
            p.pctSize = pctSize;
        } else if (pctFits) {
            // Not enough room for the name: percentage only, a bit larger
            float size = sp(14);
            pctPaint.setTextSize(size);
            while (size > minSize && pctPaint.measureText(p.pctText) > availW) {
                size -= sp(1);
                pctPaint.setTextSize(size);
            }
            p.showPct = pctPaint.measureText(p.pctText) <= availW;
            p.pctSize = size;
        }
    }

    // ---- Drawing --------------------------------------------------------

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int n = placed.size();
        for (int i = 0; i < n; i++) {
            Placed p = placed.get(i);

            float local = animProgress * (1f + STAGGER * (n - 1)) - i * STAGGER;
            local = Math.max(0f, Math.min(1f, local));
            float s = overshoot.getInterpolation(local);
            if (s <= 0f) continue;

            canvas.save();
            canvas.scale(s, s, p.px, p.py);

            circlePaint.setColor(p.color);
            canvas.drawCircle(p.px, p.py, p.pr, circlePaint);

            if (p.showName && p.showPct) {
                float gap = dp(2);
                float total = p.nameSize + gap + p.pctSize;
                float top = p.py - total / 2f;

                namePaint.setColor(p.textColor);
                namePaint.setTextSize(p.nameSize);
                canvas.drawText(p.src.label, p.px, top + p.nameSize * 0.82f, namePaint);

                pctPaint.setColor(p.textColor);
                pctPaint.setTextSize(p.pctSize);
                canvas.drawText(p.pctText, p.px, top + p.nameSize + gap + p.pctSize * 0.82f, pctPaint);
            } else if (p.showPct) {
                pctPaint.setColor(p.textColor);
                pctPaint.setTextSize(p.pctSize);
                canvas.drawText(p.pctText, p.px, p.py + p.pctSize * 0.35f, pctPaint);
            }

            canvas.restore();
        }
    }

    // ---- Animation ------------------------------------------------------

    private void startIntroAnimation() {
        if (animator != null) animator.cancel();
        animProgress = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIM_MS);
        animator.addUpdateListener(a -> {
            animProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    /**
     * True once the view is fully in view and not clipped.
     */
    private boolean isFullyVisible() {
        if (!isShown() || getHeight() == 0) return false;
        if (!getGlobalVisibleRect(visibleRect)) return false;
        return visibleRect.height() >= getHeight();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        invalidate(); // make sure a pre-draw pass happens so a pending animation can start
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
        if (animator != null) animator.cancel();
        // If we were detached mid-animation, show the final state next time instead of a blank card
        if (!animationPending && animProgress < 1f) animProgress = 1f;
        super.onDetachedFromWindow();
    }

    // ---- Helpers --------------------------------------------------------

    private void updateContentDescription() {
        if (data.isEmpty()) {
            setContentDescription(null);
            return;
        }
        StringBuilder sb = new StringBuilder("Top genres: ");
        for (int i = 0; i < data.size(); i++) {
            Bubble b = data.get(i);
            if (i > 0) sb.append(", ");
            sb.append(b.label).append(' ').append(formatPct(b.value).replace("%", " percent"));
        }
        setContentDescription(sb.toString());
    }

    private static String formatPct(float v) {
        return String.format(Locale.getDefault(), "%d%%", Math.round(v));
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }
}
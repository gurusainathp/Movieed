package com.gurusainathp.movieed.ui.person;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

/**
 * Blurs the static backdrop as the filmography scrolls into view, driven directly by the scroll position
 * (no timers), so it follows your finger both ways.
 * <p>
 * - Android 12+ : real blur (RenderEffect), radius grows from 0 to MAX_BLUR_DP
 * - older       : no cheap blur API, so only the dim layer gets stronger (still smooth)
 * <p>
 * Usage (in the activity, after setContentView):
 * new BackdropScrollEffect(
 * findViewById(R.id.scroll_person),
 * findViewById(R.id.section_filmography),
 * findViewById(R.id.iv_person_backdrop),
 * findViewById(R.id.view_backdrop_dim));
 */
public final class BackdropScrollEffect {

    // The effect starts when the filmography heading is this far down the screen (0 = top, 1 = bottom)...
    private static final float START_FRACTION = 0.90f;
    // ...and is complete when it has travelled up to here ("mostly in view").
    private static final float END_FRACTION = 0.35f;

    private static final float MAX_BLUR_DP = 22f;
    private static final float EXTRA_DIM = 0.12f;     // dim goes from its layout alpha to alpha + this

    private final NestedScrollView scroll;
    private final View section;
    private final View backdrop;
    private final View dim;

    private final float maxBlurPx;
    private final float dimStart;
    private final float dimEnd;
    private float lastRadius = -1f;

    public BackdropScrollEffect(@NonNull NestedScrollView scroll, @NonNull View section,
                                @NonNull View backdrop, @NonNull View dim) {
        this.scroll = scroll;
        this.section = section;
        this.backdrop = backdrop;
        this.dim = dim;

        this.maxBlurPx = MAX_BLUR_DP * scroll.getResources().getDisplayMetrics().density;
        this.dimStart = dim.getAlpha();                       // whatever you set in the layout
        this.dimEnd = Math.min(1f, dimStart + EXTRA_DIM);

        scroll.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, x, y, oldX, oldY) -> update());

        // The section appears (or changes size) after the data loads, so re-check then too
        section.addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, or, ob) -> update());
        scroll.post(this::update);
    }

    private void update() {
        apply(progress());
    }

    /**
     * 0 = filmography still far below, 1 = filmography mostly in view.
     */
    private float progress() {
        if (section.getVisibility() != View.VISIBLE || scroll.getHeight() == 0
                || scroll.getChildCount() == 0) {
            return 0f;
        }

        float viewport = scroll.getHeight();
        int sectionTop = section.getTop();                    // position inside the scrolled content

        float startScroll = sectionTop - viewport * START_FRACTION;
        float endScroll = sectionTop - viewport * END_FRACTION;

        // On a short page we may never be able to scroll as far as endScroll; don't wait for it
        View content = scroll.getChildAt(0);
        float maxScroll = content.getHeight() + scroll.getPaddingTop() + scroll.getPaddingBottom() - viewport;
        endScroll = Math.min(endScroll, maxScroll);

        float range = Math.max(1f, endScroll - startScroll);
        float p = (scroll.getScrollY() - startScroll) / range;
        p = Math.max(0f, Math.min(1f, p));

        return p * p * (3f - 2f * p);                         // smoothstep: eases in and out
    }

    private void apply(float p) {
        dim.setAlpha(dimStart + (dimEnd - dimStart) * p);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Half-pixel steps: smooth to the eye, but avoids rebuilding the effect every frame
            float radius = Math.round(maxBlurPx * p * 2f) / 2f;
            if (radius == lastRadius) return;
            lastRadius = radius;
            backdrop.setRenderEffect(radius < 0.5f
                    ? null
                    : RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
        }
    }
}
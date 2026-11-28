package com.gurusainathp.movieed.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.card.MaterialCardView;
import com.gurusainathp.movieed.R;

/**
 * UI-only controller for the Home search button.
 * <p>
 * Tap the circular button  ->  it morphs into a full-width search bar,
 * the content behind blurs (Android 12+) or dims (older), and the keyboard opens
 * WITHOUT pushing the bottom navigation bar up.
 * <p>
 * Usage (in HomeFragment.onViewCreated):
 * searchController = new HomeSearchController(this, view);
 * <p>
 * Expected ids in the layout:
 * scroll_home, card_home_search_button, view_search_dim_overlay,
 * card_home_search_overlay, et_home_search_query, btn_close_home_search
 */
public class HomeSearchController implements DefaultLifecycleObserver {

    // ---- Timing / look (tweak these) ------------------------------------
    private static final long EXPAND_MS = 520L;
    private static final long COLLAPSE_MS = 380L;
    private static final float CONTENT_FADE_START = 0.40f;  // hint + close icon fade in between 40%..80%
    private static final float CONTENT_FADE_END = 0.80f;
    private static final float BLUR_RADIUS_DP = 18f;
    private static final float SCRIM_ALPHA_WITH_BLUR = 0.55f;
    private static final float SCRIM_ALPHA_NO_BLUR = 1.0f;
    private static final float SIDE_MARGIN_DP = 20f;

    private final Fragment fragment;
    private final View root;
    private final View scroll;
    private final View button;
    private final View scrim;
    private final MaterialCardView overlay;
    private final EditText input;
    private final View closeButton;
    private final OnBackPressedCallback backCallback;

    private final PathInterpolator ease = new PathInterpolator(0.3f, 0f, 0.1f, 1f);

    private final int borderColor;
    private final int primaryColor;
    private final float blurRadiusPx;
    private final float scrimMaxAlpha;

    // Geometry captured when the search opens
    private int startW, endW, startTop, endTop, startEnd, endEnd, barHeight;

    /**
     * Lets another class (the results binder) react to the search UI without touching its animation code.
     */
    public interface Callbacks {
        void onQueryChanged(@NonNull String query);   // user typed (while open)

        void onSubmit(@NonNull String query);         // keyboard "search" key; keyboard is already hidden

        void onSearchExpanded();                      // bar finished expanding, keyboard is coming up

        void onSearchClosing();                       // close started (or forced), hide suggestions now
    }

    private Callbacks callbacks;

    private ValueAnimator animator;
    private float progress = 0f;      // 0 = button, 1 = full search bar
    private boolean open = false;     // target state
    private int previousSoftInputMode = -1;

    public HomeSearchController(@NonNull Fragment fragment, @NonNull View rootView) {
        this.fragment = fragment;
        this.root = rootView;
        this.scroll = rootView.findViewById(R.id.scroll_home);
        this.button = rootView.findViewById(R.id.card_home_search_button);
        this.scrim = rootView.findViewById(R.id.view_search_dim_overlay);
        this.overlay = rootView.findViewById(R.id.card_home_search_overlay);
        this.input = rootView.findViewById(R.id.et_home_search_query);
        this.closeButton = rootView.findViewById(R.id.btn_close_home_search);

        Context c = rootView.getContext();
        borderColor = ContextCompat.getColor(c, R.color.movieed_border);
        primaryColor = ContextCompat.getColor(c, R.color.movieed_primary);
        blurRadiusPx = BLUR_RADIUS_DP * c.getResources().getDisplayMetrics().density;
        scrimMaxAlpha = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? SCRIM_ALPHA_WITH_BLUR : SCRIM_ALPHA_NO_BLUR;

        // Initial hidden state
        overlay.setVisibility(View.GONE);
        scrim.setVisibility(View.GONE);
        scrim.setAlpha(0f);

        button.setOnClickListener(v -> open());
        closeButton.setOnClickListener(v -> close());
        scrim.setOnClickListener(v -> close());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                if (callbacks != null) callbacks.onSubmit(input.getText().toString());
                return true;
            }
            return false;
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Ignore the programmatic setText("") that happens after closing
                if (open && callbacks != null) callbacks.onQueryChanged(s.toString());
            }
        });

        // Back button closes the search first
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                close();
            }
        };
        fragment.requireActivity().getOnBackPressedDispatcher()
                .addCallback(fragment.getViewLifecycleOwner(), backCallback);

        fragment.getViewLifecycleOwner().getLifecycle().addObserver(this);
    }

    public boolean isOpen() {
        return open;
    }

    public void setCallbacks(@Nullable Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Y position (inside the screen root) of the bottom edge of the expanded search bar.
     */
    public int getBarBottomPx() {
        return endTop + barHeight;
    }

    // ---- Public actions -------------------------------------------------

    public void open() {
        if (open) return;
        open = true;
        backCallback.setEnabled(true);

        if (overlay.getVisibility() != View.VISIBLE) {
            computeGeometry();
            progress = 0f;
            applyProgress(0f);
            overlay.setVisibility(View.VISIBLE);
            scrim.setVisibility(View.VISIBLE);
            button.setVisibility(View.INVISIBLE);   // overlay sits exactly on top of it
        }

        // Bring the header back to the top so the bar lands where the title is
        if (scroll instanceof androidx.core.widget.NestedScrollView
                && scroll.getScrollY() > 0) {
            ((androidx.core.widget.NestedScrollView) scroll).smoothScrollTo(0, 0);
        }

        animateTo(1f);
    }

    public void close() {
        if (!open) return;
        open = false;
        backCallback.setEnabled(false);
        if (callbacks != null) callbacks.onSearchClosing();
        hideKeyboard();
        input.clearFocus();
        animateTo(0f);
    }

    // ---- Animation ------------------------------------------------------

    private void animateTo(float target) {
        if (animator != null) animator.cancel();

        boolean opening = target > progress;
        long duration = (long) ((opening ? EXPAND_MS : COLLAPSE_MS) * Math.abs(target - progress));

        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            applyProgress(progress);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelled) return;
                if (target >= 1f) onFullyOpen();
                else onFullyClosed();
            }
        });
        animator.start();
    }

    /**
     * f is the raw 0..1 progress; the easing is applied here so reversing mid-way never jumps.
     */
    private void applyProgress(float f) {
        float e = ease.getInterpolation(f);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) overlay.getLayoutParams();
        lp.width = lerp(startW, endW, e);
        lp.height = barHeight;
        lp.topMargin = lerp(startTop, endTop, e);
        lp.setMarginEnd(lerp(startEnd, endEnd, e));
        overlay.setLayoutParams(lp);

        overlay.setStrokeColor(ColorUtils.blendARGB(borderColor, primaryColor, e));

        float content = clamp01((f - CONTENT_FADE_START) / (CONTENT_FADE_END - CONTENT_FADE_START));
        input.setAlpha(content);
        closeButton.setAlpha(content);

        scrim.setAlpha(e * scrimMaxAlpha);
        applyBlur(e);
    }

    private void applyBlur(float e) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        float radius = blurRadiusPx * e;
        scroll.setRenderEffect(radius < 0.5f
                ? null
                : RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
    }

    private void onFullyOpen() {
        if (callbacks != null) callbacks.onSearchExpanded();
        input.requestFocus();
        input.post(() -> {
            InputMethodManager imm = (InputMethodManager)
                    input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void onFullyClosed() {
        overlay.setVisibility(View.GONE);
        scrim.setVisibility(View.GONE);
        button.setVisibility(View.VISIBLE);
        input.setText("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) scroll.setRenderEffect(null);
    }

    private void closeImmediately() {
        if (animator != null) animator.cancel();
        open = false;
        progress = 0f;
        backCallback.setEnabled(false);
        if (callbacks != null) callbacks.onSearchClosing();
        hideKeyboard();
        input.clearFocus();
        onFullyClosed();
    }

    // ---- Geometry -------------------------------------------------------

    private void computeGeometry() {
        int[] r = new int[2];
        int[] b = new int[2];
        root.getLocationInWindow(r);
        button.getLocationInWindow(b);

        int btnLeft = b[0] - r[0];
        int btnTop = b[1] - r[1];

        barHeight = button.getHeight();
        startW = button.getWidth();
        startTop = btnTop;
        startEnd = root.getWidth() - (btnLeft + button.getWidth());

        endEnd = dp(SIDE_MARGIN_DP);
        endW = root.getWidth() - 2 * endEnd;
        // Where the button would be if the page were scrolled to the top
        endTop = btnTop + scroll.getScrollY();

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) overlay.getLayoutParams();
        lp.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        overlay.setLayoutParams(lp);
    }

    // ---- Keyboard / window ---------------------------------------------

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    /**
     * While Home is visible the window does not resize or pan for the keyboard,
     * so the bottom navigation stays put (the keyboard simply covers it).
     * The previous mode is restored when leaving Home, so other screens
     * (e.g. a notes field in the log screen) keep their own behavior.
     */
    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        Window w = fragment.requireActivity().getWindow();
        previousSoftInputMode = w.getAttributes().softInputMode;
        int cleared = previousSoftInputMode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
        w.setSoftInputMode(cleared | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (previousSoftInputMode != -1) {
            fragment.requireActivity().getWindow().setSoftInputMode(previousSoftInputMode);
            previousSoftInputMode = -1;
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (open || overlay.getVisibility() == View.VISIBLE) closeImmediately();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if (animator != null) animator.cancel();
    }

    // ---- Helpers --------------------------------------------------------

    private int dp(float v) {
        return Math.round(v * root.getResources().getDisplayMetrics().density);
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
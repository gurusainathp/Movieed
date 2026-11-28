package com.gurusainathp.movieed.ui;

import android.view.Gravity;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.domain.model.Movie;
import com.gurusainathp.movieed.ui.search.SearchSuggestionAdapter;
import com.gurusainathp.movieed.ui.search.SearchViewModel;

import java.util.Collections;
import java.util.List;

/**
 * Connects three things for the Home search:
 * HomeSearchController (the animated bar)  ->  SearchViewModel (the data)  ->  suggestions panel (UI).
 * <p>
 * - typing is forwarded (debounced inside the ViewModel)
 * - the panel appears under the bar once there is something to say (loading / results / empty / error)
 * - the panel is kept above the keyboard, and grows when the keyboard is dismissed
 * - tapping a row calls the listener you pass in
 */
public class HomeSearchResultsBinder implements DefaultLifecycleObserver {

    public interface OnResultClickListener {
        void onResultClick(@NonNull Movie movie);
    }

    private static final int MAX_SUGGESTIONS = 8;
    private static final float SIDE_MARGIN_DP = 20f;
    private static final float GAP_BELOW_BAR_DP = 10f;
    private static final float KEYBOARD_FALLBACK_FRACTION = 0.38f;

    private final View root;
    private final HomeSearchController searchController;
    private final SearchViewModel viewModel;
    private final String apiKey;

    private final MaterialCardView panel;
    private final LinearProgressIndicator progress;
    private final View messageLayout;
    private final TextView tvMessageTitle;
    private final TextView tvMessageBody;
    private final TextView btnRetry;
    private final RecyclerView rvResults;
    private final EditText input;
    private final SearchSuggestionAdapter adapter;
    private final PathInterpolator ease = new PathInterpolator(0.2f, 0f, 0f, 1f);

    private boolean searchOpen = false;
    private boolean panelVisible = false;
    private boolean keyboardAssumedVisible = false;
    private SearchViewModel.State lastRenderedState = SearchViewModel.State.IDLE;

    public HomeSearchResultsBinder(@NonNull Fragment fragment,
                                   @NonNull View rootView,
                                   @NonNull HomeSearchController searchController,
                                   @NonNull SearchViewModel viewModel,
                                   @NonNull String apiKey,
                                   @NonNull OnResultClickListener clickListener) {
        this.root = rootView;
        this.searchController = searchController;
        this.viewModel = viewModel;
        this.apiKey = apiKey;

        panel = rootView.findViewById(R.id.card_search_results);
        progress = rootView.findViewById(R.id.progress_search);
        messageLayout = rootView.findViewById(R.id.layout_search_message);
        tvMessageTitle = rootView.findViewById(R.id.tv_search_message_title);
        tvMessageBody = rootView.findViewById(R.id.tv_search_message_body);
        btnRetry = rootView.findViewById(R.id.btn_search_retry);
        rvResults = rootView.findViewById(R.id.rv_search_results);
        input = rootView.findViewById(R.id.et_home_search_query);

        panel.setVisibility(View.GONE);

        adapter = new SearchSuggestionAdapter(clickListener::onResultClick);
        rvResults.setLayoutManager(new LinearLayoutManager(rootView.getContext()));
        rvResults.setAdapter(adapter);

        btnRetry.setOnClickListener(v -> viewModel.retry());

        // Keyboard bookkeeping: the window does not resize (adjustNothing), so we size the panel ourselves
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) setKeyboardAssumedVisible(true);
        });
        input.setOnClickListener(v -> setKeyboardAssumedVisible(true));
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            if (panelVisible) root.post(this::updatePanelLayout);
            return insets;   // not consumed
        });

        // Search bar -> ViewModel
        searchController.setCallbacks(new HomeSearchController.Callbacks() {
            @Override
            public void onQueryChanged(@NonNull String query) {
                viewModel.onQueryChanged(query, apiKey);
            }

            @Override
            public void onSubmit(@NonNull String query) {
                keyboardAssumedVisible = false;      // keyboard was hidden by the controller
                updatePanelLayout();                  // panel can use the freed space
                viewModel.searchMovies(query, apiKey);
            }

            @Override
            public void onSearchExpanded() {
                searchOpen = true;
                keyboardAssumedVisible = true;
                render(viewModel.getState().getValue());
            }

            @Override
            public void onSearchClosing() {
                searchOpen = false;
                hidePanel();
                viewModel.clear();
            }
        });

        // ViewModel -> panel
        LifecycleOwner owner = fragment.getViewLifecycleOwner();
        viewModel.getState().observe(owner, this::render);
        viewModel.getSearchResults().observe(owner, movies -> {
            List<Movie> limited = movies == null
                    ? Collections.<Movie>emptyList()
                    : movies.subList(0, Math.min(MAX_SUGGESTIONS, movies.size()));
            adapter.submitResults(limited, viewModel.getLastSearchedQuery());
        });
        viewModel.getIsLoading().observe(owner, loading ->
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.INVISIBLE));

        owner.getLifecycle().addObserver(this);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        ViewCompat.setOnApplyWindowInsetsListener(root, null);
        panel.animate().cancel();
        input.setOnFocusChangeListener(null);
        input.setOnClickListener(null);
        searchController.setCallbacks(null);
    }

    // ---- Rendering ----------------------------------------------------------

    private void render(@Nullable SearchViewModel.State state) {
        SearchViewModel.State s = state == null ? SearchViewModel.State.IDLE : state;

        if (!searchOpen || s == SearchViewModel.State.IDLE) {
            lastRenderedState = s;
            hidePanel();
            return;
        }

        showPanel();

        // Smoothly resize the card when switching between list / message / loading
        if (s != lastRenderedState) {
            AutoTransition t = new AutoTransition();
            t.setDuration(180);
            t.excludeTarget(rvResults, true);
            TransitionManager.beginDelayedTransition(panel, t);
        }
        lastRenderedState = s;

        switch (s) {
            case RESULTS:
                rvResults.setVisibility(View.VISIBLE);
                messageLayout.setVisibility(View.GONE);
                break;

            case LOADING:
                showMessage("Searching…", null, false);
                break;

            case EMPTY:
                showMessage("No matches for “" + currentQuery() + "”",
                        "Try a different title, actor or director.", false);
                break;

            case ERROR:
                String error = viewModel.getErrorMessage().getValue();
                showMessage("Couldn't search right now",
                        error != null ? error : "Please try again.", true);
                break;

            default:
                break;
        }
    }

    private void showMessage(@NonNull String title, @Nullable String body, boolean showRetry) {
        rvResults.setVisibility(View.GONE);
        messageLayout.setVisibility(View.VISIBLE);
        tvMessageTitle.setText(title);
        tvMessageBody.setText(body);
        tvMessageBody.setVisibility(body == null || body.isEmpty() ? View.GONE : View.VISIBLE);
        btnRetry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    private String currentQuery() {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    // ---- Panel show / hide ---------------------------------------------------

    private void showPanel() {
        if (panelVisible) return;
        panelVisible = true;
        updatePanelLayout();

        panel.animate().cancel();
        panel.setVisibility(View.VISIBLE);
        panel.setAlpha(0f);
        panel.setTranslationY(dp(10));
        panel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240)
                .setInterpolator(ease)
                .start();
    }

    private void hidePanel() {
        if (!panelVisible) return;
        panelVisible = false;

        panel.animate().cancel();
        panel.animate()
                .alpha(0f)
                .translationY(dp(6))
                .setDuration(140)
                .withEndAction(() -> {
                    if (!panelVisible) panel.setVisibility(View.GONE);
                })
                .start();
    }

    // ---- Layout: sit under the bar, stay above the keyboard --------------------

    private void setKeyboardAssumedVisible(boolean visible) {
        keyboardAssumedVisible = visible;
        updatePanelLayout();
    }

    private void updatePanelLayout() {
        if (!panelVisible) return;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) panel.getLayoutParams();
        int side = dp(SIDE_MARGIN_DP);
        lp.gravity = Gravity.TOP;
        lp.setMarginStart(side);
        lp.setMarginEnd(side);
        lp.topMargin = searchController.getBarBottomPx() + dp(GAP_BELOW_BAR_DP);
        lp.bottomMargin = keyboardOverlapPx() + dp(8);
        panel.setLayoutParams(lp);
    }

    /**
     * How far the keyboard reaches up into this screen (0 when hidden).
     */
    private int keyboardOverlapPx() {
        if (!keyboardAssumedVisible) return 0;

        int[] loc = new int[2];
        root.getLocationInWindow(loc);
        int rootBottom = loc[1] + root.getHeight();
        int windowHeight = root.getRootView().getHeight();

        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
        int ime = insets != null ? insets.getInsets(WindowInsetsCompat.Type.ime()).bottom : 0;
        if (ime <= 0) {
            // Some devices don't report the keyboard when the window isn't resizing; assume a typical height
            ime = Math.round(windowHeight * KEYBOARD_FALLBACK_FRACTION);
        }
        int keyboardTop = windowHeight - ime;
        return Math.max(0, rootBottom - keyboardTop);
    }

    private int dp(float v) {
        return Math.round(v * root.getResources().getDisplayMetrics().density);
    }
}
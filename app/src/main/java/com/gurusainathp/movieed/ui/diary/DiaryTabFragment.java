package com.gurusainathp.movieed.ui.diary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbCountryWatchProvidersDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbProviderDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbWatchProvidersResponseDto;
import com.gurusainathp.movieed.ui.movie.LocalProviderMapper;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiaryTabFragment extends Fragment {

    public static final int TAB_WATCHED = 0;
    public static final int TAB_WATCHLIST = 1;
    private static final String ARG_TAB_TYPE = "arg_tab_type";

    public static class ProviderInfo {
        public final int iconRes;
        public final String name;

        public ProviderInfo(int iconRes, String name) {
            this.iconRes = iconRes;
            this.name = name;
        }
    }

    private static final List<ProviderInfo> ALL_PROVIDERS = Arrays.asList(
            new ProviderInfo(R.drawable.ic_provider_netflix, "Netflix"),
            new ProviderInfo(R.drawable.ic_provider_prime_video, "Prime Video"),
            new ProviderInfo(R.drawable.jiohotstar, "JioHotstar"),
            new ProviderInfo(R.drawable.sonyliv, "SonyLIV"),
            new ProviderInfo(R.drawable.zee5, "ZEE5"),
            new ProviderInfo(R.drawable.ic_provider_apple_tv_plus, "Apple TV+"),
            new ProviderInfo(R.drawable.mx_player, "MX Player"),
            new ProviderInfo(R.drawable.ic_provider_youtube_premium, "YouTube Premium"),
            new ProviderInfo(R.drawable.ic_provider_crunchyroll, "Crunchyroll"),
            new ProviderInfo(R.drawable.ic_provider_lionsgate_plus, "Lionsgate+"),
            new ProviderInfo(R.drawable.ic_provider_paramount_plus, "Paramount+"),
            new ProviderInfo(R.drawable.ic_provider_plex, "Plex"),
            new ProviderInfo(R.drawable.ic_provider_tubi, "Tubi"),
            new ProviderInfo(R.drawable.mubi_logo, "MUBI")
    );

    private int tabType = TAB_WATCHED;
    private DiaryViewModel viewModel;
    private DiaryAdapter adapter;
    private RecyclerView rvGrid;
    private TextView tvEmpty;

    // Unified Filter Bar
    private NestedHorizontalScrollView scrollFilterBar;
    private LinearLayout layoutFilterButtons;
    private TextView btnFilterAll;

    private String selectedReactionFilter = "ALL";
    private int selectedProviderFilterRes = 0; // 0 means ALL
    private String selectedProviderName = "";

    private final Map<Long, Set<Integer>> movieProvidersMap = new HashMap<>();
    private final Map<View, String> reactionViewMap = new HashMap<>();
    private final Map<View, Integer> providerViewResMap = new HashMap<>();

    private List<MovieLogWithDetails> currentAllLogs = new ArrayList<>();

    public static DiaryTabFragment newInstance(int tabType) {
        DiaryTabFragment fragment = new DiaryTabFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_TYPE, tabType);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            tabType = getArguments().getInt(ARG_TAB_TYPE, TAB_WATCHED);
        }

        // Use requireActivity() or requireParentFragment() safely so it works when nested in ProfileFragment
        try {
            viewModel = new ViewModelProvider(requireParentFragment()).get(DiaryViewModel.class);
        } catch (Exception e) {
            viewModel = new ViewModelProvider(requireActivity()).get(DiaryViewModel.class);
        }

        rvGrid = view.findViewById(R.id.rv_tab_grid);
        tvEmpty = view.findViewById(R.id.tv_tab_empty);
        scrollFilterBar = view.findViewById(R.id.scroll_provider_filter_bar);
        layoutFilterButtons = view.findViewById(R.id.layout_provider_filters);
        btnFilterAll = view.findViewById(R.id.btn_filter_prov_all);

        if (tabType == TAB_WATCHED) {
            tvEmpty.setText("No watched films logged yet.");
        } else {
            tvEmpty.setText("Your watchlist is empty.");
        }

        btnFilterAll.setOnClickListener(v -> setFilter("ALL", 0, ""));
        buildUnifiedFilterBar();

        adapter = new DiaryAdapter(item -> {
            if (item != null && item.movie != null) {
                MovieDetailsActivity.launch(
                        requireContext(),
                        item.movie.getTmdbId(),
                        item.movie.getTitle(),
                        item.movie.getPosterPath()
                );
            }
        });
        rvGrid.setAdapter(adapter);

        observeViewModel();
    }

    private void setFilter(String reaction, int providerRes, String name) {
        selectedReactionFilter = reaction;
        selectedProviderFilterRes = providerRes;
        selectedProviderName = name;
        updateFilterUI();
        applyFilterAndRender();
    }

    private void updateFilterUI() {
        if (btnFilterAll != null) {
            boolean isAll = "ALL".equals(selectedReactionFilter) && selectedProviderFilterRes == 0;
            highlightButton(btnFilterAll, isAll);
        }

        for (Map.Entry<View, String> entry : reactionViewMap.entrySet()) {
            highlightButton(entry.getKey(), entry.getValue().equals(selectedReactionFilter));
        }

        for (Map.Entry<View, Integer> entry : providerViewResMap.entrySet()) {
            highlightButton(entry.getKey(), entry.getValue() == selectedProviderFilterRes);
        }
    }

    private void highlightButton(View btn, boolean isSelected) {
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_selected_filter);
        } else {
            btn.setBackgroundResource(R.drawable.bg_unselected_filter);
        }
    }

    private void observeViewModel() {
        viewModel.getDiaryLogs().observe(getViewLifecycleOwner(), logs -> {
            currentAllLogs = logs != null ? logs : new ArrayList<>();

            if (!currentAllLogs.isEmpty()) {
                fetchTabProviders();
            }

            applyFilterAndRender();
        });
    }

    private void fetchTabProviders() {
        for (MovieLogWithDetails item : currentAllLogs) {
            boolean isRelevant = (tabType == TAB_WATCHED && item.log.getReaction() != null && !item.log.getReaction().isEmpty())
                    || (tabType == TAB_WATCHLIST && item.log.isWatchlist());

            if (isRelevant && item.movie != null) {
                long movieId = item.movie.getTmdbId();
                if (!movieProvidersMap.containsKey(movieId)) {
                    movieProvidersMap.put(movieId, new HashSet<>());

                    Call<TmdbWatchProvidersResponseDto> call = TmdbClient.getApiService()
                            .getMovieWatchProviders((int) movieId, BuildConfig.TMDB_API_KEY);

                    call.enqueue(new Callback<TmdbWatchProvidersResponseDto>() {
                        @Override
                        public void onResponse(@NonNull Call<TmdbWatchProvidersResponseDto> call,
                                               @NonNull Response<TmdbWatchProvidersResponseDto> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                                TmdbCountryWatchProvidersDto in = response.body().getResults().get("IN");
                                if (in != null) {
                                    Set<Integer> icons = new HashSet<>();
                                    addProvidersToSet(in.getFlatrate(), icons);
                                    addProvidersToSet(in.getRent(), icons);
                                    addProvidersToSet(in.getBuy(), icons);
                                    movieProvidersMap.put(movieId, icons);
                                    applyFilterAndRender();
                                }
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<TmdbWatchProvidersResponseDto> call, @NonNull Throwable t) {}
                    });
                }
            }
        }
    }

    private void addProvidersToSet(List<TmdbProviderDto> list, Set<Integer> targetSet) {
        if (list == null) return;
        for (TmdbProviderDto p : list) {
            int iconRes = LocalProviderMapper.getLocalDrawableRes(p);
            if (iconRes != 0) {
                targetSet.add(iconRes);
            }
        }
    }

    private void buildUnifiedFilterBar() {
        if (layoutFilterButtons == null) return;

        layoutFilterButtons.removeAllViews();
        layoutFilterButtons.addView(btnFilterAll);

        reactionViewMap.clear();
        providerViewResMap.clear();

        // If Watched tab, add reaction filter buttons first
        if (tabType == TAB_WATCHED) {
            String[] emojis = {"😍", "🙂", "😐", "🙁", "😡"};
            String[] keys = {"LOVE", "LIKE", "UMM", "DISLIKE", "HATE"};

            for (int i = 0; i < emojis.length; i++) {
                String emoji = emojis[i];
                String key = keys[i];

                TextView btnEmoji = new TextView(requireContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
                lp.setMarginStart(dpToPx(4));
                lp.setMarginEnd(dpToPx(4));
                btnEmoji.setLayoutParams(lp);
                btnEmoji.setGravity(android.view.Gravity.CENTER);
                btnEmoji.setText(emoji);
                btnEmoji.setTextSize(22f);
                btnEmoji.setClickable(true);
                btnEmoji.setFocusable(true);

                boolean isSelected = key.equals(selectedReactionFilter) && selectedProviderFilterRes == 0;
                highlightButton(btnEmoji, isSelected);

                btnEmoji.setOnClickListener(v -> setFilter(key, 0, ""));
                reactionViewMap.put(btnEmoji, key);
                layoutFilterButtons.addView(btnEmoji);
            }
        }

        // Add provider filter buttons
        for (ProviderInfo info : ALL_PROVIDERS) {
            FrameLayout btnContainer = new FrameLayout(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
            lp.setMarginStart(dpToPx(4));
            lp.setMarginEnd(dpToPx(4));
            btnContainer.setLayoutParams(lp);
            btnContainer.setClickable(true);
            btnContainer.setFocusable(true);

            ImageView ivIcon = new ImageView(requireContext());
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            int padding = dpToPx(6);
            ivIcon.setPadding(padding, padding, padding, padding);
            ivIcon.setImageResource(info.iconRes);
            ivIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            btnContainer.addView(ivIcon, ivLp);

            boolean isSelected = (info.iconRes == selectedProviderFilterRes) && "ALL".equals(selectedReactionFilter);
            highlightButton(btnContainer, isSelected);

            btnContainer.setOnClickListener(v -> setFilter("ALL", info.iconRes, info.name));

            providerViewResMap.put(btnContainer, info.iconRes);
            layoutFilterButtons.addView(btnContainer);
        }
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void applyFilterAndRender() {
        List<MovieLogWithDetails> filteredList = new ArrayList<>();
        for (MovieLogWithDetails item : currentAllLogs) {
            if (tabType == TAB_WATCHED) {
                String r = item.log.getReaction();
                if (r != null && !r.isEmpty()) {
                    if (!"ALL".equals(selectedReactionFilter)) {
                        if (selectedReactionFilter.equals(r)) {
                            filteredList.add(item);
                        }
                    } else if (selectedProviderFilterRes != 0) {
                        Set<Integer> icons = item.movie != null ? movieProvidersMap.get((long) item.movie.getTmdbId()) : null;
                        if (icons != null && icons.contains(selectedProviderFilterRes)) {
                            filteredList.add(item);
                        }
                    } else {
                        filteredList.add(item);
                    }
                }
            } else {
                if (item.log.isWatchlist()) {
                    Set<Integer> icons = item.movie != null ? movieProvidersMap.get((long) item.movie.getTmdbId()) : null;
                    boolean matchesProvider = (selectedProviderFilterRes == 0) || (icons != null && icons.contains(selectedProviderFilterRes));

                    if (matchesProvider) {
                        filteredList.add(item);
                    }
                }
            }
        }

        adapter.setItems(filteredList);
        if (!filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.GONE);
            rvGrid.setVisibility(View.VISIBLE);
        } else {
            rvGrid.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);

            if (selectedProviderFilterRes != 0) {
                tvEmpty.setText("No movies available on " + selectedProviderName + ".");
            } else if (tabType == TAB_WATCHLIST) {
                tvEmpty.setText("Your watchlist is empty.");
            } else if (tabType == TAB_WATCHED && !"ALL".equals(selectedReactionFilter)) {
                tvEmpty.setText("No " + selectedReactionFilter.toLowerCase() + " movies found.");
            } else {
                tvEmpty.setText("No watched films logged yet.");
            }
        }
    }
}

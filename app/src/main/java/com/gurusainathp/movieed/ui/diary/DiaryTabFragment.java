package com.gurusainathp.movieed.ui.diary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
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
import java.util.Collections;
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

    private int tabType = TAB_WATCHED;
    private DiaryViewModel viewModel;
    private DiaryAdapter adapter;
    private RecyclerView rvGrid;
    private TextView tvEmpty;
    private DiaryFilterBar filterBar;

    private List<MovieLogWithDetails> currentAllLogs = new ArrayList<>();
    private final Map<Long, Set<Integer>> movieProvidersMap = new HashMap<>();

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

        try {
            viewModel = new ViewModelProvider(requireParentFragment()).get(DiaryViewModel.class);
        } catch (Exception e) {
            viewModel = new ViewModelProvider(requireActivity()).get(DiaryViewModel.class);
        }

        rvGrid = view.findViewById(R.id.rv_tab_grid);
        tvEmpty = view.findViewById(R.id.tv_tab_empty);

        if (tabType == TAB_WATCHED) {
            tvEmpty.setText("No watched films logged yet.");
        } else {
            tvEmpty.setText("Your watchlist is empty.");
        }

        adapter = new DiaryAdapter(item -> {
            if (item != null && item.movie != null) {
                MovieDetailsActivity.launch(
                        requireContext(),
                        item.movie.getTmdbId(),
                        item.movie.getTitle(),
                        item.movie.getPosterPath()
                );
            }
        }, tabType == TAB_WATCHLIST);

        rvGrid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvGrid.setAdapter(adapter);

        filterBar = new DiaryFilterBar(view, tabType == TAB_WATCHED, state -> applyFilterAndRender());

        observeViewModel();
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
                    Call<TmdbWatchProvidersResponseDto> call = TmdbClient.getApiService()
                            .getMovieWatchProviders((int) movieId, BuildConfig.TMDB_API_KEY);

                    call.enqueue(new Callback<TmdbWatchProvidersResponseDto>() {
                        @Override
                        public void onResponse(@NonNull Call<TmdbWatchProvidersResponseDto> call,
                                               @NonNull Response<TmdbWatchProvidersResponseDto> response) {
                            Set<Integer> providerIds = new HashSet<>();
                            if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                                TmdbCountryWatchProvidersDto in = response.body().getResults().get("IN");
                                if (in != null) {
                                    addProvidersToSet(in.getFlatrate(), providerIds);
                                    addProvidersToSet(in.getRent(), providerIds);
                                    addProvidersToSet(in.getBuy(), providerIds);
                                }
                            }
                            movieProvidersMap.put(movieId, providerIds);
                            applyFilterAndRender();
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
            if (p != null) {
                targetSet.add(p.getProviderId());
                int localRes = LocalProviderMapper.getLocalDrawableRes(p);
                if (localRes == R.drawable.ic_provider_netflix) targetSet.add(8);
                else if (localRes == R.drawable.ic_provider_prime_video) { targetSet.add(9); targetSet.add(119); }
                else if (localRes == R.drawable.jiohotstar) { targetSet.add(122); targetSet.add(337); }
                else if (localRes == R.drawable.sonyliv) targetSet.add(237);
                else if (localRes == R.drawable.zee5) targetSet.add(232);
                else if (localRes == R.drawable.ic_provider_apple_tv_plus) { targetSet.add(2); targetSet.add(350); }
                else if (localRes == R.drawable.mx_player) targetSet.add(134);
                else if (localRes == R.drawable.ic_provider_youtube_premium) targetSet.add(192);
                else if (localRes == R.drawable.ic_provider_crunchyroll) targetSet.add(283);
                else if (localRes == R.drawable.mubi_logo) targetSet.add(11);
                else if (localRes == R.drawable.ic_provider_lionsgate_plus) targetSet.add(569);
                else if (localRes == R.drawable.ic_provider_paramount_plus) targetSet.add(531);
                else if (localRes == R.drawable.ic_provider_plex) targetSet.add(538);
                else if (localRes == R.drawable.ic_provider_tubi) targetSet.add(73);
            }
        }
    }

    private void applyFilterAndRender() {
        DiaryFilterBar.State state = filterBar != null ? filterBar.getState() : DiaryFilterBar.State.defaults();

        List<MovieLogWithDetails> filteredList = new ArrayList<>();
        for (MovieLogWithDetails item : currentAllLogs) {
            if (item == null || item.log == null) continue;

            boolean isRelevant = (tabType == TAB_WATCHED && item.log.getReaction() != null && !item.log.getReaction().isEmpty())
                    || (tabType == TAB_WATCHLIST && item.log.isWatchlist());

            if (!isRelevant) continue;

            // Reaction filter
            if (state.reaction != null) {
                String r = item.log.getReaction();
                if (r == null || !r.equalsIgnoreCase(state.reaction)) continue;
            }

            // Provider filter
            if (state.providerId != DiaryFilterBar.State.ALL_PROVIDERS) {
                Set<Integer> providerIds = item.movie != null ? movieProvidersMap.get((long) item.movie.getTmdbId()) : null;
                if (providerIds == null || !providerIds.contains(state.providerId)) continue;
            }

            filteredList.add(item);
        }

        // Sort
        Collections.sort(filteredList, (a, b) -> {
            long timeA = a.log.getUpdatedAt() > 0 ? a.log.getUpdatedAt() : a.log.getCreatedAt();
            long timeB = b.log.getUpdatedAt() > 0 ? b.log.getUpdatedAt() : b.log.getCreatedAt();

            String dateA = a.movie != null && a.movie.getReleaseDate() != null ? a.movie.getReleaseDate() : "";
            String dateB = b.movie != null && b.movie.getReleaseDate() != null ? b.movie.getReleaseDate() : "";

            String titleA = a.movie != null && a.movie.getTitle() != null ? a.movie.getTitle() : "";
            String titleB = b.movie != null && b.movie.getTitle() != null ? b.movie.getTitle() : "";

            if (state.sort == DiaryFilterBar.Sort.RECENT) {
                return Long.compare(timeB, timeA);
            } else if (state.sort == DiaryFilterBar.Sort.NEWEST) {
                return dateB.compareTo(dateA);
            } else if (state.sort == DiaryFilterBar.Sort.OLDEST) {
                return dateA.compareTo(dateB);
            } else if (state.sort == DiaryFilterBar.Sort.TITLE_AZ) {
                return titleA.compareToIgnoreCase(titleB);
            } else if (state.sort == DiaryFilterBar.Sort.TITLE_ZA) {
                return titleB.compareToIgnoreCase(titleA);
            }
            return Long.compare(timeB, timeA);
        });

        adapter.setItems(filteredList);
        if (!filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.GONE);
            rvGrid.setVisibility(View.VISIBLE);
        } else {
            rvGrid.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            if (tabType == TAB_WATCHLIST) {
                tvEmpty.setText("Your watchlist is empty.");
            } else {
                tvEmpty.setText("No watched films matching filters.");
            }
        }
    }
}

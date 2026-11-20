package com.gurusainathp.movieed.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;
import com.gurusainathp.movieed.ui.movie.ReactionPickerBottomSheetDialogFragment;
import com.gurusainathp.movieed.ui.person.PersonDetailsActivity;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment implements ExploreAdapter.OnExploreActionListener {

    private ExploreViewModel viewModel;
    private ExploreAdapter adapter;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private List<MovieLogWithDetails> cachedUserLogs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.view_pager_explore);
        progressBar = view.findViewById(R.id.progress_explore);
        tvEmpty = view.findViewById(R.id.tv_explore_empty);

        // Preload next 2 pages in memory for buttery smooth swipe
        viewPager.setOffscreenPageLimit(2);

        adapter = new ExploreAdapter(this);
        viewPager.setAdapter(adapter);

        // Infinite reel pagination: fetch next page when near bottom (within 4 items)
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position >= adapter.getItemCount() - 4 && adapter.getItemCount() > 0) {
                    viewModel.loadNextPage(cachedUserLogs);
                }
            }
        });

        viewModel = new ViewModelProvider(this).get(ExploreViewModel.class);

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getExploreFeed().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                adapter.setItems(items);
                tvEmpty.setVisibility(View.GONE);
                viewPager.setVisibility(View.VISIBLE);
            } else if (!Boolean.TRUE.equals(viewModel.getIsLoading().getValue())) {
                tvEmpty.setVisibility(View.VISIBLE);
                viewPager.setVisibility(View.GONE);
            }
        });

        viewModel.getLogsMap().observe(getViewLifecycleOwner(), logsMap -> {
            if (logsMap != null) {
                adapter.setLogsMap(logsMap);
            }
        });

        viewModel.getAllLogsWithDetails().observe(getViewLifecycleOwner(), logs -> {
            cachedUserLogs = logs != null ? logs : new ArrayList<>();
            List<MovieLogEntity> rawLogs = new ArrayList<>();
            for (MovieLogWithDetails item : cachedUserLogs) {
                if (item.log != null) {
                    rawLogs.add(item.log);
                }
            }
            viewModel.updateLogsMap(rawLogs);

            if (viewModel.getExploreFeed().getValue() == null || viewModel.getExploreFeed().getValue().isEmpty()) {
                viewModel.loadExploreMovies(cachedUserLogs);
            }
        });
    }

    @Override
    public void onMovieClick(TmdbMovieDto movie) {
        if (movie == null) return;
        boolean isTv = "tv".equals(movie.getMediaType());
        MovieDetailsActivity.launch(
                requireContext(),
                movie.getId(),
                movie.getTitle(),
                movie.getPosterPath(),
                isTv
        );
    }

    @Override
    public void onReasonClick(RecommendedMovie item) {
        if (item == null) return;
        if (item.getSourceType() == RecommendedMovie.SourceType.PERSON && item.getSourceId() > 0) {
            PersonDetailsActivity.launch(requireContext(), item.getSourceId(), item.getSourceName());
        } else if (item.getSourceType() == RecommendedMovie.SourceType.MOVIE && item.getSourceId() > 0) {
            MovieDetailsActivity.launch(
                    requireContext(),
                    item.getSourceId(),
                    item.getSourceName(),
                    item.getReasonPosterPath(),
                    false
            );
        } else if (item.getMovie() != null) {
            boolean isTv = "tv".equals(item.getMovie().getMediaType());
            MovieDetailsActivity.launch(
                    requireContext(),
                    item.getMovie().getId(),
                    item.getMovie().getTitle(),
                    item.getMovie().getPosterPath(),
                    isTv
            );
        }
    }

    @Override
    public void onWatchlistClick(TmdbMovieDto movie, MovieLogEntity currentLog) {
        viewModel.toggleWatchlist(movie, currentLog);
    }

    @Override
    public void onRateClick(TmdbMovieDto movie, MovieLogEntity currentLog) {
        String currentReaction = currentLog != null ? currentLog.getReaction() : null;
        ReactionPickerBottomSheetDialogFragment dialog =
                ReactionPickerBottomSheetDialogFragment.newInstance(currentReaction);

        dialog.setOnReactionSelectedListener(new ReactionPickerBottomSheetDialogFragment.OnReactionSelectedListener() {
            @Override
            public void onReactionSelected(String reaction) {
                viewModel.saveReaction(movie, reaction, currentLog);
            }

            @Override
            public void onReactionCleared() {
                viewModel.saveReaction(movie, null, currentLog);
            }
        });
        dialog.show(getChildFragmentManager(), "ExploreReactionPicker");
    }
}

package com.gurusainathp.movieed.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.domain.model.Movie;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;

import java.util.Locale;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;

    // Adapters
    private HomeRecentLogAdapter recentLogAdapter;
    private HomeMovieAdapter recommendedAdapter;
    private HomeMovieAdapter classicsAdapter;
    private HomeMovieAdapter classicsSeriesAdapter;
    private HomeMovieAdapter trendingAdapter;
    private HomeMovieAdapter trendingSeriesAdapter;
    private HomeMovieAdapter nowPlayingAdapter;
    private HomeMovieAdapter streamingSeriesAdapter;

    // Views
    private ProgressBar pbLoading;
    private MaterialCardView cardError;
    private TextView tvError;
    private MaterialButton btnRetry;

    // Hero Banner views
    private View sectionFeatured;
    private MaterialCardView cardHeroBanner;
    private ImageView ivHeroBackdrop;
    private TextView tvHeroTitle;
    private TextView tvHeroYear;
    private TextView tvHeroRating;

    // Sections
    private View sectionRecentlyLogged;
    private TextView btnSeeAllDiary;

    private View sectionRecommended;
    private TextView btnExploreMore;

    private RecyclerView rvRecentlyLogged;
    private RecyclerView rvRecommendedMovies;
    private RecyclerView rvClassicsMovies;
    private RecyclerView rvClassicsSeries;
    private RecyclerView rvTrendingMovies;
    private RecyclerView rvTrendingSeries;
    private RecyclerView rvNowPlayingMovies;
    private RecyclerView rvStreamingSeries;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        initViews(view);
        setupRecyclerViews();
        observeViewModel();

        btnRetry.setOnClickListener(v -> loadData());
        btnSeeAllDiary.setOnClickListener(v -> navigateToTab(R.id.nav_diary));
        btnExploreMore.setOnClickListener(v -> navigateToTab(R.id.nav_explore));

        loadData();
    }

    private void initViews(View view) {
        pbLoading = view.findViewById(R.id.pb_home_loading);
        cardError = view.findViewById(R.id.card_home_error);
        tvError = view.findViewById(R.id.tv_home_error);
        btnRetry = view.findViewById(R.id.btn_home_retry);

        sectionFeatured = view.findViewById(R.id.section_featured);
        cardHeroBanner = view.findViewById(R.id.card_hero_banner);
        ivHeroBackdrop = view.findViewById(R.id.iv_hero_backdrop);
        tvHeroTitle = view.findViewById(R.id.tv_hero_title);
        tvHeroYear = view.findViewById(R.id.tv_hero_year);
        tvHeroRating = view.findViewById(R.id.tv_hero_rating);

        sectionRecentlyLogged = view.findViewById(R.id.section_recently_logged);
        btnSeeAllDiary = view.findViewById(R.id.btn_see_all_diary);

        sectionRecommended = view.findViewById(R.id.section_recommended);
        btnExploreMore = view.findViewById(R.id.btn_explore_more);

        rvRecentlyLogged = view.findViewById(R.id.rv_recently_logged);
        rvRecommendedMovies = view.findViewById(R.id.rv_recommended_home_movies);
        rvClassicsMovies = view.findViewById(R.id.rv_classics_movies);
        rvClassicsSeries = view.findViewById(R.id.rv_classics_series);
        rvTrendingMovies = view.findViewById(R.id.rv_trending_movies);
        rvTrendingSeries = view.findViewById(R.id.rv_trending_series);
        rvNowPlayingMovies = view.findViewById(R.id.rv_now_playing_movies);
        rvStreamingSeries = view.findViewById(R.id.rv_streaming_series);
    }

    private void setupRecyclerViews() {
        // User's recent logs
        recentLogAdapter = new HomeRecentLogAdapter(item -> {
            if (item != null && item.movie != null) {
                MovieDetailsActivity.launch(
                        requireContext(),
                        item.movie.getTmdbId(),
                        item.movie.getTitle(),
                        item.movie.getPosterPath()
                );
            }
        });
        rvRecentlyLogged.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecentlyLogged.setAdapter(recentLogAdapter);

        // You May Like These (Recommendations)
        recommendedAdapter = new HomeMovieAdapter(this::openMovieDetails);
        rvRecommendedMovies.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecommendedMovies.setAdapter(recommendedAdapter);

        // Hall of Fame Classics
        classicsAdapter = new HomeMovieAdapter(this::openMovieDetails);
        rvClassicsMovies.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvClassicsMovies.setAdapter(classicsAdapter);

        // Hall of Fame Classic Series
        classicsSeriesAdapter = new HomeMovieAdapter(this::openMovieDetails);
        rvClassicsSeries.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvClassicsSeries.setAdapter(classicsSeriesAdapter);

        // Trending Movies
        trendingAdapter = new HomeMovieAdapter(this::openMovieDetails);
        rvTrendingMovies.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTrendingMovies.setAdapter(trendingAdapter);

        // Trending Series
        trendingSeriesAdapter = new HomeMovieAdapter(this::openMovieDetails);
        rvTrendingSeries.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTrendingSeries.setAdapter(trendingSeriesAdapter);

        // Now Playing (In Theaters)
        nowPlayingAdapter = new HomeMovieAdapter(this::openMovieDetails);
        rvNowPlayingMovies.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvNowPlayingMovies.setAdapter(nowPlayingAdapter);

        // Streaming Series
        streamingSeriesAdapter = new HomeMovieAdapter(this::openMovieDetails);
        rvStreamingSeries.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvStreamingSeries.setAdapter(streamingSeriesAdapter);
    }

    private void openMovieDetails(Movie movie) {
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

    private void navigateToTab(int menuId) {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(menuId);
            }
        }
    }

    private void loadData() {
        viewModel.loadHomeContent(BuildConfig.TMDB_API_KEY);
    }

    private void observeViewModel() {
        // Loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            pbLoading.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });

        // Error state
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                cardError.setVisibility(View.VISIBLE);
                tvError.setText(error);
            } else {
                cardError.setVisibility(View.GONE);
            }
        });

        // User's logged movies from local Room DB
        viewModel.getUserLogs().observe(getViewLifecycleOwner(), logs -> {
            if (logs != null && !logs.isEmpty()) {
                sectionRecentlyLogged.setVisibility(View.VISIBLE);
                recentLogAdapter.setItems(logs);
            } else {
                sectionRecentlyLogged.setVisibility(View.GONE);
            }
        });

        // Featured Hero Movie
        viewModel.getFeaturedMovie().observe(getViewLifecycleOwner(), movie -> {
            if (movie != null) {
                sectionFeatured.setVisibility(View.VISIBLE);
                tvHeroTitle.setText(movie.getTitle());

                String releaseDate = movie.getReleaseDate();
                if (releaseDate != null && releaseDate.length() >= 4) {
                    tvHeroYear.setText(releaseDate.substring(0, 4));
                } else {
                    tvHeroYear.setText("");
                }

                if (movie.getVoteAverage() > 0) {
                    tvHeroRating.setVisibility(View.VISIBLE);
                    tvHeroRating.setText(String.format(Locale.US, "★ %.1f", movie.getVoteAverage()));
                } else {
                    tvHeroRating.setVisibility(View.GONE);
                }

                String imagePath = movie.getBackdropPath() != null ? movie.getBackdropPath() : movie.getPosterPath();
                if (imagePath != null) {
                    Glide.with(this)
                            .load("https://image.tmdb.org/t/p/w780" + imagePath)
                            .placeholder(R.color.movieed_surface_elevated)
                            .error(R.color.movieed_surface_elevated)
                            .into(ivHeroBackdrop);
                }

                cardHeroBanner.setOnClickListener(v -> openMovieDetails(movie));
            } else {
                sectionFeatured.setVisibility(View.GONE);
            }
        });

        // Recommended Movies ("YOU MAY LIKE THESE")
        viewModel.getRecommendedMovies().observe(getViewLifecycleOwner(), movies -> {
            if (movies != null && !movies.isEmpty()) {
                sectionRecommended.setVisibility(View.VISIBLE);
                recommendedAdapter.setMovies(movies);
            } else {
                sectionRecommended.setVisibility(View.GONE);
            }
        });

        // Hall of Fame Classics (Movies)
        viewModel.getClassicsMovies().observe(getViewLifecycleOwner(), movies -> {
            classicsAdapter.setMovies(movies);
        });

        // Hall of Fame Classic Series (TV)
        viewModel.getClassicsSeries().observe(getViewLifecycleOwner(), series -> {
            classicsSeriesAdapter.setMovies(series);
        });

        // Trending movies
        viewModel.getTrendingMovies().observe(getViewLifecycleOwner(), movies -> {
            trendingAdapter.setMovies(movies);
        });

        // Trending series
        viewModel.getTrendingSeries().observe(getViewLifecycleOwner(), series -> {
            trendingSeriesAdapter.setMovies(series);
        });

        // In Theaters
        viewModel.getNowPlayingMovies().observe(getViewLifecycleOwner(), movies -> {
            nowPlayingAdapter.setMovies(movies);
        });

        // Streaming Series
        viewModel.getStreamingSeries().observe(getViewLifecycleOwner(), series -> {
            streamingSeriesAdapter.setMovies(series);
        });
    }
}

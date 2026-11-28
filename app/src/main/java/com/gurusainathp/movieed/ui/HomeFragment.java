package com.gurusainathp.movieed.ui;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.gurusainathp.movieed.ui.search.SearchViewModel;

import java.util.Collections;
import java.util.List;
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

    // Shared by every HomeMovieAdapter list so poster views are reused across rows
    private RecyclerView.RecycledViewPool posterViewPool;

    // Controllers
    private HomeSearchController homeSearchController;
    private HomeSearchResultsBinder searchResultsBinder;
    private SearchViewModel searchViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        initViews(view);
        setupRecyclerViews();
        observeViewModel();

        // Scoped to this fragment so Home suggestions never share state with the Search page
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        homeSearchController = new HomeSearchController(this, view);
        searchResultsBinder = new HomeSearchResultsBinder(
                this, view, homeSearchController, searchViewModel,
                BuildConfig.TMDB_API_KEY, this::openSearchResult);

        btnRetry.setOnClickListener(v -> viewModel.loadHomeContent(BuildConfig.TMDB_API_KEY, true));
        btnSeeAllDiary.setOnClickListener(v -> navigateToTab(R.id.nav_diary));
        btnExploreMore.setOnClickListener(v -> navigateToTab(R.id.nav_explore));

        loadData();
    }

    @Override
    public void onDestroyView() {
        // Detach adapters so the old view tree can be garbage collected
        detachList(rvRecentlyLogged);
        detachList(rvRecommendedMovies);
        detachList(rvClassicsMovies);
        detachList(rvClassicsSeries);
        detachList(rvTrendingMovies);
        detachList(rvTrendingSeries);
        detachList(rvNowPlayingMovies);
        detachList(rvStreamingSeries);

        posterViewPool = null;
        searchResultsBinder = null;
        homeSearchController = null;
        searchViewModel = null;

        pbLoading = null;
        cardError = null;
        tvError = null;
        btnRetry = null;
        sectionFeatured = null;
        cardHeroBanner = null;
        ivHeroBackdrop = null;
        tvHeroTitle = null;
        tvHeroYear = null;
        tvHeroRating = null;
        sectionRecentlyLogged = null;
        btnSeeAllDiary = null;
        sectionRecommended = null;
        btnExploreMore = null;
        rvRecentlyLogged = null;
        rvRecommendedMovies = null;
        rvClassicsMovies = null;
        rvClassicsSeries = null;
        rvTrendingMovies = null;
        rvTrendingSeries = null;
        rvNowPlayingMovies = null;
        rvStreamingSeries = null;

        recentLogAdapter = null;
        recommendedAdapter = null;
        classicsAdapter = null;
        classicsSeriesAdapter = null;
        trendingAdapter = null;
        trendingSeriesAdapter = null;
        nowPlayingAdapter = null;
        streamingSeriesAdapter = null;

        super.onDestroyView();
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
        posterViewPool = new RecyclerView.RecycledViewPool();
        posterViewPool.setMaxRecycledViews(0, 24);

        // User's recent logs (different item layout, so it does not use the shared pool)
        recentLogAdapter = new HomeRecentLogAdapter(item -> {
            if (item != null && item.movie != null) {
                // TODO: if your logged entity stores the media type, pass it as the 5th argument
                // (like openMovieDetails does) so logged TV shows open as TV, not as movies.
                MovieDetailsActivity.launch(
                        requireContext(),
                        item.movie.getTmdbId(),
                        item.movie.getTitle(),
                        item.movie.getPosterPath()
                );
            }
        });
        setupHorizontalList(rvRecentlyLogged, recentLogAdapter, null);

        // You May Like These (Recommendations)
        recommendedAdapter = new HomeMovieAdapter(this::openMovieDetails);
        setupHorizontalList(rvRecommendedMovies, recommendedAdapter, posterViewPool);

        // Hall of Fame Classics
        classicsAdapter = new HomeMovieAdapter(this::openMovieDetails);
        setupHorizontalList(rvClassicsMovies, classicsAdapter, posterViewPool);

        // Hall of Fame Classic Series
        classicsSeriesAdapter = new HomeMovieAdapter(this::openMovieDetails);
        setupHorizontalList(rvClassicsSeries, classicsSeriesAdapter, posterViewPool);

        // Trending Movies
        trendingAdapter = new HomeMovieAdapter(this::openMovieDetails);
        setupHorizontalList(rvTrendingMovies, trendingAdapter, posterViewPool);

        // Trending Series
        trendingSeriesAdapter = new HomeMovieAdapter(this::openMovieDetails);
        setupHorizontalList(rvTrendingSeries, trendingSeriesAdapter, posterViewPool);

        // Now Playing (In Theaters)
        nowPlayingAdapter = new HomeMovieAdapter(this::openMovieDetails);
        setupHorizontalList(rvNowPlayingMovies, nowPlayingAdapter, posterViewPool);

        // Streaming Series
        streamingSeriesAdapter = new HomeMovieAdapter(this::openMovieDetails);
        setupHorizontalList(rvStreamingSeries, streamingSeriesAdapter, posterViewPool);
    }

    /**
     * One place for the horizontal list setup. Passing a shared pool lets lists with the
     * same item layout reuse each other's views while scrolling the long home page.
     */
    private void setupHorizontalList(@NonNull RecyclerView rv,
                                     @NonNull RecyclerView.Adapter<?> adapter,
                                     @Nullable RecyclerView.RecycledViewPool pool) {
        LinearLayoutManager lm = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        if (pool != null) {
            // Needed so detached views actually go back into the shared pool
            lm.setRecycleChildrenOnDetach(true);
            rv.setRecycledViewPool(pool);
        }
        rv.setLayoutManager(lm);
        rv.setAdapter(adapter);
    }

    private void detachList(@Nullable RecyclerView rv) {
        if (rv != null) rv.setAdapter(null);
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

    private void openSearchResult(@NonNull Movie movie) {
        if (movie == null) return;
        if ("person".equalsIgnoreCase(movie.getMediaType())) {
            com.gurusainathp.movieed.ui.person.PersonDetailsActivity.launch(requireContext(), movie.getId(), movie.getTitle());
        } else {
            openMovieDetails(movie);
        }
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
        viewModel.loadHomeContent(BuildConfig.TMDB_API_KEY, false);
    }

    /**
     * Lists coming from LiveData can be null; adapters should always get a real list.
     */
    private static <T> List<T> safeList(@Nullable List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    private void observeViewModel() {
        // Loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                pbLoading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

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

                String imagePath = movie.getBackdropPath() != null
                        ? movie.getBackdropPath() : movie.getPosterPath();
                if (imagePath != null) {
                    Glide.with(this)
                            .load("https://image.tmdb.org/t/p/w780" + imagePath)
                            .placeholder(R.color.movieed_surface_elevated)
                            .error(R.color.movieed_surface_elevated)
                            .into(ivHeroBackdrop);
                } else {
                    // No image for this movie: don't leave the previous hero image on screen
                    Glide.with(this).clear(ivHeroBackdrop);
                    ivHeroBackdrop.setImageDrawable(new ColorDrawable(
                            ContextCompat.getColor(requireContext(), R.color.movieed_surface_elevated)));
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
        viewModel.getClassicsMovies().observe(getViewLifecycleOwner(),
                movies -> classicsAdapter.setMovies(safeList(movies)));

        // Hall of Fame Classic Series (TV)
        viewModel.getClassicsSeries().observe(getViewLifecycleOwner(),
                series -> classicsSeriesAdapter.setMovies(safeList(series)));

        // Trending movies
        viewModel.getTrendingMovies().observe(getViewLifecycleOwner(),
                movies -> trendingAdapter.setMovies(safeList(movies)));

        // Trending series
        viewModel.getTrendingSeries().observe(getViewLifecycleOwner(),
                series -> trendingSeriesAdapter.setMovies(safeList(series)));

        // In Theaters
        viewModel.getNowPlayingMovies().observe(getViewLifecycleOwner(),
                movies -> nowPlayingAdapter.setMovies(safeList(movies)));

        // Streaming Series
        viewModel.getStreamingSeries().observe(getViewLifecycleOwner(),
                series -> streamingSeriesAdapter.setMovies(safeList(series)));
    }
}
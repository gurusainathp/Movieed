package com.gurusainathp.movieed.ui.movie;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.ui.person.PersonDetailsActivity;

import java.util.Locale;

public class MovieDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_TMDB_ID = "extra_tmdb_id";
    private static final String EXTRA_TITLE = "extra_title";
    private static final String EXTRA_POSTER = "extra_poster";
    private static final String EXTRA_IS_TV = "extra_is_tv";

    public static void launch(Context context, int tmdbId, String title, String posterPath, boolean isTv) {
        Intent intent = new Intent(context, MovieDetailsActivity.class);
        intent.putExtra(EXTRA_TMDB_ID, tmdbId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_POSTER, posterPath);
        intent.putExtra(EXTRA_IS_TV, isTv);
        context.startActivity(intent);
    }

    public static void launch(Context context, int tmdbId, String title, String posterPath) {
        launch(context, tmdbId, title, posterPath, false);
    }

    private MovieDetailsViewModel viewModel;
    private int tmdbId;
    private String movieTitle;
    private String posterPath;

    private ImageView ivBackdrop;
    private ImageView ivPoster;
    private TextView tvTitle;
    private TextView tvYear;
    private TextView tvDetailsRating;
    private TextView tvOverview;

    private Button btnActionWatchlist;
    private Button btnActionRate;

    private RecyclerView rvCast;
    private CastAdapter castAdapter;
    private RecyclerView rvCrew;
    private CrewAdapter crewAdapter;
    private RecyclerView rvProviders;
    private ProviderAdapter providerAdapter;
    private RecyclerView rvRecommendations;
    private com.gurusainathp.movieed.ui.HomeMovieAdapter recommendationAdapter;
    private TextView tvRecommendationsHeader;

    private TmdbMovieDto currentMovieDto;
    private MovieLogEntity currentLogEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        tmdbId = getIntent().getIntExtra(EXTRA_TMDB_ID, -1);
        movieTitle = getIntent().getStringExtra(EXTRA_TITLE);
        posterPath = getIntent().getStringExtra(EXTRA_POSTER);
        boolean isTv = getIntent().getBooleanExtra(EXTRA_IS_TV, false);

        viewModel = new ViewModelProvider(this).get(MovieDetailsViewModel.class);

        ivBackdrop = findViewById(R.id.iv_backdrop);
        ivPoster = findViewById(R.id.iv_details_poster);
        tvTitle = findViewById(R.id.tv_details_title);
        tvYear = findViewById(R.id.tv_details_year);
        tvDetailsRating = findViewById(R.id.tv_details_rating);
        tvOverview = findViewById(R.id.tv_details_overview);

        btnActionWatchlist = findViewById(R.id.btn_action_watchlist);
        btnActionRate = findViewById(R.id.btn_action_rate);

        rvCast = findViewById(R.id.rv_movie_cast);
        castAdapter = new CastAdapter(cast -> PersonDetailsActivity.launch(this, cast.getId(), cast.getName()));
        rvCast.setAdapter(castAdapter);

        rvCrew = findViewById(R.id.rv_movie_crew);
        crewAdapter = new CrewAdapter(crew -> PersonDetailsActivity.launch(this, crew.getId(), crew.getName()));
        rvCrew.setAdapter(crewAdapter);

        rvProviders = findViewById(R.id.rv_movie_providers);
        providerAdapter = new ProviderAdapter();
        rvProviders.setAdapter(providerAdapter);

        tvRecommendationsHeader = findViewById(R.id.tv_recommendations_header);
        rvRecommendations = findViewById(R.id.rv_movie_recommendations);
        recommendationAdapter = new com.gurusainathp.movieed.ui.HomeMovieAdapter(movie ->
                MovieDetailsActivity.launch(this, movie.getId(), movie.getTitle(), movie.getPosterPath()));
        rvRecommendations.setAdapter(recommendationAdapter);

        tvTitle.setText(movieTitle != null ? movieTitle : "");
        if (posterPath != null) {
            Glide.with(this)
                    .load("https://image.tmdb.org/t/p/w500" + posterPath)
                    .into(ivPoster);
        }

        btnActionWatchlist.setOnClickListener(v -> {
            if (currentMovieDto == null && tmdbId != -1) {
                currentMovieDto = new TmdbMovieDto();
            }
            viewModel.toggleWatchlist(currentMovieDto, currentLogEntity);
        });

        btnActionRate.setOnClickListener(v -> openReactionPicker());

        observeViewModel();

        if (tmdbId != -1) {
            viewModel.fetchMovieDetails(tmdbId, isTv);
        }
    }

    private void observeViewModel() {
        viewModel.getTmdbMovieDetails().observe(this, dto -> {
            if (dto != null) {
                currentMovieDto = dto;
                tvTitle.setText(dto.getTitle());

                String releaseDate = dto.getReleaseDate();
                if (releaseDate != null && releaseDate.length() >= 4) {
                    tvYear.setText(releaseDate.substring(0, 4));
                }
                tvOverview.setText(dto.getOverview());

                double voteAvg = dto.getVoteAverage();
                if (voteAvg > 0) {
                    tvDetailsRating.setText("★ " + String.format(Locale.US, "%.1f", voteAvg));
                    tvDetailsRating.setVisibility(View.VISIBLE);
                } else {
                    tvDetailsRating.setVisibility(View.GONE);
                }

                if (dto.getBackdropPath() != null) {
                    Glide.with(this)
                            .load("https://image.tmdb.org/t/p/w780" + dto.getBackdropPath())
                            .into(ivBackdrop);
                }
            }
        });

        viewModel.getCastList().observe(this, castList -> {
            if (castList != null) {
                castAdapter.setCastList(castList);
            }
        });

        viewModel.getCrewList().observe(this, crewList -> {
            if (crewList != null) {
                crewAdapter.setCrewList(crewList);
            }
        });

        viewModel.getProviderList().observe(this, providers -> {
            if (providers != null && !providers.isEmpty()) {
                providerAdapter.setProviders(providers);
                rvProviders.setVisibility(View.VISIBLE);
            } else {
                rvProviders.setVisibility(View.GONE);
            }
        });

        viewModel.getRecommendationList().observe(this, recommendations -> {
            if (recommendations != null && !recommendations.isEmpty()) {
                java.util.List<com.gurusainathp.movieed.domain.model.Movie> domainList = new java.util.ArrayList<>();
                for (TmdbMovieDto dto : recommendations) {
                    if (dto != null) {
                        domainList.add(new com.gurusainathp.movieed.domain.model.Movie(
                                dto.getId(),
                                dto.getTitle(),
                                dto.getOverview(),
                                dto.getReleaseDate(),
                                dto.getPosterPath(),
                                dto.getBackdropPath(),
                                dto.getVoteAverage(),
                                "movie"
                        ));
                    }
                }
                recommendationAdapter.setMovies(domainList);
                tvRecommendationsHeader.setVisibility(View.VISIBLE);
                rvRecommendations.setVisibility(View.VISIBLE);
            } else {
                tvRecommendationsHeader.setVisibility(View.GONE);
                rvRecommendations.setVisibility(View.GONE);
            }
        });

        viewModel.getLogsForMovie(tmdbId).observe(this, logs -> {
            if (logs != null && !logs.isEmpty()) {
                currentLogEntity = logs.get(0);
                if (currentLogEntity.isWatchlist()) {
                    btnActionWatchlist.setText("✓ In Watchlist");
                } else {
                    btnActionWatchlist.setText("+ Watchlist");
                }

                if (currentLogEntity.getReaction() != null && !currentLogEntity.getReaction().isEmpty()) {
                    btnActionRate.setText(MovieLogEntity.getLabelForReaction(currentLogEntity.getReaction()));
                } else {
                    btnActionRate.setText("Rate");
                }
            } else {
                currentLogEntity = null;
                btnActionWatchlist.setText("+ Watchlist");
                btnActionRate.setText("Rate");
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openReactionPicker() {
        if (currentMovieDto == null && tmdbId != -1) {
            currentMovieDto = new TmdbMovieDto();
        }

        String currentReaction = currentLogEntity != null ? currentLogEntity.getReaction() : null;
        ReactionPickerBottomSheetDialogFragment dialog = ReactionPickerBottomSheetDialogFragment.newInstance(currentReaction);
        dialog.setOnReactionSelectedListener(new ReactionPickerBottomSheetDialogFragment.OnReactionSelectedListener() {
            @Override
            public void onReactionSelected(String reaction) {
                viewModel.saveReaction(currentMovieDto, reaction, currentLogEntity);
            }

            @Override
            public void onReactionCleared() {
                viewModel.saveReaction(currentMovieDto, null, currentLogEntity);
            }
        });
        dialog.show(getSupportFragmentManager(), "ReactionPicker");
    }
}

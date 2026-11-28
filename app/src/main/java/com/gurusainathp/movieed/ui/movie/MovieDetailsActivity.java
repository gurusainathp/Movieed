package com.gurusainathp.movieed.ui.movie;

import android.app.Dialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.remote.dto.TmdbGenreDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.Movie;
import com.gurusainathp.movieed.domain.util.GenreMapper;
import com.gurusainathp.movieed.ui.person.PersonDetailsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MovieDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_TMDB_ID = "extra_tmdb_id";
    private static final String EXTRA_TITLE = "extra_title";
    private static final String EXTRA_POSTER = "extra_poster";
    private static final String EXTRA_IS_TV = "extra_is_tv";

    public static void launch(Context context, int tmdbId, String title, String posterPath) {
        launch(context, tmdbId, title, posterPath, false);
    }

    public static void launch(Context context, int tmdbId, String title, String posterPath, boolean isTv) {
        Intent intent = new Intent(context, MovieDetailsActivity.class);
        intent.putExtra(EXTRA_TMDB_ID, tmdbId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_POSTER, posterPath);
        intent.putExtra(EXTRA_IS_TV, isTv);
        context.startActivity(intent);
    }

    private MovieDetailsViewModel viewModel;
    private int tmdbId;
    private String movieTitle;
    private String posterPath;
    private boolean isTv;

    private ImageView ivBackdrop;
    private ImageView ivPoster;
    private TextView tvTypeBadge;
    private TextView tvTitle;
    private TextView tvYear;
    private TextView tvDetailsRating;
    private TextView tvOverview;
    private RecyclerView cgGenres;
    private GenreBadgeAdapter genreAdapter;

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
        isTv = getIntent().getBooleanExtra(EXTRA_IS_TV, false);

        viewModel = new ViewModelProvider(this).get(MovieDetailsViewModel.class);

        ivBackdrop = findViewById(R.id.iv_backdrop);
        ivPoster = findViewById(R.id.iv_details_poster);
        tvTypeBadge = findViewById(R.id.tv_details_type_badge);
        tvTitle = findViewById(R.id.tv_details_title);
        tvYear = findViewById(R.id.tv_details_year);
        tvDetailsRating = findViewById(R.id.tv_details_rating);
        tvOverview = findViewById(R.id.tv_details_overview);
        cgGenres = findViewById(R.id.cg_movie_genres);

        genreAdapter = new GenreBadgeAdapter();
        cgGenres.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        cgGenres.setAdapter(genreAdapter);

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
        recommendationAdapter = new com.gurusainathp.movieed.ui.HomeMovieAdapter(movie -> {
            if (movie != null) {
                boolean recIsTv = "tv".equals(movie.getMediaType());
                MovieDetailsActivity.launch(this, movie.getId(), movie.getTitle(), movie.getPosterPath(), recIsTv);
            }
        });
        rvRecommendations.setAdapter(recommendationAdapter);

        if (tvTypeBadge != null) {
            tvTypeBadge.setText(isTv ? "Series" : "Movie");
        }
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
            viewModel.toggleWatchlist(currentMovieDto, isTv, currentLogEntity);
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

                if (tvTypeBadge != null) {
                    tvTypeBadge.setText(isTv ? "Series" : "Movie");
                }

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

                if (cgGenres != null) {
                    List<String> genreNames = new ArrayList<>();
                    if (dto.getGenres() != null && !dto.getGenres().isEmpty()) {
                        for (TmdbGenreDto genre : dto.getGenres()) {
                            if (genre != null) {
                                genreNames.add(GenreMapper.getGenreName(genre.getId()));
                            }
                        }
                    } else if (dto.getGenreIds() != null && !dto.getGenreIds().isEmpty()) {
                        for (Integer gId : dto.getGenreIds()) {
                            if (gId != null) {
                                genreNames.add(GenreMapper.getGenreName(gId));
                            }
                        }
                    }

                    if (!genreNames.isEmpty()) {
                        genreAdapter.setGenres(genreNames);
                        cgGenres.setVisibility(View.VISIBLE);
                    } else {
                        cgGenres.setVisibility(View.GONE);
                    }
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
                List<Movie> movieList = new java.util.ArrayList<>();
                for (TmdbMovieDto dto : recommendations) {
                    if (dto != null && dto.getPosterPath() != null) {
                        movieList.add(new Movie(
                                dto.getId(),
                                dto.getTitle(),
                                dto.getOverview(),
                                dto.getReleaseDate(),
                                dto.getPosterPath(),
                                dto.getBackdropPath(),
                                dto.getVoteAverage(),
                                dto.getMediaType() != null ? dto.getMediaType() : (isTv ? "tv" : "movie")
                        ));
                    }
                }
                recommendationAdapter.setMovies(movieList);
                rvRecommendations.setVisibility(View.VISIBLE);
                if (tvRecommendationsHeader != null)
                    tvRecommendationsHeader.setVisibility(View.VISIBLE);
            } else {
                rvRecommendations.setVisibility(View.GONE);
                if (tvRecommendationsHeader != null)
                    tvRecommendationsHeader.setVisibility(View.GONE);
            }
        });

        viewModel.getMovieByTmdbId(tmdbId).observe(this, movieEntity -> {
            // Local entity update if needed
        });

        viewModel.getLogsForMovie(tmdbId).observe(this, logs -> {
            if (logs != null && !logs.isEmpty()) {
                currentLogEntity = logs.get(0);
                updateActionButtonsState();
            } else {
                currentLogEntity = null;
                updateActionButtonsState();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateActionButtonsState() {
        if (currentLogEntity != null) {
            boolean inWatchlist = currentLogEntity.isWatchlist();
            String reaction = currentLogEntity.getReaction();

            if (inWatchlist) {
                btnActionWatchlist.setText("✓ In Watchlist");
            } else {
                btnActionWatchlist.setText("+ Watchlist");
            }

            if (reaction != null && !reaction.isEmpty()) {
                btnActionRate.setText("Rated " + MovieLogEntity.getEmojiForReaction(reaction));
            } else {
                btnActionRate.setText("Rate");
            }
        } else {
            btnActionWatchlist.setText("+ Watchlist");
            btnActionRate.setText("Rate");
        }
    }

    private void openReactionPicker() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_reaction_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnLove = dialog.findViewById(R.id.btn_reaction_love);
        View btnLike = dialog.findViewById(R.id.btn_reaction_like);
        View btnUmm = dialog.findViewById(R.id.btn_reaction_umm);
        View btnDislike = dialog.findViewById(R.id.btn_reaction_dislike);
        View btnHate = dialog.findViewById(R.id.btn_reaction_hate);

        if (btnLove != null) btnLove.setOnClickListener(v -> { viewModel.saveReaction(currentMovieDto, "LOVE", isTv, currentLogEntity); dialog.dismiss(); });
        if (btnLike != null) btnLike.setOnClickListener(v -> { viewModel.saveReaction(currentMovieDto, "LIKE", isTv, currentLogEntity); dialog.dismiss(); });
        if (btnUmm != null) btnUmm.setOnClickListener(v -> { viewModel.saveReaction(currentMovieDto, "UMM", isTv, currentLogEntity); dialog.dismiss(); });
        if (btnDislike != null) btnDislike.setOnClickListener(v -> { viewModel.saveReaction(currentMovieDto, "DISLIKE", isTv, currentLogEntity); dialog.dismiss(); });
        if (btnHate != null) btnHate.setOnClickListener(v -> { viewModel.saveReaction(currentMovieDto, "HATE", isTv, currentLogEntity); dialog.dismiss(); });

        dialog.show();
    }
}

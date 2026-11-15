package com.gurusainathp.movieed.ui.movie;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;

public class MovieDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_TMDB_ID = "extra_tmdb_id";
    private static final String EXTRA_TITLE = "extra_title";
    private static final String EXTRA_POSTER = "extra_poster";

    public static void launch(Context context, int tmdbId, String title, String posterPath) {
        Intent intent = new Intent(context, MovieDetailsActivity.class);
        intent.putExtra(EXTRA_TMDB_ID, tmdbId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_POSTER, posterPath);
        context.startActivity(intent);
    }

    private MovieDetailsViewModel viewModel;
    private int tmdbId;
    private String movieTitle;
    private String posterPath;

    private ImageView ivBackdrop;
    private ImageView ivPoster;
    private TextView tvTitle;
    private TextView tvYear;
    private TextView tvOverview;
    private Button btnLogMovie;

    private MaterialCardView cardPersonalLog;
    private RatingBar rbUserRating;
    private TextView tvUserWatchedDate;
    private TextView tvUserReview;

    private TmdbMovieDto currentMovieDto;
    private MovieLogEntity currentLogEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        tmdbId = getIntent().getIntExtra(EXTRA_TMDB_ID, -1);
        movieTitle = getIntent().getStringExtra(EXTRA_TITLE);
        posterPath = getIntent().getStringExtra(EXTRA_POSTER);

        viewModel = new ViewModelProvider(this).get(MovieDetailsViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivBackdrop = findViewById(R.id.iv_backdrop);
        ivPoster = findViewById(R.id.iv_details_poster);
        tvTitle = findViewById(R.id.tv_details_title);
        tvYear = findViewById(R.id.tv_details_year);
        tvOverview = findViewById(R.id.tv_details_overview);
        btnLogMovie = findViewById(R.id.btn_log_movie);

        cardPersonalLog = findViewById(R.id.card_personal_log);
        rbUserRating = findViewById(R.id.rb_user_rating);
        tvUserWatchedDate = findViewById(R.id.tv_user_watched_date);
        tvUserReview = findViewById(R.id.tv_user_review);

        tvTitle.setText(movieTitle != null ? movieTitle : "");
        if (posterPath != null) {
            Glide.with(this)
                    .load("https://image.tmdb.org/t/p/w500" + posterPath)
                    .into(ivPoster);
        }

        btnLogMovie.setOnClickListener(v -> openLogDialog());

        observeViewModel();

        if (tmdbId != -1) {
            viewModel.fetchMovieDetails(tmdbId);
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

                if (dto.getBackdropPath() != null) {
                    Glide.with(this)
                            .load("https://image.tmdb.org/t/p/w780" + dto.getBackdropPath())
                            .into(ivBackdrop);
                }
            }
        });

        viewModel.getLogsForMovie(tmdbId).observe(this, logs -> {
            if (logs != null && !logs.isEmpty()) {
                currentLogEntity = logs.get(0);
                cardPersonalLog.setVisibility(View.VISIBLE);
                btnLogMovie.setText("Edit Log");
                rbUserRating.setRating(currentLogEntity.getRating());
                tvUserWatchedDate.setText("Watched on " + currentLogEntity.getWatchedDate());
                if (currentLogEntity.getReview() != null && !currentLogEntity.getReview().isEmpty()) {
                    tvUserReview.setText(currentLogEntity.getReview());
                    tvUserReview.setVisibility(View.VISIBLE);
                } else {
                    tvUserReview.setVisibility(View.GONE);
                }
            } else {
                currentLogEntity = null;
                cardPersonalLog.setVisibility(View.GONE);
                btnLogMovie.setText("Log Movie");
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openLogDialog() {
        if (currentMovieDto == null && tmdbId != -1) {
            // Minimal DTO fallback if details haven't returned yet
            currentMovieDto = new TmdbMovieDto();
        }

        LogMovieBottomSheetDialogFragment dialog = LogMovieBottomSheetDialogFragment.newInstance(currentLogEntity);
        dialog.setOnLogSaveListener(new LogMovieBottomSheetDialogFragment.OnLogSaveListener() {
            @Override
            public void onSave(float rating, String watchedDate, String review, String note) {
                if (currentMovieDto != null) {
                    viewModel.saveMovieAndLog(currentMovieDto, rating, watchedDate, review, note, currentLogEntity);
                    Toast.makeText(MovieDetailsActivity.this, "Log saved to journal!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDelete() {
                if (currentLogEntity != null) {
                    viewModel.deleteLog(currentLogEntity);
                    Toast.makeText(MovieDetailsActivity.this, "Log deleted.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog.show(getSupportFragmentManager(), "LogMovieDialog");
    }
}

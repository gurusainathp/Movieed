package com.gurusainathp.movieed.ui.person;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.domain.model.Movie;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;
import com.gurusainathp.movieed.ui.search.SearchMovieAdapter;

import java.util.ArrayList;
import java.util.List;

public class PersonDetailsActivity extends AppCompatActivity {

    private static final String EXTRA_PERSON_ID = "extra_person_id";
    private static final String EXTRA_NAME = "extra_name";

    public static void launch(Context context, int personId, String name) {
        Intent intent = new Intent(context, PersonDetailsActivity.class);
        intent.putExtra(EXTRA_PERSON_ID, personId);
        intent.putExtra(EXTRA_NAME, name);
        context.startActivity(intent);
    }

    private PersonViewModel viewModel;
    private int personId;
    private String personName;

    private ImageView ivProfile;
    private TextView tvName;
    private TextView tvInfo;
    private TextView tvBiography;
    private TextView tvReadMore;
    private RecyclerView rvFilmography;
    private SearchMovieAdapter filmographyAdapter;
    private boolean isBioExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_details);

        personId = getIntent().getIntExtra(EXTRA_PERSON_ID, -1);
        personName = getIntent().getStringExtra(EXTRA_NAME);

        viewModel = new ViewModelProvider(this).get(PersonViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.person_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (personName != null) {
            toolbar.setTitle(personName);
        }

        ivProfile = findViewById(R.id.iv_person_profile);
        tvName = findViewById(R.id.tv_person_name);
        tvInfo = findViewById(R.id.tv_person_info);
        tvBiography = findViewById(R.id.tv_person_biography);
        tvReadMore = findViewById(R.id.tv_read_more);
        rvFilmography = findViewById(R.id.rv_person_filmography);

        tvReadMore.setOnClickListener(v -> {
            isBioExpanded = !isBioExpanded;
            if (isBioExpanded) {
                tvBiography.setMaxLines(Integer.MAX_VALUE);
                tvReadMore.setText("Read Less");
            } else {
                tvBiography.setMaxLines(4);
                tvReadMore.setText("Read More");
            }
        });

        tvName.setText(personName != null ? personName : "");

        filmographyAdapter = new SearchMovieAdapter(movie -> {
            MovieDetailsActivity.launch(this, movie.getId(), movie.getTitle(), movie.getPosterPath());
        });
        rvFilmography.setAdapter(filmographyAdapter);

        observeViewModel();

        if (personId != -1) {
            viewModel.fetchPerson(personId);
        }
    }

    private void observeViewModel() {
        viewModel.getPersonDetails().observe(this, dto -> {
            if (dto != null) {
                tvName.setText(dto.getName());

                StringBuilder info = new StringBuilder();
                if (dto.getBirthday() != null && !dto.getBirthday().isEmpty()) {
                    info.append("Born: ").append(dto.getBirthday()).append("\n");
                }
                if (dto.getPlaceOfBirth() != null && !dto.getPlaceOfBirth().isEmpty()) {
                    info.append(dto.getPlaceOfBirth());
                }
                tvInfo.setText(info.toString().trim());

                if (dto.getBiography() != null && !dto.getBiography().isEmpty()) {
                    tvBiography.setText(dto.getBiography());
                    tvReadMore.setVisibility(View.VISIBLE);
                } else {
                    tvBiography.setText("No biography available.");
                    tvReadMore.setVisibility(View.GONE);
                }

                if (dto.getProfilePath() != null) {
                    Glide.with(this)
                            .load("https://image.tmdb.org/t/p/w500" + dto.getProfilePath())
                            .placeholder(R.color.movieed_surface_elevated)
                            .into(ivProfile);
                }
            }
        });

        viewModel.getFilmography().observe(this, list -> {
            if (list != null) {
                List<Movie> movies = new ArrayList<>();
                for (var dto : list) {
                    movies.add(new Movie(
                            dto.getId(),
                            dto.getTitle(),
                            dto.getOverview(),
                            dto.getReleaseDate(),
                            dto.getPosterPath(),
                            dto.getBackdropPath(),
                            dto.getVoteAverage(),
                            dto.getMediaType()
                    ));
                }
                filmographyAdapter.setMovies(movies);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

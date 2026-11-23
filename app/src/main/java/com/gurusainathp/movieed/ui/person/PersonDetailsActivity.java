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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;
import com.gurusainathp.movieed.domain.model.Movie;
import com.gurusainathp.movieed.ui.HomeMovieAdapter;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;

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

    private ImageView ivBackdrop;
    private ImageView ivProfile;
    private TextView tvName;
    private TextView tvDepartment;
    private TextView tvInfo;
    private TextView tvBiography;
    private TextView tvReadMore;

    private RecyclerView rvKnownFor;
    private HomeMovieAdapter knownForAdapter;

    private RecyclerView rvFilmography;
    private PersonFilmographyAdapter filmographyAdapter;
    private MaterialButton btnShowAll;

    private boolean isBioExpanded = false;
    private boolean isFilmographyExpanded = false;
    private List<TmdbPersonCastCreditDto> fullCreditsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_details);

        personId = getIntent().getIntExtra(EXTRA_PERSON_ID, -1);
        personName = getIntent().getStringExtra(EXTRA_NAME);

        viewModel = new ViewModelProvider(this).get(PersonViewModel.class);

        ivBackdrop = findViewById(R.id.iv_person_backdrop);
        ivProfile = findViewById(R.id.iv_person_profile);
        tvName = findViewById(R.id.tv_person_name);
        tvDepartment = findViewById(R.id.tv_person_department);
        tvInfo = findViewById(R.id.tv_person_info);
        tvBiography = findViewById(R.id.tv_person_biography);
        tvReadMore = findViewById(R.id.tv_read_more);

        rvKnownFor = findViewById(R.id.rv_person_known_for);
        rvFilmography = findViewById(R.id.rv_person_filmography);
        btnShowAll = findViewById(R.id.btn_show_all_filmography);

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

        knownForAdapter = new HomeMovieAdapter(movie -> {
            if (movie != null) {
                boolean isTv = "tv".equals(movie.getMediaType());
                MovieDetailsActivity.launch(this, movie.getId(), movie.getTitle(), movie.getPosterPath(), isTv);
            }
        });
        rvKnownFor.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvKnownFor.setAdapter(knownForAdapter);

        filmographyAdapter = new PersonFilmographyAdapter(credit -> {
            if (credit != null) {
                boolean isTv = "tv".equals(credit.getMediaType());
                MovieDetailsActivity.launch(this, credit.getId(), credit.getTitle(), credit.getPosterPath(), isTv);
            }
        });
        rvFilmography.setLayoutManager(new LinearLayoutManager(this));
        rvFilmography.setAdapter(filmographyAdapter);

        btnShowAll.setOnClickListener(v -> {
            isFilmographyExpanded = true;
            btnShowAll.setVisibility(View.GONE);
            displayFilmography();
        });

        observeViewModel();

        if (personId != -1) {
            viewModel.fetchPerson(personId);
        }
    }

    private void observeViewModel() {
        viewModel.getPersonDetails().observe(this, dto -> {
            if (dto != null) {
                tvName.setText(dto.getName());

                if (dto.getKnownForDepartment() != null && !dto.getKnownForDepartment().isEmpty()) {
                    tvDepartment.setText(formatDepartment(dto.getKnownForDepartment()));
                    tvDepartment.setVisibility(View.VISIBLE);
                } else {
                    tvDepartment.setVisibility(View.GONE);
                }

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

                // Known For from TMDB DTO if available
                if (dto.getKnownFor() != null && !dto.getKnownFor().isEmpty()) {
                    List<Movie> knownForList = new ArrayList<>();
                    for (TmdbMovieDto movieDto : dto.getKnownFor()) {
                        if (movieDto != null && movieDto.getPosterPath() != null) {
                            knownForList.add(new Movie(
                                    movieDto.getId(),
                                    movieDto.getTitle(),
                                    movieDto.getOverview(),
                                    movieDto.getReleaseDate(),
                                    movieDto.getPosterPath(),
                                    movieDto.getBackdropPath(),
                                    movieDto.getVoteAverage(),
                                    movieDto.getMediaType() != null ? movieDto.getMediaType() : "movie"
                            ));
                        }
                    }
                    if (!knownForList.isEmpty()) {
                        knownForAdapter.setMovies(knownForList);
                    }
                }
            }
        });

        viewModel.getFilmography().observe(this, list -> {
            if (list != null) {
                fullCreditsList = new ArrayList<>(list);
                fullCreditsList.sort((o1, o2) -> {
                    String d1 = o1.getReleaseDate() != null ? o1.getReleaseDate() : "";
                    String d2 = o2.getReleaseDate() != null ? o2.getReleaseDate() : "";
                    return d2.compareTo(d1);
                });

                // Load movie backdrop for the person from their highest rated film
                for (TmdbPersonCastCreditDto credit : fullCreditsList) {
                    if (credit != null && credit.getBackdropPath() != null && !credit.getBackdropPath().isEmpty()) {
                        Glide.with(this)
                                .load("https://image.tmdb.org/t/p/w780" + credit.getBackdropPath())
                                .placeholder(R.color.movieed_surface_elevated)
                                .into(ivBackdrop);
                        break;
                    }
                }

                // If TMDB person DTO knownFor was empty, rank by popularity & vote count weight
                if (knownForAdapter.getItemCount() == 0) {
                    List<TmdbPersonCastCreditDto> sortedByPopularity = new ArrayList<>(fullCreditsList);
                    sortedByPopularity.sort((o1, o2) -> {
                        double s1 = o1.getVoteAverage() * Math.log10(Math.max(1, o1.getVoteCount()));
                        double s2 = o2.getVoteAverage() * Math.log10(Math.max(1, o2.getVoteCount()));
                        return Double.compare(s2, s1);
                    });

                    List<Movie> knownForFallback = new ArrayList<>();
                    int topCount = Math.min(8, sortedByPopularity.size());
                    for (int i = 0; i < topCount; i++) {
                        TmdbPersonCastCreditDto credit = sortedByPopularity.get(i);
                        if (credit != null && credit.getPosterPath() != null) {
                            knownForFallback.add(new Movie(
                                    credit.getId(),
                                    credit.getTitle(),
                                    credit.getOverview(),
                                    credit.getReleaseDate(),
                                    credit.getPosterPath(),
                                    credit.getBackdropPath(),
                                    credit.getVoteAverage(),
                                    credit.getMediaType() != null ? credit.getMediaType() : "movie"
                            ));
                        }
                    }
                    knownForAdapter.setMovies(knownForFallback);
                }

                displayFilmography();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDepartment(String department) {
        if (department == null) return "";
        switch (department.trim().toLowerCase()) {
            case "acting": return "Actor";
            case "directing": return "Director";
            case "writing": return "Writer";
            case "production": return "Producer";
            case "editing": return "Editor";
            case "camera": return "Cinematographer";
            case "sound":
            case "music": return "Composer";
            default: return department;
        }
    }

    private void displayFilmography() {
        if (fullCreditsList.isEmpty()) return;

        if (isFilmographyExpanded || fullCreditsList.size() <= 10) {
            filmographyAdapter.setCredits(fullCreditsList);
            btnShowAll.setVisibility(View.GONE);
        } else {
            List<TmdbPersonCastCreditDto> subList = fullCreditsList.subList(0, 10);
            filmographyAdapter.setCredits(subList);
            btnShowAll.setVisibility(View.VISIBLE);
        }
    }
}

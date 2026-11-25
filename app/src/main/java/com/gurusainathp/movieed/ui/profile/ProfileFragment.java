package com.gurusainathp.movieed.ui.profile;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.ui.HomeMovieAdapter;
import com.gurusainathp.movieed.ui.diary.DiaryPagerAdapter;
import com.gurusainathp.movieed.ui.diary.DiaryViewModel;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;
import com.gurusainathp.movieed.ui.person.PersonDetailsActivity;

import java.util.Map;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private DiaryViewModel diaryViewModel;

    private TextView tvTotalFilms;
    private TextView tvWatchTime;
    private TextView tvInWatchlist;
    private TextView tvWatchedCount;

    private TextView tvFavDecade;
    private TextView tvEmptyActors;
    private TextView tvEmptyActresses;
    private TextView tvEmptyDirectors;
    private TextView tvEmptyComposers;

    private RecyclerView rvFavActors;
    private RecyclerView rvFavActresses;
    private RecyclerView rvFavDirectors;
    private RecyclerView rvFavComposers;
    private RecyclerView rvDecadeMovies;

    private ProfileCastAdapter actorAdapter;
    private ProfileCastAdapter actressAdapter;
    private ProfileCastAdapter directorAdapter;
    private ProfileCastAdapter composerAdapter;
    private HomeMovieAdapter decadeMoviesAdapter;
    private com.google.android.material.chip.ChipGroup cgTopGenres;

    private LinearProgressIndicator progressLove;
    private LinearProgressIndicator progressLike;
    private LinearProgressIndicator progressUmm;
    private LinearProgressIndicator progressDislike;
    private LinearProgressIndicator progressHate;

    private TextView tvCountLove;
    private TextView tvCountLike;
    private TextView tvCountUmm;
    private TextView tvCountDislike;
    private TextView tvCountHate;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        diaryViewModel = new ViewModelProvider(this).get(DiaryViewModel.class);

        initViews(view);
        setupRecyclerViews();

        // Setup Diary Pager inside ProfileFragment
        DiaryPagerAdapter diaryPagerAdapter = new DiaryPagerAdapter(this);
        viewPager.setAdapter(diaryPagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Watchlist");
            } else {
                tab.setText("Watched");
            }
        }).attach();

        observeViewModel();
    }

    private void initViews(View view) {
        tvTotalFilms = view.findViewById(R.id.tv_stat_total_films);
        tvWatchTime = view.findViewById(R.id.tv_stat_watch_time);
        tvInWatchlist = view.findViewById(R.id.tv_stat_in_watchlist);
        tvWatchedCount = view.findViewById(R.id.tv_stat_watched_count);

        tvFavDecade = view.findViewById(R.id.tv_fav_decade);
        tvEmptyActors = view.findViewById(R.id.tv_empty_actors);
        tvEmptyActresses = view.findViewById(R.id.tv_empty_actresses);
        tvEmptyDirectors = view.findViewById(R.id.tv_empty_directors);
        tvEmptyComposers = view.findViewById(R.id.tv_empty_composers);

        rvFavActors = view.findViewById(R.id.rv_fav_actors);
        rvFavActresses = view.findViewById(R.id.rv_fav_actresses);
        rvFavDirectors = view.findViewById(R.id.rv_fav_directors);
        rvFavComposers = view.findViewById(R.id.rv_fav_composers);
        rvDecadeMovies = view.findViewById(R.id.rv_decade_movies);

        progressLove = view.findViewById(R.id.progress_reaction_love);
        progressLike = view.findViewById(R.id.progress_reaction_like);
        progressUmm = view.findViewById(R.id.progress_reaction_umm);
        progressDislike = view.findViewById(R.id.progress_reaction_dislike);
        progressHate = view.findViewById(R.id.progress_reaction_hate);

        tvCountLove = view.findViewById(R.id.tv_count_reaction_love);
        tvCountLike = view.findViewById(R.id.tv_count_reaction_like);
        tvCountUmm = view.findViewById(R.id.tv_count_reaction_umm);
        tvCountDislike = view.findViewById(R.id.tv_count_reaction_dislike);
        tvCountHate = view.findViewById(R.id.tv_count_reaction_hate);

        tabLayout = view.findViewById(R.id.tl_profile_diary_tabs);
        viewPager = view.findViewById(R.id.vp_profile_diary_pager);
        cgTopGenres = view.findViewById(R.id.cg_top_genres);
    }

    private void setupRecyclerViews() {
        actorAdapter = new ProfileCastAdapter(person -> {
            if (person != null) {
                PersonDetailsActivity.launch(requireContext(), person.getId(), person.getName());
            }
        });
        if (rvFavActors != null) {
            rvFavActors.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rvFavActors.setAdapter(actorAdapter);
        }

        actressAdapter = new ProfileCastAdapter(person -> {
            if (person != null) {
                PersonDetailsActivity.launch(requireContext(), person.getId(), person.getName());
            }
        });
        if (rvFavActresses != null) {
            rvFavActresses.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rvFavActresses.setAdapter(actressAdapter);
        }

        directorAdapter = new ProfileCastAdapter(person -> {
            if (person != null) {
                PersonDetailsActivity.launch(requireContext(), person.getId(), person.getName());
            }
        });
        if (rvFavDirectors != null) {
            rvFavDirectors.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rvFavDirectors.setAdapter(directorAdapter);
        }

        composerAdapter = new ProfileCastAdapter(person -> {
            if (person != null) {
                PersonDetailsActivity.launch(requireContext(), person.getId(), person.getName());
            }
        });
        if (rvFavComposers != null) {
            rvFavComposers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rvFavComposers.setAdapter(composerAdapter);
        }

        decadeMoviesAdapter = new HomeMovieAdapter(movie -> {
            if (movie != null) {
                boolean isTv = "tv".equals(movie.getMediaType());
                MovieDetailsActivity.launch(requireContext(), movie.getId(), movie.getTitle(), movie.getPosterPath(), isTv);
            }
        });
        if (rvDecadeMovies != null) {
            rvDecadeMovies.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rvDecadeMovies.setAdapter(decadeMoviesAdapter);
        }
    }

    private void observeViewModel() {
        viewModel.getProfileStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                tvTotalFilms.setText(String.valueOf(stats.totalFilms));
                tvWatchTime.setText(stats.getFormattedWatchTime());

                if (tvInWatchlist != null) {
                    tvInWatchlist.setText(String.valueOf(stats.inWatchlistCount));
                }
                if (tvWatchedCount != null) {
                    tvWatchedCount.setText(String.valueOf(stats.watchedCount));
                }

                int maxCount = Math.max(1, stats.totalFilms);
                Map<String, Integer> counts = stats.reactionCounts;

                updateReactionRow(progressLove, tvCountLove, counts.get("LOVE"), maxCount);
                updateReactionRow(progressLike, tvCountLike, counts.get("LIKE"), maxCount);
                updateReactionRow(progressUmm, tvCountUmm, counts.get("UMM"), maxCount);
                updateReactionRow(progressDislike, tvCountDislike, counts.get("DISLIKE"), maxCount);
                updateReactionRow(progressHate, tvCountHate, counts.get("HATE"), maxCount);
            }
        });

        viewModel.getActorList().observe(getViewLifecycleOwner(), list -> {
            if (list != null && !list.isEmpty()) {
                actorAdapter.setPeople(list);
                if (rvFavActors != null) rvFavActors.setVisibility(View.VISIBLE);
                if (tvEmptyActors != null) tvEmptyActors.setVisibility(View.GONE);
            } else {
                if (rvFavActors != null) rvFavActors.setVisibility(View.GONE);
                if (tvEmptyActors != null) tvEmptyActors.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getActressList().observe(getViewLifecycleOwner(), list -> {
            if (list != null && !list.isEmpty()) {
                actressAdapter.setPeople(list);
                if (rvFavActresses != null) rvFavActresses.setVisibility(View.VISIBLE);
                if (tvEmptyActresses != null) tvEmptyActresses.setVisibility(View.GONE);
            } else {
                if (rvFavActresses != null) rvFavActresses.setVisibility(View.GONE);
                if (tvEmptyActresses != null) tvEmptyActresses.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getDirectorList().observe(getViewLifecycleOwner(), list -> {
            if (list != null && !list.isEmpty()) {
                directorAdapter.setPeople(list);
                if (rvFavDirectors != null) rvFavDirectors.setVisibility(View.VISIBLE);
                if (tvEmptyDirectors != null) tvEmptyDirectors.setVisibility(View.GONE);
            } else {
                if (rvFavDirectors != null) rvFavDirectors.setVisibility(View.GONE);
                if (tvEmptyDirectors != null) tvEmptyDirectors.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getComposerList().observe(getViewLifecycleOwner(), list -> {
            if (list != null && !list.isEmpty()) {
                composerAdapter.setPeople(list);
                if (rvFavComposers != null) rvFavComposers.setVisibility(View.VISIBLE);
                if (tvEmptyComposers != null) tvEmptyComposers.setVisibility(View.GONE);
            } else {
                if (rvFavComposers != null) rvFavComposers.setVisibility(View.GONE);
                if (tvEmptyComposers != null) tvEmptyComposers.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getTopDecade().observe(getViewLifecycleOwner(), decade -> {
            if (tvFavDecade != null && decade != null) {
                tvFavDecade.setText(decade);
            }
        });

        viewModel.getTopDecadeMovies().observe(getViewLifecycleOwner(), movies -> {
            if (movies != null && !movies.isEmpty()) {
                decadeMoviesAdapter.setMovies(movies);
                if (rvDecadeMovies != null) rvDecadeMovies.setVisibility(View.VISIBLE);
            } else {
                if (rvDecadeMovies != null) rvDecadeMovies.setVisibility(View.GONE);
            }
        });

        viewModel.getTopGenres().observe(getViewLifecycleOwner(), genres -> {
            if (cgTopGenres != null) {
                cgTopGenres.removeAllViews();
                if (genres != null && !genres.isEmpty()) {
                    for (ProfileViewModel.GenreStat stat : genres) {
                        com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip)
                                LayoutInflater.from(requireContext()).inflate(R.layout.item_top_genre_chip, cgTopGenres, false);

                        String text = stat.name + "  •  " + stat.percentage + "%";
                        SpannableString spannable = new SpannableString(text);
                        int dotIndex = text.indexOf("•");
                        if (dotIndex != -1) {
                            spannable.setSpan(
                                    new ForegroundColorSpan(getResources().getColor(R.color.movieed_accent, null)),
                                    dotIndex + 1,
                                    text.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            spannable.setSpan(
                                    new StyleSpan(Typeface.BOLD),
                                    dotIndex + 1,
                                    text.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                        }

                        chip.setText(spannable);
                        cgTopGenres.addView(chip);
                    }
                }
            }
        });
    }

    private void updateReactionRow(LinearProgressIndicator progress, TextView tvCount, Integer countObj, int maxCount) {
        int count = countObj != null ? countObj : 0;
        tvCount.setText(String.valueOf(count));
        int percentage = Math.round(((float) count / maxCount) * 100);
        progress.setProgress(percentage);
    }
}

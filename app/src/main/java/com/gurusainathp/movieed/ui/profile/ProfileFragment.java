package com.gurusainathp.movieed.ui.profile;

import android.animation.ValueAnimator;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class ProfileFragment extends Fragment {

    private static boolean hasAnimatedStatsThisSession = false;

    private ProfileViewModel viewModel;
    private DiaryViewModel diaryViewModel;

    private ProfileViewModel.ProfileStats pendingStats;
    private final android.graphics.Rect statVisibleRect = new android.graphics.Rect();

    private View cardAvatar;
    private TextView tvAvatarInitial;
    private TextView tvProfileHeader;
    private TextView tvMemberSince;

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
    private GenreBubbleView bubbleTopGenres;

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

        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        diaryViewModel = new ViewModelProvider(this).get(DiaryViewModel.class);

        initViews(view);
        setupRecyclerViews();
        loadUserData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();

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
        cardAvatar = view.findViewById(R.id.card_avatar);
        tvAvatarInitial = view.findViewById(R.id.tv_avatar_initial);
        tvProfileHeader = view.findViewById(R.id.tv_profile_header);
        tvMemberSince = view.findViewById(R.id.tv_member_since);

        if (cardAvatar != null) {
            cardAvatar.setOnClickListener(v -> EditProfileActivity.launch(requireContext()));
        }
        if (tvProfileHeader != null) {
            tvProfileHeader.setOnClickListener(v -> EditProfileActivity.launch(requireContext()));
        }

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
        bubbleTopGenres = view.findViewById(R.id.bubble_top_genres);

        if (tvTotalFilms != null) tvTotalFilms.setText("0");
        if (tvWatchTime != null) tvWatchTime.setText("0 hrs");
        if (tvInWatchlist != null) tvInWatchlist.setText("0");
        if (tvWatchedCount != null) tvWatchedCount.setText("0");

        if (tvCountLove != null) tvCountLove.setText("0");
        if (tvCountLike != null) tvCountLike.setText("0");
        if (tvCountUmm != null) tvCountUmm.setText("0");
        if (tvCountDislike != null) tvCountDislike.setText("0");
        if (tvCountHate != null) tvCountHate.setText("0");

        if (progressLove != null) progressLove.setProgress(0);
        if (progressLike != null) progressLike.setProgress(0);
        if (progressUmm != null) progressUmm.setProgress(0);
        if (progressDislike != null) progressDislike.setProgress(0);
        if (progressHate != null) progressHate.setProgress(0);
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
            if (stats == null) return;
            pendingStats = stats;
            updateStatsUi();
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
            if (bubbleTopGenres == null) return;
            List<GenreBubbleView.Bubble> bubbles = new ArrayList<>();
            if (genres != null) {
                for (ProfileViewModel.GenreStat stat : genres) {
                    bubbles.add(new GenreBubbleView.Bubble(stat.name, (float) stat.percentage));
                }
            }
            bubbleTopGenres.setData(bubbles);
        });

        viewModel.getMemberSince().observe(getViewLifecycleOwner(), memberSince -> {
            if (tvMemberSince != null && memberSince != null) {
                tvMemberSince.setText(memberSince);
            }
        });
    }

    private void loadUserData() {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(EditProfileActivity.PREF_NAME, android.content.Context.MODE_PRIVATE);
        String name = prefs.getString(EditProfileActivity.KEY_USER_NAME, "Alex");
        if (tvProfileHeader != null) {
            tvProfileHeader.setText(name);
        }
        if (tvAvatarInitial != null) {
            String initial = !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "A";
            tvAvatarInitial.setText(initial);
        }
    }

    private void updateStatsUi() {
        if (pendingStats == null) return;
        if (hasAnimatedStatsThisSession) {
            displayStatsDirectly(pendingStats);
        } else {
            checkAndAnimateStatsIfNeeded();
        }
    }

    private void checkAndAnimateStatsIfNeeded() {
        if (hasAnimatedStatsThisSession || pendingStats == null || tvTotalFilms == null) return;
        if (isElementFullyVisible(tvTotalFilms)) {
            hasAnimatedStatsThisSession = true;
            runStatsAnimations(pendingStats);
        } else {
            tvTotalFilms.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (tvTotalFilms != null && tvTotalFilms.getViewTreeObserver().isAlive()) {
                        tvTotalFilms.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    if (!hasAnimatedStatsThisSession && pendingStats != null && isElementFullyVisible(tvTotalFilms)) {
                        hasAnimatedStatsThisSession = true;
                        runStatsAnimations(pendingStats);
                        return true;
                    }
                    return true;
                }
            });
        }
    }

    private boolean isElementFullyVisible(View view) {
        if (view == null || !view.isShown() || view.getHeight() == 0) return false;
        if (!view.getGlobalVisibleRect(statVisibleRect)) return false;
        return statVisibleRect.height() >= view.getHeight();
    }

    private void displayStatsDirectly(ProfileViewModel.ProfileStats stats) {
        if (tvTotalFilms != null) tvTotalFilms.setText(String.valueOf(stats.totalFilms));
        if (tvWatchTime != null) tvWatchTime.setText(stats.getFormattedWatchTime());
        if (tvInWatchlist != null) tvInWatchlist.setText(String.valueOf(stats.inWatchlistCount));
        if (tvWatchedCount != null) tvWatchedCount.setText(String.valueOf(stats.watchedCount));

        int maxCount = Math.max(1, stats.totalFilms);
        Map<String, Integer> counts = stats.reactionCounts;

        updateReactionRowDirectly(progressLove, tvCountLove, counts.get("LOVE"), maxCount);
        updateReactionRowDirectly(progressLike, tvCountLike, counts.get("LIKE"), maxCount);
        updateReactionRowDirectly(progressUmm, tvCountUmm, counts.get("UMM"), maxCount);
        updateReactionRowDirectly(progressDislike, tvCountDislike, counts.get("DISLIKE"), maxCount);
        updateReactionRowDirectly(progressHate, tvCountHate, counts.get("HATE"), maxCount);
    }

    private void updateReactionRowDirectly(LinearProgressIndicator progress, TextView tvCount, Integer countObj, int maxCount) {
        int count = countObj != null ? countObj : 0;
        if (tvCount != null) tvCount.setText(String.valueOf(count));
        int targetPercentage = Math.round(((float) count / maxCount) * 100);
        if (progress != null) progress.setProgress(targetPercentage);
    }

    private void runStatsAnimations(ProfileViewModel.ProfileStats stats) {
        animateCounter(tvTotalFilms, stats.totalFilms, null, null);
        animateCounter(tvWatchTime, stats.totalRuntimeHours, "~", " hrs");

        if (tvInWatchlist != null) {
            animateCounter(tvInWatchlist, stats.inWatchlistCount, null, null);
        }
        if (tvWatchedCount != null) {
            animateCounter(tvWatchedCount, stats.watchedCount, null, null);
        }

        int maxCount = Math.max(1, stats.totalFilms);
        Map<String, Integer> counts = stats.reactionCounts;

        updateReactionRow(progressLove, tvCountLove, counts.get("LOVE"), maxCount);
        updateReactionRow(progressLike, tvCountLike, counts.get("LIKE"), maxCount);
        updateReactionRow(progressUmm, tvCountUmm, counts.get("UMM"), maxCount);
        updateReactionRow(progressDislike, tvCountDislike, counts.get("DISLIKE"), maxCount);
        updateReactionRow(progressHate, tvCountHate, counts.get("HATE"), maxCount);
    }

    private void animateCounter(TextView textView, int targetValue, String prefix, String suffix) {
        if (textView == null) return;
        if (textView.getTag() instanceof ValueAnimator) {
            ((ValueAnimator) textView.getTag()).cancel();
        }
        ValueAnimator animator = ValueAnimator.ofInt(0, targetValue);
        animator.setDuration(1000L);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int current = (int) animation.getAnimatedValue();
            StringBuilder sb = new StringBuilder();
            if (prefix != null) sb.append(prefix);
            sb.append(current);
            if (suffix != null) sb.append(suffix);
            textView.setText(sb.toString());
        });
        textView.setTag(animator);
        animator.start();
    }

    private void updateReactionRow(LinearProgressIndicator progress, TextView tvCount, Integer countObj, int maxCount) {
        int count = countObj != null ? countObj : 0;
        animateCounter(tvCount, count, null, null);
        int targetPercentage = Math.round(((float) count / maxCount) * 100);

        if (progress.getTag() instanceof ValueAnimator) {
            ((ValueAnimator) progress.getTag()).cancel();
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, targetPercentage);
        animator.setDuration(800L);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int current = (int) animation.getAnimatedValue();
            progress.setProgress(current);
        });
        progress.setTag(animator);
        animator.start();
    }
}

package com.gurusainathp.movieed.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.ui.diary.DiaryPagerAdapter;
import com.gurusainathp.movieed.ui.diary.DiaryViewModel;

import java.util.Map;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private DiaryViewModel diaryViewModel;

    private TextView tvTotalFilms;
    private TextView tvWatchTime;
    private TextView tvInWatchlist;
    private TextView tvWatchedCount;

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

        tvTotalFilms = view.findViewById(R.id.tv_stat_total_films);
        tvWatchTime = view.findViewById(R.id.tv_stat_watch_time);
        tvInWatchlist = view.findViewById(R.id.tv_stat_in_watchlist);
        tvWatchedCount = view.findViewById(R.id.tv_stat_watched_count);

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

        MonthlyBarChartView chartMonthly = view.findViewById(R.id.chart_monthly_activity);
        if (chartMonthly != null) {
            chartMonthly.setMonthlyData(new int[]{2, 4, 6, 3, 5, 8, 4, 7, 5, 9, 6, 4});
        }

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
    }

    private void updateReactionRow(LinearProgressIndicator progress, TextView tvCount, Integer countObj, int maxCount) {
        int count = countObj != null ? countObj : 0;
        tvCount.setText(String.valueOf(count));
        int percentage = Math.round(((float) count / maxCount) * 100);
        progress.setProgress(percentage);
    }
}

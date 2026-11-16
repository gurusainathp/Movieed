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

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.gurusainathp.movieed.R;

import java.util.Map;

public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;

    private TextView tvTotalFilms;
    private TextView tvWatchTime;
    private TextView tvAvgRating;

    private LinearProgressIndicator progress5;
    private LinearProgressIndicator progress4;
    private LinearProgressIndicator progress3;
    private LinearProgressIndicator progress2;
    private LinearProgressIndicator progress1;

    private TextView tvCount5;
    private TextView tvCount4;
    private TextView tvCount3;
    private TextView tvCount2;
    private TextView tvCount1;

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

        tvTotalFilms = view.findViewById(R.id.tv_stat_total_films);
        tvWatchTime = view.findViewById(R.id.tv_stat_watch_time);
        tvAvgRating = view.findViewById(R.id.tv_stat_avg_rating);

        progress5 = view.findViewById(R.id.progress_rating_5);
        progress4 = view.findViewById(R.id.progress_rating_4);
        progress3 = view.findViewById(R.id.progress_rating_3);
        progress2 = view.findViewById(R.id.progress_rating_2);
        progress1 = view.findViewById(R.id.progress_rating_1);

        tvCount5 = view.findViewById(R.id.tv_count_rating_5);
        tvCount4 = view.findViewById(R.id.tv_count_rating_4);
        tvCount3 = view.findViewById(R.id.tv_count_rating_3);
        tvCount2 = view.findViewById(R.id.tv_count_rating_2);
        tvCount1 = view.findViewById(R.id.tv_count_rating_1);

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getProfileStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                tvTotalFilms.setText(String.valueOf(stats.totalFilms));
                tvWatchTime.setText(stats.getFormattedWatchTime());
                tvAvgRating.setText(stats.getFormattedAvgRating());

                int maxCount = Math.max(1, stats.totalFilms);
                Map<Integer, Integer> counts = stats.ratingCounts;

                updateRatingRow(progress5, tvCount5, counts.get(5), maxCount);
                updateRatingRow(progress4, tvCount4, counts.get(4), maxCount);
                updateRatingRow(progress3, tvCount3, counts.get(3), maxCount);
                updateRatingRow(progress2, tvCount2, counts.get(2), maxCount);
                updateRatingRow(progress1, tvCount1, counts.get(1), maxCount);
            }
        });
    }

    private void updateRatingRow(LinearProgressIndicator progress, TextView tvCount, Integer countObj, int maxCount) {
        int count = countObj != null ? countObj : 0;
        tvCount.setText(String.valueOf(count));
        int percentage = Math.round(((float) count / maxCount) * 100);
        progress.setProgress(percentage);
    }
}

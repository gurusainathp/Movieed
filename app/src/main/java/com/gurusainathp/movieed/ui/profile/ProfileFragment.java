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

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getProfileStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                tvTotalFilms.setText(String.valueOf(stats.totalFilms));
                tvWatchTime.setText(stats.getFormattedWatchTime());

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

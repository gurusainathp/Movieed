package com.gurusainathp.movieed.ui.diary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;

public class DiaryFragment extends Fragment {

    private DiaryViewModel viewModel;
    private DiaryAdapter adapter;
    private RecyclerView rvLogs;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DiaryViewModel.class);

        rvLogs = view.findViewById(R.id.rv_diary_logs);
        tvEmpty = view.findViewById(R.id.tv_diary_empty);

        adapter = new DiaryAdapter(item -> {
            if (item != null && item.movie != null) {
                MovieDetailsActivity.launch(
                        requireContext(),
                        item.movie.getTmdbId(),
                        item.movie.getTitle(),
                        item.movie.getPosterPath()
                );
            }
        });
        rvLogs.setAdapter(adapter);

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getDiaryLogs().observe(getViewLifecycleOwner(), logs -> {
            adapter.setItems(logs);
            if (logs != null && !logs.isEmpty()) {
                tvEmpty.setVisibility(View.GONE);
                rvLogs.setVisibility(View.VISIBLE);
            } else {
                rvLogs.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}

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
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.ui.movie.MovieDetailsActivity;

import java.util.ArrayList;
import java.util.List;

public class DiaryTabFragment extends Fragment {

    public static final int TAB_WATCHED = 0;
    public static final int TAB_WATCHLIST = 1;
    private static final String ARG_TAB_TYPE = "arg_tab_type";

    private int tabType = TAB_WATCHED;
    private DiaryViewModel viewModel;
    private DiaryAdapter adapter;
    private RecyclerView rvGrid;
    private TextView tvEmpty;

    public static DiaryTabFragment newInstance(int tabType) {
        DiaryTabFragment fragment = new DiaryTabFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_TYPE, tabType);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            tabType = getArguments().getInt(ARG_TAB_TYPE, TAB_WATCHED);
        }

        viewModel = new ViewModelProvider(requireParentFragment()).get(DiaryViewModel.class);

        rvGrid = view.findViewById(R.id.rv_tab_grid);
        tvEmpty = view.findViewById(R.id.tv_tab_empty);

        if (tabType == TAB_WATCHED) {
            tvEmpty.setText("No watched films logged yet.");
        } else {
            tvEmpty.setText("Your watchlist is empty.");
        }

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
        rvGrid.setAdapter(adapter);

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getDiaryLogs().observe(getViewLifecycleOwner(), logs -> {
            List<MovieLogWithDetails> filteredList = new ArrayList<>();
            if (logs != null) {
                for (MovieLogWithDetails item : logs) {
                    if (tabType == TAB_WATCHED) {
                        if (item.log.getReaction() != null && !item.log.getReaction().isEmpty()) {
                            filteredList.add(item);
                        }
                    } else {
                        if (item.log.isWatchlist()) {
                            filteredList.add(item);
                        }
                    }
                }
            }

            adapter.setItems(filteredList);
            if (!filteredList.isEmpty()) {
                tvEmpty.setVisibility(View.GONE);
                rvGrid.setVisibility(View.VISIBLE);
            } else {
                rvGrid.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}

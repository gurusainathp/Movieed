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
    private View cardFilterBar;

    private TextView btnFilterAll;
    private TextView btnFilterLove;
    private TextView btnFilterLike;
    private TextView btnFilterUmm;
    private TextView btnFilterDislike;
    private TextView btnFilterHate;

    private String selectedReactionFilter = "ALL";
    private List<MovieLogWithDetails> currentAllLogs = new ArrayList<>();

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
        cardFilterBar = view.findViewById(R.id.card_filter_bar);

        if (tabType == TAB_WATCHED) {
            tvEmpty.setText("No watched films logged yet.");
            cardFilterBar.setVisibility(View.VISIBLE);

            btnFilterAll = view.findViewById(R.id.btn_filter_all);
            btnFilterLove = view.findViewById(R.id.btn_filter_love);
            btnFilterLike = view.findViewById(R.id.btn_filter_like);
            btnFilterUmm = view.findViewById(R.id.btn_filter_umm);
            btnFilterDislike = view.findViewById(R.id.btn_filter_dislike);
            btnFilterHate = view.findViewById(R.id.btn_filter_hate);

            btnFilterAll.setOnClickListener(v -> setFilter("ALL"));
            btnFilterLove.setOnClickListener(v -> setFilter("LOVE"));
            btnFilterLike.setOnClickListener(v -> setFilter("LIKE"));
            btnFilterUmm.setOnClickListener(v -> setFilter("UMM"));
            btnFilterDislike.setOnClickListener(v -> setFilter("DISLIKE"));
            btnFilterHate.setOnClickListener(v -> setFilter("HATE"));

            updateFilterUI();
        } else {
            tvEmpty.setText("Your watchlist is empty.");
            cardFilterBar.setVisibility(View.GONE);
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

    private void setFilter(String filter) {
        selectedReactionFilter = filter;
        updateFilterUI();
        applyFilterAndRender();
    }

    private void updateFilterUI() {
        if (btnFilterAll == null) return;

        highlightButton(btnFilterAll, "ALL".equals(selectedReactionFilter));
        highlightButton(btnFilterLove, "LOVE".equals(selectedReactionFilter));
        highlightButton(btnFilterLike, "LIKE".equals(selectedReactionFilter));
        highlightButton(btnFilterUmm, "UMM".equals(selectedReactionFilter));
        highlightButton(btnFilterDislike, "DISLIKE".equals(selectedReactionFilter));
        highlightButton(btnFilterHate, "HATE".equals(selectedReactionFilter));
    }

    private void highlightButton(TextView btn, boolean isSelected) {
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_selected_filter);
        } else {
            btn.setBackgroundResource(R.drawable.bg_unselected_filter);
        }
    }

    private void observeViewModel() {
        viewModel.getDiaryLogs().observe(getViewLifecycleOwner(), logs -> {
            currentAllLogs = logs != null ? logs : new ArrayList<>();
            applyFilterAndRender();
        });
    }

    private void applyFilterAndRender() {
        List<MovieLogWithDetails> filteredList = new ArrayList<>();
        for (MovieLogWithDetails item : currentAllLogs) {
            if (tabType == TAB_WATCHED) {
                String r = item.log.getReaction();
                if (r != null && !r.isEmpty()) {
                    if ("ALL".equals(selectedReactionFilter) || selectedReactionFilter.equals(r)) {
                        filteredList.add(item);
                    }
                }
            } else {
                if (item.log.isWatchlist()) {
                    filteredList.add(item);
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
    }
}

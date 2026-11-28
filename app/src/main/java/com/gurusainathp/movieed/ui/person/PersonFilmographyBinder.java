package com.gurusainathp.movieed.ui.person;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.ui.person.Filmography.Filter;
import com.gurusainathp.movieed.ui.person.Filmography.MediaFilter;
import com.gurusainathp.movieed.ui.person.Filmography.Options;
import com.gurusainathp.movieed.ui.person.Filmography.Role;
import com.gurusainathp.movieed.ui.person.Filmography.Sort;
import com.gurusainathp.movieed.ui.person.Filmography.State;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the filmography section of the person page: the filter chips (one compact row),
 * the list (10 at a time with "Show more"), the stats row and the diary progress.
 * <p>
 * new PersonFilmographyBinder(this, findViewById(R.id.scroll_person), personViewModel,
 * credit -> openDetails(credit));
 */
public class PersonFilmographyBinder {

    private static final int INITIAL_VISIBLE = 10;
    private static final int PAGE_SIZE = 20;
    private static final float DIVIDER_INSET_DP = 78f;   // poster 64dp + 14dp gap

    private final Context context;
    private final PersonViewModel viewModel;
    private final PersonFilmographyAdapter adapter;

    // Section + list
    private final View section;
    private final TextView tvCount;
    private final RecyclerView rvFilmography;
    private final TextView tvEmpty;
    private final MaterialButton btnShowMore;

    // Filter chips (dropdowns open a menu, toggles switch on/off)
    private final Chip chipRole;
    private final Chip chipType;
    private final Chip chipSort;
    private final Chip chipDecade;
    private final Chip chipHideSelf;
    private final Chip chipNotLogged;
    private final TextView tvReset;

    // Header stats + progress
    private final TextView tvStatMovies;
    private final TextView tvStatSeries;
    private final TextView tvStatSince;
    private final View progressBlock;
    private final TextView tvWatched;
    private final LinearProgressIndicator progressWatched;

    private State lastState;
    private Filter lastFilter;
    private int visibleLimit = INITIAL_VISIBLE;

    public PersonFilmographyBinder(@NonNull LifecycleOwner owner,
                                   @NonNull View root,
                                   @NonNull PersonViewModel viewModel,
                                   @NonNull PersonFilmographyAdapter.OnItemClickListener clickListener) {
        this.context = root.getContext();
        this.viewModel = viewModel;

        section = root.findViewById(R.id.section_filmography);
        tvCount = root.findViewById(R.id.tv_filmography_count);
        rvFilmography = root.findViewById(R.id.rv_person_filmography);
        tvEmpty = root.findViewById(R.id.tv_filmography_empty);
        btnShowMore = root.findViewById(R.id.btn_show_all_filmography);

        chipRole = root.findViewById(R.id.chip_role);
        chipType = root.findViewById(R.id.chip_type);
        chipSort = root.findViewById(R.id.chip_sort);
        chipDecade = root.findViewById(R.id.chip_decade);
        chipHideSelf = root.findViewById(R.id.chip_hide_self);
        chipNotLogged = root.findViewById(R.id.chip_not_logged);
        tvReset = root.findViewById(R.id.tv_filters_reset);

        tvStatMovies = root.findViewById(R.id.tv_stat_movies);
        tvStatSeries = root.findViewById(R.id.tv_stat_series);
        tvStatSince = root.findViewById(R.id.tv_stat_active_since);
        progressBlock = root.findViewById(R.id.card_person_watch_progress);
        tvWatched = root.findViewById(R.id.tv_person_watched);
        progressWatched = root.findViewById(R.id.progress_person_watched);

        adapter = new PersonFilmographyAdapter(clickListener);
        rvFilmography.setLayoutManager(new LinearLayoutManager(context));
        rvFilmography.addItemDecoration(
                new ListDividerDecoration(context, R.color.movieed_border, DIVIDER_INSET_DP));
        rvFilmography.setAdapter(adapter);

        setupListeners();
        viewModel.getFilmographyState().observe(owner, this::render);
    }

    // ---- Listeners ----------------------------------------------------------------------

    private void setupListeners() {
        bindDropdown(chipRole, this::showRoleMenu);
        bindDropdown(chipType, this::showTypeMenu);
        bindDropdown(chipSort, this::showSortMenu);
        bindDropdown(chipDecade, this::showDecadeMenu);

        chipHideSelf.setOnClickListener(v -> viewModel.setHideSelf(chipHideSelf.isChecked()));
        chipNotLogged.setOnClickListener(v -> viewModel.setNotLoggedOnly(chipNotLogged.isChecked()));

        tvReset.setOnClickListener(v -> viewModel.resetFilters());

        btnShowMore.setOnClickListener(v -> {
            visibleLimit += PAGE_SIZE;
            submitList();
        });
    }

    /**
     * The chevron is the chip's close icon, so it needs the same click handler as the chip body.
     */
    private void bindDropdown(Chip chip, Runnable open) {
        chip.setOnClickListener(v -> open.run());
        chip.setOnCloseIconClickListener(v -> open.run());
    }

    // ---- Dropdown menus -------------------------------------------------------------------

    private void showRoleMenu() {
        if (lastState == null) return;
        Options o = lastState.options;
        Role selected = lastState.filter.role;

        List<FilterDropdown.Option> options = new ArrayList<>();
        options.add(new FilterDropdown.Option(0, "All roles", String.valueOf(o.allRoleCount),
                selected == null, false));
        for (Role r : o.rolesPresent) {
            int count = o.countFor(r);
            options.add(new FilterDropdown.Option(r.ordinal() + 1, r.label, String.valueOf(count),
                    r == selected, count == 0));
        }
        FilterDropdown.show(chipRole, options,
                id -> viewModel.setRole(id == 0 ? null : Role.values()[id - 1]));
    }

    private void showTypeMenu() {
        if (lastState == null) return;
        Options o = lastState.options;
        MediaFilter selected = lastState.filter.media;

        List<FilterDropdown.Option> options = new ArrayList<>();
        options.add(new FilterDropdown.Option(MediaFilter.ALL.ordinal(), "All types",
                String.valueOf(o.allMediaCount), selected == MediaFilter.ALL, false));
        options.add(new FilterDropdown.Option(MediaFilter.MOVIES.ordinal(), "Movies",
                String.valueOf(o.movieCount), selected == MediaFilter.MOVIES, o.movieCount == 0));
        options.add(new FilterDropdown.Option(MediaFilter.SERIES.ordinal(), "Series",
                String.valueOf(o.seriesCount), selected == MediaFilter.SERIES, o.seriesCount == 0));
        FilterDropdown.show(chipType, options,
                id -> viewModel.setMedia(MediaFilter.values()[id]));
    }

    private void showSortMenu() {
        if (lastState == null) return;
        Sort selected = lastState.filter.sort;

        List<FilterDropdown.Option> options = new ArrayList<>();
        for (Sort s : Sort.values()) {
            options.add(new FilterDropdown.Option(s.ordinal(), s.label, null, s == selected, false));
        }
        FilterDropdown.show(chipSort, options, id -> viewModel.setSort(Sort.values()[id]));
    }

    private void showDecadeMenu() {
        if (lastState == null) return;
        Options o = lastState.options;
        int selected = lastState.filter.decade;

        List<FilterDropdown.Option> options = new ArrayList<>();
        options.add(new FilterDropdown.Option(0, "Any decade", String.valueOf(o.allDecadeCount),
                selected == 0, false));
        for (int decade : o.decades) {
            int count = o.countForDecade(decade);
            options.add(new FilterDropdown.Option(decade, decade + "s", String.valueOf(count),
                    decade == selected, count == 0));
        }
        FilterDropdown.show(chipDecade, options, id -> viewModel.setDecade(id));
    }

    // ---- Rendering --------------------------------------------------------------------------

    private void render(@Nullable State s) {
        if (s == null) return;
        lastState = s;

        bindStats(s);

        if (s.totalCredits == 0) {
            section.setVisibility(View.GONE);
            return;
        }
        section.setVisibility(View.VISIBLE);

        Filter f = s.filter;
        Filter def = s.defaultFilter;
        Options o = s.options;

        // A different filter means a different list: start from the top again
        if (!f.equals(lastFilter)) {
            visibleLimit = INITIAL_VISIBLE;
            lastFilter = f;
        }

        // Role: only when the person has more than one role
        chipRole.setVisibility(o.rolesPresent.size() >= 2 ? View.VISIBLE : View.GONE);
        chipRole.setText(f.role == null ? "All roles" : f.role.label);
        chipRole.setActivated(f.role != def.role);

        // Type: only when the person has both movies and series (or one is already selected)
        boolean showType = (o.movieCount > 0 && o.seriesCount > 0) || f.media != MediaFilter.ALL;
        chipType.setVisibility(showType ? View.VISIBLE : View.GONE);
        chipType.setText(typeLabel(f.media));
        chipType.setActivated(f.media != MediaFilter.ALL);

        chipSort.setText(f.sort.label);
        chipSort.setActivated(f.sort != def.sort);

        boolean showDecade = o.decades.size() >= 2 || f.decade != 0;
        chipDecade.setVisibility(showDecade ? View.VISIBLE : View.GONE);
        chipDecade.setText(f.decade == 0 ? "Any decade" : f.decade + "s");
        chipDecade.setActivated(f.decade != 0);

        chipHideSelf.setVisibility(o.selfCount > 0 ? View.VISIBLE : View.GONE);
        chipHideSelf.setChecked(f.hideSelf);

        chipNotLogged.setVisibility(o.hasLoggedData ? View.VISIBLE : View.GONE);
        chipNotLogged.setChecked(f.notLoggedOnly);

        tvReset.setVisibility(s.isDefaultFilter() ? View.GONE : View.VISIBLE);

        int n = s.items.size();
        tvCount.setText(n + (n == 1 ? " credit" : " credits"));

        submitList();
    }

    private static String typeLabel(MediaFilter m) {
        switch (m) {
            case MOVIES:
                return "Movies";
            case SERIES:
                return "Series";
            default:
                return "All types";
        }
    }

    private void submitList() {
        if (lastState == null) return;
        List<Filmography.Item> all = lastState.items;
        int shown = Math.min(visibleLimit, all.size());
        adapter.submitList(new ArrayList<>(all.subList(0, shown)));

        boolean empty = all.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvFilmography.setVisibility(empty ? View.GONE : View.VISIBLE);

        int remaining = all.size() - shown;
        if (remaining > 0) {
            btnShowMore.setVisibility(View.VISIBLE);
            btnShowMore.setText("Show " + Math.min(PAGE_SIZE, remaining) + " more · " + remaining + " left");
        } else {
            btnShowMore.setVisibility(View.GONE);
        }
    }

    private void bindStats(State s) {
        Filmography.Stats stats = s.stats;
        tvStatMovies.setText(String.valueOf(stats.movies));
        tvStatSeries.setText(String.valueOf(stats.series));
        tvStatSince.setText(stats.activeSince > 0 ? String.valueOf(stats.activeSince) : "–");

        if (stats.hasLoggedData && stats.coreCount > 0) {
            progressBlock.setVisibility(View.VISIBLE);
            tvWatched.setText(stats.loggedCount + " of " + stats.coreCount + " credits logged");
            progressWatched.setMax(stats.coreCount);
            progressWatched.setProgressCompat(stats.loggedCount, true);
        } else {
            progressBlock.setVisibility(View.GONE);
        }
    }
}
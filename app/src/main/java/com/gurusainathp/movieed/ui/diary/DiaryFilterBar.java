package com.gurusainathp.movieed.ui.diary;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.chip.Chip;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.ui.common.FilterDropdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The row of filter chips at the top of a diary tab: Provider, Reaction (Watched only), Type and Sort, plus Reset.
 */
public final class DiaryFilterBar {

    private static final String[] REACTION_KEYS = {"love", "like", "umm", "dislike", "hate"};

    public static final List<Provider> DEFAULT_PROVIDERS = Arrays.asList(
            new Provider(122, "JioHotstar"),
            new Provider(8, "Netflix"),
            new Provider(9, "Prime Video"),
            new Provider(237, "SonyLIV"),
            new Provider(232, "ZEE5"),
            new Provider(134, "MX Player"),
            new Provider(2, "Apple TV+"),
            new Provider(192, "YouTube Premium"),
            new Provider(283, "Crunchyroll"),
            new Provider(11, "MUBI"),
            new Provider(569, "Lionsgate+"),
            new Provider(531, "Paramount+"),
            new Provider(538, "Plex"),
            new Provider(73, "Tubi")
    );

    public enum Media {
        ALL("All types"), MOVIES("Movies"), SERIES("Series");

        public final String label;

        Media(String label) {
            this.label = label;
        }
    }

    public enum Sort {
        RECENT("Recent", "Recently added"),
        NEWEST("Newest", "Newest"),
        OLDEST("Oldest", "Oldest first"),
        TITLE_AZ("A–Z", "A–Z"),
        TITLE_ZA("Z–A", "Z–A");

        public final String chipLabel;
        public final String menuLabel;

        Sort(String chipLabel, String menuLabel) {
            this.chipLabel = chipLabel;
            this.menuLabel = menuLabel;
        }
    }

    public static final class Provider {
        public final int id;
        @NonNull
        public final String name;

        public Provider(int id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static final class State {
        public static final int ALL_PROVIDERS = -1;

        public final int providerId;
        @Nullable
        public final String reaction;
        @NonNull
        public final Media media;
        @NonNull
        public final Sort sort;

        public State(int providerId, @Nullable String reaction, @NonNull Media media, @NonNull Sort sort) {
            this.providerId = providerId;
            this.reaction = reaction;
            this.media = media;
            this.sort = sort;
        }

        public static State defaults() {
            return new State(ALL_PROVIDERS, null, Media.ALL, Sort.RECENT);
        }

        public boolean isDefault() {
            return equals(defaults());
        }

        State withProvider(int v) {
            return new State(v, reaction, media, sort);
        }

        State withReaction(@Nullable String v) {
            return new State(providerId, v, media, sort);
        }

        State withMedia(@NonNull Media v) {
            return new State(providerId, reaction, v, sort);
        }

        State withSort(@NonNull Sort v) {
            return new State(providerId, reaction, media, v);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            State s = (State) o;
            return providerId == s.providerId && Objects.equals(reaction, s.reaction)
                    && media == s.media && sort == s.sort;
        }

        @Override
        public int hashCode() {
            return Objects.hash(providerId, reaction, media, sort);
        }
    }

    public interface Listener {
        void onFilterChanged(@NonNull State state);
    }

    private final Listener listener;
    private final Chip chipProvider;
    private final Chip chipReaction;
    private final Chip chipType;
    private final Chip chipSort;
    private final TextView tvReset;

    private List<Provider> providers = new ArrayList<>(DEFAULT_PROVIDERS);
    private State state = State.defaults();

    public DiaryFilterBar(@NonNull View root, boolean showReactions, @NonNull Listener listener) {
        this.listener = listener;

        chipProvider = root.findViewById(R.id.chip_provider);
        chipReaction = root.findViewById(R.id.chip_reaction);
        chipType = root.findViewById(R.id.chip_type);
        chipSort = root.findViewById(R.id.chip_sort);
        tvReset = root.findViewById(R.id.tv_filters_reset);

        chipReaction.setVisibility(showReactions ? View.VISIBLE : View.GONE);

        bindDropdown(chipProvider, this::showProviderMenu);
        bindDropdown(chipReaction, this::showReactionMenu);
        bindDropdown(chipType, this::showTypeMenu);
        bindDropdown(chipSort, this::showSortMenu);
        tvReset.setOnClickListener(v -> update(State.defaults()));

        render();
    }

    @NonNull
    public State getState() {
        return state;
    }

    public void setProviders(@Nullable List<Provider> newProviders) {
        if (newProviders != null && !newProviders.isEmpty()) {
            List<Provider> merged = new ArrayList<>(DEFAULT_PROVIDERS);
            for (Provider p : newProviders) {
                boolean exists = false;
                for (Provider d : merged) {
                    if (d.id == p.id || d.name.equalsIgnoreCase(p.name)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) merged.add(p);
            }
            this.providers = merged;
        }
        if (state.providerId != State.ALL_PROVIDERS && findProvider(state.providerId) == null) {
            update(state.withProvider(State.ALL_PROVIDERS));
        } else {
            render();
        }
    }

    public void setState(@NonNull State newState) {
        state = newState;
        render();
    }

    private void showProviderMenu() {
        List<FilterDropdown.Option> options = new ArrayList<>();
        options.add(new FilterDropdown.Option(State.ALL_PROVIDERS, "All services", null,
                state.providerId == State.ALL_PROVIDERS, false));
        for (Provider p : providers) {
            options.add(new FilterDropdown.Option(p.id, p.name, null, p.id == state.providerId, false));
        }
        FilterDropdown.show(chipProvider, options, id -> update(state.withProvider(id)));
    }

    private void showReactionMenu() {
        List<FilterDropdown.Option> options = new ArrayList<>();
        options.add(new FilterDropdown.Option(0, "Any reaction", null, state.reaction == null, false));
        for (int i = 0; i < REACTION_KEYS.length; i++) {
            String key = REACTION_KEYS[i];
            options.add(new FilterDropdown.Option(i + 1, reactionLabel(key), null,
                    key.equals(state.reaction), false));
        }
        FilterDropdown.show(chipReaction, options,
                id -> update(state.withReaction(id == 0 ? null : REACTION_KEYS[id - 1])));
    }

    private void showTypeMenu() {
        List<FilterDropdown.Option> options = new ArrayList<>();
        for (Media m : Media.values()) {
            options.add(new FilterDropdown.Option(m.ordinal(), m.label, null, m == state.media, false));
        }
        FilterDropdown.show(chipType, options, id -> update(state.withMedia(Media.values()[id])));
    }

    private void showSortMenu() {
        List<FilterDropdown.Option> options = new ArrayList<>();
        for (Sort s : Sort.values()) {
            options.add(new FilterDropdown.Option(s.ordinal(), s.menuLabel, null, s == state.sort, false));
        }
        FilterDropdown.show(chipSort, options, id -> update(state.withSort(Sort.values()[id])));
    }

    private void update(@NonNull State newState) {
        if (newState.equals(state)) return;
        state = newState;
        render();
        listener.onFilterChanged(state);
    }

    private void render() {
        chipProvider.setVisibility(View.VISIBLE);
        Provider selected = findProvider(state.providerId);
        chipProvider.setText(selected != null ? selected.name : "All services");
        chipProvider.setActivated(selected != null);

        chipReaction.setText(state.reaction == null ? "Any reaction" : reactionLabel(state.reaction));
        chipReaction.setActivated(state.reaction != null);

        chipType.setText(state.media.label);
        chipType.setActivated(state.media != Media.ALL);

        chipSort.setText(state.sort.chipLabel);
        chipSort.setActivated(state.sort != Sort.RECENT);

        tvReset.setVisibility(state.isDefault() ? View.GONE : View.VISIBLE);
    }

    @Nullable
    private Provider findProvider(int id) {
        if (id == State.ALL_PROVIDERS) return null;
        for (Provider p : providers) {
            if (p.id == id) return p;
        }
        return null;
    }

    private static String reactionLabel(@NonNull String key) {
        String emoji = MovieLogEntity.getEmojiForReaction(key);
        String name = key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
        return emoji == null || emoji.isEmpty() ? name : emoji + " " + name;
    }

    private static void bindDropdown(Chip chip, Runnable open) {
        chip.setOnClickListener(v -> open.run());
        chip.setOnCloseIconClickListener(v -> open.run());
    }
}

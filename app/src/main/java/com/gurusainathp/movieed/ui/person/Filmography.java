package com.gurusainathp.movieed.ui.person;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Everything about the person's filmography that is NOT Android UI:
 * models, filtering, sorting and the counts shown on the filter chips.
 * Plain Java, so it is easy to unit test.
 */
public final class Filmography {

    private Filmography() {
    }

    private static final int MIN_VOTES_FOR_TOP_RATED = 20;
    private static final Pattern SELF_PATTERN =
            Pattern.compile("^(self|himself|herself|themselves)\\b.*", Pattern.CASE_INSENSITIVE);

    // ---- Enums ---------------------------------------------------------------

    public enum Role {
        ACTING("Acting"),
        DIRECTING("Directing"),
        WRITING("Writing"),
        PRODUCING("Producing"),
        CREW("Crew");

        public final String label;

        Role(String label) {
            this.label = label;
        }
    }

    public enum MediaFilter {ALL, MOVIES, SERIES}

    public enum Sort {
        NEWEST("Newest"),
        OLDEST("Oldest"),
        TOP_RATED("Top rated"),
        POPULAR("Popular");

        public final String label;

        Sort(String label) {
            this.label = label;
        }
    }

    // ---- Helpers ---------------------------------------------------------------

    /**
     * Key used to match a credit with the user's diary ("movie:603", "tv:1396").
     */
    @NonNull
    public static String key(boolean isTv, int tmdbId) {
        return (isTv ? "tv:" : "movie:") + tmdbId;
    }

    public static int yearOf(@Nullable String date) {
        if (date == null || date.length() < 4) return 0;
        try {
            return Integer.parseInt(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isSelfCharacter(@Nullable String character) {
        if (character == null) return false;
        String c = character.trim();
        if (c.isEmpty()) return false;
        return SELF_PATTERN.matcher(c).matches() || c.toLowerCase().contains("archive footage");
    }

    @NonNull
    public static Role roleForDepartment(@Nullable String department) {
        if (department == null) return Role.CREW;
        switch (department.trim().toLowerCase()) {
            case "directing":
                return Role.DIRECTING;
            case "writing":
                return Role.WRITING;
            case "production":
                return Role.PRODUCING;
            default:
                return Role.CREW;
        }
    }

    private static int decadeOf(int year) {
        return year <= 0 ? 0 : (year / 10) * 10;
    }

    // ---- Item --------------------------------------------------------------------

    /**
     * One title in the filmography (cast and crew entries for the same title are merged).
     */
    public static final class Item {
        public final TmdbPersonCastCreditDto credit;   // original DTO, used for click handling
        public final String key;
        public final boolean isTv;
        public final String title;
        public final int year;                          // 0 = unknown / not announced
        public final String subtitle;                   // "Bruce Wayne · Producer"
        public final EnumSet<Role> roles;
        public final boolean selfOnly;                  // only appears as "Self" / archive footage
        public final double rating;
        public final int voteCount;
        public final double popularity;
        public final boolean logged;                    // in the user's diary

        public Item(TmdbPersonCastCreditDto credit, String key, boolean isTv, String title, int year,
                    String subtitle, EnumSet<Role> roles, boolean selfOnly,
                    double rating, int voteCount, double popularity, boolean logged) {
            this.credit = credit;
            this.key = key;
            this.isTv = isTv;
            this.title = title;
            this.year = year;
            this.subtitle = subtitle;
            this.roles = roles;
            this.selfOnly = selfOnly;
            this.rating = rating;
            this.voteCount = voteCount;
            this.popularity = popularity;
            this.logged = logged;
        }

        Item withLogged(boolean value) {
            if (value == logged) return this;
            return new Item(credit, key, isTv, title, year, subtitle, roles, selfOnly,
                    rating, voteCount, popularity, value);
        }
    }

    // ---- Filter ------------------------------------------------------------------

    public static final class Filter {
        @Nullable
        public final Role role;               // null = all roles
        @NonNull
        public final MediaFilter media;
        @NonNull
        public final Sort sort;
        public final int decade;                        // 0 = any decade, otherwise 1990, 2010, ...
        public final boolean hideSelf;
        public final boolean notLoggedOnly;

        public Filter(@Nullable Role role, @NonNull MediaFilter media, @NonNull Sort sort,
                      int decade, boolean hideSelf, boolean notLoggedOnly) {
            this.role = role;
            this.media = media;
            this.sort = sort;
            this.decade = decade;
            this.hideSelf = hideSelf;
            this.notLoggedOnly = notLoggedOnly;
        }

        public static Filter defaults(@Nullable Role role) {
            return new Filter(role, MediaFilter.ALL, Sort.NEWEST, 0, true, false);
        }

        public Filter withRole(@Nullable Role v) {
            return new Filter(v, media, sort, decade, hideSelf, notLoggedOnly);
        }

        public Filter withMedia(@NonNull MediaFilter v) {
            return new Filter(role, v, sort, decade, hideSelf, notLoggedOnly);
        }

        public Filter withSort(@NonNull Sort v) {
            return new Filter(role, media, v, decade, hideSelf, notLoggedOnly);
        }

        public Filter withDecade(int v) {
            return new Filter(role, media, sort, v, hideSelf, notLoggedOnly);
        }

        public Filter withHideSelf(boolean v) {
            return new Filter(role, media, sort, decade, v, notLoggedOnly);
        }

        public Filter withNotLoggedOnly(boolean v) {
            return new Filter(role, media, sort, decade, hideSelf, v);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Filter)) return false;
            Filter f = (Filter) o;
            return role == f.role && media == f.media && sort == f.sort && decade == f.decade
                    && hideSelf == f.hideSelf && notLoggedOnly == f.notLoggedOnly;
        }

        @Override
        public int hashCode() {
            return Objects.hash(role, media, sort, decade, hideSelf, notLoggedOnly);
        }
    }

    // ---- Options (what the chips show) -------------------------------------------------

    public static final class Options {
        /**
         * Roles to show as chips (in enum order). Always includes the selected role.
         */
        public final List<Role> rolesPresent;
        public final int allRoleCount;
        public final Map<Role, Integer> roleCounts;

        public final int allMediaCount;
        public final int movieCount;
        public final int seriesCount;

        public final List<Integer> decades;             // newest first
        public final Map<Integer, Integer> decadeCounts;
        public final int allDecadeCount;

        public final int selfCount;                     // credits that are only "Self" appearances
        public final boolean hasLoggedData;

        Options(List<Role> rolesPresent, int allRoleCount, Map<Role, Integer> roleCounts,
                int allMediaCount, int movieCount, int seriesCount,
                List<Integer> decades, Map<Integer, Integer> decadeCounts, int allDecadeCount,
                int selfCount, boolean hasLoggedData) {
            this.rolesPresent = rolesPresent;
            this.allRoleCount = allRoleCount;
            this.roleCounts = roleCounts;
            this.allMediaCount = allMediaCount;
            this.movieCount = movieCount;
            this.seriesCount = seriesCount;
            this.decades = decades;
            this.decadeCounts = decadeCounts;
            this.allDecadeCount = allDecadeCount;
            this.selfCount = selfCount;
            this.hasLoggedData = hasLoggedData;
        }

        public int countFor(@NonNull Role role) {
            Integer c = roleCounts.get(role);
            return c == null ? 0 : c;
        }

        public int countForDecade(int decade) {
            Integer c = decadeCounts.get(decade);
            return c == null ? 0 : c;
        }
    }

    // ---- Stats (header cards) ------------------------------------------------------------

    public static final class Stats {
        public final int movies;
        public final int series;
        public final int activeSince;                   // 0 = unknown
        public final int loggedCount;
        public final int coreCount;                     // released credits in the person's main role
        public final boolean hasLoggedData;

        Stats(int movies, int series, int activeSince, int loggedCount, int coreCount, boolean hasLoggedData) {
            this.movies = movies;
            this.series = series;
            this.activeSince = activeSince;
            this.loggedCount = loggedCount;
            this.coreCount = coreCount;
            this.hasLoggedData = hasLoggedData;
        }
    }

    // ---- State (one object for the whole filmography UI) ------------------------------------

    public static final class State {
        public final List<Item> items;                  // filtered + sorted
        public final Filter filter;
        public final Filter defaultFilter;
        public final Options options;
        public final Stats stats;
        public final int totalCredits;

        State(List<Item> items, Filter filter, Filter defaultFilter, Options options,
              Stats stats, int totalCredits) {
            this.items = items;
            this.filter = filter;
            this.defaultFilter = defaultFilter;
            this.options = options;
            this.stats = stats;
            this.totalCredits = totalCredits;
        }

        public boolean isDefaultFilter() {
            return filter.equals(defaultFilter);
        }
    }

    // ---- Compute --------------------------------------------------------------------------------

    /**
     * The role with the most (non-self) credits; becomes the initial filter.
     */
    @Nullable
    public static Role dominantRole(@NonNull List<Item> all) {
        Map<Role, Integer> counts = new EnumMap<>(Role.class);
        for (Item it : all) {
            if (it.selfOnly) continue;
            for (Role r : it.roles) {
                Integer c = counts.get(r);
                counts.put(r, c == null ? 1 : c + 1);
            }
        }
        Role best = null;
        int bestCount = 0;
        for (Role r : Role.values()) {            // enum order breaks ties (Acting first)
            Integer c = counts.get(r);
            if (c != null && c > bestCount) {
                best = r;
                bestCount = c;
            }
        }
        return best;
    }

    /**
     * Applies the filter and also computes "faceted" counts: the number next to each chip is
     * what you would get if you picked it, given the OTHER active filters.
     */
    @NonNull
    public static State compute(@NonNull List<Item> all, @NonNull Filter f, @NonNull Filter def,
                                @Nullable Set<String> loggedKeys) {
        boolean hasLogged = loggedKeys != null;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        List<Item> result = new ArrayList<>();
        EnumSet<Role> rolesInBase = EnumSet.noneOf(Role.class);
        Map<Role, Integer> roleCounts = new EnumMap<>(Role.class);
        TreeSet<Integer> decadesInBase = new TreeSet<>(Collections.reverseOrder());
        Map<Integer, Integer> decadeCounts = new HashMap<>();
        int allRole = 0, allMedia = 0, movies = 0, series = 0, allDecade = 0, selfCount = 0;

        int statMovies = 0, statSeries = 0, since = 0, core = 0, loggedCore = 0;

        for (Item raw : all) {
            Item it = raw.withLogged(hasLogged && loggedKeys.contains(raw.key));

            if (it.selfOnly) selfCount++;

            // stats are about real credits, independent of the current filter
            if (!it.selfOnly) {
                if (it.isTv) statSeries++;
                else statMovies++;
                if (it.year > 0 && (since == 0 || it.year < since)) since = it.year;
                boolean inMainRole = def.role == null || it.roles.contains(def.role);
                if (inMainRole && it.year > 0 && it.year <= currentYear) {
                    core++;
                    if (it.logged) loggedCore++;
                }
            }

            // "base" filters apply everywhere
            if (f.hideSelf && it.selfOnly) continue;
            if (f.notLoggedOnly && it.logged) continue;

            rolesInBase.addAll(it.roles);
            if (it.year > 0) decadesInBase.add(decadeOf(it.year));

            boolean roleOk = f.role == null || it.roles.contains(f.role);
            boolean mediaOk = f.media == MediaFilter.ALL
                    || (f.media == MediaFilter.MOVIES && !it.isTv)
                    || (f.media == MediaFilter.SERIES && it.isTv);
            boolean decadeOk = f.decade == 0 || decadeOf(it.year) == f.decade;

            if (roleOk && mediaOk && decadeOk) result.add(it);

            if (mediaOk && decadeOk) {
                allRole++;
                for (Role r : it.roles) {
                    Integer c = roleCounts.get(r);
                    roleCounts.put(r, c == null ? 1 : c + 1);
                }
            }
            if (roleOk && decadeOk) {
                allMedia++;
                if (it.isTv) series++;
                else movies++;
            }
            if (roleOk && mediaOk) {
                allDecade++;
                if (it.year > 0) {
                    int d = decadeOf(it.year);
                    Integer c = decadeCounts.get(d);
                    decadeCounts.put(d, c == null ? 1 : c + 1);
                }
            }
        }

        if (f.role != null) rolesInBase.add(f.role);       // keep the selected chip visible
        if (f.decade != 0) decadesInBase.add(f.decade);

        Collections.sort(result, comparator(f.sort));

        Options options = new Options(
                new ArrayList<>(rolesInBase), allRole, roleCounts,
                allMedia, movies, series,
                new ArrayList<>(decadesInBase), decadeCounts, allDecade,
                selfCount, hasLogged);
        Stats stats = new Stats(statMovies, statSeries, since, loggedCore, core, hasLogged);
        return new State(result, f, def, options, stats, all.size());
    }

    // ---- Sorting --------------------------------------------------------------------------------

    private static Comparator<Item> comparator(@NonNull Sort sort) {
        switch (sort) {
            case OLDEST:
                return (a, b) -> {
                    int c = compareYear(a.year, b.year, true);
                    return c != 0 ? c : Double.compare(b.popularity, a.popularity);
                };
            case TOP_RATED:
                return (a, b) -> {
                    double ra = a.voteCount >= MIN_VOTES_FOR_TOP_RATED ? a.rating : -1;
                    double rb = b.voteCount >= MIN_VOTES_FOR_TOP_RATED ? b.rating : -1;
                    int c = Double.compare(rb, ra);
                    return c != 0 ? c : Double.compare(b.popularity, a.popularity);
                };
            case POPULAR:
                return (a, b) -> Double.compare(b.popularity, a.popularity);
            case NEWEST:
            default:
                return (a, b) -> {
                    int c = compareYear(a.year, b.year, false);
                    return c != 0 ? c : Double.compare(b.popularity, a.popularity);
                };
        }
    }

    /**
     * Unknown years (0) always go last.
     */
    private static int compareYear(int a, int b, boolean ascending) {
        if (a == 0 && b == 0) return 0;
        if (a == 0) return 1;
        if (b == 0) return -1;
        return ascending ? Integer.compare(a, b) : Integer.compare(b, a);
    }
}
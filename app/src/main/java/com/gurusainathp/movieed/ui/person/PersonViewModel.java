package com.gurusainathp.movieed.ui.person;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbCombinedCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonDto;
import com.gurusainathp.movieed.ui.person.Filmography.Filter;
import com.gurusainathp.movieed.ui.person.Filmography.MediaFilter;
import com.gurusainathp.movieed.ui.person.Filmography.Role;
import com.gurusainathp.movieed.ui.person.Filmography.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonViewModel extends ViewModel {

    private final MutableLiveData<TmdbPersonDto> personDetails = new MutableLiveData<>();
    private final MutableLiveData<List<TmdbPersonCastCreditDto>> filmography = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * Everything the filmography UI needs (list + chip counts + stats), always consistent.
     */
    private final MutableLiveData<Filmography.State> filmographyState = new MutableLiveData<>();

    private List<Filmography.Item> allItems = Collections.emptyList();
    private Filter defaultFilter = Filter.defaults(null);
    private Filter filter = defaultFilter;
    @Nullable
    private Set<String> loggedKeys = null;
    private int loadedPersonId = -1;

    // ---- Observables -------------------------------------------------------------------

    public LiveData<TmdbPersonDto> getPersonDetails() {
        return personDetails;
    }

    /**
     * All credits (cast + crew merged, no duplicates), most popular first.
     */
    public LiveData<List<TmdbPersonCastCreditDto>> getFilmography() {
        return filmography;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Filmography.State> getFilmographyState() {
        return filmographyState;
    }

    // ---- Filter actions (each one re-publishes the state) ------------------------------------

    public void setRole(@Nullable Role role) {
        update(filter.withRole(role));
    }

    public void setMedia(@NonNull MediaFilter media) {
        update(filter.withMedia(media));
    }

    public void setSort(@NonNull Sort sort) {
        update(filter.withSort(sort));
    }

    /**
     * 0 = any decade, otherwise 1990, 2010, ...
     */
    public void setDecade(int decade) {
        update(filter.withDecade(decade));
    }

    public void setHideSelf(boolean hide) {
        update(filter.withHideSelf(hide));
    }

    public void setNotLoggedOnly(boolean value) {
        update(filter.withNotLoggedOnly(value));
    }

    public void resetFilters() {
        update(defaultFilter);
    }

    /**
     * Optional: pass the titles the user has logged, built with Filmography.key(isTv, tmdbId).
     * Enables the "Not logged yet" chip, the LOGGED badge and the progress card. Pass null to disable.
     */
    public void setLoggedKeys(@Nullable Set<String> keys) {
        loggedKeys = keys == null ? null : new HashSet<>(keys);
        if (!allItems.isEmpty()) {
            if (loggedKeys == null && filter.notLoggedOnly) {
                filter = filter.withNotLoggedOnly(false);
            }
            publish();
        }
    }

    private void update(@NonNull Filter newFilter) {
        if (newFilter.equals(filter)) return;
        filter = newFilter;
        publish();
    }

    private void publish() {
        filmographyState.setValue(Filmography.compute(allItems, filter, defaultFilter, loggedKeys));
    }

    // ---- Loading -------------------------------------------------------------------------------

    public void fetchPerson(int personId) {
        if (loadedPersonId == personId) return;     // already loaded (e.g. after rotation)
        loadedPersonId = personId;

        TmdbClient.getApiService().getPersonDetails(personId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbPersonDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbPersonDto> call, @NonNull Response<TmdbPersonDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            personDetails.setValue(response.body());
                        } else {
                            loadedPersonId = -1;
                            errorMessage.setValue("Failed to load person (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbPersonDto> call, @NonNull Throwable t) {
                        loadedPersonId = -1;
                        errorMessage.setValue("Error: " + t.getLocalizedMessage());
                    }
                });

        TmdbClient.getApiService().getCombinedCredits(personId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbCombinedCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbCombinedCreditsResponseDto> call,
                                           @NonNull Response<TmdbCombinedCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            onCreditsLoaded(response.body().getCast(), response.body().getCrew());
                        } else {
                            loadedPersonId = -1;
                            errorMessage.setValue("Failed to load filmography (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbCombinedCreditsResponseDto> call, @NonNull Throwable t) {
                        loadedPersonId = -1;
                        errorMessage.setValue("Couldn't load filmography. Check your connection.");
                    }
                });
    }

    private void onCreditsLoaded(@Nullable List<TmdbPersonCastCreditDto> cast,
                                 @Nullable List<TmdbPersonCastCreditDto> crew) {
        allItems = buildItems(cast, crew);

        // Start on the role the person is mostly known for (e.g. Directing for a director)
        defaultFilter = Filter.defaults(Filmography.dominantRole(allItems));
        filter = defaultFilter;

        List<Filmography.Item> byPopularity = new ArrayList<>(allItems);
        Collections.sort(byPopularity, (a, b) -> Double.compare(b.popularity, a.popularity));
        List<TmdbPersonCastCreditDto> credits = new ArrayList<>();
        for (Filmography.Item it : byPopularity) credits.add(it.credit);
        filmography.setValue(credits);

        publish();
    }

    // ---- Merging cast + crew ---------------------------------------------------------------------

    /**
     * The old code keyed credits by id only. TMDB movie ids and TV ids are separate number spaces,
     * so a movie and a series could overwrite each other, and a crew job on a title the person also
     * acted in was dropped. Here titles are keyed by type + id and their roles are combined.
     */
    private static final class Builder {
        final TmdbPersonCastCreditDto credit;
        final boolean isTv;
        final EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        final Set<String> characters = new LinkedHashSet<>();
        final Set<String> jobs = new LinkedHashSet<>();
        boolean hasCast = false;
        boolean hasCrew = false;
        boolean allCastSelf = true;

        Builder(TmdbPersonCastCreditDto credit, boolean isTv) {
            this.credit = credit;
            this.isTv = isTv;
        }
    }

    private List<Filmography.Item> buildItems(@Nullable List<TmdbPersonCastCreditDto> cast,
                                              @Nullable List<TmdbPersonCastCreditDto> crew) {
        Map<String, Builder> map = new LinkedHashMap<>();

        if (cast != null) {
            for (TmdbPersonCastCreditDto c : cast) {
                if (c == null) continue;
                Builder b = builderFor(map, c);
                b.hasCast = true;
                b.roles.add(Role.ACTING);
                String character = c.getCharacter();
                if (character != null && !character.trim().isEmpty()) {
                    b.characters.add(character.trim());
                }
                if (!Filmography.isSelfCharacter(character)) b.allCastSelf = false;
            }
        }
        if (crew != null) {
            for (TmdbPersonCastCreditDto c : crew) {
                if (c == null) continue;
                Builder b = builderFor(map, c);
                b.hasCrew = true;
                b.roles.add(Filmography.roleForDepartment(c.getDepartment()));
                String job = c.getJob();
                if (job != null && !job.trim().isEmpty()) b.jobs.add(job.trim());
            }
        }

        List<Filmography.Item> items = new ArrayList<>(map.size());
        for (Builder b : map.values()) {
            TmdbPersonCastCreditDto c = b.credit;
            String title = c.getTitle() != null ? c.getTitle() : "";

            List<String> parts = new ArrayList<>();
            if (!b.characters.isEmpty()) parts.add(join(b.characters));
            if (!b.jobs.isEmpty()) parts.add(join(b.jobs));

            boolean selfOnly = b.hasCast && b.allCastSelf && !b.hasCrew;

            items.add(new Filmography.Item(
                    c,
                    Filmography.key(b.isTv, c.getId()),
                    b.isTv,
                    title,
                    Filmography.yearOf(c.getReleaseDate()),
                    join(parts, " · "),
                    b.roles,
                    selfOnly,
                    c.getVoteAverage(),
                    c.getVoteCount(),
                    c.getPopularity(),
                    false));
        }
        return items;
    }

    private static Builder builderFor(Map<String, Builder> map, TmdbPersonCastCreditDto c) {
        boolean isTv = "tv".equalsIgnoreCase(c.getMediaType());
        String key = Filmography.key(isTv, c.getId());
        Builder b = map.get(key);
        if (b == null) {
            b = new Builder(c, isTv);
            map.put(key, b);
        }
        return b;
    }

    private static String join(Iterable<String> parts) {
        return join(parts, ", ");
    }

    private static String join(Iterable<String> parts, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(separator);
            sb.append(p);
        }
        return sb.toString();
    }
}
package com.gurusainathp.movieed.ui.search;

import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gurusainathp.movieed.data.remote.api.TmdbApiService;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.domain.model.Movie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Search for movies, series and people.
 * <p>
 * Backward compatible: getSearchResults(), getIsLoading(), getErrorMessage() and
 * searchMovies(query, apiKey) behave as before, so the existing Search page keeps working.
 * <p>
 * New for "search as you type" (used by the Home search):
 * - onQueryChanged(): debounced, ignores very short queries
 * - getState(): one clear UI state (IDLE / LOADING / RESULTS / EMPTY / ERROR)
 * - stale-response guard: an old response can never overwrite a newer search
 * - small in-memory cache, so backspacing to a previous query is instant
 * - clear() and retry()
 */
public class SearchViewModel extends ViewModel {

    public enum State {
        IDLE,      // nothing typed (or query too short)
        LOADING,   // searching and there is nothing to show yet
        RESULTS,   // list available
        EMPTY,     // searched, no matches
        ERROR      // request failed
    }

    private static final long DEBOUNCE_MS = 350L;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int CACHE_SIZE = 20;

    private final MutableLiveData<List<Movie>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LruCache<String, List<Movie>> cache = new LruCache<>(CACHE_SIZE);

    private Runnable pendingSearch;
    private Call<TmdbMovieResponseDto> currentSearchCall;
    private long requestId = 0;

    private String lastSearchedQuery = "";   // query that produced the current results
    private String lastRequestedQuery = "";  // for retry()
    private String lastApiKey = "";

    // ---- Observables -----------------------------------------------------

    public LiveData<List<Movie>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<State> getState() {
        return state;
    }

    /**
     * The query the current results belong to (handy for highlighting matches).
     */
    @NonNull
    public String getLastSearchedQuery() {
        return lastSearchedQuery;
    }

    // ---- Actions ---------------------------------------------------------

    /**
     * Search-as-you-type: call on every text change. Debounced.
     */
    public void onQueryChanged(@Nullable String query, @NonNull String apiKey) {
        String q = query == null ? "" : query.trim();
        cancelPending();

        if (q.length() < MIN_QUERY_LENGTH) {
            reset();
            return;
        }

        // Same query as what is already shown (e.g. only a trailing space was added)
        State current = state.getValue();
        if (q.equalsIgnoreCase(lastSearchedQuery)
                && (current == State.RESULTS || current == State.EMPTY)) {
            isLoading.setValue(false);
            return;
        }

        List<Movie> cached = cache.get(cacheKey(q));
        if (cached != null) {
            cancelCall();
            requestId++;
            applyResults(q, cached);
            return;
        }

        // Show activity right away, but wait for the user to pause typing before hitting the network
        isLoading.setValue(true);
        if (isEmpty(searchResults.getValue())) {
            state.setValue(State.LOADING);
        }
        pendingSearch = () -> performSearch(q, apiKey);
        handler.postDelayed(pendingSearch, DEBOUNCE_MS);
    }

    /**
     * Immediate search (search button / keyboard action). Same behavior as before.
     */
    public void searchMovies(String query, String apiKey) {
        String q = query == null ? "" : query.trim();
        cancelPending();

        if (q.isEmpty()) {
            reset();
            return;
        }

        List<Movie> cached = cache.get(cacheKey(q));
        if (cached != null) {
            cancelCall();
            requestId++;
            applyResults(q, cached);
            return;
        }
        performSearch(q, apiKey);
    }

    /**
     * Re-run the last search after an error.
     */
    public void retry() {
        if (lastRequestedQuery.isEmpty()) return;
        cancelPending();
        performSearch(lastRequestedQuery, lastApiKey);
    }

    /**
     * Forget everything (e.g. when the search UI closes).
     */
    public void clear() {
        cancelPending();
        reset();
    }

    // ---- Internals -------------------------------------------------------

    private void performSearch(@NonNull String q, @NonNull String apiKey) {
        pendingSearch = null;
        cancelCall();

        final long id = ++requestId;
        lastRequestedQuery = q;
        lastApiKey = apiKey;

        isLoading.setValue(true);
        errorMessage.setValue(null);
        if (isEmpty(searchResults.getValue())) {
            state.setValue(State.LOADING);
        }

        TmdbApiService apiService = TmdbClient.getApiService();
        currentSearchCall = apiService.searchMulti(apiKey, q, 1);
        currentSearchCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                if (id != requestId) return;   // a newer search has started, ignore this one
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> movies = mapResults(response.body().getResults());
                    cache.put(cacheKey(q), movies);
                    applyResults(q, movies);
                } else {
                    fail("Couldn't load results (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (call.isCanceled() || id != requestId) return;
                fail(t instanceof IOException
                        ? "No connection. Check your internet and try again."
                        : "Something went wrong. Please try again.");
            }
        });
    }

    private void applyResults(@NonNull String q, @NonNull List<Movie> movies) {
        lastSearchedQuery = q;
        isLoading.setValue(false);
        errorMessage.setValue(null);
        searchResults.setValue(movies);
        state.setValue(movies.isEmpty() ? State.EMPTY : State.RESULTS);
    }

    private void fail(@NonNull String message) {
        isLoading.setValue(false);
        errorMessage.setValue(message);   // set before state so observers can read it
        state.setValue(State.ERROR);
    }

    private void reset() {
        requestId++;           // invalidates anything still in flight
        cancelCall();
        lastSearchedQuery = "";
        isLoading.setValue(false);
        errorMessage.setValue(null);
        searchResults.setValue(new ArrayList<>());
        state.setValue(State.IDLE);
    }

    private void cancelPending() {
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
    }

    private void cancelCall() {
        if (currentSearchCall != null && !currentSearchCall.isCanceled()) {
            currentSearchCall.cancel();
        }
        currentSearchCall = null;
    }

    private static boolean isEmpty(@Nullable List<?> list) {
        return list == null || list.isEmpty();
    }

    private static String cacheKey(@NonNull String q) {
        return q.toLowerCase(Locale.ROOT);
    }

    // ---- Mapping (same output as before, with null/blank filtering) -------

    private List<Movie> mapResults(@Nullable List<TmdbMovieDto> dtos) {
        List<Movie> movies = new ArrayList<>();
        if (dtos == null) return movies;

        for (TmdbMovieDto dto : dtos) {
            if (dto == null) continue;
            String title = dto.getTitle();
            if (title == null || title.trim().isEmpty()) continue;

            String mediaType = dto.getMediaType();
            if ("person".equalsIgnoreCase(mediaType)) {
                movies.add(new Movie(
                        dto.getId(),
                        dto.getTitle(),
                        formatPersonOverview(dto),
                        formatYearsActive(dto),
                        dto.getProfilePath(),
                        null,
                        0.0,
                        "person"
                ));
            } else {
                movies.add(new Movie(
                        dto.getId(),
                        dto.getTitle(),
                        dto.getOverview(),
                        dto.getReleaseDate(),
                        dto.getPosterPath(),
                        dto.getBackdropPath(),
                        dto.getVoteAverage(),
                        mediaType != null ? mediaType : "movie"
                ));
            }
        }
        return movies;
    }

    private String formatPersonOverview(TmdbMovieDto dto) {
        StringBuilder sb = new StringBuilder();
        String dept = dto.getKnownForDepartment();
        if (dept != null && !dept.isEmpty()) {
            sb.append(formatDepartment(dept));
        } else {
            sb.append("Person");
        }

        if (dto.getKnownFor() != null && !dto.getKnownFor().isEmpty()) {
            sb.append(" · Known for ");
            int limit = Math.min(3, dto.getKnownFor().size());
            boolean first = true;
            for (int i = 0; i < limit; i++) {
                TmdbMovieDto m = dto.getKnownFor().get(i);
                if (m != null && m.getTitle() != null) {
                    if (!first) sb.append(", ");
                    sb.append(m.getTitle());
                    first = false;
                }
            }
        }
        return sb.toString();
    }

    private String formatYearsActive(TmdbMovieDto dto) {
        if (dto.getKnownFor() == null || dto.getKnownFor().isEmpty()) {
            return "";
        }
        int minYear = 9999;
        int maxYear = 0;
        for (TmdbMovieDto m : dto.getKnownFor()) {
            if (m != null && m.getReleaseDate() != null && m.getReleaseDate().length() >= 4) {
                try {
                    int y = Integer.parseInt(m.getReleaseDate().substring(0, 4));
                    if (y < minYear) minYear = y;
                    if (y > maxYear) maxYear = y;
                } catch (Exception ignored) {
                }
            }
        }
        if (minYear != 9999 && maxYear != 0) {
            if (minYear == maxYear) return String.valueOf(minYear);
            return minYear + " - " + maxYear;
        }
        return "";
    }

    private String formatDepartment(String department) {
        if (department == null) return "";
        switch (department.trim().toLowerCase(Locale.ROOT)) {
            case "acting":
                return "Actor";
            case "directing":
                return "Director";
            case "writing":
                return "Writer";
            case "production":
                return "Producer";
            case "editing":
                return "Editor";
            case "camera":
                return "Cinematographer";
            case "sound":
            case "music":
                return "Composer";
            default:
                return department;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelPending();
        cancelCall();
    }
}
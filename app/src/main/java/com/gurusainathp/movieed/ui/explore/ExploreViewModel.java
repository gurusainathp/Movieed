package com.gurusainathp.movieed.ui.explore;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbCastDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCombinedCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;
import com.gurusainathp.movieed.data.repository.MovieRepository;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;
import com.gurusainathp.movieed.domain.recommendation.RecommendationEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExploreViewModel extends AndroidViewModel {

    private final MovieRepository repository;
    private final RecommendationEngine recommendationEngine = new RecommendationEngine();
    private final MutableLiveData<List<RecommendedMovie>> exploreFeed = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Map<Long, MovieLogEntity>> logsMapLiveData = new MutableLiveData<>(new HashMap<>());

    private final Random random = new Random();
    private int currentTrendingPage = 1;
    private boolean isFetchingMore = false;
    private final Set<Integer> seenMovieIds = new HashSet<>();

    public ExploreViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
    }

    public LiveData<List<RecommendedMovie>> getExploreFeed() {
        return exploreFeed;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Map<Long, MovieLogEntity>> getLogsMap() {
        return logsMapLiveData;
    }

    public LiveData<List<MovieLogWithDetails>> getAllLogsWithDetails() {
        return repository.getAllLogsWithDetails();
    }

    public void updateLogsMap(List<MovieLogEntity> logs) {
        Map<Long, MovieLogEntity> map = new HashMap<>();
        if (logs != null) {
            for (MovieLogEntity log : logs) {
                map.put(log.getMovieId(), log);
            }
        }
        logsMapLiveData.setValue(map);
    }

    public void loadExploreMovies(List<MovieLogWithDetails> userLogs) {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        isLoading.setValue(true);
        currentTrendingPage = 1 + random.nextInt(3); // Random start page for freshness
        seenMovieIds.clear();

        List<RecommendedMovie> accumulated = new ArrayList<>();
        RecommendationEngine.UserTasteProfile profile = RecommendationEngine.UserTasteProfile.build(userLogs);

        List<MovieEntity> preferredMovies = new ArrayList<>(profile.lovedMovies);
        if (preferredMovies.isEmpty()) {
            preferredMovies.addAll(profile.likedMovies);
        }

        if (!preferredMovies.isEmpty()) {
            Collections.shuffle(preferredMovies);
            int branchCount = Math.min(2, preferredMovies.size());
            // Calls: branch movie 1 (similar), branch movie 2 (credits/director/actor), classics pool, trending pool
            AtomicInteger pendingCalls = new AtomicInteger(branchCount + 2);

            MovieEntity branchMovie1 = preferredMovies.get(0);
            fetchBranchRecommendations(branchMovie1, profile, accumulated, pendingCalls);

            if (branchCount > 1) {
                MovieEntity branchMovie2 = preferredMovies.get(1);
                fetchPersonCreditsRecommendations(branchMovie2, profile, accumulated, pendingCalls);
            } else {
                fetchPersonCreditsRecommendations(branchMovie1, profile, accumulated, pendingCalls);
            }

            fetchClassicsPool(profile, accumulated, pendingCalls);
            fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls);
        } else {
            // Cold start: fetch classics pool + trending pool
            AtomicInteger pendingCalls = new AtomicInteger(2);
            fetchClassicsPool(profile, accumulated, pendingCalls);
            fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls);
        }
    }

    private void fetchBranchRecommendations(MovieEntity movie,
                                            RecommendationEngine.UserTasteProfile profile,
                                            List<RecommendedMovie> accumulated,
                                            AtomicInteger pendingCalls) {
        TmdbClient.getApiService().getMovieRecommendations(movie.getTmdbId(), BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbMovieResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                           @NonNull Response<TmdbMovieResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                            List<TmdbMovieDto> results = response.body().getResults();
                            Collections.shuffle(results);
                            int added = 0;
                            for (TmdbMovieDto dto : results) {
                                if (dto == null || profile.loggedMovieIds.contains((long) dto.getId())) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                reasons.add("Because you loved " + movie.getTitle());
                                float score = (float) dto.getVoteAverage() + 2.5f + random.nextFloat();
                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(dto, score, reasons, movie.getPosterPath()));
                                }
                                if (++added >= 6) break;
                            }
                        }
                        checkAndDispatch(pendingCalls, accumulated);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                        checkAndDispatch(pendingCalls, accumulated);
                    }
                });
    }

    private void fetchPersonCreditsRecommendations(MovieEntity movie,
                                                   RecommendationEngine.UserTasteProfile profile,
                                                   List<RecommendedMovie> accumulated,
                                                   AtomicInteger pendingCalls) {
        TmdbClient.getApiService().getMovieCredits(movie.getTmdbId(), BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbCreditsResponseDto> call,
                                           @NonNull Response<TmdbCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            TmdbCreditsResponseDto credits = response.body();

                            // Find Director
                            TmdbCrewDto director = null;
                            if (credits.getCrew() != null) {
                                for (TmdbCrewDto crew : credits.getCrew()) {
                                    if (crew != null && "Director".equalsIgnoreCase(crew.getJob())) {
                                        director = crew;
                                        break;
                                    }
                                }
                            }

                            // Find Top Actor
                            TmdbCastDto topActor = null;
                            if (credits.getCast() != null && !credits.getCast().isEmpty()) {
                                topActor = credits.getCast().get(0);
                            }

                            if (director != null) {
                                fetchPersonFilmography(director.getId(), director.getName(), "Director",
                                        director.getProfilePath() != null ? director.getProfilePath() : movie.getPosterPath(),
                                        profile, accumulated, pendingCalls);
                            } else if (topActor != null) {
                                fetchPersonFilmography(topActor.getId(), topActor.getName(), "Actor",
                                        topActor.getProfilePath() != null ? topActor.getProfilePath() : movie.getPosterPath(),
                                        profile, accumulated, pendingCalls);
                            } else {
                                checkAndDispatch(pendingCalls, accumulated);
                            }
                        } else {
                            checkAndDispatch(pendingCalls, accumulated);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Throwable t) {
                        checkAndDispatch(pendingCalls, accumulated);
                    }
                });
    }

    private void fetchPersonFilmography(int personId, String personName, String role, String posterPath,
                                        RecommendationEngine.UserTasteProfile profile,
                                        List<RecommendedMovie> accumulated,
                                        AtomicInteger pendingCalls) {
        TmdbClient.getApiService().getCombinedCredits(personId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbCombinedCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbCombinedCreditsResponseDto> call,
                                           @NonNull Response<TmdbCombinedCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<TmdbPersonCastCreditDto> list = "Director".equalsIgnoreCase(role) ?
                                    response.body().getCrew() : response.body().getCast();

                            if (list != null && !list.isEmpty()) {
                                List<TmdbPersonCastCreditDto> candidates = new ArrayList<>(list);
                                Collections.sort(candidates, (a, b) -> Double.compare(b.getVoteAverage(), a.getVoteAverage()));

                                int count = 0;
                                for (TmdbPersonCastCreditDto item : candidates) {
                                    if (item == null || item.getTitle() == null) continue;
                                    if (profile.loggedMovieIds.contains((long) item.getId())) continue;
                                    synchronized (seenMovieIds) {
                                        if (seenMovieIds.contains(item.getId())) continue;
                                        seenMovieIds.add(item.getId());
                                    }

                                    TmdbMovieDto dto = new TmdbMovieDto(
                                            item.getId(),
                                            item.getTitle(),
                                            "",
                                            item.getReleaseDate(),
                                            item.getPosterPath(),
                                            item.getBackdropPath(),
                                            item.getVoteAverage(),
                                            item.getMediaType()
                                    );

                                    List<String> reasons = new ArrayList<>();
                                    if ("Director".equalsIgnoreCase(role)) {
                                        reasons.add("Directed by " + personName);
                                    } else {
                                        reasons.add("Starring " + personName);
                                    }

                                    float score = (float) item.getVoteAverage() + 2.0f + random.nextFloat();
                                    synchronized (accumulated) {
                                        accumulated.add(new RecommendedMovie(dto, score, reasons, posterPath));
                                    }

                                    if (++count >= 4) break;
                                }
                            }
                        }
                        checkAndDispatch(pendingCalls, accumulated);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbCombinedCreditsResponseDto> call, @NonNull Throwable t) {
                        checkAndDispatch(pendingCalls, accumulated);
                    }
                });
    }

    private void fetchClassicsPool(RecommendationEngine.UserTasteProfile profile,
                                  List<RecommendedMovie> accumulated,
                                  AtomicInteger pendingCalls) {
        int randomPage = 1 + random.nextInt(8); // Random page among all-time top rated
        TmdbClient.getApiService().getTopRatedMovies(BuildConfig.TMDB_API_KEY, randomPage)
                .enqueue(new Callback<TmdbMovieResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                           @NonNull Response<TmdbMovieResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                            List<TmdbMovieDto> results = response.body().getResults();
                            Collections.shuffle(results);
                            int count = 0;
                            for (TmdbMovieDto dto : results) {
                                if (dto == null || profile.loggedMovieIds.contains((long) dto.getId())) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                reasons.add("All-Time Classic (" + String.format(java.util.Locale.US, "%.1f★", dto.getVoteAverage()) + ")");
                                float score = (float) dto.getVoteAverage() + 1.2f + random.nextFloat();
                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(dto, score, reasons, dto.getPosterPath()));
                                }
                                if (++count >= 4) break;
                            }
                        }
                        checkAndDispatch(pendingCalls, accumulated);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                        checkAndDispatch(pendingCalls, accumulated);
                    }
                });
    }

    private void fetchTrendingPool(int page,
                                   RecommendationEngine.UserTasteProfile profile,
                                   List<RecommendedMovie> accumulated,
                                   AtomicInteger pendingCalls) {
        TmdbClient.getApiService().getPopularMovies(BuildConfig.TMDB_API_KEY, page)
                .enqueue(new Callback<TmdbMovieResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                           @NonNull Response<TmdbMovieResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                            List<TmdbMovieDto> results = response.body().getResults();
                            Collections.shuffle(results);
                            int count = 0;
                            for (TmdbMovieDto dto : results) {
                                if (dto == null || profile.loggedMovieIds.contains((long) dto.getId())) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                reasons.add("Trending discovery");
                                float score = (float) dto.getVoteAverage() + random.nextFloat();
                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(dto, score, reasons, dto.getPosterPath()));
                                }
                                if (++count >= 6) break;
                            }
                        }
                        checkAndDispatch(pendingCalls, accumulated);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                        checkAndDispatch(pendingCalls, accumulated);
                    }
                });
    }

    private void checkAndDispatch(AtomicInteger pendingCalls, List<RecommendedMovie> accumulated) {
        if (pendingCalls.decrementAndGet() == 0) {
            isLoading.setValue(false);
            // Sort by score with jitter, then lightly interleave for maximum variety
            Collections.sort(accumulated, (a, b) -> Float.compare(b.getScore(), a.getScore()));
            exploreFeed.setValue(new ArrayList<>(accumulated));
        }
    }

    public void loadNextPage(List<MovieLogWithDetails> userLogs) {
        if (isFetchingMore) return;
        isFetchingMore = true;
        currentTrendingPage++;

        RecommendationEngine.UserTasteProfile profile = RecommendationEngine.UserTasteProfile.build(userLogs);
        List<RecommendedMovie> currentList = exploreFeed.getValue() != null ? new ArrayList<>(exploreFeed.getValue()) : new ArrayList<>();

        TmdbClient.getApiService().getPopularMovies(BuildConfig.TMDB_API_KEY, currentTrendingPage)
                .enqueue(new Callback<TmdbMovieResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                           @NonNull Response<TmdbMovieResponseDto> response) {
                        isFetchingMore = false;
                        if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                            for (TmdbMovieDto dto : response.body().getResults()) {
                                if (dto == null || profile.loggedMovieIds.contains((long) dto.getId())) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                reasons.add("Trending discovery");
                                currentList.add(new RecommendedMovie(dto, (float) dto.getVoteAverage(), reasons, dto.getPosterPath()));
                            }
                            exploreFeed.setValue(currentList);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                        isFetchingMore = false;
                    }
                });
    }

    public void toggleWatchlist(TmdbMovieDto dto, MovieLogEntity existingLog) {
        if (dto == null) return;
        MovieEntity movieEntity = new MovieEntity(
                dto.getId(),
                dto.getTitle(),
                dto.getOverview(),
                dto.getReleaseDate(),
                dto.getPosterPath(),
                dto.getBackdropPath(),
                0,
                System.currentTimeMillis()
        );
        repository.toggleWatchlist(movieEntity, existingLog);
    }

    public void saveReaction(TmdbMovieDto dto, String reaction, MovieLogEntity existingLog) {
        if (dto == null) return;
        MovieEntity movieEntity = new MovieEntity(
                dto.getId(),
                dto.getTitle(),
                dto.getOverview(),
                dto.getReleaseDate(),
                dto.getPosterPath(),
                dto.getBackdropPath(),
                0,
                System.currentTimeMillis()
        );
        repository.saveReaction(movieEntity, reaction, existingLog);
    }
}

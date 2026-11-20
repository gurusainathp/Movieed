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
import com.gurusainathp.movieed.data.remote.dto.TmdbCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCombinedCreditsResponseDto;
import com.gurusainathp.movieed.data.repository.MovieRepository;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;
import com.gurusainathp.movieed.domain.recommendation.RecommendationEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        currentTrendingPage = 1;
        seenMovieIds.clear();

        List<RecommendedMovie> accumulated = new ArrayList<>();
        RecommendationEngine.UserTasteProfile profile = RecommendationEngine.UserTasteProfile.build(userLogs);

        if (!profile.lovedMovies.isEmpty()) {
            // Pick up to 2 loved movies to branch out recommendations and cast/director credits
            Collections.shuffle(profile.lovedMovies);
            int branchCount = Math.min(2, profile.lovedMovies.size());
            AtomicInteger pendingCalls = new AtomicInteger(branchCount + 1); // +1 for popular pool

            for (int i = 0; i < branchCount; i++) {
                MovieEntity lovedMovie = profile.lovedMovies.get(i);
                fetchBranchRecommendations(lovedMovie, profile, accumulated, pendingCalls);
            }

            fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls);
        } else {
            // Cold start: fetch popular movies
            AtomicInteger pendingCalls = new AtomicInteger(1);
            fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls);
        }
    }

    private void fetchBranchRecommendations(MovieEntity lovedMovie,
                                            RecommendationEngine.UserTasteProfile profile,
                                            List<RecommendedMovie> accumulated,
                                            AtomicInteger pendingCalls) {
        TmdbClient.getApiService().getMovieRecommendations(lovedMovie.getTmdbId(), BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbMovieResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                           @NonNull Response<TmdbMovieResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                            List<TmdbMovieDto> results = response.body().getResults();
                            for (TmdbMovieDto dto : results) {
                                if (dto == null || profile.loggedMovieIds.contains((long) dto.getId())) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                reasons.add("Because you loved " + lovedMovie.getTitle());
                                float score = (float) dto.getVoteAverage() + 2.0f;
                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(dto, score, reasons, lovedMovie.getPosterPath()));
                                }
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
                            for (TmdbMovieDto dto : results) {
                                if (dto == null || profile.loggedMovieIds.contains((long) dto.getId())) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                reasons.add("Trending & Popular");
                                float score = (float) dto.getVoteAverage();
                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(dto, score, reasons, dto.getPosterPath()));
                                }
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
            // Interleave recommendations so user gets a varied mix
            Collections.shuffle(accumulated);
            exploreFeed.setValue(accumulated);
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

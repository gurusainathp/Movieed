package com.gurusainathp.movieed.ui.explore;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbCombinedCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;
import com.gurusainathp.movieed.data.repository.MovieRepository;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;
import com.gurusainathp.movieed.domain.recommendation.FeedPlanner;
import com.gurusainathp.movieed.domain.recommendation.SeedSampler;
import com.gurusainathp.movieed.domain.recommendation.TasteProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExploreViewModel extends AndroidViewModel {

    private final MovieRepository repository;
    private final FeedPlanner feedPlanner;
    private final SeedSampler seedSampler;

    private final MutableLiveData<List<RecommendedMovie>> exploreFeed = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<ExploreViewState> viewState = new MutableLiveData<>(ExploreViewState.LOADING);
    private final MutableLiveData<Map<Long, MovieLogEntity>> logsMapLiveData = new MutableLiveData<>(new HashMap<>());

    private final Random random = new Random();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicLong currentRequestId = new AtomicLong(0);
    private final Set<Call<?>> inFlightCalls = Collections.synchronizedSet(new HashSet<>());

    private int currentTrendingPage = 1;
    private int currentDiscoverPage = 1;
    private boolean isFetchingMore = false;
    private final Set<Integer> seenMovieIds = new HashSet<>();
    private final Set<Integer> usedSeedIds = new HashSet<>();
    private final Map<MovieEntity, List<TmdbMovieDto>> activeSeedCandidatesMap = new HashMap<>();

    public ExploreViewModel(@NonNull Application application) {
        super(application);
        this.repository = new MovieRepository(application);
        this.feedPlanner = new FeedPlanner(random);
        this.seedSampler = new SeedSampler(random);
    }

    public LiveData<List<RecommendedMovie>> getExploreFeed() {
        return exploreFeed;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<ExploreViewState> getViewState() {
        return viewState;
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

    private void trackCall(Call<?> call) {
        if (call != null) {
            inFlightCalls.add(call);
        }
    }

    private void untrackCall(Call<?> call) {
        if (call != null) {
            inFlightCalls.remove(call);
        }
    }

    private void cancelInFlightCalls() {
        synchronized (inFlightCalls) {
            for (Call<?> call : inFlightCalls) {
                if (call != null && !call.isCanceled()) {
                    call.cancel();
                }
            }
            inFlightCalls.clear();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelInFlightCalls();
        mainHandler.removeCallbacksAndMessages(null);
    }

    public void loadExploreMovies(List<MovieLogWithDetails> userLogs) {
        loadExploreMovies(userLogs, false);
    }

    public void loadExploreMovies(List<MovieLogWithDetails> userLogs, boolean forceReload) {
        if (!forceReload && Boolean.TRUE.equals(isLoading.getValue())) return;

        cancelInFlightCalls();
        long requestId = currentRequestId.incrementAndGet();

        isLoading.setValue(true);
        viewState.setValue(ExploreViewState.LOADING);
        currentTrendingPage = 1 + random.nextInt(3);
        currentDiscoverPage = 1;

        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.postDelayed(() -> {
            if (requestId == currentRequestId.get() && Boolean.TRUE.equals(isLoading.getValue())) {
                List<RecommendedMovie> current = exploreFeed.getValue();
                if (current == null || current.isEmpty()) {
                    viewState.setValue(ExploreViewState.EMPTY);
                } else {
                    viewState.setValue(ExploreViewState.CONTENT);
                }
                isLoading.setValue(false);
            }
        }, 3500);

        synchronized (seenMovieIds) {
            seenMovieIds.clear();
        }
        synchronized (usedSeedIds) {
            usedSeedIds.clear();
        }
        synchronized (activeSeedCandidatesMap) {
            activeSeedCandidatesMap.clear();
        }

        List<RecommendedMovie> accumulated = new ArrayList<>();
        TasteProfile profile = TasteProfile.build(userLogs, random);

        List<MovieEntity> seedMovies = seedSampler.sampleMovieSeeds(profile, 3, 5);
        for (MovieEntity seed : seedMovies) {
            usedSeedIds.add(seed.getTmdbId());
        }

        boolean hasTopGenre = !profile.genreAffinities.isEmpty();
        int initialCallCount = seedMovies.size() + 2 + (hasTopGenre ? 1 : 0);
        AtomicInteger pendingCalls = new AtomicInteger(Math.max(1, initialCallCount));

        if (!seedMovies.isEmpty()) {
            for (MovieEntity seedMovie : seedMovies) {
                fetchSeedRecommendations(seedMovie, profile, accumulated, pendingCalls, requestId);
            }
        }

        if (hasTopGenre) {
            fetchTopGenrePool(profile, accumulated, pendingCalls, requestId);
        }

        fetchClassicsPool(profile, accumulated, pendingCalls, requestId);
        fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls, requestId);
    }

    private void fetchSeedRecommendations(MovieEntity seedMovie,
                                          TasteProfile profile,
                                          List<RecommendedMovie> accumulated,
                                          AtomicInteger pendingCalls,
                                          long requestId) {
        Call<TmdbMovieResponseDto> call = TmdbClient.getApiService().getMovieRecommendations(seedMovie.getTmdbId(), BuildConfig.TMDB_API_KEY);
        trackCall(call);
        call.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                untrackCall(call);
                if (requestId != currentRequestId.get()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    List<TmdbMovieDto> results = response.body().getResults();
                    List<TmdbMovieDto> movieOnlyResults = new ArrayList<>();
                    for (TmdbMovieDto dto : results) {
                        if (dto != null && !"tv".equalsIgnoreCase(dto.getMediaType())) {
                            movieOnlyResults.add(dto);
                        }
                    }
                    synchronized (activeSeedCandidatesMap) {
                        activeSeedCandidatesMap.put(seedMovie, movieOnlyResults);
                    }
                }
                checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                untrackCall(call);
                if (requestId != currentRequestId.get()) return;
                checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            }
        });
    }



    private void fetchTopGenrePool(TasteProfile profile,
                                   List<RecommendedMovie> accumulated,
                                   AtomicInteger pendingCalls,
                                   long requestId) {
        int topGenreId = -1;
        float maxAffinity = 0f;
        for (Map.Entry<Integer, Float> entry : profile.genreAffinities.entrySet()) {
            if (entry.getValue() > maxAffinity) {
                maxAffinity = entry.getValue();
                topGenreId = entry.getKey();
            }
        }

        if (topGenreId <= 0) {
            checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            return;
        }

        Call<TmdbMovieResponseDto> call = TmdbClient.getApiService().discoverMovies(
                BuildConfig.TMDB_API_KEY,
                "popularity.desc",
                6.5f,
                200,
                String.valueOf(topGenreId),
                null,
                null,
                false,
                1
        );
        trackCall(call);
        call.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                untrackCall(call);
                if (requestId != currentRequestId.get()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    List<TmdbMovieDto> results = response.body().getResults();
                    int count = 0;
                    for (TmdbMovieDto dto : results) {
                        if (dto == null || "tv".equalsIgnoreCase(dto.getMediaType())) continue;
                        if (!feedPlanner.isValidMovie(dto, profile)) continue;
                        synchronized (seenMovieIds) {
                            if (seenMovieIds.contains(dto.getId())) continue;
                            seenMovieIds.add(dto.getId());
                        }
                        List<String> reasons = new ArrayList<>();
                        reasons.add("Fits your top genre interest");
                        float score = (float) dto.getVoteAverage() + 1.5f;
                        synchronized (accumulated) {
                            accumulated.add(new RecommendedMovie(
                                    dto,
                                    score,
                                    reasons,
                                    dto.getPosterPath(),
                                    RecommendedMovie.Category.PERSONAL,
                                    RecommendedMovie.SourceType.MOVIE,
                                    dto.getId(),
                                    dto.getTitle()
                            ));
                        }
                        if (++count >= 4) break;
                    }
                }
                checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                untrackCall(call);
                if (requestId != currentRequestId.get()) return;
                checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            }
        });
    }

    private void fetchClassicsPool(TasteProfile profile,
                                  List<RecommendedMovie> accumulated,
                                  AtomicInteger pendingCalls,
                                  long requestId) {
        int randomPage = 1 + random.nextInt(5);
        Call<TmdbMovieResponseDto> call = TmdbClient.getApiService().discoverMovies(
                BuildConfig.TMDB_API_KEY,
                "vote_average.desc",
                7.8f,
                500,
                null,
                null,
                "2015-12-31",
                false,
                randomPage
        );
        trackCall(call);
        call.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                untrackCall(call);
                if (requestId != currentRequestId.get()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    List<TmdbMovieDto> results = response.body().getResults();
                    Collections.shuffle(results, random);
                    int count = 0;
                    for (TmdbMovieDto dto : results) {
                        if (dto == null || "tv".equalsIgnoreCase(dto.getMediaType())) continue;
                        if (!feedPlanner.isValidMovie(dto, profile)) continue;
                        synchronized (seenMovieIds) {
                            if (seenMovieIds.contains(dto.getId())) continue;
                            seenMovieIds.add(dto.getId());
                        }
                        List<String> reasons = new ArrayList<>();
                        reasons.add("Hall of Fame Classic");
                        float score = (float) dto.getVoteAverage() + 1.2f;
                        synchronized (accumulated) {
                            accumulated.add(new RecommendedMovie(
                                    dto,
                                    score,
                                    reasons,
                                    dto.getPosterPath(),
                                    RecommendedMovie.Category.CLASSIC,
                                    RecommendedMovie.SourceType.MOVIE,
                                    dto.getId(),
                                    dto.getTitle()
                            ));
                        }
                        if (++count >= 6) break;
                    }
                }
                checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                untrackCall(call);
                if (requestId != currentRequestId.get()) return;
                checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            }
        });
    }

    private void fetchTrendingPool(int page,
                                   TasteProfile profile,
                                   List<RecommendedMovie> accumulated,
                                   AtomicInteger pendingCalls,
                                   long requestId) {
        Call<TmdbMovieResponseDto> call = TmdbClient.getApiService().getTrendingMovies(BuildConfig.TMDB_API_KEY);
        trackCall(call);
        call.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                untrackCall(call);
                if (requestId != currentRequestId.get()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    List<TmdbMovieDto> results = response.body().getResults();
                    int count = 0;
                    for (TmdbMovieDto dto : results) {
                        if (dto == null || "tv".equalsIgnoreCase(dto.getMediaType())) continue;
                        if (!feedPlanner.isValidMovie(dto, profile)) continue;
                        synchronized (seenMovieIds) {
                            if (seenMovieIds.contains(dto.getId())) continue;
                            seenMovieIds.add(dto.getId());
                        }
                        List<String> reasons = new ArrayList<>();
                        reasons.add("Trending today on Movieed");

                        float score = (float) dto.getVoteAverage();
                        synchronized (accumulated) {
                            accumulated.add(new RecommendedMovie(
                                    dto,
                                    score,
                                    reasons,
                                    dto.getPosterPath(),
                                    RecommendedMovie.Category.TRENDING,
                                    RecommendedMovie.SourceType.MOVIE,
                                    dto.getId(),
                                    dto.getTitle()
                            ));
                        }
                        if (++count >= 10) break;
                    }
                }
                checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                untrackCall(call);
                if (requestId != currentRequestId.get()) return;
                checkAndProcessMultiSeed(pendingCalls, accumulated, profile, requestId);
            }
        });
    }

    private void checkAndProcessMultiSeed(AtomicInteger pendingCalls,
                                           List<RecommendedMovie> accumulated,
                                           TasteProfile profile,
                                           long requestId) {
        if (pendingCalls.decrementAndGet() <= 0) {
            if (requestId != currentRequestId.get()) return;

            mainHandler.removeCallbacksAndMessages(null);

            List<RecommendedMovie> finalFeed;
            synchronized (activeSeedCandidatesMap) {
                finalFeed = feedPlanner.processAndPlanFeed(
                        activeSeedCandidatesMap,
                        accumulated,
                        profile,
                        seenMovieIds
                );
            }

            exploreFeed.setValue(finalFeed);
            if (finalFeed == null || finalFeed.isEmpty()) {
                viewState.setValue(ExploreViewState.EMPTY);
            } else {
                viewState.setValue(ExploreViewState.CONTENT);
            }
            isLoading.setValue(false);
        }
    }

    public void loadNextPage(List<MovieLogWithDetails> userLogs) {
        if (isFetchingMore) return;
        isFetchingMore = true;
        currentTrendingPage++;
        currentDiscoverPage++;

        long requestId = currentRequestId.get();
        TasteProfile profile = TasteProfile.build(userLogs, random);
        List<RecommendedMovie> currentList = exploreFeed.getValue() != null ? new ArrayList<>(exploreFeed.getValue()) : new ArrayList<>();
        List<RecommendedMovie> nextPageCandidates = new ArrayList<>();

        Call<TmdbMovieResponseDto> call = TmdbClient.getApiService().discoverMovies(
                BuildConfig.TMDB_API_KEY,
                "popularity.desc",
                6.0f,
                150,
                null,
                null,
                null,
                false,
                currentDiscoverPage
        );
        trackCall(call);
        call.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                untrackCall(call);
                isFetchingMore = false;
                if (requestId != currentRequestId.get()) return;

                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    for (TmdbMovieDto dto : response.body().getResults()) {
                        if (dto == null || "tv".equalsIgnoreCase(dto.getMediaType())) continue;
                        if (!feedPlanner.isValidMovie(dto, profile)) continue;
                        synchronized (seenMovieIds) {
                            if (seenMovieIds.contains(dto.getId())) continue;
                            seenMovieIds.add(dto.getId());
                        }
                        List<String> reasons = new ArrayList<>();
                        reasons.add("Trending today on Movieed");
                        nextPageCandidates.add(new RecommendedMovie(
                                dto,
                                (float) dto.getVoteAverage(),
                                reasons,
                                dto.getPosterPath(),
                                RecommendedMovie.Category.TRENDING,
                                RecommendedMovie.SourceType.MOVIE,
                                dto.getId(),
                                dto.getTitle()
                        ));
                    }

                    List<RecommendedMovie> interleavedNew = feedPlanner.getInterleaver().interleaveByReason(
                            nextPageCandidates,
                            feedPlanner.getLastCategory()
                    );
                    currentList.addAll(interleavedNew);
                    exploreFeed.setValue(currentList);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                untrackCall(call);
                isFetchingMore = false;
            }
        });
    }

    public void toggleWatchlist(TmdbMovieDto dto, MovieLogEntity existingLog) {
        if (dto == null) return;
        String genreString = dto.getGenreIds() != null ? dto.getGenreIds().toString().replaceAll("[\\[\\] ]", "") : null;
        MovieEntity movieEntity = new MovieEntity(
                dto.getId(),
                dto.getTitle(),
                dto.getOverview(),
                dto.getReleaseDate(),
                dto.getPosterPath(),
                dto.getBackdropPath(),
                0,
                System.currentTimeMillis(),
                genreString,
                dto.getOriginalLanguage()
        );
        repository.toggleWatchlist(movieEntity, existingLog);
    }

    public void saveReaction(TmdbMovieDto dto, String reaction, MovieLogEntity existingLog) {
        if (dto == null) return;
        String genreString = dto.getGenreIds() != null ? dto.getGenreIds().toString().replaceAll("[\\[\\] ]", "") : null;
        MovieEntity movieEntity = new MovieEntity(
                dto.getId(),
                dto.getTitle(),
                dto.getOverview(),
                dto.getReleaseDate(),
                dto.getPosterPath(),
                dto.getBackdropPath(),
                0,
                System.currentTimeMillis(),
                genreString,
                dto.getOriginalLanguage()
        );
        repository.saveReaction(movieEntity, reaction, existingLog);
    }
}

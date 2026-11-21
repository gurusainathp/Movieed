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
import com.gurusainathp.movieed.domain.recommendation.RecommendationEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int currentTrendingPage = 1;
    private boolean isFetchingMore = false;
    private final Set<Integer> seenMovieIds = new HashSet<>();
    private final Map<MovieEntity, List<TmdbMovieDto>> activeSeedCandidatesMap = new HashMap<>();
    private final Map<Integer, List<TmdbPersonCastCreditDto>> directorCreditsCache = new HashMap<>();

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
        loadExploreMovies(userLogs, false);
    }

    public void loadExploreMovies(List<MovieLogWithDetails> userLogs, boolean forceReload) {
        if (!forceReload && Boolean.TRUE.equals(isLoading.getValue())) return;
        isLoading.setValue(true);
        currentTrendingPage = 1 + random.nextInt(3);

        // Failsafe timeout guarantee: never hang loading for more than 3 seconds
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.postDelayed(() -> {
            if (Boolean.TRUE.equals(isLoading.getValue())) {
                isLoading.setValue(false);
            }
        }, 3000);

        synchronized (seenMovieIds) {
            seenMovieIds.clear();
        }
        synchronized (activeSeedCandidatesMap) {
            activeSeedCandidatesMap.clear();
        }

        List<RecommendedMovie> accumulated = new ArrayList<>();
        RecommendationEngine.UserTasteProfile profile = RecommendationEngine.UserTasteProfile.build(userLogs);

        List<MovieEntity> preferredMovies = new ArrayList<>(profile.lovedMovies);
        if (preferredMovies.isEmpty()) {
            preferredMovies.addAll(profile.likedMovies);
        }

        if (!preferredMovies.isEmpty()) {
            Collections.shuffle(preferredMovies, random);
            int seedCount = Math.min(preferredMovies.size(), 2 + random.nextInt(2));
            List<MovieEntity> seedMovies = preferredMovies.subList(0, seedCount);

            List<MovieEntity> directorSourcePool = new ArrayList<>(profile.lovedMovies);
            if (directorSourcePool.isEmpty()) {
                directorSourcePool.addAll(profile.likedMovies);
            }
            Collections.shuffle(directorSourcePool, random);
            int directorScanLimit = Math.min(2, directorSourcePool.size());
            List<MovieEntity> directorSourceMovies = directorSourcePool.subList(0, directorScanLimit);

            // Primary loading count = seeds + classics + trending (2)
            AtomicInteger pendingCalls = new AtomicInteger(seedMovies.size() + 2);

            for (MovieEntity seedMovie : seedMovies) {
                fetchSeedRecommendations(seedMovie, profile, accumulated, pendingCalls);
            }

            Set<Integer> collectedDirectorIds = new HashSet<>();
            for (MovieEntity sourceMovie : directorSourceMovies) {
                fetchAndPoolDirector(sourceMovie, profile, accumulated, collectedDirectorIds);
            }

            fetchClassicsPool(profile, accumulated, pendingCalls);
            fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls);
        } else {
            AtomicInteger pendingCalls = new AtomicInteger(2);
            fetchClassicsPool(profile, accumulated, pendingCalls);
            fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls);
        }
    }

    private void fetchSeedRecommendations(MovieEntity seedMovie,
                                          RecommendationEngine.UserTasteProfile profile,
                                          List<RecommendedMovie> accumulated,
                                          AtomicInteger pendingCalls) {
        TmdbClient.getApiService().getMovieRecommendations(seedMovie.getTmdbId(), BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbMovieResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                           @NonNull Response<TmdbMovieResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                            List<TmdbMovieDto> results = response.body().getResults();
                            synchronized (activeSeedCandidatesMap) {
                                activeSeedCandidatesMap.put(seedMovie, results);
                            }
                        }
                        checkAndProcessMultiSeed(pendingCalls, accumulated, profile);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                        checkAndProcessMultiSeed(pendingCalls, accumulated, profile);
                    }
                });
    }

    private void fetchAndPoolDirector(MovieEntity sourceMovie,
                                      RecommendationEngine.UserTasteProfile profile,
                                      List<RecommendedMovie> accumulated,
                                      Set<Integer> collectedDirectorIds) {
        TmdbClient.getApiService().getMovieCredits(sourceMovie.getTmdbId(), BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbCreditsResponseDto> call,
                                           @NonNull Response<TmdbCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getCrew() != null) {
                            for (TmdbCrewDto crew : response.body().getCrew()) {
                                if (crew != null && "Director".equalsIgnoreCase(crew.getJob())) {
                                    synchronized (collectedDirectorIds) {
                                        if (!collectedDirectorIds.contains(crew.getId())) {
                                            collectedDirectorIds.add(crew.getId());
                                            fetchDirectorMovies(crew.getId(), crew.getName(), sourceMovie, profile, accumulated);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Throwable t) {}
                });
    }

    private void fetchDirectorMovies(int directorId, String directorName, MovieEntity sourceMovie,
                                     RecommendationEngine.UserTasteProfile profile,
                                     List<RecommendedMovie> accumulated) {
        synchronized (directorCreditsCache) {
            if (directorCreditsCache.containsKey(directorId)) {
                List<TmdbPersonCastCreditDto> cached = directorCreditsCache.get(directorId);
                if (cached != null) {
                    processDirectorCredits(cached, directorId, directorName, sourceMovie, profile, accumulated);
                    return;
                }
            }
        }

        TmdbClient.getApiService().getCombinedCredits(directorId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbCombinedCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbCombinedCreditsResponseDto> call,
                                           @NonNull Response<TmdbCombinedCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getCrew() != null) {
                            List<TmdbPersonCastCreditDto> crewMovies = new ArrayList<>(response.body().getCrew());
                            Collections.sort(crewMovies, (a, b) -> Double.compare(b.getVoteAverage(), a.getVoteAverage()));

                            synchronized (directorCreditsCache) {
                                directorCreditsCache.put(directorId, crewMovies);
                            }

                            processDirectorCredits(crewMovies, directorId, directorName, sourceMovie, profile, accumulated);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbCombinedCreditsResponseDto> call, @NonNull Throwable t) {}
                });
    }

    private void processDirectorCredits(List<TmdbPersonCastCreditDto> crewMovies, int directorId, String directorName, MovieEntity sourceMovie,
                                        RecommendationEngine.UserTasteProfile profile, List<RecommendedMovie> accumulated) {
        int count = 0;
        List<RecommendedMovie> directorBatch = new ArrayList<>();
        for (TmdbPersonCastCreditDto item : crewMovies) {
            if (item == null || item.getTitle() == null) continue;
            if (profile.loggedMovieIds.contains((long) item.getId())) continue;
            if (item.getBackdropPath() == null && item.getPosterPath() == null) continue;

            synchronized (seenMovieIds) {
                if (seenMovieIds.contains(item.getId())) continue;
                seenMovieIds.add(item.getId());
            }

            TmdbMovieDto dto = new TmdbMovieDto(
                    item.getId(),
                    item.getTitle(),
                    item.getOverview(),
                    item.getReleaseDate(),
                    item.getPosterPath(),
                    item.getBackdropPath(),
                    item.getVoteAverage(),
                    item.getMediaType()
            );

            List<String> reasons = new ArrayList<>();
            reasons.add("Directed by " + directorName + " (from your history)");
            float score = (float) item.getVoteAverage() + 3.5f + random.nextFloat();

            directorBatch.add(new RecommendedMovie(
                    dto,
                    score,
                    reasons,
                    sourceMovie.getPosterPath(),
                    RecommendedMovie.SourceType.PERSON,
                    directorId,
                    directorName
            ));

            if (++count >= 2) break;
        }

        if (!directorBatch.isEmpty()) {
            synchronized (accumulated) {
                accumulated.addAll(directorBatch);
            }
            List<RecommendedMovie> currentFeed = exploreFeed.getValue();
            if (currentFeed != null) {
                List<RecommendedMovie> updated = new ArrayList<>(currentFeed);
                updated.addAll(directorBatch);
                Collections.sort(updated, (a, b) -> Float.compare(b.getScore(), a.getScore()));
                exploreFeed.setValue(updated);
            }
        }
    }

    private void fetchClassicsPool(RecommendationEngine.UserTasteProfile profile,
                                  List<RecommendedMovie> accumulated,
                                  AtomicInteger pendingCalls) {
        int randomPage = 1 + random.nextInt(8);
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
                                if (!isValidMovie(dto, profile)) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                reasons.add("Hall of Fame Classic (" + String.format(Locale.US, "%.1f★", dto.getVoteAverage()) + ")");
                                float score = (float) dto.getVoteAverage() + 1.2f + random.nextFloat();
                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(
                                            dto,
                                            score,
                                            reasons,
                                            dto.getPosterPath(),
                                            RecommendedMovie.SourceType.MOVIE,
                                            dto.getId(),
                                            dto.getTitle()
                                    ));
                                }
                                if (++count >= 6) break;
                            }
                        }
                        checkAndProcessMultiSeed(pendingCalls, accumulated, profile);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                        checkAndProcessMultiSeed(pendingCalls, accumulated, profile);
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
                                if (!isValidMovie(dto, profile)) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                if (dto.getVoteAverage() >= 7.5) {
                                    reasons.add("Globally Acclaimed Hit (" + String.format(Locale.US, "%.1f★", dto.getVoteAverage()) + ")");
                                } else {
                                    reasons.add("Trending Today on Movieed");
                                }

                                float score = (float) dto.getVoteAverage() + random.nextFloat();
                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(
                                            dto,
                                            score,
                                            reasons,
                                            dto.getPosterPath(),
                                            RecommendedMovie.SourceType.MOVIE,
                                            dto.getId(),
                                            dto.getTitle()
                                    ));
                                }
                                if (++count >= 10) break;
                            }
                        }
                        checkAndProcessMultiSeed(pendingCalls, accumulated, profile);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                        checkAndProcessMultiSeed(pendingCalls, accumulated, profile);
                    }
                });
    }

    private void checkAndProcessMultiSeed(AtomicInteger pendingCalls,
                                           List<RecommendedMovie> accumulated,
                                           RecommendationEngine.UserTasteProfile profile) {
        if (pendingCalls.decrementAndGet() == 0) {
            mainHandler.removeCallbacksAndMessages(null);
            isLoading.setValue(false);

            synchronized (activeSeedCandidatesMap) {
                if (!activeSeedCandidatesMap.isEmpty()) {
                    List<RecommendedMovie> multiSeedList = recommendationEngine.aggregateMultiSeedRecommendations(
                            activeSeedCandidatesMap,
                            profile
                    );

                    for (RecommendedMovie rm : multiSeedList) {
                        synchronized (seenMovieIds) {
                            if (!seenMovieIds.contains(rm.getMovie().getId())) {
                                seenMovieIds.add(rm.getMovie().getId());
                                synchronized (accumulated) {
                                    accumulated.add(rm);
                                }
                            }
                        }
                    }
                }
            }

            List<RecommendedMovie> jitteredList = new ArrayList<>();
            for (RecommendedMovie rm : accumulated) {
                float jitteredScore = rm.getScore() + (random.nextFloat() * 6.0f) - 2.0f;
                jitteredList.add(new RecommendedMovie(
                        rm.getMovie(),
                        jitteredScore,
                        rm.getReasons(),
                        rm.getReasonPosterPath(),
                        rm.getSourceType(),
                        rm.getSourceId(),
                        rm.getSourceName()
                ));
            }

            Collections.sort(jitteredList, (a, b) -> Float.compare(b.getScore(), a.getScore()));

            List<RecommendedMovie> blendedList = new ArrayList<>();
            int chunkSize = 5;
            for (int i = 0; i < jitteredList.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, jitteredList.size());
                List<RecommendedMovie> chunk = new ArrayList<>(jitteredList.subList(i, end));
                Collections.shuffle(chunk, random);
                blendedList.addAll(chunk);
            }

            List<RecommendedMovie> interleavedList = interleaveByReason(blendedList);
            exploreFeed.setValue(interleavedList);
        }
    }

    private List<RecommendedMovie> interleaveByReason(List<RecommendedMovie> inputList) {
        if (inputList == null || inputList.size() <= 1) {
            return inputList;
        }

        Map<String, List<RecommendedMovie>> buckets = new java.util.LinkedHashMap<>();
        buckets.put("MOVIE", new ArrayList<>());
        buckets.put("DIRECTOR", new ArrayList<>());
        buckets.put("CLASSIC", new ArrayList<>());
        buckets.put("TRENDING", new ArrayList<>());

        for (RecommendedMovie rm : inputList) {
            String reason = (rm.getReasons() != null && !rm.getReasons().isEmpty())
                    ? rm.getReasons().get(0) : "";

            if (reason.startsWith("Because you loved") || reason.startsWith("Because you liked")) {
                buckets.get("MOVIE").add(rm);
            } else if (reason.startsWith("Directed by")) {
                buckets.get("DIRECTOR").add(rm);
            } else if (reason.startsWith("Hall of Fame Classic")) {
                buckets.get("CLASSIC").add(rm);
            } else {
                buckets.get("TRENDING").add(rm);
            }
        }

        List<List<RecommendedMovie>> activeBuckets = new ArrayList<>();
        for (List<RecommendedMovie> bucket : buckets.values()) {
            if (!bucket.isEmpty()) {
                Collections.shuffle(bucket, random);
                activeBuckets.add(bucket);
            }
        }

        if (activeBuckets.isEmpty()) return inputList;

        List<RecommendedMovie> interleaved = new ArrayList<>();
        int totalSize = inputList.size();
        int bucketIndex = 0;

        while (interleaved.size() < totalSize) {
            boolean addedAny = false;
            int scanned = 0;
            while (scanned < activeBuckets.size()) {
                List<RecommendedMovie> currentBucket = activeBuckets.get(bucketIndex % activeBuckets.size());
                bucketIndex++;
                scanned++;

                if (!currentBucket.isEmpty()) {
                    interleaved.add(currentBucket.remove(0));
                    addedAny = true;
                    break;
                }
            }
            if (!addedAny) break;
        }

        return interleaved;
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
                                if (!isValidMovie(dto, profile)) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                if (dto.getVoteAverage() >= 7.5) {
                                    reasons.add("Globally Acclaimed Hit (" + String.format(Locale.US, "%.1f★", dto.getVoteAverage()) + ")");
                                } else {
                                    reasons.add("Trending Today on Movieed");
                                }
                                currentList.add(new RecommendedMovie(
                                        dto,
                                        (float) dto.getVoteAverage(),
                                        reasons,
                                        dto.getPosterPath(),
                                        RecommendedMovie.SourceType.MOVIE,
                                        dto.getId(),
                                        dto.getTitle()
                                ));
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

    private boolean isValidMovie(TmdbMovieDto dto, RecommendationEngine.UserTasteProfile profile) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().trim().isEmpty()) return false;
        if (profile != null && profile.loggedMovieIds.contains((long) dto.getId())) return false;
        if (dto.getBackdropPath() == null && dto.getPosterPath() == null) return false;
        if (dto.getOverview() == null || dto.getOverview().trim().isEmpty()) return false;

        String titleLower = dto.getTitle().toLowerCase(Locale.US);
        if (titleLower.contains("making of") ||
            titleLower.contains("behind the scenes") ||
            titleLower.contains("featurette") ||
            titleLower.contains("deleted scenes") ||
            titleLower.contains("bloopers")) {
            return false;
        }
        return true;
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

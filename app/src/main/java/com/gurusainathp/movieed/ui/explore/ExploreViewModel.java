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
    private int currentTrendingPage = 1;
    private boolean isFetchingMore = false;
    private final Set<Integer> seenMovieIds = new HashSet<>();
    private final Map<MovieEntity, List<TmdbMovieDto>> activeSeedCandidatesMap = new HashMap<>();

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
            int seedCount = Math.min(preferredMovies.size(), 3 + random.nextInt(3)); // 3 to 5 random seeds
            List<MovieEntity> seedMovies = preferredMovies.subList(0, seedCount);

            AtomicInteger pendingCalls = new AtomicInteger(seedMovies.size() + 2);

            for (MovieEntity seedMovie : seedMovies) {
                fetchSeedRecommendationsAndCredits(seedMovie, profile, accumulated, pendingCalls);
            }

            fetchClassicsPool(profile, accumulated, pendingCalls);
            fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls);
        } else {
            AtomicInteger pendingCalls = new AtomicInteger(2);
            fetchClassicsPool(profile, accumulated, pendingCalls);
            fetchTrendingPool(currentTrendingPage, profile, accumulated, pendingCalls);
        }
    }

    private void fetchSeedRecommendationsAndCredits(MovieEntity seedMovie,
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

                        fetchDirectorFilmography(seedMovie, profile, accumulated);
                        checkAndProcessMultiSeed(pendingCalls, accumulated, profile);
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                        checkAndProcessMultiSeed(pendingCalls, accumulated, profile);
                    }
                });
    }

    private void fetchDirectorFilmography(MovieEntity seedMovie,
                                         RecommendationEngine.UserTasteProfile profile,
                                         List<RecommendedMovie> accumulated) {
        TmdbClient.getApiService().getMovieCredits(seedMovie.getTmdbId(), BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbCreditsResponseDto> call,
                                           @NonNull Response<TmdbCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            TmdbCreditsResponseDto credits = response.body();
                            TmdbCrewDto director = null;
                            if (credits.getCrew() != null) {
                                for (TmdbCrewDto crew : credits.getCrew()) {
                                    if (crew != null && "Director".equalsIgnoreCase(crew.getJob())) {
                                        director = crew;
                                        break;
                                    }
                                }
                            }

                            if (director != null) {
                                fetchDirectorMovies(director.getId(), director.getName(), seedMovie, profile, accumulated);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Throwable t) {}
                });
    }

    private void fetchDirectorMovies(int directorId, String directorName, MovieEntity seedMovie,
                                     RecommendationEngine.UserTasteProfile profile,
                                     List<RecommendedMovie> accumulated) {
        TmdbClient.getApiService().getCombinedCredits(directorId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbCombinedCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbCombinedCreditsResponseDto> call,
                                           @NonNull Response<TmdbCombinedCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getCrew() != null) {
                            List<TmdbPersonCastCreditDto> crewMovies = response.body().getCrew();
                            int count = 0;
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
                                reasons.add("Directed by " + directorName + " (from " + seedMovie.getTitle() + ")");
                                float score = (float) item.getVoteAverage() + 3.5f + random.nextFloat();

                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(
                                            dto,
                                            score,
                                            reasons,
                                            seedMovie.getPosterPath(),
                                            RecommendedMovie.SourceType.PERSON,
                                            directorId,
                                            directorName
                                    ));
                                }

                                if (++count >= 3) break;
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbCombinedCreditsResponseDto> call, @NonNull Throwable t) {}
                });
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
                                if (++count >= 4) break;
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

    private void checkAndProcessMultiSeed(AtomicInteger pendingCalls,
                                           List<RecommendedMovie> accumulated,
                                           RecommendationEngine.UserTasteProfile profile) {
        if (pendingCalls.decrementAndGet() == 0) {
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

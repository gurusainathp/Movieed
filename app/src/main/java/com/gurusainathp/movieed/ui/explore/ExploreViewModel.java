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
        currentTrendingPage = 1 + random.nextInt(3);
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
                                if (!isValidMovie(dto, profile)) continue;
                                synchronized (seenMovieIds) {
                                    if (seenMovieIds.contains(dto.getId())) continue;
                                    seenMovieIds.add(dto.getId());
                                }
                                List<String> reasons = new ArrayList<>();
                                reasons.add("Because you loved " + movie.getTitle());
                                float score = (float) dto.getVoteAverage() + 2.5f + random.nextFloat();
                                synchronized (accumulated) {
                                    accumulated.add(new RecommendedMovie(
                                            dto,
                                            score,
                                            reasons,
                                            movie.getPosterPath(),
                                            RecommendedMovie.SourceType.MOVIE,
                                            movie.getTmdbId(),
                                            movie.getTitle()
                                    ));
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

    private void fetchPersonCreditsRecommendations(MovieEntity watchedMovie,
                                                   RecommendationEngine.UserTasteProfile profile,
                                                   List<RecommendedMovie> accumulated,
                                                   AtomicInteger pendingCalls) {
        TmdbClient.getApiService().getMovieCredits(watchedMovie.getTmdbId(), BuildConfig.TMDB_API_KEY)
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

                            TmdbCastDto topActor = null;
                            if (credits.getCast() != null && !credits.getCast().isEmpty()) {
                                topActor = credits.getCast().get(0);
                            }

                            if (director != null) {
                                fetchPersonFilmography(director.getId(), director.getName(), "Director",
                                        watchedMovie, profile, accumulated, pendingCalls);
                            } else if (topActor != null) {
                                fetchPersonFilmography(topActor.getId(), topActor.getName(), "Actor",
                                        watchedMovie, profile, accumulated, pendingCalls);
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

    private void fetchPersonFilmography(int personId, String personName, String role,
                                        MovieEntity watchedMovie,
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
                                    if (item.getBackdropPath() == null && item.getPosterPath() == null) continue;
                                    if (item.getOverview() == null || item.getOverview().trim().isEmpty()) continue;

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
                                    if ("Director".equalsIgnoreCase(role)) {
                                        reasons.add("Directed by " + personName + " (from " + watchedMovie.getTitle() + ")");
                                    } else {
                                        reasons.add("Starring " + personName + " (from " + watchedMovie.getTitle() + ")");
                                    }

                                    float score = (float) item.getVoteAverage() + 2.0f + random.nextFloat();
                                    synchronized (accumulated) {
                                        accumulated.add(new RecommendedMovie(
                                                dto,
                                                score,
                                                reasons,
                                                watchedMovie.getPosterPath(),
                                                RecommendedMovie.SourceType.PERSON,
                                                personId,
                                                personName
                                        ));
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

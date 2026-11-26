package com.gurusainathp.movieed.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.api.TmdbApiService;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.data.repository.MovieRepository;
import com.gurusainathp.movieed.domain.model.Movie;
import com.gurusainathp.movieed.domain.recommendation.RecommendationEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends AndroidViewModel {

    private final MovieRepository repository;
    private final LiveData<List<MovieLogWithDetails>> userLogs;
    private final RecommendationEngine recommendationEngine = new RecommendationEngine();

    private final MutableLiveData<List<Movie>> trendingMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> trendingSeries = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> classicsMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> classicsSeries = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> nowPlayingMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> recommendedMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> streamingSeries = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Movie> featuredMovie = new MutableLiveData<>(null);

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private Call<TmdbMovieResponseDto> trendingCall;
    private Call<TmdbMovieResponseDto> trendingTvCall;
    private Call<TmdbMovieResponseDto> classicsCall;
    private Call<TmdbMovieResponseDto> classicsTvCall;
    private Call<TmdbMovieResponseDto> nowPlayingCall;
    private Call<TmdbMovieResponseDto> recommendationsCall;
    private Call<TmdbMovieResponseDto> streamingTvCall;

    private int activePendingRequests = 0;
    private boolean hadNetworkSuccess = false;
    private boolean isContentLoaded = false;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
        userLogs = repository.getAllLogsWithDetails();
    }

    public LiveData<List<MovieLogWithDetails>> getUserLogs() { return userLogs; }
    public LiveData<List<Movie>> getTrendingMovies() { return trendingMovies; }
    public LiveData<List<Movie>> getTrendingSeries() { return trendingSeries; }
    public LiveData<List<Movie>> getClassicsMovies() { return classicsMovies; }
    public LiveData<List<Movie>> getClassicsSeries() { return classicsSeries; }
    public LiveData<List<Movie>> getNowPlayingMovies() { return nowPlayingMovies; }
    public LiveData<List<Movie>> getRecommendedMovies() { return recommendedMovies; }
    public LiveData<List<Movie>> getStreamingSeries() { return streamingSeries; }
    public LiveData<Movie> getFeaturedMovie() { return featuredMovie; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadHomeContent(String apiKey) {
        loadHomeContent(apiKey, false);
    }

    public void loadHomeContent(String apiKey, boolean forceReload) {
        if (!forceReload && (isContentLoaded || activePendingRequests > 0)) {
            return;
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            errorMessage.setValue("TMDB API Key missing in local.properties");
            return;
        }

        cancelOngoingCalls();

        isLoading.setValue(true);
        errorMessage.setValue(null);
        hadNetworkSuccess = false;
        activePendingRequests = 7;

        TmdbApiService apiService = TmdbClient.getApiService();

        // 1. Trending movies
        trendingCall = apiService.getTrendingMovies(apiKey);
        trendingCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<Movie> list = mapDtosToMovies(response.body().getResults(), "movie");
                    trendingMovies.setValue(list);
                    if (!list.isEmpty() && featuredMovie.getValue() == null) {
                        featuredMovie.setValue(list.get(0));
                    }
                }
                checkAllRequestsFinished();
            }
            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) checkAllRequestsFinished();
            }
        });

        // 1b. Trending TV Series
        trendingTvCall = apiService.getTrendingTv(apiKey);
        trendingTvCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<Movie> list = mapDtosToMovies(response.body().getResults(), "tv");
                    trendingSeries.setValue(list);
                }
                checkAllRequestsFinished();
            }
            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) checkAllRequestsFinished();
            }
        });

        // 2. Hall of Fame Classics (Movies)
        int randomPage = new Random().nextInt(2) + 1;
        classicsCall = apiService.getTopRatedMovies(apiKey, randomPage);
        classicsCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<TmdbMovieDto> raw = response.body().getResults();
                    if (raw != null) Collections.shuffle(raw);
                    classicsMovies.setValue(mapDtosToMovies(raw, "movie"));
                }
                checkAllRequestsFinished();
            }
            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) checkAllRequestsFinished();
            }
        });

        // 2b. Hall of Fame Classic Series (TV)
        classicsTvCall = apiService.getTopRatedTv(apiKey, randomPage);
        classicsTvCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<TmdbMovieDto> raw = response.body().getResults();
                    if (raw != null) Collections.shuffle(raw);
                    classicsSeries.setValue(mapDtosToMovies(raw, "tv"));
                }
                checkAllRequestsFinished();
            }
            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) checkAllRequestsFinished();
            }
        });

        // 3. Now Playing (In Theaters)
        nowPlayingCall = apiService.getNowPlayingMovies(apiKey, 1);
        nowPlayingCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    nowPlayingMovies.setValue(mapDtosToMovies(response.body().getResults(), "movie"));
                }
                checkAllRequestsFinished();
            }
            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) checkAllRequestsFinished();
            }
        });

        // 3b. Currently Airing TV Series (On The Air)
        streamingTvCall = apiService.getOnTheAirTv(apiKey, 1);
        streamingTvCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    streamingSeries.setValue(mapDtosToMovies(response.body().getResults(), "tv"));
                }
                checkAllRequestsFinished();
            }
            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) checkAllRequestsFinished();
            }
        });

        // 4. Personalized Recommendations ("You May Like These")
        fetchPersonalizedRecommendations(apiKey);
    }

    private void fetchPersonalizedRecommendations(String apiKey) {
        TmdbApiService apiService = TmdbClient.getApiService();
        List<MovieLogWithDetails> logs = userLogs.getValue();
        int seedMovieId = -1;

        if (logs != null) {
            for (MovieLogWithDetails item : logs) {
                if (item.log != null && ("LOVE".equals(item.log.getReaction()) || "LIKE".equals(item.log.getReaction()))) {
                    if (item.movie != null) {
                        seedMovieId = (int) item.movie.getTmdbId();
                        break;
                    }
                }
            }
        }

        if (seedMovieId != -1) {
            recommendationsCall = apiService.getMovieRecommendations(seedMovieId, apiKey);
        } else {
            recommendationsCall = apiService.getPopularMovies(apiKey, 1);
        }

        recommendationsCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<TmdbMovieDto> raw = response.body().getResults();
                    List<TmdbMovieDto> filtered = recommendationEngine.filterAndRankRecommendations(raw, userLogs.getValue());
                    recommendedMovies.setValue(mapDtosToMovies(filtered, "movie"));
                }
                checkAllRequestsFinished();
            }
            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) checkAllRequestsFinished();
            }
        });
    }

    private synchronized void checkAllRequestsFinished() {
        activePendingRequests--;
        if (activePendingRequests <= 0) {
            isLoading.setValue(false);
            if (hadNetworkSuccess) {
                isContentLoaded = true;
            } else {
                errorMessage.setValue("Could not connect to TMDB. Please check your internet connection.");
            }
        }
    }

    private List<Movie> mapDtosToMovies(List<TmdbMovieDto> dtos, String defaultMediaType) {
        List<Movie> result = new ArrayList<>();
        if (dtos != null) {
            for (TmdbMovieDto dto : dtos) {
                if (dto.getPosterPath() != null) {
                    String type = dto.getMediaType() != null ? dto.getMediaType() : defaultMediaType;
                    result.add(new Movie(
                            dto.getId(),
                            dto.getTitle(),
                            dto.getOverview(),
                            dto.getReleaseDate(),
                            dto.getPosterPath(),
                            dto.getBackdropPath(),
                            dto.getVoteAverage(),
                            type
                    ));
                }
            }
        }
        return result;
    }

    private void cancelOngoingCalls() {
        if (trendingCall != null && !trendingCall.isCanceled()) trendingCall.cancel();
        if (trendingTvCall != null && !trendingTvCall.isCanceled()) trendingTvCall.cancel();
        if (classicsCall != null && !classicsCall.isCanceled()) classicsCall.cancel();
        if (classicsTvCall != null && !classicsTvCall.isCanceled()) classicsTvCall.cancel();
        if (nowPlayingCall != null && !nowPlayingCall.isCanceled()) nowPlayingCall.cancel();
        if (recommendationsCall != null && !recommendationsCall.isCanceled()) recommendationsCall.cancel();
        if (streamingTvCall != null && !streamingTvCall.isCanceled()) streamingTvCall.cancel();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelOngoingCalls();
    }
}

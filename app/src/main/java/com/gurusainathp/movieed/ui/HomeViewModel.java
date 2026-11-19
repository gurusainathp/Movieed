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

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends AndroidViewModel {

    private final MovieRepository repository;
    private final LiveData<List<MovieLogWithDetails>> userLogs;

    private final MutableLiveData<List<Movie>> trendingMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> topRatedMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> nowPlayingMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Movie>> popularMovies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Movie> featuredMovie = new MutableLiveData<>(null);

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private Call<TmdbMovieResponseDto> trendingCall;
    private Call<TmdbMovieResponseDto> topRatedCall;
    private Call<TmdbMovieResponseDto> nowPlayingCall;
    private Call<TmdbMovieResponseDto> popularCall;

    private int activePendingRequests = 0;
    private boolean hadNetworkSuccess = false;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
        userLogs = repository.getAllLogsWithDetails();
    }

    public LiveData<List<MovieLogWithDetails>> getUserLogs() {
        return userLogs;
    }

    public LiveData<List<Movie>> getTrendingMovies() {
        return trendingMovies;
    }

    public LiveData<List<Movie>> getTopRatedMovies() {
        return topRatedMovies;
    }

    public LiveData<List<Movie>> getNowPlayingMovies() {
        return nowPlayingMovies;
    }

    public LiveData<List<Movie>> getPopularMovies() {
        return popularMovies;
    }

    public LiveData<Movie> getFeaturedMovie() {
        return featuredMovie;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadHomeContent(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            errorMessage.setValue("TMDB API Key missing in local.properties");
            return;
        }

        cancelOngoingCalls();

        isLoading.setValue(true);
        errorMessage.setValue(null);
        hadNetworkSuccess = false;
        activePendingRequests = 4;

        TmdbApiService apiService = TmdbClient.getApiService();

        // 1. Trending movies
        trendingCall = apiService.getTrendingMovies(apiKey);
        trendingCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<Movie> list = mapDtosToMovies(response.body().getResults());
                    trendingMovies.setValue(list);
                    if (!list.isEmpty() && featuredMovie.getValue() == null) {
                        featuredMovie.setValue(list.get(0));
                    }
                }
                checkAllRequestsFinished();
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    checkAllRequestsFinished();
                }
            }
        });

        // 2. Top Rated movies (Top 10)
        topRatedCall = apiService.getTopRatedMovies(apiKey, 1);
        topRatedCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<Movie> list = mapDtosToMovies(response.body().getResults());
                    topRatedMovies.setValue(list);
                }
                checkAllRequestsFinished();
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    checkAllRequestsFinished();
                }
            }
        });

        // 3. Now Playing (In Theaters)
        nowPlayingCall = apiService.getNowPlayingMovies(apiKey, 1);
        nowPlayingCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<Movie> list = mapDtosToMovies(response.body().getResults());
                    nowPlayingMovies.setValue(list);
                }
                checkAllRequestsFinished();
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    checkAllRequestsFinished();
                }
            }
        });

        // 4. Popular movies
        popularCall = apiService.getPopularMovies(apiKey, 1);
        popularCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hadNetworkSuccess = true;
                    List<Movie> list = mapDtosToMovies(response.body().getResults());
                    popularMovies.setValue(list);
                }
                checkAllRequestsFinished();
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    checkAllRequestsFinished();
                }
            }
        });
    }

    private synchronized void checkAllRequestsFinished() {
        activePendingRequests--;
        if (activePendingRequests <= 0) {
            isLoading.setValue(false);
            if (!hadNetworkSuccess) {
                errorMessage.setValue("Could not connect to TMDB. Please check your internet connection.");
            }
        }
    }

    private List<Movie> mapDtosToMovies(List<TmdbMovieDto> dtos) {
        List<Movie> result = new ArrayList<>();
        if (dtos != null) {
            for (TmdbMovieDto dto : dtos) {
                if (dto.getPosterPath() != null) {
                    result.add(new Movie(
                            dto.getId(),
                            dto.getTitle(),
                            dto.getOverview(),
                            dto.getReleaseDate(),
                            dto.getPosterPath(),
                            dto.getBackdropPath(),
                            dto.getVoteAverage(),
                            dto.getMediaType() != null ? dto.getMediaType() : "movie"
                    ));
                }
            }
        }
        return result;
    }

    private void cancelOngoingCalls() {
        if (trendingCall != null && !trendingCall.isCanceled()) trendingCall.cancel();
        if (topRatedCall != null && !topRatedCall.isCanceled()) topRatedCall.cancel();
        if (nowPlayingCall != null && !nowPlayingCall.isCanceled()) nowPlayingCall.cancel();
        if (popularCall != null && !popularCall.isCanceled()) popularCall.cancel();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelOngoingCalls();
    }
}

package com.gurusainathp.movieed.ui.search;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.api.TmdbApiService;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.domain.model.Movie;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends ViewModel {

    private final MutableLiveData<List<Movie>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private Call<TmdbMovieResponseDto> currentSearchCall;

    public LiveData<List<Movie>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void searchMovies(String query, String apiKey) {
        if (query == null || query.trim().isEmpty()) {
            searchResults.setValue(new ArrayList<>());
            errorMessage.setValue(null);
            return;
        }

        if (currentSearchCall != null && !currentSearchCall.isCanceled()) {
            currentSearchCall.cancel();
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        TmdbApiService apiService = TmdbClient.getApiService();
        currentSearchCall = apiService.searchMulti(apiKey, query.trim(), 1);
        currentSearchCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovieDto> dtos = response.body().getResults();
                    List<Movie> movies = new ArrayList<>();
                    if (dtos != null) {
                        for (TmdbMovieDto dto : dtos) {
                            String mediaType = dto.getMediaType();
                            // Include movies and tv shows (ignore persons)
                            if (mediaType == null || !mediaType.equals("person")) {
                                movies.add(new Movie(
                                        dto.getId(),
                                        dto.getTitle(),
                                        dto.getOverview(),
                                        dto.getReleaseDate(),
                                        dto.getPosterPath(),
                                        dto.getBackdropPath(),
                                        dto.getVoteAverage(),
                                        dto.getMediaType()
                                ));
                            }
                        }
                    }
                    searchResults.setValue(movies);
                } else {
                    errorMessage.setValue("Error fetching search results (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    isLoading.setValue(false);
                    errorMessage.setValue("Network error: " + t.getLocalizedMessage());
                }
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentSearchCall != null && !currentSearchCall.isCanceled()) {
            currentSearchCall.cancel();
        }
    }
}

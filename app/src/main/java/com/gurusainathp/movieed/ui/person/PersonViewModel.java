package com.gurusainathp.movieed.ui.person;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonMovieCreditsResponseDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonViewModel extends ViewModel {

    private final MutableLiveData<TmdbPersonDto> personDetails = new MutableLiveData<>();
    private final MutableLiveData<List<TmdbMovieDto>> filmography = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<TmdbPersonDto> getPersonDetails() { return personDetails; }
    public LiveData<List<TmdbMovieDto>> getFilmography() { return filmography; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void fetchPerson(int personId) {
        TmdbClient.getApiService().getPersonDetails(personId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbPersonDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbPersonDto> call, @NonNull Response<TmdbPersonDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            personDetails.setValue(response.body());
                        } else {
                            errorMessage.setValue("Failed to load person (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbPersonDto> call, @NonNull Throwable t) {
                        errorMessage.setValue("Error: " + t.getLocalizedMessage());
                    }
                });

        TmdbClient.getApiService().getPersonMovieCredits(personId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbPersonMovieCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbPersonMovieCreditsResponseDto> call,
                                           @NonNull Response<TmdbPersonMovieCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            filmography.setValue(response.body().getCast());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbPersonMovieCreditsResponseDto> call, @NonNull Throwable t) {}
                });
    }
}

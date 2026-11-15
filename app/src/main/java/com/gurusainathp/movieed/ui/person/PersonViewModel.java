package com.gurusainathp.movieed.ui.person;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbCombinedCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonViewModel extends ViewModel {

    private final MutableLiveData<TmdbPersonDto> personDetails = new MutableLiveData<>();
    private final MutableLiveData<List<TmdbPersonCastCreditDto>> filmography = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<TmdbPersonDto> getPersonDetails() { return personDetails; }
    public LiveData<List<TmdbPersonCastCreditDto>> getFilmography() { return filmography; }
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

        TmdbClient.getApiService().getCombinedCredits(personId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbCombinedCreditsResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<TmdbCombinedCreditsResponseDto> call,
                                           @NonNull Response<TmdbCombinedCreditsResponseDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<Integer, TmdbPersonCastCreditDto> combinedMap = new HashMap<>();
                            if (response.body().getCast() != null) {
                                for (TmdbPersonCastCreditDto c : response.body().getCast()) {
                                    combinedMap.put(c.getId(), c);
                                }
                            }
                            if (response.body().getCrew() != null) {
                                for (TmdbPersonCastCreditDto c : response.body().getCrew()) {
                                    if (!combinedMap.containsKey(c.getId())) {
                                        combinedMap.put(c.getId(), c);
                                    }
                                }
                            }
                            filmography.setValue(new ArrayList<>(combinedMap.values()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TmdbCombinedCreditsResponseDto> call, @NonNull Throwable t) {}
                });
    }
}

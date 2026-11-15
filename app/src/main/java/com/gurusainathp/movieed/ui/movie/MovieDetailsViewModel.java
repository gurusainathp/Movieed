package com.gurusainathp.movieed.ui.movie;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbCastDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.repository.MovieRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailsViewModel extends AndroidViewModel {

    private final MovieRepository repository;
    private final MutableLiveData<TmdbMovieDto> tmdbMovieDetails = new MutableLiveData<>();
    private final MutableLiveData<List<TmdbCastDto>> castList = new MutableLiveData<>();
    private final MutableLiveData<List<TmdbCrewDto>> crewList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MovieDetailsViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
    }

    public LiveData<TmdbMovieDto> getTmdbMovieDetails() {
        return tmdbMovieDetails;
    }

    public LiveData<List<TmdbCastDto>> getCastList() {
        return castList;
    }

    public LiveData<List<TmdbCrewDto>> getCrewList() {
        return crewList;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<MovieEntity> getMovieByTmdbId(int tmdbId) {
        return repository.getMovieByTmdbId(tmdbId);
    }

    public LiveData<List<MovieLogEntity>> getLogsForMovie(long movieId) {
        return repository.getLogsForMovie(movieId);
    }

    public void fetchMovieDetails(int tmdbId, boolean isTv) {
        Call<TmdbMovieDto> detailsCall = isTv ?
                TmdbClient.getApiService().getTvDetails(tmdbId, BuildConfig.TMDB_API_KEY) :
                TmdbClient.getApiService().getMovieDetails(tmdbId, BuildConfig.TMDB_API_KEY);

        detailsCall.enqueue(new Callback<TmdbMovieDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieDto> call, @NonNull Response<TmdbMovieDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tmdbMovieDetails.setValue(response.body());
                } else {
                    errorMessage.setValue("Failed to load details (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieDto> call, @NonNull Throwable t) {
                errorMessage.setValue("Error: " + t.getLocalizedMessage());
            }
        });

        Call<TmdbCreditsResponseDto> creditsCall = isTv ?
                TmdbClient.getApiService().getTvCredits(tmdbId, BuildConfig.TMDB_API_KEY) :
                TmdbClient.getApiService().getMovieCredits(tmdbId, BuildConfig.TMDB_API_KEY);

        creditsCall.enqueue(new Callback<TmdbCreditsResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Response<TmdbCreditsResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    castList.setValue(response.body().getCast());
                    crewList.setValue(response.body().getCrew());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Throwable t) {}
        });
    }

    public void saveMovieAndLog(TmdbMovieDto dto, float rating, String watchedDate, String review, String note, MovieLogEntity existingLog) {
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

        repository.saveMovieAndLog(movieEntity, rating, watchedDate, review, note, existingLog);
    }

    public void deleteLog(MovieLogEntity log) {
        if (log != null) {
            repository.deleteLog(log);
        }
    }
}

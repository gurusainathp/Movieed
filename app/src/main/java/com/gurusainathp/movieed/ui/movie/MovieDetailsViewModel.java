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
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.repository.MovieRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailsViewModel extends AndroidViewModel {

    private final MovieRepository repository;
    private final MutableLiveData<TmdbMovieDto> tmdbMovieDetails = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MovieDetailsViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
    }

    public LiveData<TmdbMovieDto> getTmdbMovieDetails() {
        return tmdbMovieDetails;
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

    public void fetchMovieDetails(int tmdbId) {
        TmdbClient.getApiService().getMovieDetails(tmdbId, BuildConfig.TMDB_API_KEY)
                .enqueue(new Callback<TmdbMovieDto>() {
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

        // We insert/ensure movie is saved, then log entry
        repository.insertMovie(movieEntity);

        // Fetch or observe to get movieId, or insert log with matching tmdbId
        long now = System.currentTimeMillis();
        if (existingLog != null) {
            existingLog.setRating(rating);
            existingLog.setWatchedDate(watchedDate);
            existingLog.setReview(review);
            existingLog.setPrivateNote(note);
            existingLog.setUpdatedAt(now);
            repository.updateLog(existingLog);
        } else {
            // New log
            MovieLogEntity newLog = new MovieLogEntity(
                    dto.getId(), // Using tmdbId as movieId relation key
                    watchedDate,
                    rating,
                    review,
                    note,
                    now,
                    now
            );
            repository.insertLog(newLog);
        }
    }

    public void deleteLog(MovieLogEntity log) {
        if (log != null) {
            repository.deleteLog(log);
        }
    }
}

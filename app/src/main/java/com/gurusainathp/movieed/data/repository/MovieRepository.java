package com.gurusainathp.movieed.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.gurusainathp.movieed.data.local.dao.MovieDao;
import com.gurusainathp.movieed.data.local.dao.MovieLogDao;
import com.gurusainathp.movieed.data.local.database.AppDatabase;
import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MovieRepository {

    private final MovieDao movieDao;
    private final MovieLogDao movieLogDao;
    private final LiveData<List<MovieEntity>> allMovies;
    private final LiveData<List<MovieLogEntity>> allLogs;
    private final LiveData<List<MovieLogWithDetails>> allLogsWithDetails;
    private final ExecutorService executor;

    public MovieRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        movieDao = database.movieDao();
        movieLogDao = database.movieLogDao();
        allMovies = movieDao.getAllMovies();
        allLogs = movieLogDao.getAllLogs();
        allLogsWithDetails = movieLogDao.getAllLogsWithDetails();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<MovieEntity>> getAllMovies() {
        return allMovies;
    }

    public LiveData<List<MovieLogEntity>> getAllLogs() {
        return allLogs;
    }

    public LiveData<List<MovieLogWithDetails>> getAllLogsWithDetails() {
        return allLogsWithDetails;
    }

    public LiveData<Long> getFirstLogTimestamp() {
        return movieLogDao.getFirstLogTimestamp();
    }

    public LiveData<MovieEntity> getMovieByTmdbId(int tmdbId) {
        return movieDao.getMovieByTmdbId(tmdbId);
    }

    public LiveData<List<MovieLogEntity>> getLogsForMovie(long movieId) {
        return movieLogDao.getLogsForMovie(movieId);
    }

    public void toggleWatchlist(MovieEntity movie, MovieLogEntity existingLog) {
        executor.execute(() -> {
            movieDao.insert(movie);
            long now = System.currentTimeMillis();
            if (existingLog != null) {
                existingLog.setWatchlist(!existingLog.isWatchlist());
                existingLog.setUpdatedAt(now);

                if (existingLog.getReaction() == null && !existingLog.isWatchlist()) {
                    movieLogDao.delete(existingLog);
                } else {
                    movieLogDao.update(existingLog);
                }
            } else {
                MovieLogEntity newLog = new MovieLogEntity(
                        movie.getTmdbId(),
                        null,
                        true,
                        now,
                        now
                );
                movieLogDao.insert(newLog);
            }
        });
    }

    public void saveReaction(MovieEntity movie, String reaction, MovieLogEntity existingLog) {
        executor.execute(() -> {
            movieDao.insert(movie);
            long now = System.currentTimeMillis();
            if (existingLog != null) {
                if (existingLog.isWatchlist() && existingLog.getReaction() == null && reaction != null) {
                    existingLog.setWatchlist(false);
                }
                existingLog.setReaction(reaction);
                existingLog.setUpdatedAt(now);

                if (existingLog.getReaction() == null && !existingLog.isWatchlist()) {
                    movieLogDao.delete(existingLog);
                } else {
                    movieLogDao.update(existingLog);
                }
            } else {
                if (reaction != null) {
                    MovieLogEntity newLog = new MovieLogEntity(
                            movie.getTmdbId(),
                            reaction,
                            false,
                            now,
                            now
                    );
                    movieLogDao.insert(newLog);
                }
            }
        });
    }

    public void insertMovie(MovieEntity movie) {
        executor.execute(() -> movieDao.insert(movie));
    }

    public void updateMovie(MovieEntity movie) {
        executor.execute(() -> movieDao.update(movie));
    }

    public void deleteMovie(MovieEntity movie) {
        executor.execute(() -> movieDao.delete(movie));
    }

    public void insertLog(MovieLogEntity log) {
        executor.execute(() -> movieLogDao.insert(log));
    }

    public void updateLog(MovieLogEntity log) {
        executor.execute(() -> movieLogDao.update(log));
    }

    public void deleteLog(MovieLogEntity log) {
        executor.execute(() -> movieLogDao.delete(log));
    }
}

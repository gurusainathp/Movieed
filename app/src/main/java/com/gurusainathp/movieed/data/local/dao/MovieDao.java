package com.gurusainathp.movieed.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;

import java.util.List;

@Dao
public interface MovieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MovieEntity movie);

    @Update
    void update(MovieEntity movie);

    @Delete
    void delete(MovieEntity movie);

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    LiveData<MovieEntity> getMovieById(long id);

    @Query("SELECT * FROM movies WHERE tmdbId = :tmdbId LIMIT 1")
    LiveData<MovieEntity> getMovieByTmdbId(int tmdbId);

    @Query("SELECT * FROM movies ORDER BY title ASC")
    LiveData<List<MovieEntity>> getAllMovies();
}

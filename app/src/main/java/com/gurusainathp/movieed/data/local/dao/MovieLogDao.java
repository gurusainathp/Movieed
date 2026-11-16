package com.gurusainathp.movieed.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface MovieLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MovieLogEntity log);

    @Update
    void update(MovieLogEntity log);

    @Delete
    void delete(MovieLogEntity log);

    @Query("SELECT * FROM movie_logs WHERE id = :id LIMIT 1")
    LiveData<MovieLogEntity> getLogById(long id);

    @Query("SELECT * FROM movie_logs WHERE movieId = :movieId ORDER BY createdAt DESC")
    LiveData<List<MovieLogEntity>> getLogsForMovie(long movieId);

    @Query("SELECT * FROM movie_logs ORDER BY createdAt DESC")
    LiveData<List<MovieLogEntity>> getAllLogs();

    @Transaction
    @Query("SELECT * FROM movie_logs ORDER BY createdAt DESC")
    LiveData<List<MovieLogWithDetails>> getAllLogsWithDetails();
}

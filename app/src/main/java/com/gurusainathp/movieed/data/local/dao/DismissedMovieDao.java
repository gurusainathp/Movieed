package com.gurusainathp.movieed.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.gurusainathp.movieed.data.local.entity.DismissedMovieEntity;

import java.util.List;

@Dao
public interface DismissedMovieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DismissedMovieEntity item);

    @Query("SELECT tmdbId FROM dismissed_movies")
    List<Integer> getAllDismissedMovieIds();
}

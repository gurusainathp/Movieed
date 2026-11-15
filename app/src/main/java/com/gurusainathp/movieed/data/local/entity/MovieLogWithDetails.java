package com.gurusainathp.movieed.data.local.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

public class MovieLogWithDetails {

    @Embedded
    public MovieLogEntity log;

    @Relation(
            parentColumn = "movieId",
            entityColumn = "tmdbId"
    )
    public MovieEntity movie;
}

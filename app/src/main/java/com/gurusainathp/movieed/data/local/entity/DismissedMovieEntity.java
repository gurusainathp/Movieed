package com.gurusainathp.movieed.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dismissed_movies")
public class DismissedMovieEntity {

    @PrimaryKey
    private int tmdbId;
    private long dismissedAt;

    public DismissedMovieEntity(int tmdbId, long dismissedAt) {
        this.tmdbId = tmdbId;
        this.dismissedAt = dismissedAt;
    }

    public int getTmdbId() { return tmdbId; }
    public void setTmdbId(int tmdbId) { this.tmdbId = tmdbId; }

    public long getDismissedAt() { return dismissedAt; }
    public void setDismissedAt(long dismissedAt) { this.dismissedAt = dismissedAt; }
}

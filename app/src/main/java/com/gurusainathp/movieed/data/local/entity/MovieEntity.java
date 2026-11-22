package com.gurusainathp.movieed.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "movies")
public class MovieEntity {

    @PrimaryKey
    private int tmdbId;
    private String title;
    private String overview;
    private String releaseDate;
    private String posterPath;
    private String backdropPath;
    private int runtime;
    private long lastUpdated;
    private String genreIds;
    private String originalLanguage;

    public MovieEntity(int tmdbId, String title, String overview, String releaseDate,
                       String posterPath, String backdropPath, int runtime, long lastUpdated,
                       String genreIds, String originalLanguage) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.runtime = runtime;
        this.lastUpdated = lastUpdated;
        this.genreIds = genreIds;
        this.originalLanguage = originalLanguage;
    }

    @Ignore
    public MovieEntity(int tmdbId, String title, String overview, String releaseDate,
                       String posterPath, String backdropPath, int runtime, long lastUpdated) {
        this(tmdbId, title, overview, releaseDate, posterPath, backdropPath, runtime, lastUpdated, null, null);
    }

    public int getTmdbId() { return tmdbId; }
    public void setTmdbId(int tmdbId) { this.tmdbId = tmdbId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }

    public String getBackdropPath() { return backdropPath; }
    public void setBackdropPath(String backdropPath) { this.backdropPath = backdropPath; }

    public int getRuntime() { return runtime; }
    public void setRuntime(int runtime) { this.runtime = runtime; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getGenreIds() { return genreIds; }
    public void setGenreIds(String genreIds) { this.genreIds = genreIds; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieEntity that = (MovieEntity) o;
        return tmdbId == that.tmdbId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tmdbId);
    }
}

package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TmdbMovieDto {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("name")
    private String name;

    @SerializedName("overview")
    private String overview;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("first_air_date")
    private String firstAirDate;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("vote_average")
    private double voteAverage;

    @SerializedName("vote_count")
    private int voteCount;

    @SerializedName("media_type")
    private String mediaType;

    @SerializedName("genre_ids")
    private List<Integer> genreIds;

    @SerializedName("original_language")
    private String originalLanguage;

    @SerializedName("runtime")
    private int runtime;

    @SerializedName("episode_run_time")
    private List<Integer> episodeRunTime;

    @SerializedName("number_of_episodes")
    private int numberOfEpisodes;

    public TmdbMovieDto() {
    }

    public TmdbMovieDto(int id, String title, String overview, String releaseDate,
                        String posterPath, String backdropPath, double voteAverage, String mediaType) {
        this(id, title, overview, releaseDate, posterPath, backdropPath, voteAverage, 0, mediaType, null, null);
    }

    public TmdbMovieDto(int id, String title, String overview, String releaseDate,
                        String posterPath, String backdropPath, double voteAverage, int voteCount,
                        String mediaType, List<Integer> genreIds, String originalLanguage) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.voteAverage = voteAverage;
        this.voteCount = voteCount;
        this.mediaType = mediaType;
        this.genreIds = genreIds;
        this.originalLanguage = originalLanguage;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title != null ? title : name;
    }

    public String getOverview() {
        return overview;
    }

    public String getReleaseDate() {
        return releaseDate != null ? releaseDate : firstAirDate;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public String getBackdropPath() {
        return backdropPath;
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public String getMediaType() {
        return mediaType;
    }

    public List<Integer> getGenreIds() {
        return genreIds;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public int getRuntime() {
        if (runtime > 0) {
            return runtime;
        }
        if (episodeRunTime != null && !episodeRunTime.isEmpty()) {
            int total = 0;
            for (Integer t : episodeRunTime) {
                if (t != null && t > 0) {
                    total += t;
                }
            }
            return total;
        }
        return 0;
    }
}

package com.gurusainathp.movieed.domain.model;

public class Movie {
    private final int id;
    private final String title;
    private final String overview;
    private final String releaseDate;
    private final String posterPath;
    private final String backdropPath;
    private final double voteAverage;
    private final String mediaType;

    public Movie(int id, String title, String overview, String releaseDate,
                 String posterPath, String backdropPath, double voteAverage, String mediaType) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.backdropPath = backdropPath;
        this.voteAverage = voteAverage;
        this.mediaType = mediaType;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getOverview() { return overview; }
    public String getReleaseDate() { return releaseDate; }
    public String getPosterPath() { return posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public double getVoteAverage() { return voteAverage; }
    public String getMediaType() { return mediaType; }

    public String getFullPosterUrl() {
        if (posterPath == null || posterPath.isEmpty()) {
            return null;
        }
        return "https://image.tmdb.org/t/p/w500" + posterPath;
    }
}

package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbPersonCastCreditDto {

    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("name")
    private String name;

    @SerializedName("overview")
    private String overview;

    @SerializedName("character")
    private String character;

    @SerializedName("job")
    private String job;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("first_air_date")
    private String firstAirDate;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("media_type")
    private String mediaType;

    @SerializedName("vote_average")
    private double voteAverage;

    public int getId() { return id; }
    public String getTitle() { return title != null ? title : name; }
    public String getOverview() { return overview != null ? overview : ""; }
    public String getCharacter() {
        if (character != null && !character.isEmpty()) {
            return character;
        }
        return job != null ? job : "";
    }
    public String getReleaseDate() { return releaseDate != null ? releaseDate : firstAirDate; }
    public String getPosterPath() { return posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public String getMediaType() { return mediaType; }
    public double getVoteAverage() { return voteAverage; }
}

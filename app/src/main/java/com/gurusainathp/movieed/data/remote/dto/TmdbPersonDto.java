package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbPersonDto {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("biography")
    private String biography;

    @SerializedName("birthday")
    private String birthday;

    @SerializedName("place_of_birth")
    private String placeOfBirth;

    @SerializedName("profile_path")
    private String profilePath;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getBiography() { return biography; }
    public String getBirthday() { return birthday; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public String getProfilePath() { return profilePath; }
}

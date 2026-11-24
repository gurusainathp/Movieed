package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbCastDto {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("character")
    private String character;

    @SerializedName("profile_path")
    private String profilePath;

    @SerializedName("gender")
    private int gender;

    public TmdbCastDto() {}

    public TmdbCastDto(int id, String name, String character, String profilePath, int gender) {
        this.id = id;
        this.name = name;
        this.character = character;
        this.profilePath = profilePath;
        this.gender = gender;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCharacter() { return character; }
    public String getProfilePath() { return profilePath; }
    public int getGender() { return gender; }
}

package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbGenreDto {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    public int getId() { return id; }
    public String getName() { return name; }
}

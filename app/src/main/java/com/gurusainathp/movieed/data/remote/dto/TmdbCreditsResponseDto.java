package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbCreditsResponseDto {

    @SerializedName("id")
    private int id;

    @SerializedName("cast")
    private List<TmdbCastDto> cast;

    @SerializedName("crew")
    private List<TmdbCrewDto> crew;

    public int getId() { return id; }
    public List<TmdbCastDto> getCast() { return cast; }
    public List<TmdbCrewDto> getCrew() { return crew; }
}

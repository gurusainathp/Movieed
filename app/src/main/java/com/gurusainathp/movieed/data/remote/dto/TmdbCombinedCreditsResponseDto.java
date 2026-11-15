package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbCombinedCreditsResponseDto {

    @SerializedName("id")
    private int id;

    @SerializedName("cast")
    private List<TmdbPersonCastCreditDto> cast;

    @SerializedName("crew")
    private List<TmdbPersonCastCreditDto> crew;

    public int getId() { return id; }
    public List<TmdbPersonCastCreditDto> getCast() { return cast; }
    public List<TmdbPersonCastCreditDto> getCrew() { return crew; }
}

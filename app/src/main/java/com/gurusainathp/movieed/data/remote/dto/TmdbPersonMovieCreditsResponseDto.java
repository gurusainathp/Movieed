package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbPersonMovieCreditsResponseDto {

    @SerializedName("id")
    private int id;

    @SerializedName("cast")
    private List<TmdbMovieDto> cast;

    public int getId() { return id; }
    public List<TmdbMovieDto> getCast() { return cast; }
}

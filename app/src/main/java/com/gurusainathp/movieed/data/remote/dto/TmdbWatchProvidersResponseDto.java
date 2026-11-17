package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class TmdbWatchProvidersResponseDto {

    @SerializedName("id")
    private int id;

    @SerializedName("results")
    private Map<String, TmdbCountryWatchProvidersDto> results;

    public int getId() { return id; }
    public Map<String, TmdbCountryWatchProvidersDto> getResults() { return results; }
}

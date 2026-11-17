package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TmdbCountryWatchProvidersDto {

    @SerializedName("flatrate")
    private List<TmdbProviderDto> flatrate;

    @SerializedName("rent")
    private List<TmdbProviderDto> rent;

    @SerializedName("buy")
    private List<TmdbProviderDto> buy;

    public List<TmdbProviderDto> getFlatrate() { return flatrate; }
    public List<TmdbProviderDto> getRent() { return rent; }
    public List<TmdbProviderDto> getBuy() { return buy; }
}

package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbProviderDto {

    @SerializedName("provider_id")
    private int providerId;

    @SerializedName("provider_name")
    private String providerName;

    @SerializedName("logo_path")
    private String logoPath;

    public int getProviderId() { return providerId; }
    public String getProviderName() { return providerName; }
    public String getLogoPath() { return logoPath; }
}

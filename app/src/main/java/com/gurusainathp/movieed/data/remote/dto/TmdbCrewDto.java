package com.gurusainathp.movieed.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TmdbCrewDto {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("job")
    private String job;

    @SerializedName("department")
    private String department;

    @SerializedName("profile_path")
    private String profilePath;

    @SerializedName("gender")
    private int gender;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getJob() { return job; }
    public String getDepartment() { return department; }
    public String getProfilePath() { return profilePath; }
    public int getGender() { return gender; }
}

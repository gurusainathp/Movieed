package com.gurusainathp.movieed.data.remote.api;

import com.gurusainathp.movieed.data.remote.dto.TmdbCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonMovieCreditsResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApiService {

    @GET("search/multi")
    Call<TmdbMovieResponseDto> searchMulti(
            @Query("api_key") String apiKey,
            @Query("query") String query,
            @Query("page") int page
    );

    @GET("movie/{movie_id}")
    Call<TmdbMovieDto> getMovieDetails(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey
    );

    @GET("tv/{tv_id}")
    Call<TmdbMovieDto> getTvDetails(
            @Path("tv_id") int tvId,
            @Query("api_key") String apiKey
    );

    @GET("movie/{movie_id}/credits")
    Call<TmdbCreditsResponseDto> getMovieCredits(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey
    );

    @GET("tv/{tv_id}/credits")
    Call<TmdbCreditsResponseDto> getTvCredits(
            @Path("tv_id") int tvId,
            @Query("api_key") String apiKey
    );

    @GET("person/{person_id}")
    Call<TmdbPersonDto> getPersonDetails(
            @Path("person_id") int personId,
            @Query("api_key") String apiKey
    );

    @GET("person/{person_id}/movie_credits")
    Call<TmdbPersonMovieCreditsResponseDto> getPersonMovieCredits(
            @Path("person_id") int personId,
            @Query("api_key") String apiKey
    );
}

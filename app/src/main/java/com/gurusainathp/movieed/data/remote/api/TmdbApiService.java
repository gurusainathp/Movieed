package com.gurusainathp.movieed.data.remote.api;

import com.gurusainathp.movieed.data.remote.dto.TmdbCombinedCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbWatchProvidersResponseDto;

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

    @GET("trending/movie/day")
    Call<TmdbMovieResponseDto> getTrendingMovies(
            @Query("api_key") String apiKey
    );

    @GET("movie/top_rated")
    Call<TmdbMovieResponseDto> getTopRatedMovies(
            @Query("api_key") String apiKey,
            @Query("page") int page
    );

    @GET("movie/now_playing")
    Call<TmdbMovieResponseDto> getNowPlayingMovies(
            @Query("api_key") String apiKey,
            @Query("page") int page
    );

    @GET("movie/popular")
    Call<TmdbMovieResponseDto> getPopularMovies(
            @Query("api_key") String apiKey,
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

    @GET("movie/{movie_id}/watch/providers")
    Call<TmdbWatchProvidersResponseDto> getMovieWatchProviders(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey
    );

    @GET("tv/{tv_id}/watch/providers")
    Call<TmdbWatchProvidersResponseDto> getTvWatchProviders(
            @Path("tv_id") int tvId,
            @Query("api_key") String apiKey
    );

    @GET("person/{person_id}")
    Call<TmdbPersonDto> getPersonDetails(
            @Path("person_id") int personId,
            @Query("api_key") String apiKey
    );

    @GET("person/{person_id}/combined_credits")
    Call<TmdbCombinedCreditsResponseDto> getCombinedCredits(
            @Path("person_id") int personId,
            @Query("api_key") String apiKey
    );
}

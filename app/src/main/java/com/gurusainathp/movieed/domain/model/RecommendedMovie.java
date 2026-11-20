package com.gurusainathp.movieed.domain.model;

import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;

import java.util.ArrayList;
import java.util.List;

public class RecommendedMovie {

    private final TmdbMovieDto movie;
    private final float score;
    private final List<String> reasons;

    public RecommendedMovie(TmdbMovieDto movie, float score, List<String> reasons) {
        this.movie = movie;
        this.score = score;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
    }

    public TmdbMovieDto getMovie() {
        return movie;
    }

    public float getScore() {
        return score;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public String getPrimaryReason() {
        if (!reasons.isEmpty()) {
            return reasons.get(0);
        }
        return "Recommended for you";
    }
}

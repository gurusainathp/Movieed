package com.gurusainathp.movieed.domain.model;

import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;

import java.util.ArrayList;
import java.util.List;

public class RecommendedMovie {

    public enum SourceType {
        NONE,
        MOVIE,
        PERSON
    }

    private final TmdbMovieDto movie;
    private final float score;
    private final List<String> reasons;
    private String reasonPosterPath;

    private SourceType sourceType = SourceType.NONE;
    private int sourceId = -1;
    private String sourceName;

    public RecommendedMovie(TmdbMovieDto movie, float score, List<String> reasons) {
        this.movie = movie;
        this.score = score;
        this.reasons = reasons != null ? reasons : new ArrayList<>();
    }

    public RecommendedMovie(TmdbMovieDto movie, float score, List<String> reasons, String reasonPosterPath) {
        this(movie, score, reasons);
        this.reasonPosterPath = reasonPosterPath;
    }

    public RecommendedMovie(TmdbMovieDto movie, float score, List<String> reasons, String reasonPosterPath,
                            SourceType sourceType, int sourceId, String sourceName) {
        this(movie, score, reasons, reasonPosterPath);
        this.sourceType = sourceType != null ? sourceType : SourceType.NONE;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
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

    public String getReasonPosterPath() {
        return reasonPosterPath != null ? reasonPosterPath : (movie != null ? movie.getPosterPath() : null);
    }

    public void setReasonPosterPath(String reasonPosterPath) {
        this.reasonPosterPath = reasonPosterPath;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public void addReason(String reason) {
        if (reason != null && !reasons.contains(reason)) {
            reasons.add(reason);
        }
    }
}

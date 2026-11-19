package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecommendationEngine {

    public static float getReactionWeight(String reaction) {
        if (reaction == null) return 0f;
        switch (reaction) {
            case "LOVE": return 3.0f;
            case "LIKE": return 1.5f;
            case "UMM": return 0.0f;
            case "DISLIKE": return -1.5f;
            case "HATE": return -3.0f;
            default: return 0f;
        }
    }

    public List<TmdbMovieDto> filterAndRankRecommendations(List<TmdbMovieDto> rawRecommendations,
                                                           List<MovieLogWithDetails> userLogs) {
        if (rawRecommendations == null || rawRecommendations.isEmpty()) {
            return new ArrayList<>();
        }

        // Collect IDs of movies the user has already logged or watched
        Set<Long> loggedMovieIds = new HashSet<>();
        Map<Integer, Float> genreWeights = new HashMap<>();

        if (userLogs != null) {
            for (MovieLogWithDetails item : userLogs) {
                if (item.log != null) {
                    loggedMovieIds.add(item.log.getMovieId());
                }
            }
        }

        List<ScoredMovie> scoredMovies = new ArrayList<>();

        for (TmdbMovieDto dto : rawRecommendations) {
            if (dto == null) continue;

            // Skip if user already logged/watched this film
            if (loggedMovieIds.contains((long) dto.getId())) {
                continue;
            }

            // Calculate base recommendation score: TMDB rating + vote count weight
            float score = (float) dto.getVoteAverage();

            // Bonus for highly rated TMDB films
            if (dto.getVoteAverage() >= 7.5) {
                score += 1.5f;
            }

            scoredMovies.add(new ScoredMovie(dto, score));
        }

        // Sort movies by recommendation score descending
        Collections.sort(scoredMovies, (a, b) -> Float.compare(b.score, a.score));

        List<TmdbMovieDto> result = new ArrayList<>();
        for (ScoredMovie sm : scoredMovies) {
            result.add(sm.dto);
        }

        return result;
    }

    private static class ScoredMovie {
        final TmdbMovieDto dto;
        final float score;

        ScoredMovie(TmdbMovieDto dto, float score) {
            this.dto = dto;
            this.score = score;
        }
    }
}

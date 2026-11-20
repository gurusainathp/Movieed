package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.dto.TmdbCastDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;

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

    public static class UserTasteProfile {
        public final Set<Long> loggedMovieIds = new HashSet<>();
        public final List<MovieEntity> lovedMovies = new ArrayList<>();
        public final Map<Integer, Float> personAffinity = new HashMap<>(); // personId -> weight
        public final Map<String, String> personNames = new HashMap<>(); // personId -> Name

        public static UserTasteProfile build(List<MovieLogWithDetails> logs) {
            UserTasteProfile profile = new UserTasteProfile();
            if (logs == null) return profile;

            for (MovieLogWithDetails item : logs) {
                if (item.log == null) continue;
                profile.loggedMovieIds.add(item.log.getMovieId());

                float weight = getReactionWeight(item.log.getReaction());
                if (item.movie != null) {
                    if (weight >= 1.5f) {
                        profile.lovedMovies.add(item.movie);
                    }
                }
            }
            return profile;
        }

        public void addPersonAffinity(int personId, String name, float weight) {
            float current = personAffinity.containsKey(personId) ? personAffinity.get(personId) : 0f;
            personAffinity.put(personId, current + weight);
            if (name != null) {
                personNames.put(String.valueOf(personId), name);
            }
        }
    }

    public List<RecommendedMovie> rankCandidateMovies(
            List<TmdbMovieDto> candidates,
            UserTasteProfile profile,
            String sourceMovieTitle,
            String sourcePosterPath,
            List<TmdbCastDto> sourceCast,
            List<TmdbCrewDto> sourceCrew) {

        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        List<RecommendedMovie> scoredList = new ArrayList<>();

        for (TmdbMovieDto candidate : candidates) {
            if (candidate == null) continue;

            if (profile != null && profile.loggedMovieIds.contains((long) candidate.getId())) {
                continue;
            }

            float score = (float) candidate.getVoteAverage();
            List<String> reasons = new ArrayList<>();
            String reasonPoster = null;

            if (sourceMovieTitle != null && !sourceMovieTitle.isEmpty()) {
                reasons.add("Because you loved " + sourceMovieTitle);
                reasonPoster = sourcePosterPath;
                score += 2.0f;
            }

            if (candidate.getVoteAverage() >= 7.5) {
                score += 1.5f;
                reasons.add("Critically acclaimed (" + String.format(java.util.Locale.US, "%.1f", candidate.getVoteAverage()) + "★)");
            }

            if (reasons.isEmpty()) {
                reasons.add("Trending pick for you");
            }

            scoredList.add(new RecommendedMovie(candidate, score, reasons, reasonPoster));
        }

        Collections.sort(scoredList, (a, b) -> Float.compare(b.getScore(), a.getScore()));
        return scoredList;
    }

    public List<TmdbMovieDto> filterAndRankRecommendations(
            List<TmdbMovieDto> rawRecommendations,
            List<MovieLogWithDetails> userLogs) {

        UserTasteProfile profile = UserTasteProfile.build(userLogs);
        List<RecommendedMovie> ranked = rankCandidateMovies(rawRecommendations, profile, null, null, null, null);

        List<TmdbMovieDto> result = new ArrayList<>();
        for (RecommendedMovie rm : ranked) {
            result.add(rm.getMovie());
        }
        return result;
    }
}

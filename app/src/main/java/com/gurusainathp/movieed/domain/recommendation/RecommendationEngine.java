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
import java.util.Random;
import java.util.Set;

public class RecommendationEngine {

    private final Random random = new Random();

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
        public final List<MovieEntity> likedMovies = new ArrayList<>();
        public final Map<Integer, Float> personAffinity = new HashMap<>(); // personId -> weight
        public final Map<String, String> personNames = new HashMap<>(); // personIdStr -> Name

        public static UserTasteProfile build(List<MovieLogWithDetails> logs) {
            UserTasteProfile profile = new UserTasteProfile();
            if (logs == null) return profile;

            for (MovieLogWithDetails item : logs) {
                if (item.log == null) continue;
                profile.loggedMovieIds.add(item.log.getMovieId());

                float weight = getReactionWeight(item.log.getReaction());
                if (item.movie != null) {
                    if (weight >= 3.0f) {
                        profile.lovedMovies.add(item.movie);
                    } else if (weight >= 1.5f) {
                        profile.likedMovies.add(item.movie);
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

    public List<RecommendedMovie> aggregateMultiSeedRecommendations(
            Map<MovieEntity, List<TmdbMovieDto>> seedToRecommendationsMap,
            UserTasteProfile profile) {

        if (seedToRecommendationsMap == null || seedToRecommendationsMap.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, MultiSeedCandidate> candidateMap = new HashMap<>();

        for (Map.Entry<MovieEntity, List<TmdbMovieDto>> entry : seedToRecommendationsMap.entrySet()) {
            MovieEntity seedMovie = entry.getKey();
            List<TmdbMovieDto> candidates = entry.getValue();

            if (candidates == null) continue;

            for (TmdbMovieDto candidate : candidates) {
                if (candidate == null) continue;

                if (profile != null && profile.loggedMovieIds.contains((long) candidate.getId())) {
                    continue;
                }

                MultiSeedCandidate candidateRecord = candidateMap.get(candidate.getId());
                if (candidateRecord == null) {
                    candidateRecord = new MultiSeedCandidate(candidate);
                    candidateMap.put(candidate.getId(), candidateRecord);
                }

                candidateRecord.addSeedMatch(seedMovie);
            }
        }

        List<RecommendedMovie> scoredList = new ArrayList<>();

        for (MultiSeedCandidate record : candidateMap.values()) {
            float score = (float) record.dto.getVoteAverage();
            List<String> reasons = new ArrayList<>();
            String primaryPoster = null;

            int matchCount = record.matchingSeeds.size();
            if (matchCount > 1) {
                score += (matchCount - 1) * 3.0f;
            }

            if (!record.matchingSeeds.isEmpty()) {
                MovieEntity topSeed = record.matchingSeeds.get(0);
                reasons.add("Because you loved " + topSeed.getTitle());
                primaryPoster = topSeed.getPosterPath();
                score += 3.0f;
            }

            if (record.dto.getVoteAverage() >= 7.8) {
                score += 1.8f;
                reasons.add("Critically acclaimed (" + String.format(java.util.Locale.US, "%.1f", record.dto.getVoteAverage()) + "★)");
            }

            score += random.nextFloat() * 0.8f;

            scoredList.add(new RecommendedMovie(record.dto, score, reasons, primaryPoster));
        }

        Collections.sort(scoredList, (a, b) -> Float.compare(b.getScore(), a.getScore()));
        return scoredList;
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
                score += 3.0f;
            }

            if (candidate.getVoteAverage() >= 7.8) {
                score += 1.8f;
                reasons.add("Critically acclaimed (" + String.format(java.util.Locale.US, "%.1f", candidate.getVoteAverage()) + "★)");
            }

            score += random.nextFloat() * 1.0f;

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

    private static class MultiSeedCandidate {
        final TmdbMovieDto dto;
        final List<MovieEntity> matchingSeeds = new ArrayList<>();

        MultiSeedCandidate(TmdbMovieDto dto) {
            this.dto = dto;
        }

        void addSeedMatch(MovieEntity seed) {
            if (seed != null && !matchingSeeds.contains(seed)) {
                matchingSeeds.add(seed);
            }
        }
    }
}

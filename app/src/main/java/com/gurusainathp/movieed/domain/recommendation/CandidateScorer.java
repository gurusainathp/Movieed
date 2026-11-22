package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.remote.dto.TmdbCastDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.Reaction;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CandidateScorer {

    private final Random random;
    private final RecommendationConfig config;

    public CandidateScorer() {
        this(new Random(), new RecommendationConfig());
    }

    public CandidateScorer(Random random) {
        this(random, new RecommendationConfig());
    }

    public CandidateScorer(Random random, RecommendationConfig config) {
        this.random = random != null ? random : new Random();
        this.config = config != null ? config : new RecommendationConfig();
    }

    public RecommendationConfig getConfig() {
        return config;
    }

    public float calculateDampedQuality(double voteAvg, int voteCount) {
        double m = config.minVoteThreshold;
        double C = config.globalAverageVote;
        return (float) ((voteCount * voteAvg + m * C) / (voteCount + m));
    }

    public float calculateTasteMatchScore(TmdbMovieDto candidate, TasteProfile profile) {
        if (candidate == null || profile == null) return 0f;
        float score = 0f;

        if (candidate.getGenreIds() != null) {
            for (int genreId : candidate.getGenreIds()) {
                Float affinity = profile.genreAffinities.get(genreId);
                if (affinity != null) {
                    score += affinity * config.genreAffinityWeight;
                }
            }
        }

        if (candidate.getOriginalLanguage() != null) {
            Float langAffinity = profile.languageAffinities.get(candidate.getOriginalLanguage().toLowerCase());
            if (langAffinity != null) {
                score += langAffinity * config.languageAffinityWeight;
            }
        }

        return score;
    }

    public float calculateAvoidPenalty(TmdbMovieDto candidate, TasteProfile profile) {
        if (candidate == null || profile == null) return 0f;
        float penalty = 0f;

        if (profile.avoidedMovieIds.contains((long) candidate.getId())) {
            penalty += config.avoidPenalty;
        }

        if (candidate.getGenreIds() != null) {
            for (int genreId : candidate.getGenreIds()) {
                if (profile.avoidedGenreIds.contains(genreId)) {
                    penalty += config.avoidPenalty * 0.5f;
                }
            }
        }

        if (candidate.getOriginalLanguage() != null) {
            if (profile.avoidedLanguages.contains(candidate.getOriginalLanguage().toLowerCase())) {
                penalty += config.avoidPenalty * 0.5f;
            }
        }

        return penalty;
    }

    public List<RecommendedMovie> aggregateMultiSeedRecommendations(
            Map<MovieEntity, List<TmdbMovieDto>> seedToRecommendationsMap,
            TasteProfile profile) {

        if (seedToRecommendationsMap == null || seedToRecommendationsMap.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, MultiSeedRecord> candidateMap = new HashMap<>();

        for (Map.Entry<MovieEntity, List<TmdbMovieDto>> entry : seedToRecommendationsMap.entrySet()) {
            MovieEntity seedMovie = entry.getKey();
            List<TmdbMovieDto> candidates = entry.getValue();

            if (candidates == null) continue;

            for (int rank = 0; rank < candidates.size(); rank++) {
                TmdbMovieDto candidate = candidates.get(rank);
                if (candidate == null) continue;

                if (profile != null && profile.loggedMovieIds.contains((long) candidate.getId())) {
                    continue;
                }

                MultiSeedRecord record = candidateMap.get(candidate.getId());
                if (record == null) {
                    record = new MultiSeedRecord(candidate);
                    candidateMap.put(candidate.getId(), record);
                }

                record.addSeedMatch(seedMovie, rank);
            }
        }

        List<RecommendedMovie> scoredList = new ArrayList<>();

        for (MultiSeedRecord record : candidateMap.values()) {
            float dampedQuality = calculateDampedQuality(record.dto.getVoteAverage(), record.dto.getVoteCount());
            float tasteScore = calculateTasteMatchScore(record.dto, profile);
            float avoidPenalty = calculateAvoidPenalty(record.dto, profile);

            float seedSignal = 0f;
            for (Map.Entry<MovieEntity, Integer> seedEntry : record.seedRanks.entrySet()) {
                MovieEntity seed = seedEntry.getKey();
                int rank = seedEntry.getValue();

                float reactionWeight = 3.0f; // default Love weight
                if (profile != null) {
                    for (MovieEntity loved : profile.lovedMovies) {
                        if (loved.getTmdbId() == seed.getTmdbId()) {
                            reactionWeight = 3.0f;
                            break;
                        }
                    }
                    for (MovieEntity liked : profile.likedMovies) {
                        if (liked.getTmdbId() == seed.getTmdbId()) {
                            reactionWeight = 1.5f;
                            break;
                        }
                    }
                }

                float rankBonus = Math.max(0f, (20f - rank) / 20f) * config.rankPositionBonusMax;
                seedSignal += (reactionWeight * config.seedWeightMultiplier) + rankBonus;
            }

            int matchCount = record.matchingSeeds.size();
            if (matchCount > 1) {
                seedSignal += (matchCount - 1) * config.overlapBonusPerSeed;
            }

            float noise = random.nextFloat() * config.tieBreakerMaxNoise;
            float totalScore = tasteScore + seedSignal + (dampedQuality * config.qualityWeight) + avoidPenalty + noise;

            List<String> reasons = generateSeedReasons(record.matchingSeeds, profile);
            String primaryPoster = !record.matchingSeeds.isEmpty() ? record.matchingSeeds.get(0).getPosterPath() : null;

            if (record.dto.getVoteAverage() >= 7.8) {
                reasons.add("Critically acclaimed (" + String.format(java.util.Locale.US, "%.1f", record.dto.getVoteAverage()) + "★)");
            }

            RecommendedMovie rm = new RecommendedMovie(
                    record.dto,
                    totalScore,
                    reasons,
                    primaryPoster,
                    RecommendedMovie.Category.PERSONAL,
                    RecommendedMovie.SourceType.MOVIE,
                    !record.matchingSeeds.isEmpty() ? record.matchingSeeds.get(0).getTmdbId() : record.dto.getId(),
                    !record.matchingSeeds.isEmpty() ? record.matchingSeeds.get(0).getTitle() : record.dto.getTitle()
            );
            scoredList.add(rm);
        }

        Collections.sort(scoredList, (a, b) -> Float.compare(b.getScore(), a.getScore()));
        return scoredList;
    }

    public List<String> generateSeedReasons(List<MovieEntity> seeds, TasteProfile profile) {
        List<String> reasons = new ArrayList<>();
        if (seeds == null || seeds.isEmpty()) {
            reasons.add("Recommended for you");
            return reasons;
        }

        List<MovieEntity> lovedInSeeds = new ArrayList<>();
        List<MovieEntity> likedInSeeds = new ArrayList<>();

        for (MovieEntity seed : seeds) {
            boolean isLove = false;
            if (profile != null) {
                for (MovieEntity loved : profile.lovedMovies) {
                    if (loved.getTmdbId() == seed.getTmdbId()) {
                        lovedInSeeds.add(seed);
                        isLove = true;
                        break;
                    }
                }
            }
            if (!isLove) {
                likedInSeeds.add(seed);
            }
        }

        if (!lovedInSeeds.isEmpty()) {
            if (lovedInSeeds.size() >= 2) {
                reasons.add("Because you loved " + lovedInSeeds.get(0).getTitle() + " and " + lovedInSeeds.get(1).getTitle());
            } else {
                reasons.add("Because you loved " + lovedInSeeds.get(0).getTitle());
            }
        } else if (!likedInSeeds.isEmpty()) {
            if (likedInSeeds.size() >= 2) {
                reasons.add("Because you liked " + likedInSeeds.get(0).getTitle() + " and " + likedInSeeds.get(1).getTitle());
            } else {
                reasons.add("Because you liked " + likedInSeeds.get(0).getTitle());
            }
        } else {
            reasons.add("Because you loved " + seeds.get(0).getTitle());
        }

        return reasons;
    }

    public List<RecommendedMovie> rankCandidateMovies(
            List<TmdbMovieDto> candidates,
            TasteProfile profile,
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

            float dampedQuality = calculateDampedQuality(candidate.getVoteAverage(), candidate.getVoteCount());
            float tasteScore = calculateTasteMatchScore(candidate, profile);
            float avoidPenalty = calculateAvoidPenalty(candidate, profile);
            float noise = random.nextFloat() * config.tieBreakerMaxNoise;

            float score = tasteScore + (dampedQuality * config.qualityWeight) + avoidPenalty + noise;
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

            if (reasons.isEmpty()) {
                reasons.add("Trending pick for you");
            }

            scoredList.add(new RecommendedMovie(
                    candidate,
                    score,
                    reasons,
                    reasonPoster,
                    RecommendedMovie.Category.PERSONAL,
                    RecommendedMovie.SourceType.NONE,
                    -1,
                    null
            ));
        }

        Collections.sort(scoredList, (a, b) -> Float.compare(b.getScore(), a.getScore()));
        return scoredList;
    }

    private static class MultiSeedRecord {
        final TmdbMovieDto dto;
        final List<MovieEntity> matchingSeeds = new ArrayList<>();
        final Map<MovieEntity, Integer> seedRanks = new HashMap<>();

        MultiSeedRecord(TmdbMovieDto dto) {
            this.dto = dto;
        }

        void addSeedMatch(MovieEntity seed, int rank) {
            if (seed != null) {
                if (!matchingSeeds.contains(seed)) {
                    matchingSeeds.add(seed);
                }
                seedRanks.put(seed, rank);
            }
        }
    }
}

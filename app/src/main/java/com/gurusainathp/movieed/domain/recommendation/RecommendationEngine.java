package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.dto.TmdbCastDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.Reaction;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RecommendationEngine {

    private final Random random;
    private final CandidateScorer candidateScorer;
    private final FeedPlanner feedPlanner;

    public RecommendationEngine() {
        this(new Random());
    }

    public RecommendationEngine(Random random) {
        this.random = random != null ? random : new Random();
        this.candidateScorer = new CandidateScorer(this.random);
        this.feedPlanner = new FeedPlanner(this.random);
    }

    public static float getReactionWeight(String reaction) {
        return Reaction.getWeight(reaction);
    }

    public static class UserTasteProfile extends TasteProfile {

        public UserTasteProfile() {
            super();
        }

        public UserTasteProfile(Random random) {
            super(random);
        }

        public static UserTasteProfile build(List<MovieLogWithDetails> logs) {
            TasteProfile base = TasteProfile.build(logs);
            UserTasteProfile profile = new UserTasteProfile();
            profile.loggedMovieIds.addAll(base.loggedMovieIds);
            profile.lovedMovies.addAll(base.lovedMovies);
            profile.likedMovies.addAll(base.likedMovies);
            return profile;
        }
    }

    public List<RecommendedMovie> aggregateMultiSeedRecommendations(
            Map<MovieEntity, List<TmdbMovieDto>> seedToRecommendationsMap,
            TasteProfile profile) {
        return candidateScorer.aggregateMultiSeedRecommendations(seedToRecommendationsMap, profile);
    }

    public List<RecommendedMovie> rankCandidateMovies(
            List<TmdbMovieDto> candidates,
            TasteProfile profile,
            String sourceMovieTitle,
            String sourcePosterPath,
            List<TmdbCastDto> sourceCast,
            List<TmdbCrewDto> sourceCrew) {
        return candidateScorer.rankCandidateMovies(candidates, profile, sourceMovieTitle, sourcePosterPath, sourceCast, sourceCrew);
    }

    public List<TmdbMovieDto> filterAndRankRecommendations(
            List<TmdbMovieDto> rawRecommendations,
            List<MovieLogWithDetails> userLogs) {

        TasteProfile profile = TasteProfile.build(userLogs, random);
        List<RecommendedMovie> ranked = rankCandidateMovies(rawRecommendations, profile, null, null, null, null);

        List<TmdbMovieDto> result = new ArrayList<>();
        for (RecommendedMovie rm : ranked) {
            result.add(rm.getMovie());
        }
        return result;
    }
}

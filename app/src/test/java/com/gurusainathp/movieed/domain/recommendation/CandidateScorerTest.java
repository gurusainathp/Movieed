package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

public class CandidateScorerTest {

    private CandidateScorer scorer;
    private RecommendationConfig config;

    @Before
    public void setUp() {
        Random seededRandom = new Random(42);
        config = new RecommendationConfig();
        scorer = new CandidateScorer(seededRandom, config);
    }

    @Test
    public void testDampedRating_lowVoteHighRatingRanksBelowWellSupported() {
        float lowVoteHighAvg = scorer.calculateDampedQuality(10.0, 5);
        float highVoteGoodAvg = scorer.calculateDampedQuality(8.5, 10000);

        assertTrue("High vote count good rating should damp higher than 5-vote 10.0★ rating",
                highVoteGoodAvg > lowVoteHighAvg);
    }

    @Test
    public void testAvoidPenalty_appliedWhenInAvoidSet() {
        TmdbMovieDto candidate = new TmdbMovieDto(
                101, "Disliked Movie Sequel", "Overview", "2023",
                "/poster.jpg", "/backdrop.jpg", 7.0, 1000, "movie",
                Arrays.asList(28), "en"
        );

        TasteProfile profile = new TasteProfile();
        profile.avoidedMovieIds.add(101L);

        float penalty = scorer.calculateAvoidPenalty(candidate, profile);
        assertEquals(config.avoidPenalty, penalty, 0.001f);
    }

    @Test
    public void testReasonGeneration_reflectsRealSeedAndReaction() {
        MovieEntity lovedSeed = new MovieEntity(1, "Loved Film", "Overview", "2020", "/poster.jpg", "/backdrop.jpg", 120, System.currentTimeMillis());
        MovieEntity likedSeed = new MovieEntity(2, "Liked Film", "Overview", "2021", "/poster.jpg", "/backdrop.jpg", 110, System.currentTimeMillis());

        TasteProfile profile = new TasteProfile();
        profile.lovedMovies.add(lovedSeed);
        profile.likedMovies.add(likedSeed);

        List<String> reasonsLoved = scorer.generateSeedReasons(Arrays.asList(lovedSeed), profile);
        assertEquals("Because you loved Loved Film", reasonsLoved.get(0));

        List<String> reasonsLiked = scorer.generateSeedReasons(Arrays.asList(likedSeed), profile);
        assertEquals("Because you liked Liked Film", reasonsLiked.get(0));
    }
}

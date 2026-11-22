package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.domain.model.Reaction;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TasteProfileTest {

    @Test
    public void testRecencyDecay_decayOverHalfLife() {
        long now = 1000000000000L;
        long halfLifeMs = 180L * 24 * 60 * 60 * 1000;
        long timestamp180DaysAgo = now - halfLifeMs;

        float freshWeight = TasteProfile.calculateDecayedWeight(Reaction.LOVE, now, now, 180.0);
        float oldWeight = TasteProfile.calculateDecayedWeight(Reaction.LOVE, timestamp180DaysAgo, now, 180.0);

        assertEquals(3.0f, freshWeight, 0.001f);
        assertEquals(1.5f, oldWeight, 0.01f);
    }

    @Test
    public void testBuildProfile_dislikeAddsToAvoidedSet() {
        List<MovieLogWithDetails> logs = new ArrayList<>();

        MovieEntity movie = new MovieEntity(500, "Bad Movie", "Overview", "2020", "/p.jpg", "/b.jpg", 90, System.currentTimeMillis(), "28", "en");
        MovieLogEntity log = new MovieLogEntity(500, "HATE", false, System.currentTimeMillis(), System.currentTimeMillis());

        MovieLogWithDetails details = new MovieLogWithDetails();
        details.movie = movie;
        details.log = log;
        logs.add(details);

        TasteProfile profile = TasteProfile.build(logs);
        assertTrue(profile.avoidedMovieIds.contains(500L));
    }
}

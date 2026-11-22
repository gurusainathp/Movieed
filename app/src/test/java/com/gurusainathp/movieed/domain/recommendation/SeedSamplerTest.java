package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class SeedSamplerTest {

    private SeedSampler sampler;

    @Before
    public void setUp() {
        sampler = new SeedSampler(new Random(12345));
    }

    @Test
    public void testSampleMovieSeeds_samplesLovedPoolFirst() {
        TasteProfile profile = new TasteProfile();
        for (int i = 1; i <= 10; i++) {
            profile.lovedMovies.add(new MovieEntity(i, "Loved " + i, "Overview", "2020", "/p.jpg", "/b.jpg", 100, System.currentTimeMillis()));
        }

        List<MovieEntity> sampled = sampler.sampleMovieSeeds(profile, 3, 5);
        assertTrue(sampled.size() >= 3 && sampled.size() <= 5);
        for (MovieEntity movie : sampled) {
            assertTrue(movie.getTitle().startsWith("Loved"));
        }
    }

    @Test
    public void testSampleMovieSeeds_fallsBackToLikedWhenNoLovedExist() {
        TasteProfile profile = new TasteProfile();
        for (int i = 1; i <= 5; i++) {
            profile.likedMovies.add(new MovieEntity(i, "Liked " + i, "Overview", "2021", "/p.jpg", "/b.jpg", 110, System.currentTimeMillis()));
        }

        List<MovieEntity> sampled = sampler.sampleMovieSeeds(profile, 3, 5);
        assertFalse(sampled.isEmpty());
        for (MovieEntity movie : sampled) {
            assertTrue(movie.getTitle().startsWith("Liked"));
        }
    }
}

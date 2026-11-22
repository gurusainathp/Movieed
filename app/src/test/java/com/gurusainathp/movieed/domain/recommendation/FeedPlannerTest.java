package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class FeedPlannerTest {

    private FeedPlanner planner;

    @Before
    public void setUp() {
        planner = new FeedPlanner(new Random(777));
    }

    @Test
    public void testIsValidMovie_filtersInvalidAndFeaturettes() {
        TasteProfile profile = new TasteProfile();

        TmdbMovieDto validMovie = new TmdbMovieDto(1, "Valid Film", "Great Overview", "2022", "/p.jpg", "/b.jpg", 8.0, "movie");
        assertTrue(planner.isValidMovie(validMovie, profile));

        TmdbMovieDto makingOf = new TmdbMovieDto(2, "The Making of Valid Film", "Overview", "2022", "/p.jpg", "/b.jpg", 7.0, "movie");
        assertFalse(planner.isValidMovie(makingOf, profile));

        TmdbMovieDto noPosterNoBackdrop = new TmdbMovieDto(3, "No Art Film", "Overview", "2022", null, null, 7.0, "movie");
        assertFalse(planner.isValidMovie(noPosterNoBackdrop, profile));

        TmdbMovieDto noOverview = new TmdbMovieDto(4, "No Overview Film", "", "2022", "/p.jpg", "/b.jpg", 7.0, "movie");
        assertFalse(planner.isValidMovie(noOverview, profile));
    }
}

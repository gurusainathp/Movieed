package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class FeedInterleaverTest {

    private FeedInterleaver interleaver;

    @Before
    public void setUp() {
        interleaver = new FeedInterleaver(new Random(9999));
    }

    @Test
    public void testInterleaveByReason_emptyInputReturnsEmpty() {
        List<RecommendedMovie> result = interleaver.interleaveByReason(new ArrayList<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInterleaveByReason_sizeInvariantPreserved() {
        List<RecommendedMovie> input = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            TmdbMovieDto dto = new TmdbMovieDto(i, "Movie " + i, "Overview", "2022", "/p.jpg", "/b.jpg", 7.5, "movie");
            RecommendedMovie.Category cat = RecommendedMovie.Category.values()[i % RecommendedMovie.Category.values().length];
            RecommendedMovie rm = new RecommendedMovie(dto, 8.0f - i * 0.1f, Arrays.asList("Reason " + i), "/p.jpg", cat, RecommendedMovie.SourceType.MOVIE, i, "Source");
            input.add(rm);
        }

        List<RecommendedMovie> output = interleaver.interleaveByReason(input);
        assertEquals("Output size must equal input size", input.size(), output.size());
    }

    @Test
    public void testInterleaveByReason_singleCategoryPreserved() {
        List<RecommendedMovie> input = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            TmdbMovieDto dto = new TmdbMovieDto(i, "Classic " + i, "Overview", "1990", "/p.jpg", "/b.jpg", 8.5, "movie");
            RecommendedMovie rm = new RecommendedMovie(dto, 9.0f - i * 0.1f, Arrays.asList("Hall of Fame Classic"), "/p.jpg", RecommendedMovie.Category.CLASSIC, RecommendedMovie.SourceType.MOVIE, i, "Source");
            input.add(rm);
        }

        List<RecommendedMovie> output = interleaver.interleaveByReason(input);
        assertEquals(5, output.size());
        for (RecommendedMovie rm : output) {
            assertEquals(RecommendedMovie.Category.CLASSIC, rm.getCategory());
        }
    }
}

package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SeedSampler {

    private final Random random;

    public SeedSampler() {
        this(new Random());
    }

    public SeedSampler(Random random) {
        this.random = random != null ? random : new Random();
    }

    public List<MovieEntity> sampleMovieSeeds(TasteProfile profile, int minSeeds, int maxSeeds) {
        if (profile == null) return new ArrayList<>();

        List<MovieEntity> pool = new ArrayList<>(profile.lovedMovies);
        if (pool.isEmpty()) {
            pool.addAll(profile.likedMovies);
        }

        if (pool.isEmpty()) {
            return new ArrayList<>();
        }

        int countRange = Math.max(1, maxSeeds - minSeeds + 1);
        int targetCount = Math.min(pool.size(), minSeeds + random.nextInt(countRange));
        Collections.shuffle(pool, random);
        return new ArrayList<>(pool.subList(0, targetCount));
    }

    public List<MovieEntity> sampleDirectorSourceMovies(TasteProfile profile, int minDirectors, int maxDirectors) {
        if (profile == null) return new ArrayList<>();

        List<MovieEntity> pool = new ArrayList<>(profile.lovedMovies);
        if (pool.isEmpty()) {
            pool.addAll(profile.likedMovies);
        }

        if (pool.isEmpty()) {
            return new ArrayList<>();
        }

        int countRange = Math.max(1, maxDirectors - minDirectors + 1);
        int targetCount = Math.min(pool.size(), minDirectors + random.nextInt(countRange));
        Collections.shuffle(pool, random);
        return new ArrayList<>(pool.subList(0, targetCount));
    }
}

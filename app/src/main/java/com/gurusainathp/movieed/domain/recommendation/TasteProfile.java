package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.domain.model.Reaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TasteProfile {

    public static final double DEFAULT_HALF_LIFE_DAYS = 180.0;

    public final Set<Long> loggedMovieIds = new HashSet<>();
    public final Set<Long> avoidedMovieIds = new HashSet<>();
    public final List<MovieEntity> lovedMovies = new ArrayList<>();
    public final List<MovieEntity> likedMovies = new ArrayList<>();
    public final List<MovieEntity> positiveSeeds = new ArrayList<>();

    public final Map<Integer, Float> genreAffinities = new HashMap<>();
    public final Map<String, Float> languageAffinities = new HashMap<>();
    public final Map<String, Float> decadeAffinities = new HashMap<>();
    public final Map<Integer, Float> directorAffinities = new HashMap<>();
    public final Map<Integer, Float> personAffinity = new HashMap<>();
    public final Map<String, String> personNames = new HashMap<>();

    public final Set<Integer> avoidedGenreIds = new HashSet<>();
    public final Set<String> avoidedLanguages = new HashSet<>();

    private final Random random;

    public TasteProfile() {
        this(new Random());
    }

    public TasteProfile(Random random) {
        this.random = random != null ? random : new Random();
    }

    public static TasteProfile build(List<MovieLogWithDetails> logs) {
        return build(logs, System.currentTimeMillis(), DEFAULT_HALF_LIFE_DAYS, new Random());
    }

    public static TasteProfile build(List<MovieLogWithDetails> logs, Random random) {
        return build(logs, System.currentTimeMillis(), DEFAULT_HALF_LIFE_DAYS, random);
    }

    public static TasteProfile build(List<MovieLogWithDetails> logs, long nowMs, double halfLifeDays, Random random) {
        TasteProfile profile = new TasteProfile(random);
        if (logs == null) return profile;

        for (MovieLogWithDetails item : logs) {
            if (item.log == null) continue;
            long movieId = item.log.getMovieId();
            profile.loggedMovieIds.add(movieId);

            Reaction reaction = Reaction.fromRawString(item.log.getReaction());
            if (reaction == null) continue;

            long timestamp = item.log.getUpdatedAt() > 0 ? item.log.getUpdatedAt() : item.log.getCreatedAt();
            float decayedWeight = calculateDecayedWeight(reaction, timestamp, nowMs, halfLifeDays);

            if (reaction == Reaction.DISLIKE || reaction == Reaction.HATE) {
                profile.avoidedMovieIds.add(movieId);
            }

            MovieEntity movie = item.movie;
            if (movie != null) {
                if (reaction == Reaction.LOVE) {
                    profile.lovedMovies.add(movie);
                    profile.positiveSeeds.add(movie);
                } else if (reaction == Reaction.LIKE) {
                    profile.likedMovies.add(movie);
                    profile.positiveSeeds.add(movie);
                }

                profile.extractMovieAffinities(movie, decayedWeight);
            }
        }

        profile.evaluateAvoidSets();
        return profile;
    }

    public static float calculateDecayedWeight(Reaction reaction, long timestampMs, long nowMs, double halfLifeDays) {
        if (reaction == null) return 0.0f;
        float baseWeight = reaction.getWeight();
        if (baseWeight == 0.0f) return 0.0f;

        long ageMs = Math.max(0L, nowMs - timestampMs);
        double halfLifeMs = halfLifeDays * 24.0 * 60.0 * 60.0 * 1000.0;
        float recencyFactor = (float) Math.pow(0.5, ageMs / halfLifeMs);
        return baseWeight * recencyFactor;
    }

    private void extractMovieAffinities(MovieEntity movie, float weight) {
        if (movie.getGenreIds() != null && !movie.getGenreIds().isEmpty()) {
            String[] parts = movie.getGenreIds().split(",");
            for (String part : parts) {
                try {
                    int genreId = Integer.parseInt(part.trim());
                    Float current = genreAffinities.get(genreId);
                    genreAffinities.put(genreId, (current != null ? current : 0f) + weight);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (movie.getOriginalLanguage() != null && !movie.getOriginalLanguage().trim().isEmpty()) {
            String lang = movie.getOriginalLanguage().trim().toLowerCase();
            Float current = languageAffinities.get(lang);
            languageAffinities.put(lang, (current != null ? current : 0f) + weight);
        }

        if (movie.getReleaseDate() != null && movie.getReleaseDate().length() >= 4) {
            try {
                int year = Integer.parseInt(movie.getReleaseDate().substring(0, 4));
                String decade = (year / 10 * 10) + "s";
                Float current = decadeAffinities.get(decade);
                decadeAffinities.put(decade, (current != null ? current : 0f) + weight);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void evaluateAvoidSets() {
        for (Map.Entry<Integer, Float> entry : genreAffinities.entrySet()) {
            if (entry.getValue() <= -1.5f) {
                avoidedGenreIds.add(entry.getKey());
            }
        }
        for (Map.Entry<String, Float> entry : languageAffinities.entrySet()) {
            if (entry.getValue() <= -1.5f) {
                avoidedLanguages.add(entry.getKey());
            }
        }
    }

    public void addDirectorAffinity(int directorId, String name, float weight) {
        Float current = directorAffinities.get(directorId);
        directorAffinities.put(directorId, (current != null ? current : 0f) + weight);
        addPersonAffinity(directorId, name, weight);
    }

    public void addPersonAffinity(int personId, String name, float weight) {
        Float current = personAffinity.get(personId);
        personAffinity.put(personId, (current != null ? current : 0f) + weight);
        if (name != null) {
            personNames.put(String.valueOf(personId), name);
        }
    }
}

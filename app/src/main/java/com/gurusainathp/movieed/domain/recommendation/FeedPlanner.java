package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class FeedPlanner {

    private final Random random;
    private final CandidateScorer scorer;
    private final FeedInterleaver interleaver;
    private RecommendedMovie.Category lastCategory;

    public FeedPlanner() {
        this(new Random());
    }

    public FeedPlanner(Random random) {
        this.random = random != null ? random : new Random();
        this.scorer = new CandidateScorer(this.random);
        this.interleaver = new FeedInterleaver(this.random);
    }

    public FeedPlanner(Random random, CandidateScorer scorer, FeedInterleaver interleaver) {
        this.random = random != null ? random : new Random();
        this.scorer = scorer != null ? scorer : new CandidateScorer(this.random);
        this.interleaver = interleaver != null ? interleaver : new FeedInterleaver(this.random);
    }

    public CandidateScorer getScorer() {
        return scorer;
    }

    public FeedInterleaver getInterleaver() {
        return interleaver;
    }

    public RecommendedMovie.Category getLastCategory() {
        return lastCategory;
    }

    public boolean isValidMovie(TmdbMovieDto dto, TasteProfile profile) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().trim().isEmpty()) return false;
        if (profile != null && profile.loggedMovieIds.contains((long) dto.getId())) return false;
        if (dto.getBackdropPath() == null && dto.getPosterPath() == null) return false;
        if (dto.getOverview() == null || dto.getOverview().trim().isEmpty()) return false;

        String titleLower = dto.getTitle().toLowerCase(Locale.US);
        if (titleLower.contains("making of") ||
            titleLower.contains("behind the scenes") ||
            titleLower.contains("featurette") ||
            titleLower.contains("deleted scenes") ||
            titleLower.contains("bloopers")) {
            return false;
        }
        return true;
    }

    public List<RecommendedMovie> processAndPlanFeed(
            Map<MovieEntity, List<TmdbMovieDto>> activeSeedCandidatesMap,
            List<RecommendedMovie> accumulated,
            TasteProfile profile,
            Set<Integer> seenMovieIds) {

        List<RecommendedMovie> workingList = new ArrayList<>();

        for (RecommendedMovie rm : accumulated) {
            if (rm != null && rm.getMovie() != null && isValidMovie(rm.getMovie(), profile)) {
                workingList.add(rm);
            }
        }

        if (activeSeedCandidatesMap != null && !activeSeedCandidatesMap.isEmpty()) {
            List<RecommendedMovie> multiSeedList = scorer.aggregateMultiSeedRecommendations(
                    activeSeedCandidatesMap,
                    profile
            );

            for (RecommendedMovie rm : multiSeedList) {
                if (rm.getMovie() == null || !isValidMovie(rm.getMovie(), profile)) continue;
                synchronized (seenMovieIds) {
                    if (!seenMovieIds.contains(rm.getMovie().getId())) {
                        seenMovieIds.add(rm.getMovie().getId());
                        workingList.add(rm);
                    }
                }
            }
        }

        Collections.sort(workingList, (a, b) -> Float.compare(b.getScore(), a.getScore()));

        List<RecommendedMovie> result = interleaver.interleaveByReason(workingList, lastCategory);
        if (!result.isEmpty()) {
            lastCategory = result.get(result.size() - 1).getCategory();
        }
        return result;
    }
}

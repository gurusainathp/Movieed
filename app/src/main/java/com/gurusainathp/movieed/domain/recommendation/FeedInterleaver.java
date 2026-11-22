package com.gurusainathp.movieed.domain.recommendation;

import com.gurusainathp.movieed.domain.model.RecommendedMovie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FeedInterleaver {

    private final Random random;
    private final RecommendationConfig config;

    public FeedInterleaver() {
        this(new Random(), new RecommendationConfig());
    }

    public FeedInterleaver(Random random) {
        this(random, new RecommendationConfig());
    }

    public FeedInterleaver(Random random, RecommendationConfig config) {
        this.random = random != null ? random : new Random();
        this.config = config != null ? config : new RecommendationConfig();
    }

    public List<RecommendedMovie> interleaveByReason(List<RecommendedMovie> inputList) {
        return interleaveByReason(inputList, null);
    }

    public List<RecommendedMovie> interleaveByReason(List<RecommendedMovie> inputList, RecommendedMovie.Category startLastCategory) {
        if (inputList == null || inputList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<RecommendedMovie.Category, List<RecommendedMovie>> buckets = new EnumMap<>(RecommendedMovie.Category.class);
        for (RecommendedMovie.Category cat : RecommendedMovie.Category.values()) {
            buckets.put(cat, new ArrayList<>());
        }

        for (RecommendedMovie rm : inputList) {
            RecommendedMovie.Category cat = rm.getCategory();
            if (cat == null) cat = RecommendedMovie.Category.TRENDING;
            List<RecommendedMovie> bucket = buckets.get(cat);
            if (bucket != null) {
                bucket.add(rm);
            }
        }

        for (List<RecommendedMovie> bucket : buckets.values()) {
            if (bucket != null && !bucket.isEmpty()) {
                Collections.sort(bucket, (a, b) -> Float.compare(b.getScore(), a.getScore()));
                shuffleNearTies(bucket);
            }
        }

        List<RecommendedMovie> interleaved = new ArrayList<>();
        RecommendedMovie.Category lastCategory = startLastCategory;
        int targetTotalSize = inputList.size();

        Map<RecommendedMovie.Category, Float> categoryWeights = new EnumMap<>(RecommendedMovie.Category.class);
        categoryWeights.put(RecommendedMovie.Category.PERSONAL, config.personalCategoryQuota);
        categoryWeights.put(RecommendedMovie.Category.DIRECTOR, config.directorCategoryQuota);
        categoryWeights.put(RecommendedMovie.Category.CLASSIC, config.classicCategoryQuota);
        categoryWeights.put(RecommendedMovie.Category.TRENDING, config.trendingCategoryQuota);

        while (interleaved.size() < targetTotalSize) {
            List<RecommendedMovie.Category> availableCategories = new ArrayList<>();
            for (Map.Entry<RecommendedMovie.Category, List<RecommendedMovie>> entry : buckets.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    availableCategories.add(entry.getKey());
                }
            }

            if (availableCategories.isEmpty()) break;

            if (availableCategories.size() == 1) {
                RecommendedMovie.Category singleCat = availableCategories.get(0);
                List<RecommendedMovie> remaining = buckets.get(singleCat);
                if (remaining != null) {
                    interleaved.addAll(remaining);
                    remaining.clear();
                }
                break;
            }

            RecommendedMovie.Category chosenCategory = null;
            float bestPriorityScore = -1f;

            for (RecommendedMovie.Category cat : availableCategories) {
                if (cat == lastCategory && availableCategories.size() > 1) {
                    continue;
                }
                Float weightObj = categoryWeights.get(cat);
                float quotaWeight = weightObj != null ? weightObj : 0.1f;
                float priorityScore = quotaWeight * (1.0f + random.nextFloat() * 0.2f);
                if (priorityScore > bestPriorityScore) {
                    bestPriorityScore = priorityScore;
                    chosenCategory = cat;
                }
            }

            if (chosenCategory == null) {
                chosenCategory = availableCategories.get(0);
            }

            List<RecommendedMovie> chosenBucket = buckets.get(chosenCategory);
            if (chosenBucket != null && !chosenBucket.isEmpty()) {
                interleaved.add(chosenBucket.remove(0));
                lastCategory = chosenCategory;
            }
        }

        return interleaved;
    }

    private void shuffleNearTies(List<RecommendedMovie> bucket) {
        if (bucket == null || bucket.size() <= 1) return;
        int i = 0;
        while (i < bucket.size() - 1) {
            int j = i + 1;
            while (j < bucket.size() && Math.abs(bucket.get(i).getScore() - bucket.get(j).getScore()) < 0.05f) {
                j++;
            }
            if (j - i > 1) {
                Collections.shuffle(bucket.subList(i, j), random);
            }
            i = j;
        }
    }
}

package com.gurusainathp.movieed.domain.recommendation;

public class RecommendationConfig {

    public double globalAverageVote = 6.8;
    public double minVoteThreshold = 200.0;

    public float genreAffinityWeight = 1.5f;
    public float languageAffinityWeight = 1.0f;
    public float directorAffinityWeight = 2.5f;

    public float seedWeightMultiplier = 1.2f;
    public float rankPositionBonusMax = 1.0f;
    public float overlapBonusPerSeed = 1.5f;

    public float qualityWeight = 0.5f;
    public float noveltyWeight = 0.3f;
    public float avoidPenalty = -5.0f;

    public float tieBreakerMaxNoise = 0.05f;

    public float personalCategoryQuota = 0.50f;
    public float directorCategoryQuota = 0.15f;
    public float classicCategoryQuota = 0.15f;
    public float trendingCategoryQuota = 0.20f;
}

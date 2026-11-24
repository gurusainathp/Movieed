package com.gurusainathp.movieed.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbCastDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;
import com.gurusainathp.movieed.data.repository.MovieRepository;
import com.gurusainathp.movieed.domain.model.Movie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileViewModel extends AndroidViewModel {

    public static class ProfileStats {
        public final int totalFilms;
        public final int totalRuntimeHours;
        public final int inWatchlistCount;
        public final int watchedCount;
        public final Map<String, Integer> reactionCounts; // "LOVE", "LIKE", "UMM", "DISLIKE", "HATE"
        public final String favActor;
        public final String favActress;
        public final String favDirector;
        public final String favComposer;

        public ProfileStats(int totalFilms, int totalRuntimeHours, int inWatchlistCount, int watchedCount,
                            Map<String, Integer> reactionCounts,
                            String favActor, String favActress, String favDirector, String favComposer) {
            this.totalFilms = totalFilms;
            this.totalRuntimeHours = totalRuntimeHours;
            this.inWatchlistCount = inWatchlistCount;
            this.watchedCount = watchedCount;
            this.reactionCounts = reactionCounts;
            this.favActor = (favActor != null && !favActor.isEmpty()) ? favActor : "Log a few more films";
            this.favActress = (favActress != null && !favActress.isEmpty()) ? favActress : "Log a few more films";
            this.favDirector = (favDirector != null && !favDirector.isEmpty()) ? favDirector : "Log a few more films";
            this.favComposer = (favComposer != null && !favComposer.isEmpty()) ? favComposer : "Log a few more films";
        }

        public String getFormattedWatchTime() {
            if (totalRuntimeHours == 0) return "0 hrs";
            return "~" + totalRuntimeHours + " hrs";
        }
    }

    public static class PersonRecord {
        public final int personId;
        public final String name;
        public final int gender; // 1 = Female, 2 = Male
        public double effectiveCountN = 0.0;
        public double weightedReactionSum = 0.0;
        public int loveCount = 0;
        public long lastLoggedTime = 0;
        public int movieCount = 0;

        public PersonRecord(int personId, String name, int gender) {
            this.personId = personId;
            this.name = name;
            this.gender = gender;
        }

        public void addAppearance(float appearanceWeight, float reactionWeight, boolean isLove, long logTime) {
            this.effectiveCountN += appearanceWeight;
            this.weightedReactionSum += (appearanceWeight * reactionWeight);
            if (isLove) this.loveCount++;
            if (logTime > this.lastLoggedTime) this.lastLoggedTime = logTime;
            this.movieCount++;
        }

        public double calculateScore(double userMeanMu, double k) {
            double shrunk = (weightedReactionSum + (k * userMeanMu)) / (effectiveCountN + k);
            if (effectiveCountN < 2.0 || shrunk < userMeanMu) {
                return -1.0; // Ineligible per Gate rule
            }
            return shrunk * Math.log(1.0 + effectiveCountN);
        }
    }

    private final MovieRepository repository;
    private final MutableLiveData<ProfileStats> profileStats = new MutableLiveData<>();

    private final MutableLiveData<List<TmdbCastDto>> actorListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<TmdbCastDto>> actressListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<TmdbCastDto>> directorListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<TmdbCastDto>> composerListLiveData = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<String> topDecadeLiveData = new MutableLiveData<>("2020s");
    private final MutableLiveData<List<Movie>> topDecadeMoviesLiveData = new MutableLiveData<>(new ArrayList<>());

    private final Map<Integer, TmdbCreditsResponseDto> creditsCache = new ConcurrentHashMap<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);

        repository.getAllLogsWithDetails().observeForever(logs -> {
            if (logs == null || logs.isEmpty()) {
                profileStats.setValue(new ProfileStats(0, 0, 0, 0, new HashMap<>(), null, null, null, null));
                actorListLiveData.setValue(new ArrayList<>());
                actressListLiveData.setValue(new ArrayList<>());
                directorListLiveData.setValue(new ArrayList<>());
                composerListLiveData.setValue(new ArrayList<>());
                topDecadeLiveData.setValue("2020s");
                topDecadeMoviesLiveData.setValue(new ArrayList<>());
                return;
            }

            computeProfileStatsWithCredits(logs);
        });
    }

    public LiveData<ProfileStats> getProfileStats() { return profileStats; }
    public LiveData<List<TmdbCastDto>> getActorList() { return actorListLiveData; }
    public LiveData<List<TmdbCastDto>> getActressList() { return actressListLiveData; }
    public LiveData<List<TmdbCastDto>> getDirectorList() { return directorListLiveData; }
    public LiveData<List<TmdbCastDto>> getComposerList() { return composerListLiveData; }
    public LiveData<String> getTopDecade() { return topDecadeLiveData; }
    public LiveData<List<Movie>> getTopDecadeMovies() { return topDecadeMoviesLiveData; }

    public static float getReactionVibeWeight(String reaction) {
        if (reaction == null) return 0.4f;
        switch (reaction.toUpperCase()) {
            case "LOVE": return 1.0f;
            case "LIKE": return 0.75f;
            case "UMM": return 0.4f;
            case "DISLIKE": return 0.1f;
            case "HATE": return 0.0f;
            default: return 0.4f;
        }
    }

    private void computeProfileStatsWithCredits(List<MovieLogWithDetails> logs) {
        int inWatchlistCount = 0;
        int watchedCount = 0;
        int totalRuntimeMinutes = 0;
        double totalVibeWeightSum = 0.0;
        Set<Long> uniqueLoggedMovieIds = new HashSet<>();

        Map<String, Integer> reactionCounts = new HashMap<>();
        reactionCounts.put("LOVE", 0);
        reactionCounts.put("LIKE", 0);
        reactionCounts.put("UMM", 0);
        reactionCounts.put("DISLIKE", 0);
        reactionCounts.put("HATE", 0);

        List<MovieLogWithDetails> watchedLogs = new ArrayList<>();

        for (MovieLogWithDetails item : logs) {
            if (item.log == null) continue;

            boolean isWatchlist = item.log.isWatchlist();
            String reaction = item.log.getReaction();
            boolean hasReaction = reaction != null && !reaction.isEmpty();

            if (isWatchlist || hasReaction) {
                uniqueLoggedMovieIds.add(item.log.getMovieId());
            }

            if (isWatchlist) {
                inWatchlistCount++;
            }

            if (hasReaction) {
                watchedCount++;
                watchedLogs.add(item);
                float vibe = getReactionVibeWeight(reaction);
                totalVibeWeightSum += vibe;

                if (reactionCounts.containsKey(reaction.toUpperCase())) {
                    reactionCounts.put(reaction.toUpperCase(), reactionCounts.get(reaction.toUpperCase()) + 1);
                }

                int runtime = (item.movie != null && item.movie.getRuntime() > 0) ? item.movie.getRuntime() : 110;
                totalRuntimeMinutes += runtime;
            }
        }

        int totalLoggedFilms = uniqueLoggedMovieIds.size();
        int hours = Math.max(0, Math.round((float) totalRuntimeMinutes / 60f));
        double userMeanMu = watchedCount > 0 ? (totalVibeWeightSum / watchedCount) : 0.7;

        if (watchedLogs.isEmpty()) {
            profileStats.setValue(new ProfileStats(totalLoggedFilms, hours, inWatchlistCount, watchedCount, reactionCounts, null, null, null, null));
            return;
        }

        // Compute Decade Stats & Movies (only watched movies and shows, excluding watchlist)
        computeDecadeStats(watchedLogs);

        // Fetch credits for all watched movies asynchronously, then calculate favorites
        AtomicInteger pendingCalls = new AtomicInteger(watchedLogs.size());

        final int finalInWatchlistCount = inWatchlistCount;
        final int finalWatchedCount = watchedCount;

        for (MovieLogWithDetails item : watchedLogs) {
            int movieId = (int) item.log.getMovieId();
            if (creditsCache.containsKey(movieId)) {
                if (pendingCalls.decrementAndGet() == 0) {
                    evaluateFavoritePeople(watchedLogs, userMeanMu, totalLoggedFilms, hours, finalInWatchlistCount, finalWatchedCount, reactionCounts);
                }
            } else {
                TmdbClient.getApiService().getMovieCredits(movieId, BuildConfig.TMDB_API_KEY)
                        .enqueue(new Callback<TmdbCreditsResponseDto>() {
                            @Override
                            public void onResponse(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Response<TmdbCreditsResponseDto> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    creditsCache.put(movieId, response.body());
                                }
                                if (pendingCalls.decrementAndGet() == 0) {
                                    evaluateFavoritePeople(watchedLogs, userMeanMu, totalLoggedFilms, hours, finalInWatchlistCount, finalWatchedCount, reactionCounts);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Throwable t) {
                                if (pendingCalls.decrementAndGet() == 0) {
                                    evaluateFavoritePeople(watchedLogs, userMeanMu, totalLoggedFilms, hours, finalInWatchlistCount, finalWatchedCount, reactionCounts);
                                }
                            }
                        });
            }
        }
    }

    private void computeDecadeStats(List<MovieLogWithDetails> watchedLogs) {
        Map<String, Float> decadeWeights = new HashMap<>();
        Map<String, List<Movie>> decadeMoviesMap = new HashMap<>();

        for (MovieLogWithDetails item : watchedLogs) {
            if (item == null || item.movie == null) continue;

            String releaseDate = item.movie.getReleaseDate();
            if (releaseDate != null && releaseDate.length() >= 4) {
                try {
                    int year = Integer.parseInt(releaseDate.substring(0, 4));
                    int decadeStart = (year / 10) * 10;
                    String decadeStr = decadeStart + "s";

                    float weight = (item.log != null && item.log.getReaction() != null) ? getReactionVibeWeight(item.log.getReaction()) : 0.5f;
                    decadeWeights.put(decadeStr, decadeWeights.getOrDefault(decadeStr, 0f) + weight);

                    String mediaType = item.movie.isTv() ? "tv" : "movie";

                    List<Movie> mList = decadeMoviesMap.computeIfAbsent(decadeStr, k -> new ArrayList<>());
                    mList.add(new Movie(
                            item.movie.getTmdbId(),
                            item.movie.getTitle(),
                            item.movie.getOverview(),
                            item.movie.getReleaseDate(),
                            item.movie.getPosterPath(),
                            item.movie.getBackdropPath(),
                            item.movie.getVoteAverage(),
                            mediaType
                    ));
                } catch (Exception ignored) {}
            }
        }

        String bestDecade = "2020s";
        float maxDecadeWeight = -1f;
        for (Map.Entry<String, Float> entry : decadeWeights.entrySet()) {
            if (entry.getValue() > maxDecadeWeight) {
                maxDecadeWeight = entry.getValue();
                bestDecade = entry.getKey();
            }
        }

        topDecadeLiveData.setValue(bestDecade);

        List<Movie> decadeMovies = decadeMoviesMap.get(bestDecade);
        if (decadeMovies != null) {
            Collections.sort(decadeMovies, (a, b) -> Double.compare(b.getVoteAverage(), a.getVoteAverage()));
            int limit = Math.min(5, decadeMovies.size());
            topDecadeMoviesLiveData.setValue(new ArrayList<>(decadeMovies.subList(0, limit)));
        } else {
            topDecadeMoviesLiveData.setValue(new ArrayList<>());
        }
    }

    private void evaluateFavoritePeople(List<MovieLogWithDetails> watchedLogs,
                                        double userMeanMu,
                                        int totalLoggedFilms,
                                        int hours,
                                        int inWatchlistCount,
                                        int watchedCount,
                                        Map<String, Integer> reactionCounts) {

        Map<Integer, PersonRecord> actorMap = new HashMap<>();
        Map<Integer, PersonRecord> actressMap = new HashMap<>();
        Map<Integer, PersonRecord> directorMap = new HashMap<>();
        Map<Integer, PersonRecord> composerMap = new HashMap<>();
        Map<Integer, String> profilePathMap = new HashMap<>();

        for (MovieLogWithDetails item : watchedLogs) {
            int movieId = (int) item.log.getMovieId();
            TmdbCreditsResponseDto credits = creditsCache.get(movieId);
            if (credits == null) continue;

            float reactionWeight = getReactionVibeWeight(item.log.getReaction());
            boolean isLove = "LOVE".equalsIgnoreCase(item.log.getReaction());
            long logTime = item.log.getUpdatedAt() > 0 ? item.log.getUpdatedAt() : item.log.getCreatedAt();

            // 1. Process Cast
            if (credits.getCast() != null) {
                int order = 0;
                for (TmdbCastDto cast : credits.getCast()) {
                    if (cast == null || cast.getName() == null) continue;

                    if (cast.getProfilePath() != null) {
                        profilePathMap.put(cast.getId(), cast.getProfilePath());
                    }

                    float appearanceWeight = 0f;
                    if (order < 3) {
                        appearanceWeight = 1.0f;
                    } else if (order < 8) {
                        appearanceWeight = 0.5f;
                    } else {
                        break;
                    }

                    int gender = cast.getGender();
                    Map<Integer, PersonRecord> targetMap = (gender == 1) ? actressMap : actorMap;

                    PersonRecord record = targetMap.get(cast.getId());
                    if (record == null) {
                        record = new PersonRecord(cast.getId(), cast.getName(), gender);
                        targetMap.put(cast.getId(), record);
                    }
                    record.addAppearance(appearanceWeight, reactionWeight, isLove, logTime);
                    order++;
                }
            }

            // 2. Process Crew (Directors and Composers)
            if (credits.getCrew() != null) {
                for (TmdbCrewDto crew : credits.getCrew()) {
                    if (crew == null || crew.getJob() == null || crew.getName() == null) continue;

                    if (crew.getProfilePath() != null) {
                        profilePathMap.put(crew.getId(), crew.getProfilePath());
                    }

                    String job = crew.getJob().trim().toLowerCase();
                    if ("director".equals(job)) {
                        PersonRecord record = directorMap.get(crew.getId());
                        if (record == null) {
                            record = new PersonRecord(crew.getId(), crew.getName(), crew.getGender());
                            directorMap.put(crew.getId(), record);
                        }
                        record.addAppearance(1.0f, reactionWeight, isLove, logTime);
                    } else if ("original music composer".equals(job) || "composer".equals(job) || "music".equals(job)) {
                        PersonRecord record = composerMap.get(crew.getId());
                        if (record == null) {
                            record = new PersonRecord(crew.getId(), crew.getName(), crew.getGender());
                            composerMap.put(crew.getId(), record);
                        }
                        record.addAppearance(1.0f, reactionWeight, isLove, logTime);
                    }
                }
            }
        }

        List<TmdbCastDto> actors = findTopCastList(actorMap, profilePathMap, userMeanMu);
        List<TmdbCastDto> actresses = findTopCastList(actressMap, profilePathMap, userMeanMu);
        List<TmdbCastDto> directors = findTopCastList(directorMap, profilePathMap, userMeanMu);
        List<TmdbCastDto> composers = findTopCastList(composerMap, profilePathMap, userMeanMu);

        actorListLiveData.setValue(actors);
        actressListLiveData.setValue(actresses);
        directorListLiveData.setValue(directors);
        composerListLiveData.setValue(composers);

        String favActor = !actors.isEmpty() ? actors.get(0).getName() : "Log a few more films";
        String favActress = !actresses.isEmpty() ? actresses.get(0).getName() : "Log a few more films";
        String favDirector = !directors.isEmpty() ? directors.get(0).getName() : "Log a few more films";
        String favComposer = !composers.isEmpty() ? composers.get(0).getName() : "Log a few more films";

        profileStats.setValue(new ProfileStats(
                totalLoggedFilms,
                hours,
                inWatchlistCount,
                watchedCount,
                reactionCounts,
                favActor,
                favActress,
                favDirector,
                favComposer
        ));
    }

    private List<TmdbCastDto> findTopCastList(Map<Integer, PersonRecord> map, Map<Integer, String> profilePathMap, double userMeanMu) {
        if (map == null || map.isEmpty()) return new ArrayList<>();

        List<PersonRecord> list = new ArrayList<>(map.values());
        double k = 2.0;

        List<ScoredPerson> eligibleList = new ArrayList<>();
        for (PersonRecord record : list) {
            double score = record.calculateScore(userMeanMu, k);
            if (score > 0) {
                eligibleList.add(new ScoredPerson(record, score));
            }
        }

        if (eligibleList.isEmpty()) return new ArrayList<>();

        Collections.sort(eligibleList, (a, b) -> {
            int scoreComp = Double.compare(b.score, a.score);
            if (scoreComp != 0) return scoreComp;
            int loveComp = Integer.compare(b.record.loveCount, a.record.loveCount);
            if (loveComp != 0) return loveComp;
            return Long.compare(b.record.lastLoggedTime, a.record.lastLoggedTime);
        });

        int limit = Math.min(5, eligibleList.size());
        List<TmdbCastDto> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            PersonRecord r = eligibleList.get(i).record;
            result.add(new TmdbCastDto(
                    r.personId,
                    r.name,
                    r.movieCount + " films",
                    profilePathMap.get(r.personId),
                    r.gender
            ));
        }

        return result;
    }

    private static class ScoredPerson {
        final PersonRecord record;
        final double score;

        ScoredPerson(PersonRecord record, double score) {
            this.record = record;
            this.score = score;
        }
    }
}

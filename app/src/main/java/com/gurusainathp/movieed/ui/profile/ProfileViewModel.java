package com.gurusainathp.movieed.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.repository.MovieRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileViewModel extends AndroidViewModel {

    public static class ProfileStats {
        public final int totalFilms;
        public final int totalRuntimeHours;
        public final int inWatchlistCount;
        public final int watchedCount;
        public final Map<String, Integer> reactionCounts; // "LOVE", "LIKE", "UMM", "DISLIKE", "HATE"

        public ProfileStats(int totalFilms, int totalRuntimeHours, int inWatchlistCount, int watchedCount, Map<String, Integer> reactionCounts) {
            this.totalFilms = totalFilms;
            this.totalRuntimeHours = totalRuntimeHours;
            this.inWatchlistCount = inWatchlistCount;
            this.watchedCount = watchedCount;
            this.reactionCounts = reactionCounts;
        }

        public String getFormattedWatchTime() {
            if (totalRuntimeHours == 0) return "0 hrs";
            return "~" + totalRuntimeHours + " hrs";
        }
    }

    private final MovieRepository repository;
    private final LiveData<ProfileStats> profileStats;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);

        profileStats = Transformations.map(repository.getAllLogsWithDetails(), logs -> {
            if (logs == null || logs.isEmpty()) {
                return new ProfileStats(0, 0, 0, 0, new HashMap<>());
            }

            int inWatchlistCount = 0;
            int watchedCount = 0;
            int totalRuntimeMinutes = 0;
            Set<Long> uniqueLoggedMovieIds = new HashSet<>();

            Map<String, Integer> reactionCounts = new HashMap<>();
            reactionCounts.put("LOVE", 0);
            reactionCounts.put("LIKE", 0);
            reactionCounts.put("UMM", 0);
            reactionCounts.put("DISLIKE", 0);
            reactionCounts.put("HATE", 0);

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
                    if (reactionCounts.containsKey(reaction)) {
                        reactionCounts.put(reaction, reactionCounts.get(reaction) + 1);
                    }

                    int runtime = (item.movie != null && item.movie.getRuntime() > 0) ? item.movie.getRuntime() : 110;
                    totalRuntimeMinutes += runtime;
                }
            }

            int totalLoggedFilms = uniqueLoggedMovieIds.size();
            int hours = Math.max(0, Math.round((float) totalRuntimeMinutes / 60f));

            return new ProfileStats(totalLoggedFilms, hours, inWatchlistCount, watchedCount, reactionCounts);
        });
    }

    public LiveData<ProfileStats> getProfileStats() {
        return profileStats;
    }
}

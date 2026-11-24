package com.gurusainathp.movieed.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.repository.MovieRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileViewModel extends AndroidViewModel {

    public static class ProfileStats {
        public final int totalFilms;
        public final int totalRuntimeHours;
        public final Map<String, Integer> reactionCounts; // "LOVE", "LIKE", "UMM", "DISLIKE", "HATE"

        public ProfileStats(int totalFilms, int totalRuntimeHours, Map<String, Integer> reactionCounts) {
            this.totalFilms = totalFilms;
            this.totalRuntimeHours = totalRuntimeHours;
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
                return new ProfileStats(0, 0, new HashMap<>());
            }

            int count = 0;
            int totalRuntimeMinutes = 0;
            Map<String, Integer> reactionCounts = new HashMap<>();
            reactionCounts.put("LOVE", 0);
            reactionCounts.put("LIKE", 0);
            reactionCounts.put("UMM", 0);
            reactionCounts.put("DISLIKE", 0);
            reactionCounts.put("HATE", 0);

            for (MovieLogWithDetails item : logs) {
                if (item.log == null) continue;

                String reaction = item.log.getReaction();
                if (reaction != null && !reaction.isEmpty()) {
                    count++;
                    if (reactionCounts.containsKey(reaction)) {
                        reactionCounts.put(reaction, reactionCounts.get(reaction) + 1);
                    }

                    int runtime = (item.movie != null && item.movie.getRuntime() > 0) ? item.movie.getRuntime() : 110;
                    totalRuntimeMinutes += runtime;
                }
            }

            int hours = Math.max(0, Math.round((float) totalRuntimeMinutes / 60f));

            return new ProfileStats(count, hours, reactionCounts);
        });
    }

    public LiveData<ProfileStats> getProfileStats() {
        return profileStats;
    }
}

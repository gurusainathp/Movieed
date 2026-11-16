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
import java.util.Locale;
import java.util.Map;

public class ProfileViewModel extends AndroidViewModel {

    public static class ProfileStats {
        public final int totalFilms;
        public final double averageRating;
        public final int totalRuntimeHours;
        public final Map<Integer, Integer> ratingCounts; // Star integer 1-5 to count

        public ProfileStats(int totalFilms, double averageRating, int totalRuntimeHours, Map<Integer, Integer> ratingCounts) {
            this.totalFilms = totalFilms;
            this.averageRating = averageRating;
            this.totalRuntimeHours = totalRuntimeHours;
            this.ratingCounts = ratingCounts;
        }

        public String getFormattedAvgRating() {
            if (totalFilms == 0) return "—";
            return String.format(Locale.US, "%.1f", averageRating);
        }

        public String getFormattedWatchTime() {
            if (totalRuntimeHours == 0) return "0 hrs";
            return totalRuntimeHours + " hrs";
        }
    }

    private final MovieRepository repository;
    private final LiveData<ProfileStats> profileStats;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);

        profileStats = Transformations.map(repository.getAllLogsWithDetails(), logs -> {
            if (logs == null || logs.isEmpty()) {
                return new ProfileStats(0, 0.0, 0, new HashMap<>());
            }

            int count = logs.size();
            double sumRating = 0.0;
            int totalRuntimeMinutes = 0;
            Map<Integer, Integer> ratingCounts = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                ratingCounts.put(i, 0);
            }

            for (MovieLogWithDetails item : logs) {
                float rating = item.log.getRating();
                sumRating += rating;

                int starInt = Math.round(rating);
                if (starInt >= 1 && starInt <= 5) {
                    ratingCounts.put(starInt, ratingCounts.get(starInt) + 1);
                }

                if (item.movie != null && item.movie.getRuntime() > 0) {
                    totalRuntimeMinutes += item.movie.getRuntime();
                }
            }

            double avgRating = sumRating / count;
            int hours = totalRuntimeMinutes / 60;

            return new ProfileStats(count, avgRating, hours, ratingCounts);
        });
    }

    public LiveData<ProfileStats> getProfileStats() {
        return profileStats;
    }
}

package com.gurusainathp.movieed.domain;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.repository.MovieRepository;
import com.gurusainathp.movieed.ui.HomeViewModel;
import com.gurusainathp.movieed.ui.explore.ExploreViewModel;
import com.gurusainathp.movieed.ui.profile.ProfileViewModel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AppPreloader {

    private static final MutableLiveData<Boolean> isPreloadReady = new MutableLiveData<>(false);
    private static final AtomicBoolean isPreloadingStarted = new AtomicBoolean(false);

    public static LiveData<Boolean> getIsPreloadReady() {
        return isPreloadReady;
    }

    public static void startPreload(Application application,
                                    HomeViewModel homeViewModel,
                                    ExploreViewModel exploreViewModel,
                                    ProfileViewModel profileViewModel) {

        if (isPreloadingStarted.getAndSet(true)) return;

        Handler mainHandler = new Handler(Looper.getMainLooper());

        // Failsafe timeout guarantee: splash screen never stays longer than 2.5 seconds
        mainHandler.postDelayed(() -> {
            if (!Boolean.TRUE.equals(isPreloadReady.getValue())) {
                isPreloadReady.setValue(true);
            }
        }, 2500);

        AtomicInteger pendingTasks = new AtomicInteger(3);

        Runnable checkComplete = () -> {
            if (pendingTasks.decrementAndGet() <= 0) {
                mainHandler.removeCallbacksAndMessages(null);
                isPreloadReady.setValue(true);
            }
        };

        // 1. Preload Home Content
        if (homeViewModel != null) {
            homeViewModel.loadHomeContent(BuildConfig.TMDB_API_KEY);
            homeViewModel.getIsLoading().observeForever(loading -> {
                if (Boolean.FALSE.equals(loading)) {
                    checkComplete.run();
                }
            });
        } else {
            pendingTasks.decrementAndGet();
        }

        // 2. Preload Profile & User Logs
        MovieRepository repository = new MovieRepository(application);
        repository.getAllLogsWithDetails().observeForever(logs -> {
            List<MovieLogWithDetails> userLogs = logs != null ? logs : Collections.emptyList();

            // 3. Preload Explore Content with user history seeds
            if (exploreViewModel != null) {
                exploreViewModel.loadExploreMovies(userLogs, true);
                exploreViewModel.getIsLoading().observeForever(loading -> {
                    if (Boolean.FALSE.equals(loading)) {
                        checkComplete.run();
                    }
                });
            } else {
                pendingTasks.decrementAndGet();
            }

            checkComplete.run(); // Profile logs loaded
        });
    }
}

package com.gurusainathp.movieed.ui.diary;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;
import com.gurusainathp.movieed.data.repository.MovieRepository;

import java.util.List;

public class DiaryViewModel extends AndroidViewModel {

    private final MovieRepository repository;
    private final LiveData<List<MovieLogWithDetails>> diaryLogs;

    public DiaryViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
        diaryLogs = repository.getAllLogsWithDetails();
    }

    public LiveData<List<MovieLogWithDetails>> getDiaryLogs() {
        return diaryLogs;
    }
}

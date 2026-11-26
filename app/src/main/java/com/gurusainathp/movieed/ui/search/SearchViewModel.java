package com.gurusainathp.movieed.ui.search;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.api.TmdbApiService;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.domain.model.Movie;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends ViewModel {

    private final MutableLiveData<List<Movie>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private Call<TmdbMovieResponseDto> currentSearchCall;

    public LiveData<List<Movie>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void searchMovies(String query, String apiKey) {
        if (query == null || query.trim().isEmpty()) {
            searchResults.setValue(new ArrayList<>());
            errorMessage.setValue(null);
            return;
        }

        if (currentSearchCall != null && !currentSearchCall.isCanceled()) {
            currentSearchCall.cancel();
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        TmdbApiService apiService = TmdbClient.getApiService();
        currentSearchCall = apiService.searchMulti(apiKey, query.trim(), 1);
        currentSearchCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call,
                                   @NonNull Response<TmdbMovieResponseDto> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<TmdbMovieDto> dtos = response.body().getResults();
                    List<Movie> movies = new ArrayList<>();
                    if (dtos != null) {
                        for (TmdbMovieDto dto : dtos) {
                            String mediaType = dto.getMediaType();
                            if ("person".equalsIgnoreCase(mediaType)) {
                                String overview = formatPersonOverview(dto);
                                String activeYears = formatYearsActive(dto);

                                movies.add(new Movie(
                                        dto.getId(),
                                        dto.getTitle(),
                                        overview,
                                        activeYears,
                                        dto.getProfilePath(),
                                        null,
                                        0.0,
                                        "person"
                                ));
                            } else {
                                movies.add(new Movie(
                                        dto.getId(),
                                        dto.getTitle(),
                                        dto.getOverview(),
                                        dto.getReleaseDate(),
                                        dto.getPosterPath(),
                                        dto.getBackdropPath(),
                                        dto.getVoteAverage(),
                                        dto.getMediaType() != null ? dto.getMediaType() : "movie"
                                ));
                            }
                        }
                    }
                    searchResults.setValue(movies);
                } else {
                    errorMessage.setValue("Error fetching search results (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    isLoading.setValue(false);
                    errorMessage.setValue("Network error: " + t.getLocalizedMessage());
                }
            }
        });
    }

    private String formatPersonOverview(TmdbMovieDto dto) {
        StringBuilder sb = new StringBuilder();
        String dept = dto.getKnownForDepartment();
        if (dept != null && !dept.isEmpty()) {
            sb.append(formatDepartment(dept));
        } else {
            sb.append("Person");
        }

        if (dto.getKnownFor() != null && !dto.getKnownFor().isEmpty()) {
            sb.append(" · Known for ");
            int limit = Math.min(3, dto.getKnownFor().size());
            for (int i = 0; i < limit; i++) {
                TmdbMovieDto m = dto.getKnownFor().get(i);
                if (m != null && m.getTitle() != null) {
                    if (i > 0) sb.append(", ");
                    sb.append(m.getTitle());
                }
            }
        }
        return sb.toString();
    }

    private String formatYearsActive(TmdbMovieDto dto) {
        if (dto.getKnownFor() == null || dto.getKnownFor().isEmpty()) {
            return "";
        }
        int minYear = 9999;
        int maxYear = 0;
        for (TmdbMovieDto m : dto.getKnownFor()) {
            if (m != null && m.getReleaseDate() != null && m.getReleaseDate().length() >= 4) {
                try {
                    int y = Integer.parseInt(m.getReleaseDate().substring(0, 4));
                    if (y < minYear) minYear = y;
                    if (y > maxYear) maxYear = y;
                } catch (Exception ignored) {}
            }
        }
        if (minYear != 9999 && maxYear != 0) {
            if (minYear == maxYear) return String.valueOf(minYear);
            return minYear + " - " + maxYear;
        }
        return "";
    }

    private String formatDepartment(String department) {
        if (department == null) return "";
        switch (department.trim().toLowerCase()) {
            case "acting": return "Actor";
            case "directing": return "Director";
            case "writing": return "Writer";
            case "production": return "Producer";
            case "editing": return "Editor";
            case "camera": return "Cinematographer";
            case "sound":
            case "music": return "Composer";
            default: return department;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentSearchCall != null && !currentSearchCall.isCanceled()) {
            currentSearchCall.cancel();
        }
    }
}

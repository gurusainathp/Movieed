package com.gurusainathp.movieed.ui.movie;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gurusainathp.movieed.BuildConfig;
import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.remote.api.TmdbClient;
import com.gurusainathp.movieed.data.remote.dto.TmdbCastDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCountryWatchProvidersDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCreditsResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieResponseDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbProviderDto;
import com.gurusainathp.movieed.data.remote.dto.TmdbWatchProvidersResponseDto;
import com.gurusainathp.movieed.data.remote.dto.WatchProviderItem;
import com.gurusainathp.movieed.data.repository.MovieRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailsViewModel extends AndroidViewModel {

    private final MovieRepository repository;
    private final MutableLiveData<TmdbMovieDto> tmdbMovieDetails = new MutableLiveData<>();
    private final MutableLiveData<List<TmdbCastDto>> castList = new MutableLiveData<>();
    private final MutableLiveData<List<TmdbCrewDto>> crewList = new MutableLiveData<>();
    private final MutableLiveData<List<WatchProviderItem>> providerList = new MutableLiveData<>();
    private final MutableLiveData<List<TmdbMovieDto>> recommendationList = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MovieDetailsViewModel(@NonNull Application application) {
        super(application);
        repository = new MovieRepository(application);
    }

    public LiveData<TmdbMovieDto> getTmdbMovieDetails() {
        return tmdbMovieDetails;
    }

    public LiveData<List<TmdbCastDto>> getCastList() {
        return castList;
    }

    public LiveData<List<TmdbCrewDto>> getCrewList() {
        return crewList;
    }

    public LiveData<List<WatchProviderItem>> getProviderList() {
        return providerList;
    }

    public LiveData<List<TmdbMovieDto>> getRecommendationList() {
        return recommendationList;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<MovieEntity> getMovieByTmdbId(int tmdbId) {
        return repository.getMovieByTmdbId(tmdbId);
    }

    public LiveData<List<MovieLogEntity>> getLogsForMovie(long movieId) {
        return repository.getLogsForMovie(movieId);
    }

    public void fetchMovieDetails(int tmdbId, boolean isTv) {
        loadDetails(tmdbId, isTv, true);
    }

    private void loadDetails(int tmdbId, boolean isTv, boolean allowFallback) {
        Call<TmdbMovieDto> detailsCall = isTv ?
                TmdbClient.getApiService().getTvDetails(tmdbId, BuildConfig.TMDB_API_KEY) :
                TmdbClient.getApiService().getMovieDetails(tmdbId, BuildConfig.TMDB_API_KEY);

        detailsCall.enqueue(new Callback<TmdbMovieDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieDto> call, @NonNull Response<TmdbMovieDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tmdbMovieDetails.setValue(response.body());
                } else if (allowFallback) {
                    loadDetails(tmdbId, !isTv, false);
                } else {
                    errorMessage.setValue("Failed to load details (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieDto> call, @NonNull Throwable t) {
                if (allowFallback) {
                    loadDetails(tmdbId, !isTv, false);
                } else {
                    errorMessage.setValue("Error: " + t.getLocalizedMessage());
                }
            }
        });

        Call<TmdbCreditsResponseDto> creditsCall = isTv ?
                TmdbClient.getApiService().getTvCredits(tmdbId, BuildConfig.TMDB_API_KEY) :
                TmdbClient.getApiService().getMovieCredits(tmdbId, BuildConfig.TMDB_API_KEY);

        creditsCall.enqueue(new Callback<TmdbCreditsResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Response<TmdbCreditsResponseDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    castList.setValue(response.body().getCast());
                    crewList.setValue(response.body().getCrew());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbCreditsResponseDto> call, @NonNull Throwable t) {}
        });

        Call<TmdbWatchProvidersResponseDto> providersCall = isTv ?
                TmdbClient.getApiService().getTvWatchProviders(tmdbId, BuildConfig.TMDB_API_KEY) :
                TmdbClient.getApiService().getMovieWatchProviders(tmdbId, BuildConfig.TMDB_API_KEY);

        providersCall.enqueue(new Callback<TmdbWatchProvidersResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbWatchProvidersResponseDto> call,
                                   @NonNull Response<TmdbWatchProvidersResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    TmdbCountryWatchProvidersDto indiaProviders = response.body().getResults().get("IN");
                    if (indiaProviders != null) {
                        Map<Integer, WatchProviderItem> flatrateMap = new LinkedHashMap<>();
                        Map<Integer, WatchProviderItem> paidMap = new LinkedHashMap<>();

                        if (indiaProviders.getFlatrate() != null) {
                            for (TmdbProviderDto p : indiaProviders.getFlatrate()) {
                                int iconRes = LocalProviderMapper.getLocalDrawableRes(p);
                                if (iconRes != 0) {
                                    if (!flatrateMap.containsKey(iconRes)) {
                                        WatchProviderItem item = new WatchProviderItem(p);
                                        item.setFlatrate(true);
                                        flatrateMap.put(iconRes, item);
                                    }
                                }
                            }
                        }

                        if (indiaProviders.getRent() != null) {
                            for (TmdbProviderDto p : indiaProviders.getRent()) {
                                int iconRes = LocalProviderMapper.getLocalDrawableRes(p);
                                if (iconRes != 0) {
                                    WatchProviderItem item = paidMap.get(iconRes);
                                    if (item == null) {
                                        item = new WatchProviderItem(p);
                                        paidMap.put(iconRes, item);
                                    }
                                    item.setRent(true);
                                }
                            }
                        }

                        if (indiaProviders.getBuy() != null) {
                            for (TmdbProviderDto p : indiaProviders.getBuy()) {
                                int iconRes = LocalProviderMapper.getLocalDrawableRes(p);
                                if (iconRes != 0) {
                                    WatchProviderItem item = paidMap.get(iconRes);
                                    if (item == null) {
                                        item = new WatchProviderItem(p);
                                        paidMap.put(iconRes, item);
                                    }
                                    item.setBuy(true);
                                }
                            }
                        }

                        List<WatchProviderItem> flatrateList = new ArrayList<>(flatrateMap.values());
                        List<WatchProviderItem> paidList = new ArrayList<>(paidMap.values());
                        Collections.sort(paidList);

                        List<WatchProviderItem> resultList = new ArrayList<>();
                        resultList.addAll(flatrateList);
                        resultList.addAll(paidList);

                        providerList.setValue(resultList);
                    } else {
                        providerList.setValue(new ArrayList<>());
                    }
                } else {
                    providerList.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbWatchProvidersResponseDto> call, @NonNull Throwable t) {
                providerList.setValue(new ArrayList<>());
            }
        });

        Call<TmdbMovieResponseDto> recommendationsCall = isTv ?
                TmdbClient.getApiService().getTvRecommendations(tmdbId, BuildConfig.TMDB_API_KEY) :
                TmdbClient.getApiService().getMovieRecommendations(tmdbId, BuildConfig.TMDB_API_KEY);

        recommendationsCall.enqueue(new Callback<TmdbMovieResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Response<TmdbMovieResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    recommendationList.setValue(response.body().getResults());
                } else {
                    recommendationList.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TmdbMovieResponseDto> call, @NonNull Throwable t) {
                recommendationList.setValue(new ArrayList<>());
            }
        });
    }

    public void toggleWatchlist(TmdbMovieDto dto, MovieLogEntity existingLog) {
        if (dto == null) return;
        MovieEntity movieEntity = new MovieEntity(
                dto.getId(),
                dto.getTitle(),
                dto.getOverview(),
                dto.getReleaseDate(),
                dto.getPosterPath(),
                dto.getBackdropPath(),
                0,
                System.currentTimeMillis()
        );
        repository.toggleWatchlist(movieEntity, existingLog);
    }

    public void saveReaction(TmdbMovieDto dto, String reaction, MovieLogEntity existingLog) {
        if (dto == null) return;
        MovieEntity movieEntity = new MovieEntity(
                dto.getId(),
                dto.getTitle(),
                dto.getOverview(),
                dto.getReleaseDate(),
                dto.getPosterPath(),
                dto.getBackdropPath(),
                0,
                System.currentTimeMillis()
        );
        repository.saveReaction(movieEntity, reaction, existingLog);
    }

    public void deleteLog(MovieLogEntity log) {
        if (log != null) {
            repository.deleteLog(log);
        }
    }
}

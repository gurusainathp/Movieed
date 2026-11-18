package com.gurusainathp.movieed.ui.movie;

import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.remote.dto.TmdbProviderDto;

public class LocalProviderMapper {

    public static int getLocalDrawableRes(TmdbProviderDto provider) {
        if (provider == null || provider.getProviderName() == null) {
            return 0;
        }

        String name = provider.getProviderName().toLowerCase();
        int id = provider.getProviderId();

        if (name.contains("netflix") || id == 8) {
            return R.drawable.ic_provider_netflix;
        }
        if (name.contains("prime") || name.contains("amazon") || id == 119 || id == 9) {
            return R.drawable.ic_provider_prime_video;
        }
        if (name.contains("hotstar") || name.contains("disney") || name.contains("jio") || id == 122 || id == 337) {
            return R.drawable.jiohotstar;
        }
        if (name.contains("apple") || id == 350 || id == 2) {
            return R.drawable.ic_provider_apple_tv_plus;
        }
        if (name.contains("sony") || id == 237) {
            return R.drawable.sonyliv;
        }
        if (name.contains("zee") || id == 232) {
            return R.drawable.zee5;
        }
        if (name.contains("mx") || id == 511) {
            return R.drawable.mx_player;
        }
        if (name.contains("youtube") || id == 192) {
            return R.drawable.ic_provider_youtube_premium;
        }
        if (name.contains("crunchyroll") || id == 283) {
            return R.drawable.ic_provider_crunchyroll;
        }
        if (name.contains("lionsgate") || id == 309) {
            return R.drawable.ic_provider_lionsgate_plus;
        }
        if (name.contains("paramount") || id == 531) {
            return R.drawable.ic_provider_paramount_plus;
        }
        if (name.contains("plex") || id == 538) {
            return R.drawable.ic_provider_plex;
        }
        if (name.contains("tubi") || id == 73) {
            return R.drawable.ic_provider_tubi;
        }
        if (name.contains("mubi") || id == 11) {
            return R.drawable.mubi_logo;
        }

        return 0;
    }
}

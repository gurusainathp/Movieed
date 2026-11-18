package com.gurusainathp.movieed.ui.movie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.remote.dto.WatchProviderItem;

import java.util.ArrayList;
import java.util.List;

public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder> {

    private final List<WatchProviderItem> providers = new ArrayList<>();

    public void setProviders(List<WatchProviderItem> newProviders) {
        providers.clear();
        if (newProviders != null) {
            providers.addAll(newProviders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider, parent, false);
        return new ProviderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        holder.bind(providers.get(position));
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    static class ProviderViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivLogo;
        private final View cardBadge;
        private final TextView tvBadge;

        public ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLogo = itemView.findViewById(R.id.iv_provider_logo);
            cardBadge = itemView.findViewById(R.id.card_provider_badge);
            tvBadge = itemView.findViewById(R.id.tv_provider_badge);
        }

        public void bind(WatchProviderItem item) {
            if (item == null || item.getProvider() == null) return;

            int iconRes = LocalProviderMapper.getLocalDrawableRes(item.getProvider());
            if (iconRes != 0) {
                ivLogo.setImageResource(iconRes);
            } else if (item.getProvider().getLogoPath() != null) {
                Glide.with(itemView.getContext())
                        .load("https://image.tmdb.org/t/p/w500" + item.getProvider().getLogoPath())
                        .placeholder(R.color.movieed_surface_elevated)
                        .into(ivLogo);
            } else {
                ivLogo.setImageResource(R.color.movieed_surface_elevated);
            }

            String badgeText = item.getBadgeText();
            if (badgeText != null) {
                tvBadge.setText(badgeText);
                cardBadge.setVisibility(View.VISIBLE);
            } else {
                cardBadge.setVisibility(View.GONE);
            }
        }
    }
}

package com.gurusainathp.movieed.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;

import java.util.ArrayList;
import java.util.List;

public class HomeRecentLogAdapter extends RecyclerView.Adapter<HomeRecentLogAdapter.RecentLogViewHolder> {

    public interface OnRecentLogClickListener {
        void onLogClick(MovieLogWithDetails item);
    }

    private final List<MovieLogWithDetails> items = new ArrayList<>();
    private final OnRecentLogClickListener listener;

    public HomeRecentLogAdapter(OnRecentLogClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<MovieLogWithDetails> newItems) {
        items.clear();
        if (newItems != null) {
            // Keep the most recent 10 logs
            int limit = Math.min(newItems.size(), 10);
            for (int i = 0; i < limit; i++) {
                items.add(newItems.get(i));
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_recent_log, parent, false);
        return new RecentLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentLogViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RecentLogViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPoster;
        private final View cardBadge;
        private final TextView tvBadge;
        private final TextView tvTitle;

        public RecentLogViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_logged_poster);
            cardBadge = itemView.findViewById(R.id.card_reaction_badge);
            tvBadge = itemView.findViewById(R.id.tv_reaction_emoji);
            tvTitle = itemView.findViewById(R.id.tv_logged_title);
        }

        public void bind(MovieLogWithDetails item, OnRecentLogClickListener listener) {
            if (item == null) return;

            if (item.movie != null) {
                tvTitle.setText(item.movie.getTitle());
                if (item.movie.getPosterPath() != null) {
                    Glide.with(itemView.getContext())
                            .load("https://image.tmdb.org/t/p/w500" + item.movie.getPosterPath())
                            .placeholder(R.color.movieed_surface_elevated)
                            .error(R.color.movieed_surface_elevated)
                            .into(ivPoster);
                } else {
                    ivPoster.setImageResource(R.color.movieed_surface_elevated);
                }
            }

            boolean hasReaction = item.log.getReaction() != null && !item.log.getReaction().isEmpty();
            boolean isWatchlist = item.log.isWatchlist();

            if (hasReaction) {
                cardBadge.setVisibility(View.VISIBLE);
                tvBadge.setText(MovieLogEntity.getEmojiForReaction(item.log.getReaction()));
            } else if (isWatchlist) {
                cardBadge.setVisibility(View.VISIBLE);
                tvBadge.setText("+");
            } else {
                cardBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLogClick(item);
                }
            });
        }
    }
}

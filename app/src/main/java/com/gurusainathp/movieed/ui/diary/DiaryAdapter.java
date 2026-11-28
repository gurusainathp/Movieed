package com.gurusainathp.movieed.ui.diary;

import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogWithDetails;

import java.util.ArrayList;
import java.util.List;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    public interface OnDiaryClickListener {
        void onDiaryClick(MovieLogWithDetails item);
    }

    // Posters are ~80dp wide in a 4-column grid, so w342 is plenty (w500 just wastes memory)
    private static final String POSTER_BASE = "https://image.tmdb.org/t/p/w342";

    private final List<MovieLogWithDetails> items = new ArrayList<>();
    private final OnDiaryClickListener listener;
    private boolean isWatchlistTab;

    public DiaryAdapter(OnDiaryClickListener listener) {
        this(listener, false);
    }

    public DiaryAdapter(OnDiaryClickListener listener, boolean isWatchlistTab) {
        this.listener = listener;
        this.isWatchlistTab = isWatchlistTab;
    }

    public void setWatchlistTab(boolean watchlistTab) {
        this.isWatchlistTab = watchlistTab;
    }

    public void setItems(List<MovieLogWithDetails> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diary_grid, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        holder.bind(items.get(position), isWatchlistTab, listener);
    }

    @Override
    public void onViewRecycled(@NonNull DiaryViewHolder holder) {
        Glide.with(holder.ivPoster).clear(holder.ivPoster);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DiaryViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPoster;
        private final View cardBadge;
        private final TextView tvBadge;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_diary_poster);
            cardBadge = itemView.findViewById(R.id.card_badge);
            tvBadge = itemView.findViewById(R.id.tv_diary_badge);
        }

        public void bind(MovieLogWithDetails item, boolean isWatchlistTab, OnDiaryClickListener listener) {
            if (item == null || item.log == null) return;

            if (item.movie != null && item.movie.getPosterPath() != null) {
                Glide.with(ivPoster)
                        .load(POSTER_BASE + item.movie.getPosterPath())
                        .placeholder(R.color.movieed_surface_elevated)
                        .error(R.color.movieed_surface_elevated)
                        .into(ivPoster);
            } else {
                Glide.with(ivPoster).clear(ivPoster);
                ivPoster.setImageDrawable(new ColorDrawable(ContextCompat.getColor(
                        itemView.getContext(), R.color.movieed_surface_elevated)));
            }

            // TalkBack: read the film title instead of nothing
            // (if getTitle() isn't available on item.movie in your model, delete these 3 lines)
            if (item.movie != null && item.movie.getTitle() != null) {
                itemView.setContentDescription(item.movie.getTitle());
            }

            boolean hasReaction = item.log.getReaction() != null && !item.log.getReaction().isEmpty();

            if (isWatchlistTab) {
                cardBadge.setVisibility(View.VISIBLE);
                tvBadge.setText("+");
            } else if (hasReaction) {
                cardBadge.setVisibility(View.VISIBLE);
                tvBadge.setText(MovieLogEntity.getEmojiForReaction(item.log.getReaction()));
            } else {
                cardBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDiaryClick(item);
                }
            });
        }
    }
}
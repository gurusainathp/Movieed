package com.gurusainathp.movieed.ui.diary;

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

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    public interface OnDiaryClickListener {
        void onDiaryClick(MovieLogWithDetails item);
    }

    private final List<MovieLogWithDetails> items = new ArrayList<>();
    private final OnDiaryClickListener listener;

    public DiaryAdapter(OnDiaryClickListener listener) {
        this.listener = listener;
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
                .inflate(R.layout.item_diary_log, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DiaryViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPoster;
        private final TextView tvTitle;
        private final TextView tvStatus;
        private final TextView tvReaction;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_diary_poster);
            tvTitle = itemView.findViewById(R.id.tv_diary_title);
            tvStatus = itemView.findViewById(R.id.tv_diary_status);
            tvReaction = itemView.findViewById(R.id.tv_diary_reaction);
        }

        public void bind(MovieLogWithDetails item, OnDiaryClickListener listener) {
            if (item == null) return;

            if (item.movie != null) {
                tvTitle.setText(item.movie.getTitle());
                if (item.movie.getPosterPath() != null) {
                    Glide.with(itemView.getContext())
                            .load("https://image.tmdb.org/t/p/w500" + item.movie.getPosterPath())
                            .placeholder(R.color.movieed_surface_elevated)
                            .into(ivPoster);
                } else {
                    ivPoster.setImageResource(R.color.movieed_surface_elevated);
                }
            } else {
                tvTitle.setText("Movie #" + item.log.getMovieId());
                ivPoster.setImageResource(R.color.movieed_surface_elevated);
            }

            if (item.log.getReaction() != null && !item.log.getReaction().isEmpty()) {
                tvStatus.setText("Watched");
            } else if (item.log.isWatchlist()) {
                tvStatus.setText("In Watchlist");
            } else {
                tvStatus.setText("Logged");
            }

            String reactionLabel = MovieLogEntity.getLabelForReaction(item.log.getReaction());
            if (reactionLabel != null && !reactionLabel.isEmpty()) {
                tvReaction.setText(reactionLabel);
                tvReaction.setVisibility(View.VISIBLE);
            } else {
                tvReaction.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDiaryClick(item);
                }
            });
        }
    }
}

package com.gurusainathp.movieed.ui.explore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;
import com.gurusainathp.movieed.data.remote.dto.TmdbMovieDto;
import com.gurusainathp.movieed.domain.model.RecommendedMovie;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExploreAdapter extends RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder> {

    public interface OnExploreActionListener {
        void onMovieClick(TmdbMovieDto movie);
        void onReasonClick(RecommendedMovie recommendedMovie);
        void onWatchlistClick(TmdbMovieDto movie, MovieLogEntity currentLog);
        void onRateClick(TmdbMovieDto movie, MovieLogEntity currentLog);
        void onNotInterestedClick(TmdbMovieDto movie);
    }

    private final List<RecommendedMovie> items = new ArrayList<>();
    private final OnExploreActionListener actionListener;
    private Map<Long, MovieLogEntity> logsMap;

    public ExploreAdapter(OnExploreActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setItems(List<RecommendedMovie> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void removeItem(int movieId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) != null && items.get(i).getMovie() != null && items.get(i).getMovie().getId() == movieId) {
                items.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    public void setLogsMap(Map<Long, MovieLogEntity> map) {
        this.logsMap = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExploreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_explore_movie, parent, false);
        return new ExploreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ExploreViewHolder extends RecyclerView.ViewHolder {
        private final View layoutHeaderDetails;
        private final View cardReason;
        private final ImageView ivBackdrop;
        private final TextView tvTitle;
        private final TextView tvYear;
        private final TextView tvRating;
        private final TextView tvOverview;
        private final ShapeableImageView ivReasonPoster;
        private final TextView tvReasonText;
        private final MaterialButton btnNotInterested;
        private final MaterialButton btnWatchlist;
        private final MaterialButton btnRate;

        ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutHeaderDetails = itemView.findViewById(R.id.layout_explore_header_details);
            cardReason = itemView.findViewById(R.id.card_explore_reason);
            ivBackdrop = itemView.findViewById(R.id.iv_explore_backdrop);
            tvTitle = itemView.findViewById(R.id.tv_explore_title);
            tvYear = itemView.findViewById(R.id.tv_explore_year);
            tvRating = itemView.findViewById(R.id.tv_explore_rating);
            tvOverview = itemView.findViewById(R.id.tv_explore_overview);
            ivReasonPoster = itemView.findViewById(R.id.iv_explore_reason_poster);
            tvReasonText = itemView.findViewById(R.id.tv_explore_reason_text);
            btnNotInterested = itemView.findViewById(R.id.btn_action_not_interested);
            btnWatchlist = itemView.findViewById(R.id.btn_action_watchlist);
            btnRate = itemView.findViewById(R.id.btn_action_rate);
        }

        void bind(RecommendedMovie item) {
            if (item == null || item.getMovie() == null) return;
            TmdbMovieDto movie = item.getMovie();

            tvTitle.setText(movie.getTitle());

            String releaseDate = movie.getReleaseDate();
            if (releaseDate != null && releaseDate.length() >= 4) {
                tvYear.setText(releaseDate.substring(0, 4));
                tvYear.setVisibility(View.VISIBLE);
            } else {
                tvYear.setVisibility(View.GONE);
            }

            if (movie.getVoteAverage() > 0) {
                tvRating.setText("★ " + String.format(Locale.US, "%.1f", movie.getVoteAverage()));
                tvRating.setVisibility(View.VISIBLE);
            } else {
                tvRating.setVisibility(View.GONE);
            }

            tvOverview.setText(movie.getOverview());

            if (movie.getBackdropPath() != null) {
                Glide.with(itemView.getContext())
                        .load("https://image.tmdb.org/t/p/w780" + movie.getBackdropPath())
                        .into(ivBackdrop);
            } else if (movie.getPosterPath() != null) {
                Glide.with(itemView.getContext())
                        .load("https://image.tmdb.org/t/p/w780" + movie.getPosterPath())
                        .into(ivBackdrop);
            }

            tvReasonText.setText(item.getPrimaryReason());

            String reasonPoster = item.getReasonPosterPath();
            if (reasonPoster != null) {
                Glide.with(itemView.getContext())
                        .load("https://image.tmdb.org/t/p/w185" + reasonPoster)
                        .into(ivReasonPoster);
            }

            MovieLogEntity currentLog = logsMap != null ? logsMap.get((long) movie.getId()) : null;

            if (currentLog != null) {
                if (currentLog.isWatchlist()) {
                    btnWatchlist.setText("✓ In Watchlist");
                } else {
                    btnWatchlist.setText("+ Watchlist");
                }

                if (currentLog.getReaction() != null && !currentLog.getReaction().isEmpty()) {
                    btnRate.setText(MovieLogEntity.getLabelForReaction(currentLog.getReaction()));
                } else {
                    btnRate.setText("Rate");
                }
            } else {
                btnWatchlist.setText("+ Watchlist");
                btnRate.setText("Rate");
            }

            if (layoutHeaderDetails != null) {
                layoutHeaderDetails.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onMovieClick(movie);
                    }
                });
            }

            if (cardReason != null) {
                cardReason.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onReasonClick(item);
                    }
                });
            }

            if (btnNotInterested != null) {
                btnNotInterested.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onNotInterestedClick(movie);
                    }
                });
            }

            btnWatchlist.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onWatchlistClick(movie, currentLog);
                }
            });

            btnRate.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onRateClick(movie, currentLog);
                }
            });
        }
    }
}

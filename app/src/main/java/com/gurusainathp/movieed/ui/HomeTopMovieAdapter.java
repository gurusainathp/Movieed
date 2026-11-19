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
import com.gurusainathp.movieed.domain.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeTopMovieAdapter extends RecyclerView.Adapter<HomeTopMovieAdapter.TopMovieViewHolder> {

    public interface OnTopMovieClickListener {
        void onMovieClick(Movie movie);
    }

    private final List<Movie> movies = new ArrayList<>();
    private final OnTopMovieClickListener listener;

    public HomeTopMovieAdapter(OnTopMovieClickListener listener) {
        this.listener = listener;
    }

    public void setMovies(List<Movie> newMovies) {
        movies.clear();
        if (newMovies != null) {
            // Keep at most top 10
            int limit = Math.min(newMovies.size(), 10);
            for (int i = 0; i < limit; i++) {
                movies.add(newMovies.get(i));
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopMovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_top_movie, parent, false);
        return new TopMovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopMovieViewHolder holder, int position) {
        holder.bind(movies.get(position), position + 1, listener);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class TopMovieViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRank;
        private final ImageView ivPoster;
        private final TextView tvTitle;
        private final TextView tvYear;
        private final TextView tvRating;

        public TopMovieViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank_number);
            ivPoster = itemView.findViewById(R.id.iv_top_poster);
            tvTitle = itemView.findViewById(R.id.tv_top_title);
            tvYear = itemView.findViewById(R.id.tv_top_year);
            tvRating = itemView.findViewById(R.id.tv_top_rating);
        }

        public void bind(Movie movie, int rank, OnTopMovieClickListener listener) {
            if (movie == null) return;

            tvRank.setText(String.valueOf(rank));
            tvTitle.setText(movie.getTitle());

            String releaseDate = movie.getReleaseDate();
            if (releaseDate != null && releaseDate.length() >= 4) {
                tvYear.setText(releaseDate.substring(0, 4));
            } else {
                tvYear.setText("");
            }

            if (movie.getVoteAverage() > 0) {
                tvRating.setVisibility(View.VISIBLE);
                tvRating.setText(String.format(Locale.US, "★ %.1f", movie.getVoteAverage()));
            } else {
                tvRating.setVisibility(View.GONE);
            }

            String posterUrl = movie.getFullPosterUrl();
            if (posterUrl != null) {
                Glide.with(itemView.getContext())
                        .load(posterUrl)
                        .placeholder(R.color.movieed_surface_elevated)
                        .error(R.color.movieed_surface_elevated)
                        .into(ivPoster);
            } else {
                ivPoster.setImageResource(R.color.movieed_surface_elevated);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMovieClick(movie);
                }
            });
        }
    }
}

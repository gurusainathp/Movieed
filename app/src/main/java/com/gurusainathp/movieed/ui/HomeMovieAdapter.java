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

public class HomeMovieAdapter extends RecyclerView.Adapter<HomeMovieAdapter.HomeMovieViewHolder> {

    public interface OnHomeMovieClickListener {
        void onMovieClick(Movie movie);
    }

    private final List<Movie> movies = new ArrayList<>();
    private final OnHomeMovieClickListener listener;

    public HomeMovieAdapter(OnHomeMovieClickListener listener) {
        this.listener = listener;
    }

    public void setMovies(List<Movie> newMovies) {
        movies.clear();
        if (newMovies != null) {
            movies.addAll(newMovies);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HomeMovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_movie_card, parent, false);
        return new HomeMovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeMovieViewHolder holder, int position) {
        holder.bind(movies.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class HomeMovieViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPoster;
        private final TextView tvTitle;
        private final TextView tvYear;
        private final TextView tvRating;

        public HomeMovieViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_movie_poster);
            tvTitle = itemView.findViewById(R.id.tv_movie_title);
            tvYear = itemView.findViewById(R.id.tv_movie_year);
            tvRating = itemView.findViewById(R.id.tv_movie_rating);
        }

        public void bind(Movie movie, OnHomeMovieClickListener listener) {
            if (movie == null) return;

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

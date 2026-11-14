package com.gurusainathp.movieed.ui.search;

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

public class SearchMovieAdapter extends RecyclerView.Adapter<SearchMovieAdapter.MovieViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    private final List<Movie> movies = new ArrayList<>();
    private final OnMovieClickListener listener;

    public SearchMovieAdapter(OnMovieClickListener listener) {
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
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie_search, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.bind(movie, listener);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPoster;
        private final TextView tvTitle;
        private final TextView tvYear;
        private final TextView tvOverview;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvYear = itemView.findViewById(R.id.tv_year);
            tvOverview = itemView.findViewById(R.id.tv_overview);
        }

        public void bind(Movie movie, OnMovieClickListener listener) {
            tvTitle.setText(movie.getTitle());

            String releaseDate = movie.getReleaseDate();
            if (releaseDate != null && releaseDate.length() >= 4) {
                tvYear.setText(releaseDate.substring(0, 4));
            } else {
                tvYear.setText("");
            }

            tvOverview.setText(movie.getOverview());

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

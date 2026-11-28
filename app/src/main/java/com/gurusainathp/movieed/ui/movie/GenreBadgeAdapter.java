package com.gurusainathp.movieed.ui.movie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.domain.util.GenreMapper;

import java.util.ArrayList;
import java.util.List;

public class GenreBadgeAdapter extends RecyclerView.Adapter<GenreBadgeAdapter.GenreViewHolder> {

    private final List<String> genres = new ArrayList<>();

    public void setGenres(List<String> newGenres) {
        genres.clear();
        if (newGenres != null) {
            genres.addAll(newGenres);
        }
        notifyDataSetChanged();
    }

    public void setGenreIds(List<Integer> genreIds) {
        genres.clear();
        if (genreIds != null) {
            for (Integer id : genreIds) {
                if (id != null) {
                    genres.add(GenreMapper.getGenreName(id));
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_genre_badge, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        holder.bind(genres.get(position));
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    static class GenreViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBadge;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tv_genre_badge);
        }

        public void bind(String genreName) {
            if (tvBadge != null) {
                tvBadge.setText(genreName);
            }
        }
    }
}

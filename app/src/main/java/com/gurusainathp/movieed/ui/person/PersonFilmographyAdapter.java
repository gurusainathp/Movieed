package com.gurusainathp.movieed.ui.person;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;

import java.util.ArrayList;
import java.util.List;

public class PersonFilmographyAdapter extends RecyclerView.Adapter<PersonFilmographyAdapter.FilmographyViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(TmdbPersonCastCreditDto credit);
    }

    private final List<TmdbPersonCastCreditDto> credits = new ArrayList<>();
    private final OnItemClickListener listener;

    public PersonFilmographyAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setCredits(List<TmdbPersonCastCreditDto> newCredits) {
        credits.clear();
        if (newCredits != null) {
            credits.addAll(newCredits);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FilmographyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie_search, parent, false);
        return new FilmographyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilmographyViewHolder holder, int position) {
        holder.bind(credits.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return credits.size();
    }

    static class FilmographyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPoster;
        private final TextView tvTitle;
        private final TextView tvYear;
        private final TextView tvOverview;

        public FilmographyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvYear = itemView.findViewById(R.id.tv_year);
            tvOverview = itemView.findViewById(R.id.tv_overview);
        }

        public void bind(TmdbPersonCastCreditDto credit, OnItemClickListener listener) {
            if (credit == null) return;

            tvTitle.setText(credit.getTitle());

            String releaseDate = credit.getReleaseDate();
            if (releaseDate != null && releaseDate.length() >= 4) {
                tvYear.setText(releaseDate.substring(0, 4));
            } else {
                tvYear.setText("");
            }

            String character = credit.getCharacter();
            if (character != null && !character.isEmpty()) {
                tvOverview.setText(character);
            } else {
                tvOverview.setText("—");
            }

            String posterPath = credit.getPosterPath();
            if (posterPath != null) {
                Glide.with(itemView.getContext())
                        .load("https://image.tmdb.org/t/p/w500" + posterPath)
                        .placeholder(R.color.movieed_surface_elevated)
                        .into(ivPoster);
            } else {
                ivPoster.setImageResource(R.color.movieed_surface_elevated);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(credit);
                }
            });
        }
    }
}

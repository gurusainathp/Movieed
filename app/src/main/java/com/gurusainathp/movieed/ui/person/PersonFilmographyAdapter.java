package com.gurusainathp.movieed.ui.person;

import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.remote.dto.TmdbPersonCastCreditDto;

import java.util.Locale;
import java.util.Objects;

/**
 * Filmography rows. The click listener still hands back the original DTO,
 * so the code that opens the details screen does not need to change.
 */
public class PersonFilmographyAdapter
        extends ListAdapter<Filmography.Item, PersonFilmographyAdapter.FilmographyViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(TmdbPersonCastCreditDto credit);
    }

    private static final String POSTER_BASE = "https://image.tmdb.org/t/p/w185";

    private static final DiffUtil.ItemCallback<Filmography.Item> DIFF =
            new DiffUtil.ItemCallback<Filmography.Item>() {
                @Override
                public boolean areItemsTheSame(@NonNull Filmography.Item a, @NonNull Filmography.Item b) {
                    return a.key.equals(b.key);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Filmography.Item a, @NonNull Filmography.Item b) {
                    return a.year == b.year
                            && a.logged == b.logged
                            && a.rating == b.rating
                            && Objects.equals(a.title, b.title)
                            && Objects.equals(a.subtitle, b.subtitle)
                            && Objects.equals(a.credit.getPosterPath(), b.credit.getPosterPath());
                }
            };

    private final OnItemClickListener listener;

    public PersonFilmographyAdapter(OnItemClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FilmographyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_person_credit, parent, false);
        return new FilmographyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilmographyViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    @Override
    public void onViewRecycled(@NonNull FilmographyViewHolder holder) {
        Glide.with(holder.ivPoster).clear(holder.ivPoster);
        super.onViewRecycled(holder);
    }

    static class FilmographyViewHolder extends RecyclerView.ViewHolder {

        final ImageView ivPoster;
        private final TextView tvType;
        private final TextView tvTitle;
        private final TextView tvYear;
        private final TextView tvCharacter;
        private final TextView tvRating;
        private final TextView tvLogged;

        FilmographyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.iv_poster);
            tvType = itemView.findViewById(R.id.tv_type);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvYear = itemView.findViewById(R.id.tv_year);
            tvCharacter = itemView.findViewById(R.id.tv_character);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvLogged = itemView.findViewById(R.id.tv_logged);
        }

        void bind(Filmography.Item item, OnItemClickListener listener) {
            tvTitle.setText(item.title);
            tvType.setText(item.isTv ? "SERIES" : "MOVIE");
            tvYear.setText(item.year > 0 ? String.valueOf(item.year) : "TBA");

            if (item.subtitle != null && !item.subtitle.isEmpty()) {
                tvCharacter.setText(item.subtitle);
                tvCharacter.setVisibility(View.VISIBLE);
            } else {
                tvCharacter.setVisibility(View.GONE);
            }

            if (item.rating > 0) {
                tvRating.setText(String.format(Locale.US, "★ %.1f", item.rating));
                tvRating.setVisibility(View.VISIBLE);
            } else {
                tvRating.setVisibility(View.GONE);
            }

            tvLogged.setVisibility(item.logged ? View.VISIBLE : View.GONE);

            String posterPath = item.credit.getPosterPath();
            if (posterPath != null && !posterPath.isEmpty()) {
                Glide.with(ivPoster)
                        .load(POSTER_BASE + posterPath)
                        .placeholder(R.color.movieed_surface_elevated)
                        .error(R.color.movieed_surface_elevated)
                        .into(ivPoster);
            } else {
                Glide.with(ivPoster).clear(ivPoster);
                ivPoster.setImageDrawable(new ColorDrawable(ContextCompat.getColor(
                        itemView.getContext(), R.color.movieed_surface_elevated)));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item.credit);
            });
        }
    }
}
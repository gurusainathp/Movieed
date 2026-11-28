package com.gurusainathp.movieed.ui.search;

import android.graphics.drawable.ColorDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.domain.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Rows for the search suggestions panel.
 * - Movies / series: 2:3 poster, "MOVIE" / "SERIES" label, release year
 * - People: round avatar, "PERSON" label, years active, "Actor · Known for …" line
 * The part of the title that matches what the user typed is tinted with the accent color.
 */
public class SearchSuggestionAdapter
        extends ListAdapter<SearchSuggestionAdapter.Row, SearchSuggestionAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull Movie movie);
    }

    /**
     * A movie plus the query it was matched against (so highlight changes trigger a rebind).
     */
    public static final class Row {
        final Movie movie;
        final String query;

        Row(@NonNull Movie movie, @NonNull String query) {
            this.movie = movie;
            this.query = query;
        }
    }

    private static final String POSTER_BASE = "https://image.tmdb.org/t/p/w154";
    private static final String PROFILE_BASE = "https://image.tmdb.org/t/p/w185";

    private static final DiffUtil.ItemCallback<Row> DIFF = new DiffUtil.ItemCallback<Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
            return Objects.equals(a.movie.getId(), b.movie.getId())
                    && Objects.equals(a.movie.getMediaType(), b.movie.getMediaType());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
            return Objects.equals(a.movie.getTitle(), b.movie.getTitle())
                    && Objects.equals(a.movie.getPosterPath(), b.movie.getPosterPath())
                    && Objects.equals(a.movie.getReleaseDate(), b.movie.getReleaseDate())
                    && Objects.equals(a.movie.getOverview(), b.movie.getOverview())
                    && Objects.equals(a.query, b.query);
        }
    };

    private final OnItemClickListener listener;

    public SearchSuggestionAdapter(@NonNull OnItemClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void submitResults(@Nullable List<Movie> movies, @Nullable String query) {
        String q = query == null ? "" : query;
        List<Row> rows = new ArrayList<>();
        if (movies != null) {
            for (Movie m : movies) {
                if (m != null) rows.add(new Row(m, q));
            }
        }
        submitList(rows);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_suggestion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        Glide.with(holder.ivThumb).clear(holder.ivThumb);
        super.onViewRecycled(holder);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardThumb;
        final ImageView ivThumb;
        final TextView tvTitle;
        final TextView tvType;
        final TextView tvMeta;
        final TextView tvSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardThumb = itemView.findViewById(R.id.card_thumb);
            ivThumb = itemView.findViewById(R.id.iv_thumb);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvType = itemView.findViewById(R.id.tv_type);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
        }

        void bind(@NonNull Row row, @NonNull OnItemClickListener listener) {
            Movie m = row.movie;
            String mediaType = m.getMediaType();
            boolean isPerson = "person".equalsIgnoreCase(mediaType);
            boolean isTv = "tv".equalsIgnoreCase(mediaType);

            // Title with highlighted match
            tvTitle.setText(highlight(m.getTitle(), row.query));

            // Type label + meta
            tvType.setText(isPerson ? "PERSON" : isTv ? "SERIES" : "MOVIE");
            String releaseDate = m.getReleaseDate();
            if (isPerson) {
                String meta = releaseDate != null ? releaseDate : "";
                tvMeta.setText(meta);
                tvMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);
            } else {
                String year = (releaseDate != null && releaseDate.length() >= 4) ? releaseDate.substring(0, 4) : "";
                double rating = m.getVoteAverage();
                if (rating > 0) {
                    String ratingStr = String.format(Locale.US, "★ %.1f", rating);
                    String text = year.isEmpty() ? ratingStr : (year + "  •  " + ratingStr);
                    SpannableString spannable = new SpannableString(text);
                    int ratingIndex = text.indexOf("★");
                    if (ratingIndex != -1) {
                        spannable.setSpan(
                                new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), R.color.movieed_accent)),
                                ratingIndex,
                                text.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    }
                    tvMeta.setText(spannable);
                    tvMeta.setVisibility(View.VISIBLE);
                } else {
                    tvMeta.setText(year);
                    tvMeta.setVisibility(year.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }

            // People get a second line
            String overview = m.getOverview();
            if (isPerson && overview != null && !overview.isEmpty()) {
                tvSubtitle.setText(overview);
                tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }

            // Thumbnail shape: poster for titles, round avatar for people
            applyThumbShape(isPerson);

            String path = m.getPosterPath();
            if (path != null && !path.isEmpty()) {
                Glide.with(ivThumb)
                        .load((isPerson ? PROFILE_BASE : POSTER_BASE) + path)
                        .placeholder(R.color.movieed_surface_elevated)
                        .error(R.color.movieed_surface_elevated)
                        .into(ivThumb);
            } else {
                Glide.with(ivThumb).clear(ivThumb);
                ivThumb.setImageDrawable(new ColorDrawable(ContextCompat.getColor(
                        itemView.getContext(), R.color.movieed_surface_elevated)));
            }

            itemView.setOnClickListener(v -> listener.onItemClick(m));
        }

        private void applyThumbShape(boolean isPerson) {
            float d = itemView.getResources().getDisplayMetrics().density;
            android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) cardThumb.getLayoutParams();
            if (isPerson) {
                lp.width = Math.round(52 * d);
                lp.height = Math.round(52 * d);
                cardThumb.setRadius(26 * d);
            } else {
                lp.width = Math.round(46 * d);
                lp.height = Math.round(69 * d);
                cardThumb.setRadius(8 * d);
            }
            lp.gravity = android.view.Gravity.CENTER;
            cardThumb.setLayoutParams(lp);
        }

        private CharSequence highlight(@Nullable String title, @NonNull String query) {
            if (title == null) return "";
            String q = query.trim();
            if (q.isEmpty()) return title;

            int start = title.toLowerCase(Locale.ROOT).indexOf(q.toLowerCase(Locale.ROOT));
            if (start < 0) return title;
            int end = Math.min(title.length(), start + q.length());

            SpannableString s = new SpannableString(title);
            s.setSpan(new ForegroundColorSpan(
                            ContextCompat.getColor(itemView.getContext(), R.color.movieed_accent)),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return s;
        }
    }
}
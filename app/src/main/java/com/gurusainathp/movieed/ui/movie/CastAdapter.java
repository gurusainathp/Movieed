package com.gurusainathp.movieed.ui.movie;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gurusainathp.movieed.R;
import com.gurusainathp.movieed.data.remote.dto.TmdbCastDto;

import java.util.ArrayList;
import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.CastViewHolder> {

    public interface OnCastClickListener {
        void onCastClick(TmdbCastDto cast);
    }

    private final List<TmdbCastDto> castList = new ArrayList<>();
    private final OnCastClickListener listener;

    public CastAdapter(OnCastClickListener listener) {
        this.listener = listener;
    }

    public void setCastList(List<TmdbCastDto> newCastList) {
        castList.clear();
        if (newCastList != null) {
            castList.addAll(newCastList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cast, parent, false);
        return new CastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CastViewHolder holder, int position) {
        holder.bind(castList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return castList.size();
    }

    static class CastViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivProfile;
        private final TextView tvName;
        private final TextView tvCharacter;

        public CastViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_cast_profile);
            tvName = itemView.findViewById(R.id.tv_cast_name);
            tvCharacter = itemView.findViewById(R.id.tv_cast_character);
        }

        public void bind(TmdbCastDto cast, OnCastClickListener listener) {
            if (cast == null) return;

            tvName.setText(cast.getName());
            tvCharacter.setText(cast.getCharacter());

            if (cast.getProfilePath() != null) {
                Glide.with(itemView.getContext())
                        .load("https://image.tmdb.org/t/p/w185" + cast.getProfilePath())
                        .placeholder(R.color.movieed_surface_elevated)
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.color.movieed_surface_elevated);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCastClick(cast);
                }
            });
        }
    }
}

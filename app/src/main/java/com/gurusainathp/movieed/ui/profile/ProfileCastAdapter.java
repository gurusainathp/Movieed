package com.gurusainathp.movieed.ui.profile;

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

public class ProfileCastAdapter extends RecyclerView.Adapter<ProfileCastAdapter.CastViewHolder> {

    public interface OnPersonClickListener {
        void onPersonClick(TmdbCastDto person);
    }

    private final List<TmdbCastDto> people = new ArrayList<>();
    private final OnPersonClickListener listener;

    public ProfileCastAdapter(OnPersonClickListener listener) {
        this.listener = listener;
    }

    public void setPeople(List<TmdbCastDto> newPeople) {
        people.clear();
        if (newPeople != null) {
            people.addAll(newPeople);
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
        holder.bind(people.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return people.size();
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

        public void bind(TmdbCastDto person, OnPersonClickListener listener) {
            if (person == null) return;

            tvName.setText(person.getName());
            if (tvCharacter != null) {
                if (person.getCharacter() != null && !person.getCharacter().isEmpty()) {
                    tvCharacter.setText(person.getCharacter());
                    tvCharacter.setVisibility(View.VISIBLE);
                } else {
                    tvCharacter.setVisibility(View.GONE);
                }
            }

            if (person.getProfilePath() != null && !person.getProfilePath().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load("https://image.tmdb.org/t/p/w500" + person.getProfilePath())
                        .placeholder(R.color.movieed_surface_elevated)
                        .error(R.color.movieed_surface_elevated)
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.color.movieed_surface_elevated);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPersonClick(person);
                }
            });
        }
    }
}

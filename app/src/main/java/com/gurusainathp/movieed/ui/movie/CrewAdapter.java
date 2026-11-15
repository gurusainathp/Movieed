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
import com.gurusainathp.movieed.data.remote.dto.TmdbCrewDto;

import java.util.ArrayList;
import java.util.List;

public class CrewAdapter extends RecyclerView.Adapter<CrewAdapter.CrewViewHolder> {

    public interface OnCrewClickListener {
        void onCrewClick(TmdbCrewDto crew);
    }

    private final List<TmdbCrewDto> crewList = new ArrayList<>();
    private final OnCrewClickListener listener;

    public CrewAdapter(OnCrewClickListener listener) {
        this.listener = listener;
    }

    public void setCrewList(List<TmdbCrewDto> newCrewList) {
        crewList.clear();
        if (newCrewList != null) {
            // Filter key crew roles: Director, Writer, Screenplay, Producer, Director of Photography, Original Music Composer
            for (TmdbCrewDto crew : newCrewList) {
                String job = crew.getJob();
                if (job != null && (job.equals("Director") || job.equals("Writer") || job.equals("Screenplay") ||
                        job.equals("Producer") || job.equals("Director of Photography") || job.equals("Original Music Composer"))) {
                    crewList.add(crew);
                }
            }
            if (crewList.isEmpty()) {
                crewList.addAll(newCrewList); // Fallback if no specific role matched
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CrewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cast, parent, false);
        return new CrewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CrewViewHolder holder, int position) {
        holder.bind(crewList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return crewList.size();
    }

    static class CrewViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivProfile;
        private final TextView tvName;
        private final TextView tvJob;

        public CrewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_cast_profile);
            tvName = itemView.findViewById(R.id.tv_cast_name);
            tvJob = itemView.findViewById(R.id.tv_cast_character);
        }

        public void bind(TmdbCrewDto crew, OnCrewClickListener listener) {
            if (crew == null) return;

            tvName.setText(crew.getName());
            tvJob.setText(crew.getJob());

            if (crew.getProfilePath() != null) {
                Glide.with(itemView.getContext())
                        .load("https://image.tmdb.org/t/p/w185" + crew.getProfilePath())
                        .placeholder(R.color.movieed_surface_elevated)
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.color.movieed_surface_elevated);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCrewClick(crew);
                }
            });
        }
    }
}

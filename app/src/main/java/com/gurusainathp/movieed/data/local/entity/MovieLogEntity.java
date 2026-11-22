package com.gurusainathp.movieed.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.gurusainathp.movieed.domain.model.Reaction;

@Entity(
        tableName = "movie_logs",
        foreignKeys = @ForeignKey(
                entity = MovieEntity.class,
                parentColumns = "tmdbId",
                childColumns = "movieId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("movieId")}
)
public class MovieLogEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long movieId;
    private String reaction; // "LOVE", "LIKE", "UMM", "DISLIKE", "HATE"
    private boolean isWatchlist;
    private long createdAt;
    private long updatedAt;

    public MovieLogEntity(long movieId, String reaction, boolean isWatchlist, long createdAt, long updatedAt) {
        this.movieId = movieId;
        this.reaction = reaction;
        this.isWatchlist = isWatchlist;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMovieId() { return movieId; }
    public void setMovieId(long movieId) { this.movieId = movieId; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public boolean isWatchlist() { return isWatchlist; }
    public void setWatchlist(boolean watchlist) { isWatchlist = watchlist; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public static String getEmojiForReaction(String reaction) {
        Reaction r = Reaction.fromRawString(reaction);
        return r != null ? r.getEmoji() : "";
    }

    public static String getLabelForReaction(String reaction) {
        Reaction r = Reaction.fromRawString(reaction);
        return r != null ? r.getLabel() : "";
    }
}

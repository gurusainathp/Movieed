package com.gurusainathp.movieed.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "movie_logs",
        foreignKeys = @ForeignKey(
                entity = MovieEntity.class,
                parentColumns = "id",
                childColumns = "movieId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("movieId")}
)
public class MovieLogEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long movieId;
    private String watchedDate;
    private float rating;
    private String review;
    private String privateNote;
    private long createdAt;
    private long updatedAt;

    public MovieLogEntity(long movieId, String watchedDate, float rating,
                          String review, String privateNote, long createdAt, long updatedAt) {
        this.movieId = movieId;
        this.watchedDate = watchedDate;
        this.rating = rating;
        this.review = review;
        this.privateNote = privateNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMovieId() { return movieId; }
    public void setMovieId(long movieId) { this.movieId = movieId; }

    public String getWatchedDate() { return watchedDate; }
    public void setWatchedDate(String watchedDate) { this.watchedDate = watchedDate; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public String getPrivateNote() { return privateNote; }
    public void setPrivateNote(String privateNote) { this.privateNote = privateNote; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}

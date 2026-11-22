package com.gurusainathp.movieed.data.local.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.gurusainathp.movieed.data.local.dao.DismissedMovieDao;
import com.gurusainathp.movieed.data.local.dao.MovieDao;
import com.gurusainathp.movieed.data.local.dao.MovieLogDao;
import com.gurusainathp.movieed.data.local.entity.DismissedMovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;

@Database(entities = {MovieEntity.class, MovieLogEntity.class, DismissedMovieEntity.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "movieed_database";
    private static volatile AppDatabase INSTANCE;

    public abstract MovieDao movieDao();
    public abstract MovieLogDao movieLogDao();
    public abstract DismissedMovieDao dismissedMovieDao();

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE movies ADD COLUMN genreIds TEXT");
            database.execSQL("ALTER TABLE movies ADD COLUMN originalLanguage TEXT");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `dismissed_movies` (`tmdbId` INTEGER NOT NULL, `dismissedAt` INTEGER NOT NULL, PRIMARY KEY(`tmdbId`))");
        }
    };

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

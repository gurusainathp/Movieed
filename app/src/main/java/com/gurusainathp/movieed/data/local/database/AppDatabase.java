package com.gurusainathp.movieed.data.local.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.gurusainathp.movieed.data.local.dao.MovieDao;
import com.gurusainathp.movieed.data.local.dao.MovieLogDao;
import com.gurusainathp.movieed.data.local.entity.MovieEntity;
import com.gurusainathp.movieed.data.local.entity.MovieLogEntity;

@Database(entities = {MovieEntity.class, MovieLogEntity.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "movieed_database";
    private static volatile AppDatabase INSTANCE;

    public abstract MovieDao movieDao();
    public abstract MovieLogDao movieLogDao();

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
            database.execSQL("ALTER TABLE movies ADD COLUMN voteAverage REAL NOT NULL DEFAULT 0.0");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE movies ADD COLUMN isTv INTEGER NOT NULL DEFAULT 0");
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}

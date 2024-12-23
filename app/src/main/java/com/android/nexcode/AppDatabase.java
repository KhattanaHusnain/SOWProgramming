package com.android.nexcode;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Course.class, Topic.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CourseDao courseDao();
    public abstract TopicDao topicDao();
    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    // Singleton method to get the database instance
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "app_database"
                            ).fallbackToDestructiveMigration() // Handle migrations (optional)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

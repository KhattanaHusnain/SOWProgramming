package com.android.nexcode.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.android.nexcode.course.Course;
import com.android.nexcode.course.CourseDao;
import com.android.nexcode.course.Topic;
import com.android.nexcode.course.TopicDao;
import com.android.nexcode.home.UserProgress;
import com.android.nexcode.home.UserProgressDao;

@Database(entities = {Course.class, Topic.class, UserProgress.class}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CourseDao courseDao();
    public abstract TopicDao topicDao();
    public abstract UserProgressDao userProgressDao();

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
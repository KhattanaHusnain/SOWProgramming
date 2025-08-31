package com.sowp.user.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.sowp.user.models.Course;
import com.sowp.user.repositories.roomdb.CourseDao;
import com.sowp.user.models.Topic;
import com.sowp.user.repositories.roomdb.TopicDao;
import com.sowp.user.utils.Converters;
import com.sowp.user.utils.DatabaseUtils;

import net.sqlcipher.database.SupportFactory;

@Database(entities = {Course.class, Topic.class}, version = 15, exportSchema = false)
@TypeConverters({Converters.class})
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
                    try {
                        SupportFactory factory = DatabaseUtils.getEncryptedFactory(context);

                        INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                        AppDatabase.class, "nexcode.db")
                                .openHelperFactory(factory)
                                .fallbackToDestructiveMigration()
                                .build();
                    } catch (Exception e) {
                        throw new RuntimeException("Error initializing encrypted DB", e);
                    }
                }
            }
        }
        return INSTANCE;
    }
}
package com.android.SOWProgramming.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.android.SOWProgramming.models.Course;
import com.android.SOWProgramming.repositories.roomdb.CourseDao;
import com.android.SOWProgramming.models.Topic;
import com.android.SOWProgramming.repositories.roomdb.TopicDao;
import com.android.SOWProgramming.utils.Converters;
import com.android.SOWProgramming.utils.DatabaseUtils;

import net.sqlcipher.database.SupportFactory;

@Database(entities = {Course.class, Topic.class}, version = 14, exportSchema = false)
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
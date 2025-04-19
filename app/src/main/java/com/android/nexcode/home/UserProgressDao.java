package com.android.nexcode.home;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE userId = :userId ORDER BY lastAccessTimestamp DESC")
    LiveData<List<UserProgress>> getUserProgressByUserId(long userId);

    @Query("SELECT * FROM user_progress WHERE completionPercentage > 0 AND completionPercentage < 100 ORDER BY lastAccessTimestamp DESC")
    LiveData<List<UserProgress>> getUserInProgressCourses();

    @Query("SELECT * FROM user_progress ORDER BY lastAccessTimestamp DESC LIMIT 1")
    LiveData<UserProgress> getLastAccessedCourse();

    @Query("SELECT COUNT(*) FROM user_progress WHERE completionPercentage = 100")
    LiveData<Integer> getCompletedCoursesCount();

    @Query("SELECT SUM(timeSpentMinutes)/60 FROM user_progress")
    LiveData<Integer> getTotalHoursSpent();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserProgress(UserProgress progress);

    @Update
    void updateUserProgress(UserProgress progress);

    @Query("DELETE FROM user_progress WHERE id = :progressId")
    void deleteUserProgress(long progressId);
}
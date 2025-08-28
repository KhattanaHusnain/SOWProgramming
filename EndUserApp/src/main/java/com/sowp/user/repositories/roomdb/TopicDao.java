package com.sowp.user.repositories.roomdb;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

import com.sowp.user.models.Topic;

import java.util.List;

@Dao
public interface TopicDao {
    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Topic topic);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Topic> topics);

    // Update operations
    @Update
    int update(Topic topic);

    @Update
    int updateAll(List<Topic> topics);

    // Delete operations
    @Delete
    int delete(Topic topic);

    @Query("DELETE FROM topics WHERE courseId = :courseId")
    int deleteTopicsForCourse(int courseId);

    @Query("DELETE FROM topics")
    int deleteAllTopics();

    // Query operations
    @Query("SELECT * FROM topics WHERE orderIndex = :topicId")
    LiveData<Topic> getTopicById(int topicId);

    @Query("SELECT * FROM topics WHERE courseId = :courseId")
    LiveData<List<Topic>> getTopicsForCourse(int courseId);

    @Query("SELECT * FROM topics WHERE courseId = :courseId ORDER BY name ASC")
    LiveData<List<Topic>> getTopicsForCourseOrderedByName(int courseId);

    @Query("SELECT * FROM topics WHERE name LIKE '%' || :query || '%' AND courseId = :courseId")
    LiveData<List<Topic>> searchTopicsByCourse(String query, int courseId);

    @Query("SELECT COUNT(*) FROM topics WHERE courseId = :courseId")
    LiveData<Integer> getTopicCountForCourse(int courseId);

    // Transaction operations
    @Transaction
    @Query("SELECT * FROM topics WHERE courseId = :courseId AND orderIndex = :topicId")
    LiveData<Topic> getTopicWithDetails(int courseId, int topicId);

    @Query("SELECT EXISTS(SELECT 1 FROM topics WHERE courseId = :courseId AND orderIndex = :topicId)")
    LiveData<Boolean> topicExists(int courseId, int topicId);
}
package com.android.nexcode;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface TopicDao {

    @Insert
    void insert(Topic topic);

    @Update
    void update(Topic topic);

    @Delete
    void delete(Topic topic);

    @Query("SELECT * FROM topics WHERE course_id = :courseId")
    List<Topic> getTopicsForCourse(int courseId);
}

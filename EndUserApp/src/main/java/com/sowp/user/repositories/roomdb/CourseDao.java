package com.sowp.user.repositories.roomdb;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sowp.user.models.Course;

import java.util.List;

@Dao
public interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Course course);

    @Update
    void update(Course course);

    @Delete
    void delete(Course course);

    @Query("SELECT * FROM courses")
    LiveData<List<Course>> getAllCoursesLive();

    @Query("SELECT * FROM courses WHERE id = :courseId")
    Course getCourseById(int courseId);

    @Query("SELECT * FROM courses WHERE members > 50") // Example threshold for popularity
    LiveData<List<Course>> getPopularCourses();


    @Query("SELECT * FROM courses WHERE title LIKE :query")
    LiveData<List<Course>> searchCourses(String query);

    @Query("SELECT COUNT(*) FROM courses")
    int getCourseCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCourses(List<Course> courses);

    @Query("DELETE FROM courses WHERE id = :courseId")
    void deleteCourseById(int courseId);

}
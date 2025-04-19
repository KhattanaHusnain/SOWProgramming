package com.android.nexcode.course;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class CourseWithTopics {

    @Embedded
    public Course course;

    @Relation(
            parentColumn = "id",
            entityColumn = "courseId"
    )
    public List<Topic> topics;
}


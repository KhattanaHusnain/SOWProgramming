package com.sowp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.sowp.admin.assignmentmanagement.AssignmentManagementActivity;
import com.sowp.admin.coursemanagement.CoursesManagementActivity;
import com.sowp.admin.quizmanagement.QuizManagementActivity;
import com.sowp.admin.topicmanagement.TopicManagementActivity;
import com.sowp.admin.usermanagement.UserManagementActivity;

public class MainActivity extends AppCompatActivity {

    CardView courseManagement;
    CardView userManagement;
    CardView quizManagement;
    CardView assignmentManagement;
    CardView topicManagement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        courseManagement = findViewById(R.id.cardCourseManagement);
        userManagement = findViewById(R.id.cardUserManagement);
        quizManagement = findViewById(R.id.cardQuizManagement);
        assignmentManagement = findViewById(R.id.cardAssignmentManagement);
        topicManagement = findViewById(R.id.cardTopicManagement);

        courseManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CoursesManagementActivity.class);
                startActivity(intent);
            }
        });
        userManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UserManagementActivity.class);
                startActivity(intent);

            }
        });

        quizManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, QuizManagementActivity.class);
                startActivity(intent);
            }
        });
        assignmentManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this, AssignmentManagementActivity.class);
                startActivity(intent);
            }
        });
        topicManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this, TopicManagementActivity.class);
                startActivity(intent);
            }
        });

    }

}
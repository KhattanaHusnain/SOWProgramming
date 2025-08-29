package com.sowp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

public class CoursesManagementActivity extends AppCompatActivity {
    TextView tvTotalCourses;
    TextView tvActiveCourses;
    TextView tvTotalTopics;
    CardView cardViewCourses,cardAddViewCourse,cardrEditCourses;
    CardView cardAddTopics,cardEditTopics,cardViewTopics;
    CardView cardManage,cardAnalytics,cardSetting;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);
        tvTotalCourses= findViewById(R.id.tvTotalCourses);
        tvActiveCourses= findViewById(R.id.tvActiveCourses);
        tvTotalTopics= findViewById(R.id.tvTotalCourses);
        cardViewCourses= findViewById(R.id.cardViewCourses);
        cardAddViewCourse= findViewById(R.id.cardViewCourses);
        cardrEditCourses= findViewById(R.id.cardEditCourses);
        cardAddTopics= findViewById(R.id.cardAddTopic);
        cardEditTopics= findViewById(R.id.cardEditTopic);
        cardViewTopics= findViewById(R.id.cardViewTopics);
        cardManage= findViewById(R.id.cardManageCategories);
        cardAnalytics= findViewById(R.id.cardManageCategories);
        cardSetting= findViewById(R.id.cardCourseSettings);

        cardViewCourses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CoursesManagementActivity.this, "view all courses", Toast.LENGTH_SHORT).show();


            }
        });
        cardAddViewCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(CoursesManagementActivity.this, AddCourseActivity.class);
                startActivity(intent);
            }
        });
        cardEditTopics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(CoursesManagementActivity.this, EditCoursesActivity.class);
                startActivity(intent);
            }
        });
        cardAddTopics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(CoursesManagementActivity.this, AddTopicActivity.class);
                startActivity(intent);
            }
        });
        cardEditTopics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(CoursesManagementActivity.this, EditTopicActivity.class);
                startActivity(intent);
            }
        });
        cardViewTopics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(CoursesManagementActivity.this, ViewTopicsActivity.class);
                startActivity(intent);
            }
        });
        cardManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CoursesManagementActivity.this, "Manage curses category", Toast.LENGTH_SHORT).show();
            }
        });
        cardAnalytics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CoursesManagementActivity.this, "Courses analytics", Toast.LENGTH_SHORT).show();
            }
        });
        cardSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CoursesManagementActivity.this, "Setting", Toast.LENGTH_SHORT).show();
            }
        });


    }

}
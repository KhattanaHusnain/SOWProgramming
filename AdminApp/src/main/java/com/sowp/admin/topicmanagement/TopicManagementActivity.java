package com.sowp.admin.topicmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.ViewCoursesActivity;

public class TopicManagementActivity extends AppCompatActivity {
    ImageView btn_back;
    LinearLayout cardViewTopics;
    LinearLayout uploadTopics;
    TextView txtTotalTopics;
    TextView txtActiveTopics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_management);

        // Initialize views
    }

}
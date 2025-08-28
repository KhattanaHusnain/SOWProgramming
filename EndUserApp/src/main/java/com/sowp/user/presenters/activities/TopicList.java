package com.sowp.user.presenters.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.repositories.firebase.TopicRepository;
import com.sowp.user.adapters.TopicListAdapter;
import com.sowp.user.models.Topic;

import java.util.ArrayList;
import java.util.List;

public class TopicList extends AppCompatActivity {
    private static final String TAG = "TopicList";

    private RecyclerView recyclerView;
    private View emptyView;

    private TopicListAdapter adapter;
    private int courseId;
    private TopicRepository topicRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        // Find views
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Initialize Firebase
        topicRepository = new TopicRepository();
        // Initialize executor

        // Get course details
        courseId = getIntent().getIntExtra("courseID", 0);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup RecyclerView
        adapter = new TopicListAdapter(this, new ArrayList<>(), "ONLINE");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        loadTopicsFromFirestore();
    }


    private void loadTopicsFromFirestore() {
        topicRepository.loadTopicsOfCourse(courseId, new TopicRepository.Callback() {
            @Override
            public void onSuccess(List<Topic> topics) {
                adapter.updateTopics(topics);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(TopicList.this, message, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
package com.android.SOWProgramming.presenters.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.SOWProgramming.R;
import com.android.SOWProgramming.adapters.TopicListAdapter;
import com.android.SOWProgramming.database.AppDatabase;
import com.android.SOWProgramming.models.Topic;
import com.android.SOWProgramming.repositories.roomdb.TopicDao;

import java.util.ArrayList;
import java.util.List;

public class OfflineTopicListActivity extends AppCompatActivity {

    private static final String TAG = "OfflineTopicListActivity";
    TopicDao topicDao;
    RecyclerView recyclerView;
    TopicListAdapter adapter;
    LiveData<List<Topic>> topics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_offline_topic_list);
        int id = getIntent().getIntExtra("ID", 0);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TopicListAdapter(this, new ArrayList<>(), "OFFLINE");
        recyclerView.setAdapter(adapter);
        topicDao = AppDatabase.getInstance(this).topicDao();
        topics = topicDao.getTopicsForCourse(id);
        topics.observe(this, new Observer<List<Topic>>() {
            @Override
            public void onChanged(List<Topic> topics) {
                adapter.updateTopics(topics);
            }
        });
    }
}
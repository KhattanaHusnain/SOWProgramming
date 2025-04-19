package com.android.nexcode.course;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.nexcode.R;
import com.android.nexcode.database.AppDatabase;
import com.android.nexcode.utils.NetworkUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TopicList extends AppCompatActivity {
    private static final String TAG = "TopicList";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyView;
    private View networkErrorView;

    private TopicListAdapter adapter;
    private TopicDao topicDao;
    private int courseId;
    private FirebaseFirestore db;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        // Find views
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        emptyView = findViewById(R.id.empty_view);
        networkErrorView = findViewById(R.id.network_error_view);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        // Initialize executor
        executor = Executors.newSingleThreadExecutor();

        // Get course details
        courseId = getIntent().getIntExtra("courseID", 0);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize database
        topicDao = AppDatabase.getInstance(this).topicDao();
        loadTopicsFromFirestore();

        // Setup RecyclerView
        adapter = new TopicListAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set up pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary, R.color.colorAccent, R.color.colorPrimaryDark);

        // Observe local database changes
        LiveData<List<Topic>> topics = topicDao.getTopicsForCourse(courseId);
        topics.observe(this, this::updateUI);

        // Initial data load
        refreshData();
    }

    private void refreshData() {
        swipeRefreshLayout.setRefreshing(true);

        if (NetworkUtils.isNetworkAvailable(this)) {
            loadTopicsFromFirestore();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            networkErrorView.setVisibility(View.VISIBLE);
        }
    }

    private void updateUI(List<Topic> topicList) {
        swipeRefreshLayout.setRefreshing(false);

        if (topicList != null && !topicList.isEmpty()) {
            adapter.updateTopics(topicList);
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            networkErrorView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void loadTopicsFromFirestore() {
        networkErrorView.setVisibility(View.GONE);

        db.collection("Topic")
                .whereEqualTo("courseId", courseId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Topic> topicsList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        try {
                            // Extract document ID
                            String docId = document.getId();
                            int topicId = Integer.parseInt(docId);
                            String name = document.getString("name");
                            String description = document.getString("description");
                            String content = document.getString("content");
                            String videoID = document.getString("videoID");
                            Topic topic = new Topic(
                                    topicId,
                                    courseId,
                                    name,
                                    description,
                                    content,
                                    videoID );

                            topicsList.add(topic);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing topic: " + document.getId(), e);
                        }
                    }

                    // Handle the case where no topics were found
                    if (topicsList.isEmpty()) {
                        runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            emptyView.setVisibility(View.VISIBLE);
                            Toast.makeText(TopicList.this, "No topics found for this course", Toast.LENGTH_SHORT).show();
                            recyclerView.setVisibility(View.GONE);
                        });
                        return;
                    }

                    // Update local database with the fetched topics
                    executor.execute(() -> {
                        try {
                            // Use a transaction-like approach
                            topicDao.insertAll(topicsList);

                            // LiveData observer will handle UI updates
                        } catch (Exception e) {
                            Log.e(TAG, "Database operation failed", e);
                            runOnUiThread(() -> {
                                swipeRefreshLayout.setRefreshing(false);
                                Toast.makeText(TopicList.this, "Database operation failed", Toast.LENGTH_SHORT).show();
                                networkErrorView.setVisibility(View.VISIBLE);
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading topics from Firestore", e);
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        networkErrorView.setVisibility(View.VISIBLE);
                    });
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
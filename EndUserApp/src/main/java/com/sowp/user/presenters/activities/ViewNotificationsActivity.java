package com.sowp.user.presenters.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.reflect.TypeToken;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import com.sowp.user.R;
import com.sowp.user.adapters.NotificationsAdapter;
import com.sowp.user.models.Notification;


import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ViewNotificationsActivity extends AppCompatActivity {

    private static final String TAG = "ViewNotificationsActivity";
    private static final String PREFS_NAME = "notifications_prefs";
    private static final String KEY_CACHED_NOTIFICATIONS = "cached_notifications";
    private static final String KEY_LAST_UPDATED = "last_updated";
    private static final int MAX_NOTIFICATIONS = 20;

    // UI Components
    private Toolbar toolbar;
    private RecyclerView rvNotifications;
    private LinearLayout layoutEmptyState, layoutLoading, layoutErrorState;
    private TextView tvNotificationsCount, tvLastUpdated, tvErrorMessage;
    private ImageButton btnRefresh;
    private Button btnRefreshEmpty, btnRetry;

    // Data and Firebase
    private FirebaseFirestore firestore;
    private ListenerRegistration notificationListener;
    private NotificationsAdapter adapter;
    private List<Notification> notificationsList;
    private SharedPreferences preferences;
    private Gson gson;
    private SimpleDateFormat lastUpdatedFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_notifications);

        initializeComponents();
        setupUI();
        loadCachedNotifications();
        setupFirestoreListener();
    }

    private void initializeComponents() {
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();

        // Initialize SharedPreferences and Gson
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        gson = new Gson();
        lastUpdatedFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        // Initialize data structures
        notificationsList = new ArrayList<>();

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar);
        rvNotifications = findViewById(R.id.rv_notifications);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutErrorState = findViewById(R.id.layout_error_state);
        tvNotificationsCount = findViewById(R.id.tv_notifications_count);
        tvLastUpdated = findViewById(R.id.tv_last_updated);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnRefreshEmpty = findViewById(R.id.btn_refresh_empty);
        btnRetry = findViewById(R.id.btn_retry);
    }

    private void setupUI() {
        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Setup RecyclerView
        adapter = new NotificationsAdapter(this, notificationsList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        // Setup click listeners
        btnRefresh.setOnClickListener(v -> refreshNotifications());
        btnRefreshEmpty.setOnClickListener(v -> refreshNotifications());
        btnRetry.setOnClickListener(v -> refreshNotifications());

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        updateLastUpdatedTime();
    }

    private void loadCachedNotifications() {
        showLoading();

        try {
            String cachedJson = preferences.getString(KEY_CACHED_NOTIFICATIONS, "");
            if (!cachedJson.isEmpty()) {
                Type listType = new TypeToken<List<Notification>>(){}.getType();
                List<Notification> cachedNotifications = gson.fromJson(cachedJson, listType);

                if (cachedNotifications != null) {
                    // Filter out expired notifications
                    filterExpiredNotifications(cachedNotifications);

                    notificationsList.clear();
                    notificationsList.addAll(cachedNotifications);

                    Log.d(TAG, "Loaded " + notificationsList.size() + " cached notifications");
                }
            }

            updateUI();

        } catch (Exception e) {
            Log.e(TAG, "Error loading cached notifications", e);
            showError("Error loading cached notifications");
        }
    }

    private void setupFirestoreListener() {
        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = firestore.collection("Notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(MAX_NOTIFICATIONS)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to notifications", error);
                        showError("Error connecting to server: " + error.getMessage());
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        List<Notification> newNotifications = new ArrayList<>();

                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            try {
                                Notification notification = document.toObject(Notification.class);
                                if (notification != null && !notification.isExpired()) {
                                    newNotifications.add(notification);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing notification document", e);
                            }
                        }

                        // Update notifications list
                        updateNotificationsList(newNotifications);

                        // Cache the notifications
                        cacheNotifications();

                        Log.d(TAG, "Updated with " + newNotifications.size() + " notifications from Firestore");
                    }

                    updateUI();
                    updateLastUpdatedTime();
                });
    }

    private void updateNotificationsList(List<Notification> newNotifications) {
        // Clear current list and add new notifications
        notificationsList.clear();

        // Filter and add non-expired notifications
        for (Notification notification : newNotifications) {
            if (!notification.isExpired()) {
                notificationsList.add(notification);
            }
        }

        // Ensure we don't exceed the maximum limit
        if (notificationsList.size() > MAX_NOTIFICATIONS) {
            notificationsList = notificationsList.subList(0, MAX_NOTIFICATIONS);
        }

        runOnUiThread(() -> adapter.updateNotifications(notificationsList));
    }

    private void filterExpiredNotifications(List<Notification> notifications) {
        Iterator<Notification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            if (notification.isExpired()) {
                iterator.remove();
            }
        }
    }

    private void cacheNotifications() {
        try {
            String json = gson.toJson(notificationsList);
            preferences.edit()
                    .putString(KEY_CACHED_NOTIFICATIONS, json)
                    .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error caching notifications", e);
        }
    }

    private void refreshNotifications() {
        showLoading();

        // Re-setup the Firestore listener to force refresh
        setupFirestoreListener();

        Toast.makeText(this, "Refreshing notifications...", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        runOnUiThread(() -> {
            hideAllStates();

            if (notificationsList.isEmpty()) {
                layoutEmptyState.setVisibility(View.VISIBLE);
                tvNotificationsCount.setText("0 notifications");
            } else {
                rvNotifications.setVisibility(View.VISIBLE);
                String countText = notificationsList.size() == 1 ?
                        "1 notification" : notificationsList.size() + " notifications";
                tvNotificationsCount.setText(countText);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showLoading() {
        runOnUiThread(() -> {
            hideAllStates();
            layoutLoading.setVisibility(View.VISIBLE);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            hideAllStates();
            layoutErrorState.setVisibility(View.VISIBLE);
            tvErrorMessage.setText(message);
        });
    }

    private void hideAllStates() {
        rvNotifications.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.GONE);
    }

    private void updateLastUpdatedTime() {
        long lastUpdated = preferences.getLong(KEY_LAST_UPDATED, 0);
        String timeText;

        if (lastUpdated == 0) {
            timeText = "Last updated: Never";
        } else {
            timeText = "Last updated: " + lastUpdatedFormat.format(new Date(lastUpdated));
        }

        runOnUiThread(() -> tvLastUpdated.setText(timeText));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Remove expired notifications when activity resumes
        filterExpiredNotifications(notificationsList);
        adapter.notifyDataSetChanged();
        updateUI();

        // Update cache after filtering
        cacheNotifications();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
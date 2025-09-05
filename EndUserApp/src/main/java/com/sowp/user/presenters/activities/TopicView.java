package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sowp.user.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class TopicView extends AppCompatActivity {
    private WebView topicContent;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private Toolbar toolbar;
    private MaterialButton btnWatchVideo;

    private String topicName;
    private String topicContentHtml;
    private String topicVideoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_view);

        // Initialize views
        initializeViews();

        // Setup toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Configure WebView
        configureWebView();

        // Setup SwipeRefreshLayout
        setupSwipeRefresh();

        // Retrieve data from intent
        retrieveIntentData();

        // Load topic content
        loadTopicContent();

        // Setup video button
        setupVideoButton();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        topicContent = findViewById(R.id.topic_webview);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        btnWatchVideo = findViewById(R.id.btn_watch_video);
    }

    private void configureWebView() {
        WebSettings settings = topicContent.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        topicContent.setWebViewClient(new TopicWebViewClient());
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,        // instead of R.color.colorPrimary
                R.color.accent,         // instead of R.color.colorAccent
                R.color.primary_dark    // instead of R.color.colorPrimaryDark
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadTopicContent();
        });
    }

    private void retrieveIntentData() {
        Intent intent = getIntent();
        topicName = intent.getStringExtra("TOPIC_NAME");
        topicContentHtml = intent.getStringExtra("TOPIC_CONTENT");
        topicVideoId = intent.getStringExtra("VIDEO_ID");

        // Set default values if data is missing
        if (topicName == null || topicName.isEmpty()) {
            topicName = "Topic";
        }
        if (topicContentHtml == null || topicContentHtml.isEmpty()) {
            topicContentHtml = "<p>No content available</p>";
        }
    }

    private void loadTopicContent() {
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);

        // Set toolbar title
        getSupportActionBar().setTitle(topicName);

        // Load topic content with improved styling
        topicContent.loadDataWithBaseURL(null, topicContentHtml, "text/html", "UTF-8", null);
    }

    private void setupVideoButton() {
        btnWatchVideo.setOnClickListener(v -> {
            if (topicVideoId != null && !topicVideoId.isEmpty()) {
                // Launch video activity with video ID
                Intent videoIntent = new Intent(this, VideoPlayerActivity.class);
                videoIntent.putExtra("VIDEO_ID", topicVideoId);
                startActivity(videoIntent);
            } else {
                Snackbar.make(coordinatorLayout, "Video not available", Snackbar.LENGTH_SHORT).show();
            }
        });

        // Hide video button if no video ID is provided
        if (topicVideoId == null || topicVideoId.isEmpty()) {
            btnWatchVideo.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> loadTopicContent())
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        if (topicContent != null) {
            topicContent.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (topicContent != null) {
            topicContent.onResume();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (topicContent != null) {
            topicContent.loadUrl("about:blank");
            topicContent.destroy();
            topicContent = null;
        }
        super.onDestroy();
    }

    // Custom WebViewClient to handle errors and loading
    private class TopicWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                showError("Failed to load content");
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
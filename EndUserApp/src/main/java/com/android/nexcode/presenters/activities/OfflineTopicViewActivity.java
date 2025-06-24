package com.android.nexcode.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.nexcode.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class OfflineTopicViewActivity extends AppCompatActivity {

    private WebView topicContent;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private Toolbar toolbar;

    private String topicName;
    private String topicContentHtml;

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
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        topicContent = findViewById(R.id.topic_webview);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        coordinatorLayout = findViewById(R.id.coordinator_layout);
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
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadTopicContent();
        });
    }

    private void retrieveIntentData() {
        Intent intent = getIntent();
        topicName = intent.getStringExtra("TOPIC_NAME");
        topicContentHtml = intent.getStringExtra("TOPIC_CONTENT");

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
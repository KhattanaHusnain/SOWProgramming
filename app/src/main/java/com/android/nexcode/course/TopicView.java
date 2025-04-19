package com.android.nexcode.course;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.LiveData;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.nexcode.R;
import com.android.nexcode.database.AppDatabase;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class TopicView extends AppCompatActivity {
    private WebView topicContent;
    private WebView youtubeWebView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private FrameLayout fullscreenContainer;
    private FloatingActionButton fab;
    private boolean isFullScreen = false;
    private boolean isBookmarked = false;
    private int topicId;
    private CustomWebChromeClient webChromeClient;

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

        // Configure WebViews
        configureWebViews();

        // Setup SwipeRefreshLayout
        setupSwipeRefresh();

        // Retrieve topic ID from intent
        topicId = getIntent().getIntExtra("TOPIC_ID", 0);
        if (topicId > 0) {
            loadTopicContent(topicId);
        } else {
            showError("Invalid topic ID");
        }

        // Setup floating action button
        setupFab();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.app_bar_layout);
        topicContent = findViewById(R.id.topic_webview);
        youtubeWebView = findViewById(R.id.youtube_webview);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        coordinatorLayout = findViewById(R.id.coordinator_layout);
        fullscreenContainer = findViewById(R.id.fullscreen_container);
        fab = findViewById(R.id.fab);
    }

    private void configureWebViews() {
        // Configure Topic WebView
        WebSettings topicSettings = topicContent.getSettings();
        topicSettings.setJavaScriptEnabled(true);
        topicSettings.setDomStorageEnabled(true);
        topicSettings.setAllowFileAccess(false);
        topicSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        topicSettings.setLoadWithOverviewMode(true);
        topicSettings.setUseWideViewPort(true);
        topicContent.setWebViewClient(new TopicWebViewClient());

        // Configure YouTube WebView
        WebSettings youtubeSettings = youtubeWebView.getSettings();
        youtubeSettings.setJavaScriptEnabled(true);
        youtubeSettings.setDomStorageEnabled(true);
        youtubeSettings.setAllowFileAccess(false);
        youtubeSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        youtubeSettings.setMediaPlaybackRequiresUserGesture(false);

        // Initialize and set WebChromeClient
        webChromeClient = new CustomWebChromeClient();
        youtubeWebView.setWebChromeClient(webChromeClient);
        youtubeWebView.setWebViewClient(new TopicWebViewClient());

        // Add JavaScript interface for communication
        youtubeWebView.addJavascriptInterface(new JavaScriptInterface(), "AndroidPlayer");
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (topicId > 0) {
                loadTopicContent(topicId);
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void setupFab() {
        fab.setOnClickListener(view -> {
            isBookmarked = !isBookmarked;

            // Toggle between bookmark icons
            if (isBookmarked) {
                fab.setImageResource(R.drawable.ic_bookmark);
                Snackbar.make(view, "Topic bookmarked", Snackbar.LENGTH_SHORT).show();
                // Save bookmark to database
                saveBookmark(topicId);
            } else {
                fab.setImageResource(R.drawable.ic_bookmark_border);
                Snackbar.make(view, "Bookmark removed", Snackbar.LENGTH_SHORT).show();
                // Remove bookmark from database
                removeBookmark(topicId);
            }
        });

        // Check if already bookmarked
        checkIfBookmarked(topicId);
    }

    private void saveBookmark(int topicId) {
        // Implement logic to save bookmark to database
    }

    private void removeBookmark(int topicId) {
        // Implement logic to remove bookmark from database
    }

    private void checkIfBookmarked(int topicId) {
        // Query database to check if topic is already bookmarked
        // Update FAB icon accordingly
    }

    private void loadTopicContent(int topicId) {
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(true);

        // Fetch topic data from database
        LiveData<Topic> topicLiveData = AppDatabase.getInstance(this).topicDao().getTopicById(topicId);

        // Observe topic data
        topicLiveData.observe(this, topic -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (topic != null) {
                // Set toolbar title
                getSupportActionBar().setTitle(topic.getName());

                // Load YouTube video with improved HTML
                youtubeWebView.loadDataWithBaseURL(
                        "https://www.youtube.com",
                        getEnhancedYouTubeIframeHtml(topic.getVideoID()),
                        "text/html",
                        "UTF-8",
                        null
                );

                // Load topic content with improved styling
                String enhancedContent = wrapContentWithCSS(topic.getContent());
                topicContent.loadDataWithBaseURL(null, enhancedContent, "text/html", "UTF-8", null);
            } else {
                showError("Topic not found");
            }
        });
    }

    private String getEnhancedYouTubeIframeHtml(String videoId) {
        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">" +
                "    <style>" +
                "        body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #000; }" +
                "        .video-container { position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden; }" +
                "        .video-container iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }" +
                "        .fullscreen-button { position: absolute; bottom: 10px; right: 10px; background: rgba(0,0,0,0.5); color: white; " +
                "                            border: none; border-radius: 4px; padding: 5px 10px; cursor: pointer; z-index: 10; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"video-container\">" +
                "        <iframe id=\"youtube-player\" src=\"https://www.youtube.com/embed/" + videoId + "?enablejsapi=1&rel=0&playsinline=0&controls=1\"" +
                "            frameborder=\"0\" allowfullscreen=\"true\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\">" +
                "        </iframe>" +
                "        <button class=\"fullscreen-button\" onclick=\"toggleFullscreen()\">Fullscreen</button>" +
                "    </div>" +
                "    <script>" +
                "        var tag = document.createElement('script');" +
                "        tag.src = \"https://www.youtube.com/iframe_api\";" +
                "        var firstScriptTag = document.getElementsByTagName('script')[0];" +
                "        firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);" +
                "        " +
                "        var player;" +
                "        function onYouTubeIframeAPIReady() {" +
                "            player = new YT.Player('youtube-player', {" +
                "                events: {" +
                "                    'onReady': function(event) {" +
                "                        console.log('YouTube player ready');" +
                "                    }," +
                "                    'onStateChange': function(event) {" +
                "                        if (event.data == YT.PlayerState.PLAYING) {" +
                "                            window.AndroidPlayer.onVideoPlay();" +
                "                        } else if (event.data == YT.PlayerState.PAUSED) {" +
                "                            window.AndroidPlayer.onVideoPause();" +
                "                        } else if (event.data == YT.PlayerState.ENDED) {" +
                "                            window.AndroidPlayer.onVideoEnd();" +
                "                        }" +
                "                    }" +
                "                }" +
                "            });" +
                "        }" +
                "        " +
                "        function toggleFullscreen() {" +
                "            window.AndroidPlayer.toggleFullscreen();" +
                "        }" +
                "    </script>" +
                "</body>" +
                "</html>";
    }

    private String wrapContentWithCSS(String htmlContent) {
        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <style>" +
                "        body { font-family: 'Roboto', Arial, sans-serif; line-height: 1.6; color: #333; padding: 16px; }" +
                "        h1, h2, h3 { color: #2196F3; }" +
                "        h1 { font-size: 24px; }" +
                "        h2 { font-size: 20px; }" +
                "        h3 { font-size: 18px; }" +
                "        p { margin-bottom: 16px; }" +
                "        img { max-width: 100%; height: auto; display: block; margin: 16px auto; border-radius: 4px; }" +
                "        code { background-color: #f5f5f5; padding: 2px 4px; border-radius: 4px; font-family: monospace; }" +
                "        pre { background-color: #f5f5f5; padding: 16px; border-radius: 4px; overflow-x: auto; }" +
                "        a { color: #2196F3; text-decoration: none; }" +
                "        ul, ol { padding-left: 20px; margin-bottom: 16px; }" +
                "        blockquote { border-left: 4px solid #2196F3; padding-left: 16px; margin-left: 0; margin-right: 0; color: #666; }" +
                "        table { border-collapse: collapse; width: 100%; margin-bottom: 16px; }" +
                "        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }" +
                "        th { background-color: #f5f5f5; }" +
                "        .note { background-color: #e3f2fd; padding: 16px; border-radius: 4px; margin-bottom: 16px; }" +
                "        .warning { background-color: #fff3e0; padding: 16px; border-radius: 4px; margin-bottom: 16px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                htmlContent +
                "</body>" +
                "</html>";
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> {
                    if (topicId > 0) {
                        loadTopicContent(topicId);
                    }
                })
                .show();
    }

    // Method to enter fullscreen mode
    private void enterFullscreen() {
        if (isFullScreen) return;

        // Hide system UI (status bar, navigation bar)
        hideSystemUI();

        // Set orientation to landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Hide app bar and other UI elements
        appBarLayout.setVisibility(View.GONE);

        // Move the YouTube WebView to the fullscreen container
        ViewGroup parent = (ViewGroup) youtubeWebView.getParent();
        parent.removeView(youtubeWebView);

        // Configure fullscreen container
        fullscreenContainer.addView(youtubeWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Show fullscreen container
        fullscreenContainer.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);

        isFullScreen = true;
    }

    // Method to exit fullscreen mode
    private void exitFullscreen() {
        if (!isFullScreen) return;

        // Show system UI
        showSystemUI();

        // Set orientation back to user preference/unspecified
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Show app bar and other UI elements
        appBarLayout.setVisibility(View.VISIBLE);

        // Move YouTube WebView back to its original container
        fullscreenContainer.removeView(youtubeWebView);
        ViewGroup youtubeContainer = findViewById(R.id.youtube_container);
        youtubeContainer.addView(youtubeWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Hide fullscreen container and show other UI elements
        fullscreenContainer.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
        fab.setVisibility(View.VISIBLE);

        isFullScreen = false;
    }

    // Hide the system UI
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    // Show the system UI
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isFullScreen) {
            exitFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {
        pauseWebViews();
        super.onPause();
    }

    @Override
    protected void onResume() {
        resumeWebViews();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        destroyWebViews();
        super.onDestroy();
    }

    private void pauseWebViews() {
        youtubeWebView.onPause();
        topicContent.onPause();
    }

    private void resumeWebViews() {
        youtubeWebView.onResume();
        topicContent.onResume();
    }

    private void destroyWebViews() {
        if (youtubeWebView != null) {
            youtubeWebView.loadUrl("about:blank");
            youtubeWebView.destroy();
            youtubeWebView = null;
        }

        if (topicContent != null) {
            topicContent.loadUrl("about:blank");
            topicContent.destroy();
            topicContent = null;
        }
    }

    // JavaScript interface for communication between WebView and Android
    private class JavaScriptInterface {
        @android.webkit.JavascriptInterface
        public void toggleFullscreen() {
            runOnUiThread(() -> {
                if (isFullScreen) {
                    exitFullscreen();
                } else {
                    enterFullscreen();
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void onVideoPlay() {
            // Handle video play event
            // You could add analytics tracking here
        }

        @android.webkit.JavascriptInterface
        public void onVideoPause() {
            // Handle video pause event
        }

        @android.webkit.JavascriptInterface
        public void onVideoEnd() {
            // Handle video end event
            // Maybe show a "next topic" button
        }
    }

    // Custom WebViewClient to handle errors
    private class TopicWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                showError("Failed to load content");
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    // Custom WebChromeClient to handle fullscreen from the native YouTube player
    private class CustomWebChromeClient extends WebChromeClient {
        private View customView;
        private WebChromeClient.CustomViewCallback customViewCallback;
        private int originalOrientation;
        private int originalSystemUiVisibility;

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                onHideCustomView();
                return;
            }

            customView = view;
            originalOrientation = getRequestedOrientation();
            originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            customViewCallback = callback;

            // Enter fullscreen mode
            enterFullscreen();

            // Add the custom view to the fullscreen container
            fullscreenContainer.addView(customView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) {
                return;
            }

            // Exit fullscreen mode
            exitFullscreen();

            // Remove the custom view
            fullscreenContainer.removeView(customView);

            customView = null;
            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
        }
    }
}
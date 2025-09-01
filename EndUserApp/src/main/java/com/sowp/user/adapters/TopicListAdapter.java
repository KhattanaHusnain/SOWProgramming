package com.sowp.user.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.models.Topic;
import com.sowp.user.presenters.activities.OfflineTopicViewActivity;
import com.sowp.user.presenters.activities.TopicView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class TopicListAdapter extends RecyclerView.Adapter<TopicListAdapter.TopicViewHolder> {
    private final Context context;
    private final List<Topic> topics;
    String mode;
    private OnTopicClickListener onTopicClickListener;

    public interface OnTopicClickListener {
        void onTopicClicked(Topic topic);
    }

    public TopicListAdapter(Context context, List<Topic> topics, String mode) {
        this.context = context;
        this.topics = new ArrayList<>(topics);
        this.mode = mode;
    }

    public void setOnTopicClickListener(OnTopicClickListener listener) {
        this.onTopicClickListener = listener;
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_topic, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        Topic topic = topics.get(position);
        holder.bind(topic, context, mode, onTopicClickListener);
    }

    @Override
    public int getItemCount() {
        return topics != null ? topics.size() : 0;
    }

    public void updateTopics(List<Topic> newTopics) {
        this.topics.clear();
        if (newTopics != null) {
            this.topics.addAll(newTopics);
        }
        notifyDataSetChanged();
    }

    public void clearTopics() {
        this.topics.clear();
        notifyDataSetChanged();
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        private final TextView topicTitle;
        private final TextView topicDescription;
        private final TextView topicOrder;
        private final TextView topicCreatedDate;
        private final TextView topicViews;
        private final TextView topicTags;
        private final View videoIndicator;
        private final MaterialCardView cardView;

        TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            topicTitle = itemView.findViewById(R.id.topic_title);
            topicDescription = itemView.findViewById(R.id.topic_description);
            topicOrder = itemView.findViewById(R.id.topic_order);
            topicCreatedDate = itemView.findViewById(R.id.topic_created_date);
            topicViews = itemView.findViewById(R.id.topic_views);
            topicTags = itemView.findViewById(R.id.topic_tags);
            videoIndicator = itemView.findViewById(R.id.video_indicator);
            cardView = (MaterialCardView) itemView;
        }

        public void bind(Topic topic, Context context, String mode, OnTopicClickListener clickListener) {
            if (topic != null) {
                // Set basic info
                topicTitle.setText(topic.getName() != null ? topic.getName() : "Untitled Topic");
                topicDescription.setText(topic.getDescription() != null ? topic.getDescription() : "No description available");

                // Display order index
                topicOrder.setText("Lesson " + topic.getOrderIndex());

                // Format and display creation date
                if (topic.getCreatedAt() > 0) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String formattedDate = dateFormat.format(new Date(topic.getCreatedAt()));
                    topicCreatedDate.setText("Created: " + formattedDate);
                } else {
                    topicCreatedDate.setText("Created: Unknown");
                }

                // Display views count
                if (topicViews != null) {
                    topicViews.setText(String.valueOf(topic.getViews()));
                }

                // Display tags if available
                if (topicTags != null) {
                    if (topic.getTags() != null && !topic.getTags().trim().isEmpty()) {
                        topicTags.setText(topic.getTags());
                        topicTags.setVisibility(View.VISIBLE);
                    } else {
                        topicTags.setVisibility(View.GONE);
                    }
                }

                // Show video indicator if topic has video
                if (videoIndicator != null) {
                    if (topic.getVideoID() != null && !topic.getVideoID().trim().isEmpty()) {
                        videoIndicator.setVisibility(View.VISIBLE);
                    } else {
                        videoIndicator.setVisibility(View.GONE);
                    }
                }

                // Set click listeners
                itemView.setOnClickListener(v -> {
                    // Notify listener first (for view tracking)
                    if (clickListener != null) {
                        clickListener.onTopicClicked(topic);
                    }

                    // Then navigate
                    if (mode.equals("ONLINE")) {
                        navigateToTopicView(topic, context);
                    } else {
                        navigateToOfflineTopicView(topic, context);
                    }
                });
            }
        }

        protected void navigateToTopicView(Topic topic, Context context) {
            Intent intent = new Intent(context, TopicView.class);
            intent.putExtra("TOPIC_ID", topic.getOrderIndex());
            intent.putExtra("TOPIC_NAME", topic.getName());
            intent.putExtra("TOPIC_CONTENT", topic.getContent());
            intent.putExtra("VIDEO_ID", topic.getVideoID());
            intent.putExtra("COURSE_ID", topic.getCourseId());
            context.startActivity(intent);
        }

        private void navigateToOfflineTopicView(Topic topic, Context context) {
            Intent intent = new Intent(context, OfflineTopicViewActivity.class);
            intent.putExtra("TOPIC_ID", topic.getOrderIndex());
            intent.putExtra("TOPIC_NAME", topic.getName());
            intent.putExtra("TOPIC_CONTENT", topic.getContent());
            intent.putExtra("VIDEO_ID", topic.getVideoID());
            intent.putExtra("COURSE_ID", topic.getCourseId());
            context.startActivity(intent);
        }
    }
}
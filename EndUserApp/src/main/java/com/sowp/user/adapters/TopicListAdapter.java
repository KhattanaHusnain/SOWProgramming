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
import com.sowp.user.models.User;
import com.sowp.user.presenters.activities.TopicView;
import com.sowp.user.repositories.UserRepository;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class TopicListAdapter extends RecyclerView.Adapter<TopicListAdapter.TopicViewHolder> {
    private final Context context;
    private final List<Topic> topics;
    private final UserRepository userRepository;
    private OnTopicClickListener onTopicClickListener;

    public interface OnTopicClickListener {
        void onTopicClicked(Topic topic);
    }

    public TopicListAdapter(Context context, List<Topic> topics) {
        this.context = context;
        this.topics = new ArrayList<>(topics != null ? topics : new ArrayList<>());
        this.userRepository = new UserRepository(context);
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
        holder.bind(topic, context, userRepository, onTopicClickListener);
    }

    @Override
    public int getItemCount() {
        return topics.size();
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
        private final View videoIndicator;

        TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            topicTitle = itemView.findViewById(R.id.topic_title);
            topicDescription = itemView.findViewById(R.id.topic_description);
            topicOrder = itemView.findViewById(R.id.topic_order);
            topicCreatedDate = itemView.findViewById(R.id.topic_created_date);
            topicViews = itemView.findViewById(R.id.topic_views);
            videoIndicator = itemView.findViewById(R.id.video_indicator);
        }

        public void bind(Topic topic, Context context, UserRepository userRepository, OnTopicClickListener clickListener) {
            if (topic == null) return;

            setupTopicInfo(topic);
            setupVideoIndicator(topic);
            setupClickListener(topic, context, userRepository, clickListener);
        }

        private void setupTopicInfo(Topic topic) {
            // Set title and description
            topicTitle.setText(topic.getName() != null ? topic.getName() : "Untitled Topic");
            topicDescription.setText(topic.getDescription() != null ? topic.getDescription() : "No description available");

            // Set lesson order
            topicOrder.setText(String.valueOf(topic.getOrderIndex()));

            // Format creation date
            setupCreationDate(topic);

            // Set views count
            if (topicViews != null) {
                topicViews.setText(String.valueOf(topic.getViews()));
            }

        }

        private void setupCreationDate(Topic topic) {
            if (topicCreatedDate != null) {
                if (topic.getCreatedAt() > 0) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String formattedDate = dateFormat.format(new Date(topic.getCreatedAt()));
                    topicCreatedDate.setText("Created: " + formattedDate);
                } else {
                    topicCreatedDate.setText("Created: Unknown");
                }
            }
        }



        private void setupVideoIndicator(Topic topic) {
            if (videoIndicator != null) {
                String videoId = topic.getVideoID();
                if (videoId != null && !videoId.trim().isEmpty()) {
                    videoIndicator.setVisibility(View.VISIBLE);
                } else {
                    videoIndicator.setVisibility(View.GONE);
                }
            }
        }

        private void setupClickListener(Topic topic, Context context, UserRepository userRepository, OnTopicClickListener clickListener) {
            itemView.setOnClickListener(v -> {
                // Update progress first
                updateCourseProgress(topic, userRepository);

                // Notify listener
                if (clickListener != null) {
                    clickListener.onTopicClicked(topic);
                }

                // Navigate to topic view
                navigateToTopicView(topic, context);
            });
        }

        private void updateCourseProgress(Topic topic, UserRepository userRepository) {
            if (topic == null || userRepository == null) return;

            int courseId = topic.getCourseId();
            int topicId = topic.getOrderIndex();

            if (courseId <= 0) return;

            // Update course progress
            userRepository.addViewedTopic(courseId, topicId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    // Progress updated successfully
                }

                @Override
                public void onFailure(String message) {
                    // Handle silently
                }
            });

            // Update legacy viewed topics for backward compatibility
            userRepository.addViewedTopic(courseId, topicId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    // Updated successfully
                }

                @Override
                public void onFailure(String message) {
                    // Handle silently
                }
            });
        }

        private void navigateToTopicView(Topic topic, Context context) {
            Intent intent = new Intent(context, TopicView.class);
            intent.putExtra("TOPIC_ID", topic.getOrderIndex());
            intent.putExtra("TOPIC_NAME", topic.getName());
            intent.putExtra("TOPIC_CONTENT", topic.getContent());
            intent.putExtra("VIDEO_ID", topic.getVideoID());
            intent.putExtra("COURSE_ID", topic.getCourseId());
            context.startActivity(intent);
        }
    }
}
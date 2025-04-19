package com.android.nexcode.course;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TopicListAdapter extends RecyclerView.Adapter<TopicListAdapter.TopicViewHolder> {
    private final Context context;
    private final List<Topic> topics;

    public TopicListAdapter(Context context, List<Topic> topics) {
        this.context = context;
        this.topics = new ArrayList<>(topics);
    }

    @NonNull
    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.topic_item, parent, false);
        return new TopicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
        Topic topic = topics.get(position);
        holder.bind(topic, context);
    }

    @Override
    public int getItemCount() {
        return topics != null ? topics.size() : 0;
    }

    public void updateTopics(List<Topic> newTopics) {
        if (newTopics != null) {
            // Use DiffUtil to calculate differences and update efficiently
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new TopicDiffCallback(this.topics, newTopics));

            // Update data
            this.topics.clear();
            this.topics.addAll(newTopics);

            // Notify adapter with calculated diff
            diffResult.dispatchUpdatesTo(this);
        } else {
            this.topics.clear();
            notifyDataSetChanged();
        }
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        private final TextView topicTitle;
        private final TextView topicDescription;
        private final ImageView topicIcon;
        private final MaterialButton startButton;
        private final Chip durationChip;
        private final MaterialCardView cardView;

        TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            topicTitle = itemView.findViewById(R.id.topic_title);
            topicDescription = itemView.findViewById(R.id.topic_description);
            topicIcon = itemView.findViewById(R.id.topic_icon);
            startButton = itemView.findViewById(R.id.start_button);
            durationChip = itemView.findViewById(R.id.duration_chip);
            cardView = (MaterialCardView) itemView;
        }

        public void bind(Topic topic, Context context) {
            if (topic != null) {
                // Set basic info
                topicTitle.setText(topic.getName());
                topicDescription.setText(topic.getDescription());

                // Set estimated duration (mock data for now)
                durationChip.setText(45 + " min"); // Replace with actual duration calculation));

                // Set icon based on topic content type
                int iconResId = R.drawable.ic_empty_topic;
//                if (topic.getVideoID() != null && !topic.getVideoID().isEmpty()) {
//                    iconResId = R.drawable.ic_video_topic;
//                } else if (topic.getContent() != null && topic.getContent().contains("<code>")) {
//                    iconResId = R.drawable.ic_code_topic;
//                }

                // Load icon with Glide
                Glide.with(context)
                        .load(iconResId)
                        .into(topicIcon);

                // Check if topic is completed
                checkTopicCompletion(topic, context);

                // Set click listeners
                View.OnClickListener clickListener = v -> navigateToTopicView(topic, context);
                itemView.setOnClickListener(clickListener);
                startButton.setOnClickListener(clickListener);
            }
        }

        private void navigateToTopicView(Topic topic, Context context) {
            // Use the existing navigation pattern
            Intent intent = new Intent(context, TopicView.class);
            intent.putExtra("TOPIC_ID", topic.getId());

            // Add user progress tracking
            updateUserProgress(topic, context);

            // Start the activity
            context.startActivity(intent);
        }

        private void checkTopicCompletion(Topic topic, Context context) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser == null) return;

            String uid = currentUser.getUid();
            db.collection("UserProgress")
                    .document(uid)
                    .collection("courses")
                    .document(String.valueOf(topic.getCourseId()))
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            List<Long> completedTopics = (List<Long>) document.get("completedTopics");
                            if (completedTopics != null && completedTopics.contains((long) topic.getId())) {
                                // Topic is completed
                                startButton.setText("continue learning");
                                topicIcon.setAlpha(1.0f);
                                cardView.setStrokeColor(context.getResources().getColor(R.color.progress_color));
                            } else {
                                // Topic not completed
                                startButton.setText("Start Learning");
                                topicIcon.setAlpha(0.7f);
                                cardView.setStrokeColor(context.getResources().getColor(R.color.progress_color));
                            }
                        }
                    });
        }

        private void updateUserProgress(Topic topic, Context context) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser == null) return;

            String uid = currentUser.getUid();
            db.collection("UserProgress")
                    .document(uid)
                    .collection("courses")
                    .document(String.valueOf(topic.getCourseId()))
                    .update("completedTopics", com.google.firebase.firestore.FieldValue.arrayUnion(topic.getId()))
                    .addOnFailureListener(e -> {
                        // If document doesn't exist, create it
                        if (e.getMessage() != null && e.getMessage().contains("No document to update")) {
                            List<Integer> completedTopics = new ArrayList<>();
                            completedTopics.add(topic.getId());

                            db.collection("userProgress")
                                    .document(uid)
                                    .collection("courses")
                                    .document(String.valueOf(topic.getCourseId()))
                                    .set(new UserProgress(topic.getCourseId(), completedTopics));
                        }
                    });
        }
    }

    // Helper class for Firestore
    private static class UserProgress {
        private final int courseId;
        private final List<Integer> completedTopics;

        UserProgress(int courseId, List<Integer> completedTopics) {
            this.courseId = courseId;
            this.completedTopics = completedTopics;
        }

        public int getCourseId() { return courseId; }
        public List<Integer> getCompletedTopics() { return completedTopics; }
    }

    private static class TopicDiffCallback extends DiffUtil.Callback {
        private final List<Topic> oldTopics;
        private final List<Topic> newTopics;

        TopicDiffCallback(List<Topic> oldTopics, List<Topic> newTopics) {
            this.oldTopics = oldTopics;
            this.newTopics = newTopics;
        }

        @Override
        public int getOldListSize() {
            return oldTopics.size();
        }

        @Override
        public int getNewListSize() {
            return newTopics.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPosition, int newPosition) {
            return oldTopics.get(oldPosition).getId() == newTopics.get(newPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldPosition, int newPosition) {
            Topic oldTopic = oldTopics.get(oldPosition);
            Topic newTopic = newTopics.get(newPosition);
            return oldTopic.equals(newTopic);
        }
    }
}
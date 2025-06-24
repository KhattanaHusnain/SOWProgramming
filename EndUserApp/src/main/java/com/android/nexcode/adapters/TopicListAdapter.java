package com.android.nexcode.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;
import com.android.nexcode.models.Topic;
import com.android.nexcode.presenters.activities.OfflineTopicViewActivity;
import com.android.nexcode.presenters.activities.TopicView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import java.util.ArrayList;
import java.util.List;

public class TopicListAdapter extends RecyclerView.Adapter<TopicListAdapter.TopicViewHolder> {
    private final Context context;
    private final List<Topic> topics;
    String mode;

    public TopicListAdapter(Context context, List<Topic> topics, String mode) {
        this.context = context;
        this.topics = new ArrayList<>(topics);
        this.mode = mode;
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
        holder.bind(topic, context, mode);
    }

    @Override
    public int getItemCount() {
        return topics != null ? topics.size() : 0;
    }

    public void updateTopics(List<Topic> newTopics) {
            this.topics.clear();
            this.topics.addAll(newTopics);
            notifyDataSetChanged();
    }

    static class TopicViewHolder extends RecyclerView.ViewHolder {
        private final TextView topicTitle;
        private final TextView topicDescription;
        private final TextView topicOrder;
        private final TextView topicCreatedDate;
        private final MaterialCardView cardView;

        TopicViewHolder(@NonNull View itemView) {
            super(itemView);
            topicTitle = itemView.findViewById(R.id.topic_title);
            topicDescription = itemView.findViewById(R.id.topic_description);
            topicOrder = itemView.findViewById(R.id.topic_order);
            topicCreatedDate = itemView.findViewById(R.id.topic_created_date);
            cardView = (MaterialCardView) itemView;
        }

        public void bind(Topic topic, Context context, String mode) {
            if (topic != null) {
                // Set basic info using all available attributes
                topicTitle.setText(topic.getName());
                topicDescription.setText(topic.getDescription());

                // Display order index
                topicOrder.setText("Lesson " + topic.getOrderIndex());

                // Format and display creation date
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDate = dateFormat.format(topic.getCreatedAt());
                topicCreatedDate.setText("Created: " + formattedDate);


                if(mode.equals("ONLINE")) {
                    itemView.setOnClickListener(v -> navigateToTopicView(topic, context));
                } else {
                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(context, OfflineTopicViewActivity.class);
                            intent.putExtra("TOPIC_ID", topic.getOrderIndex());
                            intent.putExtra("TOPIC_NAME", topic.getName());
                            intent.putExtra("TOPIC_CONTENT", topic.getContent());
                            intent.putExtra("VIDEO_ID", topic.getVideoID());
                            intent.putExtra("COURSE_ID", topic.getCourseId());
                            context.startActivity(intent);
                        }
                    });
                }
            }
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
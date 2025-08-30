package com.sowp.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TopicsAdapter extends RecyclerView.Adapter<TopicsAdapter.TopicViewHolder> {

    private List<Topic> topics;
    private Context context;
    private OnTopicClickListener onTopicClickListener;
    private SimpleDateFormat dateFormat;

    public interface OnTopicClickListener {
        void onTopicClick(Topic topic, int position);
    }

    public TopicsAdapter(Context context, OnTopicClickListener listener) {
        this.context = context;
        this.topics = new ArrayList<>();
        this.onTopicClickListener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
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
        holder.bind(topic, position);
    }

    @Override
    public int getItemCount() {
        return topics.size();
    }

    public void setTopics(List<Topic> topics) {
        this.topics.clear();
        if (topics != null) {
            this.topics.addAll(topics);
        }
        notifyDataSetChanged();
    }

    public void addTopics(List<Topic> newTopics) {
        if (newTopics != null) {
            int startPosition = this.topics.size();
            this.topics.addAll(newTopics);
            notifyItemRangeInserted(startPosition, newTopics.size());
        }
    }

    public void clearTopics() {
        this.topics.clear();
        notifyDataSetChanged();
    }

    public class TopicViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTopicOrder;
        private TextView tvTopicName;
        private TextView tvTopicDescription;
        private TextView tvViews;
        private TextView tvCreatedDate;
        private TextView tvTags;
        private View statusIndicator;
        private LinearLayout videoIndicator;
        private LinearLayout tagsContainer;

        public TopicViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTopicOrder = itemView.findViewById(R.id.tvTopicOrder);
            tvTopicName = itemView.findViewById(R.id.tvTopicName);
            tvTopicDescription = itemView.findViewById(R.id.tvTopicDescription);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            tvTags = itemView.findViewById(R.id.tvTags);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            videoIndicator = itemView.findViewById(R.id.videoIndicator);
            tagsContainer = itemView.findViewById(R.id.tagsContainer);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onTopicClickListener != null) {
                    onTopicClickListener.onTopicClick(topics.get(position), position);
                }
            });
        }

        public void bind(Topic topic, int position) {
            // Set topic order
            tvTopicOrder.setText(String.valueOf(topic.getOrderIndex() > 0 ? topic.getOrderIndex() : position + 1));

            // Set topic name
            tvTopicName.setText(topic.getName() != null ? topic.getName() : "Untitled Topic");

            // Set description
            if (topic.getDescription() != null && !topic.getDescription().trim().isEmpty()) {
                tvTopicDescription.setText(topic.getDescription());
                tvTopicDescription.setVisibility(View.VISIBLE);
            } else {
                tvTopicDescription.setVisibility(View.GONE);
            }

            // Set views count
            tvViews.setText(String.valueOf(topic.getViews()));

            // Set created date
            if (topic.getCreatedAt() > 0) {
                Date createdDate = new Date(topic.getCreatedAt());
                tvCreatedDate.setText(dateFormat.format(createdDate));
            } else {
                tvCreatedDate.setText("No date");
            }

            // Set status indicator color
            if (topic.isPublic()) {
                statusIndicator.setBackgroundResource(R.drawable.status_public);
            } else {
                statusIndicator.setBackgroundResource(R.drawable.status_private);
            }

            // Show video indicator if video exists
            if (topic.getVideoID() != null && !topic.getVideoID().trim().isEmpty()) {
                videoIndicator.setVisibility(View.VISIBLE);
            } else {
                videoIndicator.setVisibility(View.GONE);
            }

            // Handle tags
            if (topic.getTags() != null && !topic.getTags().trim().isEmpty()) {
                String formattedTags = formatTags(topic.getTags());
                tvTags.setText(formattedTags);
                tagsContainer.setVisibility(View.VISIBLE);
            } else {
                tagsContainer.setVisibility(View.GONE);
            }
        }

        private String formatTags(String tags) {
            if (tags == null || tags.trim().isEmpty()) {
                return "";
            }

            // Split tags by comma and format them
            String[] tagArray = tags.split(",");
            StringBuilder formattedTags = new StringBuilder();

            for (int i = 0; i < tagArray.length && i < 3; i++) { // Limit to 3 tags for UI
                String tag = tagArray[i].trim();
                if (!tag.isEmpty()) {
                    if (formattedTags.length() > 0) {
                        formattedTags.append(" • ");
                    }
                    formattedTags.append(tag);
                }
            }

            if (tagArray.length > 3) {
                formattedTags.append(" • +").append(tagArray.length - 3);
            }

            return formattedTags.toString();
        }
    }
}
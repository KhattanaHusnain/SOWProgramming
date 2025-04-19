package com.android.nexcode.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class LearningPathsAdapter extends RecyclerView.Adapter<LearningPathsAdapter.PathViewHolder> {

    private final Context context;
    private List<LearningPath> paths;
    private OnPathClickListener listener;

    public LearningPathsAdapter(Context context, List<LearningPath> paths) {
        this.context = context;
        this.paths = paths;
    }

    public void setOnPathClickListener(OnPathClickListener listener) {
        this.listener = listener;
    }

    public void updatePaths(List<LearningPath> newPaths) {
        this.paths = newPaths;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.learning_path_item, parent, false);
        return new PathViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PathViewHolder holder, int position) {
        LearningPath path = paths.get(position);

        holder.pathTitle.setText(path.getTitle());
        holder.pathDescription.setText(path.getDescription());
        holder.courseCount.setText(path.getCourseCount() + " Courses");
        holder.duration.setText(path.getTotalHours() + " Hours");

        // Set image if available
        if (path.getImageResourceId() != 0) {
            holder.pathImage.setImageResource(path.getImageResourceId());
        }

        // Set different background colors for cards to make them visually distinct
        int[] colorResources = {
                R.color.primary_color_light,
                R.color.accent_color,
                R.color.accent,
                R.color.badge_gold
        };

        int colorIndex = position % colorResources.length;
        holder.pathCard.setCardBackgroundColor(
                context.getResources().getColor(colorResources[colorIndex]));

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPathClick(path);
            }
        });
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    static class PathViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView pathCard;
        ImageView pathImage;
        TextView pathTitle;
        TextView pathDescription;
        TextView courseCount;
        TextView duration;

        public PathViewHolder(@NonNull View itemView) {
            super(itemView);
            pathCard = (MaterialCardView) itemView;
            pathImage = itemView.findViewById(R.id.path_thumbnail);
            pathTitle = itemView.findViewById(R.id.path_title);
            pathDescription = itemView.findViewById(R.id.path_description);
            courseCount = itemView.findViewById(R.id.courses_count);
            duration = itemView.findViewById(R.id.path_duration);
        }
    }

    public interface OnPathClickListener {
        void onPathClick(LearningPath path);
    }
}
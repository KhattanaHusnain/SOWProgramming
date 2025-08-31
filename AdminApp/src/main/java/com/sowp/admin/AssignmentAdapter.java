package com.sowp.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

    private Context context;
    private List<Assignment> assignments;
    private OnAssignmentClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnAssignmentClickListener {
        void onAssignmentClick(Assignment assignment);
    }

    public AssignmentAdapter(Context context, List<Assignment> assignments, OnAssignmentClickListener listener) {
        this.context = context;
        // Create defensive copy to prevent concurrent modification
        this.assignments = new ArrayList<>(assignments);
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        if (position >= 0 && position < assignments.size()) {
            Assignment assignment = assignments.get(position);
            holder.bind(assignment, position);
        }
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    /**
     * Thread-safe method to update assignments list
     * Creates defensive copies to prevent ConcurrentModificationException
     */
    public void updateAssignments(List<Assignment> newAssignments) {
        if (newAssignments == null) {
            this.assignments.clear();
        } else {
            // Create a defensive copy to avoid ConcurrentModificationException
            List<Assignment> safeCopy = new ArrayList<>(newAssignments);
            this.assignments.clear();
            this.assignments.addAll(safeCopy);
        }
        notifyDataSetChanged();
    }

    public class AssignmentViewHolder extends RecyclerView.ViewHolder {

        private TextView tvAssignmentNumber, tvAssignmentTitle, tvAssignmentDescription;
        private TextView tvTotalScore, tvPassingScore, tvSemester, tvCreatedDate, tvImageCount;
        private ChipGroup chipGroupTags, chipGroupCategories;
        private ImageView ivHasImages;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAssignmentNumber = itemView.findViewById(R.id.tvAssignmentNumber);
            tvAssignmentTitle = itemView.findViewById(R.id.tvAssignmentTitle);
            tvAssignmentDescription = itemView.findViewById(R.id.tvAssignmentDescription);
            tvTotalScore = itemView.findViewById(R.id.tvTotalScore);
            tvPassingScore = itemView.findViewById(R.id.tvPassingScore);
            tvSemester = itemView.findViewById(R.id.tvSemester);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            tvImageCount = itemView.findViewById(R.id.tvImageCount);
            chipGroupTags = itemView.findViewById(R.id.chipGroupTags);
            chipGroupCategories = itemView.findViewById(R.id.chipGroupCategories);
            ivHasImages = itemView.findViewById(R.id.ivHasImages);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < assignments.size() && listener != null) {
                    listener.onAssignmentClick(assignments.get(position));
                }
            });
        }

        public void bind(Assignment assignment, int position) {
            if (assignment == null) return;

            // Assignment number/ID
            tvAssignmentNumber.setText(String.valueOf(assignment.getId()));

            // Safe string handling with null checks
            tvAssignmentTitle.setText(assignment.getTitle() != null ? assignment.getTitle() : "Untitled Assignment");

            // Truncate description if too long
            String description = assignment.getDescription() != null ? assignment.getDescription() : "No description";
            if (description.length() > 100) {
                description = description.substring(0, 100) + "...";
            }
            tvAssignmentDescription.setText(description);

            // Scores
            tvTotalScore.setText(String.format(Locale.getDefault(), "%.1f", assignment.getScore()));
            tvPassingScore.setText(String.format(Locale.getDefault(), "%.1f", assignment.getPassingScore()));

            // Semester
            tvSemester.setText(assignment.getSemester() != null ? assignment.getSemester() : "N/A");

            // Format date with null safety
            if (assignment.getCreatedAt() > 0) {
                try {
                    Date date = new Date(assignment.getCreatedAt());
                    tvCreatedDate.setText(dateFormat.format(date));
                } catch (Exception e) {
                    tvCreatedDate.setText("N/A");
                }
            } else {
                tvCreatedDate.setText("N/A");
            }

            // Handle images
            if (assignment.getBase64Images() != null && !assignment.getBase64Images().isEmpty()) {
                ivHasImages.setVisibility(View.VISIBLE);
                int imageCount = assignment.getBase64Images().size();
                tvImageCount.setText(imageCount + " image" + (imageCount > 1 ? "s" : ""));
                tvImageCount.setVisibility(View.VISIBLE);
            } else {
                ivHasImages.setVisibility(View.GONE);
                tvImageCount.setVisibility(View.GONE);
            }

            // Handle tags
            chipGroupTags.removeAllViews();
            if (assignment.getTags() != null && !assignment.getTags().isEmpty()) {
                for (String tag : assignment.getTags()) {
                    if (tag != null && !tag.trim().isEmpty()) {
                        Chip chip = new Chip(context);
                        chip.setText(tag);
                        chip.setTextSize(10);
                        chip.setChipBackgroundColorResource(R.color.chip_tag_background);
                        chip.setTextColor(context.getResources().getColor(R.color.chip_tag_text_color));
                        chip.setClickable(false);
                        chip.setFocusable(false);
                        chipGroupTags.addView(chip);
                    }
                }
            }

            // Handle categories
            chipGroupCategories.removeAllViews();
            if (assignment.getCategories() != null && !assignment.getCategories().isEmpty()) {
                for (String category : assignment.getCategories()) {
                    if (category != null && !category.trim().isEmpty()) {
                        Chip chip = new Chip(context);
                        chip.setText(category);
                        chip.setTextSize(10);
                        chip.setChipBackgroundColorResource(R.color.chip_category_background);
                        chip.setTextColor(context.getResources().getColor(R.color.chip_category_text_color));
                        chip.setClickable(false);
                        chip.setFocusable(false);
                        chipGroupCategories.addView(chip);
                    }
                }
            }
        }
    }
}
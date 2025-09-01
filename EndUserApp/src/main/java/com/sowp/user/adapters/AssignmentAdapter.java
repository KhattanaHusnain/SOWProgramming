package com.sowp.user.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.sowp.user.R;
import com.sowp.user.models.Assignment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

    private Context context;
    private List<Assignment> assignmentList;
    private OnAssignmentClickListener listener;

    public interface OnAssignmentClickListener {
        void onAssignmentClick(Assignment assignment);
    }

    public AssignmentAdapter(Context context, List<Assignment> assignmentList, OnAssignmentClickListener listener) {
        this.context = context;
        this.assignmentList = assignmentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_assignment, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        Assignment assignment = assignmentList.get(position);
        holder.bind(assignment, listener);
    }

    @Override
    public int getItemCount() {
        return assignmentList != null ? assignmentList.size() : 0;
    }

    public void updateData(List<Assignment> newAssignmentList) {
        this.assignmentList.clear();
        this.assignmentList.addAll(newAssignmentList);
        notifyDataSetChanged();
    }

    public static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAssignmentNumber;
        private final TextView tvAssignmentTitle;
        private final TextView tvSemester;
        private final TextView tvAssignmentDescription;
        private final TextView tvTotalScore;
        private final TextView tvPassingScore;
        private final TextView tvCreatedDate;
        private final ImageView ivHasImages;
        private final TextView tvImageCount;
        private final CardView assignmentCardView;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAssignmentNumber = itemView.findViewById(R.id.tvAssignmentNumber);
            tvAssignmentTitle = itemView.findViewById(R.id.tvAssignmentTitle);
            tvSemester = itemView.findViewById(R.id.tvSemester);
            tvAssignmentDescription = itemView.findViewById(R.id.tvAssignmentDescription);
            tvTotalScore = itemView.findViewById(R.id.tvTotalScore);
            tvPassingScore = itemView.findViewById(R.id.tvPassingScore);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            ivHasImages = itemView.findViewById(R.id.ivHasImages);
            tvImageCount = itemView.findViewById(R.id.tvImageCount);
            assignmentCardView = itemView.findViewById(R.id.assignmentCardView);
        }

        void bind(Assignment assignment, OnAssignmentClickListener listener) {
            if (assignment == null) return;

            // Populate assignment data
            tvAssignmentNumber.setText(String.valueOf(assignment.getOrderIndex()));
            tvAssignmentTitle.setText(assignment.getTitle() != null ? assignment.getTitle() : "Untitled Assignment");
            tvSemester.setText(assignment.getSemester() != null ? assignment.getSemester() : "N/A");
            tvAssignmentDescription.setText(assignment.getDescription() != null ? assignment.getDescription() : "No description available");
            tvTotalScore.setText(String.valueOf(assignment.getScore()));
            tvPassingScore.setText(String.valueOf(assignment.getPassingScore()));

            // Set created date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvCreatedDate.setText(dateFormat.format(new Date(assignment.getCreatedAt())));

            // Handle images
            if (assignment.getBase64Images() != null && !assignment.getBase64Images().isEmpty()) {
                ivHasImages.setVisibility(View.VISIBLE);
                tvImageCount.setVisibility(View.VISIBLE);
                tvImageCount.setText(assignment.getBase64Images().size() + " images");
            } else {
                ivHasImages.setVisibility(View.GONE);
                tvImageCount.setVisibility(View.GONE);
            }

            // Set click listener
            assignmentCardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAssignmentClick(assignment);
                }
            });
        }
    }
}
package com.android.nexcode.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;
import com.android.nexcode.models.AssignmentAttempt;
import com.android.nexcode.presenters.activities.AssignmentDetailActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class AssignmentHistoryAdapter extends RecyclerView.Adapter<AssignmentHistoryAdapter.AssignmentViewHolder> {

    private Context context;
    private List<AssignmentAttempt> attempts;

    public AssignmentHistoryAdapter(Context context, List<AssignmentAttempt> attempts) {
        this.context = context;
        this.attempts = attempts != null ? attempts : new ArrayList<>();
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_assignment_history, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        AssignmentAttempt attempt = attempts.get(position);
        holder.bind(attempt);
    }

    @Override
    public int getItemCount() {
        return attempts.size();
    }

    public void updateAttempts(List<AssignmentAttempt> newAttempts) {
        this.attempts.clear();
        if (newAttempts != null) {
            this.attempts.addAll(newAttempts);
        }
        notifyDataSetChanged();
    }

    public void addAttempts(List<AssignmentAttempt> newAttempts) {
        if (newAttempts != null && !newAttempts.isEmpty()) {
            int startPosition = this.attempts.size();
            this.attempts.addAll(newAttempts);
            notifyItemRangeInserted(startPosition, newAttempts.size());
        }
    }

    public void clearAttempts() {
        int size = this.attempts.size();
        this.attempts.clear();
        notifyItemRangeRemoved(0, size);
    }

    public class AssignmentViewHolder extends RecyclerView.ViewHolder {

        // UI Components
        private TextView tvAssignmentId;
        private TextView tvScore;
        private TextView tvPercentage;
        private TextView tvImageCount;
        private TextView tvCheckedStatus;
        private TextView tvSubmissionTime;
        private Chip chipStatus;
        private ImageView ivCheckedIcon;
        private MaterialButton btnViewDetails;
        private LinearLayout imageCountLayout;
        private LinearLayout checkedStatusLayout;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews();
        }

        private void initViews() {
            tvAssignmentId = itemView.findViewById(R.id.tvAssignmentId);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
            tvImageCount = itemView.findViewById(R.id.tvImageCount);
            tvCheckedStatus = itemView.findViewById(R.id.tvCheckedStatus);
            tvSubmissionTime = itemView.findViewById(R.id.tvSubmissionTime);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            ivCheckedIcon = itemView.findViewById(R.id.ivCheckedIcon);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            imageCountLayout = itemView.findViewById(R.id.imageCountLayout);
            checkedStatusLayout = itemView.findViewById(R.id.checkedStatusLayout);
        }

        public void bind(AssignmentAttempt attempt) {
            if (attempt == null) return;

            // Set assignment ID with formatting
            setAssignmentId(attempt.getAssignmentId());

            // Set score and percentage
            setScoreAndPercentage(attempt);

            // Set submission time
            setSubmissionTime(attempt);

            // Set image count
            setImageCount(attempt);

            // Set status chip
            setStatusChip(attempt.getStatus());

            // Set checked status
            setCheckedStatus(attempt.isChecked());

            // Set click listener for view details
            setViewDetailsClickListener(attempt);
        }

        private void setAssignmentId(String assignmentId) {
            if (tvAssignmentId != null) {
                String formattedId = formatAssignmentId(assignmentId);
                tvAssignmentId.setText(formattedId);
            }
        }

        private void setScoreAndPercentage(AssignmentAttempt attempt) {
            if (tvScore != null && tvPercentage != null) {
                // Set score text
                String scoreText = attempt.getScore() + " / " + attempt.getMaxScore();
                tvScore.setText(scoreText);

                // Set percentage text
                tvPercentage.setText(attempt.getFormattedPercentage());

                // Set score color based on percentage
                setScoreColor(attempt.getPercentageScore());
            }
        }

        private void setSubmissionTime(AssignmentAttempt attempt) {
            if (tvSubmissionTime != null) {
                String timeText = "Submitted on " + attempt.getFormattedSubmissionDate();
                tvSubmissionTime.setText(timeText);
            }
        }

        private void setImageCount(AssignmentAttempt attempt) {
            int imageCount = attempt.getSubmittedImagesCount();

            if (tvImageCount != null) {
                if (imageCount > 0) {
                    String imageText = imageCount + " image" + (imageCount > 1 ? "s" : "");
                    tvImageCount.setText(imageText);
                    showImageCountLayout(true);
                } else {
                    tvImageCount.setText("No images");
                    showImageCountLayout(true);
                }
            }
        }

        private void showImageCountLayout(boolean show) {
            if (imageCountLayout != null) {
                imageCountLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (tvImageCount != null) {
                tvImageCount.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }

        private void setStatusChip(String status) {
            if (chipStatus == null || status == null) return;

            chipStatus.setText(status);

            StatusColors colors = getStatusColors(status.toLowerCase());
            chipStatus.setChipBackgroundColorResource(colors.backgroundColorRes);
            chipStatus.setTextColor(ContextCompat.getColor(context, colors.textColorRes));
        }

        private void setCheckedStatus(boolean isChecked) {
            if (tvCheckedStatus != null && ivCheckedIcon != null) {
                if (isChecked) {
                    tvCheckedStatus.setText("Checked");
                    tvCheckedStatus.setTextColor(ContextCompat.getColor(context, R.color.success_color));
                    ivCheckedIcon.setImageResource(R.drawable.ic_checked);
                    ivCheckedIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_color));
                } else {
                    tvCheckedStatus.setText("Not Checked");
                    tvCheckedStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_color));
                    ivCheckedIcon.setImageResource(R.drawable.ic_pending);
                    ivCheckedIcon.setColorFilter(ContextCompat.getColor(context, R.color.warning_color));
                }

                showCheckedStatusLayout(true);
            }
        }

        private void showCheckedStatusLayout(boolean show) {
            if (checkedStatusLayout != null) {
                checkedStatusLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }

        private void setViewDetailsClickListener(AssignmentAttempt attempt) {
            if (btnViewDetails != null) {
                btnViewDetails.setOnClickListener(v -> {
                    Intent intent = new Intent(context, AssignmentDetailActivity.class);
                    intent.putExtra("attemptId", attempt.getAttemptId());
                    intent.putExtra("assignmentId", attempt.getAssignmentId());
                    context.startActivity(intent);
                });
            }
        }

        private String formatAssignmentId(String assignmentId) {
            if (assignmentId == null || assignmentId.isEmpty()) {
                return "Unknown Assignment";
            }

            // Convert "assignment1" to "Assignment 1"
            if (assignmentId.toLowerCase().startsWith("assignment")) {
                try {
                    String number = assignmentId.substring(10); // Remove "assignment"
                    return "Assignment " + number;
                } catch (StringIndexOutOfBoundsException e) {
                    return assignmentId;
                }
            }

            // Handle other formats - capitalize first letter
            return assignmentId.substring(0, 1).toUpperCase() + assignmentId.substring(1);
        }

        private StatusColors getStatusColors(String status) {
            switch (status) {
                case "submitted":
                    return new StatusColors(R.color.status_submitted_bg, R.color.status_submitted_text);
                case "graded":
                    return new StatusColors(R.color.status_graded_bg, R.color.status_graded_text);
                case "pending":
                    return new StatusColors(R.color.status_pending_bg, R.color.status_pending_text);
                case "late":
                    return new StatusColors(R.color.status_late_bg, R.color.status_late_text);
                default:
                    return new StatusColors(R.color.status_default_bg, R.color.status_default_text);
            }
        }

        private void setScoreColor(double percentage) {
            if (tvScore == null) return;

            int colorRes;
            if (percentage >= 80) {
                colorRes = R.color.success_color;
            } else if (percentage >= 60) {
                colorRes = R.color.warning_color;
            } else {
                colorRes = R.color.error_color;
            }

            tvScore.setTextColor(ContextCompat.getColor(context, colorRes));
        }
    }

    // Helper class for status colors
    private static class StatusColors {
        final int backgroundColorRes;
        final int textColorRes;

        StatusColors(int backgroundColorRes, int textColorRes) {
            this.backgroundColorRes = backgroundColorRes;
            this.textColorRes = textColorRes;
        }
    }
}
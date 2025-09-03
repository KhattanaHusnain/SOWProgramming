package com.sowp.user.adapters;

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

import com.sowp.user.R;
import com.sowp.user.models.AssignmentAttempt;
import com.sowp.user.presenters.activities.AssignmentDetailActivity;
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
        private TextView tvAssignmentTitle;
        private TextView tvAssignmentId;
        private TextView tvScore;
        private TextView tvPercentage;
        private TextView tvImageCount;
        private TextView tvCheckedStatus;
        private TextView tvSubmissionTime;
        private TextView tvFeedback;
        private Chip chipStatus;
        private ImageView ivCheckedIcon;
        private MaterialButton btnViewDetails;
        private LinearLayout imageCountLayout;
        private LinearLayout checkedStatusLayout;
        private LinearLayout feedbackLayout;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            initViews();
        }

        private void initViews() {
            tvAssignmentTitle = itemView.findViewById(R.id.tvAssignmentTitle);
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

            // Set assignment title and ID
            setAssignmentInfo(attempt);

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

            // Set feedback
            setFeedback(attempt);

            // Set click listener for view details
            setViewDetailsClickListener(attempt);
        }

        private void setAssignmentInfo(AssignmentAttempt attempt) {
            // Set assignment title if available
            if (tvAssignmentTitle != null) {
                String title = attempt.getAssignmentTitle();
                if (title != null && !title.trim().isEmpty()) {
                    tvAssignmentTitle.setText(title);
                    tvAssignmentTitle.setVisibility(View.VISIBLE);
                } else {
                    tvAssignmentTitle.setVisibility(View.GONE);
                }
            }

            // Set assignment ID
            if (tvAssignmentId != null) {
                String formattedId = formatAssignmentId(attempt.getAssignmentId());
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
                    tvCheckedStatus.setText("Graded");
                    tvCheckedStatus.setTextColor(ContextCompat.getColor(context, R.color.success));
                    ivCheckedIcon.setImageResource(R.drawable.ic_checked);
                    ivCheckedIcon.setColorFilter(ContextCompat.getColor(context, R.color.success));
                } else {
                    tvCheckedStatus.setText("Pending Grade");
                    tvCheckedStatus.setTextColor(ContextCompat.getColor(context, R.color.warning));
                    ivCheckedIcon.setImageResource(R.drawable.ic_pending);
                    ivCheckedIcon.setColorFilter(ContextCompat.getColor(context, R.color.warning));
                }

                showCheckedStatusLayout(true);
            }
        }

        private void setFeedback(AssignmentAttempt attempt) {
            if (tvFeedback != null && feedbackLayout != null) {
                if (attempt.hasFeedback()) {
                    tvFeedback.setText(attempt.getFeedback());
                    feedbackLayout.setVisibility(View.VISIBLE);
                } else {
                    feedbackLayout.setVisibility(View.GONE);
                }
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
                    intent.putExtra("courseId", attempt.getCourseId());
                    context.startActivity(intent);
                });
            }

            // Also make the entire item clickable
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, AssignmentDetailActivity.class);
                intent.putExtra("attemptId", attempt.getAttemptId());
                intent.putExtra("assignmentId", attempt.getAssignmentId());
                intent.putExtra("courseId", attempt.getCourseId());
                context.startActivity(intent);
            });
        }

        private String formatAssignmentId(int assignmentId) {
            return "Assignment #" + assignmentId;
        }

        private StatusColors getStatusColors(String status) {
            switch (status) {
                case "submitted":
                    return new StatusColors(R.color.selected, R.color.status_submitted);
                case "graded":
                    return new StatusColors(R.color.selected, R.color.success);
                case "failed":
                    return new StatusColors(R.color.background_secondary, R.color.error);
                case "pending":
                    return new StatusColors(R.color.background_secondary, R.color.warning);
                case "in_progress":
                    return new StatusColors(R.color.background_secondary, R.color.status_in_progress);
                case "overdue":
                    return new StatusColors(R.color.background_secondary, R.color.status_overdue);
                case "not_started":
                    return new StatusColors(R.color.gray_light, R.color.status_not_started);
                default:
                    return new StatusColors(R.color.gray_light, R.color.text_secondary);
            }
        }

        private void setScoreColor(double percentage) {
            if (tvScore == null || tvPercentage == null) return;

            int colorRes;
            if (percentage >= 80) {
                colorRes = R.color.success;
            } else if (percentage >= 60) {
                colorRes = R.color.warning;
            } else {
                colorRes = R.color.error;
            }

            tvScore.setTextColor(ContextCompat.getColor(context, colorRes));
            tvPercentage.setTextColor(ContextCompat.getColor(context, colorRes));
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
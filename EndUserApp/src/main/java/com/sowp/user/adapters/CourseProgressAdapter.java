package com.sowp.user.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.models.CourseProgress;
import com.sowp.user.repositories.CourseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseProgressAdapter extends RecyclerView.Adapter<CourseProgressAdapter.CourseProgressViewHolder> {
    private List<CourseProgress> progressList;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private Map<Integer, Integer> courseLecturesCache; // Cache for course lectures
    private CourseRepository courseRepository;

    public interface OnItemClickListener {
        void onViewDetailsClick(CourseProgress courseProgress);
        void onItemClick(CourseProgress courseProgress);
    }

    public CourseProgressAdapter(Context context) {
        this.context = context;
        this.progressList = new ArrayList<>();
        this.courseLecturesCache = new HashMap<>();
        this.courseRepository = new CourseRepository(context);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setProgressList(List<CourseProgress> progressList) {
        // Create a defensive copy to avoid concurrent modification
        this.progressList = progressList != null ? new ArrayList<>(progressList) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addProgressList(List<CourseProgress> newProgressList) {
        if (newProgressList != null && !newProgressList.isEmpty()) {
            int startPosition = this.progressList.size();
            // Create a defensive copy before adding
            List<CourseProgress> newItems = new ArrayList<>(newProgressList);
            this.progressList.addAll(newItems);
            notifyItemRangeInserted(startPosition, newItems.size());
        }
    }

    public void clearList() {
        int itemCount = this.progressList.size();
        this.progressList = new ArrayList<>(); // Create new list instead of clearing
        notifyItemRangeRemoved(0, itemCount);
    }

    @NonNull
    @Override
    public CourseProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course_progress, parent, false);
        return new CourseProgressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseProgressViewHolder holder, int position) {
        CourseProgress progress = progressList.get(position);
        holder.bind(progress);
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }

    public class CourseProgressViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCourseName;
        private TextView tvStatus;
        private ProgressBar progressBar;
        private TextView tvProgressPercentage;
        private RatingBar ratingBar;
        private TextView tvRatingValue;
        private TextView tvEnrollmentDate;
        private TextView tvCompletionDate;
        private TextView tvUnenrollmentDate;
        private View layoutRating;

        public CourseProgressViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvProgressPercentage = itemView.findViewById(R.id.tvProgressPercentage);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvRatingValue = itemView.findViewById(R.id.tvRatingValue);
            tvEnrollmentDate = itemView.findViewById(R.id.tvEnrollmentDate);
            tvCompletionDate = itemView.findViewById(R.id.tvCompletionDate);
            tvUnenrollmentDate = itemView.findViewById(R.id.tvUnenrollmentDate);
            layoutRating = itemView.findViewById(R.id.layoutRating);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(progressList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(CourseProgress progress) {
            // Set course name
            tvCourseName.setText(progress.getCourseName() != null ?
                    progress.getCourseName() : "Course #" + progress.getCourseId());

            // Set status and status styling
            String status = progress.getStatusString();
            tvStatus.setText(status);
            setStatusStyling(tvStatus, status);

            // Load course lectures and calculate progress
            loadCourseProgressData(progress);

            // Set rating
            if (progress.getUserRating() > 0) {
                layoutRating.setVisibility(View.VISIBLE);
                ratingBar.setRating(progress.getUserRating());
                tvRatingValue.setText(String.format(Locale.getDefault(), "%.1f", progress.getUserRating()));
            } else {
                layoutRating.setVisibility(View.GONE);
            }

            // Set enrollment date
            if (progress.getEnrolledAt() != null && progress.getEnrolledAt() > 0) {
                String enrollmentDate = formatDate(progress.getEnrolledAt());
                tvEnrollmentDate.setText("Enrolled: " + enrollmentDate);
                tvEnrollmentDate.setVisibility(View.VISIBLE);
            } else {
                tvEnrollmentDate.setVisibility(View.GONE);
            }

            // Set completion date
            if (progress.isCompleted() && progress.getCompletedAt() != null && progress.getCompletedAt() > 0) {
                String completionDate = formatDate(progress.getCompletedAt());
                tvCompletionDate.setText("Completed: " + completionDate);
                tvCompletionDate.setVisibility(View.VISIBLE);
            } else {
                tvCompletionDate.setVisibility(View.GONE);
            }

            // Set unenrollment date
            if (progress.isUnenrolled()) {
                String unenrollmentDate = formatDate(progress.getUnenrolledAt());
                tvUnenrollmentDate.setText("Unenrolled: " + unenrollmentDate);
                tvUnenrollmentDate.setVisibility(View.VISIBLE);
            } else {
                tvUnenrollmentDate.setVisibility(View.GONE);
            }
        }

        private void loadCourseProgressData(CourseProgress progress) {
            int courseId = progress.getCourseId();

            // Check cache first
            if (courseLecturesCache.containsKey(courseId)) {
                int totalLectures = courseLecturesCache.get(courseId);
                updateProgressUI(progress, totalLectures);
            } else {
                // Load from repository
                courseRepository.getCourse(courseId, new CourseRepository.Callback() {
                    @Override
                    public void onSuccess(List<com.sowp.user.models.Course> courses) {
                        if (!courses.isEmpty()) {
                            int totalLectures = courses.get(0).getLectures();
                            courseLecturesCache.put(courseId, totalLectures);
                            updateProgressUI(progress, totalLectures);
                        } else {
                            // Fallback if course not found
                            updateProgressUI(progress, 10);
                        }
                    }

                    @Override
                    public void onFailure(String message) {
                        // Fallback on error
                        updateProgressUI(progress, 10);
                    }
                });
            }
        }

        private void updateProgressUI(CourseProgress progress, int totalLectures) {
            int viewedTopicsCount = progress.getViewedTopics() != null ? progress.getViewedTopics().size() : 0;
            float progressPercentage = progress.getProgressPercentage(totalLectures);

            progressBar.setProgress((int) progressPercentage);
            tvProgressPercentage.setText(String.format(Locale.getDefault(), "%.0f%% (%d/%d)",
                    progressPercentage, viewedTopicsCount, totalLectures));
        }

        private void setStatusStyling(TextView statusView, String status) {
            int backgroundRes;
            int textColor = ContextCompat.getColor(context, android.R.color.white);

            switch (status) {
                case "Completed":
                    backgroundRes = R.drawable.status_badge_completed;
                    break;
                case "Enrolled":
                    backgroundRes = R.drawable.status_badge_enrolled;
                    break;
                case "Unenrolled":
                    backgroundRes = R.drawable.status_badge_unenrolled;
                    break;
                default:
                    backgroundRes = R.drawable.status_badge_default;
                    break;
            }

            statusView.setBackground(ContextCompat.getDrawable(context, backgroundRes));
            statusView.setTextColor(textColor);
        }

        private String formatDate(Long timestamp) {
            if (timestamp == null || timestamp <= 0) {
                return "Unknown";
            }
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
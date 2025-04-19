package com.android.nexcode.course;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;
import com.android.nexcode.databinding.CourseItemBinding;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private final Context context;
    private List<Course> courses;

    public CourseAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CourseItemBinding binding = CourseItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course, context);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public void updateData(List<Course> newCourses) {
        this.courses.clear();
        this.courses.addAll(newCourses);
        notifyDataSetChanged();
    }

    public List<Course> getCourses() {
        return courses;
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        private static final String LEARNING_FORMAT = "%d Learning";
        private static final String COMPLETE_FORMAT = "%d%% complete";
        private static final String TIME_REMAINING_FORMAT = "%dh %dm remaining";
        private final CourseItemBinding binding;

        CourseViewHolder(@NonNull CourseItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Course course, Context context) {
            if (course != null) {
                // Load course image
                Glide.with(context)
                        .load(course.getIllustration())
                        .placeholder(R.drawable.course_placeholder)
                        .error(R.drawable.course_placeholder)
                        .into(binding.courseImageView);

                // Set course basic info
                binding.courseTitleTextView.setText(course.getTitle() != null ? course.getTitle() : "Untitled");

                // Set instructor name (can be extracted from course description or added to model)
                String instructorName = getInstructorName(course);
                binding.instructorTextView.setText(instructorName);

                // Set rating (if available in your model, otherwise use default or hide)
                float rating = getRating(course);
                binding.courseRatingBar.setRating(rating);
                binding.ratingTextView.setText(String.format(Locale.getDefault(), "%.1f (%d)", rating, getRatingCount(course)));

                // Set progress
                int progress = getProgress(course);
                binding.courseProgressBar.setProgress(progress);
                binding.progressTextView.setText(String.format(Locale.getDefault(), COMPLETE_FORMAT, progress));

                // Set time remaining
                int hours = calculateRemainingHours(course);
                int minutes = calculateRemainingMinutes(course);
                binding.timeRemainingTextView.setText(
                        String.format(Locale.getDefault(), TIME_REMAINING_FORMAT, hours, minutes));

                // Set click listener
                binding.courseCardView.setOnClickListener(v -> navigateToCourseDescription(course, context));
            }
        }

        private String getInstructorName(Course course) {
            // Ideally, your Course model would have an instructor field
            // This is a fallback if it doesn't
            return "Instructor"; // Replace with actual logic to get instructor name
        }

        private float getRating(Course course) {
            // Ideally, your Course model would have a rating field
            // This is a fallback if it doesn't
            return 4.5f; // Replace with actual logic to get rating
        }

        private int getRatingCount(Course course) {
            // Ideally, your Course model would have a ratingCount field
            // This is a fallback if it doesn't
            return 120; // Replace with actual logic to get rating count
        }

        private int getProgress(Course course) {
            // Ideally, your Course model would have a progress field
            // This is a fallback if it doesn't
            return 65; // Replace with actual logic to get progress
        }

        private int calculateRemainingHours(Course course) {
            // Calculate remaining hours based on course duration and progress
            // This is a placeholder implementation
            int totalMinutes = 5000; // Assuming getDuration returns minutes
            int remainingMinutes = (int) (totalMinutes * (1 - (getProgress(course) / 100.0)));
            return (int) TimeUnit.MINUTES.toHours(remainingMinutes);
        }

        private int calculateRemainingMinutes(Course course) {
            // Calculate remaining minutes (modulo hours)
            int totalMinutes = 5000; // Assuming getDuration returns minutes
            int remainingMinutes = (int) (totalMinutes * (1 - (getProgress(course) / 100.0)));
            return (int) (remainingMinutes % 60);
        }

        private void navigateToCourseDescription(Course course, Context context) {
            if (course != null) {
                Intent intent = new Intent(context, Description.class)
                        .putExtra("ID", course.getId());
                context.startActivity(intent);
            } else {
                Log.e("CourseAdapter", "Course ID is null. Cannot navigate to description.");
            }
        }
    }
}
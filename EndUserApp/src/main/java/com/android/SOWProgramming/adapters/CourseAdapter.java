package com.android.SOWProgramming.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.SOWProgramming.presenters.activities.Description;
import com.android.SOWProgramming.R;
import com.android.SOWProgramming.databinding.CourseItemBinding;
import com.android.SOWProgramming.models.Course;
import com.android.SOWProgramming.presenters.activities.OfflineTopicListActivity;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private final Context context;
    private List<Course> courses;
    String mode;

    public CourseAdapter(Context context, List<Course> courses, String mode) {
        this.context = context;
        this.courses = courses;
        this.mode = mode;
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
        holder.bind(course, context, mode);
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
        private final CourseItemBinding binding;

        CourseViewHolder(@NonNull CourseItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Course course, Context context, String mode) {
            if (course != null) {
                // Load course image
                Glide.with(context)
                        .load(course.getIllustration())
                        .placeholder(R.drawable.course_placeholder)
                        .error(R.drawable.course_placeholder)
                        .into(binding.courseImageView);

                // Set course basic info
                binding.courseTitleTextView.setText(course.getTitle() != null ? course.getTitle() : "Untitled");

                // Set category
                binding.categoryTextView.setText(course.getCategory());

                // Set description
                binding.descriptionTextView.setText(course.getDescription());

                // Set course info (duration, lectures, members)
                binding.durationTextView.setText(course.getDuration());
                binding.lecturesTextView.setText(formatLectures(course));
                binding.membersTextView.setText(formatMembers(course));

                // Set created date
                binding.createdAtTextView.setText(formatCreatedDate(course));

                // Set public status
                binding.publicStatusTextView.setText(getVisibilityStatus(course));

                // Set click listener
                if(mode.equals("ONLINE")) {
                    binding.courseCardView.setOnClickListener(v -> navigateToCourseDescription(course, context));
                } else {
                    binding.courseCardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(context, OfflineTopicListActivity.class);
                            intent.putExtra("ID", course.getId());
                            context.startActivity(intent);
                        }
                    });
                }
            }
        }

        private String formatLectures(Course course) {
            // Get lecture count from course
            // Add getLectureCount() method to Course model or extract from existing data
            int lectureCount = course.getLectures(); // Placeholder - replace with course.getLectureCount() or similar
            return String.format(Locale.getDefault(), "%d lectures", lectureCount);
        }

        private String formatMembers(Course course) {
            // Get student/member count from course
            // Add getStudentCount() method to Course model or extract from existing data
            int studentCount = course.getMembers(); // Placeholder - replace with course.getStudentCount() or similar

            if (studentCount >= 1000) {
                double displayCount = studentCount / 1000.0;
                return String.format(Locale.getDefault(), "%.1fk students", displayCount);
            } else {
                return String.format(Locale.getDefault(), "%d students", studentCount);
            }
        }

        private String formatCreatedDate(Course course) {
            // Format created date
            // Assuming Course model has getCreatedAt() method returning Date or timestamp
            try {
                // Replace with actual date from course.getCreatedAt() or similar
                Date createdDate = new Date(); // Placeholder
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return "Created on " + dateFormat.format(createdDate);
            } catch (Exception e) {
                return "Created recently";
            }
        }

        private String getVisibilityStatus(Course course) {
            // Get visibility status from course
            // Add getVisibility() or isPublic() method to Course model
            boolean isPublic = true; // Placeholder - replace with course.isPublic() or similar
            return isPublic ? "PUBLIC" : "PRIVATE";
        }

        private void navigateToCourseDescription(Course course, Context context) {
            if (course != null && course.getId() != 0) {
                Intent intent = new Intent(context, Description.class)
                        .putExtra("ID", course.getId());
                context.startActivity(intent);
            } else {
                Log.e("CourseAdapter", "Course ID is null. Cannot navigate to description.");
            }
        }
    }
}
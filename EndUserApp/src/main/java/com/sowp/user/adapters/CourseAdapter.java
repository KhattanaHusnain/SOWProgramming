package com.sowp.user.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.presenters.activities.Description;
import com.sowp.user.R;
import com.sowp.user.models.Course;
import com.sowp.user.presenters.activities.OfflineTopicListActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private final Context context;
    private List<Course> courses;
    private OnCourseClickListener listener;

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public CourseAdapter(Context context, List<Course> courses, OnCourseClickListener listener) {
        this.context = context;
        this.courses = courses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course, listener);
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
        private final ImageView courseImageView;
        private final TextView courseTitleTextView;
        private final TextView categoryTextView;
        private final TextView descriptionTextView;
        private final TextView durationTextView;
        private final TextView lecturesTextView;
        private final TextView membersTextView;
        private final TextView createdAtTextView;
        private final TextView publicStatusTextView;
        private final TextView instructorTextView;
        private final TextView levelTextView;
        private final TextView ratingTextView;
        private final View courseCardView;

        CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            courseImageView = itemView.findViewById(R.id.courseImageView);
            courseTitleTextView = itemView.findViewById(R.id.courseTitleTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            durationTextView = itemView.findViewById(R.id.durationTextView);
            lecturesTextView = itemView.findViewById(R.id.lecturesTextView);
            membersTextView = itemView.findViewById(R.id.membersTextView);
            createdAtTextView = itemView.findViewById(R.id.createdAtTextView);
            publicStatusTextView = itemView.findViewById(R.id.publicStatusTextView);
            instructorTextView = itemView.findViewById(R.id.instructorTextView);
            levelTextView = itemView.findViewById(R.id.levelTextView);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
            courseCardView = itemView.findViewById(R.id.courseCardView);
        }

        void bind(Course course, OnCourseClickListener listener) {
            if (course == null) return;

            loadBase64Image(course.getIllustration(), courseImageView);
            courseTitleTextView.setText(course.getTitle() != null ? course.getTitle() : "Untitled");
            categoryTextView.setText(formatCategories(course));
            descriptionTextView.setText(course.getDescription());
            durationTextView.setText(course.getDuration());
            lecturesTextView.setText(formatLectures(course));
            membersTextView.setText(formatMembers(course));
            createdAtTextView.setText(formatCreatedDate(course));
            publicStatusTextView.setText(getVisibilityStatus(course));

            if (instructorTextView != null) {
                instructorTextView.setText(course.getInstructor());
            }

            if (levelTextView != null) {
                levelTextView.setText(course.getLevel() != null ? course.getLevel() : "");
            }

            if (ratingTextView != null) {
                ratingTextView.setText(String.format(Locale.getDefault(), "%.1f", course.getAvgCourseRating()));
            }

            courseCardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCourseClick(course);
                }
            });
        }

        private void loadBase64Image(String base64String, ImageView imageView) {
            try {
                if (base64String != null && !base64String.isEmpty()) {
                    String cleanBase64 = base64String.startsWith("data:image")
                            ? base64String.substring(base64String.indexOf(",") + 1)
                            : base64String;

                    byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(R.drawable.course_placeholder);
                    }
                } else {
                    imageView.setImageResource(R.drawable.course_placeholder);
                }
            } catch (Exception e) {
                Log.e("CourseAdapter", "Error loading base64 image: " + e.getMessage());
                imageView.setImageResource(R.drawable.course_placeholder);
            }
        }

        private String formatCategories(Course course) {
            return (course.getCategoryArray() != null && !course.getCategoryArray().isEmpty())
                    ? course.getCategoryArray().get(0)
                    : "General";
        }

        private String formatLectures(Course course) {
            return String.format(Locale.getDefault(), "%d lectures", course.getLectures());
        }

        private String formatMembers(Course course) {
            int studentCount = course.getMembers();
            return studentCount >= 1000
                    ? String.format(Locale.getDefault(), "%.1fk students", studentCount / 1000.0)
                    : String.format(Locale.getDefault(), "%d students", studentCount);
        }

        private String formatCreatedDate(Course course) {
            try {
                Date createdDate = new Date(course.getCreatedAt());
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return "Created on " + dateFormat.format(createdDate);
            } catch (Exception e) {
                return "Created recently";
            }
        }

        private String getVisibilityStatus(Course course) {
            return course.isPublic() ? "PUBLIC" : "PRIVATE";
        }
    }
}
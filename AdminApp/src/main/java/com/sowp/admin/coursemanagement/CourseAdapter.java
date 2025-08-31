package com.sowp.admin.coursemanagement;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sowp.admin.R;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private List<Course> courseList;
    private Context context;
    private OnCourseClickListener listener;

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public CourseAdapter(List<Course> courseList, OnCourseClickListener listener) {
        this.courseList = courseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);

        // Basic information
        holder.tvTitle.setText(course.getTitle());
        holder.tvCourseCode.setText(course.getCourseCode());
        holder.tvInstructor.setText(course.getInstructor() != null ? course.getInstructor() : "TBA");
        holder.tvSemester.setText(course.getSemester() != null ? course.getSemester() : "N/A");
        holder.tvLevel.setText(course.getLevel() != null ? course.getLevel() : "N/A");

        // Course type chips (only show essential ones)
        holder.chipGroup.removeAllViews();

        if (course.isLab()) {
            addChip(holder.chipGroup, "Lab", R.color.chip_lab);
        } else {
            addChip(holder.chipGroup, "Theory", R.color.chip_theory);
        }

        if (course.isPaid()) {
            addChip(holder.chipGroup, "Paid", R.color.chip_paid);
        } else {
            addChip(holder.chipGroup, "Free", R.color.chip_free);
        }

        if (!course.isPublic()) {
            addChip(holder.chipGroup, "Private", R.color.chip_private);
        }

        // Course image
        loadCourseImage(holder.ivCourseImage, course.getIllustration());

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        });

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        });
    }

    private void addChip(ChipGroup chipGroup, String text, int colorRes) {
        Chip chip = new Chip(context);
        chip.setText(text);
        chip.setChipBackgroundColorResource(colorRes);
        chip.setTextColor(context.getResources().getColor(android.R.color.white));
        chip.setTextSize(10f);
        chip.setClickable(false);
        chipGroup.addView(chip);
    }

    private void loadCourseImage(ImageView imageView, String base64Image) {
        if (!TextUtils.isEmpty(base64Image)) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    imageView.setImageBitmap(decodedByte);
                } else {
                    imageView.setImageResource(R.drawable.placeholder_course);
                }
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.placeholder_course);
            }
        } else {
            imageView.setImageResource(R.drawable.placeholder_course);
        }
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivCourseImage;
        TextView tvTitle, tvCourseCode, tvInstructor, tvSemester, tvLevel;
        ChipGroup chipGroup;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.cardView);
            ivCourseImage = itemView.findViewById(R.id.ivCourseImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCourseCode = itemView.findViewById(R.id.tvCourseCode);
            tvInstructor = itemView.findViewById(R.id.tvInstructor);
            tvSemester = itemView.findViewById(R.id.tvSemester);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            chipGroup = itemView.findViewById(R.id.chipGroup);
        }
    }
}
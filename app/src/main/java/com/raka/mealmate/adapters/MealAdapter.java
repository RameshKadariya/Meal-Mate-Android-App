package com.raka.mealmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.raka.mealmate.R;
import com.raka.mealmate.models.MealPlan;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {
    private static final String TAG = "MealAdapter";
    private List<MealPlan> meals;
    private OnMealClickListener listener;

    public interface OnMealClickListener {
        void onMealClick(MealPlan meal);
        void onDeleteClick(MealPlan meal);
    }

    public MealAdapter(List<MealPlan> meals, OnMealClickListener listener) {
        this.meals = meals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_plan, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MealPlan meal = meals.get(position);
        holder.bind(meal);
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    class MealViewHolder extends RecyclerView.ViewHolder {
        private ImageView recipeImage;
        private TextView recipeName;
        private TextView cookingTime;
        private ImageButton deleteButton;

        MealViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.ivRecipeImage);
            recipeName = itemView.findViewById(R.id.tvRecipeName);
            cookingTime = itemView.findViewById(R.id.tvCookingTime);
            deleteButton = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMealClick(meals.get(position));
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteClick(meals.get(position));
                }
            });
        }

        void bind(MealPlan meal) {
            // Set recipe name
            recipeName.setText(meal.getRecipeName());
            
            // Format and set time
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            String formattedTime = timeFormat.format(meal.getDateTime());
            cookingTime.setText(formattedTime);

            // Load image using Glide
            if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(meal.getImageUrl())
                        .placeholder(R.drawable.placeholder_recipe)
                        .error(R.drawable.error_recipe)
                        .centerCrop()
                        .into(recipeImage);
            } else {
                recipeImage.setImageResource(R.drawable.placeholder_recipe);
            }
        }
    }
} 
package com.raka.mealmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.raka.mealmate.R;
import com.raka.mealmate.models.Recipe;

import java.util.List;

public class PopularRecipeAdapter extends RecyclerView.Adapter<PopularRecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private OnItemClickListener listener;

    // Interface for click events
    public interface OnItemClickListener {
        void onItemClick(Recipe recipe);
    }

    // Setter for the click listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public PopularRecipeAdapter(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_popular_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe, listener);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView recipeImage;
        TextView recipeTitle;
        TextView recipeDuration;
        TextView recipeRating;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeTitle = itemView.findViewById(R.id.recipeTitle);
            recipeDuration = itemView.findViewById(R.id.recipeDuration);
            recipeRating = itemView.findViewById(R.id.recipeRating);
        }

        void bind(Recipe recipe, OnItemClickListener listener) {
            recipeTitle.setText(recipe.getTitle());
            recipeDuration.setText(recipe.getDuration());
            recipeRating.setText(recipe.getRating() + " ★");

            Glide.with(itemView.getContext())
                    .load(recipe.getImageUrl())
                    .centerCrop()
                    .into(recipeImage);
                    
            // Set click listener on the item view
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onItemClick(recipe));
            }
        }
    }
}
package com.raka.mealmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.raka.mealmate.R;
import com.raka.mealmate.models.Recipe;

import java.util.List;

public class FeaturedRecipeAdapter extends RecyclerView.Adapter<FeaturedRecipeAdapter.RecipeViewHolder> {

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

    public FeaturedRecipeAdapter(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe, listener);
        
        // Add animation
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(
            holder.itemView.getContext(), 
            R.anim.recipe_item_animation
        ));
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
        MaterialButton viewRecipeButton;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            recipeTitle = itemView.findViewById(R.id.recipeTitle);
            recipeDuration = itemView.findViewById(R.id.recipeDuration);
            recipeRating = itemView.findViewById(R.id.recipeRating);
            viewRecipeButton = itemView.findViewById(R.id.viewRecipeButton);
        }

        void bind(Recipe recipe, OnItemClickListener listener) {
            recipeTitle.setText(recipe.getTitle());
            recipeDuration.setText(recipe.getDuration());
            recipeRating.setText(String.format("%.1f ★", recipe.getRating()));
            
            // Load image with crossfade animation
            Glide.with(itemView.getContext())
                    .load(recipe.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .centerCrop()
                    .into(recipeImage);
                    
            // Set click listeners
            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onItemClick(recipe);
                }
            };
            
            itemView.setOnClickListener(clickListener);
            viewRecipeButton.setOnClickListener(clickListener);
        }
    }
}
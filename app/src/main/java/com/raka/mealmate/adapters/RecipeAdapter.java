package com.raka.mealmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.raka.mealmate.R;
import com.raka.mealmate.models.Recipe;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private final OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onAddToMealPlanClick(Recipe recipe);
    }

    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    public void updateRecipes(List<Recipe> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        
        holder.titleText.setText(recipe.getTitle());
        holder.cookingTimeText.setText(recipe.getDuration());
        holder.ratingText.setText(String.format("%.1f", recipe.getRating()));

        Glide.with(holder.itemView.getContext())
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder_recipe)
                .error(R.drawable.error_recipe)
                .centerCrop()
                .into(holder.recipeImage);

        holder.cardView.setOnClickListener(v -> listener.onRecipeClick(recipe));
        holder.addToMealPlanButton.setOnClickListener(v -> listener.onAddToMealPlanClick(recipe));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView recipeImage;
        TextView titleText;
        TextView cookingTimeText;
        TextView ratingText;
        MaterialButton addToMealPlanButton;

        RecipeViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            recipeImage = itemView.findViewById(R.id.recipeImage);
            titleText = itemView.findViewById(R.id.titleText);
            cookingTimeText = itemView.findViewById(R.id.cookingTimeText);
            ratingText = itemView.findViewById(R.id.ratingText);
            addToMealPlanButton = itemView.findViewById(R.id.addToMealPlanButton);
        }
    }
} 
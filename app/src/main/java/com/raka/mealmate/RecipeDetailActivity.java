package com.raka.mealmate;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.raka.mealmate.adapters.IngredientAdapter;
import com.raka.mealmate.adapters.InstructionAdapter;
import com.raka.mealmate.dialogs.MealPlanDialog;
import com.raka.mealmate.models.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.raka.mealmate.models.MealPlan;
import com.raka.mealmate.repositories.MealPlanRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecipeDetailActivity extends AppCompatActivity {

    private static final String EXTRA_RECIPE = "extra_recipe";
    private Recipe recipe;
    private MealPlanRepository mealPlanRepository;

    public static Intent newIntent(Context context, Recipe recipe) {
        Intent intent = new Intent(context, RecipeDetailActivity.class);
        intent.putExtra(EXTRA_RECIPE, recipe);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        // Initialize repository
        mealPlanRepository = new MealPlanRepository();

        recipe = (Recipe) getIntent().getSerializableExtra(EXTRA_RECIPE);
        if (recipe == null) {
            finish();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(recipe.getTitle());

        // Set recipe title
        TextView recipeTitleText = findViewById(R.id.recipeTitleText);
        recipeTitleText.setText(recipe.getTitle());

        // Load recipe image
        ImageView recipeImage = findViewById(R.id.recipeImage);
        Glide.with(this)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder_recipe)
                .error(R.drawable.error_recipe)
                .centerCrop()
                .into(recipeImage);

        // Setup cooking time and rating
        TextView cookingTimeText = findViewById(R.id.cookingTimeText);
        TextView ratingText = findViewById(R.id.ratingText);
        cookingTimeText.setText(recipe.getDuration());
        ratingText.setText(String.format("%.1f", recipe.getRating()));

        // Setup ingredients list
        RecyclerView ingredientsRecyclerView = findViewById(R.id.ingredientsRecyclerView);
        ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        IngredientAdapter ingredientAdapter = new IngredientAdapter(recipe.getIngredients());
        ingredientsRecyclerView.setAdapter(ingredientAdapter);

        // Setup instructions list
        RecyclerView instructionsRecyclerView = findViewById(R.id.instructionsRecyclerView);
        instructionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        InstructionAdapter instructionAdapter = new InstructionAdapter(recipe.getInstructions());
        instructionsRecyclerView.setAdapter(instructionAdapter);

        // Setup add to meal plan button
        ExtendedFloatingActionButton addToMealPlanFab = findViewById(R.id.addToMealPlanExtendedFab);
        addToMealPlanFab.setVisibility(View.VISIBLE); // Ensure visibility
        addToMealPlanFab.extend(); // Start in extended state
        
        // Setup scroll behavior
        NestedScrollView scrollView = findViewById(R.id.nestedScrollView);
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) 
            (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > oldScrollY && addToMealPlanFab.isExtended()) {
                    addToMealPlanFab.shrink();
                } else if (scrollY < oldScrollY && !addToMealPlanFab.isExtended()) {
                    addToMealPlanFab.extend();
                }
        });

        addToMealPlanFab.setOnClickListener(v -> showAddToMealPlanDialog());
    }

    private void showAddToMealPlanDialog() {
        MealPlanDialog dialog = new MealPlanDialog(this, recipe, new MealPlanDialog.OnMealPlanSelectedListener() {
            @Override
            public void onMealPlanSelected(Recipe recipe, Date date, String mealTime) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(RecipeDetailActivity.this, "Please log in to add meal plans", Toast.LENGTH_SHORT).show();
                    return;
                }

                MealPlan mealPlan = new MealPlan(currentUser.getUid(), recipe, date, mealTime);
                mealPlanRepository.addMealPlan(mealPlan)
                    .addOnSuccessListener(aVoid -> {
                        String message = String.format("Added %s to %s for %s", 
                                recipe.getTitle(), 
                                mealTime.toLowerCase(), 
                                new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date));
                        Toast.makeText(RecipeDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(RecipeDetailActivity.this, 
                                "Failed to add to meal plan: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
            }
        });
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 
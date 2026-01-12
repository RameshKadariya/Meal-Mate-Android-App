package com.raka.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.raka.mealmate.adapters.EditableIngredientAdapter;
import com.raka.mealmate.adapters.EditableInstructionAdapter;
import com.raka.mealmate.models.Ingredient;
import com.raka.mealmate.models.Recipe;

import java.util.ArrayList;
import java.util.List;

public class AddRecipeActivity extends AppCompatActivity implements 
        EditableIngredientAdapter.OnIngredientRemovedListener,
        EditableInstructionAdapter.OnInstructionRemovedListener {

    private TextInputEditText recipeNameEditText;
    private TextInputEditText imageUrlEditText;
    private TextInputEditText prepTimeEditText;
    private TextInputEditText ratingEditText;
    private RecyclerView ingredientsRecyclerView;
    private RecyclerView instructionsRecyclerView;
    private MaterialButton addIngredientButton;
    private MaterialButton addInstructionButton;
    private MaterialButton saveRecipeButton;
    private View loadingView;

    private List<Ingredient> ingredients = new ArrayList<>();
    private List<String> instructions = new ArrayList<>();
    private EditableIngredientAdapter ingredientAdapter;
    private EditableInstructionAdapter instructionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("Add New Recipe");

        // Initialize views
        recipeNameEditText = findViewById(R.id.recipeNameEditText);
        imageUrlEditText = findViewById(R.id.imageUrlEditText);
        prepTimeEditText = findViewById(R.id.prepTimeEditText);
        ratingEditText = findViewById(R.id.ratingEditText);
        ingredientsRecyclerView = findViewById(R.id.ingredientsRecyclerView);
        instructionsRecyclerView = findViewById(R.id.instructionsRecyclerView);
        addIngredientButton = findViewById(R.id.addIngredientButton);
        addInstructionButton = findViewById(R.id.addInstructionButton);
        saveRecipeButton = findViewById(R.id.saveRecipeButton);
        loadingView = findViewById(R.id.loadingView);

        // Setup RecyclerViews
        setupIngredientsRecyclerView();
        setupInstructionsRecyclerView();

        // Setup buttons
        addIngredientButton.setOnClickListener(v -> addNewIngredient());
        addInstructionButton.setOnClickListener(v -> addNewInstruction());
        saveRecipeButton.setOnClickListener(v -> saveRecipe());
    }

    private void setupIngredientsRecyclerView() {
        ingredientsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ingredientAdapter = new EditableIngredientAdapter(ingredients, this);
        ingredientsRecyclerView.setAdapter(ingredientAdapter);
    }

    private void setupInstructionsRecyclerView() {
        instructionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        instructionAdapter = new EditableInstructionAdapter(instructions, this);
        instructionsRecyclerView.setAdapter(instructionAdapter);
    }

    private void addNewIngredient() {
        ingredients.add(new Ingredient("", 0, "", ""));
        ingredientAdapter.notifyItemInserted(ingredients.size() - 1);
    }

    private void addNewInstruction() {
        instructions.add("");
        instructionAdapter.notifyItemInserted(instructions.size() - 1);
    }

    private void saveRecipe() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        showLoading();

        // Create new recipe
        String recipeName = recipeNameEditText.getText().toString().trim();
        String imageUrl = imageUrlEditText.getText().toString().trim();
        String prepTime = prepTimeEditText.getText().toString().trim();
        float rating = Float.parseFloat(ratingEditText.getText().toString().trim());

        // Filter out any empty ingredients or instructions
        List<Ingredient> validIngredients = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient != null && !TextUtils.isEmpty(ingredient.getName())) {
                validIngredients.add(ingredient);
            }
        }
        
        List<String> validInstructions = new ArrayList<>();
        for (String instruction : instructions) {
            if (!TextUtils.isEmpty(instruction)) {
                validInstructions.add(instruction);
            }
        }

        Recipe newRecipe = new Recipe(
                recipeName,
                imageUrl,
                prepTime,
                rating,
                validIngredients,
                validInstructions
        );
        
        // Set a default category
        newRecipe.setCategory("Other");

        // Return the new recipe to the calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("new_recipe", newRecipe);
        setResult(RESULT_OK, resultIntent);
        hideLoading();
        finish();
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Check recipe name
        if (TextUtils.isEmpty(recipeNameEditText.getText())) {
            recipeNameEditText.setError("Recipe name is required");
            isValid = false;
        }

        // Check image URL
        if (TextUtils.isEmpty(imageUrlEditText.getText())) {
            imageUrlEditText.setError("Image URL is required");
            isValid = false;
        }

        // Check prep time
        if (TextUtils.isEmpty(prepTimeEditText.getText())) {
            prepTimeEditText.setError("Preparation time is required");
            isValid = false;
        }

        // Check rating
        if (TextUtils.isEmpty(ratingEditText.getText())) {
            ratingEditText.setError("Rating is required");
            isValid = false;
        } else {
            try {
                float rating = Float.parseFloat(ratingEditText.getText().toString());
                if (rating < 0 || rating > 5) {
                    ratingEditText.setError("Rating must be between 0 and 5");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                ratingEditText.setError("Invalid rating format");
                isValid = false;
            }
        }

        // Check ingredients
        if (ingredients.isEmpty()) {
            Toast.makeText(this, "Please add at least one ingredient", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            for (Ingredient ingredient : ingredients) {
                if (TextUtils.isEmpty(ingredient.getName())) {
                    Toast.makeText(this, "All ingredients must have a name", Toast.LENGTH_SHORT).show();
                    isValid = false;
                    break;
                }
            }
        }

        // Check instructions
        if (instructions.isEmpty()) {
            Toast.makeText(this, "Please add at least one instruction", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else {
            for (String instruction : instructions) {
                if (TextUtils.isEmpty(instruction)) {
                    Toast.makeText(this, "All instructions must have content", Toast.LENGTH_SHORT).show();
                    isValid = false;
                    break;
                }
            }
        }

        return isValid;
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
    }

    @Override
    public void onIngredientRemoved(int position) {
        ingredients.remove(position);
        ingredientAdapter.notifyItemRemoved(position);
        ingredientAdapter.notifyItemRangeChanged(position, ingredients.size());
    }

    @Override
    public void onInstructionRemoved(int position) {
        instructions.remove(position);
        instructionAdapter.notifyItemRemoved(position);
        instructionAdapter.notifyItemRangeChanged(position, instructions.size());
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

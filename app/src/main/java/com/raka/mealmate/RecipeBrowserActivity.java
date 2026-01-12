package com.raka.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.raka.mealmate.adapters.RecipeAdapter;
import com.raka.mealmate.dialogs.MealPlanDialog;
import com.raka.mealmate.models.Recipe;
import com.raka.mealmate.models.Ingredient;
import com.raka.mealmate.models.MealPlan;
import com.raka.mealmate.repositories.MealPlanRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class RecipeBrowserActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener, MealPlanDialog.OnMealPlanSelectedListener {

    private static final int ADD_RECIPE_REQUEST_CODE = 100;
    private static final String RECIPES_NODE = "recipes";
    
    private RecyclerView recipesRecyclerView;
    private TextInputEditText searchEditText;
    private ChipGroup categoryChipGroup;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> allRecipes;
    private MealPlanRepository mealPlanRepository;
    private View loadingView;
    private View errorView;
    private MaterialButton retryButton;
    private FloatingActionButton addRecipeFab;
    private DatabaseReference recipesRef;
    
    private ActivityResultLauncher<Intent> addRecipeLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_browser);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Initialize views
        recipesRecyclerView = findViewById(R.id.recipesRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        loadingView = findViewById(R.id.loadingView);
        errorView = findViewById(R.id.errorView);
        retryButton = findViewById(R.id.retryButton);
        addRecipeFab = findViewById(R.id.addRecipeFab);

        // Initialize Firebase Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        recipesRef = database.getReference(RECIPES_NODE);

        // Initialize activity result launcher
        addRecipeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Recipe newRecipe = (Recipe) result.getData().getSerializableExtra("new_recipe");
                    if (newRecipe != null) {
                        addRecipeToList(newRecipe);
                        saveRecipeToDatabase(newRecipe);
                    }
                }
            }
        );

        // Initialize recipes
        allRecipes = new ArrayList<>();
        
        // Load recipes from Firebase Realtime Database first, then add default recipes if needed
        loadRecipesFromDatabase();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search
        setupSearch();

        // Setup category filter
        setupCategoryFilter();

        // Setup add recipe button
        addRecipeFab.setOnClickListener(v -> launchAddRecipeActivity());

        // Initialize repository
        try {
            mealPlanRepository = new MealPlanRepository();
        } catch (Exception e) {
            // Handle the case where Firebase Auth is not initialized or user is not logged in
            showError("Please log in to use meal planning features");
        }

        // Setup retry button
        retryButton.setOnClickListener(v -> {
            hideError();
            loadRecipesFromDatabase();
        });
    }

    private void loadRecipesFromDatabase() {
        showLoading();
        
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Recipe> loadedRecipes = new ArrayList<>();
                
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    try {
                        Recipe recipe = child.getValue(Recipe.class);
                        recipe.setId(child.getKey());
                        loadedRecipes.add(recipe);
                    } catch (Exception e) {
                        // Log error but continue processing other recipes
                        e.printStackTrace();
                    }
                }
                
                // First setup default recipes
                setupRecipes();
                
                // Then add loaded recipes, avoiding duplicates
                if (!loadedRecipes.isEmpty()) {
                    for (Recipe loadedRecipe : loadedRecipes) {
                        boolean isDuplicate = false;
                        for (Recipe existingRecipe : allRecipes) {
                            if (existingRecipe.getId() != null && 
                                existingRecipe.getId().equals(loadedRecipe.getId())) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (!isDuplicate) {
                            allRecipes.add(loadedRecipe);
                        }
                    }
                }
                
                // Update the RecyclerView
                if (recipeAdapter != null) {
                    recipeAdapter.updateRecipes(allRecipes);
                }
                hideLoading();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // If there was an error loading recipes, make sure we at least have the default recipes
                setupRecipes();
                if (recipeAdapter != null) {
                    recipeAdapter.updateRecipes(allRecipes);
                }
                hideLoading();
            }
        });
    }

    private void saveRecipeToDatabase(Recipe recipe) {
        // Create a new child with auto-generated key
        String key = recipesRef.push().getKey();
        recipe.setId(key);
        
        // Update the child with the recipe
        recipesRef.child(key).setValue(recipe)
                .addOnSuccessListener(aVoid -> {
                    // Recipe successfully saved
                })
                .addOnFailureListener(e -> {
                    // Error saving recipe
                    Toast.makeText(RecipeBrowserActivity.this, 
                            "Error saving recipe: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRecipes() {
        // Create a temporary list for the default recipes
        List<Recipe> defaultRecipes = new ArrayList<>();

        // 1. Chicken Stir-Fry
        List<Ingredient> stirFryIngredients = new ArrayList<>();
        stirFryIngredients.add(new Ingredient("Chicken breast", 500, "g", "Protein"));
        stirFryIngredients.add(new Ingredient("Bell peppers", 2, "pieces", "Vegetables"));
        stirFryIngredients.add(new Ingredient("Broccoli", 1, "head", "Vegetables"));
        stirFryIngredients.add(new Ingredient("Soy sauce", 3, "tbsp", "Condiments"));
        stirFryIngredients.add(new Ingredient("Ginger", 1, "tbsp", "Spices"));
        stirFryIngredients.add(new Ingredient("Garlic", 3, "cloves", "Spices"));

        List<String> stirFryInstructions = new ArrayList<>();
        stirFryInstructions.add("Cut chicken into bite-sized pieces");
        stirFryInstructions.add("Chop all vegetables");
        stirFryInstructions.add("Heat oil in a large wok");
        stirFryInstructions.add("Cook chicken until golden");
        stirFryInstructions.add("Add vegetables and stir-fry");
        stirFryInstructions.add("Add sauce and simmer");

        defaultRecipes.add(new Recipe(
                "Chicken Stir-Fry",
                "https://images.unsplash.com/photo-1603133872878-684f208fb84b",
                "25 mins",
                4.5f,
                stirFryIngredients,
                stirFryInstructions
        ));

        // 2. Vegetable Quinoa Bowl
        List<Ingredient> quinoaIngredients = new ArrayList<>();
        quinoaIngredients.add(new Ingredient("Quinoa", 1, "cup", "Grains"));
        quinoaIngredients.add(new Ingredient("Sweet potato", 1, "large", "Vegetables"));
        quinoaIngredients.add(new Ingredient("Chickpeas", 400, "g", "Protein"));
        quinoaIngredients.add(new Ingredient("Kale", 2, "cups", "Vegetables"));
        quinoaIngredients.add(new Ingredient("Avocado", 1, "piece", "Vegetables"));

        List<String> quinoaInstructions = new ArrayList<>();
        quinoaInstructions.add("Cook quinoa according to package instructions");
        quinoaInstructions.add("Roast sweet potato cubes in the oven");
        quinoaInstructions.add("Drain and season chickpeas");
        quinoaInstructions.add("Massage kale with olive oil");
        quinoaInstructions.add("Assemble bowl with all ingredients");

        defaultRecipes.add(new Recipe(
                "Vegetable Quinoa Bowl",
                "https://images.unsplash.com/photo-1543340713-1bf56d3d1b68",
                "30 mins",
                4.2f,
                quinoaIngredients,
                quinoaInstructions
        ));

        // 3. Pasta Primavera
        List<Ingredient> pastaIngredients = new ArrayList<>();
        pastaIngredients.add(new Ingredient("Spaghetti", 250, "g", "Pasta"));
        pastaIngredients.add(new Ingredient("Cherry tomatoes", 200, "g", "Vegetables"));
        pastaIngredients.add(new Ingredient("Zucchini", 1, "piece", "Vegetables"));
        pastaIngredients.add(new Ingredient("Bell peppers", 2, "pieces", "Vegetables"));
        pastaIngredients.add(new Ingredient("Olive oil", 2, "tbsp", "Oil"));
        pastaIngredients.add(new Ingredient("Garlic", 2, "cloves", "Spices"));
        pastaIngredients.add(new Ingredient("Parmesan", 50, "g", "Dairy"));

        List<String> pastaInstructions = new ArrayList<>();
        pastaInstructions.add("Cook spaghetti in salted water");
        pastaInstructions.add("Sauté vegetables in olive oil");
        pastaInstructions.add("Add garlic and cook for 1 minute");
        pastaInstructions.add("Combine cooked spaghetti and vegetables");
        pastaInstructions.add("Top with parmesan cheese");

        defaultRecipes.add(new Recipe(
                "Pasta Primavera",
                "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8",
                "20 mins",
                4.8f,
                pastaIngredients,
                pastaInstructions
        ));

        // 4. Grilled Salmon
        List<Ingredient> salmonIngredients = new ArrayList<>();
        salmonIngredients.add(new Ingredient("Salmon fillet", 200, "g", "Protein"));
        salmonIngredients.add(new Ingredient("Lemon", 1, "piece", "Fruits"));
        salmonIngredients.add(new Ingredient("Olive oil", 2, "tbsp", "Condiments"));
        salmonIngredients.add(new Ingredient("Garlic", 2, "cloves", "Spices"));
        salmonIngredients.add(new Ingredient("Dill", 2, "tbsp", "Herbs"));
        salmonIngredients.add(new Ingredient("Black pepper", 1, "tsp", "Spices"));

        List<String> salmonInstructions = new ArrayList<>();
        salmonInstructions.add("Marinate salmon with oil, lemon, and herbs");
        salmonInstructions.add("Preheat grill to medium-high");
        salmonInstructions.add("Grill salmon for 4-5 minutes per side");
        salmonInstructions.add("Rest for 5 minutes before serving");

        defaultRecipes.add(new Recipe(
                "Grilled Salmon",
                "https://images.unsplash.com/photo-1485921325833-c519f76c4927",
                "25 mins",
                4.6f,
                salmonIngredients,
                salmonInstructions
        ));

        // 5. Buddha Bowl
        List<Ingredient> buddhaBowlIngredients = new ArrayList<>();
        buddhaBowlIngredients.add(new Ingredient("Brown rice", 1, "cup", "Grains"));
        buddhaBowlIngredients.add(new Ingredient("Tofu", 200, "g", "Protein"));
        buddhaBowlIngredients.add(new Ingredient("Spinach", 2, "cups", "Vegetables"));
        buddhaBowlIngredients.add(new Ingredient("Carrots", 2, "pieces", "Vegetables"));
        buddhaBowlIngredients.add(new Ingredient("Tahini", 2, "tbsp", "Condiments"));

        List<String> buddhaBowlInstructions = new ArrayList<>();
        buddhaBowlInstructions.add("Cook brown rice");
        buddhaBowlInstructions.add("Press and cube tofu");
        buddhaBowlInstructions.add("Roast vegetables");
        buddhaBowlInstructions.add("Make tahini dressing");
        buddhaBowlInstructions.add("Assemble bowl");

        defaultRecipes.add(new Recipe(
                "Buddha Bowl",
                "https://images.pexels.com/photos/1640770/pexels-photo-1640770.jpeg",
                "35 mins",
                4.3f,
                buddhaBowlIngredients,
                buddhaBowlInstructions
        ));

        // 6. Chicken Curry
        List<Ingredient> curryIngredients = new ArrayList<>();
        curryIngredients.add(new Ingredient("Chicken thighs", 600, "g", "Protein"));
        curryIngredients.add(new Ingredient("Coconut milk", 400, "ml", "Dairy"));
        curryIngredients.add(new Ingredient("Curry powder", 2, "tbsp", "Spices"));
        curryIngredients.add(new Ingredient("Onion", 1, "large", "Vegetables"));
        curryIngredients.add(new Ingredient("Tomatoes", 2, "pieces", "Vegetables"));

        List<String> curryInstructions = new ArrayList<>();
        curryInstructions.add("Sauté onions until translucent");
        curryInstructions.add("Add curry powder and toast");
        curryInstructions.add("Add chicken and brown");
        curryInstructions.add("Pour in coconut milk and simmer");
        curryInstructions.add("Cook until chicken is tender");

        defaultRecipes.add(new Recipe(
                "Chicken Curry",
                "https://images.unsplash.com/photo-1565557623262-b51c2513a641",
                "45 mins",
                4.7f,
                curryIngredients,
                curryInstructions
        ));

        // 7. Greek Salad
        List<Ingredient> greekSaladIngredients = new ArrayList<>();
        greekSaladIngredients.add(new Ingredient("Cucumber", 1, "large", "Vegetables"));
        greekSaladIngredients.add(new Ingredient("Tomatoes", 3, "medium", "Vegetables"));
        greekSaladIngredients.add(new Ingredient("Red onion", 1, "medium", "Vegetables"));
        greekSaladIngredients.add(new Ingredient("Feta cheese", 200, "g", "Dairy"));
        greekSaladIngredients.add(new Ingredient("Olives", 100, "g", "Vegetables"));

        List<String> greekSaladInstructions = new ArrayList<>();
        greekSaladInstructions.add("Chop all vegetables");
        greekSaladInstructions.add("Cube feta cheese");
        greekSaladInstructions.add("Mix with olive oil and oregano");
        greekSaladInstructions.add("Season with salt and pepper");

        defaultRecipes.add(new Recipe(
                "Greek Salad",
                "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe",
                "15 mins",
                4.4f,
                greekSaladIngredients,
                greekSaladInstructions
        ));

        // 8. Chocolate Brownies
        List<Ingredient> brownieIngredients = new ArrayList<>();
        brownieIngredients.add(new Ingredient("Dark chocolate", 200, "g", "Sweets"));
        brownieIngredients.add(new Ingredient("Butter", 180, "g", "Dairy"));
        brownieIngredients.add(new Ingredient("Eggs", 3, "large", "Protein"));
        brownieIngredients.add(new Ingredient("Sugar", 220, "g", "Sweets"));
        brownieIngredients.add(new Ingredient("Flour", 120, "g", "Grains"));

        List<String> brownieInstructions = new ArrayList<>();
        brownieInstructions.add("Melt chocolate and butter");
        brownieInstructions.add("Whisk eggs and sugar");
        brownieInstructions.add("Combine all ingredients");
        brownieInstructions.add("Bake for 25 minutes");

        defaultRecipes.add(new Recipe(
                "Chocolate Brownies",
                "https://images.unsplash.com/photo-1606313564200-e75d5e30476c",
                "40 mins",
                4.9f,
                brownieIngredients,
                brownieInstructions
        ));

        // 9. Mushroom Risotto
        List<Ingredient> risottoIngredients = new ArrayList<>();
        risottoIngredients.add(new Ingredient("Arborio rice", 300, "g", "Grains"));
        risottoIngredients.add(new Ingredient("Mushrooms", 400, "g", "Vegetables"));
        risottoIngredients.add(new Ingredient("Parmesan", 100, "g", "Dairy"));
        risottoIngredients.add(new Ingredient("White wine", 100, "ml", "Condiments"));
        risottoIngredients.add(new Ingredient("Vegetable stock", 1, "L", "Condiments"));

        List<String> risottoInstructions = new ArrayList<>();
        risottoInstructions.add("Sauté mushrooms until golden");
        risottoInstructions.add("Toast rice with wine");
        risottoInstructions.add("Add stock gradually while stirring");
        risottoInstructions.add("Finish with parmesan");

        defaultRecipes.add(new Recipe(
                "Mushroom Risotto",
                "https://images.unsplash.com/photo-1476124369491-e7addf5db371",
                "35 mins",
                4.5f,
                risottoIngredients,
                risottoInstructions
        ));

        // 10. Berry Smoothie Bowl
        List<Ingredient> smoothieIngredients = new ArrayList<>();
        smoothieIngredients.add(new Ingredient("Mixed berries", 300, "g", "Fruits"));
        smoothieIngredients.add(new Ingredient("Banana", 1, "piece", "Fruits"));
        smoothieIngredients.add(new Ingredient("Greek yogurt", 200, "g", "Dairy"));
        smoothieIngredients.add(new Ingredient("Honey", 2, "tbsp", "Condiments"));
        smoothieIngredients.add(new Ingredient("Granola", 50, "g", "Grains"));

        List<String> smoothieInstructions = new ArrayList<>();
        smoothieInstructions.add("Blend frozen berries and banana");
        smoothieInstructions.add("Add yogurt and honey");
        smoothieInstructions.add("Pour into bowl");
        smoothieInstructions.add("Top with fresh fruits and granola");

        defaultRecipes.add(new Recipe(
                "Berry Smoothie Bowl",
                "https://images.unsplash.com/photo-1553530979-7ee52a2670c4",
                "10 mins",
                4.3f,
                smoothieIngredients,
                smoothieInstructions
        ));

        // 11. Pad Thai
        List<Ingredient> padThaiIngredients = new ArrayList<>();
        padThaiIngredients.add(new Ingredient("Rice noodles", 200, "g", "Grains"));
        padThaiIngredients.add(new Ingredient("Tofu", 200, "g", "Protein"));
        padThaiIngredients.add(new Ingredient("Bean sprouts", 100, "g", "Vegetables"));
        padThaiIngredients.add(new Ingredient("Eggs", 2, "pieces", "Protein"));
        padThaiIngredients.add(new Ingredient("Tamarind paste", 2, "tbsp", "Condiments"));
        padThaiIngredients.add(new Ingredient("Fish sauce", 2, "tbsp", "Condiments"));
        padThaiIngredients.add(new Ingredient("Peanuts", 50, "g", "Nuts"));

        List<String> padThaiInstructions = new ArrayList<>();
        padThaiInstructions.add("Soak rice noodles in warm water");
        padThaiInstructions.add("Make sauce with tamarind and fish sauce");
        padThaiInstructions.add("Stir-fry tofu until golden");
        padThaiInstructions.add("Add eggs and scramble");
        padThaiInstructions.add("Add noodles and sauce");
        padThaiInstructions.add("Top with peanuts and bean sprouts");

        defaultRecipes.add(new Recipe(
                "Pad Thai",
                "https://images.unsplash.com/photo-1559314809-0d155014e29e",
                "35 mins",
                4.6f,
                padThaiIngredients,
                padThaiInstructions
        ));

        // 12. Margherita Pizza
        List<Ingredient> pizzaIngredients = new ArrayList<>();
        pizzaIngredients.add(new Ingredient("Pizza dough", 1, "piece", "Grains"));
        pizzaIngredients.add(new Ingredient("Tomato sauce", 100, "ml", "Condiments"));
        pizzaIngredients.add(new Ingredient("Fresh mozzarella", 200, "g", "Dairy"));
        pizzaIngredients.add(new Ingredient("Fresh basil", 10, "leaves", "Herbs"));
        pizzaIngredients.add(new Ingredient("Olive oil", 2, "tbsp", "Condiments"));

        List<String> pizzaInstructions = new ArrayList<>();
        pizzaInstructions.add("Preheat oven to 220°C");
        pizzaInstructions.add("Roll out pizza dough");
        pizzaInstructions.add("Spread tomato sauce");
        pizzaInstructions.add("Add torn mozzarella");
        pizzaInstructions.add("Bake for 12-15 minutes");
        pizzaInstructions.add("Top with fresh basil");

        defaultRecipes.add(new Recipe(
                "Margherita Pizza",
                "https://images.unsplash.com/photo-1604068549290-dea0e4a305ca",
                "25 mins",
                4.8f,
                pizzaIngredients,
                pizzaInstructions
        ));

        // 13. Miso Soup
        List<Ingredient> misoIngredients = new ArrayList<>();
        misoIngredients.add(new Ingredient("Dashi stock", 1, "L", "Condiments"));
        misoIngredients.add(new Ingredient("Miso paste", 3, "tbsp", "Condiments"));
        misoIngredients.add(new Ingredient("Tofu", 200, "g", "Protein"));
        misoIngredients.add(new Ingredient("Wakame seaweed", 10, "g", "Vegetables"));
        misoIngredients.add(new Ingredient("Green onions", 2, "stalks", "Vegetables"));

        List<String> misoInstructions = new ArrayList<>();
        misoInstructions.add("Heat dashi stock");
        misoInstructions.add("Dissolve miso paste");
        misoInstructions.add("Add cubed tofu");
        misoInstructions.add("Add rehydrated wakame");
        misoInstructions.add("Garnish with green onions");

        defaultRecipes.add(new Recipe(
                "Miso Soup",
                "https://images.unsplash.com/photo-1547592166-23ac45744acd",
                "15 mins",
                4.4f,
                misoIngredients,
                misoInstructions
        ));

        // 14. Dal Bhat
        List<Ingredient> dalBhatIngredients = new ArrayList<>();
        dalBhatIngredients.add(new Ingredient("Rice", 300, "g", "Grains"));
        dalBhatIngredients.add(new Ingredient("Yellow lentils", 200, "g", "Protein"));
        dalBhatIngredients.add(new Ingredient("Spinach", 200, "g", "Vegetables"));
        dalBhatIngredients.add(new Ingredient("Tomatoes", 2, "pieces", "Vegetables"));
        dalBhatIngredients.add(new Ingredient("Cumin seeds", 1, "tsp", "Spices"));
        dalBhatIngredients.add(new Ingredient("Turmeric", 1, "tsp", "Spices"));
        dalBhatIngredients.add(new Ingredient("Garlic", 4, "cloves", "Spices"));
        dalBhatIngredients.add(new Ingredient("Ginger", 2, "inch", "Spices"));

        List<String> dalBhatInstructions = new ArrayList<>();
        dalBhatInstructions.add("Cook rice until fluffy");
        dalBhatInstructions.add("Wash and cook yellow lentils with turmeric");
        dalBhatInstructions.add("Prepare tempering with cumin seeds, garlic, and ginger");
        dalBhatInstructions.add("Add tempering to dal");
        dalBhatInstructions.add("Serve hot with rice and spinach");

        defaultRecipes.add(new Recipe(
                "Dal Bhat",
                "https://images.unsplash.com/photo-1585937421612-70a008356fbe",
                "45 mins",
                4.9f,
                dalBhatIngredients,
                dalBhatInstructions
        ));

        // 15. Momo
        List<Ingredient> momoIngredients = new ArrayList<>();
        momoIngredients.add(new Ingredient("All-purpose flour", 300, "g", "Grains"));
        momoIngredients.add(new Ingredient("Cabbage", 200, "g", "Vegetables"));
        momoIngredients.add(new Ingredient("Carrots", 100, "g", "Vegetables"));
        momoIngredients.add(new Ingredient("Onions", 2, "large", "Vegetables"));
        momoIngredients.add(new Ingredient("Ginger", 2, "inch", "Spices"));
        momoIngredients.add(new Ingredient("Garlic", 6, "cloves", "Spices"));
        momoIngredients.add(new Ingredient("Cilantro", 1, "bunch", "Herbs"));

        List<String> momoInstructions = new ArrayList<>();
        momoInstructions.add("Make dough with flour and water");
        momoInstructions.add("Prepare filling with minced vegetables");
        momoInstructions.add("Roll dough and cut into circles");
        momoInstructions.add("Fill and pleat the momos");
        momoInstructions.add("Steam for 15-20 minutes");
        momoInstructions.add("Serve with spicy tomato chutney");

        defaultRecipes.add(new Recipe(
                "Vegetable Momo",
                "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb",
                "60 mins",
                4.8f,
                momoIngredients,
                momoInstructions
        ));

        // 16. Sel Roti
        List<Ingredient> selRotiIngredients = new ArrayList<>();
        selRotiIngredients.add(new Ingredient("Rice flour", 400, "g", "Grains"));
        selRotiIngredients.add(new Ingredient("Banana", 2, "pieces", "Fruits"));
        selRotiIngredients.add(new Ingredient("Cardamom powder", 1, "tsp", "Spices"));
        selRotiIngredients.add(new Ingredient("Sugar", 100, "g", "Sweets"));
        selRotiIngredients.add(new Ingredient("Ghee", 100, "ml", "Dairy"));

        List<String> selRotiInstructions = new ArrayList<>();
        selRotiInstructions.add("Soak rice for 4-6 hours");
        selRotiInstructions.add("Grind into smooth batter");
        selRotiInstructions.add("Let batter ferment for 2-3 hours");
        selRotiInstructions.add("Heat oil in a large pan");
        selRotiInstructions.add("Pour batter in circular motion");
        selRotiInstructions.add("Fry until golden brown");

        defaultRecipes.add(new Recipe(
                "Sel Roti",
                "https://en.wikipedia.org/wiki/File:Sel_Roti.jpg",
                "40 mins",
                4.7f,
                selRotiIngredients,
                selRotiInstructions
        ));

        // 17. Aloo Tama
        List<Ingredient> alooTamaIngredients = new ArrayList<>();
        alooTamaIngredients.add(new Ingredient("Potatoes", 400, "g", "Vegetables"));
        alooTamaIngredients.add(new Ingredient("Bamboo shoots", 200, "g", "Vegetables"));
        alooTamaIngredients.add(new Ingredient("Black-eyed peas", 100, "g", "Protein"));
        alooTamaIngredients.add(new Ingredient("Turmeric", 1, "tsp", "Spices"));
        alooTamaIngredients.add(new Ingredient("Cumin seeds", 1, "tsp", "Spices"));
        alooTamaIngredients.add(new Ingredient("Dried red chilies", 2, "pieces", "Spices"));

        List<String> alooTamaInstructions = new ArrayList<>();
        alooTamaInstructions.add("Soak black-eyed peas overnight");
        alooTamaInstructions.add("Cook black-eyed peas until tender");
        alooTamaInstructions.add("Fry spices in oil");
        alooTamaInstructions.add("Add potatoes and bamboo shoots");
        alooTamaInstructions.add("Simmer until potatoes are cooked");
        alooTamaInstructions.add("Serve hot with rice");

        defaultRecipes.add(new Recipe(
                "Aloo Tama",
                "https://images.unsplash.com/photo-1567337710282-00832b415979",
                "50 mins",
                4.6f,
                alooTamaIngredients,
                alooTamaInstructions
        ));

        // 18. Kwati
        List<Ingredient> kwatiIngredients = new ArrayList<>();
        kwatiIngredients.add(new Ingredient("Mixed beans", 500, "g", "Protein"));
        kwatiIngredients.add(new Ingredient("Onion", 2, "pieces", "Vegetables"));
        kwatiIngredients.add(new Ingredient("Ginger paste", 2, "tbsp", "Spices"));
        kwatiIngredients.add(new Ingredient("Garlic paste", 2, "tbsp", "Spices"));
        kwatiIngredients.add(new Ingredient("Jimbu", 1, "tsp", "Herbs"));
        kwatiIngredients.add(new Ingredient("Turmeric", 1, "tsp", "Spices"));

        List<String> kwatiInstructions = new ArrayList<>();
        kwatiInstructions.add("Soak mixed beans overnight");
        kwatiInstructions.add("Pressure cook beans until soft");
        kwatiInstructions.add("Prepare tempering with spices");
        kwatiInstructions.add("Add beans and simmer");
        kwatiInstructions.add("Season with jimbu");
        kwatiInstructions.add("Garnish with cilantro");

        defaultRecipes.add(new Recipe(
                "Kwati",
                "https://images.unsplash.com/photo-1546549032-9571cd6b27df",
                "55 mins",
                4.7f,
                kwatiIngredients,
                kwatiInstructions
        ));

        // 19. Chicken Biryani
        List<Ingredient> biryaniIngredients = new ArrayList<>();
        biryaniIngredients.add(new Ingredient("Basmati rice", 400, "g", "Grains"));
        biryaniIngredients.add(new Ingredient("Chicken thighs", 500, "g", "Protein"));
        biryaniIngredients.add(new Ingredient("Yogurt", 200, "g", "Dairy"));
        biryaniIngredients.add(new Ingredient("Onions", 2, "large", "Vegetables"));
        biryaniIngredients.add(new Ingredient("Biryani spices", 3, "tbsp", "Spices"));
        biryaniIngredients.add(new Ingredient("Saffron", 1, "pinch", "Spices"));

        List<String> biryaniInstructions = new ArrayList<>();
        biryaniInstructions.add("Marinate chicken in yogurt and spices");
        biryaniInstructions.add("Cook rice with saffron");
        biryaniInstructions.add("Fry onions until golden");
        biryaniInstructions.add("Layer rice and chicken");
        biryaniInstructions.add("Steam for 20 minutes");

        defaultRecipes.add(new Recipe(
                "Chicken Biryani",
                "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8",
                "60 mins",
                4.9f,
                biryaniIngredients,
                biryaniInstructions
        ));

        // 20. Sushi Rolls
        List<Ingredient> sushiIngredients = new ArrayList<>();
        sushiIngredients.add(new Ingredient("Sushi rice", 300, "g", "Grains"));
        sushiIngredients.add(new Ingredient("Nori sheets", 4, "pieces", "Vegetables"));
        sushiIngredients.add(new Ingredient("Cucumber", 1, "piece", "Vegetables"));
        sushiIngredients.add(new Ingredient("Avocado", 1, "piece", "Vegetables"));
        sushiIngredients.add(new Ingredient("Salmon", 200, "g", "Protein"));
        sushiIngredients.add(new Ingredient("Rice vinegar", 3, "tbsp", "Condiments"));

        List<String> sushiInstructions = new ArrayList<>();
        sushiInstructions.add("Cook and season rice");
        sushiInstructions.add("Prepare fillings");
        sushiInstructions.add("Place nori on bamboo mat");
        sushiInstructions.add("Spread rice and add fillings");
        sushiInstructions.add("Roll tightly");
        sushiInstructions.add("Slice into pieces");

        defaultRecipes.add(new Recipe(
                "Sushi Rolls",
                "https://images.unsplash.com/photo-1579871494447-9811cf80d66c",
                "45 mins",
                4.5f,
                sushiIngredients,
                sushiInstructions
        ));

        // 21. French Onion Soup
        List<Ingredient> onionSoupIngredients = new ArrayList<>();
        onionSoupIngredients.add(new Ingredient("Onions", 6, "large", "Vegetables"));
        onionSoupIngredients.add(new Ingredient("Beef broth", 1.5, "L", "Condiments"));
        onionSoupIngredients.add(new Ingredient("Baguette", 1, "piece", "Grains"));
        onionSoupIngredients.add(new Ingredient("Gruyere cheese", 200, "g", "Dairy"));
        onionSoupIngredients.add(new Ingredient("Butter", 50, "g", "Dairy"));
        onionSoupIngredients.add(new Ingredient("Thyme", 4, "sprigs", "Herbs"));

        List<String> onionSoupInstructions = new ArrayList<>();
        onionSoupInstructions.add("Slowly caramelize onions");
        onionSoupInstructions.add("Add broth and thyme");
        onionSoupInstructions.add("Simmer for 30 minutes");
        onionSoupInstructions.add("Top with bread and cheese");
        onionSoupInstructions.add("Broil until cheese melts");

        defaultRecipes.add(new Recipe(
                "French Onion Soup",
                "https://images.unsplash.com/photo-1547592180-85f173990554",
                "65 mins",
                4.6f,
                onionSoupIngredients,
                onionSoupInstructions
        ));

        // 22. Lemon Cheesecake
        List<Ingredient> cheesecakeIngredients = new ArrayList<>();
        cheesecakeIngredients.add(new Ingredient("Cream cheese", 750, "g", "Dairy"));
        cheesecakeIngredients.add(new Ingredient("Graham crackers", 200, "g", "Grains"));
        cheesecakeIngredients.add(new Ingredient("Butter", 100, "g", "Dairy"));
        cheesecakeIngredients.add(new Ingredient("Sugar", 200, "g", "Sweets"));
        cheesecakeIngredients.add(new Ingredient("Eggs", 3, "large", "Protein"));
        cheesecakeIngredients.add(new Ingredient("Lemons", 2, "pieces", "Fruits"));

        List<String> cheesecakeInstructions = new ArrayList<>();
        cheesecakeInstructions.add("Make graham cracker crust");
        cheesecakeInstructions.add("Beat cream cheese and sugar");
        cheesecakeInstructions.add("Add eggs and lemon");
        cheesecakeInstructions.add("Pour into crust");
        cheesecakeInstructions.add("Bake in water bath");
        cheesecakeInstructions.add("Chill overnight");

        defaultRecipes.add(new Recipe(
                "Lemon Cheesecake",
                "https://images.unsplash.com/photo-1524351199678-941a58a3df50",
                "90 mins",
                4.8f,
                cheesecakeIngredients,
                cheesecakeInstructions
        ));

        // Add new breakfast recipes:

        // Masala Dosa
        List<Ingredient> dosaIngredients = new ArrayList<>();
        dosaIngredients.add(new Ingredient("Rice", 2, "cups", "Grains"));
        dosaIngredients.add(new Ingredient("Urad dal", 1, "cup", "Protein"));
        dosaIngredients.add(new Ingredient("Potatoes", 3, "medium", "Vegetables"));
        dosaIngredients.add(new Ingredient("Onions", 2, "medium", "Vegetables"));
        dosaIngredients.add(new Ingredient("Mustard seeds", 1, "tsp", "Spices"));
        dosaIngredients.add(new Ingredient("Curry leaves", 10, "pieces", "Herbs"));
        dosaIngredients.add(new Ingredient("Green chilies", 2, "pieces", "Spices"));

        List<String> dosaInstructions = new ArrayList<>();
        dosaInstructions.add("Soak rice and dal separately for 6 hours");
        dosaInstructions.add("Grind into smooth batter");
        dosaInstructions.add("Ferment overnight");
        dosaInstructions.add("Prepare potato filling");
        dosaInstructions.add("Spread batter on hot griddle");
        dosaInstructions.add("Add filling and fold");

        defaultRecipes.add(new Recipe(
                "Masala Dosa",
                "https://images.unsplash.com/photo-1589301760014-d929f3979dbc",
                "30 mins",
                4.7f,
                dosaIngredients,
                dosaInstructions
        ));

        // Eggs Benedict
        List<Ingredient> benedictIngredients = new ArrayList<>();
        benedictIngredients.add(new Ingredient("English muffins", 2, "pieces", "Grains"));
        benedictIngredients.add(new Ingredient("Eggs", 4, "large", "Protein"));
        benedictIngredients.add(new Ingredient("Canadian bacon", 4, "slices", "Protein"));
        benedictIngredients.add(new Ingredient("Butter", 100, "g", "Dairy"));
        benedictIngredients.add(new Ingredient("Egg yolks", 3, "pieces", "Protein"));
        benedictIngredients.add(new Ingredient("Lemon juice", 1, "tbsp", "Condiments"));

        List<String> benedictInstructions = new ArrayList<>();
        benedictInstructions.add("Make hollandaise sauce");
        benedictInstructions.add("Toast English muffins");
        benedictInstructions.add("Cook Canadian bacon");
        benedictInstructions.add("Poach eggs");
        benedictInstructions.add("Assemble and top with sauce");

        defaultRecipes.add(new Recipe(
                "Eggs Benedict",
                "https://images.unsplash.com/photo-1608039829572-78524f79c4c7",
                "25 mins",
                4.8f,
                benedictIngredients,
                benedictInstructions
        ));

        // Shakshuka
        List<Ingredient> shakshukaIngredients = new ArrayList<>();
        shakshukaIngredients.add(new Ingredient("Eggs", 6, "large", "Protein"));
        shakshukaIngredients.add(new Ingredient("Tomatoes", 6, "large", "Vegetables"));
        shakshukaIngredients.add(new Ingredient("Bell peppers", 2, "medium", "Vegetables"));
        shakshukaIngredients.add(new Ingredient("Onion", 1, "large", "Vegetables"));
        shakshukaIngredients.add(new Ingredient("Garlic", 4, "cloves", "Spices"));
        shakshukaIngredients.add(new Ingredient("Cumin", 1, "tsp", "Spices"));
        shakshukaIngredients.add(new Ingredient("Paprika", 1, "tsp", "Spices"));

        List<String> shakshukaInstructions = new ArrayList<>();
        shakshukaInstructions.add("Sauté onions and peppers");
        shakshukaInstructions.add("Add tomatoes and spices");
        shakshukaInstructions.add("Simmer until sauce thickens");
        shakshukaInstructions.add("Create wells and add eggs");
        shakshukaInstructions.add("Cover and cook eggs");
        shakshukaInstructions.add("Garnish with herbs");

        defaultRecipes.add(new Recipe(
                "Shakshuka",
                "https://images.unsplash.com/photo-1590412200988-a436970781fa",
                "30 mins",
                4.6f,
                shakshukaIngredients,
                shakshukaInstructions
        ));

        // French Toast
        List<Ingredient> frenchToastIngredients = new ArrayList<>();
        frenchToastIngredients.add(new Ingredient("Bread", 8, "slices", "Grains"));
        frenchToastIngredients.add(new Ingredient("Eggs", 4, "large", "Protein"));
        frenchToastIngredients.add(new Ingredient("Milk", 1, "cup", "Dairy"));
        frenchToastIngredients.add(new Ingredient("Vanilla extract", 1, "tsp", "Condiments"));
        frenchToastIngredients.add(new Ingredient("Cinnamon", 1, "tsp", "Spices"));
        frenchToastIngredients.add(new Ingredient("Maple syrup", 1/2, "cup", "Condiments"));

        List<String> frenchToastInstructions = new ArrayList<>();
        frenchToastInstructions.add("Whisk eggs, milk, and spices");
        frenchToastInstructions.add("Dip bread in mixture");
        frenchToastInstructions.add("Heat butter in pan");
        frenchToastInstructions.add("Cook until golden");
        frenchToastInstructions.add("Serve with maple syrup");

        defaultRecipes.add(new Recipe(
                "French Toast",
                "https://images.unsplash.com/photo-1484723091739-30a097e8f929",
                "20 mins",
                4.5f,
                frenchToastIngredients,
                frenchToastInstructions
        ));

        // Breakfast Burrito
        List<Ingredient> burritoIngredients = new ArrayList<>();
        burritoIngredients.add(new Ingredient("Tortillas", 4, "large", "Grains"));
        burritoIngredients.add(new Ingredient("Eggs", 6, "large", "Protein"));
        burritoIngredients.add(new Ingredient("Potatoes", 2, "medium", "Vegetables"));
        burritoIngredients.add(new Ingredient("Bell peppers", 1, "medium", "Vegetables"));
        burritoIngredients.add(new Ingredient("Cheese", 1, "cup", "Dairy"));
        burritoIngredients.add(new Ingredient("Salsa", 1/2, "cup", "Condiments"));

        List<String> burritoInstructions = new ArrayList<>();
        burritoInstructions.add("Dice and cook potatoes");
        burritoInstructions.add("Scramble eggs");
        burritoInstructions.add("Sauté peppers");
        burritoInstructions.add("Warm tortillas");
        burritoInstructions.add("Assemble with cheese");
        burritoInstructions.add("Roll and grill seam-side down");

        defaultRecipes.add(new Recipe(
                "Breakfast Burrito",
                "https://images.unsplash.com/photo-1626700051175-6818013e1d4f",
                "25 mins",
                4.6f,
                burritoIngredients,
                burritoInstructions
        ));

        // Add new tea recipes
        // Masala Chai
        List<Ingredient> masalaChaiIngredients = new ArrayList<>();
        masalaChaiIngredients.add(new Ingredient("Black tea leaves", 2, "tbsp", "Tea"));
        masalaChaiIngredients.add(new Ingredient("Water", 2, "cups", "Liquid"));
        masalaChaiIngredients.add(new Ingredient("Milk", 1, "cup", "Dairy"));
        masalaChaiIngredients.add(new Ingredient("Cardamom", 4, "pods", "Spices"));
        masalaChaiIngredients.add(new Ingredient("Cinnamon", 1, "stick", "Spices"));
        masalaChaiIngredients.add(new Ingredient("Ginger", 1, "inch", "Spices"));
        masalaChaiIngredients.add(new Ingredient("Black peppercorns", 4, "pieces", "Spices"));
        masalaChaiIngredients.add(new Ingredient("Sugar", 2, "tbsp", "Sweets"));

        List<String> masalaChaiInstructions = new ArrayList<>();
        masalaChaiInstructions.add("Crush spices lightly");
        masalaChaiInstructions.add("Boil water with spices");
        masalaChaiInstructions.add("Add tea leaves and simmer");
        masalaChaiInstructions.add("Add milk and bring to boil");
        masalaChaiInstructions.add("Strain and serve hot");

        defaultRecipes.add(new Recipe(
                "Masala Chai",
                "https://www.teaforturmeric.com/wp-content/uploads/2021/11/Masala-Chai-Tea-9-1024x1536.jpg",
                "10 mins",
                4.9f,
                masalaChaiIngredients,
                masalaChaiInstructions
        ));

        // Green Tea
        List<Ingredient> greenTeaIngredients = new ArrayList<>();
        greenTeaIngredients.add(new Ingredient("Green tea leaves", 1, "tsp", "Tea"));
        greenTeaIngredients.add(new Ingredient("Water", 1, "cup", "Liquid"));
        greenTeaIngredients.add(new Ingredient("Honey", 1, "tsp", "Condiments"));
        greenTeaIngredients.add(new Ingredient("Lemon", 1, "slice", "Fruits"));

        List<String> greenTeaInstructions = new ArrayList<>();
        greenTeaInstructions.add("Heat water to 80°C");
        greenTeaInstructions.add("Add tea leaves");
        greenTeaInstructions.add("Steep for 2-3 minutes");
        greenTeaInstructions.add("Strain into cup");
        greenTeaInstructions.add("Add honey and lemon if desired");

        defaultRecipes.add(new Recipe(
                "Green Tea",
                "https://images.unsplash.com/photo-1627435601361-ec25f5b1d0e5",
                "5 mins",
                4.5f,
                greenTeaIngredients,
                greenTeaInstructions
        ));

        // Bubble Tea
        List<Ingredient> bubbleTeaIngredients = new ArrayList<>();
        bubbleTeaIngredients.add(new Ingredient("Black tea", 2, "bags", "Tea"));
        bubbleTeaIngredients.add(new Ingredient("Tapioca pearls", 1/2, "cup", "Grains"));
        bubbleTeaIngredients.add(new Ingredient("Milk", 1/2, "cup", "Dairy"));
        bubbleTeaIngredients.add(new Ingredient("Brown sugar", 2, "tbsp", "Sweets"));
        bubbleTeaIngredients.add(new Ingredient("Ice cubes", 1, "cup", "Other"));

        List<String> bubbleTeaInstructions = new ArrayList<>();
        bubbleTeaInstructions.add("Cook tapioca pearls until soft");
        bubbleTeaInstructions.add("Prepare strong black tea");
        bubbleTeaInstructions.add("Make brown sugar syrup");
        bubbleTeaInstructions.add("Combine tea with milk");
        bubbleTeaInstructions.add("Add tapioca pearls");
        bubbleTeaInstructions.add("Serve over ice");

        defaultRecipes.add(new Recipe(
                "Bubble Tea",
                "https://assets.epicurious.com/photos/5953ca064919e41593325d97/1:1/w_1920,c_limit/bubble_tea_recipe_062817.jpg",
                "15 mins",
                4.7f,
                bubbleTeaIngredients,
                bubbleTeaInstructions
        ));

        // Add all default recipes to the allRecipes list
        // But avoid duplicates by checking titles
        for (Recipe defaultRecipe : defaultRecipes) {
            boolean exists = false;
            for (Recipe existingRecipe : allRecipes) {
                if (existingRecipe.getTitle() != null && 
                    existingRecipe.getTitle().equals(defaultRecipe.getTitle())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                allRecipes.add(defaultRecipe);
            }
        }

        // After adding all default recipes, save them to Firebase Realtime Database if they don't exist yet
        for (Recipe recipe : defaultRecipes) {
            // Only save recipes that don't have an ID yet (new default recipes)
            if (recipe.getId() == null || recipe.getId().isEmpty()) {
                saveRecipeToDatabase(recipe);
            }
        }
        
        // Update the RecyclerView
        if (recipeAdapter != null) {
            recipeAdapter.updateRecipes(allRecipes);
        }
        
        hideLoading();
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recipesRecyclerView.setLayoutManager(layoutManager);
        
        recipeAdapter = new RecipeAdapter(allRecipes, this);
        recipesRecyclerView.setAdapter(recipeAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecipes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryFilter() {
        String[] categories = {"All", "Breakfast", "Lunch", "Dinner", "Vegetarian", "Quick Meals", "Desserts"};
        
        for (String category : categories) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setClickable(true);
            categoryChipGroup.addView(chip);
            
            if (category.equals("All")) {
                chip.setChecked(true);
            }
        }

        categoryChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                // If no chip is selected, select "All"
                ((Chip) group.getChildAt(0)).setChecked(true);
                return;
            }

            Chip selectedChip = group.findViewById(checkedId);
            String category = selectedChip.getText().toString();
            filterRecipesByCategory(category);
        });
    }

    private void launchAddRecipeActivity() {
        Intent intent = new Intent(this, AddRecipeActivity.class);
        addRecipeLauncher.launch(intent);
    }

    private void addRecipeToList(Recipe newRecipe) {
        allRecipes.add(newRecipe);
        recipeAdapter.updateRecipes(allRecipes);
        Toast.makeText(this, "Recipe added successfully", Toast.LENGTH_SHORT).show();
    }

    private void filterRecipesByCategory(String category) {
        if (category.equals("All")) {
            recipeAdapter.updateRecipes(allRecipes);
            return;
        }

        List<Recipe> filteredList = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            boolean matches = false;
            switch (category) {
                case "Breakfast":
                    matches = recipe.getTitle().toLowerCase().contains("smoothie") ||
                            recipe.getTitle().toLowerCase().contains("chai") ||
                            recipe.getTitle().toLowerCase().contains("breakfast") ||
                            recipe.getTitle().toLowerCase().contains("toast") ||
                            recipe.getTitle().toLowerCase().contains("dosa") ||
                            recipe.getTitle().toLowerCase().contains("tea") ||
                            recipe.getTitle().toLowerCase().contains("eggs") ||
                            recipe.getTitle().toLowerCase().contains("shakshuka");
                    break;
                case "Lunch":
                    matches = recipe.getTitle().toLowerCase().contains("salad") ||
                            recipe.getTitle().toLowerCase().contains("bowl") ||
                            recipe.getTitle().toLowerCase().contains("sandwich") ||
                            recipe.getTitle().toLowerCase().contains("momo") ||
                            recipe.getTitle().toLowerCase().contains("dal") ||
                            recipe.getTitle().toLowerCase().contains("curry") ||
                            recipe.getTitle().toLowerCase().contains("pad thai") ||
                            recipe.getTitle().toLowerCase().contains("pizza");
                    break;
                case "Dinner":
                    matches = recipe.getTitle().toLowerCase().contains("curry") ||
                            recipe.getTitle().toLowerCase().contains("stir-fry") ||
                            recipe.getTitle().toLowerCase().contains("risotto") ||
                            recipe.getTitle().toLowerCase().contains("biryani") ||
                            recipe.getTitle().toLowerCase().contains("carbonara") ||
                            recipe.getTitle().toLowerCase().contains("aloo") ||
                            recipe.getTitle().toLowerCase().contains("pizza") ||
                            recipe.getTitle().toLowerCase().contains("soup");
                    break;
                case "Vegetarian":
                    matches = !recipe.getIngredients().stream()
                            .anyMatch(ingredient -> {
                                String name = ingredient.getName().toLowerCase();
                                return ingredient.getCategory().equals("Protein") &&
                                       (name.contains("chicken") ||
                                        name.contains("beef") ||
                                        name.contains("fish") ||
                                        name.contains("salmon") ||
                                        name.contains("bacon") ||
                                        name.contains("ham") ||
                                        name.contains("shrimp") ||
                                        name.contains("prawn"));
                            }) &&
                            (recipe.getTitle().toLowerCase().contains("vegetable") ||
                             recipe.getTitle().toLowerCase().contains("mushroom") ||
                             recipe.getTitle().toLowerCase().contains("dal") ||
                             recipe.getTitle().toLowerCase().contains("momo") ||
                             recipe.getTitle().toLowerCase().contains("aloo") ||
                             recipe.getTitle().toLowerCase().contains("quinoa") ||
                             recipe.getTitle().toLowerCase().contains("tofu") ||
                             recipe.getTitle().toLowerCase().contains("salad"));
                    break;
                case "Quick Meals":
                    int cookingTime = Integer.parseInt(recipe.getDuration().replaceAll("[^0-9]", ""));
                    matches = cookingTime <= 20;
                    break;
                case "Desserts":
                    matches = recipe.getTitle().toLowerCase().contains("brownie") ||
                            recipe.getTitle().toLowerCase().contains("cake") ||
                            recipe.getTitle().toLowerCase().contains("sweet") ||
                            recipe.getIngredients().stream()
                                    .anyMatch(ingredient -> 
                                        ingredient.getCategory().equals("Sweets"));
                    break;
            }
            if (matches) {
                filteredList.add(recipe);
            }
        }
        recipeAdapter.updateRecipes(filteredList);
    }

    private void filterRecipes(String query) {
        // Get currently selected category
        int checkedId = categoryChipGroup.getCheckedChipId();
        String category = "All";
        if (checkedId != View.NO_ID) {
            Chip selectedChip = categoryChipGroup.findViewById(checkedId);
            category = selectedChip.getText().toString();
        }

        List<Recipe> categoryFiltered = new ArrayList<>();
        if (category.equals("All")) {
            categoryFiltered.addAll(allRecipes);
        } else {
            for (Recipe recipe : allRecipes) {
                // Reuse existing category filtering logic
                boolean matches = false;
                switch (category) {
                    case "Breakfast":
                        matches = recipe.getTitle().toLowerCase().contains("smoothie") ||
                                recipe.getTitle().toLowerCase().contains("breakfast");
                        break;
                    // ... (rest of the category cases)
                }
                if (matches) {
                    categoryFiltered.add(recipe);
                }
            }
        }

        // Apply search filter on top of category filter
        List<Recipe> searchFiltered = new ArrayList<>();
        for (Recipe recipe : categoryFiltered) {
            if (recipe.getTitle().toLowerCase().contains(query.toLowerCase())) {
                searchFiltered.add(recipe);
            }
        }
        recipeAdapter.updateRecipes(searchFiltered);
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        startActivity(RecipeDetailActivity.newIntent(this, recipe));
    }

    @Override
    public void onAddToMealPlanClick(Recipe recipe) {
        MealPlanDialog dialog = new MealPlanDialog(this, recipe, this);
        dialog.show();
    }

    @Override
    public void onMealPlanSelected(Recipe recipe, Date date, String mealTime) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add meal plans", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();
        MealPlan mealPlan = new MealPlan(currentUser.getUid(), recipe, date, mealTime);

        mealPlanRepository.addMealPlan(mealPlan)
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    String message = String.format("Added %s to %s for %s", 
                            recipe.getTitle(), 
                            mealTime.toLowerCase(), 
                            new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date));
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    Toast.makeText(this, "Failed to add to meal plan: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLoading() {
        loadingView.setVisibility(View.VISIBLE);
        recipesRecyclerView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
        recipesRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        TextView errorText = errorView.findViewById(R.id.errorText);
        errorText.setText(message);
        errorView.setVisibility(View.VISIBLE);
        recipesRecyclerView.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
    }

    private void hideError() {
        errorView.setVisibility(View.GONE);
        recipesRecyclerView.setVisibility(View.VISIBLE);
    }
}
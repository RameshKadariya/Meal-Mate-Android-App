package com.raka.mealmate;

import android.app.ActivityOptions;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.raka.mealmate.adapters.FeaturedRecipeAdapter;
import com.raka.mealmate.models.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private VideoView recipeVideoBackground;
    private TextView timeBasedGreeting;
    private TextView userFirstName;
    private TextView weatherInfo;
    private ShapeableImageView profileImage;
    private TextView nutritionTipText;
    private Button refreshNutritionTip;
    private ViewPager2 recipeShowcaseViewPager;
    
    private List<Integer> videoResourceIds = new ArrayList<>();
    private int currentVideoIndex = 0;
    private List<Recipe> recipeList = new ArrayList<>();
    private FeaturedRecipeAdapter recipeAdapter;
    private Handler recipeSwapHandler = new Handler();
    private Runnable recipeSwapRunnable;
    private int currentRecipeIndex = 0;
    
    // Variables for orbit animation
    private Handler orbitAnimationHandler = new Handler();
    private Runnable orbitAnimationRunnable;
    private float orbitAngle = 0f;
    private boolean isOrbitAnimationRunning = false;
    private MaterialCardView centerFeature, topFeature, bottomFeature, leftFeature, rightFeature;
    private float orbitRadius;
    
    // Original positions of feature items
    private float[] topFeaturePos = new float[2];
    private float[] rightFeaturePos = new float[2];
    private float[] bottomFeaturePos = new float[2];
    private float[] leftFeaturePos = new float[2];
    
    // Nutrition tips array
    private String[] nutritionTips = {
        "Eating a variety of colorful fruits and vegetables ensures you get a range of nutrients.",
        "Drink water before meals to help control your appetite and stay hydrated.",
        "Include protein with every meal to help maintain muscle and feel fuller longer.",
        "Choose whole grains over refined grains for more fiber and nutrients.",
        "Healthy fats from avocados, nuts, and olive oil support brain health.",
        "Fermented foods like yogurt and kimchi support gut health.",
        "Limit added sugars - they provide calories with little nutritional value.",
        "Meal planning can help you eat healthier and reduce food waste.",
        "Herbs and spices add flavor without sodium or calories.",
        "Eating slowly helps you enjoy your food and recognize when you're full."
    };

    // Add constants for recipe showcase
    private static final int RECIPE_AUTO_SCROLL_DELAY = 5000; // 5 seconds
    private static final int RECIPE_ANIMATION_DURATION = 800;
    private static final float MIN_SCALE = 0.85f;
    private static final float MIN_ALPHA = 0.5f;
    
    // Add variables for recipe loading state
    private boolean isLoadingRecipes = false;
    private DatabaseReference recipesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupGreetingSection();
        setupVideoBackground();
        setupFeatureClicks();
        setupNutritionInsight();
        setupRecipeShowcase();
        loadPremiumRecipes();
        
        // Start the orbit animation after layout is measured
        centerFeature.post(() -> startOrbitAnimation());
    }
    
    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        recipeVideoBackground = findViewById(R.id.recipeVideoBackground);
        timeBasedGreeting = findViewById(R.id.timeBasedGreeting);
        userFirstName = findViewById(R.id.userFirstName);
        weatherInfo = findViewById(R.id.weatherInfo);
        profileImage = findViewById(R.id.profileImage);
        nutritionTipText = findViewById(R.id.nutritionTipText);
        refreshNutritionTip = findViewById(R.id.refreshNutritionTip);
        recipeShowcaseViewPager = findViewById(R.id.recipeShowcaseViewPager);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
    
    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, 
                findViewById(R.id.toolbar), R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }
    
    private void setupGreetingSection() {
        // Set time-based greeting
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        
        String greeting;
        if (hourOfDay >= 5 && hourOfDay < 12) {
            greeting = "Good Morning";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }
        
        timeBasedGreeting.setText(greeting);
        
        // Set user first name
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                String firstName = name.split(" ")[0];
                userFirstName.setText(firstName + "!");
            } else {
                userFirstName.setText("Friend!");
            }
            
            // Load profile image if available
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.profile_placeholder)
                        .into(profileImage);
            }
        } else {
            userFirstName.setText("Friend!");
        }
        
        // Set mock weather for demonstration
        weatherInfo.setText("28°C • Kathmandu");
    }
    
    private void setupVideoBackground() {
        // Add video resource IDs
        videoResourceIds.add(R.raw.v1);
        videoResourceIds.add(R.raw.v2);
        videoResourceIds.add(R.raw.v3);
        videoResourceIds.add(R.raw.v4);
        
        // Shuffle videos for random order
        Collections.shuffle(videoResourceIds);
        
        // Start playing videos
        playNextVideo();
        
        // Set audio to mute
        recipeVideoBackground.setOnPreparedListener(mp -> {
            mp.setVolume(0f, 0f);
            mp.setLooping(false);
        });
        
        // Set completion listener to play next video
        recipeVideoBackground.setOnCompletionListener(mp -> {
            currentVideoIndex = (currentVideoIndex + 1) % videoResourceIds.size();
            
            // Reshuffle if we've gone through all videos
            if (currentVideoIndex == 0) {
                Collections.shuffle(videoResourceIds);
            }
            
            playNextVideo();
        });
        
        // Handle errors
        recipeVideoBackground.setOnErrorListener((mp, what, extra) -> {
            // Skip to next video if there's an error
            currentVideoIndex = (currentVideoIndex + 1) % videoResourceIds.size();
            playNextVideo();
            return true;
        });
    }
    
    private void playNextVideo() {
        try {
            String videoPath = "android.resource://" + getPackageName() + "/" + videoResourceIds.get(currentVideoIndex);
            recipeVideoBackground.setVideoURI(Uri.parse(videoPath));
            recipeVideoBackground.start();
        } catch (Exception e) {
            e.printStackTrace();
            // If there's an error, try the next video
            currentVideoIndex = (currentVideoIndex + 1) % videoResourceIds.size();
            playNextVideo();
        }
    }
    
    private void setupFeatureClicks() {
        // Set up click listeners for feature buttons
        centerFeature = findViewById(R.id.centerFeature);
        topFeature = findViewById(R.id.topFeature);
        bottomFeature = findViewById(R.id.bottomFeature);
        leftFeature = findViewById(R.id.leftFeature);
        rightFeature = findViewById(R.id.rightFeature);
        
        centerFeature.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecipeBrowserActivity.class);
            startActivity(intent);
        });
        
        topFeature.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ShoppingListActivity.class);
            startActivity(intent);
        });
        
        bottomFeature.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StoresMapActivity.class);
            startActivity(intent);
        });
        
        leftFeature.setOnClickListener(v -> {
            // Meal planner feature
            Intent intent = new Intent(MainActivity.this, MealPlannerActivity.class);
            startActivity(intent);
        });
        
        rightFeature.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
        
        // Long press on center feature to toggle animation
        centerFeature.setOnLongClickListener(v -> {
            if (isOrbitAnimationRunning) {
                stopOrbitAnimation();
            } else {
                startOrbitAnimation();
            }
            return true;
        });
        
        // Calculate orbit radius after layouts are measured
        centerFeature.post(() -> {
            // Calculate appropriate radius based on layout
            orbitRadius = centerFeature.getWidth() * 1.1f;
            
            // Store original positions to reset if needed
            saveOriginalPositions();
        });
    }
    
    private void showFeatureComingSoon(String featureName) {
        // Show a simple toast that the feature is coming soon
        android.widget.Toast.makeText(this, 
                featureName + " feature coming soon!", 
                android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void setupNutritionInsight() {
        // Display a random nutrition tip
        updateNutritionTip();
        
        // Set up refresh button
        refreshNutritionTip.setOnClickListener(v -> updateNutritionTip());
    }
    
    private void updateNutritionTip() {
        // Display a random nutrition tip
        int randomIndex = new Random().nextInt(nutritionTips.length);
        nutritionTipText.setText(nutritionTips[randomIndex]);
    }
    
    private void setupRecipeShowcase() {
        // Initialize recipe showcase ViewPager
        recipeAdapter = new FeaturedRecipeAdapter(recipeList);
        recipeShowcaseViewPager.setAdapter(recipeAdapter);
        
        // Set offscreen page limit
        recipeShowcaseViewPager.setOffscreenPageLimit(3);
        
        // Add page transformer for scaling and fading effect
        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(40));
        transformer.addTransformer((page, position) -> {
            float absPosition = Math.abs(position);
            // Scale the page down
            float scale = Math.max(MIN_SCALE, 1 - absPosition);
            page.setScaleY(scale);
            page.setScaleX(scale);
            
            // Fade the page
            page.setAlpha(Math.max(MIN_ALPHA, 1 - absPosition));
            
            // Add vertical offset for 3D effect
            float verticalOffset = absPosition * 30;
            page.setTranslationY(verticalOffset);
        });
        
        recipeShowcaseViewPager.setPageTransformer(transformer);
        
        // Set click listener for recipes
        recipeAdapter.setOnItemClickListener(recipe -> {
            Intent intent = new Intent(MainActivity.this, RecipeDetailActivity.class);
            intent.putExtra("RECIPE_ID", recipe.getId());
            
            // Add shared element transition
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                this, 
                recipeShowcaseViewPager, 
                "recipeTransition"
            );
            startActivity(intent, options.toBundle());
        });
        
        // Register page change callback
        recipeShowcaseViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentRecipeIndex = position;
                resetAutoScroll(); // Reset timer when user manually changes page
            }
        });
    }
    
    private void loadPremiumRecipes() {
        if (isLoadingRecipes) return;
        isLoadingRecipes = true;
        
        // Initialize Firebase reference
        recipesRef = FirebaseDatabase.getInstance().getReference("premium_recipes");
        
        recipesRef.orderByChild("rating").limitToLast(10).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Recipe> newRecipes = new ArrayList<>();
                for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                    Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                    if (recipe != null && recipe.getRating() >= 4.5) {
                        newRecipes.add(recipe);
                    }
                }
                
                // Sort by rating in descending order
                Collections.sort(newRecipes, (r1, r2) -> Float.compare(r2.getRating(), r1.getRating()));
                
                // Update UI
                recipeList.clear();
                recipeList.addAll(newRecipes);
                recipeAdapter.notifyDataSetChanged();
                
                // Start auto-scrolling if we have recipes
                if (!recipeList.isEmpty()) {
                    startAutoScroll();
                } else {
                    // If no premium recipes found, add sample ones
                    addSampleRecipes();
                }
                
                isLoadingRecipes = false;
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoadingRecipes = false;
                addSampleRecipes(); // Fallback to sample recipes
            }
        });
    }
    
    private void startAutoScroll() {
        if (recipeSwapRunnable != null) {
            recipeSwapHandler.removeCallbacks(recipeSwapRunnable);
        }
        
        recipeSwapRunnable = new Runnable() {
            @Override
            public void run() {
                if (recipeList.size() > 1) {
                    currentRecipeIndex = (currentRecipeIndex + 1) % recipeList.size();
                    recipeShowcaseViewPager.setCurrentItem(currentRecipeIndex, true);
                }
                recipeSwapHandler.postDelayed(this, RECIPE_AUTO_SCROLL_DELAY);
            }
        };
        
        recipeSwapHandler.postDelayed(recipeSwapRunnable, RECIPE_AUTO_SCROLL_DELAY);
    }
    
    private void resetAutoScroll() {
        if (recipeSwapRunnable != null) {
            recipeSwapHandler.removeCallbacks(recipeSwapRunnable);
            startAutoScroll();
        }
    }
    
    private void stopAutoScroll() {
        if (recipeSwapRunnable != null) {
            recipeSwapHandler.removeCallbacks(recipeSwapRunnable);
        }
    }
    
    public void onFeatureClick(View view) {
        // This method is referenced in the layout XML for the feature cards
        // The individual click handlers are set in setupFeatureClicks()
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (recipeVideoBackground != null && !recipeVideoBackground.isPlaying()) {
            recipeVideoBackground.start();
        }
        
        // Resume auto-scrolling recipes
        startAutoScroll();
        
        // Resume orbit animation
        startOrbitAnimation();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (recipeVideoBackground != null && recipeVideoBackground.isPlaying()) {
            recipeVideoBackground.pause();
        }
        
        // Stop auto-scrolling recipes
        stopAutoScroll();
        
        // Pause orbit animation
        stopOrbitAnimation();
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            // Already on home screen, do nothing
        } else if (id == R.id.nav_recipes) {
            startActivity(new Intent(this, RecipeBrowserActivity.class));
        } else if (id == R.id.nav_meal_planner) {
           startActivity(new Intent(this, MealPlannerActivity.class));
        } else if (id == R.id.nav_shopping) {
            startActivity(new Intent(this, ShoppingListActivity.class));
        } else if (id == R.id.nav_stores) {
            startActivity(new Intent(this, StoresMapActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Save the original positions of all feature items
     */
    private void saveOriginalPositions() {
        topFeaturePos[0] = topFeature.getX();
        topFeaturePos[1] = topFeature.getY();
        
        rightFeaturePos[0] = rightFeature.getX();
        rightFeaturePos[1] = rightFeature.getY();
        
        bottomFeaturePos[0] = bottomFeature.getX();
        bottomFeaturePos[1] = bottomFeature.getY();
        
        leftFeaturePos[0] = leftFeature.getX();
        leftFeaturePos[1] = leftFeature.getY();
    }
    
    /**
     * Starts the orbit animation that makes feature buttons revolve around the center feature
     */
    private void startOrbitAnimation() {
        if (isOrbitAnimationRunning || centerFeature == null) return;
        
        orbitAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                // Update the orbit angle
                orbitAngle += 0.5f;
                // Adjust speed here - smaller value for slower rotation
                if (orbitAngle >= 360f) orbitAngle = 0f;
                
                // Calculate positions for each feature
                updateOrbitPositions();
                
                // Schedule the next animation frame
                orbitAnimationHandler.postDelayed(this, 50); // 20 frames per second
            }
        };
        
        // Start the animation
        orbitAnimationHandler.post(orbitAnimationRunnable);
        isOrbitAnimationRunning = true;
    }
    
    /**
     * Stops the orbit animation
     */
    private void stopOrbitAnimation() {
        if (orbitAnimationHandler != null && orbitAnimationRunnable != null) {
            orbitAnimationHandler.removeCallbacks(orbitAnimationRunnable);
            isOrbitAnimationRunning = false;
            
            // Reset to original positions
            resetFeaturePositions();
        }
    }
    
    /**
     * Resets features to their original positions
     */
    private void resetFeaturePositions() {
        topFeature.animate().x(topFeaturePos[0]).y(topFeaturePos[1]).setDuration(300).start();
        rightFeature.animate().x(rightFeaturePos[0]).y(rightFeaturePos[1]).setDuration(300).start();
        bottomFeature.animate().x(bottomFeaturePos[0]).y(bottomFeaturePos[1]).setDuration(300).start();
        leftFeature.animate().x(leftFeaturePos[0]).y(leftFeaturePos[1]).setDuration(300).start();
    }
    
    /**
     * Updates the positions of features based on the current orbit angle
     */
    private void updateOrbitPositions() {
        // Get center coordinates (center of centerFeature)
        float centerX = centerFeature.getX() + centerFeature.getWidth() / 2;
        float centerY = centerFeature.getY() + centerFeature.getHeight() / 2;
        
        // Position top feature (starts at top - 0 degrees in unit circle)
        float topAngle = (float) Math.toRadians(orbitAngle);
        positionFeature(topFeature, centerX, centerY, orbitRadius, topAngle);
        
        // Position right feature (starts at right - 90 degrees in unit circle)
        float rightAngle = (float) Math.toRadians(orbitAngle + 90);
        positionFeature(rightFeature, centerX, centerY, orbitRadius, rightAngle);
        
        // Position bottom feature (starts at bottom - 180 degrees in unit circle)
        float bottomAngle = (float) Math.toRadians(orbitAngle + 180);
        positionFeature(bottomFeature, centerX, centerY, orbitRadius, bottomAngle);
        
        // Position left feature (starts at left - 270 degrees in unit circle)
        float leftAngle = (float) Math.toRadians(orbitAngle + 270);
        positionFeature(leftFeature, centerX, centerY, orbitRadius, leftAngle);
    }
    
    /**
     * Positions a single feature at the calculated position based on angle and radius
     */
    private void positionFeature(View feature, float centerX, float centerY, float radius, float angle) {
        // Calculate new position
        float featureWidth = feature.getWidth();
        float featureHeight = feature.getHeight();
        
        float newX = (float) (centerX + radius * Math.sin(angle) - featureWidth / 2);
        float newY = (float) (centerY - radius * Math.cos(angle) - featureHeight / 2);
        
        // Apply translation animation
        feature.animate()
               .x(newX)
               .y(newY)
               .setDuration(0)
               .start();
    }
    
    private void addSampleRecipes() {
        // Add high-rated premium recipe samples
        recipeList.add(new Recipe(
            "1",
            "Truffle Mushroom Risotto",
            "Dinner",
            "45 minutes",
            4.9f,
            "https://images.unsplash.com/photo-1476124369491-e7addf5db371?auto=format&fit=crop&w=800&q=60"
        ));
        
        recipeList.add(new Recipe(
            "2",
            "Seared Wagyu Steak",
            "Dinner",
            "35 minutes",
            4.8f,
            "https://images.unsplash.com/photo-1546964124-0cce460f38ef?auto=format&fit=crop&w=800&q=60"
        ));
        
        recipeList.add(new Recipe(
            "3",
            "Lobster Thermidor",
            "Dinner",
            "50 minutes",
            4.7f,
            "https://images.unsplash.com/photo-1553247407-23251ce81f59?auto=format&fit=crop&w=800&q=60"
        ));
        
        recipeList.add(new Recipe(
            "4",
            "Saffron Seafood Paella",
            "Dinner",
            "55 minutes",
            4.8f,
            "https://images.unsplash.com/photo-1534080564583-6be75777b70a?auto=format&fit=crop&w=800&q=60"
        ));
        
        recipeList.add(new Recipe(
            "5",
            "Duck Confit",
            "Dinner",
            "3 hours",
                4.9f,
            "https://images.unsplash.com/photo-1580476262798-bddd9f4b7369?auto=format&fit=crop&w=800&q=60"
        ));
        
        // Update adapter and start auto-scroll
        recipeAdapter.notifyDataSetChanged();
        startAutoScroll();
    }
}
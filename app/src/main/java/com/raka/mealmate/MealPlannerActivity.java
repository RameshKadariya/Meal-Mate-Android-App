package com.raka.mealmate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.raka.mealmate.adapters.MealAdapter;
import com.raka.mealmate.dialogs.MealPlanDialog;
import com.raka.mealmate.helpers.NotificationHelper;
import com.raka.mealmate.models.MealPlan;
import com.raka.mealmate.models.Recipe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MealPlannerActivity extends AppCompatActivity implements MealAdapter.OnMealClickListener {
    private static final String TAG = "MealPlannerActivity";

    private RecyclerView rvBreakfastMeals;
    private RecyclerView rvLunchMeals;
    private RecyclerView rvDinnerMeals;
    private TextView tvCurrentWeek;
    private TextView tvNoBreakfast;
    private TextView tvNoLunch;
    private TextView tvNoDinner;
    private DatabaseReference mDatabase;
    private String userId;
    private Calendar currentDate;
    private SimpleDateFormat weekFormat;
    private List<MealPlan> breakfastMeals;
    private List<MealPlan> lunchMeals;
    private List<MealPlan> dinnerMeals;
    private MealAdapter breakfastAdapter;
    private MealAdapter lunchAdapter;
    private MealAdapter dinnerAdapter;
    private CalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_planner);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "Initializing MealPlannerActivity with userId: " + userId);

        // Initialize date formats
        currentDate = Calendar.getInstance();
        weekFormat = new SimpleDateFormat("MMM dd-", Locale.getDefault());

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Meal Planner");

        // Initialize views and adapters
        initializeViews();
        setupRecyclerViews();
        updateWeekDisplay();

        // Load meals for current date
        loadMealsForCurrentDate();
    }

    private void initializeViews() {
        tvCurrentWeek = findViewById(R.id.tvCurrentWeek);
        rvBreakfastMeals = findViewById(R.id.rvBreakfastMeals);
        rvLunchMeals = findViewById(R.id.rvLunchMeals);
        rvDinnerMeals = findViewById(R.id.rvDinnerMeals);

        tvNoBreakfast = findViewById(R.id.tvNoBreakfast);
        tvNoLunch = findViewById(R.id.tvNoLunch);
        tvNoDinner = findViewById(R.id.tvNoDinner);

        calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar newDate = Calendar.getInstance();
            newDate.set(year, month, dayOfMonth);
            currentDate = newDate;
            updateWeekDisplay();
            loadMealsForCurrentDate();
        });

        findViewById(R.id.btnPreviousWeek).setOnClickListener(v -> {
            navigateWeek(-7);
            calendarView.setDate(currentDate.getTimeInMillis());
        });

        findViewById(R.id.btnNextWeek).setOnClickListener(v -> {
            navigateWeek(7);
            calendarView.setDate(currentDate.getTimeInMillis());
        });

        FloatingActionButton fabAddMeal = findViewById(R.id.fabAddMeal);
        fabAddMeal.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecipeBrowserActivity.class);
            startActivity(intent);
        });

        FloatingActionButton fabShoppingList = findViewById(R.id.fabShoppingList);
        fabShoppingList.setOnClickListener(v -> {
            Intent intent = new Intent(MealPlannerActivity.this, ShoppingListActivity.class);
            startActivity(intent);
        });

        // Set up long click listener for fab to test notifications
        fabAddMeal.setOnLongClickListener(v -> {
            Toast.makeText(this, "Testing notification system...", Toast.LENGTH_SHORT).show();
            
            // Create a test meal plan
            MealPlan testMealPlan = new MealPlan();
            testMealPlan.setId("test_" + System.currentTimeMillis());
            testMealPlan.setRecipeName("Test Meal");
            testMealPlan.setMealTime("Dinner");
            testMealPlan.setDateTime(new Date());
            
            // Schedule the notification
            NotificationHelper.scheduleMealPrepReminder(this, testMealPlan);
            
            // For debugging, also show the notification directly after 2 seconds
            new Handler().postDelayed(() -> {
                Toast.makeText(this, "Showing direct test notification", Toast.LENGTH_SHORT).show();
                NotificationHelper.showMealPrepNotification(
                    this, "Direct Test Meal", "Lunch", testMealPlan.getId().hashCode() + 1);
            }, 2000);
            
            return true;
        });
    }

    private void setupRecyclerViews() {
        // Initialize lists
        breakfastMeals = new ArrayList<>();
        lunchMeals = new ArrayList<>();
        dinnerMeals = new ArrayList<>();

        // Setup adapters
        breakfastAdapter = new MealAdapter(breakfastMeals, this);
        lunchAdapter = new MealAdapter(lunchMeals, this);
        dinnerAdapter = new MealAdapter(dinnerMeals, this);

        // Setup RecyclerViews
        setupRecyclerView(rvBreakfastMeals, breakfastAdapter);
        setupRecyclerView(rvLunchMeals, lunchAdapter);
        setupRecyclerView(rvDinnerMeals, dinnerAdapter);
    }

    private void setupRecyclerView(RecyclerView recyclerView, MealAdapter adapter) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    private void updateWeekDisplay() {
        Calendar endOfWeek = (Calendar) currentDate.clone();
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);

        String weekDisplay = weekFormat.format(currentDate.getTime()) +
                           new SimpleDateFormat("dd", Locale.getDefault()).format(endOfWeek.getTime());
        tvCurrentWeek.setText(weekDisplay);
    }

    private void navigateWeek(int days) {
        currentDate.add(Calendar.DAY_OF_MONTH, days);
        updateWeekDisplay();
        loadMealsForCurrentDate();
    }

    private void loadMealsForCurrentDate() {
        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(currentDate.getTime());

        Log.d(TAG, "Loading meals for date: " + dateKey);
        Log.d(TAG, "Current time millis: " + currentDate.getTimeInMillis());
        Log.d(TAG, "Database path: /mealPlans/" + userId + "/" + dateKey);

        DatabaseReference dateRef = mDatabase.child("mealPlans").child(userId).child(dateKey);

        dateRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Data snapshot received: " + dataSnapshot.toString());
                Log.d(TAG, "Number of meals found: " + dataSnapshot.getChildrenCount());

                breakfastMeals.clear();
                lunchMeals.clear();
                dinnerMeals.clear();

                for (DataSnapshot mealSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Log.d(TAG, "Processing meal snapshot: " + mealSnapshot.toString());

                        MealPlan meal = mealSnapshot.getValue(MealPlan.class);
                        if (meal != null) {
                            meal.setId(mealSnapshot.getKey()); // Ensure ID is set
                            Log.d(TAG, "Meal loaded - Name: " + meal.getRecipeName() +
                                      ", Time: " + meal.getMealTime() +
                                      ", ID: " + meal.getId() +
                                      ", Date: " + meal.getDate());

                            String mealTime = meal.getMealTime().toLowerCase();
                            switch (mealTime) {
                                case "breakfast":
                                    breakfastMeals.add(meal);
                                    Log.d(TAG, "Added to breakfast meals");
                                    break;
                                case "lunch":
                                    lunchMeals.add(meal);
                                    Log.d(TAG, "Added to lunch meals");
                                    break;
                                case "dinner":
                                    dinnerMeals.add(meal);
                                    Log.d(TAG, "Added to dinner meals");
                                    break;
                                default:
                                    Log.w(TAG, "Unknown meal time: " + mealTime);
                                    break;
                            }
                        } else {
                            Log.w(TAG, "Null meal from snapshot: " + mealSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing meal snapshot", e);
                        
                        // Manual parsing as fallback
                        try {
                            String id = mealSnapshot.getKey();
                            String recipeName = mealSnapshot.child("recipeName").getValue(String.class);
                            String mealTime = mealSnapshot.child("mealTime").getValue(String.class);
                            String imageUrl = mealSnapshot.child("imageUrl").getValue(String.class);
                            
                            // Handle dateTime properly
                            Date dateTime;
                            Object dateTimeObj = mealSnapshot.child("dateTime").getValue();
                            if (dateTimeObj instanceof Long) {
                                dateTime = new Date((Long) dateTimeObj);
                            } else {
                                dateTime = new Date(); // Default to current time
                            }
                            
                            // Create meal plan manually
                            MealPlan mealPlan = new MealPlan();
                            mealPlan.setId(id);
                            mealPlan.setRecipeName(recipeName);
                            mealPlan.setMealTime(mealTime);
                            mealPlan.setImageUrl(imageUrl);
                            mealPlan.setDateTime(dateTime);
                            
                            if (mealTime != null) {
                                switch (mealTime.toLowerCase()) {
                                    case "breakfast":
                                        breakfastMeals.add(mealPlan);
                                        Log.d(TAG, "Added to breakfast meals (manual fallback)");
                                        break;
                                    case "lunch":
                                        lunchMeals.add(mealPlan);
                                        Log.d(TAG, "Added to lunch meals (manual fallback)");
                                        break;
                                    case "dinner":
                                        dinnerMeals.add(mealPlan);
                                        Log.d(TAG, "Added to dinner meals (manual fallback)");
                                        break;
                                }
                            }
                        } catch (Exception e2) {
                            Log.e(TAG, "Failed manual fallback parsing", e2);
                        }
                    }
                }

                // Update UI
                updateMealSection(breakfastMeals, breakfastAdapter, tvNoBreakfast, "breakfast");
                updateMealSection(lunchMeals, lunchAdapter, tvNoLunch, "lunch");
                updateMealSection(dinnerMeals, dinnerAdapter, tvNoDinner, "dinner");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(MealPlannerActivity.this,
                             "Failed to load meals: " + databaseError.getMessage(),
                             Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMealSection(List<MealPlan> meals, MealAdapter adapter, TextView noMealsView, String section) {
        Log.d(TAG, section + " meals count: " + meals.size());

        if (meals.isEmpty()) {
            noMealsView.setVisibility(View.VISIBLE);
            noMealsView.setText("No " + section + " meals planned");
        } else {
            noMealsView.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    public void addMealToDate(Recipe recipe, String mealTime) {
        if (recipe == null) {
            Log.e(TAG, "Cannot add a null recipe to mealPlan");
            return;
        }
        
        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(currentDate.getTime());
        
        Log.d(TAG, "Adding meal to date: " + dateKey);
        Log.d(TAG, "Recipe: " + recipe.getTitle() + ", MealTime: " + mealTime);
        
        // Create new MealPlan
        MealPlan mealPlan = new MealPlan(userId, recipe, currentDate.getTime(), mealTime);
        
        // Save to database
        mDatabase.child("mealPlans")
                .child(userId)
                .child(dateKey)
                .child(mealPlan.getId())
                .setValue(mealPlan)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Meal added to plan", Toast.LENGTH_SHORT).show();
                    loadMealsForCurrentDate();
                    
                    // Schedule a reminder notification
                    NotificationHelper.scheduleMealPrepReminder(this, mealPlan);
                    Log.d(TAG, "Scheduled notification for: " + mealPlan.getRecipeName());
                    Log.d(TAG, "Regular meal scheduled - ID: " + mealPlan.getId() 
                            + ", DateTime: " + (mealPlan.getDateTime() != null ? mealPlan.getDateTime().toString() : "null"));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add meal", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error adding meal", e);
                });
    }

    private void refreshMealList(Calendar date) {
        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(date.getTime());

        Log.d(TAG, "Refreshing meals for date: " + dateKey);

        DatabaseReference dateRef = mDatabase.child("mealPlans").child(userId).child(dateKey);

        dateRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Data snapshot received: " + dataSnapshot.toString());
                Log.d(TAG, "Number of meals found: " + dataSnapshot.getChildrenCount());

                breakfastMeals.clear();
                lunchMeals.clear();
                dinnerMeals.clear();

                for (DataSnapshot mealSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Log.d(TAG, "Processing meal snapshot: " + mealSnapshot.toString());

                        MealPlan meal = mealSnapshot.getValue(MealPlan.class);
                        if (meal != null) {
                            meal.setId(mealSnapshot.getKey()); // Ensure ID is set
                            Log.d(TAG, "Meal loaded - Name: " + meal.getRecipeName() +
                                      ", Time: " + meal.getMealTime() +
                                      ", ID: " + meal.getId() +
                                      ", Date: " + meal.getDate());

                            String mealTime = meal.getMealTime().toLowerCase();
                            switch (mealTime) {
                                case "breakfast":
                                    breakfastMeals.add(meal);
                                    Log.d(TAG, "Added to breakfast meals");
                                    break;
                                case "lunch":
                                    lunchMeals.add(meal);
                                    Log.d(TAG, "Added to lunch meals");
                                    break;
                                case "dinner":
                                    dinnerMeals.add(meal);
                                    Log.d(TAG, "Added to dinner meals");
                                    break;
                                default:
                                    Log.w(TAG, "Unknown meal time: " + mealTime);
                                    break;
                            }
                        } else {
                            Log.w(TAG, "Null meal from snapshot: " + mealSnapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing meal snapshot", e);
                        
                        // Manual parsing as fallback
                        try {
                            String id = mealSnapshot.getKey();
                            String recipeName = mealSnapshot.child("recipeName").getValue(String.class);
                            String mealTime = mealSnapshot.child("mealTime").getValue(String.class);
                            String imageUrl = mealSnapshot.child("imageUrl").getValue(String.class);
                            
                            // Handle dateTime properly
                            Date dateTime;
                            Object dateTimeObj = mealSnapshot.child("dateTime").getValue();
                            if (dateTimeObj instanceof Long) {
                                dateTime = new Date((Long) dateTimeObj);
                            } else {
                                dateTime = new Date(); // Default to current time
                            }
                            
                            // Create meal plan manually
                            MealPlan mealPlan = new MealPlan();
                            mealPlan.setId(id);
                            mealPlan.setRecipeName(recipeName);
                            mealPlan.setMealTime(mealTime);
                            mealPlan.setImageUrl(imageUrl);
                            mealPlan.setDateTime(dateTime);
                            
                            if (mealTime != null) {
                                switch (mealTime.toLowerCase()) {
                                    case "breakfast":
                                        breakfastMeals.add(mealPlan);
                                        Log.d(TAG, "Added to breakfast meals (manual fallback)");
                                        break;
                                    case "lunch":
                                        lunchMeals.add(mealPlan);
                                        Log.d(TAG, "Added to lunch meals (manual fallback)");
                                        break;
                                    case "dinner":
                                        dinnerMeals.add(mealPlan);
                                        Log.d(TAG, "Added to dinner meals (manual fallback)");
                                        break;
                                }
                            }
                        } catch (Exception e2) {
                            Log.e(TAG, "Failed manual fallback parsing", e2);
                        }
                    }
                }

                // Update UI
                updateMealSection(breakfastMeals, breakfastAdapter, tvNoBreakfast, "breakfast");
                updateMealSection(lunchMeals, lunchAdapter, tvNoLunch, "lunch");
                updateMealSection(dinnerMeals, dinnerAdapter, tvNoDinner, "dinner");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(MealPlannerActivity.this,
                             "Failed to load meals: " + databaseError.getMessage(),
                             Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshMealList(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        refreshMealList(cal);
    }

    @Override
    public void onMealClick(MealPlan meal) {
        // Use the static method from RecipeDetailActivity to create the Intent
        Intent intent;
        if (meal.getRecipe() != null) {
            // If the Recipe object is available, pass it directly
            intent = RecipeDetailActivity.newIntent(this, meal.getRecipe());
            Log.d(TAG, "Opening recipe detail with Recipe object for: " + meal.getRecipeName());
        } else {
            // If Recipe is null, create a minimal Recipe object from flat fields
            Recipe fallbackRecipe = new Recipe(
                    meal.getRecipeName(),           // title
                    meal.getImageUrl(),            // imageUrl
                    meal.getCookingTime(),         // duration
                    0.0f,                          // rating (default, since not stored in MealPlan)
                    new ArrayList<>(),             // empty ingredients list
                    new ArrayList<>()              // empty instructions list
            );
            intent = RecipeDetailActivity.newIntent(this, fallbackRecipe);
            Log.d(TAG, "Opening recipe detail with fallback Recipe for: " + meal.getRecipeName());
        }
        startActivity(intent);
        Toast.makeText(this, "Opening " + meal.getRecipeName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(MealPlan meal) {
        // Handle delete click
        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(currentDate.getTime());

        mDatabase.child("mealPlans")
                .child(userId)
                .child(dateKey)
                .child(meal.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Meal removed from plan", Toast.LENGTH_SHORT).show();
                    
                    // Cancel the notification for this meal
                    NotificationHelper.cancelMealPrepReminder(this, meal);
                    Log.d(TAG, "Cancelled notification for: " + meal.getRecipeName());
                    
                    loadMealsForCurrentDate();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove meal", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error removing meal", e);
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        // For testing purposes - add a test notification button
        FloatingActionButton fabTest = findViewById(R.id.fabAddMeal);
        if (fabTest != null) {
            fabTest.setOnLongClickListener(v -> {
                Toast.makeText(this, "Testing notification...", Toast.LENGTH_SHORT).show();
                
                // Create a test meal plan
                Recipe testRecipe = new Recipe();
                testRecipe.setTitle("Test Recipe");
                testRecipe.setDuration("30 min");
                testRecipe.setImageUrl("https://example.com/test.jpg");
                
                // Create a meal plan and notification
                MealPlan testMeal = new MealPlan(userId, testRecipe, new Date(), "Dinner");
                testMeal.setId("test_" + System.currentTimeMillis());
                NotificationHelper.scheduleMealPrepReminder(this, testMeal);
                Log.d(TAG, "Scheduled notification for: " + testMeal.getRecipeName());
                
                return true;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMealsForCurrentDate(); // Reload meals when activity resumes
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
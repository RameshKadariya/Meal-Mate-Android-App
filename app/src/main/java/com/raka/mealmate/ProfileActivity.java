package com.raka.mealmate;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.raka.mealmate.helpers.StoreManager;
import com.raka.mealmate.models.UserProfile;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final String PROFILES_REF = "profiles";
    private static final String RECIPES_REF = "recipes";
    private static final String MEAL_PLANS_REF = "mealPlans";

    private ImageView profileImage;
    private TextView profileName, profileEmail;
    private TextView recipeCount, plannedMealsCount, storesCount, completedListsCount;
    private TextView phoneNumber, cookingLevel, dietaryPreferences;
    private LinearLayout allergensContainer;
    private TextView noAllergensText;
    private Button btnEditProfile, btnChangePassword;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private UserProfile currentProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Profile");

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load profile data
        loadUserProfile();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        phoneNumber = findViewById(R.id.phoneNumber);

        recipeCount = findViewById(R.id.recipeCount);
        plannedMealsCount = findViewById(R.id.plannedMealsCount);
        storesCount = findViewById(R.id.storesCount);
        completedListsCount = findViewById(R.id.completedListsCount);

        cookingLevel = findViewById(R.id.cookingLevel);
        dietaryPreferences = findViewById(R.id.dietaryPreferences);

        allergensContainer = findViewById(R.id.allergensContainer);
        noAllergensText = findViewById(R.id.noAllergensText);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
    }

    private void setClickListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set basic user info from Firebase Auth
        profileEmail.setText(user.getEmail());

        if (user.getDisplayName() != null) {
            profileName.setText(user.getDisplayName());
        }

        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.profile_placeholder)
                    .into(profileImage);
        }

        // Load user profile from Firebase Realtime Database
        DatabaseReference profileRef = mDatabase.child(PROFILES_REF).child(user.getUid());
        profileRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    currentProfile = dataSnapshot.getValue(UserProfile.class);
                    updateUIWithProfile();
                    // Now load actual recipe and meal plan counts
                    loadRecipeCount(user.getUid());
                    loadPlannedMealsCount(user.getUid());
                    loadFavoriteStoresCount(); // Load stores directly from Firebase
                } else {
                    // Create a new profile if one doesn't exist
                    createNewProfile(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadUserProfile:onCancelled", databaseError.toException());
                Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        });

        // Get store count
        loadFavoriteStoresCount();
    }

    private void createNewProfile(FirebaseUser user) {
        currentProfile = new UserProfile(user.getUid(), user.getDisplayName(), user.getEmail());

        // Set some sample data for the dashboard
        currentProfile.setTotalRecipes(0);
        currentProfile.setPlannedMeals(0);
        currentProfile.setFavoriteStores(0);
        currentProfile.setCompletedShoppingLists(0);

        // Save to database
        mDatabase.child(PROFILES_REF).child(user.getUid()).setValue(currentProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile saved successfully");
                    updateUIWithProfile();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating profile", e);
                    Toast.makeText(ProfileActivity.this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUIWithProfile() {
        if (currentProfile == null) return;

        // Update user info
        if (currentProfile.getName() != null) {
            profileName.setText(currentProfile.getName());
        }

        // Update dashboard stats
        recipeCount.setText(String.valueOf(currentProfile.getTotalRecipes()));
        plannedMealsCount.setText(String.valueOf(currentProfile.getPlannedMeals()));
        storesCount.setText(String.valueOf(currentProfile.getFavoriteStores()));
        completedListsCount.setText(String.valueOf(currentProfile.getCompletedShoppingLists()));

        // Update personal info
        if (currentProfile.getPhoneNumber() != null) {
            phoneNumber.setText(currentProfile.getPhoneNumber());
        } else {
            phoneNumber.setText("Not set");
        }

        if (currentProfile.getCookingLevel() != null) {
            cookingLevel.setText(currentProfile.getCookingLevel());
        } else {
            cookingLevel.setText("Beginner");
        }

        if (currentProfile.getDietaryPreferences() != null) {
            dietaryPreferences.setText(currentProfile.getDietaryPreferences());
        } else {
            dietaryPreferences.setText("None specified");
        }

        // Update allergens
        updateAllergensView();
    }

    private void updateAllergensView() {
        allergensContainer.removeAllViews();

        if (currentProfile.getAllergens() == null || currentProfile.getAllergens().isEmpty()) {
            noAllergensText.setVisibility(View.VISIBLE);
            return;
        }

        noAllergensText.setVisibility(View.GONE);

        // Add text views for each allergen
        for (Map.Entry<String, Boolean> entry : currentProfile.getAllergens().entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                TextView allergenText = new TextView(this);
                allergenText.setText("• " + entry.getKey());
                allergenText.setTextSize(16);
                allergenText.setPadding(0, 4, 0, 4);
                allergensContainer.addView(allergenText);
            }
        }
    }

    private void loadRecipeCount(String userId) {
        // Get all recipes from the database
        DatabaseReference recipesRef = FirebaseDatabase.getInstance().getReference(RECIPES_REF);
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // First get the count of default recipes (these are not in the database)
                int defaultRecipesCount = getDefaultRecipesCount();
                
                // Then count the recipes in database
                int databaseRecipesCount = (int) dataSnapshot.getChildrenCount();
                
                // Total count is the sum of both
                int totalCount = defaultRecipesCount + databaseRecipesCount;
                
                // Update the UI with total count
                recipeCount.setText(String.valueOf(totalCount));
                
                // Also update the profile object to keep it in sync
                if (currentProfile != null) {
                    currentProfile.setTotalRecipes(totalCount);
                    mDatabase.child(PROFILES_REF).child(userId).child("totalRecipes").setValue(totalCount);
                }
                
                Log.d(TAG, "Recipe count: " + totalCount + " (Default: " + defaultRecipesCount + ", Database: " + databaseRecipesCount + ")");
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadRecipeCount:onCancelled", databaseError.toException());
            }
        });
    }
    
    // This method returns the count of default recipes used in RecipeBrowserActivity
    private int getDefaultRecipesCount() {
        // There are 22 default recipes in the RecipeBrowserActivity as counted from the code:
        // 1-22. Various recipes including Chicken Stir-Fry, Vegetable Quinoa Bowl, etc.
        return 22;
    }

    private void loadPlannedMealsCount(String userId) {
        // Get all planned meals for this user
        DatabaseReference mealsRef = FirebaseDatabase.getInstance().getReference(MEAL_PLANS_REF).child(userId);
        mealsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Count the total number of meal plans across all dates
                int totalMeals = 0;
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    totalMeals += (int) dateSnapshot.getChildrenCount();
                }

                // Update the UI
                plannedMealsCount.setText(String.valueOf(totalMeals));

                // Also update the profile object to keep it in sync
                if (currentProfile != null) {
                    currentProfile.setPlannedMeals(totalMeals);
                    mDatabase.child(PROFILES_REF).child(userId).child("plannedMeals").setValue(totalMeals);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadPlannedMealsCount:onCancelled", databaseError.toException());
            }
        });
    }

    private void loadFavoriteStoresCount() {
        DatabaseReference storesRef = FirebaseDatabase.getInstance().getReference("stores");
        storesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Count all stores in the database
                int count = (int) dataSnapshot.getChildrenCount();
                
                // Update UI with store count
                storesCount.setText(String.valueOf(count));
                
                // Also update the profile object
                if (currentProfile != null) {
                    currentProfile.setFavoriteStores(count);
                    String userId = mAuth.getCurrentUser().getUid();
                    mDatabase.child(PROFILES_REF).child(userId).child("favoriteStores").setValue(count);
                }
                
                Log.d(TAG, "Favorite stores count: " + count);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadFavoriteStoresCount:onCancelled", databaseError.toException());
            }
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        // Initialize dialog views
        EditText editName = dialogView.findViewById(R.id.editName);
        EditText editPhone = dialogView.findViewById(R.id.editPhone);
        RadioGroup radioGroupCookingLevel = dialogView.findViewById(R.id.radioGroupCookingLevel);
        EditText editDietaryPreferences = dialogView.findViewById(R.id.editDietaryPreferences);
        CheckBox checkNuts = dialogView.findViewById(R.id.checkNuts);
        CheckBox checkDairy = dialogView.findViewById(R.id.checkDairy);
        CheckBox checkShellfish = dialogView.findViewById(R.id.checkShellfish);
        CheckBox checkGluten = dialogView.findViewById(R.id.checkGluten);
        CheckBox checkEggs = dialogView.findViewById(R.id.checkEggs);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Populate dialog with current data
        if (currentProfile != null) {
            editName.setText(currentProfile.getName());

            if (currentProfile.getPhoneNumber() != null) {
                editPhone.setText(currentProfile.getPhoneNumber());
            }

            // Set cooking level radio button
            String level = currentProfile.getCookingLevel();
            if (level != null) {
                if (level.equals("Beginner")) {
                    radioGroupCookingLevel.check(R.id.radioBeginner);
                } else if (level.equals("Intermediate")) {
                    radioGroupCookingLevel.check(R.id.radioIntermediate);
                } else if (level.equals("Advanced")) {
                    radioGroupCookingLevel.check(R.id.radioAdvanced);
                }
            } else {
                radioGroupCookingLevel.check(R.id.radioBeginner);
            }

            if (currentProfile.getDietaryPreferences() != null) {
                editDietaryPreferences.setText(currentProfile.getDietaryPreferences());
            }

            // Set allergen checkboxes
            Map<String, Boolean> allergens = currentProfile.getAllergens();
            if (allergens != null) {
                checkNuts.setChecked(Boolean.TRUE.equals(allergens.get("Nuts")));
                checkDairy.setChecked(Boolean.TRUE.equals(allergens.get("Dairy")));
                checkShellfish.setChecked(Boolean.TRUE.equals(allergens.get("Shellfish")));
                checkGluten.setChecked(Boolean.TRUE.equals(allergens.get("Gluten")));
                checkEggs.setChecked(Boolean.TRUE.equals(allergens.get("Eggs")));
            }
        }

        // Create and show dialog
        AlertDialog dialog = builder.create();

        // Set button click listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            // Get updated values
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();

            // Get selected cooking level
            String level;
            int checkedId = radioGroupCookingLevel.getCheckedRadioButtonId();
            if (checkedId == R.id.radioBeginner) {
                level = "Beginner";
            } else if (checkedId == R.id.radioIntermediate) {
                level = "Intermediate";
            } else {
                level = "Advanced";
            }

            String dietary = editDietaryPreferences.getText().toString().trim();

            // Update allergens
            Map<String, Boolean> allergens = new HashMap<>();
            allergens.put("Nuts", checkNuts.isChecked());
            allergens.put("Dairy", checkDairy.isChecked());
            allergens.put("Shellfish", checkShellfish.isChecked());
            allergens.put("Gluten", checkGluten.isChecked());
            allergens.put("Eggs", checkEggs.isChecked());

            // Update profile
            currentProfile.setName(name);
            currentProfile.setPhoneNumber(phone);
            currentProfile.setCookingLevel(level);
            currentProfile.setDietaryPreferences(dietary);
            currentProfile.setAllergens(allergens);

            // Save to database
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                mDatabase.child(PROFILES_REF).child(user.getUid()).setValue(currentProfile)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error updating profile", e);
                        });
            }
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        EditText editCurrentPassword = dialogView.findViewById(R.id.editCurrentPassword);
        EditText editNewPassword = dialogView.findViewById(R.id.editNewPassword);
        EditText editConfirmPassword = dialogView.findViewById(R.id.editConfirmPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String currentPassword = editCurrentPassword.getText().toString().trim();
            String newPassword = editNewPassword.getText().toString().trim();
            String confirmPassword = editConfirmPassword.getText().toString().trim();

            // Validation
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Change password
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                // Reauthenticate user first (for security)
                // This is a simplified version - in a real app you would need to reauthenticate
                // the user with Firebase Auth before changing password

                user.updatePassword(newPassword)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ProfileActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ProfileActivity.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error updating password", e);
                        });
            }
        });

        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

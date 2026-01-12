package com.raka.mealmate.repositories;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.raka.mealmate.models.MealPlan;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MealPlanRepository {
    private static final String TAG = "MealPlanRepository";
    private final DatabaseReference mealPlansRef;

    public MealPlanRepository() {
        mealPlansRef = FirebaseDatabase.getInstance().getReference().child("mealPlans");
    }

    public Task<Void> addMealPlan(MealPlan mealPlan) {
        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(mealPlan.getDateTime());
        
        Log.d(TAG, "Adding meal plan: " + mealPlan.getRecipeName());
        Log.d(TAG, "Date key: " + dateKey);
        Log.d(TAG, "Meal time: " + mealPlan.getMealTime());
        
        DatabaseReference dateRef = mealPlansRef
                .child(mealPlan.getUserId())
                .child(dateKey);

        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        // First check if this recipe already exists for this meal time
        dateRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                boolean duplicateFound = false;
                for (DataSnapshot mealSnapshot : task.getResult().getChildren()) {
                    MealPlan existingMeal = mealSnapshot.getValue(MealPlan.class);
                    if (existingMeal != null && 
                        existingMeal.getRecipeName().equals(mealPlan.getRecipeName()) && 
                        existingMeal.getMealTime().equals(mealPlan.getMealTime())) {
                        duplicateFound = true;
                        Log.d(TAG, "Duplicate meal found, skipping add");
                        taskCompletionSource.setResult(null);
                        break;
                    }
                }
                
                if (!duplicateFound) {
                    dateRef.child(mealPlan.getId())
                          .setValue(mealPlan)
                          .addOnSuccessListener(aVoid -> {
                              Log.d(TAG, "Meal plan added successfully");
                              taskCompletionSource.setResult(null);
                          })
                          .addOnFailureListener(e -> {
                              Log.e(TAG, "Failed to add meal plan", e);
                              taskCompletionSource.setException(e);
                          });
                }
            } else if (task.getException() != null) {
                taskCompletionSource.setException(task.getException());
            } else {
                taskCompletionSource.setResult(null);
            }
        });

        return taskCompletionSource.getTask();
    }

    public Task<Void> removeMealPlan(MealPlan mealPlan) {
        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(mealPlan.getDateTime());
        
        Log.d(TAG, "Removing meal plan: " + mealPlan.getRecipeName());
        
        return mealPlansRef
                .child(mealPlan.getUserId())
                .child(dateKey)
                .child(mealPlan.getId())
                .removeValue();
    }
}
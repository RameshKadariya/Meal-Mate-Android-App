package com.raka.mealmate.dialogs;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.raka.mealmate.R;
import com.raka.mealmate.helpers.NotificationHelper;
import com.raka.mealmate.models.MealPlan;
import com.raka.mealmate.models.Recipe;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MealPlanDialog extends Dialog {
    private static final String TAG = "MealPlanDialog";
    
    private Recipe recipe;
    private OnMealPlanSelectedListener listener;
    private TextView dateText;
    private RadioGroup mealTimeGroup;
    private Calendar selectedDate;
    private ImageView recipePreviewImage;
    private TextView recipePreviewTitle;
    private DatabaseReference mDatabase;
    private String userId;

    public interface OnMealPlanSelectedListener {
        void onMealPlanSelected(Recipe recipe, Date date, String mealTime);
    }

    public MealPlanDialog(@NonNull Context context, Recipe recipe, OnMealPlanSelectedListener listener) {
        super(context);
        this.recipe = recipe;
        this.listener = listener;
        this.selectedDate = Calendar.getInstance();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_meal_plan);

        // Set dialog width to 90% of screen width
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = (int)(getContext().getResources().getDisplayMetrics().widthPixels * 0.9);
        getWindow().setAttributes(params);

        // Initialize views
        dateText = findViewById(R.id.dateText);
        mealTimeGroup = findViewById(R.id.mealTimeGroup);
        recipePreviewImage = findViewById(R.id.recipePreviewImage);
        recipePreviewTitle = findViewById(R.id.recipePreviewTitle);
        Button selectDateButton = findViewById(R.id.selectDateButton);
        Button addButton = findViewById(R.id.addButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        // Set recipe preview
        recipePreviewTitle.setText(recipe.getTitle());
        Glide.with(getContext())
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.placeholder_recipe)
                .error(R.drawable.error_recipe)
                .centerCrop()
                .into(recipePreviewImage);

        // Set initial date
        updateDateText();

        // Setup date selection
        selectDateButton.setOnClickListener(v -> showDatePicker());

        // Setup buttons
        addButton.setOnClickListener(v -> {
            int selectedId = mealTimeGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(getContext(), "Please select a meal time", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadioButton = findViewById(selectedId);
            String mealTime = selectedRadioButton.getText().toString().toLowerCase();
            
            // Create MealPlan object
            MealPlan mealPlan = new MealPlan(userId, recipe, selectedDate.getTime(), mealTime);
            
            // Save to Firebase
            String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    .format(selectedDate.getTime());
            
            Log.d(TAG, "Creating meal plan:");
            Log.d(TAG, "Date Key: " + dateKey);
            Log.d(TAG, "Selected Date Millis: " + selectedDate.getTimeInMillis());
            Log.d(TAG, "Meal Time: " + mealTime);
            Log.d(TAG, "Recipe Name: " + recipe.getTitle());
            
            String mealId = mDatabase.child("mealPlans").child(userId).child(dateKey).push().getKey();
            if (mealId != null) {
                mealPlan.setId(mealId);
                
                Log.d(TAG, "Saving meal plan - Name: " + mealPlan.getRecipeName() + 
                          ", Time: " + mealPlan.getMealTime() +
                          ", Date: " + dateKey +
                          ", ID: " + mealId);

                mDatabase.child("mealPlans")
                        .child(userId)
                        .child(dateKey)
                        .child(mealId)
                        .setValue(mealPlan)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Meal plan saved successfully");
                            
                            // Schedule notification for this new meal plan
                            NotificationHelper.scheduleMealPrepReminder(getContext(), mealPlan);
                            
                            if (listener != null) {
                                listener.onMealPlanSelected(recipe, selectedDate.getTime(), mealTime);
                            }
                            dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error saving meal plan", e);
                            Toast.makeText(getContext(), "Failed to add meal to plan", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(),
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateText();
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDateText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        dateText.setText(dateFormat.format(selectedDate.getTime()));
    }
}
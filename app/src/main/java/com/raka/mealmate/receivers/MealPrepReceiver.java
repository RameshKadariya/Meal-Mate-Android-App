package com.raka.mealmate.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.raka.mealmate.helpers.NotificationHelper;
import com.raka.mealmate.models.MealPlan;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class

MealPrepReceiver extends BroadcastReceiver {
    private static final String TAG = "MealPrepReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "MealPrepReceiver onReceive called with intent: " + intent);
        
        String action = intent.getAction();
        Log.d(TAG, "Intent action: " + action);
        
        if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Device just booted, reschedule all meal prep reminders
            Log.d(TAG, "Device booted, rescheduling meal prep notifications");
            rescheduleNotifications(context);
        } else if (action != null && action.equals(NotificationHelper.ACTION_MEAL_PREP_REMINDER)) {
            // This is our custom action for meal prep reminders
            processMealPrepReminder(context, intent);
        } else {
            // Fallback case - try to process notification anyway for backward compatibility
            Log.d(TAG, "No specific action matched, attempting to process notification anyway");
            processMealPrepReminder(context, intent);
        }
    }
    
    private void processMealPrepReminder(Context context, Intent intent) {
        try {
            String mealName = intent.getStringExtra("MEAL_NAME");
            String mealTime = intent.getStringExtra("MEAL_TIME");
            int notificationId = intent.getIntExtra("NOTIFICATION_ID", 0);
            
            Log.d(TAG, "Processing meal prep reminder for: " + mealName + ", time: " + mealTime + ", id: " + notificationId);
            
            if (mealName == null || mealTime == null) {
                Log.e(TAG, "Missing meal information in intent");
                return;
            }
            
            // Show the notification
            NotificationHelper.showMealPrepNotification(context, mealName, mealTime, notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Error processing meal prep reminder: " + e.getMessage(), e);
        }
    }
    
    private void rescheduleNotifications(Context context) {
        // Get current user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.d(TAG, "No user logged in, cannot reschedule notifications");
            return;
        }
        
        String userId = auth.getCurrentUser().getUid();
        
        // Get today's date
        Calendar calendar = Calendar.getInstance();
        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                .format(calendar.getTime());
        
        // Get upcoming meal plans from Firebase
        DatabaseReference mealPlansRef = FirebaseDatabase.getInstance().getReference()
                .child("mealPlans").child(userId).child(dateKey);
        
        mealPlansRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Found meal plans for today, rescheduling...");
                    for (DataSnapshot mealSnapshot : dataSnapshot.getChildren()) {
                        MealPlan mealPlan = mealSnapshot.getValue(MealPlan.class);
                        if (mealPlan != null) {
                            NotificationHelper.scheduleMealPrepReminder(context, mealPlan);
                            Log.d(TAG, "Rescheduled notification for: " + mealPlan.getRecipeName());
                        }
                    }
                } else {
                    Log.d(TAG, "No meal plans found for today");
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error retrieving meal plans: " + databaseError.getMessage());
            }
        });
    }
}

package com.raka.mealmate.helpers;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.raka.mealmate.MealPlannerActivity;
import com.raka.mealmate.R;
import com.raka.mealmate.models.MealPlan;
import com.raka.mealmate.receivers.MealPrepReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "meal_prep_channel";
    private static final String CHANNEL_NAME = "Meal Prep Reminders";
    public static final String ACTION_MEAL_PREP_REMINDER = "com.raka.mealmate.ACTION_MEAL_PREP_REMINDER";
    
    // Schedule a notification for upcoming meal prep
    public static void scheduleMealPrepReminder(Context context, MealPlan mealPlan) {
        try {
            // Create notification channel first (required for Android 8.0+)
            createNotificationChannel(context);
            
            Log.d(TAG, "Scheduling notification for " + mealPlan.getRecipeName() + " in 10 seconds");
            
            // Single notification with 10-second delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    showMealPrepNotification(context, mealPlan.getRecipeName(), mealPlan.getMealTime(), mealPlan.getId().hashCode());
                    Log.d(TAG, "Notification shown for " + mealPlan.getRecipeName());
                } catch (Exception e) {
                    Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
                }
            }, 10000); // 10-second delay
            
        } catch (Exception e) {
            Log.e(TAG, "Error in scheduleMealPrepReminder: " + e.getMessage(), e);
        }
    }
    
    // Cancel a scheduled notification
    public static void cancelMealPrepReminder(Context context, MealPlan mealPlan) {
        try {
            Intent intent = new Intent(context, MealPrepReceiver.class);
            intent.setAction(ACTION_MEAL_PREP_REMINDER);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    mealPlan.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "Cancelled notification for " + mealPlan.getRecipeName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification: " + e.getMessage(), e);
        }
    }
    
    // Show a notification immediately
    public static void showMealPrepNotification(Context context, String mealName, String mealTime, int notificationId) {
        try {
            // Create notification channel first (required for Android 8.0+)
            createNotificationChannel(context);
            
            // Create intent for when notification is tapped
            Intent intent = new Intent(context, MealPlannerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 
                    0, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Build the notification with high priority
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_food_logo)
                    .setContentTitle("Time to prepare " + mealName)
                    .setContentText("It's time to start preparing your " + mealTime.toLowerCase() + "!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            
            // Show the notification
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Showed notification for " + mealName);
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage(), e);
        }
    }
    
    // Create the notification channel (required for Android 8.0+)
    private static void createNotificationChannel(Context context) {
        try {
            // Only needed for Android 8.0 (API level 26) and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Reminders for upcoming meal preparations");
                channel.enableVibration(true);
                channel.enableLights(true);
                
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Created notification channel: " + CHANNEL_ID);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel: " + e.getMessage(), e);
        }
    }
}

package com.raka.mealmate.models;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MealPlan implements Serializable {
    private String id;
    private String recipeName;
    private String imageUrl;
    private String cookingTime;
    private String mealTime;
    private String date;      // format: yyyyMMdd
    private String userId;
    private Recipe recipe;
    private Date dateTime;  // This will store the actual Date object
    private long timestamp;

    public MealPlan() {
        // Required for Firebase
        this.dateTime = new Date();  // Initialize with current time
        this.timestamp = System.currentTimeMillis();
    }

    public MealPlan(String recipeName, String imageUrl, String cookingTime, String mealTime, long dateTime, String userId) {
        this.recipeName = recipeName;
        this.imageUrl = imageUrl;
        this.cookingTime = cookingTime;
        this.mealTime = mealTime;
        this.userId = userId;
        this.dateTime = new Date(dateTime);  // Initialize with the given date/time
        this.timestamp = System.currentTimeMillis();
        this.date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(this.dateTime);
        this.id = userId + "_" + this.date + "_" + mealTime + "_" + this.timestamp;
    }

    // Constructor for creating MealPlan from Recipe
    public MealPlan(String userId, Recipe recipe, Date dateTime, String mealTime) {
        this.userId = userId;
        this.recipe = recipe;
        this.dateTime = dateTime;  // Store the actual Date object
        this.mealTime = mealTime;
        this.timestamp = System.currentTimeMillis();
        
        // Set derived fields
        this.recipeName = recipe.getTitle();
        this.imageUrl = recipe.getImageUrl();
        this.cookingTime = recipe.getDuration();
        this.date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(dateTime);
        this.id = userId + "_" + this.date + "_" + mealTime + "_" + this.timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCookingTime() {
        return cookingTime;
    }

    public void setCookingTime(String cookingTime) {
        this.cookingTime = cookingTime;
    }

    public String getMealTime() {
        return mealTime;
    }

    public void setMealTime(String mealTime) {
        this.mealTime = mealTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Object dateTimeObj) {
        if (dateTimeObj instanceof Long) {
            this.dateTime = new Date((Long) dateTimeObj);
        } else if (dateTimeObj instanceof Date) {
            this.dateTime = (Date) dateTimeObj;
        } else if (dateTimeObj == null) {
            this.dateTime = new Date();  // Default to current time if null
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    // Get the timestamp in milliseconds for notification scheduling
    public long getTimeInMillis() {
        return dateTime != null ? dateTime.getTime() : System.currentTimeMillis();
    }
}
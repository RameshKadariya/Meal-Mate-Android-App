package com.raka.mealmate.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class for user profile data
 */
public class UserProfile {
    private String userId;
    private String name;
    private String email;
    private String photoUrl;
    private String phoneNumber;
    private int totalRecipes;
    private int plannedMeals;
    private int favoriteStores;
    private int completedShoppingLists;
    private String dietaryPreferences;
    private String cookingLevel;
    private Map<String, Boolean> allergens;

    // Required empty constructor for Firebase
    public UserProfile() {
    }

    public UserProfile(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.totalRecipes = 0;
        this.plannedMeals = 0;
        this.favoriteStores = 0;
        this.completedShoppingLists = 0;
        this.cookingLevel = "Beginner";
        this.allergens = new HashMap<>();
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getTotalRecipes() {
        return totalRecipes;
    }

    public void setTotalRecipes(int totalRecipes) {
        this.totalRecipes = totalRecipes;
    }

    public int getPlannedMeals() {
        return plannedMeals;
    }

    public void setPlannedMeals(int plannedMeals) {
        this.plannedMeals = plannedMeals;
    }

    public int getFavoriteStores() {
        return favoriteStores;
    }

    public void setFavoriteStores(int favoriteStores) {
        this.favoriteStores = favoriteStores;
    }

    public int getCompletedShoppingLists() {
        return completedShoppingLists;
    }

    public void setCompletedShoppingLists(int completedShoppingLists) {
        this.completedShoppingLists = completedShoppingLists;
    }

    public String getDietaryPreferences() {
        return dietaryPreferences;
    }

    public void setDietaryPreferences(String dietaryPreferences) {
        this.dietaryPreferences = dietaryPreferences;
    }

    public String getCookingLevel() {
        return cookingLevel;
    }

    public void setCookingLevel(String cookingLevel) {
        this.cookingLevel = cookingLevel;
    }

    public Map<String, Boolean> getAllergens() {
        return allergens;
    }

    public void setAllergens(Map<String, Boolean> allergens) {
        this.allergens = allergens;
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("name", name);
        result.put("email", email);
        result.put("photoUrl", photoUrl);
        result.put("phoneNumber", phoneNumber);
        result.put("totalRecipes", totalRecipes);
        result.put("plannedMeals", plannedMeals);
        result.put("favoriteStores", favoriteStores);
        result.put("completedShoppingLists", completedShoppingLists);
        result.put("dietaryPreferences", dietaryPreferences);
        result.put("cookingLevel", cookingLevel);
        result.put("allergens", allergens);
        return result;
    }
}

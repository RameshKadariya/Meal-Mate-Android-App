package com.raka.mealmate.models;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Recipe implements Serializable {
    private String id;
    private String title;
    private String imageUrl;
    private String duration;//old
    private float rating;
    private List<Ingredient> ingredients;
    private List<String> instructions;
    private String category;

    public Recipe() {
        // Required empty constructor for Firebase
        this.ingredients = new ArrayList<>();
        this.instructions = new ArrayList<>();
    }

    // Simple constructor for featured recipes
    public Recipe(String title, String imageUrl, String duration) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.duration = duration;
        this.rating = 0.0f;
        this.ingredients = new ArrayList<>();
        this.instructions = new ArrayList<>();
        this.category = "";
        this.id = String.valueOf(System.currentTimeMillis());  // Generate a simple ID
    }

    // Constructor with rating
    public Recipe(String title, String imageUrl, String duration, float rating) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.duration = duration;
        this.rating = rating;
        this.ingredients = new ArrayList<>();
        this.instructions = new ArrayList<>();
        this.category = "";
        this.id = String.valueOf(System.currentTimeMillis());
    }

    // Constructor without id and category
    public Recipe(String title, String imageUrl, String duration, float rating,
                 List<Ingredient> ingredients, List<String> instructions) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.duration = duration;
        this.rating = rating;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.instructions = instructions != null ? instructions : new ArrayList<>();
        this.category = "";
        this.id = String.valueOf(System.currentTimeMillis());
    }

    public Recipe(String id, String title, String imageUrl, String duration, float rating,
                 List<Ingredient> ingredients, List<String> instructions, String category) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.duration = duration;
        this.rating = rating;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
        this.instructions = instructions != null ? instructions : new ArrayList<>();
        this.category = category;
    }

    public Recipe(String id, String title, String category, String duration, float rating,
                 String imageUrl) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.duration = duration;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.ingredients = new ArrayList<>();
        this.instructions = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }

    public List<String> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<String> instructions) {
        this.instructions = instructions != null ? instructions : new ArrayList<>();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
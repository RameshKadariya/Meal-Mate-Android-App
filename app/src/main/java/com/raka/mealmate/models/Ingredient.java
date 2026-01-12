package com.raka.mealmate.models;

import java.io.Serializable;

public class Ingredient implements Serializable {
    private String name;
    private double amount;
    private String unit;
    private String category;

    // Required empty constructor for Firebase
    public Ingredient() {
    }

    public Ingredient(String name, double amount, String unit, String category) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
        this.category = category;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

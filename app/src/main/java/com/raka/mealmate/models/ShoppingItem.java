package com.raka.mealmate.models;

import java.io.Serializable;

public class ShoppingItem implements Serializable {
    private String id;
    private String name;
    private double amount;
    private String unit;
    private String category;
    private boolean purchased;
    private String store; // Store name for backward compatibility
    private String storeId; // Reference to the Store object id
    private double price;

    // Required empty constructor for Firebase
    public ShoppingItem() {
    }

    public ShoppingItem(String name, double amount, String unit, String category) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
        this.category = category;
        this.purchased = false;
        this.price = 0.0;
    }

    // Constructor with store reference
    public ShoppingItem(String name, double amount, String unit, String category, String storeId) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
        this.category = category;
        this.purchased = false;
        this.storeId = storeId;
        this.price = 0.0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }
    
    public String getStoreId() {
        return storeId;
    }
    
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}

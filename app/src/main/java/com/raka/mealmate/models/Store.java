package com.raka.mealmate.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class for grocery stores that can be geotagged on a map.
 */
public class Store implements Serializable {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String storeType; // e.g., Supermarket, Convenience Store, Farmers Market
    private String notes;
    private List<String> availableItems; // List of items typically found at this store
    
    /**
     * Required empty constructor for Firebase
     */
    public Store() {
        this.availableItems = new ArrayList<>();
    }
    
    /**
     * Constructor with essential fields
     */
    public Store(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.storeType = "";
        this.notes = "";
        this.availableItems = new ArrayList<>();
        this.id = String.valueOf(System.currentTimeMillis()); // Generate a simple ID
    }
    
    /**
     * Full constructor
     */
    public Store(String id, String name, String address, double latitude, double longitude, 
                String storeType, String notes, List<String> availableItems) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.storeType = storeType;
        this.notes = notes;
        this.availableItems = availableItems != null ? availableItems : new ArrayList<>();
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getStoreType() {
        return storeType;
    }
    
    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public List<String> getAvailableItems() {
        return availableItems;
    }
    
    public void setAvailableItems(List<String> availableItems) {
        this.availableItems = availableItems != null ? availableItems : new ArrayList<>();
    }
    
    public void addAvailableItem(String item) {
        if (item != null && !item.isEmpty()) {
            this.availableItems.add(item);
        }
    }
    
    public void removeAvailableItem(String item) {
        this.availableItems.remove(item);
    }
    
    @Override
    public String toString() {
        return name + " (" + address + ")";
    }
}

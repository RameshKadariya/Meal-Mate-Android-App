package com.raka.mealmate.helpers;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.raka.mealmate.models.Store;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for managing grocery store operations.
 */
public class StoreManager {
    private static final String STORES_REF = "stores";
    private final DatabaseReference storesRef;
    private List<Store> storeList = new ArrayList<>();
    private StoreLoadListener listener;
    private Context context;

    // Singleton pattern
    private static StoreManager instance;

    public static StoreManager getInstance(Context context) {
        if (instance == null) {
            instance = new StoreManager(context);
        }
        return instance;
    }

    private StoreManager(Context context) {
        this.context = context;
        storesRef = FirebaseDatabase.getInstance().getReference(STORES_REF);
    }

    // Interface for store load callbacks
    public interface StoreLoadListener {
        void onStoresLoaded(List<Store> stores);
        void onStoreLoadError(Exception error);
    }

    /**
     * Set listener for store load events
     */
    public void setStoreLoadListener(StoreLoadListener listener) {
        this.listener = listener;
    }

    /**
     * Load all stores from the database
     */
    public void loadStores() {
        storesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                storeList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Store store = snapshot.getValue(Store.class);
                        if (store != null) {
                            store.setId(snapshot.getKey());
                            storeList.add(store);
                        }
                    } catch (Exception e) {
                        if (listener != null) {
                            listener.onStoreLoadError(e);
                        }
                    }
                }
                
                // Add sample stores if none exist (for demonstration purposes)
                if (storeList.isEmpty()) {
                    createSampleStores();
                }
                
                if (listener != null) {
                    listener.onStoresLoaded(storeList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (listener != null) {
                    listener.onStoreLoadError(databaseError.toException());
                }
            }
        });
    }
    
    /**
     * Creates sample stores if none exist yet
     */
    public void createSampleStores() {
        // Create sample stores with more specific data
        Store store1 = new Store();
        store1.setId("sample1");
        store1.setName("Central Supermarket");
        store1.setAddress("123 Main St, Kathmandu");
        store1.setLatitude(27.7172);
        store1.setLongitude(85.3240);
        store1.setStoreType("Supermarket");
        store1.setNotes("Large supermarket with wide variety of products");
        
        Store store2 = new Store();
        store2.setId("sample2");
        store2.setName("Neighborhood Grocery");
        store2.setAddress("456 Oak Ave, Kathmandu");
        store2.setLatitude(27.7102);
        store2.setLongitude(85.3156);
        store2.setStoreType("Convenience Store");
        store2.setNotes("Small local store with basic essentials");
        
        Store store3 = new Store();
        store3.setId("sample3");
        store3.setName("Fresh Farmers Market");
        store3.setAddress("789 Green Rd, Kathmandu");
        store3.setLatitude(27.7242);
        store3.setLongitude(85.3320);
        store3.setStoreType("Farmers Market");
        store3.setNotes("Fresh local produce and organic goods");
        
        // Save the sample stores to Firebase
        DatabaseReference storeRef1 = storesRef.child(store1.getId());
        storeRef1.setValue(store1);
        
        DatabaseReference storeRef2 = storesRef.child(store2.getId());
        storeRef2.setValue(store2);
        
        DatabaseReference storeRef3 = storesRef.child(store3.getId());
        storeRef3.setValue(store3);
        
        // Notify listeners
        Toast.makeText(context, "Sample stores created successfully", Toast.LENGTH_SHORT).show();
        
        // Reload stores to update the list
        loadStores();
    }

    /**
     * Save a store to the database
     */
    public void saveStore(Store store) {
        if (store.getId() == null || store.getId().isEmpty()) {
            // Generate a new ID if none exists
            String storeId = storesRef.push().getKey();
            store.setId(storeId);
        }
        
        storesRef.child(store.getId()).setValue(store);
    }

    /**
     * Update a store in the database
     */
    public void updateStore(Store store) {
        if (store.getId() != null && !store.getId().isEmpty()) {
            storesRef.child(store.getId()).setValue(store);
        }
    }

    /**
     * Delete a store from the database
     */
    public void deleteStore(Store store) {
        if (store.getId() != null && !store.getId().isEmpty()) {
            storesRef.child(store.getId()).removeValue();
        }
    }

    /**
     * Get a store by its ID
     */
    public Store getStoreById(String storeId) {
        for (Store store : storeList) {
            if (store.getId().equals(storeId)) {
                return store;
            }
        }
        return null;
    }
    
    /**
     * Get the current list of stores
     */
    public List<Store> getStoreList() {
        return new ArrayList<>(storeList);
    }
}

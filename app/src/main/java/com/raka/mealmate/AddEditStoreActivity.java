package com.raka.mealmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.raka.mealmate.adapters.StoreItemAdapter;
import com.raka.mealmate.helpers.StoreManager;
import com.raka.mealmate.models.Store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddEditStoreActivity extends AppCompatActivity {
    
    private static final int PICK_LOCATION_REQUEST = 1;
    
    // UI Elements
    private EditText editTextStoreName, editTextAddress, editTextLatitude, editTextLongitude, editTextNotes, editTextItem;
    private Spinner spinnerStoreType;
    private Button btnPickLocation, btnAddItem, btnSaveStore;
    private RecyclerView recyclerViewItems;
    
    // Data
    private String storeId;
    private Store currentStore;
    private StoreItemAdapter itemAdapter;
    private List<String> availableItems = new ArrayList<>();
    private List<String> storeTypes = Arrays.asList("Supermarket", "Convenience Store", "Farmers Market", "Specialty Store", "Other");
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_store);
        
        // Initialize UI elements
        editTextStoreName = findViewById(R.id.editTextStoreName);
        editTextAddress = findViewById(R.id.editTextAddress);
        editTextLatitude = findViewById(R.id.editTextLatitude);
        editTextLongitude = findViewById(R.id.editTextLongitude);
        editTextNotes = findViewById(R.id.editTextNotes);
        editTextItem = findViewById(R.id.editTextItem);
        spinnerStoreType = findViewById(R.id.spinnerStoreType);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnSaveStore = findViewById(R.id.btnSaveStore);
        recyclerViewItems = findViewById(R.id.recyclerViewItems);
        
        // Set up spinner with store types
        ArrayAdapter<String> storeTypeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, storeTypes);
        storeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStoreType.setAdapter(storeTypeAdapter);
        
        // Set up recycler view for items
        itemAdapter = new StoreItemAdapter(this, availableItems);
        recyclerViewItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItems.setAdapter(itemAdapter);
        
        // Get Store ID from intent
        storeId = getIntent().getStringExtra("STORE_ID");
        
        // If we have a store ID, load the store to edit
        if (!TextUtils.isEmpty(storeId)) {
            loadStore(storeId);
        } else {
            // For new store, check if we were given coordinates
            double latitude = getIntent().getDoubleExtra("LATITUDE", 0);
            double longitude = getIntent().getDoubleExtra("LONGITUDE", 0);
            
            if (latitude != 0 && longitude != 0) {
                editTextLatitude.setText(String.valueOf(latitude));
                editTextLongitude.setText(String.valueOf(longitude));
            }
            
            setTitle("Add New Store");
        }
        
        // Set up button click listeners
        btnPickLocation.setOnClickListener(view -> {
            Intent intent = new Intent(AddEditStoreActivity.this, StoresMapActivity.class);
            intent.putExtra("PICK_LOCATION", true);
            startActivityForResult(intent, PICK_LOCATION_REQUEST);
        });
        
        btnAddItem.setOnClickListener(view -> {
            String item = editTextItem.getText().toString().trim();
            if (!TextUtils.isEmpty(item)) {
                availableItems.add(item);
                itemAdapter.notifyItemInserted(availableItems.size() - 1);
                editTextItem.setText("");
                Snackbar.make(view, "Item added", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(view, "Please enter an item name", Snackbar.LENGTH_SHORT).show();
            }
        });
        
        btnSaveStore.setOnClickListener(view -> saveStore());
        
        // Set up item adapter's delete callback
        itemAdapter.setOnDeleteClickListener(position -> {
            availableItems.remove(position);
            itemAdapter.notifyItemRemoved(position);
        });
    }
    
    private void loadStore(String storeId) {
        StoreManager storeManager = StoreManager.getInstance(this);
        currentStore = storeManager.getStoreById(storeId);
        
        if (currentStore != null) {
            // Populate fields with store data
            editTextStoreName.setText(currentStore.getName());
            editTextAddress.setText(currentStore.getAddress());
            editTextLatitude.setText(String.valueOf(currentStore.getLatitude()));
            editTextLongitude.setText(String.valueOf(currentStore.getLongitude()));
            editTextNotes.setText(currentStore.getNotes());
            
            // Set store type in spinner
            int typePosition = storeTypes.indexOf(currentStore.getStoreType());
            if (typePosition >= 0) {
                spinnerStoreType.setSelection(typePosition);
            }
            
            // Load available items
            availableItems.clear();
            availableItems.addAll(currentStore.getAvailableItems());
            itemAdapter.notifyDataSetChanged();
            
            setTitle("Edit Store: " + currentStore.getName());
        } else {
            Toast.makeText(this, "Store not found!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void saveStore() {
        // Validate required fields
        String name = editTextStoreName.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();
        String latitudeStr = editTextLatitude.getText().toString().trim();
        String longitudeStr = editTextLongitude.getText().toString().trim();
        
        if (TextUtils.isEmpty(name)) {
            editTextStoreName.setError("Store name is required");
            return;
        }
        
        if (TextUtils.isEmpty(address)) {
            editTextAddress.setError("Address is required");
            return;
        }
        
        if (TextUtils.isEmpty(latitudeStr)) {
            editTextLatitude.setError("Latitude is required");
            return;
        }
        
        if (TextUtils.isEmpty(longitudeStr)) {
            editTextLongitude.setError("Longitude is required");
            return;
        }
        
        double latitude;
        double longitude;
        
        try {
            latitude = Double.parseDouble(latitudeStr);
            longitude = Double.parseDouble(longitudeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid coordinates format", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String storeType = spinnerStoreType.getSelectedItem().toString();
        String notes = editTextNotes.getText().toString().trim();
        
        // Create or update the store
        StoreManager storeManager = StoreManager.getInstance(this);
        
        if (currentStore != null) {
            // Update existing store
            currentStore.setName(name);
            currentStore.setAddress(address);
            currentStore.setLatitude(latitude);
            currentStore.setLongitude(longitude);
            currentStore.setStoreType(storeType);
            currentStore.setNotes(notes);
            currentStore.setAvailableItems(new ArrayList<>(availableItems));
            
            storeManager.updateStore(currentStore);
            Toast.makeText(this, "Store updated", Toast.LENGTH_SHORT).show();
        } else {
            // Create new store
            Store newStore = new Store(name, address, latitude, longitude);
            newStore.setStoreType(storeType);
            newStore.setNotes(notes);
            newStore.setAvailableItems(new ArrayList<>(availableItems));
            
            storeManager.saveStore(newStore);
            Toast.makeText(this, "Store added", Toast.LENGTH_SHORT).show();
        }
        
        finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK && data != null) {
            double latitude = data.getDoubleExtra("LATITUDE", 0);
            double longitude = data.getDoubleExtra("LONGITUDE", 0);
            String address = data.getStringExtra("ADDRESS");
            
            if (latitude != 0 && longitude != 0) {
                editTextLatitude.setText(String.valueOf(latitude));
                editTextLongitude.setText(String.valueOf(longitude));
                
                if (!TextUtils.isEmpty(address)) {
                    editTextAddress.setText(address);
                }
            }
        }
    }
}

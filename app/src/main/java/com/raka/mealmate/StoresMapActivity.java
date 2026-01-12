package com.raka.mealmate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.raka.mealmate.adapters.StoreCardAdapter;
import com.raka.mealmate.adapters.StoreItemAdapter;
import com.raka.mealmate.helpers.ShakeDetector;
import com.raka.mealmate.helpers.StoreManager;
import com.raka.mealmate.models.Store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoresMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "StoresMapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 15f;
    
    private GoogleMap mMap;
    private CardView storeInfoCard;
    private TextView storeName, storeAddress, storeType;
    private Button btnViewItems, btnEditStore, btnDeleteStore;
    private FloatingActionButton fab;
    
    private FusedLocationProviderClient fusedLocationClient;
    private StoreManager storeManager;
    private Store selectedStore;
    private Map<Marker, Store> markerStoreMap;
    
    // Shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private Location mLastLocation;
    
    // Flag to indicate whether we're in "Pick Location" mode
    private boolean isPickingLocation = false;
    
    // Flag to indicate whether to highlight stores with shopping items
    private boolean highlightShoppingStores = false;
    
    // Flag to indicate whether we're in demo mode (no real map)
    private boolean isDemoMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stores_map);
        
        Log.d(TAG, "onCreate: Initializing StoresMapActivity");
        
        // Check if we're in picking location mode
        if (getIntent().hasExtra("PICK_LOCATION")) {
            isPickingLocation = true;
        }
        
        // Check if we should highlight stores with shopping items
        if (getIntent().hasExtra("HIGHLIGHT_SHOPPING_STORES")) {
            highlightShoppingStores = getIntent().getBooleanExtra("HIGHLIGHT_SHOPPING_STORES", false);
            Log.d(TAG, "onCreate: Highlight shopping stores: " + highlightShoppingStores);
        }
        
        // Check if we're in demo mode (passed as extra or from preferences)
        if (getIntent().hasExtra("DEMO_MODE")) {
            isDemoMode = getIntent().getBooleanExtra("DEMO_MODE", false);
        }
        
        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            Log.d(TAG, "onCreate: Getting map async");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "onCreate: MapFragment is null");
            showMapErrorView("Could not initialize map fragment");
        }
        
        // Initialize views
        storeName = findViewById(R.id.storeName);
        storeAddress = findViewById(R.id.storeAddress);
        storeType = findViewById(R.id.storeType);
        storeInfoCard = findViewById(R.id.storeInfoCard);
        btnViewItems = findViewById(R.id.btnViewItems);
        btnEditStore = findViewById(R.id.btnEditStore);
        btnDeleteStore = findViewById(R.id.btnDeleteStore);
        fab = findViewById(R.id.fab);
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Initialize store manager
        storeManager = StoreManager.getInstance(this);
        markerStoreMap = new HashMap<>();
        
        // Initialize shake detection
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(count -> sortStoresByDistance());
        
        // Setup click listeners
        fab.setOnClickListener(view -> {
            if (isPickingLocation) {
                Snackbar.make(view, "Long press on the map to select a location", Snackbar.LENGTH_LONG).show();
            } else {
                // Open add store activity
                Intent intent = new Intent(StoresMapActivity.this, AddEditStoreActivity.class);
                startActivity(intent);
            }
        });
        
        btnViewItems.setOnClickListener(view -> {
            if (selectedStore != null) {
                // Show a dialog with the store's available items
                AlertDialog.Builder builder = new AlertDialog.Builder(StoresMapActivity.this);
                builder.setTitle("Items at " + selectedStore.getName());
                
                List<String> availableItems = selectedStore.getAvailableItems();
                if (availableItems != null && !availableItems.isEmpty()) {
                    // Create a RecyclerView to display items
                    RecyclerView recyclerView = new RecyclerView(StoresMapActivity.this);
                    recyclerView.setLayoutManager(new LinearLayoutManager(StoresMapActivity.this));
                    StoreItemAdapter adapter = new StoreItemAdapter(StoresMapActivity.this, availableItems);
                    recyclerView.setAdapter(adapter);
                    
                    builder.setView(recyclerView);
                } else {
                    // If no items are available, show a message
                    TextView textView = new TextView(StoresMapActivity.this);
                    textView.setText("No items available for this store.");
                    textView.setPadding(30, 30, 30, 30);
                    textView.setGravity(Gravity.CENTER);
                    
                    builder.setView(textView);
                    
                    // Provide option to add items
                    builder.setPositiveButton("Add Items", (dialog, which) -> {
                        Intent intent = new Intent(StoresMapActivity.this, AddEditStoreActivity.class);
                        intent.putExtra("STORE_ID", selectedStore.getId());
                        startActivity(intent);
                    });
                }
                
                builder.setNegativeButton("Close", null);
                builder.show();
            }
        });
        
        btnEditStore.setOnClickListener(view -> {
            if (selectedStore != null) {
                Intent intent = new Intent(StoresMapActivity.this, AddEditStoreActivity.class);
                intent.putExtra("STORE_ID", selectedStore.getId());
                startActivity(intent);
            }
        });
        
        btnDeleteStore.setOnClickListener(view -> {
            if (selectedStore != null) {
                new AlertDialog.Builder(StoresMapActivity.this)
                        .setTitle("Delete Store")
                        .setMessage("Are you sure you want to delete " + selectedStore.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            storeManager.deleteStore(selectedStore);
                            storeInfoCard.setVisibility(View.GONE);
                            selectedStore = null;
                            Snackbar.make(view, "Store deleted", Snackbar.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        
        // Set up store load listener
        storeManager.setStoreLoadListener(new StoreManager.StoreLoadListener() {
            @Override
            public void onStoresLoaded(List<Store> stores) {
                updateMap();
            }
            
            @Override
            public void onStoreLoadError(Exception error) {
                Snackbar.make(findViewById(R.id.map), "Error loading stores: " + error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Loading stores");
        // Load stores when the activity resumes
        storeManager.loadStores();
        
        // Register shake detector
        if (mAccelerometer != null) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }
    
    @Override
    protected void onPause() {
        // Unregister shake detector to save battery
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
        super.onPause();
    }
    
    /**
     * Manipulates the map once available.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;
        
        try {
            // Set up map UI settings
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            
            // Set up map listeners
            mMap.setOnMarkerClickListener(this);
            mMap.setOnMapLongClickListener(this);
            
            // Enable location if permission is granted
            enableMyLocation();
            
            // Load existing stores on the map
            updateMap();
        } catch (Exception e) {
            Log.e(TAG, "onMapReady: Error setting up map", e);
            showMapErrorView("Error initializing map: " + e.getMessage());
        }
    }
    
    /**
     * Updates the map with all stores from the StoreManager
     */
    private void updateMap() {
        if (mMap == null) {
            Log.e(TAG, "updateMap: Map is null");
            return;
        }
        
        List<Store> stores = storeManager.getStoreList();
        Log.d(TAG, "updateMap: Loading " + stores.size() + " stores");
        
        // Clear existing markers and map
        mMap.clear();
        markerStoreMap.clear();
        
        if (stores.isEmpty()) {
            Log.d(TAG, "updateMap: No stores available, showing message");
            Snackbar.make(findViewById(R.id.map), 
                "No stores found. Long press on the map to add a store.", 
                Snackbar.LENGTH_LONG).show();
            return;
        }
        
        // Add markers for all stores
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidCoordinates = false;
        
        for (Store store : stores) {
            LatLng storeLocation = new LatLng(store.getLatitude(), store.getLongitude());
            
            // Skip invalid coordinates
            if (store.getLatitude() == 0 && store.getLongitude() == 0) {
                Log.d(TAG, "updateMap: Skipping store with invalid coordinates: " + store.getName());
                continue;
            }
            
            hasValidCoordinates = true;
            boundsBuilder.include(storeLocation);
            
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(storeLocation)
                    .title(store.getName())
                    .snippet(store.getAddress());
            
            // Use different colors based on store type or highlight status
            if (highlightShoppingStores && hasShoppingItemsForStore(store.getId())) {
                // Highlight stores with shopping items in magenta
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                markerOptions.zIndex(2.0f); // Make these markers appear on top
                markerOptions.title("🛒 " + store.getName()); // Add shopping cart emoji
            } else if ("Supermarket".equals(store.getStoreType())) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            } else if ("Convenience Store".equals(store.getStoreType())) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            } else if ("Farmers Market".equals(store.getStoreType())) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            }
            
            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                markerStoreMap.put(marker, store);
            }
        }
        
        // Move camera to show all markers
        if (hasValidCoordinates) {
            try {
                Log.d(TAG, "updateMap: Moving camera to show all stores");
                LatLngBounds bounds = boundsBuilder.build();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            } catch (Exception e) {
                Log.e(TAG, "updateMap: Error animating camera", e);
                // If bounds calculation fails, just zoom to the first store
                if (!stores.isEmpty()) {
                    Store firstStore = stores.get(0);
                    LatLng firstLocation = new LatLng(firstStore.getLatitude(), firstStore.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, DEFAULT_ZOOM));
                }
            }
        } else {
            // If no valid coordinates, use default location (e.g., city center)
            Log.d(TAG, "updateMap: No valid coordinates, using default location");
            LatLng defaultLocation = new LatLng(27.7172, 85.3240); // Kathmandu center
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
        }
    }
    
    /**
     * Sorts stores by distance from current location when user shakes the device
     */
    private void sortStoresByDistance() {
        if (mLastLocation == null) {
            // Try to get current location first
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission required to sort by distance", Toast.LENGTH_SHORT).show();
                return;
            }
            
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    mLastLocation = location;
                    performDistanceSort();
                } else {
                    Toast.makeText(this, "Could not get current location", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            performDistanceSort();
        }
    }
    
    private void performDistanceSort() {
        if (mLastLocation == null) return;
        
        // Get list of stores
        List<Store> stores = new ArrayList<>(storeManager.getStoreList());
        
        // Sort by distance
        Collections.sort(stores, (store1, store2) -> {
            float[] results1 = new float[1];
            Location.distanceBetween(
                    mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                    store1.getLatitude(), store1.getLongitude(), results1);
            
            float[] results2 = new float[1];
            Location.distanceBetween(
                    mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                    store2.getLatitude(), store2.getLongitude(), results2);
            
            return Float.compare(results1[0], results2[0]);
        });
        
        // Show first store (closest)
        if (!stores.isEmpty()) {
            Store closest = stores.get(0);
            LatLng storeLocation = new LatLng(closest.getLatitude(), closest.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(storeLocation, DEFAULT_ZOOM));
            
            // Find marker for this store and click it
            for (Map.Entry<Marker, Store> entry : markerStoreMap.entrySet()) {
                if (entry.getValue().getId().equals(closest.getId())) {
                    entry.getKey().showInfoWindow();
                    selectedStore = closest;
                    storeName.setText(selectedStore.getName());
                    storeAddress.setText(selectedStore.getAddress());
                    storeType.setText(selectedStore.getStoreType());
                    storeInfoCard.setVisibility(View.VISIBLE);
                    break;
                }
            }
            
            Snackbar.make(findViewById(R.id.map), 
                    "Showing closest store: " + closest.getName(), 
                    Snackbar.LENGTH_LONG).show();
        }
    }
    
    /**
     * Attempts to enable the My Location layer if the user has granted location permission.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                Log.d(TAG, "enableMyLocation: Enabling my location");
                mMap.setMyLocationEnabled(true);
                
                // Move camera to the user's current location
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "enableMyLocation: Got last location: " + location.getLatitude() + ", " + location.getLongitude());
                        mLastLocation = location;
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_ZOOM));
                    } else {
                        Log.d(TAG, "enableMyLocation: Last location is null");
                    }
                });
            }
        } else {
            Log.d(TAG, "enableMyLocation: Requesting location permission");
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Store store = markerStoreMap.get(marker);
        if (store != null) {
            // If we're picking a location, return this location
            if (isPickingLocation) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("STORE_ID", store.getId());
                resultIntent.putExtra("STORE_NAME", store.getName());
                resultIntent.putExtra("STORE_LATITUDE", store.getLatitude());
                resultIntent.putExtra("STORE_LONGITUDE", store.getLongitude());
                setResult(RESULT_OK, resultIntent);
                finish();
                return true;
            }
            
            // Otherwise, show store details
            selectedStore = store;
            storeName.setText(store.getName());
            storeAddress.setText(store.getAddress());
            storeType.setText(store.getStoreType());
            
            // Show shopping items if this store has any
            if (highlightShoppingStores && hasShoppingItemsForStore(store.getId())) {
                btnViewItems.setText("View Shopping Items");
            } else {
                btnViewItems.setText("View Items");
            }
            
            storeInfoCard.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }
    
    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if (isPickingLocation) {
            // If we're in pick location mode, return the selected location
            Intent result = new Intent();
            result.putExtra("LATITUDE", latLng.latitude);
            result.putExtra("LONGITUDE", latLng.longitude);
            setResult(RESULT_OK, result);
            finish();
        } else {
            // Otherwise start the add store activity with this location
            Intent intent = new Intent(StoresMapActivity.this, AddEditStoreActivity.class);
            intent.putExtra("LATITUDE", latLng.latitude);
            intent.putExtra("LONGITUDE", latLng.longitude);
            startActivity(intent);
        }
    }
    
    /**
     * Check if there are any shopping items for a specific store
     * @param storeId the store ID to check
     * @return true if there are shopping items for this store
     */
    private boolean hasShoppingItemsForStore(String storeId) {
        // In a real implementation, we would query the database
        // For now, we'll just return true for our sample stores to demonstrate the feature
        return storeId != null && (
            storeId.equals("sample1") || 
            storeId.equals("sample2") || 
            storeId.equals("sample3"));
    }
    
    /**
     * Handle map errors by showing an error view with instructions
     */
    private void showMapErrorView(String errorMessage) {
        // Replace map fragment with error view
        Log.e(TAG, "showMapErrorView: " + errorMessage);
        
        // Inflate error view
        View errorView = getLayoutInflater().inflate(R.layout.layout_map_error, null);
        
        // Add to parent view
        ViewGroup mapContainer = findViewById(R.id.mapContainer);
        mapContainer.removeAllViews();
        mapContainer.addView(errorView);
        
        // Set error message
        TextView errorText = errorView.findViewById(R.id.errorText);
        errorText.setText(errorMessage);
        
        // Button to open developer console
        Button btnDeveloperConsole = errorView.findViewById(R.id.btnDeveloperConsole);
        btnDeveloperConsole.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://console.cloud.google.com/google/maps-apis/credentials"));
            startActivity(intent);
        });
        
        // Button to continue with demo mode
        Button btnDemoMode = errorView.findViewById(R.id.btnDemoMode);
        btnDemoMode.setOnClickListener(v -> {
            // Enter demo mode - this will use a list view instead of a map
            switchToDemoMode();
        });
    }
    
    /**
     * Switch to demo mode when map can't be loaded
     */
    private void switchToDemoMode() {
        isDemoMode = true;
        
        // Create sample stores if they don't exist
        storeManager.createSampleStores();
        
        // Inflate demo layout
        View demoView = getLayoutInflater().inflate(R.layout.layout_demo_map, null);
        
        // Add to parent view
        ViewGroup mapContainer = findViewById(R.id.mapContainer);
        mapContainer.removeAllViews();
        mapContainer.addView(demoView);
        
        // Set up RecyclerView with stores
        RecyclerView storeListView = demoView.findViewById(R.id.storeListRecyclerView);
        storeListView.setLayoutManager(new LinearLayoutManager(this));
        
        // Create adapter with current store list
        StoreCardAdapter adapter = new StoreCardAdapter(this, storeManager.getStoreList());
        storeListView.setAdapter(adapter);
        
        // Add button listeners
        Button btnAddNewStore = demoView.findViewById(R.id.btnAddNewStore);
        btnAddNewStore.setOnClickListener(v -> {
            Intent intent = new Intent(StoresMapActivity.this, AddEditStoreActivity.class);
            startActivity(intent);
        });
        
        Button btnConfigureApiKey = demoView.findViewById(R.id.btnConfigureApiKey);
        btnConfigureApiKey.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://console.cloud.google.com/google/maps-apis/credentials"));
            startActivity(intent);
        });
        
        // Update adapter when stores change
        storeManager.setStoreLoadListener(new StoreManager.StoreLoadListener() {
            @Override
            public void onStoresLoaded(List<Store> stores) {
                adapter.updateData(stores);
            }
            
            @Override
            public void onStoreLoadError(Exception error) {
                Toast.makeText(StoresMapActivity.this, 
                        "Error loading stores: " + error.getMessage(), 
                        Toast.LENGTH_LONG).show();
            }
        });
        
        // Show a toast to indicate we're in demo mode
        Toast.makeText(this, "Demo mode activated. Map is not available.", Toast.LENGTH_LONG).show();
    }
}

package com.raka.mealmate;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.raka.mealmate.adapters.ShoppingListAdapter;
import com.raka.mealmate.helpers.StoreManager;
import com.raka.mealmate.helpers.SwipeToGestureCallback;
import com.raka.mealmate.models.Ingredient;
import com.raka.mealmate.models.ShoppingItem;
import com.raka.mealmate.models.MealPlan;
import com.raka.mealmate.models.Recipe;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ShoppingListActivity extends AppCompatActivity implements ShoppingListAdapter.OnItemActionListener {
    private static final String TAG = "ShoppingListActivity";
    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 123;

    private RecyclerView rvShoppingList;
    private TextView tvEmptyList;
    private FloatingActionButton fabSms;
    private FloatingActionButton fabViewMap;
    private ShoppingListAdapter adapter;
    private List<ShoppingItem> shoppingItems;
    private DatabaseReference mDatabase;
    private String userId;
    private double totalPrice = 0.0;
    private TextView tvTotalPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Shopping List");

        // Initialize views
        initializeViews();

        // Load shopping list
        loadShoppingList();
    }

    private void initializeViews() {
        rvShoppingList = findViewById(R.id.rvShoppingList);
        tvEmptyList = findViewById(R.id.tvEmptyList);
        fabSms = findViewById(R.id.fabSms);
        fabViewMap = findViewById(R.id.fabViewMap);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);

        // Initialize RecyclerView
        shoppingItems = new ArrayList<>();
        adapter = new ShoppingListAdapter(this, shoppingItems);
        adapter.setOnItemActionListener(this);
        rvShoppingList.setLayoutManager(new LinearLayoutManager(this));
        rvShoppingList.setAdapter(adapter);
        
        // Setup swipe gestures
        SwipeToGestureCallback swipeCallback = new SwipeToGestureCallback(this, new SwipeToGestureCallback.SwipeActionListener() {
            @Override
            public void onSwipeDelete(int position) {
                // Check if the item at this position is a ShoppingItem
                if (position >= 0 && position < adapter.getItemCount() && 
                    adapter.getItemViewType(position) == 1) { // 1 = TYPE_ITEM
                    
                    // Get the ShoppingItem from the adapter's items list
                    Object item = adapter.getItemAt(position);
                    if (item instanceof ShoppingItem) {
                        onDeleteItem((ShoppingItem) item);
                    }
                }
            }

            @Override
            public void onSwipeToggleCheck(int position) {
                // Check if the item at this position is a ShoppingItem
                if (position >= 0 && position < adapter.getItemCount() && 
                    adapter.getItemViewType(position) == 1) { // 1 = TYPE_ITEM
                    
                    // Get the ShoppingItem from the adapter's items list
                    Object item = adapter.getItemAt(position);
                    if (item instanceof ShoppingItem) {
                        ShoppingItem shoppingItem = (ShoppingItem) item;
                        onItemChecked(shoppingItem, !shoppingItem.isPurchased());
                    }
                }
            }
        });
        
        // Attach swipe handler to RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(rvShoppingList);

        // Setup SMS share button
        fabSms.setOnClickListener(v -> shareBySms());
        
        // Setup View Map button
        fabViewMap.setOnClickListener(v -> viewStoresOnMap());
    }

    private void loadShoppingList() {
        mDatabase.child("mealPlans").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Double> ingredientMap = new HashMap<>();
                Map<String, String> ingredientDetails = new HashMap<>();
                Random random = new Random(); // Initialize Random for generating prices

                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot mealSnapshot : dateSnapshot.getChildren()) {
                        try {
                            MealPlan mealPlan = mealSnapshot.getValue(MealPlan.class);
                            if (mealPlan != null && mealPlan.getRecipe() != null) {
                                Recipe recipe = mealPlan.getRecipe();
                                if (recipe.getIngredients() != null) {
                                    for (Ingredient ingredient : recipe.getIngredients()) {
                                        String category = standardizeCategory(ingredient.getCategory());
                                        String key = ingredient.getName().trim().toLowerCase();

                                        // Store ingredient details
                                        ingredientDetails.put(key, category + "|" + ingredient.getUnit());

                                        // Add amounts
                                        double amount;
                                        try {
                                            amount = ingredient.getAmount();
                                        } catch (Exception e) {
                                            try {
                                                String amountStr = String.valueOf(ingredient.getAmount());
                                                amount = Double.parseDouble(amountStr);
                                            } catch (NumberFormatException ex) {
                                                Log.e(TAG, "Error parsing amount for " + ingredient.getName() + ": " + ex.getMessage());
                                                amount = 1.0;
                                            }
                                        }

                                        double currentAmount = ingredientMap.getOrDefault(key, 0.0);
                                        ingredientMap.put(key, currentAmount + amount);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing meal plan: " + e.getMessage());
                        }
                    }
                }

                // Convert ingredient map to shopping items
                shoppingItems.clear();
                totalPrice = 0.0;

                for (Map.Entry<String, Double> entry : ingredientMap.entrySet()) {
                    String key = entry.getKey();
                    String details = ingredientDetails.get(key);
                    if (details != null) {
                        String[] parts = details.split("\\|");
                        if (parts.length == 2) {
                            try {
                                String name = key.substring(0, 1).toUpperCase() + key.substring(1);
                                ShoppingItem item = new ShoppingItem(name, entry.getValue(), parts[1], parts[0]);

                                // Generate a random price between 10.0 and 500.0 Nrs
                                double randomPrice = 10.0 + (500.0 - 10.0) * random.nextDouble();
                                item.setPrice(randomPrice); // Set random price in Nrs
                                totalPrice += item.getPrice();
                                shoppingItems.add(item);
                            } catch (Exception e) {
                                Log.e(TAG, "Error creating shopping item: " + e.getMessage());
                            }
                        }
                    }
                }

                // Update UI
                updateUI();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading shopping list: " + databaseError.getMessage());
                Toast.makeText(ShoppingListActivity.this, "Error loading shopping list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String standardizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Other";
        }

        category = category.toLowerCase().trim();
        switch (category) {
            case "vegetable":
            case "vegetables":
            case "veg":
                return "Vegetables";
            case "protein":
            case "proteins":
            case "meat":
                return "Proteins";
            case "grain":
            case "grains":
            case "carb":
            case "carbs":
                return "Grains";
            case "dairy":
            case "dairy products":
                return "Dairy";
            case "spice":
            case "spices":
            case "seasoning":
            case "seasonings":
                return "Spices & Seasonings";
            default:
                return category.substring(0, 1).toUpperCase() + category.substring(1);
        }
    }

    private void updateUI() {
        if (shoppingItems.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            rvShoppingList.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            rvShoppingList.setVisibility(View.VISIBLE);
            adapter = new ShoppingListAdapter(this, shoppingItems);
            adapter.setOnItemActionListener(this);
            rvShoppingList.setAdapter(adapter);
        }

        // Update total price with Nrs currency
        tvTotalPrice.setText(String.format("Total: Nrs %.2f", totalPrice));
    }

    @Override
    public void onEditItem(ShoppingItem item) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_shopping_item, null);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);

        etAmount.setText(String.valueOf(item.getAmount()));
        etPrice.setText(String.format("%.2f", item.getPrice()));

        new AlertDialog.Builder(this)
                .setTitle("Edit " + item.getName())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double newAmount = Double.parseDouble(etAmount.getText().toString());
                        double newPrice = Double.parseDouble(etPrice.getText().toString());

                        item.setAmount(newAmount);
                        item.setPrice(newPrice);

                        updateTotalPrice();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDeleteItem(ShoppingItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete " + item.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    shoppingItems.remove(item);
                    // Create new adapter with updated list
                    adapter = new ShoppingListAdapter(this, shoppingItems);
                    adapter.setOnItemActionListener(this);
                    rvShoppingList.setAdapter(adapter);
                    updateTotalPrice();

                    // Show empty state if no items left
                    if (shoppingItems.isEmpty()) {
                        tvEmptyList.setVisibility(View.VISIBLE);
                        rvShoppingList.setVisibility(View.GONE);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onItemChecked(ShoppingItem item, boolean isChecked) {
        item.setPurchased(isChecked);
        // Here you would typically update the database
    }

    private void updateTotalPrice() {
        totalPrice = 0.0;
        for (ShoppingItem item : shoppingItems) {
            totalPrice += item.getPrice();
        }
        tvTotalPrice.setText(String.format("Total: Nrs %.2f", totalPrice));
    }

    private void shareBySms() {
        // Check for permissions first
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_CODE);
            return;
        }

        // Create intent to pick a contact
        try {
            Intent pickContact = new Intent(Intent.ACTION_PICK);
            pickContact.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(pickContact, PICK_CONTACT_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening contacts", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareBySms();
            } else {
                Toast.makeText(this, "Permission required to access contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            try {
                Uri contactUri = data.getData();
                if (contactUri == null) {
                    Toast.makeText(this, "No contact selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get the contact's phone number
                String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                try (Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        String number = cursor.getString(numberIndex);

                        if (number != null && !number.isEmpty()) {
                            // Format the message
                            String message = formatShoppingListForSms();

                            // Create SMS intent
                            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                            smsIntent.setData(Uri.parse("smsto:" + Uri.encode(number)));
                            smsIntent.putExtra("sms_body", message);

                            // Start SMS activity
                            try {
                                startActivity(smsIntent);
                            } catch (Exception e) {
                                Toast.makeText(this, "Error opening SMS app", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No phone number found for selected contact", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error accessing contact information", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String formatShoppingListForSms() {
        StringBuilder sms = new StringBuilder("Shopping List:\n\n");

        // Group items by category
        Map<String, List<ShoppingItem>> categoryMap = new HashMap<>();
        for (ShoppingItem item : shoppingItems) {
            categoryMap.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
        }

        // Build SMS text
        for (Map.Entry<String, List<ShoppingItem>> entry : categoryMap.entrySet()) {
            sms.append(entry.getKey()).append(":\n");
            for (ShoppingItem item : entry.getValue()) {
                sms.append("- ")
                        .append(item.getName())
                        .append(": ")
                        .append(String.format("%.1f", item.getAmount()))
                        .append(" ")
                        .append(item.getUnit())
                        .append(" (Nrs ")
                        .append(String.format("%.2f", item.getPrice()))
                        .append(")\n");
            }
            sms.append("\n");
        }

        sms.append("\nTotal: Nrs ").append(String.format("%.2f", totalPrice));
        sms.append("\n\nSent from MealMate");

        return sms.toString();
    }

    /**
     * Open the map view to see stores with shopping items
     */
    private void viewStoresOnMap() {
        // Initialize store manager (if needed for checking stores)
        StoreManager storeManager = StoreManager.getInstance(this);
        
        // Check if we have any shopping items with store associations
        boolean hasStoreItems = false;
        for (ShoppingItem item : shoppingItems) {
            if (item.getStoreId() != null && !item.getStoreId().isEmpty()) {
                hasStoreItems = true;
                break;
            }
        }
        
        if (hasStoreItems) {
            Toast.makeText(this, "Showing stores with your shopping items", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Opening store map", Toast.LENGTH_SHORT).show();
        }
        
        // Open the map activity
        Intent intent = new Intent(this, StoresMapActivity.class);
        // Add a flag to highlight stores that have shopping items
        intent.putExtra("HIGHLIGHT_SHOPPING_STORES", true);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
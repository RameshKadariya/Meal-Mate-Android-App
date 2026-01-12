package com.raka.mealmate.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raka.mealmate.AddEditStoreActivity;
import com.raka.mealmate.R;
import com.raka.mealmate.helpers.StoreManager;
import com.raka.mealmate.models.Store;

import java.util.List;

public class StoreCardAdapter extends RecyclerView.Adapter<StoreCardAdapter.ViewHolder> {

    private final List<Store> stores;
    private final Context context;
    private final StoreManager storeManager;

    public StoreCardAdapter(Context context, List<Store> stores) {
        this.context = context;
        this.stores = stores;
        this.storeManager = StoreManager.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_store_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Store store = stores.get(position);
        
        holder.storeName.setText(store.getName());
        holder.storeAddress.setText(store.getAddress());
        holder.storeType.setText(store.getStoreType());
        holder.storeCoordinates.setText(String.format("Location: %.4f, %.4f", 
                store.getLatitude(), store.getLongitude()));
        
        // Set up button click listeners
        holder.btnViewItems.setOnClickListener(v -> {
            // View items functionality
            // For now, just show the note as a hint of what items might be there
            if (store.getNotes() != null && !store.getNotes().isEmpty()) {
                holder.storeAddress.setText(store.getNotes());
            }
        });
        
        holder.btnEditStore.setOnClickListener(v -> {
            // Edit store functionality
            Intent intent = new Intent(context, AddEditStoreActivity.class);
            intent.putExtra("STORE_ID", store.getId());
            context.startActivity(intent);
        });
        
        holder.btnDeleteStore.setOnClickListener(v -> {
            // Delete store functionality
            storeManager.deleteStore(store);
        });
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    public void updateData(List<Store> newStores) {
        stores.clear();
        stores.addAll(newStores);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView storeName, storeAddress, storeType, storeCoordinates;
        Button btnViewItems, btnEditStore, btnDeleteStore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            storeName = itemView.findViewById(R.id.storeName);
            storeAddress = itemView.findViewById(R.id.storeAddress);
            storeType = itemView.findViewById(R.id.storeType);
            storeCoordinates = itemView.findViewById(R.id.storeCoordinates);
            btnViewItems = itemView.findViewById(R.id.btnViewItems);
            btnEditStore = itemView.findViewById(R.id.btnEditStore);
            btnDeleteStore = itemView.findViewById(R.id.btnDeleteStore);
        }
    }
}

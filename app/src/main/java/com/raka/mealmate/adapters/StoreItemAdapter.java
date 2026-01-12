package com.raka.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raka.mealmate.R;

import java.util.List;

/**
 * Adapter for displaying available items at a store.
 */
public class StoreItemAdapter extends RecyclerView.Adapter<StoreItemAdapter.ItemViewHolder> {
    
    private final Context context;
    private final List<String> items;
    private OnDeleteClickListener onDeleteClickListener;
    
    public StoreItemAdapter(Context context, List<String> items) {
        this.context = context;
        this.items = items;
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }
    
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }
    
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_store_item, parent, false);
        return new ItemViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        String item = items.get(position);
        holder.textViewItem.setText(item);
        
        holder.btnDeleteItem.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(holder.getAdapterPosition());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView textViewItem;
        ImageButton btnDeleteItem;
        
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewItem = itemView.findViewById(R.id.textViewItem);
            btnDeleteItem = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}

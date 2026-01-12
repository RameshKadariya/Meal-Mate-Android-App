package com.raka.mealmate.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.raka.mealmate.R;
import com.raka.mealmate.models.ShoppingItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ShoppingListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    
    private final Context context;
    private final List<Object> items; // Can be either String (header) or ShoppingItem
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onEditItem(ShoppingItem item);
        void onDeleteItem(ShoppingItem item);
        void onItemChecked(ShoppingItem item, boolean isChecked);
    }

    public ShoppingListAdapter(Context context, List<ShoppingItem> shoppingItems) {
        this.context = context;
        this.items = new ArrayList<>();
        
        // Group items by category
        Map<String, List<ShoppingItem>> categoryMap = new TreeMap<>();
        for (ShoppingItem item : shoppingItems) {
            String category = item.getCategory();
            if (!categoryMap.containsKey(category)) {
                categoryMap.put(category, new ArrayList<>());
            }
            categoryMap.get(category).add(item);
        }
        
        // Sort items within each category and create final list
        for (Map.Entry<String, List<ShoppingItem>> entry : categoryMap.entrySet()) {
            // Add category header
            items.add(entry.getKey());
            
            // Sort items by name
            List<ShoppingItem> categoryItems = entry.getValue();
            Collections.sort(categoryItems, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            
            // Add sorted items
            items.addAll(categoryItems);
        }
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind((ShoppingItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * Returns the item at the specified position in the data set
     * @param position Position to get item from
     * @return The object at the specified position
     */
    public Object getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryHeader;

        HeaderViewHolder(View itemView) {
            super(itemView);
            tvCategoryHeader = itemView.findViewById(R.id.tvCategoryHeader);
        }

        void bind(String category) {
            tvCategoryHeader.setText(category);
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName;
        TextView tvAmount;
        TextView tvPrice;
        CheckBox cbPurchased;
        ImageButton btnEdit;
        ImageButton btnDelete;

        ItemViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            cbPurchased = itemView.findViewById(R.id.cbPurchased);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(ShoppingItem item) {
            tvItemName.setText(item.getName());
            tvAmount.setText(String.format("%.1f %s", item.getAmount(), item.getUnit()));
            tvPrice.setText(String.format("Nrs%.2f", item.getPrice()));
            cbPurchased.setChecked(item.isPurchased());
            
            if (listener != null) {
                btnEdit.setOnClickListener(v -> listener.onEditItem(item));
                btnDelete.setOnClickListener(v -> listener.onDeleteItem(item));
                cbPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> 
                    listener.onItemChecked(item, isChecked));
            }
        }
    }
}

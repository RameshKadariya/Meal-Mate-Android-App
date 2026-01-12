package com.raka.mealmate.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.raka.mealmate.R;
import com.raka.mealmate.models.Ingredient;

import java.util.List;

public class EditableIngredientAdapter extends RecyclerView.Adapter<EditableIngredientAdapter.IngredientViewHolder> {

    private List<Ingredient> ingredients;
    private OnIngredientRemovedListener listener;

    public interface OnIngredientRemovedListener {
        void onIngredientRemoved(int position);
    }

    public EditableIngredientAdapter(List<Ingredient> ingredients, OnIngredientRemovedListener listener) {
        this.ingredients = ingredients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IngredientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_editable_ingredient, parent, false);
        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IngredientViewHolder holder, int position) {
        Ingredient ingredient = ingredients.get(position);
        
        holder.nameEditText.setText(ingredient.getName());
        holder.amountEditText.setText(String.valueOf(ingredient.getAmount()));
        holder.unitEditText.setText(ingredient.getUnit());
        holder.categoryEditText.setText(ingredient.getCategory());
        
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIngredientRemoved(holder.getAdapterPosition());
            }
        });
        
        // Setup TextWatchers to update the ingredient object when text changes
        setupTextWatcher(holder.nameEditText, (s) -> ingredient.setName(s.toString()));
        setupTextWatcher(holder.amountEditText, (s) -> {
            try {
                ingredient.setAmount(Double.parseDouble(s.toString()));
            } catch (NumberFormatException e) {
                ingredient.setAmount(0);
            }
        });
        setupTextWatcher(holder.unitEditText, (s) -> ingredient.setUnit(s.toString()));
        setupTextWatcher(holder.categoryEditText, (s) -> ingredient.setCategory(s.toString()));
    }

    private void setupTextWatcher(TextInputEditText editText, TextChangedListener listener) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                listener.onTextChanged(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    interface TextChangedListener {
        void onTextChanged(CharSequence s);
    }

    @Override
    public int getItemCount() {
        return ingredients.size();
    }

    static class IngredientViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText nameEditText;
        TextInputEditText amountEditText;
        TextInputEditText unitEditText;
        TextInputEditText categoryEditText;
        ImageButton removeButton;

        IngredientViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.ingredientNameEditText);
            amountEditText = itemView.findViewById(R.id.amountEditText);
            unitEditText = itemView.findViewById(R.id.unitEditText);
            categoryEditText = itemView.findViewById(R.id.categoryEditText);
            removeButton = itemView.findViewById(R.id.removeIngredientButton);
        }
    }
}

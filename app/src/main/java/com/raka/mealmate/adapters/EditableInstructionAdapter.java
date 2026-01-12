package com.raka.mealmate.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.raka.mealmate.R;

import java.util.List;

public class EditableInstructionAdapter extends RecyclerView.Adapter<EditableInstructionAdapter.InstructionViewHolder> {

    private List<String> instructions;
    private OnInstructionRemovedListener listener;

    public interface OnInstructionRemovedListener {
        void onInstructionRemoved(int position);
    }

    public EditableInstructionAdapter(List<String> instructions, OnInstructionRemovedListener listener) {
        this.instructions = instructions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InstructionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_editable_instruction, parent, false);
        return new InstructionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstructionViewHolder holder, int position) {
        String instruction = instructions.get(position);
        
        holder.stepNumberTextView.setText((position + 1) + ".");
        holder.instructionEditText.setText(instruction);
        
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onInstructionRemoved(holder.getAdapterPosition());
            }
        });
        
        // Setup TextWatcher to update the instruction when text changes
        holder.instructionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                instructions.set(holder.getAdapterPosition(), s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public int getItemCount() {
        return instructions.size();
    }

    static class InstructionViewHolder extends RecyclerView.ViewHolder {
        TextView stepNumberTextView;
        TextInputEditText instructionEditText;
        ImageButton removeButton;

        InstructionViewHolder(View itemView) {
            super(itemView);
            stepNumberTextView = itemView.findViewById(R.id.stepNumberTextView);
            instructionEditText = itemView.findViewById(R.id.instructionEditText);
            removeButton = itemView.findViewById(R.id.removeInstructionButton);
        }
    }
}

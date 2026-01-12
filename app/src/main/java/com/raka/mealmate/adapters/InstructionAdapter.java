package com.raka.mealmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raka.mealmate.R;

import java.util.List;

public class InstructionAdapter extends RecyclerView.Adapter<InstructionAdapter.ViewHolder> {

    private final List<String> instructions;

    public InstructionAdapter(List<String> instructions) {
        this.instructions = instructions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_instruction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String instruction = instructions.get(position);
        holder.stepNumberText.setText(String.format("%d.", position + 1));
        holder.instructionText.setText(instruction);
    }

    @Override
    public int getItemCount() {
        return instructions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView stepNumberText;
        final TextView instructionText;

        ViewHolder(View view) {
            super(view);
            stepNumberText = view.findViewById(R.id.stepNumberText);
            instructionText = view.findViewById(R.id.instructionText);
        }
    }
} 
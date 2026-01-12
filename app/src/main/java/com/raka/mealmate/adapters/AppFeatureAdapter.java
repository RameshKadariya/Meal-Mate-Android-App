package com.raka.mealmate.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.raka.mealmate.R;
import com.raka.mealmate.models.AppFeature;

import java.util.List;

public class AppFeatureAdapter extends RecyclerView.Adapter<AppFeatureAdapter.FeatureViewHolder> {

    private List<AppFeature> features;
    private OnFeatureClickListener listener;

    public interface OnFeatureClickListener {
        void onFeatureClick(AppFeature feature);
    }

    public AppFeatureAdapter(List<AppFeature> features, OnFeatureClickListener listener) {
        this.features = features;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_feature, parent, false);
        return new FeatureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
        AppFeature feature = features.get(position);
        holder.featureIcon.setImageResource(feature.getIconResourceId());
        holder.featureName.setText(feature.getName());
        
        holder.featureCard.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFeatureClick(feature);
            }
        });
    }

    @Override
    public int getItemCount() {
        return features.size();
    }

    static class FeatureViewHolder extends RecyclerView.ViewHolder {
        CardView featureCard;
        ImageView featureIcon;
        TextView featureName;

        FeatureViewHolder(@NonNull View itemView) {
            super(itemView);
            featureCard = itemView.findViewById(R.id.featureCard);
            featureIcon = itemView.findViewById(R.id.featureIcon);
            featureName = itemView.findViewById(R.id.featureName);
        }
    }
}

package com.chatbox.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chatbox.app.R;
import com.chatbox.app.data.entity.ProviderSettings;

import java.util.List;

/**
 * ProviderAdapter - RecyclerView adapter for provider list
 * 
 * This adapter displays AI providers in a list format.
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder> {
    
    /**
     * List of providers
     */
    private List<ProviderSettings> providers;
    
    /**
     * Click listener
     */
    private OnProviderClickListener listener;
    
    /**
     * Constructor
     * 
     * @param listener Click listener
     * @param providers Initial provider list
     */
    public ProviderAdapter(OnProviderClickListener listener, List<ProviderSettings> providers) {
        this.listener = listener;
        this.providers = providers;
    }
    
    /**
     * Update the provider list
     * 
     * @param newProviders New provider list
     */
    public void updateProviders(List<ProviderSettings> newProviders) {
        this.providers = newProviders;
        notifyDataSetChanged();
    }
    
    /**
     * Set the click listener
     * 
     * @param listener Click listener
     */
    public void setOnProviderClickListener(OnProviderClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_provider, parent, false);
        return new ProviderViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        ProviderSettings provider = providers.get(position);
        holder.bind(provider);
    }
    
    @Override
    public int getItemCount() {
        return providers != null ? providers.size() : 0;
    }
    
    // =========================================================================
    // ViewHolder
    // =========================================================================
    
    /**
     * ViewHolder for provider items
     */
    class ProviderViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView textName;
        private final TextView textStatus;
        private final ImageView imageStatus;
        
        ProviderViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_provider_name);
            textStatus = itemView.findViewById(R.id.text_provider_status);
            imageStatus = itemView.findViewById(R.id.image_provider_status);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onProviderClick(providers.get(position));
                }
            });
        }
        
        /**
         * Bind provider data to views
         * 
         * @param provider The provider to display
         */
        void bind(ProviderSettings provider) {
            textName.setText(provider.getDisplayName());
            
            if (provider.isConfigured()) {
                textStatus.setText(R.string.provider_configured);
                textStatus.setTextColor(itemView.getContext().getColor(R.color.status_success));
                imageStatus.setImageResource(R.drawable.ic_check_circle);
                imageStatus.setColorFilter(itemView.getContext().getColor(R.color.status_success));
            } else {
                textStatus.setText(R.string.provider_not_configured);
                textStatus.setTextColor(itemView.getContext().getColor(R.color.status_error));
                imageStatus.setImageResource(R.drawable.ic_error);
                imageStatus.setColorFilter(itemView.getContext().getColor(R.color.status_error));
            }
        }
    }
    
    // =========================================================================
    // Interface
    // =========================================================================
    
    /**
     * Interface for provider click events
     */
    public interface OnProviderClickListener {
        /**
         * Called when a provider is clicked
         * 
         * @param provider The clicked provider
         */
        void onProviderClick(ProviderSettings provider);
    }
}

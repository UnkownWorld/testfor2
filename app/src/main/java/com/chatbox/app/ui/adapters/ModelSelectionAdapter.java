package com.chatbox.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chatbox.app.R;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ModelSelectionAdapter - Adapter for model selection list
 */
public class ModelSelectionAdapter extends RecyclerView.Adapter<ModelSelectionAdapter.ViewHolder> {
    
    private List<String> allModels = new ArrayList<>();
    private Set<String> selectedModels = new HashSet<>();
    private Set<String> customModels = new HashSet<>();
    private OnModelSelectionListener listener;
    
    public void setModels(List<String> models, Set<String> selected) {
        this.allModels = new ArrayList<>(models);
        this.selectedModels = new HashSet<>(selected);
        notifyDataSetChanged();
    }
    
    public void addCustomModel(String model) {
        if (!allModels.contains(model)) {
            allModels.add(0, model); // Add to beginning
            customModels.add(model);
            selectedModels.add(model);
            notifyItemInserted(0);
        }
    }
    
    public void removeModel(String model) {
        int index = allModels.indexOf(model);
        if (index >= 0) {
            allModels.remove(index);
            selectedModels.remove(model);
            customModels.remove(model);
            notifyItemRemoved(index);
        }
    }
    
    public List<String> getSelectedModels() {
        return new ArrayList<>(selectedModels);
    }
    
    public void setOnModelSelectionListener(OnModelSelectionListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_model, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String model = allModels.get(position);
        holder.textModelName.setText(model);
        holder.checkboxSelected.setChecked(selectedModels.contains(model));
        
        // Show delete button only for custom models
        holder.btnDelete.setVisibility(customModels.contains(model) ? View.VISIBLE : View.GONE);
        
        holder.checkboxSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedModels.add(model);
            } else {
                selectedModels.remove(model);
            }
            if (listener != null) {
                listener.onSelectionChanged(getSelectedModels());
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            removeModel(model);
            if (listener != null) {
                listener.onSelectionChanged(getSelectedModels());
            }
        });
        
        holder.itemView.setOnClickListener(v -> {
            holder.checkboxSelected.toggle();
        });
    }
    
    @Override
    public int getItemCount() {
        return allModels.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCheckBox checkboxSelected;
        TextView textModelName;
        ImageButton btnDelete;
        
        ViewHolder(View itemView) {
            super(itemView);
            checkboxSelected = itemView.findViewById(R.id.checkbox_selected);
            textModelName = itemView.findViewById(R.id.text_model_name);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
    
    public interface OnModelSelectionListener {
        void onSelectionChanged(List<String> selectedModels);
    }
}

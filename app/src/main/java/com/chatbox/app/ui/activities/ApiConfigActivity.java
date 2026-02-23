package com.chatbox.app.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chatbox.app.R;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.databinding.ActivityApiConfigBinding;
import com.chatbox.app.ui.adapters.ProviderAdapter;
import com.chatbox.app.ui.viewmodels.ApiConfigViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * ApiConfigActivity - Activity for configuring API settings
 * 
 * This activity allows users to:
 * - View configured providers
 * - Add/edit provider settings
 * - Test API connections
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ApiConfigActivity extends AppCompatActivity implements ProviderAdapter.OnProviderClickListener {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "ApiConfigActivity";
    
    /**
     * View binding
     */
    private ActivityApiConfigBinding binding;
    
    /**
     * ViewModel
     */
    private ApiConfigViewModel viewModel;
    
    /**
     * Provider adapter
     */
    private ProviderAdapter providerAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        binding = ActivityApiConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.api_configuration);
        }
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ApiConfigViewModel.class);
        
        // Initialize RecyclerView
        setupRecyclerView();
        
        // Observe data
        observeData();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Set up the RecyclerView for providers
     */
    private void setupRecyclerView() {
        providerAdapter = new ProviderAdapter(this, new ArrayList<>());
        providerAdapter.setOnProviderClickListener(this);
        
        binding.recyclerViewProviders.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewProviders.setAdapter(providerAdapter);
    }
    
    /**
     * Observe LiveData from ViewModel
     */
    private void observeData() {
        viewModel.getProviders().observe(this, providers -> {
            if (providers != null) {
                providerAdapter.updateProviders(providers);
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    // =========================================================================
    // ProviderAdapter.OnProviderClickListener
    // =========================================================================
    
    @Override
    public void onProviderClick(ProviderSettings provider) {
        showProviderConfigDialog(provider);
    }
    
    /**
     * Show configuration dialog for a provider
     * 
     * @param provider The provider to configure
     */
    private void showProviderConfigDialog(ProviderSettings provider) {
        // Create dialog view
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_provider_config, null);
        
        TextInputLayout apiKeyLayout = dialogView.findViewById(R.id.layout_api_key);
        TextInputEditText apiKeyInput = dialogView.findViewById(R.id.input_api_key);
        TextInputLayout apiHostLayout = dialogView.findViewById(R.id.layout_api_host);
        TextInputEditText apiHostInput = dialogView.findViewById(R.id.input_api_host);
        
        // Set existing values
        if (provider.getApiKey() != null) {
            apiKeyInput.setText(provider.getApiKey());
        }
        if (provider.getApiHost() != null) {
            apiHostInput.setText(provider.getApiHost());
        } else {
            apiHostInput.setText(ProviderSettings.getDefaultHost(provider.getProvider()));
        }
        
        // Show dialog
        new MaterialAlertDialogBuilder(this)
            .setTitle(provider.getDisplayName())
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String apiKey = apiKeyInput.getText() != null ? apiKeyInput.getText().toString().trim() : "";
                String apiHost = apiHostInput.getText() != null ? apiHostInput.getText().toString().trim() : "";
                
                if (apiKey.isEmpty()) {
                    Toast.makeText(this, R.string.error_provider_not_configured, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Update provider settings
                provider.setApiKey(apiKey);
                provider.setApiHost(apiHost);
                viewModel.saveProviderSettings(provider);
                
                Toast.makeText(this, R.string.provider_configured, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.test_connection, (dialog, which) -> {
                // Test connection
                testConnection(provider);
            })
            .show();
    }
    
    /**
     * Test API connection
     * 
     * @param provider The provider to test
     */
    private void testConnection(ProviderSettings provider) {
        Toast.makeText(this, R.string.testing_connection, Toast.LENGTH_SHORT).show();
        
        viewModel.testConnection(provider, new ApiConfigViewModel.TestCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(ApiConfigActivity.this, R.string.connection_successful, Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ApiConfigActivity.this, getString(R.string.connection_failed) + ": " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}

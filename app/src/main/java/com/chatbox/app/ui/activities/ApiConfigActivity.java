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

import com.chatbox.app.R;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.databinding.ActivityApiConfigBinding;
import com.chatbox.app.ui.adapters.ProviderAdapter;
import com.chatbox.app.ui.viewmodels.ApiConfigViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.UUID;

/**
 * ApiConfigActivity - Activity for configuring API settings
 * 
 * This activity allows users to:
 * - View configured providers
 * - Add/edit provider settings
 * - Add custom providers
 * - Test API connections
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ApiConfigActivity extends AppCompatActivity implements ProviderAdapter.OnProviderClickListener {
    
    private static final String TAG = "ApiConfigActivity";
    
    private ActivityApiConfigBinding binding;
    private ApiConfigViewModel viewModel;
    private ProviderAdapter providerAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        binding = ActivityApiConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.api_configuration);
        }
        
        viewModel = new ViewModelProvider(this).get(ApiConfigViewModel.class);
        
        setupRecyclerView();
        setupFab();
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
    
    private void setupRecyclerView() {
        providerAdapter = new ProviderAdapter(this, new ArrayList<>());
        providerAdapter.setOnProviderClickListener(this);
        
        binding.recyclerViewProviders.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewProviders.setAdapter(providerAdapter);
    }
    
    private void setupFab() {
        binding.fabAddProvider.setOnClickListener(v -> showAddCustomProviderDialog());
    }
    
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
    
    @Override
    public void onProviderClick(ProviderSettings provider) {
        showProviderConfigDialog(provider);
    }
    
    private void showProviderConfigDialog(ProviderSettings provider) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_provider_config, null);
        
        TextInputEditText apiKeyInput = dialogView.findViewById(R.id.input_api_key);
        TextInputEditText apiHostInput = dialogView.findViewById(R.id.input_api_host);
        
        if (provider.getApiKey() != null) {
            apiKeyInput.setText(provider.getApiKey());
        }
        if (provider.getApiHost() != null) {
            apiHostInput.setText(provider.getApiHost());
        } else {
            apiHostInput.setText(ProviderSettings.getDefaultHost(provider.getProvider()));
        }
        
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
                
                provider.setApiKey(apiKey);
                provider.setApiHost(apiHost);
                viewModel.saveProviderSettings(provider);
                
                Toast.makeText(this, R.string.provider_configured, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.test_connection, (dialog, which) -> {
                testConnection(provider);
            })
            .show();
    }
    
    private void showAddCustomProviderDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_custom_provider, null);
        
        TextInputEditText nameInput = dialogView.findViewById(R.id.input_provider_name);
        TextInputEditText hostInput = dialogView.findViewById(R.id.input_api_host);
        TextInputEditText keyInput = dialogView.findViewById(R.id.input_api_key);
        TextInputEditText modelInput = dialogView.findViewById(R.id.input_default_model);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_custom_provider)
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                String host = hostInput.getText() != null ? hostInput.getText().toString().trim() : "";
                String key = keyInput.getText() != null ? keyInput.getText().toString().trim() : "";
                String model = modelInput.getText() != null ? modelInput.getText().toString().trim() : "";
                
                if (name.isEmpty() || host.isEmpty() || key.isEmpty()) {
                    Toast.makeText(this, R.string.error_provider_not_configured, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Create custom provider with unique ID
                String customId = "custom_" + UUID.randomUUID().toString().substring(0, 8);
                ProviderSettings customProvider = new ProviderSettings(customId);
                customProvider.setDisplayName(name);
                customProvider.setApiHost(host);
                customProvider.setApiKey(key);
                customProvider.setDefaultModel(model.isEmpty() ? "gpt-3.5-turbo" : model);
                
                viewModel.saveProviderSettings(customProvider);
                Toast.makeText(this, R.string.provider_configured, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
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

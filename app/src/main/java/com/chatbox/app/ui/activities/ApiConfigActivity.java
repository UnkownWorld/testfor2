package com.chatbox.app.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chatbox.app.R;
import com.chatbox.app.api.ModelsService;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.databinding.ActivityApiConfigBinding;
import com.chatbox.app.databinding.DialogModelSelectionBinding;
import com.chatbox.app.ui.adapters.ModelSelectionAdapter;
import com.chatbox.app.ui.viewmodels.ApiConfigViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ApiConfigActivity - Activity for configuring API settings
 */
public class ApiConfigActivity extends AppCompatActivity implements com.chatbox.app.ui.adapters.ProviderAdapter.OnProviderClickListener {
    
    private static final String TAG = "ApiConfigActivity";
    
    private ActivityApiConfigBinding binding;
    private ApiConfigViewModel viewModel;
    private com.chatbox.app.ui.adapters.ProviderAdapter providerAdapter;
    private ModelsService modelsService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        binding = ActivityApiConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.api_configuration);
        }
        
        viewModel = new ViewModelProvider(this).get(ApiConfigViewModel.class);
        modelsService = new ModelsService();
        
        setupRecyclerView();
        setupFab();
        observeData();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void setupRecyclerView() {
        providerAdapter = new com.chatbox.app.ui.adapters.ProviderAdapter(this, new ArrayList<>());
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
        TextInputEditText apiPathInput = dialogView.findViewById(R.id.input_api_path);
        RadioGroup apiModeGroup = dialogView.findViewById(R.id.radio_api_mode);
        RadioButton radioChat = dialogView.findViewById(R.id.radio_mode_chat);
        RadioButton radioResponses = dialogView.findViewById(R.id.radio_mode_responses);
        
        // Load existing settings
        if (provider.getApiKey() != null) {
            apiKeyInput.setText(provider.getApiKey());
        }
        if (provider.getApiHost() != null) {
            apiHostInput.setText(provider.getApiHost());
        } else {
            apiHostInput.setText(ProviderSettings.getDefaultHost(provider.getProvider()));
        }
        if (provider.getApiPath() != null && !provider.getApiPath().isEmpty()) {
            apiPathInput.setText(provider.getApiPath());
        }
        
        // Set API mode
        String apiMode = provider.getApiMode();
        if (ProviderSettings.API_MODE_RESPONSES.equals(apiMode)) {
            radioResponses.setChecked(true);
        } else {
            radioChat.setChecked(true);
        }
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(provider.getDisplayName())
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String apiKey = apiKeyInput.getText() != null ? 
                    apiKeyInput.getText().toString().trim() : "";
                String apiHost = apiHostInput.getText() != null ? 
                    apiHostInput.getText().toString().trim() : "";
                String apiPath = apiPathInput.getText() != null ? 
                    apiPathInput.getText().toString().trim() : "";
                String selectedMode = radioResponses.isChecked() ? 
                    ProviderSettings.API_MODE_RESPONSES : ProviderSettings.API_MODE_CHAT;
                
                if (apiKey.isEmpty()) {
                    Toast.makeText(this, R.string.error_provider_not_configured, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                provider.setApiKey(apiKey);
                provider.setApiHost(apiHost);
                provider.setApiPath(apiPath.isEmpty() ? null : apiPath);
                provider.setApiMode(selectedMode);
                viewModel.saveProviderSettings(provider);
                
                showModelSelectionDialog(provider);
            })
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.select_models, (dialog, which) -> {
                String apiKey = apiKeyInput.getText() != null ? 
                    apiKeyInput.getText().toString().trim() : "";
                String apiHost = apiHostInput.getText() != null ? 
                    apiHostInput.getText().toString().trim() : "";
                String apiPath = apiPathInput.getText() != null ? 
                    apiPathInput.getText().toString().trim() : "";
                String selectedMode = radioResponses.isChecked() ? 
                    ProviderSettings.API_MODE_RESPONSES : ProviderSettings.API_MODE_CHAT;
                
                if (!apiKey.isEmpty()) {
                    provider.setApiKey(apiKey);
                    provider.setApiHost(apiHost);
                    provider.setApiPath(apiPath.isEmpty() ? null : apiPath);
                    provider.setApiMode(selectedMode);
                    viewModel.saveProviderSettings(provider);
                }
                
                showModelSelectionDialog(provider);
            })
            .show();
    }
    
    private void showModelSelectionDialog(ProviderSettings provider) {
        DialogModelSelectionBinding dialogBinding = DialogModelSelectionBinding.inflate(getLayoutInflater());
        
        ModelSelectionAdapter adapter = new ModelSelectionAdapter();
        dialogBinding.recyclerViewModels.setLayoutManager(new LinearLayoutManager(this));
        dialogBinding.recyclerViewModels.setAdapter(adapter);
        
        Set<String> selectedModels = new HashSet<>();
        if (provider.getModelsJson() != null && !provider.getModelsJson().isEmpty()) {
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<String>>(){}.getType();
                List<String> saved = gson.fromJson(provider.getModelsJson(), type);
                if (saved != null) {
                    selectedModels.addAll(saved);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing saved models", e);
            }
        }
        
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_models)
            .setView(dialogBinding.getRoot())
            .setPositiveButton(R.string.save, (dialog, which) -> {
                List<String> selected = adapter.getSelectedModels();
                if (selected.isEmpty()) {
                    Toast.makeText(this, R.string.please_select_at_least_one_model, Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.saveCustomModels(provider.getProvider(), selected);
                Toast.makeText(this, getString(R.string.models_selected, selected.size()), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null);
        
        androidx.appcompat.app.AlertDialog dialog = dialogBuilder.create();
        dialog.show();
        
        dialogBinding.progressBar.setVisibility(View.VISIBLE);
        dialogBinding.textLoading.setVisibility(View.VISIBLE);
        dialogBinding.textLoading.setText(R.string.fetching_models);
        dialogBinding.recyclerViewModels.setVisibility(View.GONE);
        
        if (provider.getApiKey() == null || provider.getApiKey().isEmpty()) {
            dialogBinding.progressBar.setVisibility(View.GONE);
            dialogBinding.textLoading.setText(R.string.please_configure_provider);
            return;
        }
        
        modelsService.fetchModels(provider, new ModelsService.ModelsCallback() {
            @Override
            public void onSuccess(List<String> models) {
                runOnUiThread(() -> {
                    dialogBinding.progressBar.setVisibility(View.GONE);
                    dialogBinding.textLoading.setVisibility(View.GONE);
                    dialogBinding.recyclerViewModels.setVisibility(View.VISIBLE);
                    
                    if (models.isEmpty()) {
                        dialogBinding.textLoading.setText(R.string.no_models_found);
                        dialogBinding.textLoading.setVisibility(View.VISIBLE);
                        loadDefaultModels(adapter, provider, selectedModels);
                    } else {
                        adapter.setModels(models, selectedModels);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Failed to fetch models: " + error);
                    dialogBinding.progressBar.setVisibility(View.GONE);
                    dialogBinding.textLoading.setText(getString(R.string.fetch_models_failed, error));
                    dialogBinding.textLoading.setVisibility(View.VISIBLE);
                    
                    loadDefaultModels(adapter, provider, selectedModels);
                    dialogBinding.recyclerViewModels.setVisibility(View.VISIBLE);
                    dialogBinding.textLoading.setVisibility(View.GONE);
                });
            }
        });
        
        dialogBinding.btnAddModel.setOnClickListener(v -> {
            String customModel = dialogBinding.inputCustomModel.getText() != null ?
                dialogBinding.inputCustomModel.getText().toString().trim() : "";
            if (!customModel.isEmpty()) {
                adapter.addCustomModel(customModel);
                dialogBinding.inputCustomModel.setText("");
                Toast.makeText(this, R.string.model_added, Toast.LENGTH_SHORT).show();
            }
        });
        
        dialogBinding.btnRefreshModels.setOnClickListener(v -> {
            dialogBinding.progressBar.setVisibility(View.VISIBLE);
            dialogBinding.textLoading.setText(R.string.fetching_models);
            dialogBinding.textLoading.setVisibility(View.VISIBLE);
            dialogBinding.recyclerViewModels.setVisibility(View.GONE);
            
            modelsService.fetchModels(provider, new ModelsService.ModelsCallback() {
                @Override
                public void onSuccess(List<String> models) {
                    runOnUiThread(() -> {
                        dialogBinding.progressBar.setVisibility(View.GONE);
                        dialogBinding.textLoading.setVisibility(View.GONE);
                        dialogBinding.recyclerViewModels.setVisibility(View.VISIBLE);
                        
                        if (models.isEmpty()) {
                            dialogBinding.textLoading.setText(R.string.no_models_found);
                            dialogBinding.textLoading.setVisibility(View.VISIBLE);
                        } else {
                            Set<String> currentSelected = new HashSet<>(adapter.getSelectedModels());
                            adapter.setModels(models, currentSelected);
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        dialogBinding.progressBar.setVisibility(View.GONE);
                        dialogBinding.textLoading.setText(getString(R.string.fetch_models_failed, error));
                        dialogBinding.textLoading.setVisibility(View.VISIBLE);
                    });
                }
            });
        });
    }
    
    private void loadDefaultModels(ModelSelectionAdapter adapter, ProviderSettings provider, Set<String> selectedModels) {
        List<String> defaultModels = viewModel.getModelsForProvider(provider.getProvider());
        if (!defaultModels.isEmpty()) {
            adapter.setModels(defaultModels, selectedModels);
        }
    }
    
    private void showAddCustomProviderDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_custom_provider, null);
        
        TextInputEditText nameInput = dialogView.findViewById(R.id.input_provider_name);
        TextInputEditText hostInput = dialogView.findViewById(R.id.input_api_host);
        TextInputEditText pathInput = dialogView.findViewById(R.id.input_api_path);
        TextInputEditText keyInput = dialogView.findViewById(R.id.input_api_key);
        TextInputEditText modelInput = dialogView.findViewById(R.id.input_default_model);
        RadioGroup apiModeGroup = dialogView.findViewById(R.id.radio_api_mode);
        RadioButton radioChat = dialogView.findViewById(R.id.radio_mode_chat);
        RadioButton radioResponses = dialogView.findViewById(R.id.radio_mode_responses);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_custom_provider)
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                String host = hostInput.getText() != null ? hostInput.getText().toString().trim() : "";
                String path = pathInput.getText() != null ? pathInput.getText().toString().trim() : "";
                String key = keyInput.getText() != null ? keyInput.getText().toString().trim() : "";
                String model = modelInput.getText() != null ? modelInput.getText().toString().trim() : "";
                String selectedMode = radioResponses.isChecked() ? 
                    ProviderSettings.API_MODE_RESPONSES : ProviderSettings.API_MODE_CHAT;
                
                if (name.isEmpty() || host.isEmpty() || key.isEmpty()) {
                    Toast.makeText(this, R.string.error_provider_not_configured, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String customId = "custom_" + UUID.randomUUID().toString().substring(0, 8);
                ProviderSettings customProvider = new ProviderSettings(customId);
                customProvider.setDisplayName(name);
                customProvider.setApiHost(host);
                customProvider.setApiPath(path.isEmpty() ? null : path);
                customProvider.setApiKey(key);
                customProvider.setApiMode(selectedMode);
                customProvider.setDefaultModel(model.isEmpty() ? "gpt-4o-mini" : model);
                
                viewModel.saveProviderSettings(customProvider);
                
                showModelSelectionDialog(customProvider);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
}

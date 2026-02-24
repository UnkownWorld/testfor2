package com.chatbox.app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chatbox.app.R;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.data.entity.Session;
import com.chatbox.app.databinding.ActivityMainBinding;
import com.chatbox.app.databinding.DialogNewChatBinding;
import com.chatbox.app.ui.adapters.SessionAdapter;
import com.chatbox.app.ui.viewmodels.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity - Main activity of the Chatbox app
 * 
 * This activity displays the list of chat sessions and provides
 * navigation to create new chats, open existing chats, and access settings.
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class MainActivity extends AppCompatActivity implements SessionAdapter.OnSessionClickListener {
    
    private static final String TAG = "MainActivity";
    
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private SessionAdapter sessionAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        setupRecyclerView();
        setupFab();
        observeData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        viewModel.refreshSessions();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        binding = null;
    }
    
    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter(this, new ArrayList<>());
        sessionAdapter.setOnSessionClickListener(this);
        
        binding.recyclerViewSessions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSessions.setAdapter(sessionAdapter);
        
        binding.recyclerViewSessions.addItemDecoration(
            new SessionAdapter.SessionItemDecoration(getResources().getDimensionPixelSize(R.dimen.session_item_margin))
        );
    }
    
    private void setupFab() {
        binding.fabNewChat.setOnClickListener(v -> showNewChatDialog());
    }
    
    private void observeData() {
        viewModel.getSessions().observe(this, sessions -> {
            Log.d(TAG, "Sessions updated: " + (sessions != null ? sessions.size() : 0) + " sessions");
            
            if (sessions == null || sessions.isEmpty()) {
                showEmptyState(true);
            } else {
                showEmptyState(false);
                sessionAdapter.updateSessions(sessions);
            }
        });
        
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
        
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }
    
    private void showEmptyState(boolean show) {
        binding.emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewSessions.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    /**
     * Show new chat dialog with provider/model selection
     */
    private void showNewChatDialog() {
        Log.d(TAG, "Showing new chat dialog");
        
        // Get configured providers
        List<ProviderSettings> configuredProviders = viewModel.getConfiguredProviders();
        
        if (configuredProviders.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.no_provider_configured)
                .setMessage(R.string.please_configure_provider)
                .setPositiveButton(R.string.configure, (dialog, which) -> openSettings())
                .setNegativeButton(R.string.cancel, null)
                .show();
            return;
        }
        
        // Create dialog
        DialogNewChatBinding dialogBinding = DialogNewChatBinding.inflate(getLayoutInflater());
        
        // Setup provider spinner
        List<String> providerNames = new ArrayList<>();
        List<String> providerIds = new ArrayList<>();
        for (ProviderSettings provider : configuredProviders) {
            providerNames.add(provider.getDisplayName());
            providerIds.add(provider.getProvider());
        }
        
        android.widget.ArrayAdapter<String> providerAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line, providerNames);
        dialogBinding.spinnerProvider.setAdapter(providerAdapter);
        
        // Setup model spinner (will be updated based on provider selection)
        List<String> modelIds = new ArrayList<>();
        android.widget.ArrayAdapter<String> modelAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_dropdown_item_1line);
        dialogBinding.spinnerModel.setAdapter(modelAdapter);
        
        // Get last used settings
        ProviderSettings lastProvider = viewModel.getLastUsedProvider();
        String lastModel = viewModel.getLastUsedModel();
        
        // Set default provider
        int defaultProviderIndex = 0;
        if (lastProvider != null) {
            int idx = providerIds.indexOf(lastProvider.getProvider());
            if (idx >= 0) defaultProviderIndex = idx;
        }
        dialogBinding.spinnerProvider.setText(providerNames.get(defaultProviderIndex), false);
        
        // Update models for selected provider
        updateModelSpinner(configuredProviders.get(defaultProviderIndex), modelAdapter, modelIds);
        
        // Set default model
        if (lastModel != null && modelIds.contains(lastModel)) {
            dialogBinding.spinnerModel.setText(lastModel, false);
        } else if (!modelIds.isEmpty()) {
            dialogBinding.spinnerModel.setText(modelAdapter.getItem(0), false);
        }
        
        // Provider selection listener
        dialogBinding.spinnerProvider.setOnItemClickListener((parent, view, position, id) -> {
            ProviderSettings selectedProvider = configuredProviders.get(position);
            updateModelSpinner(selectedProvider, modelAdapter, modelIds);
            if (!modelIds.isEmpty()) {
                dialogBinding.spinnerModel.setText(modelAdapter.getItem(0), false);
            }
        });
        
        // Advanced settings toggle
        dialogBinding.btnAdvancedSettings.setOnClickListener(v -> {
            if (dialogBinding.layoutAdvanced.getVisibility() == View.GONE) {
                dialogBinding.layoutAdvanced.setVisibility(View.VISIBLE);
                dialogBinding.btnAdvancedSettings.setIcon(getDrawable(R.drawable.ic_expand_less));
            } else {
                dialogBinding.layoutAdvanced.setVisibility(View.GONE);
                dialogBinding.btnAdvancedSettings.setIcon(getDrawable(R.drawable.ic_expand_more));
            }
        });
        
        // Temperature slider
        dialogBinding.sliderTemperature.addOnChangeListener((slider, value, fromUser) -> {
            dialogBinding.textTemperatureValue.setText(String.format("%.1f", value));
        });
        
        // Top P slider
        dialogBinding.sliderTopP.addOnChangeListener((slider, value, fromUser) -> {
            dialogBinding.textTopPValue.setText(String.format("%.2f", value));
        });
        
        // Set default values from last session
        if (lastProvider != null) {
            dialogBinding.sliderTemperature.setValue(viewModel.getLastTemperature());
            dialogBinding.sliderTopP.setValue(viewModel.getLastTopP());
            dialogBinding.inputMaxContext.setText(String.valueOf(viewModel.getLastMaxContext()));
            dialogBinding.inputMaxTokens.setText(String.valueOf(viewModel.getLastMaxTokens()));
            dialogBinding.switchStreaming.setChecked(viewModel.isLastStreaming());
            String lastSystemPrompt = viewModel.getLastSystemPrompt();
            if (lastSystemPrompt != null && !lastSystemPrompt.isEmpty()) {
                dialogBinding.inputSystemPrompt.setText(lastSystemPrompt);
            }
        }
        
        // Show dialog
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.new_chat)
            .setView(dialogBinding.getRoot())
            .setPositiveButton(R.string.create_chat, (dialog, which) -> {
                // Get selected provider
                int providerPosition = providerNames.indexOf(dialogBinding.spinnerProvider.getText().toString());
                if (providerPosition < 0) providerPosition = 0;
                ProviderSettings selectedProvider = configuredProviders.get(providerPosition);
                
                // Get selected model
                String selectedModel = dialogBinding.spinnerModel.getText().toString();
                if (selectedModel.isEmpty() && !modelIds.isEmpty()) {
                    selectedModel = modelIds.get(0);
                }
                
                // Get other settings
                String chatName = dialogBinding.inputChatName.getText() != null ? 
                    dialogBinding.inputChatName.getText().toString().trim() : "";
                String systemPrompt = dialogBinding.inputSystemPrompt.getText() != null ?
                    dialogBinding.inputSystemPrompt.getText().toString().trim() : "";
                float temperature = dialogBinding.sliderTemperature.getValue();
                float topP = dialogBinding.sliderTopP.getValue();
                int maxContext = 20;
                int maxTokens = 4096;
                
                try {
                    String maxContextStr = dialogBinding.inputMaxContext.getText().toString();
                    if (!maxContextStr.isEmpty()) {
                        maxContext = Integer.parseInt(maxContextStr);
                    }
                } catch (NumberFormatException e) {
                    maxContext = 20;
                }
                
                try {
                    String maxTokensStr = dialogBinding.inputMaxTokens.getText().toString();
                    if (!maxTokensStr.isEmpty()) {
                        maxTokens = Integer.parseInt(maxTokensStr);
                    }
                } catch (NumberFormatException e) {
                    maxTokens = 4096;
                }
                
                boolean streaming = dialogBinding.switchStreaming.isChecked();
                
                // Save settings for next time
                viewModel.saveLastSettings(selectedProvider.getProvider(), selectedModel, 
                    temperature, topP, maxContext, maxTokens, streaming, systemPrompt);
                
                // Create session
                Session session = viewModel.createSession(
                    chatName.isEmpty() ? null : chatName,
                    selectedProvider.getProvider(),
                    selectedModel,
                    systemPrompt,
                    temperature,
                    topP,
                    maxContext,
                    maxTokens,
                    streaming
                );
                
                if (session != null) {
                    openChat(session.getId());
                } else {
                    Toast.makeText(this, R.string.error_creating_session, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Update model spinner based on selected provider
     */
    private void updateModelSpinner(ProviderSettings provider, android.widget.ArrayAdapter<String> modelAdapter, List<String> modelIds) {
        modelAdapter.clear();
        modelIds.clear();
        
        // Get models for this provider
        List<String> models = viewModel.getModelsForProvider(provider.getProvider());
        for (String model : models) {
            modelAdapter.add(model);
            modelIds.add(model);
        }
        modelAdapter.notifyDataSetChanged();
    }
    
    private void openChat(String sessionId) {
        Log.d(TAG, "Opening chat: " + sessionId);
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_SESSION_ID, sessionId);
        startActivity(intent);
    }
    
    private void openSettings() {
        Log.d(TAG, "Opening settings");
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    private void showDeleteDialog(Session session) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_session)
            .setMessage(getString(R.string.delete_session_confirm, session.getName()))
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                viewModel.deleteSession(session);
                Toast.makeText(this, R.string.session_deleted, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    @Override
    public void onSessionClick(Session session) {
        openChat(session.getId());
    }
    
    @Override
    public void onSessionLongClick(Session session, View anchor) {
        showSessionContextMenu(session, anchor);
    }
    
    @Override
    public void onStarClick(Session session) {
        viewModel.toggleStarred(session);
    }
    
    private void showSessionContextMenu(Session session, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_session_context, popup.getMenu());
        
        MenuItem starItem = popup.getMenu().findItem(R.id.action_star);
        starItem.setTitle(session.isStarred() ? R.string.unstar : R.string.star);
        starItem.setIcon(session.isStarred() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.action_rename) {
                showRenameDialog(session);
                return true;
            } else if (itemId == R.id.action_star) {
                viewModel.toggleStarred(session);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteDialog(session);
                return true;
            }
            
            return false;
        });
        
        popup.show();
    }
    
    private void showRenameDialog(Session session) {
        com.google.android.material.textfield.TextInputEditText input = 
            new com.google.android.material.textfield.TextInputEditText(this);
        input.setText(session.getName());
        input.setSelection(session.getName().length());
        
        com.google.android.material.textfield.TextInputLayout textInputLayout = 
            new com.google.android.material.textfield.TextInputLayout(this);
        textInputLayout.setHint(getString(R.string.session_name));
        textInputLayout.addView(input);
        
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        textInputLayout.setPadding(padding, padding, padding, 0);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rename_session)
            .setView(textInputLayout)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String newName = input.getText() != null ? input.getText().toString().trim() : "";
                if (!newName.isEmpty()) {
                    viewModel.renameSession(session, newName);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_settings) {
            openSettings();
            return true;
        } else if (itemId == R.id.action_search) {
            Toast.makeText(this, R.string.search_coming_soon, Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}

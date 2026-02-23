package com.chatbox.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.chatbox.app.data.dao.ProviderSettingsDao;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.data.preferences.SettingsPreferences;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SettingsRepository - Repository for settings operations
 * 
 * This class acts as a single source of truth for settings data.
 * It coordinates between SharedPreferences (app settings) and 
 * Room database (provider settings).
 * 
 * Responsibilities:
 * - Manage provider settings (create, read, update, delete)
 * - Access app settings via SettingsPreferences
 * - Provide unified interface for all settings
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class SettingsRepository {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "SettingsRepository";
    
    /**
     * Singleton instance
     */
    private static SettingsRepository instance;
    
    /**
     * Provider settings DAO
     */
    private final ProviderSettingsDao providerSettingsDao;
    
    /**
     * Settings preferences
     */
    private final SettingsPreferences preferences;
    
    /**
     * Executor service for background operations
     */
    private final ExecutorService executor;
    
    // =========================================================================
    // Constructor
    // =========================================================================
    
    /**
     * Private constructor
     * 
     * @param providerSettingsDao Provider settings DAO
     * @param preferences Settings preferences
     */
    private SettingsRepository(ProviderSettingsDao providerSettingsDao, SettingsPreferences preferences) {
        this.providerSettingsDao = providerSettingsDao;
        this.preferences = preferences;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Get the singleton instance
     * 
     * @param providerSettingsDao Provider settings DAO
     * @param preferences Settings preferences
     * @return SettingsRepository instance
     */
    public static synchronized SettingsRepository getInstance(
            ProviderSettingsDao providerSettingsDao, 
            SettingsPreferences preferences) {
        if (instance == null) {
            instance = new SettingsRepository(providerSettingsDao, preferences);
        }
        return instance;
    }
    
    // =========================================================================
    // Provider Settings Operations
    // =========================================================================
    
    /**
     * Get settings for a specific provider
     * 
     * @param provider Provider ID
     * @return Provider settings, or null if not found
     */
    public ProviderSettings getProviderSettings(String provider) {
        return providerSettingsDao.getSettings(provider);
    }
    
    /**
     * Get settings for a specific provider (LiveData)
     * 
     * @param provider Provider ID
     * @return LiveData containing provider settings
     */
    public LiveData<ProviderSettings> getProviderSettingsLive(String provider) {
        return providerSettingsDao.getSettingsLive(provider);
    }
    
    /**
     * Get all provider settings
     * 
     * @return List of all provider settings
     */
    public List<ProviderSettings> getAllProviderSettings() {
        return providerSettingsDao.getAllSettings();
    }
    
    /**
     * Get all provider settings (LiveData)
     * 
     * @return LiveData containing list of all settings
     */
    public LiveData<List<ProviderSettings>> getAllProviderSettingsLive() {
        return providerSettingsDao.getAllSettingsLive();
    }
    
    /**
     * Get all enabled providers
     * 
     * @return List of enabled providers
     */
    public List<ProviderSettings> getEnabledProviders() {
        return providerSettingsDao.getEnabledProviders();
    }
    
    /**
     * Get all enabled providers (LiveData)
     * 
     * @return LiveData containing list of enabled providers
     */
    public LiveData<List<ProviderSettings>> getEnabledProvidersLive() {
        return providerSettingsDao.getEnabledProvidersLive();
    }
    
    /**
     * Get all configured providers (have API key)
     * 
     * @return List of configured providers
     */
    public List<ProviderSettings> getConfiguredProviders() {
        return providerSettingsDao.getConfiguredProviders();
    }
    
    /**
     * Get all configured providers (LiveData)
     * 
     * @return LiveData containing list of configured providers
     */
    public LiveData<List<ProviderSettings>> getConfiguredProvidersLive() {
        return providerSettingsDao.getConfiguredProvidersLive();
    }
    
    /**
     * Save provider settings
     * 
     * @param settings The settings to save
     */
    public void saveProviderSettings(ProviderSettings settings) {
        settings.touch();
        executor.execute(() -> providerSettingsDao.insert(settings));
        Log.d(TAG, "Saved settings for provider: " + settings.getProvider());
    }
    
    /**
     * Update provider settings
     * 
     * @param settings The settings to update
     */
    public void updateProviderSettings(ProviderSettings settings) {
        settings.touch();
        executor.execute(() -> providerSettingsDao.update(settings));
        Log.d(TAG, "Updated settings for provider: " + settings.getProvider());
    }
    
    /**
     * Update API key for a provider
     * 
     * @param provider Provider ID
     * @param apiKey The API key
     */
    public void updateApiKey(String provider, String apiKey) {
        executor.execute(() -> {
            providerSettingsDao.updateApiKey(provider, apiKey, System.currentTimeMillis());
            Log.d(TAG, "Updated API key for provider: " + provider);
        });
    }
    
    /**
     * Update API host for a provider
     * 
     * @param provider Provider ID
     * @param apiHost The API host URL
     */
    public void updateApiHost(String provider, String apiHost) {
        executor.execute(() -> {
            providerSettingsDao.updateApiHost(provider, apiHost, System.currentTimeMillis());
            Log.d(TAG, "Updated API host for provider: " + provider);
        });
    }
    
    /**
     * Enable or disable a provider
     * 
     * @param provider Provider ID
     * @param enabled true to enable, false to disable
     */
    public void setProviderEnabled(String provider, boolean enabled) {
        executor.execute(() -> {
            providerSettingsDao.setEnabled(provider, enabled);
            Log.d(TAG, "Set provider " + provider + " enabled: " + enabled);
        });
    }
    
    /**
     * Update default model for a provider
     * 
     * @param provider Provider ID
     * @param model Model ID
     */
    public void updateDefaultModel(String provider, String model) {
        executor.execute(() -> {
            providerSettingsDao.updateDefaultModel(provider, model);
            Log.d(TAG, "Updated default model for " + provider + ": " + model);
        });
    }
    
    /**
     * Check if a provider is configured (has API key)
     * 
     * @param provider Provider ID
     * @return true if configured
     */
    public boolean isProviderConfigured(String provider) {
        ProviderSettings settings = providerSettingsDao.getSettings(provider);
        return settings != null && settings.isConfigured();
    }
    
    /**
     * Delete provider settings
     * 
     * @param settings The settings to delete
     */
    public void deleteProviderSettings(ProviderSettings settings) {
        executor.execute(() -> providerSettingsDao.delete(settings));
    }
    
    /**
     * Initialize default settings for all providers
     * This should be called on first app launch
     */
    public void initializeDefaultSettings() {
        executor.execute(() -> {
            providerSettingsDao.initializeDefaultSettings();
            Log.d(TAG, "Initialized default provider settings");
        });
    }
    
    // =========================================================================
    // App Settings (via SettingsPreferences)
    // =========================================================================
    
    /**
     * Get the SettingsPreferences instance
     * 
     * @return SettingsPreferences
     */
    public SettingsPreferences getPreferences() {
        return preferences;
    }
    
    /**
     * Get the default provider from preferences
     * 
     * @return Default provider ID
     */
    public String getDefaultProvider() {
        return preferences.getDefaultProvider();
    }
    
    /**
     * Set the default provider in preferences
     * 
     * @param provider Provider ID
     */
    public void setDefaultProvider(String provider) {
        preferences.setDefaultProvider(provider);
    }
    
    /**
     * Get the default model from preferences
     * 
     * @return Default model ID
     */
    public String getDefaultModel() {
        return preferences.getDefaultModel();
    }
    
    /**
     * Set the default model in preferences
     * 
     * @param model Model ID
     */
    public void setDefaultModel(String model) {
        preferences.setDefaultModel(model);
    }
    
    /**
     * Get the theme setting
     * 
     * @return Theme ("light", "dark", "system")
     */
    public String getTheme() {
        return preferences.getTheme();
    }
    
    /**
     * Set the theme
     * 
     * @param theme Theme ("light", "dark", "system")
     */
    public void setTheme(String theme) {
        preferences.setTheme(theme);
    }
    
    /**
     * Check if markdown rendering is enabled
     * 
     * @return true if enabled
     */
    public boolean isRenderMarkdown() {
        return preferences.isRenderMarkdown();
    }
    
    /**
     * Set markdown rendering
     * 
     * @param render true to enable
     */
    public void setRenderMarkdown(boolean render) {
        preferences.setRenderMarkdown(render);
    }
    
    /**
     * Check if streaming responses are enabled
     * 
     * @return true if enabled
     */
    public boolean isStreamingResponses() {
        return preferences.isStreamingResponses();
    }
    
    /**
     * Set streaming responses
     * 
     * @param streaming true to enable
     */
    public void setStreamingResponses(boolean streaming) {
        preferences.setStreamingResponses(streaming);
    }
    
    // =========================================================================
    // Utility Methods
    // =========================================================================
    
    /**
     * Get a provider's display name
     * 
     * @param provider Provider ID
     * @return Display name
     */
    public String getProviderDisplayName(String provider) {
        ProviderSettings settings = providerSettingsDao.getSettings(provider);
        if (settings != null && settings.getDisplayName() != null) {
            return settings.getDisplayName();
        }
        return ProviderSettings.getDefaultDisplayName(provider);
    }
    
    /**
     * Get the API host for a provider
     * 
     * @param provider Provider ID
     * @return API host URL
     */
    public String getProviderApiHost(String provider) {
        ProviderSettings settings = providerSettingsDao.getSettings(provider);
        if (settings != null && settings.getApiHost() != null) {
            return settings.getApiHost();
        }
        return ProviderSettings.getDefaultHost(provider);
    }
    
    // =========================================================================
    // Cleanup
    // =========================================================================
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        executor.shutdown();
        instance = null;
    }
}

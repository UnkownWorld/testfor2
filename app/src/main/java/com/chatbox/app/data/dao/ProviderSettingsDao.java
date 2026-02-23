package com.chatbox.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.chatbox.app.data.entity.ProviderSettings;

import java.util.List;

/**
 * ProviderSettingsDao - Data Access Object for ProviderSettings entity
 * 
 * This interface defines database operations for the ProviderSettings entity.
 * Room automatically generates implementations for these methods.
 * 
 * Available Operations:
 * - Insert: Add new provider settings
 * - Update: Modify existing provider settings
 * - Delete: Remove provider settings
 * - Query: Retrieve provider settings with various filters
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
@Dao
public interface ProviderSettingsDao {
    
    // =========================================================================
    // Insert Operations
    // =========================================================================
    
    /**
     * Insert provider settings into the database
     * If settings for the same provider already exist, they will be replaced
     * 
     * @param settings The settings to insert
     * @return The row ID of the inserted settings
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ProviderSettings settings);
    
    /**
     * Insert multiple provider settings
     * 
     * @param settingsArray The settings to insert
     * @return Array of row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(ProviderSettings... settingsArray);
    
    /**
     * Insert a list of provider settings
     * 
     * @param settingsList The list of settings to insert
     * @return List of row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<ProviderSettings> settingsList);
    
    // =========================================================================
    // Update Operations
    // =========================================================================
    
    /**
     * Update existing provider settings
     * 
     * @param settings The settings to update
     * @return The number of rows updated
     */
    @Update
    int update(ProviderSettings settings);
    
    /**
     * Update multiple provider settings
     * 
     * @param settingsArray The settings to update
     * @return The number of rows updated
     */
    @Update
    int updateAll(ProviderSettings... settingsArray);
    
    /**
     * Update the API key for a provider
     * 
     * @param provider The provider ID
     * @param apiKey The new API key
     * @param updatedAt The update timestamp
     */
    @Query("UPDATE provider_settings SET api_key = :apiKey, updated_at = :updatedAt WHERE provider = :provider")
    void updateApiKey(String provider, String apiKey, long updatedAt);
    
    /**
     * Update the API host for a provider
     * 
     * @param provider The provider ID
     * @param apiHost The new API host
     * @param updatedAt The update timestamp
     */
    @Query("UPDATE provider_settings SET api_host = :apiHost, updated_at = :updatedAt WHERE provider = :provider")
    void updateApiHost(String provider, String apiHost, long updatedAt);
    
    /**
     * Enable or disable a provider
     * 
     * @param provider The provider ID
     * @param enabled The enabled status
     */
    @Query("UPDATE provider_settings SET enabled = :enabled WHERE provider = :provider")
    void setEnabled(String provider, boolean enabled);
    
    /**
     * Update the default model for a provider
     * 
     * @param provider The provider ID
     * @param model The default model ID
     */
    @Query("UPDATE provider_settings SET default_model = :model WHERE provider = :provider")
    void updateDefaultModel(String provider, String model);
    
    // =========================================================================
    // Delete Operations
    // =========================================================================
    
    /**
     * Delete provider settings
     * 
     * @param settings The settings to delete
     * @return The number of rows deleted
     */
    @Delete
    int delete(ProviderSettings settings);
    
    /**
     * Delete provider settings by provider ID
     * 
     * @param provider The provider ID
     */
    @Query("DELETE FROM provider_settings WHERE provider = :provider")
    void deleteByProvider(String provider);
    
    /**
     * Delete all provider settings
     * Use with caution
     */
    @Query("DELETE FROM provider_settings")
    void deleteAll();
    
    // =========================================================================
    // Query Operations - Single Provider
    // =========================================================================
    
    /**
     * Get settings for a specific provider
     * 
     * @param provider The provider ID
     * @return The provider settings, or null if not found
     */
    @Query("SELECT * FROM provider_settings WHERE provider = :provider LIMIT 1")
    ProviderSettings getSettings(String provider);
    
    /**
     * Get settings for a specific provider (LiveData)
     * 
     * @param provider The provider ID
     * @return LiveData containing the provider settings
     */
    @Query("SELECT * FROM provider_settings WHERE provider = :provider")
    LiveData<ProviderSettings> getSettingsLive(String provider);
    
    // =========================================================================
    // Query Operations - Multiple Providers
    // =========================================================================
    
    /**
     * Get all provider settings
     * 
     * @return List of all provider settings
     */
    @Query("SELECT * FROM provider_settings ORDER BY provider ASC")
    List<ProviderSettings> getAllSettings();
    
    /**
     * Get all provider settings (LiveData)
     * 
     * @return LiveData containing list of all settings
     */
    @Query("SELECT * FROM provider_settings ORDER BY provider ASC")
    LiveData<List<ProviderSettings>> getAllSettingsLive();
    
    /**
     * Get all enabled providers
     * 
     * @return List of enabled provider settings
     */
    @Query("SELECT * FROM provider_settings WHERE enabled = 1 ORDER BY provider ASC")
    List<ProviderSettings> getEnabledProviders();
    
    /**
     * Get all enabled providers (LiveData)
     * 
     * @return LiveData containing list of enabled providers
     */
    @Query("SELECT * FROM provider_settings WHERE enabled = 1 ORDER BY provider ASC")
    LiveData<List<ProviderSettings>> getEnabledProvidersLive();
    
    /**
     * Get all configured providers (have API key set)
     * 
     * @return List of configured provider settings
     */
    @Query("SELECT * FROM provider_settings WHERE api_key IS NOT NULL AND api_key != '' ORDER BY provider ASC")
    List<ProviderSettings> getConfiguredProviders();
    
    /**
     * Get all configured providers (LiveData)
     * 
     * @return LiveData containing list of configured providers
     */
    @Query("SELECT * FROM provider_settings WHERE api_key IS NOT NULL AND api_key != '' ORDER BY provider ASC")
    LiveData<List<ProviderSettings>> getConfiguredProvidersLive();
    
    // =========================================================================
    // Query Operations - Check Existence
    // =========================================================================
    
    /**
     * Check if settings exist for a provider
     * 
     * @param provider The provider ID
     * @return The count (0 or 1)
     */
    @Query("SELECT COUNT(*) FROM provider_settings WHERE provider = :provider")
    int hasSettings(String provider);
    
    /**
     * Check if a provider is enabled
     * 
     * @param provider The provider ID
     * @return true if enabled, false otherwise
     */
    @Query("SELECT enabled FROM provider_settings WHERE provider = :provider")
    boolean isEnabled(String provider);
    
    /**
     * Check if a provider has an API key configured
     * 
     * @param provider The provider ID
     * @return true if API key exists, false otherwise
     */
    @Query("SELECT CASE WHEN api_key IS NOT NULL AND api_key != '' THEN 1 ELSE 0 END FROM provider_settings WHERE provider = :provider")
    boolean hasApiKey(String provider);
    
    // =========================================================================
    // Default Settings Operations
    // =========================================================================
    
    /**
     * Initialize default settings for all providers
     * This should be called on first app launch
     */
    default void initializeDefaultSettings() {
        String[] providers = {
            ProviderSettings.PROVIDER_OPENAI,
            ProviderSettings.PROVIDER_CLAUDE,
            ProviderSettings.PROVIDER_GEMINI,
            ProviderSettings.PROVIDER_AZURE,
            ProviderSettings.PROVIDER_DEEPSEEK,
            ProviderSettings.PROVIDER_SILICONFLOW,
            ProviderSettings.PROVIDER_OLLAMA,
            ProviderSettings.PROVIDER_GROQ,
            ProviderSettings.PROVIDER_MISTRAL,
            ProviderSettings.PROVIDER_LMSTUDIO,
            ProviderSettings.PROVIDER_PERPLEXITY,
            ProviderSettings.PROVIDER_XAI,
            ProviderSettings.PROVIDER_OPENROUTER,
            ProviderSettings.PROVIDER_CUSTOM
        };
        
        for (String provider : providers) {
            if (getSettings(provider) == null) {
                ProviderSettings settings = new ProviderSettings(provider);
                insert(settings);
            }
        }
    }
}

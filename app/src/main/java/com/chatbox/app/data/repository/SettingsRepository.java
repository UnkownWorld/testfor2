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
     * Get all providers
     * 
     * @return List of all providers
     */
    public List<ProviderSettings> getAllProviders() {
        return providerSettingsDao.getAllSettings();
    }
    
    /**
     * Get available models for a provider
     * 
     * @param providerId Provider ID
     * @return List of model IDs
     */
    public List<String> getModelsForProvider(String providerId) {
        List<String> models = new java.util.ArrayList<>();
        
        // Check if provider has custom models saved
        ProviderSettings settings = providerSettingsDao.getSettings(providerId);
        if (settings != null && settings.getModelsJson() != null && !settings.getModelsJson().isEmpty()) {
            // Parse custom models from JSON
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<String>>(){}.getType();
                List<String> customModels = gson.fromJson(settings.getModelsJson(), type);
                if (customModels != null && !customModels.isEmpty()) {
                    return customModels;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing custom models", e);
            }
        }
        
        // Default models for each provider (updated with latest models)
        switch (providerId) {
            case ProviderSettings.PROVIDER_OPENAI:
                // GPT-4 Series
                models.add("gpt-4o");
                models.add("gpt-4o-mini");
                models.add("gpt-4o-2024-11-20");
                models.add("gpt-4o-2024-08-06");
                models.add("gpt-4o-2024-05-13");
                models.add("gpt-4-turbo");
                models.add("gpt-4-turbo-2024-04-09");
                models.add("gpt-4");
                models.add("gpt-4-32k");
                // GPT-3.5 Series
                models.add("gpt-3.5-turbo");
                models.add("gpt-3.5-turbo-16k");
                // o1 Series (Reasoning)
                models.add("o1");
                models.add("o1-mini");
                models.add("o1-preview");
                break;
            case ProviderSettings.PROVIDER_CLAUDE:
                // Claude 3.5 Series (Latest)
                models.add("claude-3-5-sonnet-20241022");
                models.add("claude-3-5-haiku-20241022");
                models.add("claude-3-5-sonnet-20240620");
                // Claude 3 Series
                models.add("claude-3-opus-20240229");
                models.add("claude-3-sonnet-20240229");
                models.add("claude-3-haiku-20240307");
                break;
            case ProviderSettings.PROVIDER_GEMINI:
                // Gemini 2.0 Series (Latest)
                models.add("gemini-2.0-flash-exp");
                models.add("gemini-2.0-flash-thinking-exp-1219");
                // Gemini 1.5 Series
                models.add("gemini-1.5-pro");
                models.add("gemini-1.5-pro-002");
                models.add("gemini-1.5-flash");
                models.add("gemini-1.5-flash-002");
                models.add("gemini-1.5-flash-8b");
                // Gemini 1.0 Series
                models.add("gemini-1.0-pro");
                break;
            case ProviderSettings.PROVIDER_DEEPSEEK:
                // DeepSeek V3
                models.add("deepseek-chat");
                models.add("deepseek-reasoner");
                // DeepSeek Coder
                models.add("deepseek-coder");
                break;
            case ProviderSettings.PROVIDER_GROQ:
                // Llama 3.3 Series
                models.add("llama-3.3-70b-versatile");
                models.add("llama-3.3-70b-specdec");
                // Llama 3.2 Series
                models.add("llama-3.2-90b-vision-preview");
                models.add("llama-3.2-11b-vision-preview");
                models.add("llama-3.2-3b-preview");
                models.add("llama-3.2-1b-preview");
                // Llama 3.1 Series
                models.add("llama-3.1-405b-reasoning");
                models.add("llama-3.1-70b-versatile");
                models.add("llama-3.1-8b-instant");
                // Other Models
                models.add("mixtral-8x7b-32768");
                models.add("gemma2-9b-it");
                models.add("qwen-2.5-32b");
                models.add("qwen-2.5-coder-32b");
                break;
            case ProviderSettings.PROVIDER_MISTRAL:
                // Mistral Latest
                models.add("mistral-large-2411");
                models.add("mistral-large-2407");
                models.add("mistral-small-latest");
                models.add("mistral-medium-latest");
                // Codestral
                models.add("codestral-2405");
                models.add("codestral-latest");
                // Open Models
                models.add("open-mistral-nemo");
                models.add("open-codestral-mamba");
                models.add("ministral-8b-latest");
                models.add("ministral-3b-latest");
                // Pixtral
                models.add("pixtral-12b");
                models.add("pixtral-large-2411");
                break;
            case ProviderSettings.PROVIDER_OLLAMA:
                // Llama Series
                models.add("llama3.3");
                models.add("llama3.2");
                models.add("llama3.2:1b");
                models.add("llama3.1");
                models.add("llama3.1:405b");
                models.add("llama3.1:70b");
                models.add("llama3");
                // Qwen Series
                models.add("qwen2.5");
                models.add("qwen2.5:72b");
                models.add("qwen2.5-coder");
                models.add("qwq");
                // Mistral Series
                models.add("mistral");
                models.add("mixtral");
                models.add("codestral");
                models.add("minicpm-v");
                // DeepSeek
                models.add("deepseek-r1");
                models.add("deepseek-v2");
                models.add("deepseek-coder-v2");
                // Code Models
                models.add("codellama");
                models.add("starcoder2");
                // Other
                models.add("phi4");
                models.add("phi3.5");
                models.add("gemma2");
                models.add("command-r");
                break;
            case ProviderSettings.PROVIDER_XAI:
                models.add("grok-2-1212");
                models.add("grok-2-vision-1212");
                models.add("grok-beta");
                models.add("grok-vision-beta");
                break;
            case ProviderSettings.PROVIDER_PERPLEXITY:
                // Sonar Series
                models.add("sonar");
                models.add("sonar-pro");
                models.add("sonar-reasoning");
                models.add("sonar-reasoning-pro");
                // Legacy
                models.add("llama-3.1-sonar-small-128k-online");
                models.add("llama-3.1-sonar-large-128k-online");
                models.add("llama-3.1-sonar-huge-128k-online");
                break;
            case ProviderSettings.PROVIDER_OPENROUTER:
                // OpenAI
                models.add("openai/gpt-4o");
                models.add("openai/gpt-4o-mini");
                models.add("openai/o1");
                models.add("openai/o1-mini");
                models.add("openai/o1-preview");
                // Anthropic
                models.add("anthropic/claude-3.5-sonnet");
                models.add("anthropic/claude-3.5-haiku");
                models.add("anthropic/claude-3-opus");
                // Google
                models.add("google/gemini-2.0-flash-exp");
                models.add("google/gemini-pro-1.5");
                models.add("google/gemini-flash-1.5");
                // Meta
                models.add("meta-llama/llama-3.3-70b-instruct");
                models.add("meta-llama/llama-3.2-11b-vision-instruct");
                models.add("meta-llama/llama-3.1-405b-instruct");
                models.add("meta-llama/llama-3.1-70b-instruct");
                // DeepSeek
                models.add("deepseek/deepseek-chat");
                models.add("deepseek/deepseek-reasoner");
                models.add("deepseek/deepseek-coder");
                // Qwen
                models.add("qwen/qwen-2.5-72b-instruct");
                models.add("qwen/qwen-2.5-coder-32b-instruct");
                models.add("qwen/qwq-32b-preview");
                // Mistral
                models.add("mistralai/mistral-large");
                models.add("mistralai/codestral-mamba");
                // Other
                models.add("x-ai/grok-beta");
                models.add("perplexity/sonar-reasoning-pro");
                break;
            case ProviderSettings.PROVIDER_SILICONFLOW:
                models.add("deepseek-ai/DeepSeek-V3");
                models.add("deepseek-ai/DeepSeek-R1");
                models.add("Qwen/Qwen2.5-72B-Instruct");
                models.add("Qwen/Qwen2.5-32B-Instruct");
                models.add("Qwen/Qwen2.5-Coder-32B-Instruct");
                models.add("meta-llama/Llama-3.3-70B-Instruct");
                models.add("meta-llama/Meta-Llama-3.1-405B-Instruct");
                break;
            default:
                // For custom providers, add some common models
                models.add("gpt-4o");
                models.add("gpt-4o-mini");
                models.add("claude-3-5-sonnet-20241022");
                models.add("gemini-2.0-flash-exp");
                models.add("deepseek-chat");
                break;
        }
        
        return models;
    }
    
    /**
     * Save custom models for a provider
     * 
     * @param providerId Provider ID
     * @param models List of model IDs
     */
    public void saveCustomModels(String providerId, List<String> models) {
        executor.execute(() -> {
            ProviderSettings settings = providerSettingsDao.getSettings(providerId);
            if (settings != null) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String modelsJson = gson.toJson(models);
                settings.setModelsJson(modelsJson);
                providerSettingsDao.update(settings);
                Log.d(TAG, "Saved custom models for provider: " + providerId);
            }
        });
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

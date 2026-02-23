package com.chatbox.app.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.chatbox.app.ChatboxApplication;
import com.chatbox.app.api.OpenAIService;
import com.chatbox.app.data.database.ChatboxDatabase;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.data.preferences.SettingsPreferences;
import com.chatbox.app.data.repository.SettingsRepository;
import com.chatbox.app.model.ChatRequest;
import com.chatbox.app.model.ChatResponse;

import java.util.List;

/**
 * ApiConfigViewModel - ViewModel for API configuration
 * 
 * This ViewModel manages provider settings and API testing.
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ApiConfigViewModel extends AndroidViewModel {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "ApiConfigViewModel";
    
    /**
     * Repository for settings operations
     */
    private final SettingsRepository settingsRepository;
    
    /**
     * LiveData for providers
     */
    private final LiveData<List<ProviderSettings>> providers;
    
    /**
     * MutableLiveData for error messages
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    // =========================================================================
    // Constructor
    // =========================================================================
    
    /**
     * Constructor
     * 
     * @param application The Application instance
     */
    public ApiConfigViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "ApiConfigViewModel created");
        
        // Get database instance
        ChatboxDatabase database = ChatboxDatabase.getInstance(application);
        
        // Get preferences
        SettingsPreferences preferences = ((ChatboxApplication) application).getSettingsPreferences();
        
        // Initialize repository
        settingsRepository = SettingsRepository.getInstance(
            database.providerSettingsDao(), 
            preferences
        );
        
        // Get providers LiveData
        providers = settingsRepository.getAllProviderSettingsLive();
    }
    
    // =========================================================================
    // Getters
    // =========================================================================
    
    /**
     * Get the providers LiveData
     * 
     * @return LiveData containing list of providers
     */
    public LiveData<List<ProviderSettings>> getProviders() {
        return providers;
    }
    
    /**
     * Get error messages LiveData
     * 
     * @return LiveData containing error messages
     */
    public LiveData<String> getError() {
        return error;
    }
    
    // =========================================================================
    // Provider Operations
    // =========================================================================
    
    /**
     * Save provider settings
     * 
     * @param settings The settings to save
     */
    public void saveProviderSettings(ProviderSettings settings) {
        settingsRepository.saveProviderSettings(settings);
        Log.d(TAG, "Saved settings for provider: " + settings.getProvider());
    }
    
    /**
     * Update API key for a provider
     * 
     * @param provider Provider ID
     * @param apiKey The API key
     */
    public void updateApiKey(String provider, String apiKey) {
        settingsRepository.updateApiKey(provider, apiKey);
    }
    
    /**
     * Update API host for a provider
     * 
     * @param provider Provider ID
     * @param apiHost The API host
     */
    public void updateApiHost(String provider, String apiHost) {
        settingsRepository.updateApiHost(provider, apiHost);
    }
    
    /**
     * Enable or disable a provider
     * 
     * @param provider Provider ID
     * @param enabled true to enable
     */
    public void setProviderEnabled(String provider, boolean enabled) {
        settingsRepository.setProviderEnabled(provider, enabled);
    }
    
    // =========================================================================
    // API Testing
    // =========================================================================
    
    /**
     * Test API connection
     * 
     * @param provider The provider to test
     * @param callback Callback for test result
     */
    public void testConnection(ProviderSettings provider, TestCallback callback) {
        if (!provider.isConfigured()) {
            callback.onError("Provider not configured");
            return;
        }
        
        // Create OpenAI service
        OpenAIService service = new OpenAIService(provider, 30);
        
        // Create test request
        ChatRequest request = new ChatRequest();
        request.setModel(provider.getDefaultModel() != null ? provider.getDefaultModel() : "gpt-4o-mini");
        request.addUserMessage("Hello");
        request.setStream(false);
        request.setMaxTokens(10);
        
        // Send test request
        service.chatCompletionSync(request, new OpenAIService.ChatCallback() {
            @Override
            public void onStart() {
                // Test started
            }
            
            @Override
            public void onChunk(String chunk) {
                // Received chunk
            }
            
            @Override
            public void onComplete(ChatResponse response) {
                callback.onSuccess();
            }
            
            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
    
    // =========================================================================
    // Callback Interface
    // =========================================================================
    
    /**
     * Callback interface for API testing
     */
    public interface TestCallback {
        /**
         * Called when connection test succeeds
         */
        void onSuccess();
        
        /**
         * Called when connection test fails
         * 
         * @param error The error message
         */
        void onError(String error);
    }
    
    // =========================================================================
    // Cleanup
    // =========================================================================
    
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ApiConfigViewModel cleared");
    }
}

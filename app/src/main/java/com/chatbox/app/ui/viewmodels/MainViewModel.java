package com.chatbox.app.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.chatbox.app.ChatboxApplication;
import com.chatbox.app.data.database.ChatboxDatabase;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.data.entity.Session;
import com.chatbox.app.data.preferences.SettingsPreferences;
import com.chatbox.app.data.repository.ChatRepository;
import com.chatbox.app.data.repository.SettingsRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * MainViewModel - ViewModel for MainActivity
 * 
 * This ViewModel manages the UI-related data for the MainActivity.
 * It provides:
 * - Session list management
 * - Session creation and deletion
 * - Provider configuration checking
 * - Last used settings persistence
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class MainViewModel extends AndroidViewModel {
    
    private static final String TAG = "MainViewModel";
    
    private final ChatRepository chatRepository;
    private final SettingsRepository settingsRepository;
    private final SettingsPreferences preferences;
    
    private final LiveData<List<Session>> sessions;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "MainViewModel created");
        
        ChatboxDatabase database = ChatboxDatabase.getInstance(application);
        preferences = ((ChatboxApplication) application).getSettingsPreferences();
        
        settingsRepository = SettingsRepository.getInstance(
            database.providerSettingsDao(), 
            preferences
        );
        
        chatRepository = ChatRepository.getInstance(
            database.sessionDao(),
            database.messageDao(),
            null
        );
        
        sessions = chatRepository.getAllSessionsLive();
    }
    
    public LiveData<List<Session>> getSessions() {
        return sessions;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    // =========================================================================
    // Provider Operations
    // =========================================================================
    
    /**
     * Get list of configured providers
     */
    public List<ProviderSettings> getConfiguredProviders() {
        List<ProviderSettings> allProviders = settingsRepository.getAllProviders();
        List<ProviderSettings> configured = new ArrayList<>();
        
        for (ProviderSettings provider : allProviders) {
            if (provider.isConfigured()) {
                configured.add(provider);
            }
        }
        
        return configured;
    }
    
    /**
     * Get models for a specific provider
     */
    public List<String> getModelsForProvider(String providerId) {
        return settingsRepository.getModelsForProvider(providerId);
    }
    
    /**
     * Get last used provider
     */
    public ProviderSettings getLastUsedProvider() {
        String lastProviderId = preferences.getLastUsedProvider();
        if (lastProviderId != null && !lastProviderId.isEmpty()) {
            return settingsRepository.getProviderSettings(lastProviderId);
        }
        
        // Return first configured provider
        List<ProviderSettings> configured = getConfiguredProviders();
        return configured.isEmpty() ? null : configured.get(0);
    }
    
    /**
     * Get last used model
     */
    public String getLastUsedModel() {
        return preferences.getLastUsedModel();
    }
    
    /**
     * Get last used temperature
     */
    public float getLastTemperature() {
        return preferences.getLastTemperature();
    }
    
    /**
     * Get last used Top P
     */
    public float getLastTopP() {
        return preferences.getLastTopP();
    }
    
    /**
     * Get last used max context
     */
    public int getLastMaxContext() {
        return preferences.getLastMaxContext();
    }
    
    /**
     * Get last used max tokens
     */
    public int getLastMaxTokens() {
        return preferences.getLastMaxTokens();
    }
    
    /**
     * Get last used streaming setting
     */
    public boolean isLastStreaming() {
        return preferences.isLastStreaming();
    }
    
    /**
     * Get last used system prompt
     */
    public String getLastSystemPrompt() {
        return preferences.getLastSystemPrompt();
    }
    
    /**
     * Save last used settings
     */
    public void saveLastSettings(String provider, String model, float temperature, 
            float topP, int maxContext, int maxTokens, boolean streaming, String systemPrompt) {
        preferences.setLastUsedProvider(provider);
        preferences.setLastUsedModel(model);
        preferences.setLastTemperature(temperature);
        preferences.setLastTopP(topP);
        preferences.setLastMaxContext(maxContext);
        preferences.setLastMaxTokens(maxTokens);
        preferences.setLastStreaming(streaming);
        preferences.setLastSystemPrompt(systemPrompt);
    }
    
    // =========================================================================
    // Session Operations
    // =========================================================================
    
    /**
     * Create a new chat session with specified settings
     */
    public Session createSession(String name, String provider, String model,
            String systemPrompt, float temperature, float topP, 
            int maxContext, int maxTokens, boolean streaming) {
        Log.d(TAG, "Creating new session with provider: " + provider + ", model: " + model);
        
        try {
            Session session = chatRepository.createSession(
                name != null ? name : "New Chat", 
                provider, 
                model
            );
            
            // Set session parameters
            session.setSystemPrompt(systemPrompt);
            session.setTemperature(temperature);
            session.setTopP(topP);
            session.setMaxContextMessages(maxContext);
            session.setMaxTokens(maxTokens);
            session.setStreamingEnabled(streaming);
            
            // Update session in database
            chatRepository.updateSession(session);
            
            Log.d(TAG, "Session created: " + session.getId());
            return session;
        } catch (Exception e) {
            Log.e(TAG, "Error creating session", e);
            error.setValue("Error creating session: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a simple session (backward compatibility)
     */
    public Session createSession() {
        ProviderSettings lastProvider = getLastUsedProvider();
        if (lastProvider == null) {
            error.setValue("Please configure API settings first");
            return null;
        }
        
        String model = getLastUsedModel();
        if (model == null || model.isEmpty()) {
            List<String> models = getModelsForProvider(lastProvider.getProvider());
            model = models.isEmpty() ? "gpt-3.5-turbo" : models.get(0);
        }
        
        return createSession(
            null,
            lastProvider.getProvider(),
            model,
            getLastSystemPrompt(),
            getLastTemperature(),
            getLastTopP(),
            getLastMaxContext(),
            getLastMaxTokens(),
            isLastStreaming()
        );
    }
    
    public void deleteSession(Session session) {
        Log.d(TAG, "Deleting session: " + session.getId());
        try {
            chatRepository.deleteSession(session);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting session", e);
            error.setValue("Error deleting session: " + e.getMessage());
        }
    }
    
    public void toggleStarred(Session session) {
        Log.d(TAG, "Toggling starred for session: " + session.getId());
        try {
            chatRepository.setSessionStarred(session.getId(), !session.isStarred());
        } catch (Exception e) {
            Log.e(TAG, "Error toggling starred", e);
            error.setValue("Error updating session: " + e.getMessage());
        }
    }
    
    public void renameSession(Session session, String newName) {
        Log.d(TAG, "Renaming session " + session.getId() + " to: " + newName);
        try {
            chatRepository.renameSession(session.getId(), newName);
        } catch (Exception e) {
            Log.e(TAG, "Error renaming session", e);
            error.setValue("Error renaming session: " + e.getMessage());
        }
    }
    
    public void refreshSessions() {
        Log.d(TAG, "Refreshing sessions");
    }
    
    public boolean isDefaultProviderConfigured() {
        String provider = settingsRepository.getDefaultProvider();
        return settingsRepository.isProviderConfigured(provider);
    }
    
    public String getDefaultProvider() {
        return settingsRepository.getDefaultProvider();
    }
    
    public String getDefaultModel() {
        return settingsRepository.getDefaultModel();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "MainViewModel cleared");
    }
}

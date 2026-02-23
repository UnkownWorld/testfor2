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

import java.util.List;

/**
 * MainViewModel - ViewModel for MainActivity
 * 
 * This ViewModel manages the UI-related data for the MainActivity.
 * It provides:
 * - Session list management
 * - Session creation and deletion
 * - Provider configuration checking
 * 
 * Architecture:
 * - Uses AndroidViewModel to access Application context
 * - Communicates with repositories for data operations
 * - Exposes LiveData for UI observation
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class MainViewModel extends AndroidViewModel {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "MainViewModel";
    
    /**
     * Repository for chat operations
     */
    private final ChatRepository chatRepository;
    
    /**
     * Repository for settings operations
     */
    private final SettingsRepository settingsRepository;
    
    /**
     * LiveData for session list
     */
    private final LiveData<List<Session>> sessions;
    
    /**
     * MutableLiveData for error messages
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    /**
     * MutableLiveData for loading state
     */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    // =========================================================================
    // Constructor
    // =========================================================================
    
    /**
     * Constructor
     * 
     * @param application The Application instance
     */
    public MainViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "MainViewModel created");
        
        // Get database instance
        ChatboxDatabase database = ChatboxDatabase.getInstance(application);
        
        // Get preferences
        SettingsPreferences preferences = ((ChatboxApplication) application).getSettingsPreferences();
        
        // Initialize repositories
        settingsRepository = SettingsRepository.getInstance(
            database.providerSettingsDao(), 
            preferences
        );
        
        // Initialize chat repository (will be properly initialized when needed)
        chatRepository = ChatRepository.getInstance(
            database.sessionDao(),
            database.messageDao(),
            null // Will be set when provider is selected
        );
        
        // Get sessions LiveData
        sessions = chatRepository.getAllSessionsLive();
    }
    
    // =========================================================================
    // Getters
    // =========================================================================
    
    /**
     * Get the session list LiveData
     * 
     * @return LiveData containing list of sessions
     */
    public LiveData<List<Session>> getSessions() {
        return sessions;
    }
    
    /**
     * Get error messages LiveData
     * 
     * @return LiveData containing error messages
     */
    public LiveData<String> getError() {
        return error;
    }
    
    /**
     * Get loading state LiveData
     * 
     * @return LiveData containing loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    // =========================================================================
    // Session Operations
    // =========================================================================
    
    /**
     * Create a new chat session
     * Uses the default provider and model from settings
     * 
     * @return The created session, or null if creation failed
     */
    public Session createSession() {
        Log.d(TAG, "Creating new session");
        
        String provider = settingsRepository.getDefaultProvider();
        String model = settingsRepository.getDefaultModel();
        
        // Check if provider is configured
        ProviderSettings providerSettings = settingsRepository.getProviderSettings(provider);
        if (providerSettings == null || !providerSettings.isConfigured()) {
            Log.w(TAG, "Default provider not configured: " + provider);
            error.setValue("Please configure API settings first");
            return null;
        }
        
        try {
            Session session = chatRepository.createSession(provider, model);
            Log.d(TAG, "Session created: " + session.getId());
            return session;
        } catch (Exception e) {
            Log.e(TAG, "Error creating session", e);
            error.setValue("Error creating session: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Delete a session
     * 
     * @param session The session to delete
     */
    public void deleteSession(Session session) {
        Log.d(TAG, "Deleting session: " + session.getId());
        try {
            chatRepository.deleteSession(session);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting session", e);
            error.setValue("Error deleting session: " + e.getMessage());
        }
    }
    
    /**
     * Toggle the starred status of a session
     * 
     * @param session The session to toggle
     */
    public void toggleStarred(Session session) {
        Log.d(TAG, "Toggling starred for session: " + session.getId());
        try {
            chatRepository.setSessionStarred(session.getId(), !session.isStarred());
        } catch (Exception e) {
            Log.e(TAG, "Error toggling starred", e);
            error.setValue("Error updating session: " + e.getMessage());
        }
    }
    
    /**
     * Rename a session
     * 
     * @param session The session to rename
     * @param newName The new name
     */
    public void renameSession(Session session, String newName) {
        Log.d(TAG, "Renaming session " + session.getId() + " to: " + newName);
        try {
            chatRepository.renameSession(session.getId(), newName);
        } catch (Exception e) {
            Log.e(TAG, "Error renaming session", e);
            error.setValue("Error renaming session: " + e.getMessage());
        }
    }
    
    /**
     * Refresh the session list
     * This triggers a re-query of the database
     */
    public void refreshSessions() {
        Log.d(TAG, "Refreshing sessions");
        // The LiveData will automatically update when the database changes
        // This method can be used to trigger manual refresh if needed
    }
    
    // =========================================================================
    // Provider Configuration
    // =========================================================================
    
    /**
     * Check if the default provider is configured
     * 
     * @return true if the default provider has an API key configured
     */
    public boolean isDefaultProviderConfigured() {
        String provider = settingsRepository.getDefaultProvider();
        return settingsRepository.isProviderConfigured(provider);
    }
    
    /**
     * Get the default provider ID
     * 
     * @return The default provider ID
     */
    public String getDefaultProvider() {
        return settingsRepository.getDefaultProvider();
    }
    
    /**
     * Get the default model ID
     * 
     * @return The default model ID
     */
    public String getDefaultModel() {
        return settingsRepository.getDefaultModel();
    }
    
    // =========================================================================
    // Cleanup
    // =========================================================================
    
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "MainViewModel cleared");
    }
}

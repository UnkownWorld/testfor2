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
import com.chatbox.app.data.entity.Message;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.data.entity.Session;
import com.chatbox.app.data.preferences.SettingsPreferences;
import com.chatbox.app.data.repository.ChatRepository;
import com.chatbox.app.data.repository.SettingsRepository;
import com.chatbox.app.model.ChatResponse;

import java.util.List;

/**
 * ChatViewModel - ViewModel for ChatActivity
 * 
 * This ViewModel manages the UI-related data for the ChatActivity.
 * It handles:
 * - Message sending and receiving
 * - Session management
 * - API communication
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ChatViewModel extends AndroidViewModel {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "ChatViewModel";
    
    /**
     * Repository for chat operations
     */
    private final ChatRepository chatRepository;
    
    /**
     * Repository for settings operations
     */
    private final SettingsRepository settingsRepository;
    
    /**
     * OpenAI service for API calls
     */
    private OpenAIService openAIService;
    
    /**
     * Session ID
     */
    private String sessionId;
    
    /**
     * LiveData for session
     */
    private LiveData<Session> session;
    
    /**
     * LiveData for messages
     */
    private LiveData<List<Message>> messages;
    
    /**
     * MutableLiveData for loading state
     */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
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
    public ChatViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "ChatViewModel created");
        
        // Get database instance
        ChatboxDatabase database = ChatboxDatabase.getInstance(application);
        
        // Get preferences
        SettingsPreferences preferences = ((ChatboxApplication) application).getSettingsPreferences();
        
        // Initialize repositories
        settingsRepository = SettingsRepository.getInstance(
            database.providerSettingsDao(), 
            preferences
        );
        
        // Initialize chat repository
        chatRepository = ChatRepository.getInstance(
            database.sessionDao(),
            database.messageDao(),
            null
        );
    }
    
    /**
     * Set the session ID
     * 
     * @param sessionId The session ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        
        // Get session LiveData
        session = chatRepository.getSessionLive(sessionId);
        
        // Get messages LiveData
        messages = chatRepository.getMessagesLive(sessionId);
        
        // Initialize OpenAI service
        initializeOpenAIService();
    }
    
    /**
     * Initialize OpenAI service with provider settings
     */
    private void initializeOpenAIService() {
        Session currentSession = chatRepository.getSession(sessionId);
        if (currentSession != null) {
            String provider = currentSession.getProvider();
            ProviderSettings settings = settingsRepository.getProviderSettings(provider);
            
            if (settings != null && settings.isConfigured()) {
                openAIService = new OpenAIService(settings, settingsRepository.getPreferences().getTimeoutSeconds());
            } else {
                Log.w(TAG, "Provider not configured: " + provider);
            }
        }
    }
    
    // =========================================================================
    // Getters
    // =========================================================================
    
    /**
     * Get the session LiveData
     * 
     * @return LiveData containing the session
     */
    public LiveData<Session> getSession() {
        return session;
    }
    
    /**
     * Get the messages LiveData
     * 
     * @return LiveData containing list of messages
     */
    public LiveData<List<Message>> getMessages() {
        return messages;
    }
    
    /**
     * Get loading state LiveData
     * 
     * @return LiveData containing loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
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
    // Message Operations
    // =========================================================================
    
    /**
     * Send a message
     * 
     * @param content Message content
     * @param callback Callback for response handling
     */
    public void sendMessage(String content, SendCallback callback) {
        if (sessionId == null) {
            error.setValue("Session not initialized");
            return;
        }
        
        if (openAIService == null) {
            initializeOpenAIService();
            if (openAIService == null) {
                error.setValue("Provider not configured");
                return;
            }
        }
        
        isLoading.setValue(true);
        
        chatRepository.sendMessage(sessionId, content, new ChatRepository.ChatCallback() {
            @Override
            public void onStart() {
                // Message sending started
            }
            
            @Override
            public void onChunk(String chunk) {
                callback.onChunk(chunk);
            }
            
            @Override
            public void onComplete(ChatResponse response) {
                isLoading.setValue(false);
                callback.onComplete();
            }
            
            @Override
            public void onError(String errorMessage) {
                isLoading.setValue(false);
                error.setValue(errorMessage);
                callback.onError(errorMessage);
            }
        });
    }
    
    /**
     * Delete a message
     * 
     * @param message The message to delete
     */
    public void deleteMessage(Message message) {
        chatRepository.deleteMessage(message);
    }
    
    /**
     * Update a message
     * 
     * @param message The message to update
     */
    public void updateMessage(Message message) {
        chatRepository.updateMessage(message);
    }
    
    /**
     * Clear all messages in the session
     */
    public void clearMessages() {
        chatRepository.clearMessages(sessionId);
    }
    
    // =========================================================================
    // Settings Access
    // =========================================================================
    
    /**
     * Check if send on Enter is enabled
     * 
     * @return true if enabled
     */
    public boolean isSendOnEnter() {
        return settingsRepository.getPreferences().isSendOnEnter();
    }
    
    /**
     * Check if auto-scroll is enabled
     * 
     * @return true if enabled
     */
    public boolean isAutoScroll() {
        return settingsRepository.getPreferences().isAutoScroll();
    }
    
    /**
     * Check if streaming responses are enabled
     * 
     * @return true if enabled
     */
    public boolean isStreamingResponses() {
        return settingsRepository.getPreferences().isStreamingResponses();
    }
    
    // =========================================================================
    // Callback Interface
    // =========================================================================
    
    /**
     * Callback interface for sending messages
     */
    public interface SendCallback {
        /**
         * Called when a content chunk is received
         * 
         * @param chunk The content chunk
         */
        void onChunk(String chunk);
        
        /**
         * Called when the response is complete
         */
        void onComplete();
        
        /**
         * Called when an error occurs
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
        Log.d(TAG, "ChatViewModel cleared");
    }
}

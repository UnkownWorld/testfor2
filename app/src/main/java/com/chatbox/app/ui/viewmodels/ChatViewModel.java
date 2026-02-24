package com.chatbox.app.ui.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
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
 */
public class ChatViewModel extends AndroidViewModel {
    
    private static final String TAG = "ChatViewModel";
    
    private final ChatRepository chatRepository;
    private final SettingsRepository settingsRepository;
    private OpenAIService openAIService;
    private String sessionId;
    private LiveData<Session> session;
    private LiveData<List<Message>> messages;
    
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public ChatViewModel(@NonNull Application application) {
        super(application);
        Log.d(TAG, "ChatViewModel created");
        
        ChatboxDatabase database = ChatboxDatabase.getInstance(application);
        SettingsPreferences preferences = ((ChatboxApplication) application).getSettingsPreferences();
        
        settingsRepository = SettingsRepository.getInstance(
            database.providerSettingsDao(), 
            preferences
        );
        
        chatRepository = ChatRepository.getInstance(
            database.sessionDao(),
            database.messageDao(),
            null
        );
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        
        session = chatRepository.getSessionLive(sessionId);
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
                chatRepository.setOpenAIService(openAIService);
                Log.d(TAG, "OpenAIService initialized for provider: " + provider);
            } else {
                Log.w(TAG, "Provider not configured: " + provider);
                postError("提供商未配置，请先在设置中配置API密钥");
            }
        } else {
            Log.e(TAG, "Session not found: " + sessionId);
            postError("会话不存在");
        }
    }
    
    // Getters
    
    public LiveData<Session> getSession() {
        return session;
    }
    
    public LiveData<List<Message>> getMessages() {
        return messages;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    // Message Operations
    
    /**
     * Send a message
     */
    public void sendMessage(String content, SendCallback callback) {
        sendMessageWithFile(content, content, callback);
    }
    
    /**
     * Send a message with file content
     * @param displayContent Content to display in UI (user input only)
     * @param apiContent Content to send to API (user input + file content)
     * @param callback Callback for the operation
     */
    public void sendMessageWithFile(String displayContent, String apiContent, SendCallback callback) {
        if (sessionId == null) {
            String errorMsg = "会话未初始化";
            postError(errorMsg);
            callback.onError(errorMsg);
            return;
        }
        
        // 每次发送消息前检查并初始化OpenAIService
        if (openAIService == null) {
            initializeOpenAIService();
            if (openAIService == null) {
                String errorMsg = "API服务未配置，请先在设置中配置提供商";
                postError(errorMsg);
                callback.onError(errorMsg);
                return;
            }
        }
        
        postLoading(true);
        
        // Use displayContent for UI, apiContent for API
        chatRepository.sendMessageWithFile(sessionId, displayContent, apiContent, new ChatRepository.ChatCallback() {
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
                postLoading(false);
                callback.onComplete();
            }
            
            @Override
            public void onError(String errorMessage) {
                postLoading(false);
                postError(errorMessage);
                callback.onError(errorMessage);
            }
        });
    }
    
    /**
     * 在主线程中设置加载状态
     */
    private void postLoading(boolean loading) {
        mainHandler.post(() -> isLoading.setValue(loading));
    }
    
    /**
     * 在主线程中设置错误消息
     */
    private void postError(String errorMsg) {
        mainHandler.post(() -> error.setValue(errorMsg));
    }
    
    public void deleteMessage(Message message) {
        chatRepository.deleteMessage(message);
    }
    
    public void updateMessage(Message message) {
        chatRepository.updateMessage(message);
    }
    
    public void clearMessages() {
        chatRepository.clearMessages(sessionId);
    }
    
    // Session Update Operations
    
    public void updateSessionModel(String model) {
        Session currentSession = chatRepository.getSession(sessionId);
        if (currentSession != null) {
            currentSession.setModel(model);
            chatRepository.updateSession(currentSession);
            initializeOpenAIService();
        }
    }
    
    public void updateSessionProvider(String provider, String model) {
        Session currentSession = chatRepository.getSession(sessionId);
        if (currentSession != null) {
            currentSession.setProvider(provider);
            currentSession.setModel(model);
            chatRepository.updateSession(currentSession);
            initializeOpenAIService();
        }
    }
    
    // Provider Operations
    
    public List<ProviderSettings> getConfiguredProviders() {
        return settingsRepository.getConfiguredProviders();
    }
    
    /**
     * Get models for a provider (from saved models)
     */
    public List<String> getModelsForProvider(String providerId) {
        return settingsRepository.getModelsForProvider(providerId);
    }
    
    public String getProviderDisplayName(String providerId) {
        return settingsRepository.getProviderDisplayName(providerId);
    }
    
    // Settings Access
    
    public boolean isSendOnEnter() {
        return settingsRepository.getPreferences().isSendOnEnter();
    }
    
    public boolean isAutoScroll() {
        return settingsRepository.getPreferences().isAutoScroll();
    }
    
    public boolean isStreamingResponses() {
        return settingsRepository.getPreferences().isStreamingResponses();
    }
    
    // Callback Interface
    
    public interface SendCallback {
        void onChunk(String chunk);
        void onComplete();
        void onError(String error);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ChatViewModel cleared");
    }
}

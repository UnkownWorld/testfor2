package com.chatbox.app.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.chatbox.app.api.OpenAIService;
import com.chatbox.app.data.dao.MessageDao;
import com.chatbox.app.data.dao.SessionDao;
import com.chatbox.app.data.entity.Message;
import com.chatbox.app.data.entity.Session;
import com.chatbox.app.model.ChatRequest;
import com.chatbox.app.model.ChatResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChatRepository - Repository for chat operations
 */
public class ChatRepository {
    
    private static final String TAG = "ChatRepository";
    
    private static ChatRepository instance;
    
    private final SessionDao sessionDao;
    private final MessageDao messageDao;
    private OpenAIService openAIService;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private ChatRepository(SessionDao sessionDao, MessageDao messageDao, OpenAIService openAIService) {
        this.sessionDao = sessionDao;
        this.messageDao = messageDao;
        this.openAIService = openAIService;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized ChatRepository getInstance(
            SessionDao sessionDao, 
            MessageDao messageDao, 
            OpenAIService openAIService) {
        if (instance == null) {
            instance = new ChatRepository(sessionDao, messageDao, openAIService);
        }
        return instance;
    }
    
    /**
     * Set the OpenAI service
     */
    public void setOpenAIService(OpenAIService service) {
        this.openAIService = service;
    }
    
    // Session Operations
    
    public Session createSession(String name, String provider, String model) {
        Log.d(TAG, "Creating session: name=" + name + ", provider=" + provider + ", model=" + model);
        
        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setName(name);
        session.setProvider(provider);
        session.setModel(model);
        session.setType(Session.TYPE_CHAT);
        session.setCreatedAt(System.currentTimeMillis());
        session.setUpdatedAt(System.currentTimeMillis());
        
        executor.execute(() -> sessionDao.insert(session));
        
        return session;
    }
    
    public Session createSession(String provider, String model) {
        return createSession("New Chat", provider, model);
    }
    
    public Session getSession(String sessionId) {
        return sessionDao.getSessionById(sessionId);
    }
    
    public LiveData<Session> getSessionLive(String sessionId) {
        return sessionDao.getSessionByIdLive(sessionId);
    }
    
    public List<Session> getAllSessions() {
        return sessionDao.getAllSessions();
    }
    
    public LiveData<List<Session>> getAllSessionsLive() {
        return sessionDao.getAllSessionsLive();
    }
    
    public void updateSession(Session session) {
        session.touch();
        executor.execute(() -> sessionDao.update(session));
    }
    
    public void deleteSession(Session session) {
        executor.execute(() -> sessionDao.delete(session));
    }
    
    public void deleteSession(String sessionId) {
        executor.execute(() -> sessionDao.deleteById(sessionId));
    }
    
    public void setSessionStarred(String sessionId, boolean starred) {
        executor.execute(() -> sessionDao.setStarred(sessionId, starred));
    }
    
    public void renameSession(String sessionId, String newName) {
        executor.execute(() -> sessionDao.updateName(sessionId, newName, System.currentTimeMillis()));
    }
    
    // Message Operations
    
    public List<Message> getMessages(String sessionId) {
        return messageDao.getMessagesBySession(sessionId);
    }
    
    public LiveData<List<Message>> getMessagesLive(String sessionId) {
        return messageDao.getMessagesBySessionLive(sessionId);
    }
    
    /**
     * Send a message and get AI response
     */
    public void sendMessage(String sessionId, String content, ChatCallback callback) {
        sendMessageWithFile(sessionId, content, content, callback);
    }
    
    /**
     * Send a message with file content
     */
    public void sendMessageWithFile(String sessionId, String displayContent, String apiContent, ChatCallback callback) {
        sendMessageWithSystem(sessionId, displayContent, apiContent, "", callback);
    }
    
    /**
     * Send a message with file content and custom system prompt
     * @param sessionId Session ID
     * @param displayContent Content to display in UI (user input only)
     * @param apiContent Content to send to API (user input + file content)
     * @param skillSystemPrompt Custom system prompt from skills (overrides session's system prompt)
     * @param callback Callback for the operation
     */
    public void sendMessageWithSystem(String sessionId, String displayContent, String apiContent, 
                                      String skillSystemPrompt, ChatCallback callback) {
        executor.execute(() -> {
            try {
                // Check if OpenAI service is initialized
                if (openAIService == null) {
                    postError(callback, "API服务未初始化，请检查提供商配置");
                    return;
                }
                
                // Get session info
                Session session = sessionDao.getSessionById(sessionId);
                if (session == null) {
                    postError(callback, "会话不存在");
                    return;
                }
                
                // Check if model is set
                if (session.getModel() == null || session.getModel().isEmpty()) {
                    postError(callback, "未设置模型，请先选择要使用的模型");
                    return;
                }
                
                // Create user message - use displayContent for UI
                Message userMessage = new Message();
                userMessage.setId(UUID.randomUUID().toString());
                userMessage.setSessionId(sessionId);
                userMessage.setRole(Message.ROLE_USER);
                userMessage.setContent(displayContent); // Only show user input in UI
                userMessage.setTimestamp(System.currentTimeMillis());
                userMessage.setUpdatedAt(System.currentTimeMillis());
                
                // Save user message
                messageDao.insert(userMessage);
                sessionDao.updateTimestamp(sessionId, System.currentTimeMillis());
                
                // Create assistant message (placeholder)
                Message assistantMessage = new Message();
                assistantMessage.setId(UUID.randomUUID().toString());
                assistantMessage.setSessionId(sessionId);
                assistantMessage.setRole(Message.ROLE_ASSISTANT);
                assistantMessage.setContent("");
                assistantMessage.setGenerating(true);
                assistantMessage.setAiProvider(session.getProvider());
                assistantMessage.setModel(session.getModel());
                assistantMessage.setTimestamp(System.currentTimeMillis());
                assistantMessage.setUpdatedAt(System.currentTimeMillis());
                
                messageDao.insert(assistantMessage);
                
                // Build request - use apiContent for API (includes file content)
                // Use skillSystemPrompt if provided, otherwise use session's system prompt
                ChatRequest request = buildChatRequest(session, apiContent, skillSystemPrompt);
                
                Log.d(TAG, "Sending request to API: model=" + session.getModel() + ", provider=" + session.getProvider());
                
                // Call API
                openAIService.chatCompletion(request, new OpenAIService.ChatCallback() {
                    @Override
                    public void onStart() {
                        mainHandler.post(() -> callback.onStart());
                    }
                    
                    @Override
                    public void onChunk(String chunk) {
                        // Append content to message
                        assistantMessage.appendContent(chunk);
                        messageDao.updateContent(
                            assistantMessage.getId(), 
                            assistantMessage.getContent(), 
                            System.currentTimeMillis()
                        );
                        mainHandler.post(() -> callback.onChunk(chunk));
                    }
                    
                    @Override
                    public void onComplete(ChatResponse response) {
                        // Update message with final data
                        assistantMessage.setGenerating(false);
                        assistantMessage.setFinishReason(response.getFinishReason());
                        
                        if (response.getUsage() != null) {
                            assistantMessage.setInputTokens(response.getUsage().getPromptTokens());
                            assistantMessage.setOutputTokens(response.getUsage().getCompletionTokens());
                            assistantMessage.setTotalTokens(response.getUsage().getTotalTokens());
                        }
                        
                        messageDao.update(assistantMessage);
                        sessionDao.updateTimestamp(sessionId, System.currentTimeMillis());
                        
                        mainHandler.post(() -> callback.onComplete(response));
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "API Error: " + error);
                        // Update message with error
                        assistantMessage.setGenerating(false);
                        assistantMessage.setError(error);
                        messageDao.setError(assistantMessage.getId(), error, null);
                        
                        mainHandler.post(() -> callback.onError(error));
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                postError(callback, "发送消息失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * Post error to callback on main thread
     */
    private void postError(ChatCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
    
    /**
     * Build a chat request from session and user input
     * @param session Session info
     * @param userContent User content for API
     * @param skillSystemPrompt Custom system prompt from skills (overrides session's system prompt)
     */
    private ChatRequest buildChatRequest(Session session, String userContent, String skillSystemPrompt) {
        ChatRequest request = new ChatRequest();
        request.setModel(session.getModel());
        
        List<ChatRequest.Message> messages = new ArrayList<>();
        
        // Add system message - prefer skillSystemPrompt over session's system prompt
        String systemPrompt = skillSystemPrompt;
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = session.getSystemPrompt();
        }
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new ChatRequest.Message("system", systemPrompt));
        }
        
        // Add conversation history (if max context messages is set)
        int maxContext = session.getMaxContextMessages() != null ? session.getMaxContextMessages() : 20;
        if (maxContext > 0) {
            List<Message> history = messageDao.getRecentMessages(session.getId(), maxContext);
            // Reverse to get oldest first
            for (int i = history.size() - 1; i >= 0; i--) {
                Message msg = history.get(i);
                if (!msg.isUser() && !msg.isAssistant()) continue;
                if (msg.hasError()) continue;
                messages.add(new ChatRequest.Message(msg.getRole(), msg.getContent()));
            }
        }
        
        // Add current user message
        messages.add(new ChatRequest.Message("user", userContent));
        
        request.setMessages(messages);
        request.setStream(true);
        
        // Add optional parameters
        if (session.getTemperature() != null) {
            request.setTemperature(session.getTemperature());
        }
        if (session.getTopP() != null) {
            request.setTopP(session.getTopP());
        }
        if (session.getMaxTokens() != null) {
            request.setMaxTokens(session.getMaxTokens());
        }
        
        return request;
    }
    
    public void deleteMessage(Message message) {
        executor.execute(() -> messageDao.delete(message));
    }
    
    public void updateMessage(Message message) {
        message.setUpdatedAt(System.currentTimeMillis());
        executor.execute(() -> messageDao.update(message));
    }
    
    public void clearMessages(String sessionId) {
        executor.execute(() -> {
            messageDao.deleteMessagesBySession(sessionId);
            sessionDao.updateTimestamp(sessionId, System.currentTimeMillis());
        });
    }
    
    // Callback Interface
    
    public interface ChatCallback {
        void onStart();
        void onChunk(String chunk);
        void onComplete(ChatResponse response);
        void onError(String error);
    }
    
    // Cleanup
    
    public void cleanup() {
        executor.shutdown();
        instance = null;
    }
}

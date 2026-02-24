package com.chatbox.app.data.repository;

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
 * 
 * This class acts as a single source of truth for chat data.
 * It coordinates between the database (local) and API service (remote).
 * 
 * Responsibilities:
 * - Manage sessions (create, read, update, delete)
 * - Manage messages (create, read, update, delete)
 * - Handle API communication for chat
 * - Coordinate between local and remote data sources
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ChatRepository {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "ChatRepository";
    
    /**
     * Singleton instance
     */
    private static ChatRepository instance;
    
    /**
     * Session DAO for database operations
     */
    private final SessionDao sessionDao;
    
    /**
     * Message DAO for database operations
     */
    private final MessageDao messageDao;
    
    /**
     * OpenAI service for API calls
     */
    private final OpenAIService openAIService;
    
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
     * @param sessionDao Session DAO
     * @param messageDao Message DAO
     * @param openAIService OpenAI service
     */
    private ChatRepository(SessionDao sessionDao, MessageDao messageDao, OpenAIService openAIService) {
        this.sessionDao = sessionDao;
        this.messageDao = messageDao;
        this.openAIService = openAIService;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Get the singleton instance
     * 
     * @param sessionDao Session DAO
     * @param messageDao Message DAO
     * @param openAIService OpenAI service
     * @return ChatRepository instance
     */
    public static synchronized ChatRepository getInstance(
            SessionDao sessionDao, 
            MessageDao messageDao, 
            OpenAIService openAIService) {
        if (instance == null) {
            instance = new ChatRepository(sessionDao, messageDao, openAIService);
        }
        return instance;
    }
    
    // =========================================================================
    // Session Operations
    // =========================================================================
    
    /**
     * Create a new chat session
     * 
     * @param name Session name
     * @param provider AI provider ID
     * @param model AI model ID
     * @return The created session
     */
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
    
    /**
     * Create a new chat session with default name
     * 
     * @param provider AI provider ID
     * @param model AI model ID
     * @return The created session
     */
    public Session createSession(String provider, String model) {
        return createSession("New Chat", provider, model);
    }
    
    /**
     * Get a session by ID
     * 
     * @param sessionId Session ID
     * @return The session, or null if not found
     */
    public Session getSession(String sessionId) {
        return sessionDao.getSessionById(sessionId);
    }
    
    /**
     * Get a session by ID (LiveData)
     * 
     * @param sessionId Session ID
     * @return LiveData containing the session
     */
    public LiveData<Session> getSessionLive(String sessionId) {
        return sessionDao.getSessionByIdLive(sessionId);
    }
    
    /**
     * Get all sessions
     * 
     * @return List of all sessions
     */
    public List<Session> getAllSessions() {
        return sessionDao.getAllSessions();
    }
    
    /**
     * Get all sessions (LiveData)
     * 
     * @return LiveData containing list of sessions
     */
    public LiveData<List<Session>> getAllSessionsLive() {
        return sessionDao.getAllSessionsLive();
    }
    
    /**
     * Update a session
     * 
     * @param session The session to update
     */
    public void updateSession(Session session) {
        session.touch();
        executor.execute(() -> sessionDao.update(session));
    }
    
    /**
     * Delete a session
     * This will also delete all messages in the session
     * 
     * @param session The session to delete
     */
    public void deleteSession(Session session) {
        executor.execute(() -> sessionDao.delete(session));
    }
    
    /**
     * Delete a session by ID
     * 
     * @param sessionId The session ID to delete
     */
    public void deleteSession(String sessionId) {
        executor.execute(() -> sessionDao.deleteById(sessionId));
    }
    
    /**
     * Set the starred status of a session
     * 
     * @param sessionId Session ID
     * @param starred Starred status
     */
    public void setSessionStarred(String sessionId, boolean starred) {
        executor.execute(() -> sessionDao.setStarred(sessionId, starred));
    }
    
    /**
     * Rename a session
     * 
     * @param sessionId Session ID
     * @param newName New name
     */
    public void renameSession(String sessionId, String newName) {
        executor.execute(() -> sessionDao.updateName(sessionId, newName, System.currentTimeMillis()));
    }
    
    // =========================================================================
    // Message Operations
    // =========================================================================
    
    /**
     * Get all messages for a session
     * 
     * @param sessionId Session ID
     * @return List of messages
     */
    public List<Message> getMessages(String sessionId) {
        return messageDao.getMessagesBySession(sessionId);
    }
    
    /**
     * Get all messages for a session (LiveData)
     * 
     * @param sessionId Session ID
     * @return LiveData containing list of messages
     */
    public LiveData<List<Message>> getMessagesLive(String sessionId) {
        return messageDao.getMessagesBySessionLive(sessionId);
    }
    
    /**
     * Send a message and get AI response
     * 
     * @param sessionId Session ID
     * @param content Message content
     * @param callback Callback for response handling
     */
    public void sendMessage(String sessionId, String content, ChatCallback callback) {
        executor.execute(() -> {
            try {
                // Get session info
                Session session = sessionDao.getSessionById(sessionId);
                if (session == null) {
                    callback.onError("Session not found");
                    return;
                }
                
                // Create user message
                Message userMessage = new Message();
                userMessage.setId(UUID.randomUUID().toString());
                userMessage.setSessionId(sessionId);
                userMessage.setRole(Message.ROLE_USER);
                userMessage.setContent(content);
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
                
                // Build request
                ChatRequest request = buildChatRequest(session, content);
                
                // Call API
                openAIService.chatCompletion(request, new OpenAIService.ChatCallback() {
                    @Override
                    public void onStart() {
                        callback.onStart();
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
                        callback.onChunk(chunk);
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
                        
                        callback.onComplete(response);
                    }
                    
                    @Override
                    public void onError(String error) {
                        // Update message with error
                        assistantMessage.setGenerating(false);
                        assistantMessage.setError(error);
                        messageDao.setError(assistantMessage.getId(), error, null);
                        
                        callback.onError(error);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Build a chat request from session and user input
     * 
     * @param session The session
     * @param userContent User message content
     * @return ChatRequest object
     */
    private ChatRequest buildChatRequest(Session session, String userContent) {
        ChatRequest request = new ChatRequest();
        request.setModel(session.getModel());
        
        List<ChatRequest.Message> messages = new ArrayList<>();
        
        // Add system message if exists
        if (session.getSystemPrompt() != null && !session.getSystemPrompt().isEmpty()) {
            messages.add(new ChatRequest.Message("system", session.getSystemPrompt()));
        }
        
        // Add user message
        messages.add(new ChatRequest.Message("user", userContent));
        
        request.setMessages(messages);
        request.setStream(true);
        
        // Add optional parameters
        if (session.getTemperature() != null) {
            request.setTemperature(session.getTemperature());
        }
        if (session.getMaxTokens() != null) {
            request.setMaxTokens(session.getMaxTokens());
        }
        
        return request;
    }
    
    /**
     * Delete a message
     * 
     * @param message The message to delete
     */
    public void deleteMessage(Message message) {
        executor.execute(() -> messageDao.delete(message));
    }
    
    /**
     * Update a message
     * 
     * @param message The message to update
     */
    public void updateMessage(Message message) {
        message.setUpdatedAt(System.currentTimeMillis());
        executor.execute(() -> messageDao.update(message));
    }
    
    /**
     * Clear all messages in a session
     * 
     * @param sessionId Session ID
     */
    public void clearMessages(String sessionId) {
        executor.execute(() -> {
            messageDao.deleteMessagesBySession(sessionId);
            sessionDao.updateTimestamp(sessionId, System.currentTimeMillis());
        });
    }
    
    // =========================================================================
    // Callback Interface
    // =========================================================================
    
    /**
     * Callback interface for chat operations
     */
    public interface ChatCallback {
        /**
         * Called when the API call starts
         */
        void onStart();
        
        /**
         * Called when a content chunk is received
         * 
         * @param chunk The content chunk
         */
        void onChunk(String chunk);
        
        /**
         * Called when the response is complete
         * 
         * @param response The complete response
         */
        void onComplete(ChatResponse response);
        
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
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        executor.shutdown();
        instance = null;
    }
}

package com.chatbox.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.chatbox.app.data.entity.Message;

import java.util.List;

/**
 * MessageDao - Data Access Object for Message entity
 * 
 * This interface defines database operations for the Message entity.
 * Room automatically generates implementations for these methods.
 * 
 * Available Operations:
 * - Insert: Add new messages
 * - Update: Modify existing messages
 * - Delete: Remove messages
 * - Query: Retrieve messages with various filters
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
@Dao
public interface MessageDao {
    
    // =========================================================================
    // Insert Operations
    // =========================================================================
    
    /**
     * Insert a single message into the database
     * If a message with the same ID already exists, it will be replaced
     * 
     * @param message The message to insert
     * @return The row ID of the inserted message
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Message message);
    
    /**
     * Insert multiple messages into the database
     * 
     * @param messages The messages to insert
     * @return Array of row IDs for the inserted messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(Message... messages);
    
    /**
     * Insert a list of messages into the database
     * 
     * @param messages The list of messages to insert
     * @return List of row IDs for the inserted messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Message> messages);
    
    // =========================================================================
    // Update Operations
    // =========================================================================
    
    /**
     * Update an existing message in the database
     * 
     * @param message The message to update
     * @return The number of rows updated (should be 1)
     */
    @Update
    int update(Message message);
    
    /**
     * Update multiple messages in the database
     * 
     * @param messages The messages to update
     * @return The number of rows updated
     */
    @Update
    int updateAll(Message... messages);
    
    /**
     * Update the content of a message
     * Used during streaming responses to append content
     * 
     * @param messageId The ID of the message to update
     * @param content The new content
     * @param updatedAt The update timestamp
     */
    @Query("UPDATE messages SET content = :content, updated_at = :updatedAt WHERE id = :messageId")
    void updateContent(String messageId, String content, long updatedAt);
    
    /**
     * Append content to a message
     * Used during streaming responses
     * 
     * @param messageId The ID of the message
     * @param content The content to append
     * @param updatedAt The update timestamp
     */
    @Query("UPDATE messages SET content = content || :content, updated_at = :updatedAt WHERE id = :messageId")
    void appendContent(String messageId, String content, long updatedAt);
    
    /**
     * Set the generating status of a message
     * 
     * @param messageId The ID of the message
     * @param generating The generating status
     */
    @Query("UPDATE messages SET generating = :generating WHERE id = :messageId")
    void setGenerating(String messageId, boolean generating);
    
    /**
     * Set the error for a message
     * 
     * @param messageId The ID of the message
     * @param error The error message
     * @param errorCode The error code
     */
    @Query("UPDATE messages SET error = :error, error_code = :errorCode, generating = 0 WHERE id = :messageId")
    void setError(String messageId, String error, Integer errorCode);
    
    /**
     * Update token usage for a message
     * 
     * @param messageId The ID of the message
     * @param inputTokens Input tokens used
     * @param outputTokens Output tokens used
     * @param totalTokens Total tokens used
     */
    @Query("UPDATE messages SET input_tokens = :inputTokens, output_tokens = :outputTokens, " +
           "total_tokens = :totalTokens WHERE id = :messageId")
    void updateTokenUsage(String messageId, Integer inputTokens, Integer outputTokens, Integer totalTokens);
    
    /**
     * Update the finish reason for a message
     * 
     * @param messageId The ID of the message
     * @param finishReason The finish reason
     */
    @Query("UPDATE messages SET finish_reason = :finishReason WHERE id = :messageId")
    void updateFinishReason(String messageId, String finishReason);
    
    // =========================================================================
    // Delete Operations
    // =========================================================================
    
    /**
     * Delete a message from the database
     * 
     * @param message The message to delete
     * @return The number of rows deleted (should be 1)
     */
    @Delete
    int delete(Message message);
    
    /**
     * Delete multiple messages from the database
     * 
     * @param messages The messages to delete
     * @return The number of rows deleted
     */
    @Delete
    int deleteAll(Message... messages);
    
    /**
     * Delete a message by its ID
     * 
     * @param messageId The ID of the message to delete
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    void deleteById(String messageId);
    
    /**
     * Delete all messages from a specific session
     * 
     * @param sessionId The ID of the session
     */
    @Query("DELETE FROM messages WHERE session_id = :sessionId")
    void deleteMessagesBySession(String sessionId);
    
    /**
     * Delete all messages from the database
     * Use with caution
     */
    @Query("DELETE FROM messages")
    void deleteAll();
    
    // =========================================================================
    // Query Operations - Single Message
    // =========================================================================
    
    /**
     * Get a message by its ID
     * 
     * @param messageId The ID of the message
     * @return The message, or null if not found
     */
    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    Message getMessageById(String messageId);
    
    /**
     * Get a message by its ID (LiveData)
     * 
     * @param messageId The ID of the message
     * @return LiveData containing the message
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    LiveData<Message> getMessageByIdLive(String messageId);
    
    // =========================================================================
    // Query Operations - Multiple Messages
    // =========================================================================
    
    /**
     * Get all messages for a specific session
     * Ordered by timestamp ascending (oldest first)
     * 
     * @param sessionId The ID of the session
     * @return List of messages in the session
     */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    List<Message> getMessagesBySession(String sessionId);
    
    /**
     * Get all messages for a specific session (LiveData)
     * 
     * @param sessionId The ID of the session
     * @return LiveData containing list of messages
     */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    LiveData<List<Message>> getMessagesBySessionLive(String sessionId);
    
    /**
     * Get messages for a session with a limit
     * Useful for pagination
     * 
     * @param sessionId The ID of the session
     * @param limit Maximum number of messages to return
     * @return List of messages
     */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY timestamp ASC LIMIT :limit")
    List<Message> getMessagesBySessionLimited(String sessionId, int limit);
    
    /**
     * Get the most recent messages for a session
     * 
     * @param sessionId The ID of the session
     * @param limit Maximum number of messages to return
     * @return List of recent messages
     */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    List<Message> getRecentMessages(String sessionId, int limit);
    
    /**
     * Get all messages from the database
     * 
     * @return List of all messages
     */
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    List<Message> getAllMessages();
    
    // =========================================================================
    // Query Operations - Count
    // =========================================================================
    
    /**
     * Get the total count of messages
     * 
     * @return The number of messages
     */
    @Query("SELECT COUNT(*) FROM messages")
    int getMessageCount();
    
    /**
     * Get the count of messages in a session
     * 
     * @param sessionId The ID of the session
     * @return The number of messages in the session
     */
    @Query("SELECT COUNT(*) FROM messages WHERE session_id = :sessionId")
    int getMessageCountBySession(String sessionId);
    
    // =========================================================================
    // Search Operations
    // =========================================================================
    
    /**
     * Search messages by content
     * 
     * @param query The search query
     * @return List of messages matching the query
     */
    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    List<Message> searchMessages(String query);
    
    /**
     * Search messages in a specific session
     * 
     * @param sessionId The ID of the session
     * @param query The search query
     * @return List of matching messages
     */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId AND content LIKE '%' || :query || '%' ORDER BY timestamp ASC")
    List<Message> searchMessagesInSession(String sessionId, String query);
    
    // =========================================================================
    // Special Queries
    // =========================================================================
    
    /**
     * Get the last message in a session
     * 
     * @param sessionId The ID of the session
     * @return The last message, or null if no messages
     */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY timestamp DESC LIMIT 1")
    Message getLastMessage(String sessionId);
    
    /**
     * Get the first message in a session
     * 
     * @param sessionId The ID of the session
     * @return The first message, or null if no messages
     */
    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY timestamp ASC LIMIT 1")
    Message getFirstMessage(String sessionId);
    
    /**
     * Get all messages with errors
     * 
     * @return List of messages with errors
     */
    @Query("SELECT * FROM messages WHERE error IS NOT NULL ORDER BY timestamp DESC")
    List<Message> getMessagesWithErrors();
    
    /**
     * Get all generating messages
     * Useful for resuming interrupted generations
     * 
     * @return List of generating messages
     */
    @Query("SELECT * FROM messages WHERE generating = 1")
    List<Message> getGeneratingMessages();
    
    // =========================================================================
    // Transaction Operations
    // =========================================================================
    
    /**
     * Insert a message and update the session's timestamp
     * This ensures the session appears at the top of the list
     * 
     * @param message The message to insert
     * @param sessionDao The SessionDao for updating the session
     */
    @Transaction
    default void insertAndUpdateSession(Message message, SessionDao sessionDao) {
        insert(message);
        sessionDao.updateTimestamp(message.getSessionId(), System.currentTimeMillis());
    }
    
    /**
     * Delete all messages from a session and update the session
     * 
     * @param sessionId The ID of the session
     * @param sessionDao The SessionDao for updating the session
     */
    @Transaction
    default void clearSessionMessages(String sessionId, SessionDao sessionDao) {
        deleteMessagesBySession(sessionId);
        sessionDao.updateTimestamp(sessionId, System.currentTimeMillis());
    }
}

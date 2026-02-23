package com.chatbox.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.chatbox.app.data.entity.Session;

import java.util.List;

/**
 * SessionDao - Data Access Object for Session entity
 * 
 * This interface defines database operations for the Session entity.
 * Room automatically generates implementations for these methods.
 * 
 * Available Operations:
 * - Insert: Add new sessions
 * - Update: Modify existing sessions
 * - Delete: Remove sessions
 * - Query: Retrieve sessions with various filters
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
@Dao
public interface SessionDao {
    
    // =========================================================================
    // Insert Operations
    // =========================================================================
    
    /**
     * Insert a single session into the database
     * If a session with the same ID already exists, it will be replaced
     * 
     * @param session The session to insert
     * @return The row ID of the inserted session
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Session session);
    
    /**
     * Insert multiple sessions into the database
     * 
     * @param sessions The sessions to insert
     * @return Array of row IDs for the inserted sessions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(Session... sessions);
    
    /**
     * Insert a list of sessions into the database
     * 
     * @param sessions The list of sessions to insert
     * @return List of row IDs for the inserted sessions
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<Session> sessions);
    
    // =========================================================================
    // Update Operations
    // =========================================================================
    
    /**
     * Update an existing session in the database
     * 
     * @param session The session to update
     * @return The number of rows updated (should be 1)
     */
    @Update
    int update(Session session);
    
    /**
     * Update multiple sessions in the database
     * 
     * @param sessions The sessions to update
     * @return The number of rows updated
     */
    @Update
    int updateAll(Session... sessions);
    
    /**
     * Update the session name
     * 
     * @param sessionId The ID of the session to update
     * @param name The new name
     */
    @Query("UPDATE sessions SET name = :name, updated_at = :updatedAt WHERE id = :sessionId")
    void updateName(String sessionId, String name, long updatedAt);
    
    /**
     * Update the session's updated_at timestamp
     * 
     * @param sessionId The ID of the session
     * @param updatedAt The new timestamp
     */
    @Query("UPDATE sessions SET updated_at = :updatedAt WHERE id = :sessionId")
    void updateTimestamp(String sessionId, long updatedAt);
    
    /**
     * Toggle the starred status of a session
     * 
     * @param sessionId The ID of the session
     * @param starred The new starred status
     */
    @Query("UPDATE sessions SET starred = :starred WHERE id = :sessionId")
    void setStarred(String sessionId, boolean starred);
    
    // =========================================================================
    // Delete Operations
    // =========================================================================
    
    /**
     * Delete a session from the database
     * This will also delete all associated messages due to cascade delete
     * 
     * @param session The session to delete
     * @return The number of rows deleted (should be 1)
     */
    @Delete
    int delete(Session session);
    
    /**
     * Delete multiple sessions from the database
     * 
     * @param sessions The sessions to delete
     * @return The number of rows deleted
     */
    @Delete
    int deleteAll(Session... sessions);
    
    /**
     * Delete a session by its ID
     * 
     * @param sessionId The ID of the session to delete
     */
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    void deleteById(String sessionId);
    
    /**
     * Delete all sessions from the database
     * Use with caution - this will delete all messages too
     */
    @Query("DELETE FROM sessions")
    void deleteAll();
    
    /**
     * Delete all hidden sessions
     */
    @Query("DELETE FROM sessions WHERE hidden = 1")
    void deleteHiddenSessions();
    
    // =========================================================================
    // Query Operations - Single Session
    // =========================================================================
    
    /**
     * Get a session by its ID
     * 
     * @param sessionId The ID of the session
     * @return The session, or null if not found
     */
    @Query("SELECT * FROM sessions WHERE id = :sessionId LIMIT 1")
    Session getSessionById(String sessionId);
    
    /**
     * Get a session by its ID (LiveData)
     * This allows UI components to observe changes automatically
     * 
     * @param sessionId The ID of the session
     * @return LiveData containing the session
     */
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    LiveData<Session> getSessionByIdLive(String sessionId);
    
    // =========================================================================
    // Query Operations - Multiple Sessions
    // =========================================================================
    
    /**
     * Get all sessions ordered by updated_at descending (most recent first)
     * 
     * @return List of all sessions
     */
    @Query("SELECT * FROM sessions ORDER BY updated_at DESC")
    List<Session> getAllSessions();
    
    /**
     * Get all sessions ordered by updated_at descending (LiveData)
     * 
     * @return LiveData containing list of all sessions
     */
    @Query("SELECT * FROM sessions ORDER BY updated_at DESC")
    LiveData<List<Session>> getAllSessionsLive();
    
    /**
     * Get all visible (not hidden) sessions
     * 
     * @return List of visible sessions
     */
    @Query("SELECT * FROM sessions WHERE hidden = 0 ORDER BY updated_at DESC")
    List<Session> getVisibleSessions();
    
    /**
     * Get all visible sessions (LiveData)
     * 
     * @return LiveData containing list of visible sessions
     */
    @Query("SELECT * FROM sessions WHERE hidden = 0 ORDER BY updated_at DESC")
    LiveData<List<Session>> getVisibleSessionsLive();
    
    /**
     * Get all starred sessions
     * 
     * @return List of starred sessions
     */
    @Query("SELECT * FROM sessions WHERE starred = 1 ORDER BY updated_at DESC")
    List<Session> getStarredSessions();
    
    /**
     * Get all starred sessions (LiveData)
     * 
     * @return LiveData containing list of starred sessions
     */
    @Query("SELECT * FROM sessions WHERE starred = 1 ORDER BY updated_at DESC")
    LiveData<List<Session>> getStarredSessionsLive();
    
    /**
     * Get sessions by provider
     * 
     * @param provider The provider ID
     * @return List of sessions using the specified provider
     */
    @Query("SELECT * FROM sessions WHERE provider = :provider ORDER BY updated_at DESC")
    List<Session> getSessionsByProvider(String provider);
    
    // =========================================================================
    // Query Operations - Count
    // =========================================================================
    
    /**
     * Get the total count of sessions
     * 
     * @return The number of sessions
     */
    @Query("SELECT COUNT(*) FROM sessions")
    int getSessionCount();
    
    /**
     * Get the count of visible sessions
     * 
     * @return The number of visible sessions
     */
    @Query("SELECT COUNT(*) FROM sessions WHERE hidden = 0")
    int getVisibleSessionCount();
    
    /**
     * Get the count of starred sessions
     * 
     * @return The number of starred sessions
     */
    @Query("SELECT COUNT(*) FROM sessions WHERE starred = 1")
    int getStarredSessionCount();
    
    // =========================================================================
    // Search Operations
    // =========================================================================
    
    /**
     * Search sessions by name
     * 
     * @param query The search query
     * @return List of sessions matching the query
     */
    @Query("SELECT * FROM sessions WHERE name LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    List<Session> searchSessions(String query);
    
    /**
     * Search sessions by name (LiveData)
     * 
     * @param query The search query
     * @return LiveData containing list of matching sessions
     */
    @Query("SELECT * FROM sessions WHERE name LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    LiveData<List<Session>> searchSessionsLive(String query);
    
    // =========================================================================
    // Transaction Operations
    // =========================================================================
    
    /**
     * Insert a session and return the inserted session
     * This is useful when you need the session with its generated ID
     * 
     * @param session The session to insert
     * @return The inserted session
     */
    @Transaction
    default Session insertAndReturn(Session session) {
        insert(session);
        return getSessionById(session.getId());
    }
    
    /**
     * Delete a session by ID and all its messages
     * This is a convenience method for cascade delete
     * 
     * @param sessionId The ID of the session to delete
     */
    @Transaction
    default void deleteSessionWithMessages(String sessionId) {
        deleteById(sessionId);
    }
}

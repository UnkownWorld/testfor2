package com.chatbox.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.chatbox.app.data.database.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Session - Entity class representing a chat session
 * 
 * This class defines the structure of a chat session stored in the database.
 * A session represents a conversation between the user and an AI assistant.
 * 
 * Database Schema:
 * - Table name: sessions
 * - Primary key: id (String UUID)
 * 
 * Relationships:
 * - One-to-Many with Message: A session can have multiple messages
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
@Entity(tableName = "sessions")
@TypeConverters({Converters.class})
public class Session {
    
    // =========================================================================
    // Constants
    // =========================================================================
    
    /**
     * Session type: Regular chat session
     */
    public static final String TYPE_CHAT = "chat";
    
    /**
     * Session type: Picture generation session
     */
    public static final String TYPE_PICTURE = "picture";
    
    // =========================================================================
    // Fields (Database Columns)
    // =========================================================================
    
    /**
     * Unique identifier for the session
     * This is the primary key for the sessions table
     */
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    
    /**
     * Session type: "chat" or "picture"
     * Default is "chat"
     */
    @ColumnInfo(name = "type")
    private String type;
    
    /**
     * Display name of the session
     * This is shown in the session list
     */
    @ColumnInfo(name = "name")
    private String name;
    
    /**
     * URL of the session picture (if applicable)
     * Used for picture-type sessions
     */
    @ColumnInfo(name = "pic_url")
    private String picUrl;
    
    /**
     * Whether the session is starred/favorited
     * Starred sessions can be filtered and shown first
     */
    @ColumnInfo(name = "starred")
    private boolean starred;
    
    /**
     * Whether the session is hidden from the list
     * Hidden sessions are not shown in the main list
     */
    @ColumnInfo(name = "hidden")
    private boolean hidden;
    
    /**
     * ID of the copilot/assistant used in this session
     * References a predefined assistant configuration
     */
    @ColumnInfo(name = "copilot_id")
    private String copilotId;
    
    /**
     * Key for the assistant avatar image
     * Used to display the assistant's avatar
     */
    @ColumnInfo(name = "assistant_avatar_key")
    private String assistantAvatarKey;
    
    /**
     * AI provider ID for this session
     * e.g., "openai", "claude", "gemini", etc.
     */
    @ColumnInfo(name = "provider")
    private String provider;
    
    /**
     * Model ID used in this session
     * e.g., "gpt-4", "claude-3-opus", etc.
     */
    @ColumnInfo(name = "model")
    private String model;
    
    /**
     * System prompt for this session
     * Defines the AI's behavior and personality
     */
    @ColumnInfo(name = "system_prompt")
    private String systemPrompt;
    
    /**
     * Temperature parameter for the model
     * Controls randomness: 0.0 = deterministic, 2.0 = very random
     */
    @ColumnInfo(name = "temperature")
    private Float temperature;
    
    /**
     * Top P parameter for the model
     * Controls diversity via nucleus sampling
     */
    @ColumnInfo(name = "top_p")
    private Float topP;
    
    /**
     * Maximum context length (in tokens)
     * Limits how much conversation history is sent to the API
     */
    @ColumnInfo(name = "context_length")
    private Integer contextLength;
    
    /**
     * Maximum context messages
     * Limits how many previous messages to include
     */
    @ColumnInfo(name = "max_context_messages")
    private Integer maxContextMessages;
    
    /**
     * Maximum tokens to generate
     * Limits the length of AI responses
     */
    @ColumnInfo(name = "max_tokens")
    private Integer maxTokens;
    
    /**
     * Whether streaming is enabled for this session
     */
    @ColumnInfo(name = "streaming_enabled")
    private boolean streamingEnabled;
    
    /**
     * Timestamp when the session was created
     * Stored as milliseconds since epoch
     */
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    /**
     * Timestamp when the session was last updated
     * Used for sorting sessions by recency
     */
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    // =========================================================================
    // Transient Fields (Not stored in database)
    // =========================================================================
    
    /**
     * List of messages in this session
     * This is loaded separately and not stored directly in the session table
     */
    @Ignore
    private List<Message> messages;
    
    // =========================================================================
    // Constructors
    // =========================================================================
    
    /**
     * Default constructor required by Room
     * Creates a new session with default values
     */
    public Session() {
        this.id = UUID.randomUUID().toString();
        this.type = TYPE_CHAT;
        this.name = "New Chat";
        this.starred = false;
        this.hidden = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.messages = new ArrayList<>();
    }
    
    /**
     * Constructor with name
     * 
     * @param name The display name for the session
     */
    @Ignore
    public Session(String name) {
        this();
        this.name = name;
    }
    
    /**
     * Constructor with all fields
     * 
     * @param id Session ID
     * @param name Session name
     * @param provider AI provider
     * @param model AI model
     */
    @Ignore
    public Session(String id, String name, String provider, String model) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.model = model;
        this.type = TYPE_CHAT;
        this.starred = false;
        this.hidden = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.messages = new ArrayList<>();
    }
    
    // =========================================================================
    // Getters and Setters
    // =========================================================================
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPicUrl() {
        return picUrl;
    }
    
    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }
    
    public boolean isStarred() {
        return starred;
    }
    
    public void setStarred(boolean starred) {
        this.starred = starred;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    public String getCopilotId() {
        return copilotId;
    }
    
    public void setCopilotId(String copilotId) {
        this.copilotId = copilotId;
    }
    
    public String getAssistantAvatarKey() {
        return assistantAvatarKey;
    }
    
    public void setAssistantAvatarKey(String assistantAvatarKey) {
        this.assistantAvatarKey = assistantAvatarKey;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public Float getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }
    
    public Float getTopP() {
        return topP;
    }
    
    public void setTopP(Float topP) {
        this.topP = topP;
    }
    
    public Integer getContextLength() {
        return contextLength;
    }
    
    public void setContextLength(Integer contextLength) {
        this.contextLength = contextLength;
    }
    
    public Integer getMaxContextMessages() {
        return maxContextMessages;
    }
    
    public void setMaxContextMessages(Integer maxContextMessages) {
        this.maxContextMessages = maxContextMessages;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }
    
    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    // =========================================================================
    // Helper Methods
    // =========================================================================
    
    /**
     * Update the updatedAt timestamp to current time
     * Call this when the session is modified
     */
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Check if this is a chat-type session
     * 
     * @return true if type is "chat"
     */
    public boolean isChat() {
        return TYPE_CHAT.equals(type);
    }
    
    /**
     * Check if this is a picture-type session
     * 
     * @return true if type is "picture"
     */
    public boolean isPicture() {
        return TYPE_PICTURE.equals(type);
    }
    
    /**
     * Get the display title for the session
     * Returns the name or a default title if name is empty
     * 
     * @return The display title
     */
    public String getDisplayTitle() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return "New Chat";
    }
    
    /**
     * Get a summary of the last message
     * 
     * @return Summary text or empty string if no messages
     */
    public String getLastMessageSummary() {
        if (messages != null && !messages.isEmpty()) {
            Message lastMessage = messages.get(messages.size() - 1);
            String content = lastMessage.getContentPreview();
            if (content.length() > 50) {
                return content.substring(0, 50) + "...";
            }
            return content;
        }
        return "";
    }
    
    // =========================================================================
    // Object Methods
    // =========================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return Objects.equals(id, session.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Session{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", provider='" + provider + '\'' +
                ", model='" + model + '\'' +
                ", createdAt=" + createdAt +
                ", messageCount=" + (messages != null ? messages.size() : 0) +
                '}';
    }
}

package com.chatbox.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.chatbox.app.data.database.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Message - Entity class representing a chat message
 * 
 * This class defines the structure of a chat message stored in the database.
 * A message belongs to a session and can be from the user, assistant, or system.
 * 
 * Database Schema:
 * - Table name: messages
 * - Primary key: id (String UUID)
 * - Foreign key: session_id references sessions(id)
 * - Index: session_id for efficient querying
 * 
 * Relationships:
 * - Many-to-One with Session: Each message belongs to one session
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
@Entity(
    tableName = "messages",
    foreignKeys = @ForeignKey(
        entity = Session.class,
        parentColumns = "id",
        childColumns = "session_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("session_id")}
)
@TypeConverters({Converters.class})
public class Message {
    
    // =========================================================================
    // Constants - Message Roles
    // =========================================================================
    
    /**
     * System role: Defines AI behavior and context
     */
    public static final String ROLE_SYSTEM = "system";
    
    /**
     * User role: Messages from the user
     */
    public static final String ROLE_USER = "user";
    
    /**
     * Assistant role: Messages from the AI assistant
     */
    public static final String ROLE_ASSISTANT = "assistant";
    
    /**
     * Tool role: Messages from tool/function calls
     */
    public static final String ROLE_TOOL = "tool";
    
    // =========================================================================
    // Constants - Content Part Types
    // =========================================================================
    
    /**
     * Text content type
     */
    public static final String TYPE_TEXT = "text";
    
    /**
     * Image content type
     */
    public static final String TYPE_IMAGE = "image";
    
    /**
     * Info content type
     */
    public static final String TYPE_INFO = "info";
    
    /**
     * Reasoning content type
     */
    public static final String TYPE_REASONING = "reasoning";
    
    /**
     * Tool call content type
     */
    public static final String TYPE_TOOL_CALL = "tool-call";
    
    // =========================================================================
    // Fields (Database Columns)
    // =========================================================================
    
    /**
     * Unique identifier for the message
     * This is the primary key for the messages table
     */
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    
    /**
     * ID of the session this message belongs to
     * Foreign key referencing sessions.id
     */
    @ColumnInfo(name = "session_id")
    private String sessionId;
    
    /**
     * Role of the message sender
     * One of: "system", "user", "assistant", "tool"
     */
    @ColumnInfo(name = "role")
    private String role;
    
    /**
     * Name of the sender (optional)
     * Used for identifying different system messages or tools
     */
    @ColumnInfo(name = "name")
    private String name;
    
    /**
     * Main text content of the message
     * This is the primary content displayed to the user
     */
    @ColumnInfo(name = "content")
    private String content;
    
    /**
     * Content parts as JSON string
     * Stores complex content structures (text, images, etc.)
     */
    @ColumnInfo(name = "content_parts_json")
    private String contentPartsJson;
    
    /**
     * AI provider that generated this message
     * e.g., "openai", "claude", "gemini"
     */
    @ColumnInfo(name = "ai_provider")
    private String aiProvider;
    
    /**
     * Model used to generate this message
     * e.g., "gpt-4", "claude-3-opus"
     */
    @ColumnInfo(name = "model")
    private String model;
    
    /**
     * Whether the message is currently being generated
     * True while streaming response from API
     */
    @ColumnInfo(name = "generating")
    private boolean generating;
    
    /**
     * Error message if the generation failed
     * Null if no error occurred
     */
    @ColumnInfo(name = "error")
    private String error;
    
    /**
     * Error code for programmatic error handling
     * HTTP status code or custom error code
     */
    @ColumnInfo(name = "error_code")
    private Integer errorCode;
    
    /**
     * Reasoning content (for models that show reasoning)
     * e.g., DeepSeek R1's thinking process
     */
    @ColumnInfo(name = "reasoning_content")
    private String reasoningContent;
    
    /**
     * Word count of the message content
     * Calculated for display purposes
     */
    @ColumnInfo(name = "word_count")
    private Integer wordCount;
    
    /**
     * Token count of the message
     * Estimated or actual token usage
     */
    @ColumnInfo(name = "token_count")
    private Integer tokenCount;
    
    /**
     * Input tokens used (from API response)
     */
    @ColumnInfo(name = "input_tokens")
    private Integer inputTokens;
    
    /**
     * Output tokens used (from API response)
     */
    @ColumnInfo(name = "output_tokens")
    private Integer outputTokens;
    
    /**
     * Total tokens used (from API response)
     */
    @ColumnInfo(name = "total_tokens")
    private Integer totalTokens;
    
    /**
     * Timestamp when the message was created
     * Stored as milliseconds since epoch
     */
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    
    /**
     * Timestamp when the message was last updated
     */
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    /**
     * First token latency in milliseconds
     * Time from request to first token received
     */
    @ColumnInfo(name = "first_token_latency")
    private Long firstTokenLatency;
    
    /**
     * Finish reason from the API
     * e.g., "stop", "length", "content_filter"
     */
    @ColumnInfo(name = "finish_reason")
    private String finishReason;
    
    /**
     * Whether this message is a summary/compaction
     * Used for context management
     */
    @ColumnInfo(name = "is_summary")
    private boolean isSummary;
    
    // =========================================================================
    // Transient Fields (Not stored in database)
    // =========================================================================
    
    /**
     * Content parts list (parsed from contentPartsJson)
     * Not stored directly, reconstructed from JSON
     */
    @Ignore
    private List<ContentPart> contentParts;
    
    /**
     * Files attached to this message
     * Stored separately or as part of contentParts
     */
    @Ignore
    private List<MessageFile> files;
    
    // =========================================================================
    // Constructors
    // =========================================================================
    
    /**
     * Default constructor required by Room
     * Creates a new message with default values
     */
    public Message() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.generating = false;
        this.isSummary = false;
        this.contentParts = new ArrayList<>();
        this.files = new ArrayList<>();
    }
    
    /**
     * Constructor for creating a user message
     * 
     * @param sessionId The session ID this message belongs to
     * @param content The message content
     */
    @Ignore
    public Message(String sessionId, String content) {
        this();
        this.sessionId = sessionId;
        this.role = ROLE_USER;
        this.content = content;
    }
    
    /**
     * Constructor for creating a message with specific role
     * 
     * @param sessionId The session ID this message belongs to
     * @param role The message role
     * @param content The message content
     */
    @Ignore
    public Message(String sessionId, String role, String content) {
        this();
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
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
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContentPartsJson() {
        return contentPartsJson;
    }
    
    public void setContentPartsJson(String contentPartsJson) {
        this.contentPartsJson = contentPartsJson;
    }
    
    public String getAiProvider() {
        return aiProvider;
    }
    
    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public boolean isGenerating() {
        return generating;
    }
    
    public void setGenerating(boolean generating) {
        this.generating = generating;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public Integer getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getReasoningContent() {
        return reasoningContent;
    }
    
    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }
    
    public Integer getWordCount() {
        return wordCount;
    }
    
    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }
    
    public Integer getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public Integer getInputTokens() {
        return inputTokens;
    }
    
    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }
    
    public Integer getOutputTokens() {
        return outputTokens;
    }
    
    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }
    
    public Integer getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getFirstTokenLatency() {
        return firstTokenLatency;
    }
    
    public void setFirstTokenLatency(Long firstTokenLatency) {
        this.firstTokenLatency = firstTokenLatency;
    }
    
    public String getFinishReason() {
        return finishReason;
    }
    
    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
    
    public boolean isSummary() {
        return isSummary;
    }
    
    public void setSummary(boolean summary) {
        isSummary = summary;
    }
    
    public List<ContentPart> getContentParts() {
        return contentParts;
    }
    
    public void setContentParts(List<ContentPart> contentParts) {
        this.contentParts = contentParts;
    }
    
    public List<MessageFile> getFiles() {
        return files;
    }
    
    public void setFiles(List<MessageFile> files) {
        this.files = files;
    }
    
    // =========================================================================
    // Helper Methods
    // =========================================================================
    
    /**
     * Update the updatedAt timestamp to current time
     * Call this when the message is modified
     */
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Check if this is a system message
     * 
     * @return true if role is "system"
     */
    public boolean isSystem() {
        return ROLE_SYSTEM.equals(role);
    }
    
    /**
     * Check if this is a user message
     * 
     * @return true if role is "user"
     */
    public boolean isUser() {
        return ROLE_USER.equals(role);
    }
    
    /**
     * Check if this is an assistant message
     * 
     * @return true if role is "assistant"
     */
    public boolean isAssistant() {
        return ROLE_ASSISTANT.equals(role);
    }
    
    /**
     * Check if this is a tool message
     * 
     * @return true if role is "tool"
     */
    public boolean isTool() {
        return ROLE_TOOL.equals(role);
    }
    
    /**
     * Check if this message has an error
     * 
     * @return true if error is not null
     */
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
    
    /**
     * Get a preview of the content (first 100 characters)
     * 
     * @return Content preview
     */
    public String getContentPreview() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        if (content.length() > 100) {
            return content.substring(0, 100) + "...";
        }
        return content;
    }
    
    /**
     * Append content to the message
     * Used during streaming responses
     * 
     * @param chunk The content chunk to append
     */
    public void appendContent(String chunk) {
        if (content == null) {
            content = chunk;
        } else {
            content += chunk;
        }
        touch();
    }
    
    /**
     * Get formatted timestamp for display
     * 
     * @return Formatted time string
     */
    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
    
    // =========================================================================
    // Object Methods
    // =========================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", role='" + role + '\'' +
                ", content='" + getContentPreview() + '\'' +
                ", generating=" + generating +
                ", timestamp=" + timestamp +
                '}';
    }
    
    // =========================================================================
    // Inner Classes
    // =========================================================================
    
    /**
     * ContentPart - Represents a part of message content
     * Can be text, image, info, reasoning, or tool-call
     */
    public static class ContentPart {
        private String type;
        private String text;
        private String storageKey;
        private String ocrResult;
        private Long startTime;
        private Long duration;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getStorageKey() { return storageKey; }
        public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
        
        public String getOcrResult() { return ocrResult; }
        public void setOcrResult(String ocrResult) { this.ocrResult = ocrResult; }
        
        public Long getStartTime() { return startTime; }
        public void setStartTime(Long startTime) { this.startTime = startTime; }
        
        public Long getDuration() { return duration; }
        public void setDuration(Long duration) { this.duration = duration; }
    }
    
    /**
     * MessageFile - Represents a file attached to a message
     */
    public static class MessageFile {
        private String id;
        private String name;
        private String fileType;
        private String url;
        private String storageKey;
        private Long lineCount;
        private Long byteLength;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getStorageKey() { return storageKey; }
        public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
        
        public Long getLineCount() { return lineCount; }
        public void setLineCount(Long lineCount) { this.lineCount = lineCount; }
        
        public Long getByteLength() { return byteLength; }
        public void setByteLength(Long byteLength) { this.byteLength = byteLength; }
    }
}

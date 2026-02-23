package com.chatbox.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * ChatResponse - Model for OpenAI API chat completion response
 * 
 * This class represents the response body from the OpenAI Chat Completions API.
 * It includes all fields returned by the API.
 * 
 * API Documentation: https://platform.openai.com/docs/api-reference/chat/object
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ChatResponse {
    
    /**
     * A unique identifier for the chat completion
     */
    @SerializedName("id")
    private String id;
    
    /**
     * The object type, which is always "chat.completion" or "chat.completion.chunk"
     */
    @SerializedName("object")
    private String object;
    
    /**
     * The Unix timestamp (in seconds) of when the chat completion was created
     */
    @SerializedName("created")
    private Long created;
    
    /**
     * The model used for the chat completion
     */
    @SerializedName("model")
    private String model;
    
    /**
     * A list of chat completion choices
     * Can be more than one if n is greater than 1
     */
    @SerializedName("choices")
    private List<Choice> choices;
    
    /**
     * Usage statistics for the completion request
     */
    @SerializedName("usage")
    private Usage usage;
    
    /**
     * The system fingerprint for the completion
     * This fingerprint can be used to track changes in the model behavior
     */
    @SerializedName("system_fingerprint")
    private String systemFingerprint;
    
    /**
     * This field is not from API, used internally to store complete content
     */
    private String content;
    
    /**
     * This field is not from API, used internally to store finish reason
     */
    private String finishReason;
    
    // =========================================================================
    // Constructors
    // =========================================================================
    
    /**
     * Default constructor
     */
    public ChatResponse() {}
    
    /**
     * Constructor with content
     * 
     * @param content The response content
     */
    public ChatResponse(String content) {
        this.content = content;
    }
    
    // =========================================================================
    // Getters and Setters
    // =========================================================================
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public Long getCreated() {
        return created;
    }
    
    public void setCreated(Long created) {
        this.created = created;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Choice> getChoices() {
        return choices;
    }
    
    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }
    
    public Usage getUsage() {
        return usage;
    }
    
    public void setUsage(Usage usage) {
        this.usage = usage;
    }
    
    public String getSystemFingerprint() {
        return systemFingerprint;
    }
    
    public void setSystemFingerprint(String systemFingerprint) {
        this.systemFingerprint = systemFingerprint;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getFinishReason() {
        return finishReason;
    }
    
    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
    
    // =========================================================================
    // Helper Methods
    // =========================================================================
    
    /**
     * Get the first choice's message content
     * 
     * @return The content, or empty string if no choices
     */
    public String getFirstChoiceContent() {
        if (choices != null && !choices.isEmpty()) {
            Choice choice = choices.get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
            if (choice.getDelta() != null) {
                return choice.getDelta().getContent();
            }
        }
        return "";
    }
    
    /**
     * Get the first choice's finish reason
     * 
     * @return The finish reason, or null if no choices
     */
    public String getFirstChoiceFinishReason() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getFinishReason();
        }
        return null;
    }
    
    /**
     * Check if the response has usage information
     * 
     * @return true if usage is available
     */
    public boolean hasUsage() {
        return usage != null;
    }
    
    /**
     * Get total tokens used
     * 
     * @return Total tokens, or 0 if usage is not available
     */
    public int getTotalTokens() {
        if (usage != null) {
            return usage.getTotalTokens();
        }
        return 0;
    }
    
    /**
     * Check if the response is complete (has finish reason)
     * 
     * @return true if complete
     */
    public boolean isComplete() {
        return getFirstChoiceFinishReason() != null;
    }
    
    // =========================================================================
    // Inner Classes
    // =========================================================================
    
    /**
     * Choice - A chat completion choice
     */
    public static class Choice {
        /**
         * The index of the choice in the list of choices
         */
        @SerializedName("index")
        private Integer index;
        
        /**
         * A chat completion message generated by the model
         * Present in non-streaming responses
         */
        @SerializedName("message")
        private Message message;
        
        /**
         * A chat completion delta generated by streamed model responses
         * Present in streaming responses
         */
        @SerializedName("delta")
        private Delta delta;
        
        /**
         * The reason the model stopped generating tokens
         * "stop", "length", "tool_calls", "content_filter", or null
         */
        @SerializedName("finish_reason")
        private String finishReason;
        
        /**
         * Log probability information for the choice
         */
        @SerializedName("logprobs")
        private Object logprobs;
        
        // Getters and setters
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
        
        public Delta getDelta() { return delta; }
        public void setDelta(Delta delta) { this.delta = delta; }
        
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
        
        public Object getLogprobs() { return logprobs; }
        public void setLogprobs(Object logprobs) { this.logprobs = logprobs; }
    }
    
    /**
     * Message - A chat completion message
     */
    public static class Message {
        /**
         * The role of the author of this message
         */
        @SerializedName("role")
        private String role;
        
        /**
         * The contents of the message
         */
        @SerializedName("content")
        private String content;
        
        /**
         * The name of the author of this message
         */
        @SerializedName("name")
        private String name;
        
        /**
         * The tool calls generated by the model
         */
        @SerializedName("tool_calls")
        private List<ToolCall> toolCalls;
        
        /**
         * The tool call that this message is responding to
         */
        @SerializedName("tool_call_id")
        private String toolCallId;
        
        // Getters and setters
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
        
        public String getToolCallId() { return toolCallId; }
        public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
    }
    
    /**
     * Delta - A chat completion delta (for streaming)
     */
    public static class Delta {
        /**
         * The role of the author of this message
         */
        @SerializedName("role")
        private String role;
        
        /**
         * The contents of the chunk message
         */
        @SerializedName("content")
        private String content;
        
        /**
         * The tool calls generated by the model
         */
        @SerializedName("tool_calls")
        private List<ToolCall> toolCalls;
        
        // Getters and setters
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    }
    
    /**
     * ToolCall - A tool call generated by the model
     */
    public static class ToolCall {
        /**
         * The ID of the tool call
         */
        @SerializedName("id")
        private String id;
        
        /**
         * The type of the tool
         */
        @SerializedName("type")
        private String type;
        
        /**
         * The function that the model called
         */
        @SerializedName("function")
        private Function function;
        
        /**
         * The index of the tool call in the list
         */
        @SerializedName("index")
        private Integer index;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Function getFunction() { return function; }
        public void setFunction(Function function) { this.function = function; }
        
        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
    }
    
    /**
     * Function - A function called by the model
     */
    public static class Function {
        /**
         * The name of the function to call
         */
        @SerializedName("name")
        private String name;
        
        /**
         * The arguments to call the function with, as a JSON string
         */
        @SerializedName("arguments")
        private String arguments;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }
    
    /**
     * Usage - Usage statistics for the completion request
     */
    public static class Usage {
        /**
         * Number of tokens in the prompt
         */
        @SerializedName("prompt_tokens")
        private Integer promptTokens;
        
        /**
         * Number of tokens in the generated completion
         */
        @SerializedName("completion_tokens")
        private Integer completionTokens;
        
        /**
         * Total number of tokens used (prompt + completion)
         */
        @SerializedName("total_tokens")
        private Integer totalTokens;
        
        /**
         * Breakdown of tokens used in the prompt
         */
        @SerializedName("prompt_tokens_details")
        private TokenDetails promptTokensDetails;
        
        /**
         * Breakdown of tokens used in the completion
         */
        @SerializedName("completion_tokens_details")
        private TokenDetails completionTokensDetails;
        
        // Getters and setters
        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        
        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
        
        public TokenDetails getPromptTokensDetails() { return promptTokensDetails; }
        public void setPromptTokensDetails(TokenDetails promptTokensDetails) { this.promptTokensDetails = promptTokensDetails; }
        
        public TokenDetails getCompletionTokensDetails() { return completionTokensDetails; }
        public void setCompletionTokensDetails(TokenDetails completionTokensDetails) { this.completionTokensDetails = completionTokensDetails; }
    }
    
    /**
     * TokenDetails - Breakdown of token usage
     */
    public static class TokenDetails {
        /**
         * Tokens from the text
         */
        @SerializedName("text_tokens")
        private Integer textTokens;
        
        /**
         * Tokens from the audio (if applicable)
         */
        @SerializedName("audio_tokens")
        private Integer audioTokens;
        
        /**
         * Tokens from the image (if applicable)
         */
        @SerializedName("image_tokens")
        private Integer imageTokens;
        
        /**
         * Cached tokens
         */
        @SerializedName("cached_tokens")
        private Integer cachedTokens;
        
        // Getters and setters
        public Integer getTextTokens() { return textTokens; }
        public void setTextTokens(Integer textTokens) { this.textTokens = textTokens; }
        
        public Integer getAudioTokens() { return audioTokens; }
        public void setAudioTokens(Integer audioTokens) { this.audioTokens = audioTokens; }
        
        public Integer getImageTokens() { return imageTokens; }
        public void setImageTokens(Integer imageTokens) { this.imageTokens = imageTokens; }
        
        public Integer getCachedTokens() { return cachedTokens; }
        public void setCachedTokens(Integer cachedTokens) { this.cachedTokens = cachedTokens; }
    }
}

package com.chatbox.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatRequest - Model for OpenAI API chat completion request
 * 
 * This class represents the request body for the OpenAI Chat Completions API.
 * It includes all parameters that can be sent to the API.
 * 
 * API Documentation: https://platform.openai.com/docs/api-reference/chat/create
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ChatRequest {
    
    /**
     * ID of the model to use
     * Example: "gpt-4", "gpt-4o-mini", "gpt-3.5-turbo"
     */
    @SerializedName("model")
    private String model;
    
    /**
     * List of messages comprising the conversation
     */
    @SerializedName("messages")
    private List<Message> messages;
    
    /**
     * Whether to stream back partial progress
     * If true, tokens are sent as data-only server-sent events
     */
    @SerializedName("stream")
    private Boolean stream;
    
    /**
     * What sampling temperature to use, between 0 and 2
     * Higher values make output more random, lower more deterministic
     * Default: 1.0
     */
    @SerializedName("temperature")
    private Float temperature;
    
    /**
     * Alternative to temperature, called nucleus sampling
     * The model considers results of tokens with top_p probability mass
     * Default: 1.0
     */
    @SerializedName("top_p")
    private Float topP;
    
    /**
     * How many chat completion choices to generate
     * Default: 1
     */
    @SerializedName("n")
    private Integer n;
    
    /**
     * Whether to stream back partial progress
     * Deprecated: use stream instead
     */
    @SerializedName("stream_options")
    private StreamOptions streamOptions;
    
    /**
     * Up to 4 sequences where the API will stop generating further tokens
     */
    @SerializedName("stop")
    private Object stop;
    
    /**
     * The maximum number of tokens to generate
     * Default: infinity (model's maximum)
     */
    @SerializedName("max_tokens")
    private Integer maxTokens;
    
    /**
     * Deprecated: use max_tokens instead
     */
    @SerializedName("max_completion_tokens")
    private Integer maxCompletionTokens;
    
    /**
     * Penalty for new tokens based on their presence in the text so far
     * Default: 0
     */
    @SerializedName("presence_penalty")
    private Float presencePenalty;
    
    /**
     * Penalty for new tokens based on their frequency in the text so far
     * Default: 0
     */
    @SerializedName("frequency_penalty")
    private Float frequencyPenalty;
    
    /**
     * Modify likelihood of specified tokens appearing in completion
     * Maps token IDs to bias values from -100 to 100
     */
    @SerializedName("logit_bias")
    private Object logitBias;
    
    /**
     * A unique identifier representing your end-user
     * Helps OpenAI monitor and detect abuse
     */
    @SerializedName("user")
    private String user;
    
    /**
     * A list of tools the model may call
     * Currently only functions are supported
     */
    @SerializedName("tools")
    private List<Tool> tools;
    
    /**
     * Controls which (if any) function is called by the model
     * "none" means model will not call a function
     * "auto" means model can pick between generating a message or calling a function
     */
    @SerializedName("tool_choice")
    private Object toolChoice;
    
    /**
     * An object specifying the format that the model must output
     * Setting to {"type": "json_object"} enables JSON mode
     */
    @SerializedName("response_format")
    private ResponseFormat responseFormat;
    
    /**
     * This feature is in Beta
     * If specified, system will make a best effort to sample deterministically
     */
    @SerializedName("seed")
    private Integer seed;
    
    // =========================================================================
    // Constructors
    // =========================================================================
    
    /**
     * Default constructor
     */
    public ChatRequest() {
        this.messages = new ArrayList<>();
        this.stream = true;
        this.n = 1;
    }
    
    /**
     * Constructor with model and messages
     * 
     * @param model Model ID
     * @param messages List of messages
     */
    public ChatRequest(String model, List<Message> messages) {
        this();
        this.model = model;
        this.messages = messages;
    }
    
    // =========================================================================
    // Getters and Setters
    // =========================================================================
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
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
    
    public Integer getN() {
        return n;
    }
    
    public void setN(Integer n) {
        this.n = n;
    }
    
    public StreamOptions getStreamOptions() {
        return streamOptions;
    }
    
    public void setStreamOptions(StreamOptions streamOptions) {
        this.streamOptions = streamOptions;
    }
    
    public Object getStop() {
        return stop;
    }
    
    public void setStop(Object stop) {
        this.stop = stop;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }
    
    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }
    
    public Float getPresencePenalty() {
        return presencePenalty;
    }
    
    public void setPresencePenalty(Float presencePenalty) {
        this.presencePenalty = presencePenalty;
    }
    
    public Float getFrequencyPenalty() {
        return frequencyPenalty;
    }
    
    public void setFrequencyPenalty(Float frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }
    
    public Object getLogitBias() {
        return logitBias;
    }
    
    public void setLogitBias(Object logitBias) {
        this.logitBias = logitBias;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public List<Tool> getTools() {
        return tools;
    }
    
    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }
    
    public Object getToolChoice() {
        return toolChoice;
    }
    
    public void setToolChoice(Object toolChoice) {
        this.toolChoice = toolChoice;
    }
    
    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }
    
    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }
    
    public Integer getSeed() {
        return seed;
    }
    
    public void setSeed(Integer seed) {
        this.seed = seed;
    }
    
    // =========================================================================
    // Helper Methods
    // =========================================================================
    
    /**
     * Add a message to the request
     * 
     * @param role Message role ("system", "user", "assistant")
     * @param content Message content
     */
    public void addMessage(String role, String content) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(new Message(role, content));
    }
    
    /**
     * Add a system message
     * 
     * @param content System message content
     */
    public void addSystemMessage(String content) {
        addMessage("system", content);
    }
    
    /**
     * Add a user message
     * 
     * @param content User message content
     */
    public void addUserMessage(String content) {
        addMessage("user", content);
    }
    
    /**
     * Add an assistant message
     * 
     * @param content Assistant message content
     */
    public void addAssistantMessage(String content) {
        addMessage("assistant", content);
    }
    
    // =========================================================================
    // Inner Classes
    // =========================================================================
    
    /**
     * Message - Represents a message in the conversation
     */
    public static class Message {
        @SerializedName("role")
        private String role;
        
        @SerializedName("content")
        private String content;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("tool_calls")
        private List<ToolCall> toolCalls;
        
        @SerializedName("tool_call_id")
        private String toolCallId;
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public Message(String role, String content, String name) {
            this.role = role;
            this.content = content;
            this.name = name;
        }
        
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
     * StreamOptions - Options for streaming responses
     */
    public static class StreamOptions {
        @SerializedName("include_usage")
        private Boolean includeUsage;
        
        public Boolean getIncludeUsage() { return includeUsage; }
        public void setIncludeUsage(Boolean includeUsage) { this.includeUsage = includeUsage; }
    }
    
    /**
     * Tool - Tool definition for function calling
     */
    public static class Tool {
        @SerializedName("type")
        private String type;
        
        @SerializedName("function")
        private Function function;
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Function getFunction() { return function; }
        public void setFunction(Function function) { this.function = function; }
    }
    
    /**
     * Function - Function definition
     */
    public static class Function {
        @SerializedName("name")
        private String name;
        
        @SerializedName("description")
        private String description;
        
        @SerializedName("parameters")
        private Object parameters;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Object getParameters() { return parameters; }
        public void setParameters(Object parameters) { this.parameters = parameters; }
    }
    
    /**
     * ToolCall - Tool call from the model
     */
    public static class ToolCall {
        @SerializedName("id")
        private String id;
        
        @SerializedName("type")
        private String type;
        
        @SerializedName("function")
        private FunctionCall function;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public FunctionCall getFunction() { return function; }
        public void setFunction(FunctionCall function) { this.function = function; }
    }
    
    /**
     * FunctionCall - Function call details
     */
    public static class FunctionCall {
        @SerializedName("name")
        private String name;
        
        @SerializedName("arguments")
        private String arguments;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }
    
    /**
     * ResponseFormat - Format for model output
     */
    public static class ResponseFormat {
        @SerializedName("type")
        private String type;
        
        public ResponseFormat() {}
        
        public ResponseFormat(String type) {
            this.type = type;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}

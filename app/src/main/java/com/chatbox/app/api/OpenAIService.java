package com.chatbox.app.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.model.ChatRequest;
import com.chatbox.app.model.ChatResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * OpenAIService - Service for OpenAI API communication
 * 
 * This class handles all communication with the OpenAI API.
 * It supports both streaming and non-streaming chat completions.
 * 
 * Features:
 * - Chat completions (streaming and non-streaming)
 * - API key management
 * - Error handling
 * - Timeout configuration
 * 
 * Supported Providers:
 * - OpenAI (GPT models)
 * - Azure OpenAI
 * - Compatible OpenAI API providers
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class OpenAIService {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "OpenAIService";
    
    /**
     * JSON media type for requests
     */
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    /**
     * SSE content type
     */
    private static final String SSE_CONTENT_TYPE = "text/event-stream";
    
    /**
     * Gson instance for JSON parsing
     */
    private final Gson gson = new Gson();
    
    /**
     * OkHttp client for HTTP requests
     */
    private final OkHttpClient client;
    
    /**
     * Provider settings
     */
    private ProviderSettings settings;
    
    /**
     * Current API call (for cancellation)
     */
    private Call currentCall;
    
    // =========================================================================
    // Constructor
    // =========================================================================
    
    /**
     * Constructor with provider settings
     * 
     * @param settings Provider settings
     */
    public OpenAIService(ProviderSettings settings) {
        this.settings = settings;
        this.client = createClient();
    }
    
    /**
     * Constructor with custom timeout
     * 
     * @param settings Provider settings
     * @param timeoutSeconds Timeout in seconds
     */
    public OpenAIService(ProviderSettings settings, int timeoutSeconds) {
        this.settings = settings;
        this.client = createClient(timeoutSeconds);
    }
    
    // =========================================================================
    // Client Configuration
    // =========================================================================
    
    /**
     * Create OkHttp client with default timeout
     * 
     * @return Configured OkHttpClient
     */
    private OkHttpClient createClient() {
        return createClient(60);
    }
    
    /**
     * Create OkHttp client with custom timeout
     * 
     * @param timeoutSeconds Timeout in seconds
     * @return Configured OkHttpClient
     */
    private OkHttpClient createClient(int timeoutSeconds) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        return new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build();
    }
    
    // =========================================================================
    // Provider Settings
    // =========================================================================
    
    /**
     * Update provider settings
     * 
     * @param settings New provider settings
     */
    public void setSettings(ProviderSettings settings) {
        this.settings = settings;
    }
    
    /**
     * Get current provider settings
     * 
     * @return Current settings
     */
    public ProviderSettings getSettings() {
        return settings;
    }
    
    // =========================================================================
    // Chat Completion
    // =========================================================================
    
    /**
     * Send a chat completion request (streaming)
     * 
     * @param request Chat request
     * @param callback Callback for response handling
     */
    public void chatCompletion(ChatRequest request, ChatCallback callback) {
        if (settings == null || !settings.isConfigured()) {
            callback.onError("Provider not configured");
            return;
        }
        
        String url = buildApiUrl();
        String apiKey = settings.getApiKey();
        
        // Ensure streaming is enabled
        request.setStream(true);
        
        String jsonBody = gson.toJson(request);
        Log.d(TAG, "Request body: " + jsonBody);
        
        RequestBody body = RequestBody.create(jsonBody, JSON);
        
        Request httpRequest = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(body)
            .build();
        
        callback.onStart();
        
        currentCall = client.newCall(httpRequest);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Request failed", e);
                if (!call.isCanceled()) {
                    callback.onError("Network error: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API error: " + response.code() + " - " + errorBody);
                    callback.onError("API error: " + response.code());
                    return;
                }
                
                if (response.body() == null) {
                    callback.onError("Empty response");
                    return;
                }
                
                // Handle streaming response
                handleStreamingResponse(response, callback);
            }
        });
    }
    
    /**
     * Handle streaming SSE response
     * 
     * @param response HTTP response
     * @param callback Chat callback
     */
    private void handleStreamingResponse(Response response, ChatCallback callback) {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(response.body().byteStream())
        );
        
        StringBuilder fullContent = new StringBuilder();
        ChatResponse finalResponse = new ChatResponse();
        
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "SSE line: " + line);
                
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    
                    // Check for stream end
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    
                    try {
                        // Parse the chunk
                        ChatResponse chunk = gson.fromJson(data, ChatResponse.class);
                        
                        if (chunk != null && chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            ChatResponse.Choice choice = chunk.getChoices().get(0);
                            ChatResponse.Delta delta = choice.getDelta();
                            
                            if (delta != null && delta.getContent() != null) {
                                String content = delta.getContent();
                                fullContent.append(content);
                                callback.onChunk(content);
                            }
                            
                            // Store finish reason
                            if (choice.getFinishReason() != null) {
                                finalResponse.setFinishReason(choice.getFinishReason());
                            }
                        }
                        
                        // Store usage if available
                        if (chunk.getUsage() != null) {
                            finalResponse.setUsage(chunk.getUsage());
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing chunk", e);
                    }
                }
            }
            
            // Build final response
            finalResponse.setContent(fullContent.toString());
            callback.onComplete(finalResponse);
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading stream", e);
            callback.onError("Error reading response: " + e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing reader", e);
            }
        }
    }
    
    /**
     * Send a chat completion request (non-streaming)
     * 
     * @param request Chat request
     * @param callback Callback for response handling
     */
    public void chatCompletionSync(ChatRequest request, ChatCallback callback) {
        if (settings == null || !settings.isConfigured()) {
            callback.onError("Provider not configured");
            return;
        }
        
        String url = buildApiUrl();
        String apiKey = settings.getApiKey();
        
        // Disable streaming
        request.setStream(false);
        
        String jsonBody = gson.toJson(request);
        RequestBody body = RequestBody.create(jsonBody, JSON);
        
        Request httpRequest = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(body)
            .build();
        
        callback.onStart();
        
        currentCall = client.newCall(httpRequest);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!call.isCanceled()) {
                    callback.onError("Network error: " + e.getMessage());
                }
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onError("API error: " + response.code());
                    return;
                }
                
                String responseBody = response.body().string();
                ChatResponse chatResponse = gson.fromJson(responseBody, ChatResponse.class);
                
                if (chatResponse != null && chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                    ChatResponse.Choice choice = chatResponse.getChoices().get(0);
                    if (choice.getMessage() != null) {
                        callback.onChunk(choice.getMessage().getContent());
                    }
                    callback.onComplete(chatResponse);
                } else {
                    callback.onError("Invalid response");
                }
            }
        });
    }
    
    // =========================================================================
    // Utility Methods
    // =========================================================================
    
    /**
     * Build the API URL
     * 
     * @return Full API URL
     */
    private String buildApiUrl() {
        String baseUrl = settings.getApiHost();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = ProviderSettings.getDefaultHost(settings.getProvider());
        }
        
        // Remove trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        // Azure uses different endpoint format
        if (settings.isAzure()) {
            String endpoint = settings.getAzureEndpoint();
            String deployment = settings.getAzureDeploymentName();
            String apiVersion = settings.getAzureApiVersion();
            
            if (endpoint != null && deployment != null) {
                return endpoint + "/openai/deployments/" + deployment + "/chat/completions?api-version=" + apiVersion;
            }
        }
        
        return baseUrl + "/v1/chat/completions";
    }
    
    /**
     * Cancel the current API call
     */
    public void cancel() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            Log.d(TAG, "API call cancelled");
        }
    }
    
    /**
     * Check if a call is in progress
     * 
     * @return true if a call is in progress
     */
    public boolean isInProgress() {
        return currentCall != null && !currentCall.isCanceled() && !currentCall.isExecuted();
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
         * Called when a content chunk is received (streaming)
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
}

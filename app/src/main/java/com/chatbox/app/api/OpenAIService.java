package com.chatbox.app.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.model.ChatRequest;
import com.chatbox.app.model.ChatResponse;
import com.chatbox.app.utils.ApiUrlUtils;
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
 * 支持 apiHost + apiPath 的配置方式
 */
public class OpenAIService {
    
    private static final String TAG = "OpenAIService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final Gson gson = new Gson();
    private final OkHttpClient client;
    private ProviderSettings settings;
    private Call currentCall;
    
    public OpenAIService(ProviderSettings settings) {
        this.settings = settings;
        this.client = createClient(60);
    }
    
    public OpenAIService(ProviderSettings settings, int timeoutSeconds) {
        this.settings = settings;
        this.client = createClient(timeoutSeconds);
    }
    
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
    
    public void setSettings(ProviderSettings settings) {
        this.settings = settings;
    }
    
    public ProviderSettings getSettings() {
        return settings;
    }
    
    /**
     * Send a chat completion request (streaming)
     */
    public void chatCompletion(ChatRequest request, ChatCallback callback) {
        if (settings == null || !settings.isConfigured()) {
            callback.onError("Provider not configured");
            return;
        }
        
        // 构建完整 URL
        String url = buildApiUrl();
        String apiKey = settings.getApiKey();
        
        // Ensure streaming is enabled
        request.setStream(true);
        
        String jsonBody = gson.toJson(request);
        Log.d(TAG, "Request URL: " + url);
        Log.d(TAG, "Request body: " + jsonBody);
        
        RequestBody body = RequestBody.create(jsonBody, JSON);
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(body);
        
        // OpenRouter 需要额外的 headers
        if (url.contains("openrouter.ai")) {
            requestBuilder.header("HTTP-Referer", "https://chatboxai.app");
            requestBuilder.header("X-Title", "Chatbox AI");
        }
        
        Request httpRequest = requestBuilder.build();
        
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
                    callback.onError("API error: " + response.code() + " - " + errorBody);
                    return;
                }
                
                if (response.body() == null) {
                    callback.onError("Empty response");
                    return;
                }
                
                handleStreamingResponse(response, callback);
            }
        });
    }
    
    /**
     * Handle streaming SSE response
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
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    
                    try {
                        ChatResponse chunk = gson.fromJson(data, ChatResponse.class);
                        
                        if (chunk != null && chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            ChatResponse.Choice choice = chunk.getChoices().get(0);
                            ChatResponse.Delta delta = choice.getDelta();
                            
                            if (delta != null && delta.getContent() != null) {
                                String content = delta.getContent();
                                fullContent.append(content);
                                callback.onChunk(content);
                            }
                            
                            if (choice.getFinishReason() != null) {
                                finalResponse.setFinishReason(choice.getFinishReason());
                            }
                        }
                        
                        if (chunk.getUsage() != null) {
                            finalResponse.setUsage(chunk.getUsage());
                        }
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing chunk: " + data, e);
                    }
                }
            }
            
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
     * Build the API URL using apiHost + apiPath
     */
    private String buildApiUrl() {
        String apiHost = settings.getApiHost();
        String apiPath = settings.getApiPath();
        
        // 如果 apiHost 为空，使用默认值
        if (apiHost == null || apiHost.isEmpty()) {
            apiHost = ProviderSettings.getDefaultHost(settings.getProvider());
        }
        
        // Azure 使用不同的端点格式
        if (settings.isAzure()) {
            String endpoint = settings.getAzureEndpoint();
            String deployment = settings.getAzureDeploymentName();
            String apiVersion = settings.getAzureApiVersion();
            
            if (endpoint != null && deployment != null) {
                return endpoint + "/openai/deployments/" + deployment + "/chat/completions?api-version=" + apiVersion;
            }
        }
        
        // 使用 ApiUrlUtils 规范化 URL
        ApiUrlUtils.ApiUrl normalized = ApiUrlUtils.normalizeApiHostAndPath(apiHost, apiPath);
        Log.d(TAG, "URL: apiHost=" + apiHost + ", apiPath=" + apiPath + " -> " + normalized.getFullUrl());
        
        return normalized.getFullUrl();
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
    
    public boolean isInProgress() {
        return currentCall != null && !currentCall.isCanceled() && !currentCall.isExecuted();
    }
    
    /**
     * Callback interface for chat operations
     */
    public interface ChatCallback {
        void onStart();
        void onChunk(String chunk);
        void onComplete(ChatResponse response);
        void onError(String error);
    }
}

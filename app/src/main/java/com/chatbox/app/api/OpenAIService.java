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
    
    public void setSettings(ProviderSettings settings) { this.settings = settings; }
    public ProviderSettings getSettings() { return settings; }
    
    public void chatCompletion(ChatRequest request, ChatCallback callback) {
        doRequest(request, true, callback);
    }
    
    public void chatCompletionSync(ChatRequest request, ChatCallback callback) {
        doRequest(request, false, callback);
    }
    
    private void doRequest(ChatRequest request, boolean stream, ChatCallback callback) {
        if (settings == null || !settings.isConfigured()) {
            callback.onError("Provider not configured");
            return;
        }
        
        String url = buildApiUrl();
        String apiKey = settings.getApiKey();
        request.setStream(stream);
        
        String jsonBody = gson.toJson(request);
        RequestBody body = RequestBody.create(jsonBody, JSON);
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(body);
        
        if (url.contains("openrouter.ai")) {
            requestBuilder.header("HTTP-Referer", "https://chatboxai.app");
            requestBuilder.header("X-Title", "Chatbox AI");
        }
        
        callback.onStart();
        currentCall = client.newCall(requestBuilder.build());
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!call.isCanceled()) callback.onError("Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    callback.onError("API error: " + response.code() + " - " + errorBody);
                    return;
                }
                if (response.body() == null) { callback.onError("Empty response"); return; }
                if (stream) handleStreamingResponse(response, callback);
                else handleSyncResponse(response, callback);
            }
        });
    }
    
    private void handleSyncResponse(Response response, ChatCallback callback) throws IOException {
        String responseBody = response.body().string();
        ChatResponse chatResponse = gson.fromJson(responseBody, ChatResponse.class);
        if (chatResponse != null && chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
            ChatResponse.Choice choice = chatResponse.getChoices().get(0);
            if (choice.getMessage() != null) callback.onChunk(choice.getMessage().getContent());
            callback.onComplete(chatResponse);
        } else {
            callback.onError("Invalid response");
        }
    }
    
    private void handleStreamingResponse(Response response, ChatCallback callback) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
        StringBuilder fullContent = new StringBuilder();
        ChatResponse finalResponse = new ChatResponse();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) break;
                    try {
                        ChatResponse chunk = gson.fromJson(data, ChatResponse.class);
                        if (chunk != null && chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                            ChatResponse.Choice choice = chunk.getChoices().get(0);
                            ChatResponse.Delta delta = choice.getDelta();
                            if (delta != null && delta.getContent() != null) {
                                fullContent.append(delta.getContent());
                                callback.onChunk(delta.getContent());
                            }
                            if (choice.getFinishReason() != null) finalResponse.setFinishReason(choice.getFinishReason());
                        }
                        if (chunk.getUsage() != null) finalResponse.setUsage(chunk.getUsage());
                    } catch (Exception e) { Log.e(TAG, "Parse error", e); }
                }
            }
            finalResponse.setContent(fullContent.toString());
            callback.onComplete(finalResponse);
        } catch (IOException e) {
            callback.onError("Error reading response: " + e.getMessage());
        } finally { try { reader.close(); } catch (IOException e) { } }
    }
    
    private String buildApiUrl() {
        String apiHost = settings.getApiHost();
        String apiPath = settings.getApiPath();
        if (apiHost == null || apiHost.isEmpty()) apiHost = ProviderSettings.getDefaultHost(settings.getProvider());
        if (settings.isAzure()) {
            String endpoint = settings.getAzureEndpoint();
            String deployment = settings.getAzureDeploymentName();
            String apiVersion = settings.getAzureApiVersion();
            if (endpoint != null && deployment != null)
                return endpoint + "/openai/deployments/" + deployment + "/chat/completions?api-version=" + apiVersion;
        }
        return ApiUrlUtils.normalizeApiHostAndPath(apiHost, apiPath).getFullUrl();
    }
    
    public void cancel() { if (currentCall != null && !currentCall.isCanceled()) currentCall.cancel(); }
    public boolean isInProgress() { return currentCall != null && !currentCall.isCanceled() && !currentCall.isExecuted(); }
    
    public interface ChatCallback {
        void onStart();
        void onChunk(String chunk);
        void onComplete(ChatResponse response);
        void onError(String error);
    }
}

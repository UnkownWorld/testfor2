package com.chatbox.app.api;

import android.util.Log;

import com.chatbox.app.data.entity.ProviderSettings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ModelsService - Service for fetching available models from API
 */
public class ModelsService {
    
    private static final String TAG = "ModelsService";
    private final OkHttpClient client;
    private final Gson gson;
    
    public ModelsService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Fetch models from provider API
     */
    public void fetchModels(ProviderSettings settings, ModelsCallback callback) {
        new Thread(() -> {
            try {
                List<String> models = fetchModelsInternal(settings);
                callback.onSuccess(models);
            } catch (Exception e) {
                Log.e(TAG, "Error fetching models", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    private List<String> fetchModelsInternal(ProviderSettings settings) throws IOException {
        String provider = settings.getProvider();
        String apiHost = settings.getApiHost();
        String apiKey = settings.getApiKey();
        
        if (apiHost == null || apiHost.isEmpty()) {
            apiHost = ProviderSettings.getDefaultHost(provider);
        }
        
        List<String> models = new ArrayList<>();
        
        switch (provider) {
            case ProviderSettings.PROVIDER_OPENAI:
            case ProviderSettings.PROVIDER_GROQ:
            case ProviderSettings.PROVIDER_MISTRAL:
            case ProviderSettings.PROVIDER_PERPLEXITY:
            case ProviderSettings.PROVIDER_XAI:
            case ProviderSettings.PROVIDER_DEEPSEEK:
            case ProviderSettings.PROVIDER_SILICONFLOW:
                models = fetchOpenAICompatibleModels(apiHost, apiKey);
                break;
                
            case ProviderSettings.PROVIDER_CLAUDE:
                models = getDefaultClaudeModels();
                break;
                
            case ProviderSettings.PROVIDER_GEMINI:
                models = fetchGeminiModels(apiKey);
                break;
                
            case ProviderSettings.PROVIDER_OLLAMA:
                models = fetchOllamaModels(apiHost);
                break;
                
            case ProviderSettings.PROVIDER_OPENROUTER:
                models = fetchOpenRouterModels(apiKey);
                break;
                
            default:
                // Try OpenAI-compatible API for custom providers
                if (apiHost != null && !apiHost.isEmpty()) {
                    try {
                        models = fetchOpenAICompatibleModels(apiHost, apiKey);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to fetch models for custom provider", e);
                    }
                }
                break;
        }
        
        return models;
    }
    
    private String normalizeApiHost(String apiHost) {
        if (apiHost == null) return "";
        
        // 移除末尾斜杠
        String url = apiHost.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        return url;
    }
    
    private List<String> fetchOpenAICompatibleModels(String apiHost, String apiKey) throws IOException {
        String url = normalizeApiHost(apiHost);
        
        // 构建模型列表URL - 直接使用 /models 或 /v1/models
        if (!url.endsWith("/models")) {
            if (url.contains("/v1")) {
                // URL已经包含/v1，直接添加/models
                url = url + "/models";
            } else {
                // URL不包含/v1，添加/v1/models
                url = url + "/v1/models";
            }
        }
        
        Log.d(TAG, "Fetching models from: " + url);
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .get();
        
        // 添加认证头
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
        }
        
        Request request = requestBuilder.build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e(TAG, "HTTP " + response.code() + ": " + errorBody);
                throw new IOException("HTTP " + response.code() + ": " + errorBody);
            }
            
            String body = response.body().string();
            Log.d(TAG, "Response: " + body);
            
            JsonObject json = gson.fromJson(body, JsonObject.class);
            
            // 检查是否有data字段（OpenAI格式）
            if (json.has("data")) {
                JsonArray data = json.getAsJsonArray("data");
                List<String> models = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    JsonObject model = data.get(i).getAsJsonObject();
                    if (model.has("id")) {
                        String id = model.get("id").getAsString();
                        models.add(id);
                    }
                }
                return models;
            }
            
            // 检查是否有models字段（某些API格式）
            if (json.has("models")) {
                JsonArray modelsArray = json.getAsJsonArray("models");
                List<String> models = new ArrayList<>();
                for (int i = 0; i < modelsArray.size(); i++) {
                    JsonObject model = modelsArray.get(i).getAsJsonObject();
                    if (model.has("id")) {
                        models.add(model.get("id").getAsString());
                    } else if (model.has("name")) {
                        models.add(model.get("name").getAsString());
                    }
                }
                return models;
            }
            
            // 如果都没有，返回空列表
            Log.w(TAG, "No models found in response");
            return new ArrayList<>();
        }
    }
    
    private List<String> fetchGeminiModels(String apiKey) throws IOException {
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
        
        Log.d(TAG, "Fetching Gemini models from: " + url);
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            
            String body = response.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray models = json.getAsJsonArray("models");
            
            List<String> modelIds = new ArrayList<>();
            for (int i = 0; i < models.size(); i++) {
                JsonObject model = models.get(i).getAsJsonObject();
                String name = model.get("name").getAsString();
                if (name.startsWith("models/")) {
                    name = name.substring(7);
                }
                // Only include generative models
                if (model.has("supportedGenerationMethods")) {
                    JsonArray methods = model.getAsJsonArray("supportedGenerationMethods");
                    for (int j = 0; j < methods.size(); j++) {
                        if ("generateContent".equals(methods.get(j).getAsString())) {
                            modelIds.add(name);
                            break;
                        }
                    }
                } else {
                    modelIds.add(name);
                }
            }
            
            return modelIds;
        }
    }
    
    private List<String> fetchOllamaModels(String apiHost) throws IOException {
        if (apiHost == null || apiHost.isEmpty()) {
            apiHost = "http://localhost:11434";
        }
        String url = normalizeApiHost(apiHost) + "/api/tags";
        
        Log.d(TAG, "Fetching Ollama models from: " + url);
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            
            String body = response.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray models = json.getAsJsonArray("models");
            
            List<String> modelIds = new ArrayList<>();
            for (int i = 0; i < models.size(); i++) {
                JsonObject model = models.get(i).getAsJsonObject();
                String name = model.get("name").getAsString();
                modelIds.add(name);
            }
            
            return modelIds;
        }
    }
    
    private List<String> fetchOpenRouterModels(String apiKey) throws IOException {
        String url = "https://openrouter.ai/api/v1/models";
        
        Log.d(TAG, "Fetching OpenRouter models");
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + apiKey)
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            
            String body = response.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray data = json.getAsJsonArray("data");
            
            List<String> models = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                JsonObject model = data.get(i).getAsJsonObject();
                String id = model.get("id").getAsString();
                models.add(id);
            }
            
            return models;
        }
    }
    
    private List<String> getDefaultClaudeModels() {
        List<String> models = new ArrayList<>();
        models.add("claude-3-5-sonnet-20241022");
        models.add("claude-3-5-haiku-20241022");
        models.add("claude-3-opus-20240229");
        models.add("claude-3-sonnet-20240229");
        models.add("claude-3-haiku-20240307");
        return models;
    }
    
    public interface ModelsCallback {
        void onSuccess(List<String> models);
        void onError(String error);
    }
}

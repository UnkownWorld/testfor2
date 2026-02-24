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
 * 
 * This service fetches the list of available models from various AI providers.
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
     * 
     * @param settings Provider settings
     * @param callback Callback for results
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
                // OpenAI-compatible API
                models = fetchOpenAICompatibleModels(apiHost, apiKey);
                break;
                
            case ProviderSettings.PROVIDER_CLAUDE:
                // Anthropic doesn't have a models endpoint, return default list
                models = getDefaultClaudeModels();
                break;
                
            case ProviderSettings.PROVIDER_GEMINI:
                // Google Gemini models endpoint
                models = fetchGeminiModels(apiKey);
                break;
                
            case ProviderSettings.PROVIDER_OLLAMA:
                // Ollama local API
                models = fetchOllamaModels(apiHost);
                break;
                
            case ProviderSettings.PROVIDER_OPENROUTER:
                // OpenRouter models endpoint
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
    
    private String stripTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
    
    private List<String> fetchOpenAICompatibleModels(String apiHost, String apiKey) throws IOException {
        String url = stripTrailingSlash(apiHost);
        if (!url.endsWith("/v1")) {
            url = url + "/v1";
        }
        url = url + "/models";
        
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
    
    private List<String> fetchGeminiModels(String apiKey) throws IOException {
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
        
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
                // Extract model ID from "models/gemini-pro" format
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
        String url = stripTrailingSlash(apiHost) + "/api/tags";
        
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
    
    /**
     * Callback interface for models fetch
     */
    public interface ModelsCallback {
        void onSuccess(List<String> models);
        void onError(String error);
    }
}

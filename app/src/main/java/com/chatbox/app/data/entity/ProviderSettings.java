package com.chatbox.app.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.chatbox.app.data.database.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ProviderSettings - Entity class for AI provider configuration
 * 
 * This class stores the configuration settings for each AI provider.
 * Each provider can have its own API key, host, model list, etc.
 * 
 * Database Schema:
 * - Table name: provider_settings
 * - Primary key: provider (String, the provider ID)
 * 
 * Supported Providers:
 * - openai: OpenAI (GPT models)
 * - claude: Anthropic Claude
 * - gemini: Google Gemini
 * - azure: Azure OpenAI
 * - deepseek: DeepSeek
 * - siliconflow: SiliconFlow
 * - ollama: Ollama (local models)
 * - groq: Groq
 * - mistral: Mistral AI
 * - lmstudio: LM Studio
 * - perplexity: Perplexity
 * - xai: xAI (Grok)
 * - openrouter: OpenRouter
 * - custom: Custom provider
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
@Entity(tableName = "provider_settings")
@TypeConverters({Converters.class})
public class ProviderSettings {
    
    // =========================================================================
    // Constants - Provider IDs
    // =========================================================================
    
    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_CLAUDE = "claude";
    public static final String PROVIDER_GEMINI = "gemini";
    public static final String PROVIDER_AZURE = "azure";
    public static final String PROVIDER_DEEPSEEK = "deepseek";
    public static final String PROVIDER_SILICONFLOW = "siliconflow";
    public static final String PROVIDER_OLLAMA = "ollama";
    public static final String PROVIDER_GROQ = "groq";
    public static final String PROVIDER_MISTRAL = "mistral-ai";
    public static final String PROVIDER_LMSTUDIO = "lm-studio";
    public static final String PROVIDER_PERPLEXITY = "perplexity";
    public static final String PROVIDER_XAI = "xAI";
    public static final String PROVIDER_OPENROUTER = "openrouter";
    public static final String PROVIDER_CUSTOM = "custom";
    
    // =========================================================================
    // Default API Hosts
    // =========================================================================
    
    public static final String DEFAULT_OPENAI_HOST = "https://api.openai.com";
    public static final String DEFAULT_CLAUDE_HOST = "https://api.anthropic.com";
    public static final String DEFAULT_GEMINI_HOST = "https://generativelanguage.googleapis.com";
    public static final String DEFAULT_DEEPSEEK_HOST = "https://api.deepseek.com";
    public static final String DEFAULT_SILICONFLOW_HOST = "https://api.siliconflow.cn";
    public static final String DEFAULT_GROQ_HOST = "https://api.groq.com/openai";
    public static final String DEFAULT_MISTRAL_HOST = "https://api.mistral.ai";
    public static final String DEFAULT_PERPLEXITY_HOST = "https://api.perplexity.ai";
    public static final String DEFAULT_XAI_HOST = "https://api.x.ai";
    public static final String DEFAULT_OPENROUTER_HOST = "https://openrouter.ai/api";
    public static final String DEFAULT_OLLAMA_HOST = "http://localhost:11434";
    
    // =========================================================================
    // Fields (Database Columns)
    // =========================================================================
    
    /**
     * Provider ID (primary key)
     * e.g., "openai", "claude", "gemini"
     */
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "provider")
    private String provider;
    
    /**
     * Display name for the provider
     * e.g., "OpenAI", "Anthropic Claude"
     */
    @ColumnInfo(name = "display_name")
    private String displayName;
    
    /**
     * API key for authentication
     * Stored securely (consider using EncryptedSharedPreferences in production)
     */
    @ColumnInfo(name = "api_key")
    private String apiKey;
    
    /**
     * API host/base URL
     * e.g., "https://api.openai.com"
     */
    @ColumnInfo(name = "api_host")
    private String apiHost;
    
    /**
     * API path (optional)
     * Additional path appended to the host
     */
    @ColumnInfo(name = "api_path")
    private String apiPath;
    
    /**
     * List of available models as JSON string
     * Stores model configurations
     */
    @ColumnInfo(name = "models_json")
    private String modelsJson;
    
    /**
     * List of excluded model IDs
     * Models to hide from the selection list
     */
    @ColumnInfo(name = "excluded_models")
    private List<String> excludedModels;
    
    /**
     * Whether to use a proxy
     * For regions with API access restrictions
     */
    @ColumnInfo(name = "use_proxy")
    private boolean useProxy;
    
    /**
     * Proxy URL (if useProxy is true)
     */
    @ColumnInfo(name = "proxy_url")
    private String proxyUrl;
    
    // =========================================================================
    // Azure-specific Fields
    // =========================================================================
    
    /**
     * Azure endpoint URL
     * Full Azure OpenAI endpoint
     */
    @ColumnInfo(name = "azure_endpoint")
    private String azureEndpoint;
    
    /**
     * Azure deployment name
     * The name of the deployed model
     */
    @ColumnInfo(name = "azure_deployment_name")
    private String azureDeploymentName;
    
    /**
     * Azure API version
     * e.g., "2024-02-01"
     */
    @ColumnInfo(name = "azure_api_version")
    private String azureApiVersion;
    
    // =========================================================================
    // Additional Settings
    // =========================================================================
    
    /**
     * Default model ID for this provider
     * The model selected by default
     */
    @ColumnInfo(name = "default_model")
    private String defaultModel;
    
    /**
     * Default temperature for this provider
     * Range: 0.0 - 2.0
     */
    @ColumnInfo(name = "default_temperature")
    private Float defaultTemperature;
    
    /**
     * Default max tokens for this provider
     */
    @ColumnInfo(name = "default_max_tokens")
    private Integer defaultMaxTokens;
    
    /**
     * Whether this provider is enabled
     * Disabled providers are hidden from selection
     */
    @ColumnInfo(name = "enabled")
    private boolean enabled;
    
    /**
     * Custom headers as JSON string
     * Additional headers to include in API requests
     */
    @ColumnInfo(name = "custom_headers_json")
    private String customHeadersJson;
    
    /**
     * Timestamp when settings were last updated
     */
    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    // =========================================================================
    // Constructors
    // =========================================================================
    
    /**
     * Default constructor required by Room
     */
    public ProviderSettings() {
        this.excludedModels = new ArrayList<>();
        this.enabled = true;
        this.useProxy = false;
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Constructor with provider ID
     * 
     * @param provider The provider ID
     */
    public ProviderSettings(String provider) {
        this();
        this.provider = provider;
        this.displayName = getDefaultDisplayName(provider);
        this.apiHost = getDefaultHost(provider);
    }
    
    // =========================================================================
    // Getters and Setters
    // =========================================================================
    
    @NonNull
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(@NonNull String provider) {
        this.provider = provider;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getApiHost() {
        return apiHost;
    }
    
    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }
    
    public String getApiPath() {
        return apiPath;
    }
    
    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }
    
    public String getModelsJson() {
        return modelsJson;
    }
    
    public void setModelsJson(String modelsJson) {
        this.modelsJson = modelsJson;
    }
    
    public List<String> getExcludedModels() {
        return excludedModels;
    }
    
    public void setExcludedModels(List<String> excludedModels) {
        this.excludedModels = excludedModels;
    }
    
    public boolean isUseProxy() {
        return useProxy;
    }
    
    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }
    
    public String getProxyUrl() {
        return proxyUrl;
    }
    
    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }
    
    public String getAzureEndpoint() {
        return azureEndpoint;
    }
    
    public void setAzureEndpoint(String azureEndpoint) {
        this.azureEndpoint = azureEndpoint;
    }
    
    public String getAzureDeploymentName() {
        return azureDeploymentName;
    }
    
    public void setAzureDeploymentName(String azureDeploymentName) {
        this.azureDeploymentName = azureDeploymentName;
    }
    
    public String getAzureApiVersion() {
        return azureApiVersion;
    }
    
    public void setAzureApiVersion(String azureApiVersion) {
        this.azureApiVersion = azureApiVersion;
    }
    
    public String getDefaultModel() {
        return defaultModel;
    }
    
    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }
    
    public Float getDefaultTemperature() {
        return defaultTemperature;
    }
    
    public void setDefaultTemperature(Float defaultTemperature) {
        this.defaultTemperature = defaultTemperature;
    }
    
    public Integer getDefaultMaxTokens() {
        return defaultMaxTokens;
    }
    
    public void setDefaultMaxTokens(Integer defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getCustomHeadersJson() {
        return customHeadersJson;
    }
    
    public void setCustomHeadersJson(String customHeadersJson) {
        this.customHeadersJson = customHeadersJson;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // =========================================================================
    // Helper Methods
    // =========================================================================
    
    /**
     * Get the default display name for a provider
     * 
     * @param provider The provider ID
     * @return The default display name
     */
    public static String getDefaultDisplayName(String provider) {
        switch (provider) {
            case PROVIDER_OPENAI:
                return "OpenAI";
            case PROVIDER_CLAUDE:
                return "Claude";
            case PROVIDER_GEMINI:
                return "Gemini";
            case PROVIDER_AZURE:
                return "Azure OpenAI";
            case PROVIDER_DEEPSEEK:
                return "DeepSeek";
            case PROVIDER_SILICONFLOW:
                return "SiliconFlow";
            case PROVIDER_OLLAMA:
                return "Ollama";
            case PROVIDER_GROQ:
                return "Groq";
            case PROVIDER_MISTRAL:
                return "Mistral AI";
            case PROVIDER_LMSTUDIO:
                return "LM Studio";
            case PROVIDER_PERPLEXITY:
                return "Perplexity";
            case PROVIDER_XAI:
                return "xAI";
            case PROVIDER_OPENROUTER:
                return "OpenRouter";
            case PROVIDER_CUSTOM:
                return "Custom";
            default:
                return provider;
        }
    }
    
    /**
     * Get the default API host for a provider
     * 
     * @param provider The provider ID
     * @return The default API host URL
     */
    public static String getDefaultHost(String provider) {
        switch (provider) {
            case PROVIDER_OPENAI:
                return DEFAULT_OPENAI_HOST;
            case PROVIDER_CLAUDE:
                return DEFAULT_CLAUDE_HOST;
            case PROVIDER_GEMINI:
                return DEFAULT_GEMINI_HOST;
            case PROVIDER_DEEPSEEK:
                return DEFAULT_DEEPSEEK_HOST;
            case PROVIDER_SILICONFLOW:
                return DEFAULT_SILICONFLOW_HOST;
            case PROVIDER_GROQ:
                return DEFAULT_GROQ_HOST;
            case PROVIDER_MISTRAL:
                return DEFAULT_MISTRAL_HOST;
            case PROVIDER_PERPLEXITY:
                return DEFAULT_PERPLEXITY_HOST;
            case PROVIDER_XAI:
                return DEFAULT_XAI_HOST;
            case PROVIDER_OPENROUTER:
                return DEFAULT_OPENROUTER_HOST;
            case PROVIDER_OLLAMA:
                return DEFAULT_OLLAMA_HOST;
            default:
                return "";
        }
    }
    
    /**
     * Get the full API URL
     * Combines apiHost and apiPath
     * 
     * @return The full API URL
     */
    public String getFullApiUrl() {
        if (apiHost == null || apiHost.isEmpty()) {
            return "";
        }
        String url = apiHost;
        if (apiPath != null && !apiPath.isEmpty()) {
            if (!apiPath.startsWith("/")) {
                url += "/";
            }
            url += apiPath;
        }
        return url;
    }
    
    /**
     * Check if the provider is properly configured
     * 
     * @return true if API key and host are set
     */
    public boolean isConfigured() {
        if (PROVIDER_AZURE.equals(provider)) {
            return azureEndpoint != null && !azureEndpoint.isEmpty() &&
                   apiKey != null && !apiKey.isEmpty();
        }
        return apiKey != null && !apiKey.isEmpty() &&
               apiHost != null && !apiHost.isEmpty();
    }
    
    /**
     * Check if this is an Azure provider
     * 
     * @return true if provider is Azure
     */
    public boolean isAzure() {
        return PROVIDER_AZURE.equals(provider);
    }
    
    /**
     * Check if this is a local provider (Ollama, LM Studio)
     * 
     * @return true if provider is local
     */
    public boolean isLocal() {
        return PROVIDER_OLLAMA.equals(provider) || PROVIDER_LMSTUDIO.equals(provider);
    }
    
    /**
     * Update the updatedAt timestamp
     */
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    // =========================================================================
    // Object Methods
    // =========================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderSettings that = (ProviderSettings) o;
        return Objects.equals(provider, that.provider);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(provider);
    }
    
    @Override
    public String toString() {
        return "ProviderSettings{" +
                "provider='" + provider + '\'' +
                ", displayName='" + displayName + '\'' +
                ", apiHost='" + apiHost + '\'' +
                ", enabled=" + enabled +
                ", configured=" + isConfigured() +
                '}';
    }
    
    /**
     * ModelInfo - Inner class representing model information
     */
    public static class ModelInfo {
        private String modelId;
        private String nickname;
        private List<String> capabilities;
        private Integer contextWindow;
        private Integer maxOutput;
        
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        
        public List<String> getCapabilities() { return capabilities; }
        public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
        
        public Integer getContextWindow() { return contextWindow; }
        public void setContextWindow(Integer contextWindow) { this.contextWindow = contextWindow; }
        
        public Integer getMaxOutput() { return maxOutput; }
        public void setMaxOutput(Integer maxOutput) { this.maxOutput = maxOutput; }
    }
}

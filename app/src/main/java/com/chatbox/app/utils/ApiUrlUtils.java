package com.chatbox.app.utils;

/**
 * API URL 工具类
 * 参考 chatbox-original/src/shared/utils/llm_utils.ts
 * 
 * API模式说明：
 * - OpenAI API兼容：使用 /chat/completions 路径（默认）
 * - OpenAI Responses API兼容：使用 /responses 路径
 */
public class ApiUrlUtils {
    
    // API模式常量
    public static final String API_MODE_CHAT = "chat";           // OpenAI API兼容模式
    public static final String API_MODE_RESPONSES = "responses"; // OpenAI Responses API兼容模式
    
    private static final String DEFAULT_HOST = "https://api.openai.com/v1";
    private static final String DEFAULT_PATH = "/chat/completions";
    private static final String RESPONSES_PATH = "/responses";
    
    /**
     * 规范化 OpenAI API 主机和路径
     * 
     * @param apiHost API 主机地址
     * @param apiPath API 路径（可选）
     * @return 包含规范化后的 apiHost 和 apiPath 的对象
     */
    public static ApiUrl normalizeApiHostAndPath(String apiHost, String apiPath) {
        return normalizeApiHostAndPath(apiHost, apiPath, DEFAULT_HOST, DEFAULT_PATH);
    }
    
    /**
     * 规范化 API 主机和路径
     * 
     * @param apiHost API 主机地址
     * @param apiPath API 路径（可选）
     * @param defaultHost 默认主机
     * @param defaultPath 默认路径
     * @return 包含规范化后的 apiHost 和 apiPath 的对象
     */
    public static ApiUrl normalizeApiHostAndPath(String apiHost, String apiPath, 
                                                  String defaultHost, String defaultPath) {
        // 如果 apiHost 为空，直接返回默认值
        if (apiHost == null || apiHost.trim().isEmpty()) {
            return new ApiUrl(defaultHost, defaultPath);
        }
        
        apiHost = apiHost.trim();
        if (apiPath != null) {
            apiPath = apiPath.trim();
            // 空字符串视为null
            if (apiPath.isEmpty()) {
                apiPath = null;
            }
        }
        
        // 处理前后 '/' 的干扰
        while (apiHost.endsWith("/")) {
            apiHost = apiHost.substring(0, apiHost.length() - 1);
        }
        if (apiPath != null && !apiPath.startsWith("/")) {
            apiPath = "/" + apiPath;
        }
        
        // 添加 https 协议
        if (!apiHost.startsWith("http://") && !apiHost.startsWith("https://")) {
            apiHost = "https://" + apiHost;
        }
        
        // 如果用户在 host 配置了完整的 host+path 接口地址
        // 例如：apiHost=https://my.proxy.com/v1/chat/completions
        if (apiHost.endsWith(defaultPath)) {
            apiHost = apiHost.substring(0, apiHost.length() - defaultPath.length());
            apiPath = defaultPath;
        }
        
        // 如果当前配置的是 OpenAI 的 API，统一为默认的 apiHost 和 apiPath
        if (apiHost.endsWith("://api.openai.com") || apiHost.endsWith("://api.openai.com/v1")) {
            return new ApiUrl(DEFAULT_HOST, DEFAULT_PATH);
        }
        
        // 如果当前配置的是 OpenRouter 的 API
        if (apiHost.endsWith("://openrouter.ai") || apiHost.endsWith("://openrouter.ai/api")) {
            return new ApiUrl("https://openrouter.ai/api/v1", DEFAULT_PATH);
        }
        
        // 如果当前配置的是 xAI 的 API
        if (apiHost.endsWith("://api.x.com") || apiHost.endsWith("://api.x.com/v1")) {
            return new ApiUrl("https://api.x.com/v1", DEFAULT_PATH);
        }
        
        // 关键逻辑：如果用户提供了自定义apiPath，直接使用，不自动添加/v1
        if (apiPath != null && !apiPath.isEmpty()) {
            return new ApiUrl(apiHost, apiPath);
        }
        
        // 如果只配置 apiHost，且 apiHost 不以 /v1 结尾，自动添加 /v1
        if (!apiHost.endsWith("/v1")) {
            apiHost = apiHost + "/v1";
        }
        
        // 使用默认路径
        return new ApiUrl(apiHost, defaultPath);
    }
    
    /**
     * 根据API模式获取默认路径
     * 
     * @param apiMode API模式 (chat 或 responses)
     * @return 默认路径
     */
    public static String getDefaultPathForMode(String apiMode) {
        if (API_MODE_RESPONSES.equals(apiMode)) {
            return RESPONSES_PATH;
        }
        return DEFAULT_PATH;
    }
    
    /**
     * 根据API模式获取默认主机
     * 
     * @param apiMode API模式
     * @return 默认主机
     */
    public static String getDefaultHostForMode(String apiMode) {
        if (API_MODE_RESPONSES.equals(apiMode)) {
            return "https://api.openai.com";
        }
        return DEFAULT_HOST;
    }
    
    /**
     * 获取完整的 API URL
     * 
     * @param apiHost API 主机
     * @param apiPath API 路径
     * @return 完整 URL
     */
    public static String getFullUrl(String apiHost, String apiPath) {
        ApiUrl normalized = normalizeApiHostAndPath(apiHost, apiPath);
        return normalized.apiHost + normalized.apiPath;
    }
    
    /**
     * 获取模型列表 URL
     * 
     * @param apiHost API 主机
     * @return 模型列表 URL
     */
    public static String getModelsUrl(String apiHost) {
        if (apiHost == null || apiHost.trim().isEmpty()) {
            return DEFAULT_HOST + "/models";
        }
        
        apiHost = apiHost.trim();
        
        // 移除末尾斜杠
        while (apiHost.endsWith("/")) {
            apiHost = apiHost.substring(0, apiHost.length() - 1);
        }
        
        // 添加 https 协议
        if (!apiHost.startsWith("http://") && !apiHost.startsWith("https://")) {
            apiHost = "https://" + apiHost;
        }
        
        // 如果以 /chat/completions 或 /responses 结尾，移除它
        if (apiHost.endsWith("/chat/completions")) {
            apiHost = apiHost.substring(0, apiHost.length() - "/chat/completions".length());
        } else if (apiHost.endsWith("/responses")) {
            apiHost = apiHost.substring(0, apiHost.length() - "/responses".length());
        }
        
        // 如果不以 /v1 结尾，添加 /v1
        if (!apiHost.endsWith("/v1")) {
            apiHost = apiHost + "/v1";
        }
        
        return apiHost + "/models";
    }
    
    /**
     * API URL 结果类
     */
    public static class ApiUrl {
        public final String apiHost;
        public final String apiPath;
        
        public ApiUrl(String apiHost, String apiPath) {
            this.apiHost = apiHost;
            this.apiPath = apiPath;
        }
        
        public String getFullUrl() {
            return apiHost + apiPath;
        }
        
        @Override
        public String toString() {
            return "ApiUrl{apiHost='" + apiHost + "', apiPath='" + apiPath + "'}";
        }
    }
}

package com.chatbox.app.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SettingsPreferences - Manages application settings using SharedPreferences
 * 
 * This class provides a centralized way to store and retrieve application settings.
 * It uses SharedPreferences for lightweight key-value storage.
 * 
 * Stored Settings:
 * - Theme (light/dark/system)
 * - Language
 * - Default provider and model
 * - Message display preferences
 * - Notification settings
 * - Privacy settings
 * 
 * For sensitive data like API keys, consider using EncryptedSharedPreferences.
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class SettingsPreferences {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "SettingsPreferences";
    
    /**
     * SharedPreferences file name
     */
    private static final String PREFS_NAME = "chatbox_settings";
    
    // =========================================================================
    // Preference Keys
    // =========================================================================
    
    // Theme Settings
    private static final String KEY_THEME = "theme";
    private static final String KEY_DYNAMIC_COLORS = "dynamic_colors";
    
    // Language Settings
    private static final String KEY_LANGUAGE = "language";
    
    // Default Provider Settings
    private static final String KEY_DEFAULT_PROVIDER = "default_provider";
    private static final String KEY_DEFAULT_MODEL = "default_model";
    
    // Message Display Settings
    private static final String KEY_SHOW_TIMESTAMP = "show_timestamp";
    private static final String KEY_SHOW_TOKEN_COUNT = "show_token_count";
    private static final String KEY_RENDER_MARKDOWN = "render_markdown";
    private static final String KEY_CODE_HIGHLIGHTING = "code_highlighting";
    private static final String KEY_FONT_SIZE = "font_size";
    
    // Chat Behavior Settings
    private static final String KEY_SEND_ON_ENTER = "send_on_enter";
    private static final String KEY_AUTO_SCROLL = "auto_scroll";
    private static final String KEY_STREAMING_RESPONSES = "streaming_responses";
    
    // Notification Settings
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    
    // Privacy Settings
    private static final String KEY_ANALYTICS_ENABLED = "analytics_enabled";
    private static final String KEY_CRASH_REPORTING = "crash_reporting";
    
    // Advanced Settings
    private static final String KEY_TIMEOUT_SECONDS = "timeout_seconds";
    private static final String KEY_MAX_RETRIES = "max_retries";
    private static final String KEY_RETRY_DELAY_MS = "retry_delay_ms";
    
    // First Launch
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_VERSION_CODE = "version_code";
    
    // =========================================================================
    // Default Values
    // =========================================================================
    
    private static final String DEFAULT_THEME = "system";
    private static final String DEFAULT_LANGUAGE = "system";
    private static final String DEFAULT_PROVIDER = "openai";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_RETRY_DELAY_MS = 1000;
    
    // =========================================================================
    // Singleton Instance
    // =========================================================================
    
    /**
     * Singleton instance
     */
    private static SettingsPreferences instance;
    
    /**
     * SharedPreferences instance
     */
    private final SharedPreferences preferences;
    
    /**
     * Private constructor
     * 
     * @param context The application context
     */
    private SettingsPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get the singleton instance
     * 
     * @param context The application context
     * @return SettingsPreferences instance
     */
    public static synchronized SettingsPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsPreferences(context.getApplicationContext());
        }
        return instance;
    }
    
    // =========================================================================
    // Theme Settings
    // =========================================================================
    
    /**
     * Get the current theme
     * 
     * @return Theme setting ("light", "dark", "system")
     */
    public String getTheme() {
        return preferences.getString(KEY_THEME, DEFAULT_THEME);
    }
    
    /**
     * Set the theme
     * 
     * @param theme The theme ("light", "dark", "system")
     */
    public void setTheme(String theme) {
        preferences.edit().putString(KEY_THEME, theme).apply();
        Log.d(TAG, "Theme set to: " + theme);
    }
    
    /**
     * Check if dynamic colors are enabled
     * 
     * @return true if dynamic colors are enabled
     */
    public boolean isDynamicColorsEnabled() {
        return preferences.getBoolean(KEY_DYNAMIC_COLORS, true);
    }
    
    /**
     * Set dynamic colors enabled
     * 
     * @param enabled true to enable dynamic colors
     */
    public void setDynamicColorsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply();
    }
    
    // =========================================================================
    // Language Settings
    // =========================================================================
    
    /**
     * Get the current language
     * 
     * @return Language code (e.g., "en", "zh", "system")
     */
    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }
    
    /**
     * Set the language
     * 
     * @param language The language code
     */
    public void setLanguage(String language) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply();
        Log.d(TAG, "Language set to: " + language);
    }
    
    // =========================================================================
    // Default Provider Settings
    // =========================================================================
    
    /**
     * Get the default AI provider
     * 
     * @return Provider ID (e.g., "openai", "claude")
     */
    public String getDefaultProvider() {
        return preferences.getString(KEY_DEFAULT_PROVIDER, DEFAULT_PROVIDER);
    }
    
    /**
     * Set the default AI provider
     * 
     * @param provider The provider ID
     */
    public void setDefaultProvider(String provider) {
        preferences.edit().putString(KEY_DEFAULT_PROVIDER, provider).apply();
        Log.d(TAG, "Default provider set to: " + provider);
    }
    
    /**
     * Get the default model
     * 
     * @return Model ID (e.g., "gpt-4", "claude-3-opus")
     */
    public String getDefaultModel() {
        return preferences.getString(KEY_DEFAULT_MODEL, DEFAULT_MODEL);
    }
    
    /**
     * Set the default model
     * 
     * @param model The model ID
     */
    public void setDefaultModel(String model) {
        preferences.edit().putString(KEY_DEFAULT_MODEL, model).apply();
        Log.d(TAG, "Default model set to: " + model);
    }
    
    // =========================================================================
    // Message Display Settings
    // =========================================================================
    
    /**
     * Check if timestamps should be shown
     * 
     * @return true if timestamps are enabled
     */
    public boolean isShowTimestamp() {
        return preferences.getBoolean(KEY_SHOW_TIMESTAMP, true);
    }
    
    /**
     * Set timestamp display
     * 
     * @param show true to show timestamps
     */
    public void setShowTimestamp(boolean show) {
        preferences.edit().putBoolean(KEY_SHOW_TIMESTAMP, show).apply();
    }
    
    /**
     * Check if token count should be shown
     * 
     * @return true if token count is enabled
     */
    public boolean isShowTokenCount() {
        return preferences.getBoolean(KEY_SHOW_TOKEN_COUNT, false);
    }
    
    /**
     * Set token count display
     * 
     * @param show true to show token count
     */
    public void setShowTokenCount(boolean show) {
        preferences.edit().putBoolean(KEY_SHOW_TOKEN_COUNT, show).apply();
    }
    
    /**
     * Check if markdown should be rendered
     * 
     * @return true if markdown rendering is enabled
     */
    public boolean isRenderMarkdown() {
        return preferences.getBoolean(KEY_RENDER_MARKDOWN, true);
    }
    
    /**
     * Set markdown rendering
     * 
     * @param render true to render markdown
     */
    public void setRenderMarkdown(boolean render) {
        preferences.edit().putBoolean(KEY_RENDER_MARKDOWN, render).apply();
    }
    
    /**
     * Check if code highlighting is enabled
     * 
     * @return true if code highlighting is enabled
     */
    public boolean isCodeHighlighting() {
        return preferences.getBoolean(KEY_CODE_HIGHLIGHTING, true);
    }
    
    /**
     * Set code highlighting
     * 
     * @param highlight true to enable code highlighting
     */
    public void setCodeHighlighting(boolean highlight) {
        preferences.edit().putBoolean(KEY_CODE_HIGHLIGHTING, highlight).apply();
    }
    
    /**
     * Get the font size for messages
     * 
     * @return Font size in sp
     */
    public int getFontSize() {
        return preferences.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    
    /**
     * Set the font size
     * 
     * @param size Font size in sp
     */
    public void setFontSize(int size) {
        preferences.edit().putInt(KEY_FONT_SIZE, size).apply();
    }
    
    // =========================================================================
    // Chat Behavior Settings
    // =========================================================================
    
    /**
     * Check if messages should be sent on Enter key
     * 
     * @return true if send on Enter is enabled
     */
    public boolean isSendOnEnter() {
        return preferences.getBoolean(KEY_SEND_ON_ENTER, true);
    }
    
    /**
     * Set send on Enter
     * 
     * @param send true to send on Enter
     */
    public void setSendOnEnter(boolean send) {
        preferences.edit().putBoolean(KEY_SEND_ON_ENTER, send).apply();
    }
    
    /**
     * Check if auto-scroll is enabled
     * 
     * @return true if auto-scroll is enabled
     */
    public boolean isAutoScroll() {
        return preferences.getBoolean(KEY_AUTO_SCROLL, true);
    }
    
    /**
     * Set auto-scroll
     * 
     * @param scroll true to enable auto-scroll
     */
    public void setAutoScroll(boolean scroll) {
        preferences.edit().putBoolean(KEY_AUTO_SCROLL, scroll).apply();
    }
    
    /**
     * Check if streaming responses are enabled
     * 
     * @return true if streaming is enabled
     */
    public boolean isStreamingResponses() {
        return preferences.getBoolean(KEY_STREAMING_RESPONSES, true);
    }
    
    /**
     * Set streaming responses
     * 
     * @param streaming true to enable streaming
     */
    public void setStreamingResponses(boolean streaming) {
        preferences.edit().putBoolean(KEY_STREAMING_RESPONSES, streaming).apply();
    }
    
    // =========================================================================
    // Notification Settings
    // =========================================================================
    
    /**
     * Check if notifications are enabled
     * 
     * @return true if notifications are enabled
     */
    public boolean isNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
    
    /**
     * Set notifications enabled
     * 
     * @param enabled true to enable notifications
     */
    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }
    
    /**
     * Check if sound is enabled
     * 
     * @return true if sound is enabled
     */
    public boolean isSoundEnabled() {
        return preferences.getBoolean(KEY_SOUND_ENABLED, true);
    }
    
    /**
     * Set sound enabled
     * 
     * @param enabled true to enable sound
     */
    public void setSoundEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }
    
    /**
     * Check if vibration is enabled
     * 
     * @return true if vibration is enabled
     */
    public boolean isVibrationEnabled() {
        return preferences.getBoolean(KEY_VIBRATION_ENABLED, true);
    }
    
    /**
     * Set vibration enabled
     * 
     * @param enabled true to enable vibration
     */
    public void setVibrationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }
    
    // =========================================================================
    // Privacy Settings
    // =========================================================================
    
    /**
     * Check if analytics are enabled
     * 
     * @return true if analytics are enabled
     */
    public boolean isAnalyticsEnabled() {
        return preferences.getBoolean(KEY_ANALYTICS_ENABLED, false);
    }
    
    /**
     * Set analytics enabled
     * 
     * @param enabled true to enable analytics
     */
    public void setAnalyticsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ANALYTICS_ENABLED, enabled).apply();
    }
    
    /**
     * Check if crash reporting is enabled
     * 
     * @return true if crash reporting is enabled
     */
    public boolean isCrashReporting() {
        return preferences.getBoolean(KEY_CRASH_REPORTING, true);
    }
    
    /**
     * Set crash reporting
     * 
     * @param enabled true to enable crash reporting
     */
    public void setCrashReporting(boolean enabled) {
        preferences.edit().putBoolean(KEY_CRASH_REPORTING, enabled).apply();
    }
    
    // =========================================================================
    // Advanced Settings
    // =========================================================================
    
    /**
     * Get the API timeout in seconds
     * 
     * @return Timeout in seconds
     */
    public int getTimeoutSeconds() {
        return preferences.getInt(KEY_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
    }
    
    /**
     * Set the API timeout
     * 
     * @param seconds Timeout in seconds
     */
    public void setTimeoutSeconds(int seconds) {
        preferences.edit().putInt(KEY_TIMEOUT_SECONDS, seconds).apply();
    }
    
    /**
     * Get the maximum number of retries
     * 
     * @return Maximum retries
     */
    public int getMaxRetries() {
        return preferences.getInt(KEY_MAX_RETRIES, DEFAULT_MAX_RETRIES);
    }
    
    /**
     * Set the maximum number of retries
     * 
     * @param retries Maximum retries
     */
    public void setMaxRetries(int retries) {
        preferences.edit().putInt(KEY_MAX_RETRIES, retries).apply();
    }
    
    /**
     * Get the retry delay in milliseconds
     * 
     * @return Retry delay in ms
     */
    public int getRetryDelayMs() {
        return preferences.getInt(KEY_RETRY_DELAY_MS, DEFAULT_RETRY_DELAY_MS);
    }
    
    /**
     * Set the retry delay
     * 
     * @param delayMs Delay in milliseconds
     */
    public void setRetryDelayMs(int delayMs) {
        preferences.edit().putInt(KEY_RETRY_DELAY_MS, delayMs).apply();
    }
    
    // =========================================================================
    // First Launch
    // =========================================================================
    
    /**
     * Check if this is the first launch
     * 
     * @return true if first launch
     */
    public boolean isFirstLaunch() {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    /**
     * Set first launch flag
     * 
     * @param first true if first launch
     */
    public void setFirstLaunch(boolean first) {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, first).apply();
    }
    
    /**
     * Get the stored version code
     * 
     * @return Version code, or -1 if not set
     */
    public int getVersionCode() {
        return preferences.getInt(KEY_VERSION_CODE, -1);
    }
    
    /**
     * Set the version code
     * 
     * @param versionCode The version code
     */
    public void setVersionCode(int versionCode) {
        preferences.edit().putInt(KEY_VERSION_CODE, versionCode).apply();
    }
    
    // =========================================================================
    // General Methods
    // =========================================================================
    
    /**
     * Clear all settings
     * Use with caution - resets all preferences to defaults
     */
    public void clearAll() {
        preferences.edit().clear().apply();
        Log.d(TAG, "All settings cleared");
    }
    
    /**
     * Register a listener for preference changes
     * 
     * @param listener The listener to register
     */
    public void registerOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }
    
    /**
     * Unregister a preference change listener
     * 
     * @param listener The listener to unregister
     */
    public void unregisterOnSharedPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }
}

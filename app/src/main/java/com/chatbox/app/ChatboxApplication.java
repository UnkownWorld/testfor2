package com.chatbox.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.chatbox.app.data.database.ChatboxDatabase;
import com.chatbox.app.data.preferences.SettingsPreferences;

/**
 * ChatboxApplication - Application class for the Chatbox app
 * 
 * This class serves as the main entry point for the application lifecycle.
 * It initializes global components and provides application-wide context.
 * 
 * Responsibilities:
 * - Initialize the application context
 * - Set up the database
 * - Initialize settings preferences
 * - Provide global access to application-level dependencies
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ChatboxApplication extends Application {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "ChatboxApplication";
    
    /**
     * Singleton instance of the application
     */
    private static ChatboxApplication instance;
    
    /**
     * Settings preferences instance
     */
    private SettingsPreferences settingsPreferences;

    /**
     * Called when the application is starting, before any other application objects are created.
     * This is where application-level initialization should happen.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Initializing Chatbox Application");
        
        // Store the application instance
        instance = this;
        
        // Initialize settings preferences
        initializeSettings();
        
        // Initialize database
        initializeDatabase();
        
        Log.d(TAG, "onCreate: Application initialized successfully");
    }

    /**
     * Initialize settings preferences
     * This method creates the SettingsPreferences instance for storing user settings
     */
    private void initializeSettings() {
        Log.d(TAG, "initializeSettings: Creating SettingsPreferences");
        settingsPreferences = SettingsPreferences.getInstance(this);
    }

    /**
     * Initialize the Room database
     * The database is created lazily, but we can perform any pre-database setup here
     */
    private void initializeDatabase() {
        Log.d(TAG, "initializeDatabase: Preparing database");
        // Database is created lazily when first accessed
        // This ensures we don't block the main thread during startup
    }

    /**
     * Get the application instance
     * 
     * @return The singleton ChatboxApplication instance
     */
    public static ChatboxApplication getInstance() {
        return instance;
    }

    /**
     * Get the application context
     * 
     * @return The application Context
     */
    public static Context getContext() {
        return instance.getApplicationContext();
    }

    /**
     * Get the settings preferences instance
     * 
     * @return SettingsPreferences instance for accessing user settings
     */
    public SettingsPreferences getSettingsPreferences() {
        return settingsPreferences;
    }

    /**
     * Called when the application is terminating
     * This is a good place to perform cleanup operations
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "onTerminate: Application terminating");
        // Perform cleanup if needed
    }

    /**
     * Called when the system is running low on memory
     * This is a good place to release cached data that can be recreated
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory: System running low on memory");
        // Release non-critical cached data
    }

    /**
     * Called when the operating system has determined that it is a good time
     * for a process to trim unneeded memory from its process
     * 
     * @param level The memory trim level
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d(TAG, "onTrimMemory: Level = " + level);
        // Handle different trim levels
        switch (level) {
            case TRIM_MEMORY_RUNNING_CRITICAL:
                // Release as much memory as possible
                break;
            case TRIM_MEMORY_RUNNING_LOW:
                // Release some memory
                break;
            case TRIM_MEMORY_UI_HIDDEN:
                // UI is hidden, can release UI-related resources
                break;
        }
    }
}

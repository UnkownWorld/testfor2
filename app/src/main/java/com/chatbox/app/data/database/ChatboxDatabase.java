package com.chatbox.app.data.database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.chatbox.app.data.dao.MessageDao;
import com.chatbox.app.data.dao.ProviderSettingsDao;
import com.chatbox.app.data.dao.SessionDao;
import com.chatbox.app.data.entity.Message;
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.data.entity.Session;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChatboxDatabase - Room database for the Chatbox application
 * 
 * This class defines the Room database configuration and provides access to DAOs.
 * It uses the singleton pattern to ensure only one database instance exists.
 * 
 * Database Schema:
 * - sessions: Stores chat sessions
 * - messages: Stores chat messages
 * - provider_settings: Stores AI provider configurations
 * 
 * Version History:
 * - 1: Initial version with sessions, messages, and provider_settings tables
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
@Database(
    entities = {
        Session.class,
        Message.class,
        ProviderSettings.class
    },
    version = 1,
    exportSchema = false
)
public abstract class ChatboxDatabase extends RoomDatabase {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "ChatboxDatabase";
    
    /**
     * Database file name
     */
    private static final String DATABASE_NAME = "chatbox_database";
    
    /**
     * Singleton instance of the database
     */
    private static volatile ChatboxDatabase instance;
    
    /**
     * Thread pool for database operations
     * Using a fixed thread pool with 4 threads
     */
    private static final ExecutorService databaseWriteExecutor = 
        Executors.newFixedThreadPool(4);
    
    // =========================================================================
    // DAO Accessors
    // =========================================================================
    
    /**
     * Get the SessionDao for session operations
     * 
     * @return SessionDao instance
     */
    public abstract SessionDao sessionDao();
    
    /**
     * Get the MessageDao for message operations
     * 
     * @return MessageDao instance
     */
    public abstract MessageDao messageDao();
    
    /**
     * Get the ProviderSettingsDao for provider settings operations
     * 
     * @return ProviderSettingsDao instance
     */
    public abstract ProviderSettingsDao providerSettingsDao();
    
    // =========================================================================
    // Singleton Instance
    // =========================================================================
    
    /**
     * Get the singleton instance of the database
     * Creates the database if it doesn't exist
     * 
     * @param context The application context
     * @return The ChatboxDatabase instance
     */
    public static ChatboxDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (ChatboxDatabase.class) {
                if (instance == null) {
                    Log.d(TAG, "Creating database instance");
                    instance = buildDatabase(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Build the database with configuration
     * 
     * @param context The application context
     * @return The built ChatboxDatabase
     */
    private static ChatboxDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(
                context,
                ChatboxDatabase.class,
                DATABASE_NAME
            )
            // Add migrations here when schema changes
            // .addMigrations(MIGRATION_1_2)
            
            // Callback for database creation and opening
            .addCallback(roomCallback)
            
            // Allow main thread queries for simple operations
            // In production, this should be false and all queries should use async methods
            .allowMainThreadQueries()
            
            // Build the database
            .build();
    }
    
    // =========================================================================
    // Database Callback
    // =========================================================================
    
    /**
     * Callback for database lifecycle events
     */
    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.d(TAG, "Database created");
            
            // Initialize default provider settings on database creation
            databaseWriteExecutor.execute(() -> {
                Log.d(TAG, "Initializing default provider settings");
                instance.providerSettingsDao().initializeDefaultSettings();
            });
        }
        
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            Log.d(TAG, "Database opened");
        }
        
        @Override
        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
            super.onDestructiveMigration(db);
            Log.w(TAG, "Destructive migration performed");
        }
    };
    
    // =========================================================================
    // Migrations
    // =========================================================================
    
    /**
     * Example migration from version 1 to 2
     * Uncomment and modify when schema changes
     */
    /*
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new column example
            // database.execSQL("ALTER TABLE sessions ADD COLUMN new_column TEXT");
        }
    };
    */
    
    // =========================================================================
    // Executor Service
    // =========================================================================
    
    /**
     * Get the database write executor
     * Use this for running database operations on background threads
     * 
     * @return ExecutorService for database writes
     */
    public static ExecutorService getDatabaseWriteExecutor() {
        return databaseWriteExecutor;
    }
    
    /**
     * Execute a database operation on a background thread
     * 
     * @param runnable The operation to execute
     */
    public static void executeOnBackground(Runnable runnable) {
        databaseWriteExecutor.execute(runnable);
    }
    
    // =========================================================================
    // Cleanup
    // =========================================================================
    
    /**
     * Close the database and clean up resources
     * Call this when the application is terminating
     */
    public static void closeDatabase() {
        if (instance != null) {
            Log.d(TAG, "Closing database");
            instance.close();
            instance = null;
        }
        databaseWriteExecutor.shutdown();
    }
    
    /**
     * Clear all data from the database
     * Use with caution - this will delete all sessions, messages, and settings
     */
    public void clearAllData() {
        Log.w(TAG, "Clearing all database data");
        executeOnBackground(() -> {
            sessionDao().deleteAll();
            messageDao().deleteAll();
            providerSettingsDao().deleteAll();
            // Re-initialize default settings
            providerSettingsDao().initializeDefaultSettings();
        });
    }
}

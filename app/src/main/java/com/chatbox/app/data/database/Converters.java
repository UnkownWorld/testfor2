package com.chatbox.app.data.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Converters - Type converters for Room database
 * 
 * This class provides type conversion methods for Room database.
 * Room can only store primitive types, so complex types like Lists
 * need to be converted to/from JSON strings.
 * 
 * Supported conversions:
 * - List<String> <-> JSON string
 * - List<Long> <-> JSON string
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class Converters {
    
    /**
     * Gson instance for JSON serialization/deserialization
     * Gson is thread-safe and can be reused
     */
    private static final Gson gson = new Gson();
    
    /**
     * Type token for List<String>
     * Used for Gson deserialization
     */
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();
    
    /**
     * Type token for List<Long>
     * Used for Gson deserialization
     */
    private static final Type LONG_LIST_TYPE = new TypeToken<List<Long>>() {}.getType();
    
    // =========================================================================
    // List<String> Converters
    // =========================================================================
    
    /**
     * Convert a List<String> to a JSON string
     * 
     * @param list The list to convert
     * @return JSON string representation of the list, or null if list is null
     */
    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }
    
    /**
     * Convert a JSON string to a List<String>
     * 
     * @param value The JSON string to convert
     * @return List<String> representation of the JSON string, or empty list if value is null
     */
    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return gson.fromJson(value, STRING_LIST_TYPE);
        } catch (Exception e) {
            // Return empty list if parsing fails
            return new ArrayList<>();
        }
    }
    
    // =========================================================================
    // List<Long> Converters
    // =========================================================================
    
    /**
     * Convert a List<Long> to a JSON string
     * 
     * @param list The list to convert
     * @return JSON string representation of the list, or null if list is null
     */
    @TypeConverter
    public static String fromLongList(List<Long> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }
    
    /**
     * Convert a JSON string to a List<Long>
     * 
     * @param value The JSON string to convert
     * @return List<Long> representation of the JSON string, or empty list if value is null
     */
    @TypeConverter
    public static List<Long> toLongList(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return gson.fromJson(value, LONG_LIST_TYPE);
        } catch (Exception e) {
            // Return empty list if parsing fails
            return new ArrayList<>();
        }
    }
}

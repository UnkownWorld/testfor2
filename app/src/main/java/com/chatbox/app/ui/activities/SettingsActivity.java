package com.chatbox.app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.chatbox.app.R;
import com.chatbox.app.data.preferences.SettingsPreferences;
import com.chatbox.app.databinding.ActivitySettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * SettingsActivity - Activity for app settings
 * 
 * This activity displays the app settings using PreferenceFragmentCompat.
 * It includes settings for:
 * - Appearance (theme, language)
 * - API configuration
 * - Chat behavior
 * - Notifications
 * - Privacy
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class SettingsActivity extends AppCompatActivity {
    
    private static final String TAG = "SettingsActivity";
    private ActivitySettingsBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Set up toolbar with back button
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }
        
        // Load settings fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // 返回上一级
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    
    /**
     * SettingsFragment - Fragment for displaying settings
     */
    public static class SettingsFragment extends PreferenceFragmentCompat 
            implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
        
        private SettingsPreferences preferences;
        
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            
            preferences = SettingsPreferences.getInstance(requireContext());
            
            // Set up preference click listeners
            setupPreferenceListeners();
        }
        
        private void setupPreferenceListeners() {
            // API Configuration
            Preference apiConfigPref = findPreference("api_configuration");
            if (apiConfigPref != null) {
                apiConfigPref.setOnPreferenceClickListener(this);
            }
            
            // Clear Data
            Preference clearDataPref = findPreference("clear_data");
            if (clearDataPref != null) {
                clearDataPref.setOnPreferenceClickListener(this);
            }
            
            // Theme
            Preference themePref = findPreference("theme");
            if (themePref != null) {
                themePref.setOnPreferenceChangeListener(this);
            }
            
            // Dynamic Colors
            SwitchPreferenceCompat dynamicColorsPref = findPreference("dynamic_colors");
            if (dynamicColorsPref != null) {
                dynamicColorsPref.setOnPreferenceChangeListener(this);
            }
        }
        
        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            String key = preference.getKey();
            
            if ("api_configuration".equals(key)) {
                Intent intent = new Intent(requireContext(), ApiConfigActivity.class);
                startActivity(intent);
                return true;
            } else if ("clear_data".equals(key)) {
                showClearDataDialog();
                return true;
            }
            
            return false;
        }
        
        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
            String key = preference.getKey();
            
            if ("theme".equals(key)) {
                String theme = (String) newValue;
                preferences.setTheme(theme);
                requireActivity().recreate();
                return true;
            } else if ("dynamic_colors".equals(key)) {
                boolean enabled = (Boolean) newValue;
                preferences.setDynamicColorsEnabled(enabled);
                requireActivity().recreate();
                return true;
            }
            
            return true;
        }
        
        private void showClearDataDialog() {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_data)
                .setMessage(R.string.clear_data_confirm)
                .setPositiveButton(R.string.clear, (dialog, which) -> {
                    clearAllData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        }
        
        private void clearAllData() {
            com.chatbox.app.data.database.ChatboxDatabase.getInstance(requireContext()).clearAllData();
            preferences.clearAll();
            Toast.makeText(requireContext(), R.string.data_cleared, Toast.LENGTH_SHORT).show();
        }
    }
}

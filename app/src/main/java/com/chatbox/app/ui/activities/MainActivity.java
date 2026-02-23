package com.chatbox.app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chatbox.app.R;
import com.chatbox.app.data.entity.Session;
import com.chatbox.app.databinding.ActivityMainBinding;
import com.chatbox.app.ui.adapters.SessionAdapter;
import com.chatbox.app.ui.viewmodels.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity - Main activity of the Chatbox app
 * 
 * This activity displays the list of chat sessions and provides
 * navigation to create new chats, open existing chats, and access settings.
 * 
 * Features:
 * - Display list of chat sessions
 * - Create new chat sessions
 * - Open existing chat sessions
 * - Delete chat sessions
 * - Star/unstar sessions
 * - Navigate to settings
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class MainActivity extends AppCompatActivity implements SessionAdapter.OnSessionClickListener {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "MainActivity";
    
    /**
     * View binding for the activity
     */
    private ActivityMainBinding binding;
    
    /**
     * ViewModel for this activity
     */
    private MainViewModel viewModel;
    
    /**
     * Adapter for the session list
     */
    private SessionAdapter sessionAdapter;
    
    // =========================================================================
    // Activity Lifecycle
    // =========================================================================
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // Initialize RecyclerView
        setupRecyclerView();
        
        // Set up FAB (Floating Action Button)
        setupFab();
        
        // Observe data
        observeData();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // Refresh the session list
        viewModel.refreshSessions();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        binding = null;
    }
    
    // =========================================================================
    // UI Setup
    // =========================================================================
    
    /**
     * Set up the RecyclerView for session list
     */
    private void setupRecyclerView() {
        sessionAdapter = new SessionAdapter(this, new ArrayList<>());
        sessionAdapter.setOnSessionClickListener(this);
        
        binding.recyclerViewSessions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSessions.setAdapter(sessionAdapter);
        
        // Add item decoration for spacing
        binding.recyclerViewSessions.addItemDecoration(
            new SessionAdapter.SessionItemDecoration(getResources().getDimensionPixelSize(R.dimen.session_item_margin))
        );
    }
    
    /**
     * Set up the Floating Action Button
     */
    private void setupFab() {
        binding.fabNewChat.setOnClickListener(v -> createNewSession());
    }
    
    /**
     * Observe LiveData from ViewModel
     */
    private void observeData() {
        // Observe sessions
        viewModel.getSessions().observe(this, sessions -> {
            Log.d(TAG, "Sessions updated: " + (sessions != null ? sessions.size() : 0) + " sessions");
            
            if (sessions == null || sessions.isEmpty()) {
                // Show empty state
                showEmptyState(true);
            } else {
                // Show session list
                showEmptyState(false);
                sessionAdapter.updateSessions(sessions);
            }
        });
        
        // Observe errors
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }
    
    /**
     * Show or hide the empty state view
     * 
     * @param show true to show empty state, false to hide
     */
    private void showEmptyState(boolean show) {
        binding.emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewSessions.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    // =========================================================================
    // Actions
    // =========================================================================
    
    /**
     * Create a new chat session
     */
    private void createNewSession() {
        Log.d(TAG, "Creating new session");
        
        // Check if default provider is configured
        if (!viewModel.isDefaultProviderConfigured()) {
            // Show dialog to configure API first
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.configure_api_first)
                .setMessage(R.string.configure_api_message)
                .setPositiveButton(R.string.configure, (dialog, which) -> {
                    openSettings();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
            return;
        }
        
        // Create new session
        Session session = viewModel.createSession();
        if (session != null) {
            openChat(session.getId());
        } else {
            Toast.makeText(this, R.string.error_creating_session, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Open a chat session
     * 
     * @param sessionId The session ID
     */
    private void openChat(String sessionId) {
        Log.d(TAG, "Opening chat: " + sessionId);
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_SESSION_ID, sessionId);
        startActivity(intent);
    }
    
    /**
     * Open settings
     */
    private void openSettings() {
        Log.d(TAG, "Opening settings");
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    /**
     * Show delete confirmation dialog
     * 
     * @param session The session to delete
     */
    private void showDeleteDialog(Session session) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_session)
            .setMessage(getString(R.string.delete_session_confirm, session.getName()))
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                viewModel.deleteSession(session);
                Toast.makeText(this, R.string.session_deleted, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    // =========================================================================
    // SessionAdapter.OnSessionClickListener
    // =========================================================================
    
    @Override
    public void onSessionClick(Session session) {
        openChat(session.getId());
    }
    
    @Override
    public void onSessionLongClick(Session session, View anchor) {
        // Show context menu
        showSessionContextMenu(session, anchor);
    }
    
    @Override
    public void onStarClick(Session session) {
        viewModel.toggleStarred(session);
    }
    
    /**
     * Show context menu for a session
     * 
     * @param session The session
     * @param anchor The anchor view for the popup
     */
    private void showSessionContextMenu(Session session, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_session_context, popup.getMenu());
        
        // Update star menu item
        MenuItem starItem = popup.getMenu().findItem(R.id.action_star);
        starItem.setTitle(session.isStarred() ? R.string.unstar : R.string.star);
        starItem.setIcon(session.isStarred() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.action_rename) {
                showRenameDialog(session);
                return true;
            } else if (itemId == R.id.action_star) {
                viewModel.toggleStarred(session);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteDialog(session);
                return true;
            }
            
            return false;
        });
        
        popup.show();
    }
    
    /**
     * Show rename dialog
     * 
     * @param session The session to rename
     */
    private void showRenameDialog(Session session) {
        // Create EditText for input
        com.google.android.material.textfield.TextInputEditText input = 
            new com.google.android.material.textfield.TextInputEditText(this);
        input.setText(session.getName());
        input.setSelection(session.getName().length());
        
        com.google.android.material.textfield.TextInputLayout textInputLayout = 
            new com.google.android.material.textfield.TextInputLayout(this);
        textInputLayout.setHint(getString(R.string.session_name));
        textInputLayout.addView(input);
        
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        textInputLayout.setPadding(padding, padding, padding, 0);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rename_session)
            .setView(textInputLayout)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String newName = input.getText() != null ? input.getText().toString().trim() : "";
                if (!newName.isEmpty()) {
                    viewModel.renameSession(session, newName);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    // =========================================================================
    // Menu
    // =========================================================================
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_settings) {
            openSettings();
            return true;
        } else if (itemId == R.id.action_search) {
            // TODO: Implement search
            Toast.makeText(this, R.string.search_coming_soon, Toast.LENGTH_SHORT).show();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}

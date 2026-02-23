package com.chatbox.app.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import com.chatbox.app.data.entity.Message;
import com.chatbox.app.databinding.ActivityChatBinding;
import com.chatbox.app.ui.adapters.MessageAdapter;
import com.chatbox.app.ui.viewmodels.ChatViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatActivity - Activity for chatting with AI
 * 
 * This activity displays a chat interface where users can:
 * - View message history
 * - Send new messages
 * - Receive AI responses
 * - Copy and delete messages
 * 
 * @author Chatbox Team
 * @version 1.0.0
 * @since 2024
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnMessageClickListener {
    
    /**
     * Log tag for debugging
     */
    private static final String TAG = "ChatActivity";
    
    /**
     * Extra key for session ID
     */
    public static final String EXTRA_SESSION_ID = "session_id";
    
    /**
     * View binding
     */
    private ActivityChatBinding binding;
    
    /**
     * ViewModel
     */
    private ChatViewModel viewModel;
    
    /**
     * Message adapter
     */
    private MessageAdapter messageAdapter;
    
    /**
     * Session ID
     */
    private String sessionId;
    
    // =========================================================================
    // Activity Lifecycle
    // =========================================================================
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get session ID from intent
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        if (sessionId == null || sessionId.isEmpty()) {
            Log.e(TAG, "No session ID provided");
            Toast.makeText(this, R.string.error_session_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.setSessionId(sessionId);
        
        // Initialize RecyclerView
        setupRecyclerView();
        
        // Set up input
        setupInput();
        
        // Observe data
        observeData();
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
     * Set up the RecyclerView for messages
     */
    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, new ArrayList<>());
        messageAdapter.setOnMessageClickListener(this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        
        binding.recyclerViewMessages.setLayoutManager(layoutManager);
        binding.recyclerViewMessages.setAdapter(messageAdapter);
        
        // Scroll to bottom when new messages arrive
        messageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (viewModel.isAutoScroll()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }
        });
    }
    
    /**
     * Set up the input area
     */
    private void setupInput() {
        // Send button click
        binding.buttonSend.setOnClickListener(v -> sendMessage());
        
        // Enter key handling
        binding.editMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (viewModel.isSendOnEnter()) {
                sendMessage();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Observe LiveData from ViewModel
     */
    private void observeData() {
        // Observe session
        viewModel.getSession().observe(this, session -> {
            if (session != null) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(session.getDisplayTitle());
                }
            }
        });
        
        // Observe messages
        viewModel.getMessages().observe(this, messages -> {
            if (messages != null) {
                messageAdapter.updateMessages(messages);
                if (!messages.isEmpty()) {
                    binding.recyclerViewMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.buttonSend.setEnabled(!isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // Observe errors
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    // =========================================================================
    // Actions
    // =========================================================================
    
    /**
     * Send a message
     */
    private void sendMessage() {
        String content = binding.editMessage.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }
        
        // Clear input
        binding.editMessage.setText("");
        
        // Send message
        viewModel.sendMessage(content, new ChatViewModel.SendCallback() {
            @Override
            public void onChunk(String chunk) {
                // Update UI with chunk (handled by LiveData)
            }
            
            @Override
            public void onComplete() {
                // Message complete
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(ChatActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    /**
     * Clear all messages in the session
     */
    private void clearChat() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_chat)
            .setMessage(R.string.clear_chat_confirm)
            .setPositiveButton(R.string.clear, (dialog, which) -> {
                viewModel.clearMessages();
                Toast.makeText(this, R.string.chat_cleared, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Copy message to clipboard
     * 
     * @param message The message to copy
     */
    private void copyMessage(Message message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Message", message.getContent());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.message_copied, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Delete a message
     * 
     * @param message The message to delete
     */
    private void deleteMessage(Message message) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_message)
            .setMessage(R.string.delete_session_confirm)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                viewModel.deleteMessage(message);
                Toast.makeText(this, R.string.message_deleted, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    // =========================================================================
    // MessageAdapter.OnMessageClickListener
    // =========================================================================
    
    @Override
    public void onMessageClick(Message message) {
        // Handle message click (could expand/collapse)
    }
    
    @Override
    public void onMessageLongClick(Message message, View anchor) {
        // Show context menu
        showMessageContextMenu(message, anchor);
    }
    
    /**
     * Show context menu for a message
     * 
     * @param message The message
     * @param anchor The anchor view
     */
    private void showMessageContextMenu(Message message, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_message_context, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.action_copy) {
                copyMessage(message);
                return true;
            } else if (itemId == R.id.action_delete) {
                deleteMessage(message);
                return true;
            }
            
            return false;
        });
        
        popup.show();
    }
    
    // =========================================================================
    // Menu
    // =========================================================================
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_clear) {
            clearChat();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}

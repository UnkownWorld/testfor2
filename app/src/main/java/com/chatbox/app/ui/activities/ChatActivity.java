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
import com.chatbox.app.data.entity.ProviderSettings;
import com.chatbox.app.data.entity.Session;
import com.chatbox.app.databinding.ActivityChatBinding;
import com.chatbox.app.ui.adapters.MessageAdapter;
import com.chatbox.app.ui.viewmodels.ChatViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatActivity - Activity for chatting with AI
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnMessageClickListener {
    
    private static final String TAG = "ChatActivity";
    public static final String EXTRA_SESSION_ID = "session_id";
    
    private ActivityChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private String sessionId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        if (sessionId == null || sessionId.isEmpty()) {
            Log.e(TAG, "No session ID provided");
            Toast.makeText(this, R.string.error_session_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.setSessionId(sessionId);
        
        setupRecyclerView();
        setupInput();
        observeData();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        binding = null;
    }
    
    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, new ArrayList<>());
        messageAdapter.setOnMessageClickListener(this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        
        binding.recyclerViewMessages.setLayoutManager(layoutManager);
        binding.recyclerViewMessages.setAdapter(messageAdapter);
        
        messageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (viewModel.isAutoScroll()) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }
        });
    }
    
    private void setupInput() {
        binding.buttonSend.setOnClickListener(v -> sendMessage());
        
        binding.editMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (viewModel.isSendOnEnter()) {
                sendMessage();
                return true;
            }
            return false;
        });
    }
    
    private void observeData() {
        viewModel.getSession().observe(this, session -> {
            if (session != null && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(session.getDisplayTitle());
                updateSubtitle(session);
            }
        });
        
        viewModel.getMessages().observe(this, messages -> {
            if (messages != null) {
                messageAdapter.updateMessages(messages);
                if (!messages.isEmpty()) {
                    binding.recyclerViewMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });
        
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.buttonSend.setEnabled(!isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // 观察错误消息并显示给用户
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showErrorDialog(error);
            }
        });
    }
    
    private void updateSubtitle(Session session) {
        if (getSupportActionBar() != null) {
            String provider = viewModel.getProviderDisplayName(session.getProvider());
            String model = session.getModel();
            if (provider != null && model != null) {
                getSupportActionBar().setSubtitle(provider + " / " + model);
            }
        }
    }
    
    private void sendMessage() {
        String content = binding.editMessage.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }
        
        binding.editMessage.setText("");
        
        viewModel.sendMessage(content, new ChatViewModel.SendCallback() {
            @Override
            public void onChunk(String chunk) {}
            
            @Override
            public void onComplete() {}
            
            @Override
            public void onError(String error) {
                // 错误已经在ViewModel中通过LiveData传递，这里不需要额外处理
                Log.e(TAG, "Send message error: " + error);
            }
        });
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String error) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.error)
            .setMessage(error)
            .setPositiveButton(R.string.ok, null)
            .show();
    }
    
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
     * 显示切换模型对话框
     */
    private void showSwitchModelDialog() {
        Session session = viewModel.getSession().getValue();
        if (session == null) {
            showErrorDialog("会话不存在");
            return;
        }
        
        // 获取当前提供商的模型列表（从已保存的模型）
        List<String> models = viewModel.getModelsForProvider(session.getProvider());
        if (models.isEmpty()) {
            // 如果没有保存的模型，提示用户先配置
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.no_models_found)
                .setMessage("请先在API配置中选择要使用的模型")
                .setPositiveButton(R.string.ok, null)
                .show();
            return;
        }
        
        String[] modelArray = models.toArray(new String[0]);
        int currentIndex = models.indexOf(session.getModel());
        if (currentIndex < 0) currentIndex = 0;
        
        int[] selectedIndex = {currentIndex};
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.switch_model)
            .setSingleChoiceItems(modelArray, currentIndex, (dialog, which) -> {
                selectedIndex[0] = which;
            })
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String newModel = models.get(selectedIndex[0]);
                viewModel.updateSessionModel(newModel);
                Toast.makeText(this, getString(R.string.current_model, newModel), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * 显示切换提供商对话框
     */
    private void showSwitchProviderDialog() {
        List<ProviderSettings> providers = viewModel.getConfiguredProviders();
        if (providers.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("无可用提供商")
                .setMessage("请先在设置中配置API提供商")
                .setPositiveButton(R.string.ok, null)
                .show();
            return;
        }
        
        Session session = viewModel.getSession().getValue();
        String currentProvider = session != null ? session.getProvider() : "";
        
        List<String> providerNames = new ArrayList<>();
        List<String> providerIds = new ArrayList<>();
        int currentIndex = 0;
        
        for (int i = 0; i < providers.size(); i++) {
            ProviderSettings p = providers.get(i);
            providerNames.add(p.getDisplayName());
            providerIds.add(p.getProvider());
            if (p.getProvider().equals(currentProvider)) {
                currentIndex = i;
            }
        }
        
        String[] providerArray = providerNames.toArray(new String[0]);
        int[] selectedIndex = {currentIndex};
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.switch_provider)
            .setSingleChoiceItems(providerArray, currentIndex, (dialog, which) -> {
                selectedIndex[0] = which;
            })
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String newProviderId = providerIds.get(selectedIndex[0]);
                ProviderSettings newProvider = providers.get(selectedIndex[0]);
                
                // 获取新提供商的模型列表
                List<String> models = viewModel.getModelsForProvider(newProviderId);
                String defaultModel = models.isEmpty() ? "gpt-4o-mini" : models.get(0);
                
                // 更新会话的提供商和模型
                viewModel.updateSessionProvider(newProviderId, defaultModel);
                
                Toast.makeText(this, getString(R.string.current_provider, newProvider.getDisplayName()), 
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    private void copyMessage(Message message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Message", message.getContent());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.message_copied, Toast.LENGTH_SHORT).show();
    }
    
    private void editMessage(Message message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        TextInputEditText inputContent = dialogView.findViewById(R.id.input_message_content);
        inputContent.setText(message.getContent());
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_message)
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String newContent = inputContent.getText() != null ? 
                    inputContent.getText().toString().trim() : "";
                if (!newContent.isEmpty()) {
                    message.setContent(newContent);
                    viewModel.updateMessage(message);
                    Toast.makeText(this, R.string.message_updated, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
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
    
    @Override
    public void onMessageClick(Message message) {
        // Handle message click (could expand/collapse)
    }
    
    @Override
    public void onMessageLongClick(Message message, View anchor) {
        showMessageContextMenu(message, anchor);
    }
    
    private void showMessageContextMenu(Message message, View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_message_context, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.action_edit) {
                editMessage(message);
                return true;
            } else if (itemId == R.id.action_copy) {
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_clear) {
            clearChat();
            return true;
        } else if (itemId == R.id.action_switch_model) {
            showSwitchModelDialog();
            return true;
        } else if (itemId == R.id.action_switch_provider) {
            showSwitchProviderDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}

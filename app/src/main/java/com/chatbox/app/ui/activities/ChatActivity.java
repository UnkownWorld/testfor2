package com.chatbox.app.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.chatbox.app.utils.FileContentManager;
import com.chatbox.app.utils.FileSplitter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatActivity - Activity for chatting with AI
 * 
 * Supports file attachment and content splitting features.
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnMessageClickListener {
    
    private static final String TAG = "ChatActivity";
    public static final String EXTRA_SESSION_ID = "session_id";
    
    // SharedPreferences keys
    private static final String PREFS_NAME = "file_split_prefs";
    private static final String KEY_LAST_REGEX = "last_regex";
    private static final String KEY_LAST_BATCH_SIZE = "last_batch_size";
    private static final String KEY_LAST_SPLIT_MODE = "last_split_mode";
    
    private ActivityChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;
    private String sessionId;
    
    // File attachment
    private FileContentManager fileContentManager;
    private ActivityResultLauncher<String[]> filePickerLauncher;
    private int currentBatchIndex = 0;
    private int selectedBatchIndex = -1; // -1 means send all content
    
    // Split settings
    private SharedPreferences splitPrefs;
    private boolean hasPerformedSplit = false; // 是否已执行分割
    
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
        
        // Initialize preferences
        splitPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Initialize file content manager
        fileContentManager = new FileContentManager(this);
        
        // Initialize file picker
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::handleFilePicked
        );
        
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
        
        // File attachment button
        binding.buttonAttachFile.setOnClickListener(v -> openFilePicker());
        
        // Remove file button
        binding.buttonRemoveFile.setOnClickListener(v -> removeFileAttachment());
        
        // Split options button
        binding.buttonSplitOptions.setOnClickListener(v -> showSplitOptionsDialog());
        
        // Select batch button
        binding.buttonSelectBatch.setOnClickListener(v -> showBatchSelectionDialog());
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
    
    /**
     * Open file picker
     */
    private void openFilePicker() {
        String[] mimeTypes = {
            "text/plain",
            "text/markdown",
            "application/json",
            "text/html",
            "text/csv",
            "text/xml"
        };
        filePickerLauncher.launch(mimeTypes);
    }
    
    /**
     * Handle file picked from picker
     */
    private void handleFilePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        
        Log.d(TAG, "File picked: " + uri.toString());
        
        // Read file content
        FileContentManager.FileAttachment file = fileContentManager.readFile(uri);
        
        if (file.hasError()) {
            Toast.makeText(this, file.getError(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Reset split state
        hasPerformedSplit = false;
        selectedBatchIndex = -1;
        currentBatchIndex = 0;
        
        // Update UI - show file attached but don't split yet
        updateFileAttachmentUI(file);
        
        // Show hint for user to configure split
        binding.textSplitInfo.setVisibility(View.VISIBLE);
        binding.textSplitInfo.setText(R.string.click_split_to_configure);
        binding.buttonSelectBatch.setVisibility(View.GONE);
        
        Toast.makeText(this, R.string.file_attached, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Update file attachment UI
     */
    private void updateFileAttachmentUI(FileContentManager.FileAttachment file) {
        if (file == null || file.hasError()) {
            binding.fileAttachmentLayout.setVisibility(View.GONE);
            return;
        }
        
        binding.fileAttachmentLayout.setVisibility(View.VISIBLE);
        binding.textFileName.setText(file.getFileName());
        binding.textFileInfo.setText(fileContentManager.getFileInfo());
    }
    
    /**
     * Perform content split with current settings
     */
    private void performSplit() {
        FileSplitter splitter = fileContentManager.getSplitter();
        if (splitter == null) {
            return;
        }
        
        // Apply saved regex pattern if exists
        String lastRegex = splitPrefs.getString(KEY_LAST_REGEX, "");
        if (!lastRegex.isEmpty()) {
            splitter.setSplitPattern(lastRegex);
        }
        
        // Apply saved batch size
        int lastBatchSize = splitPrefs.getInt(KEY_LAST_BATCH_SIZE, 5);
        splitter.setBatchSize(lastBatchSize);
        
        int segmentCount = splitter.split();
        hasPerformedSplit = true;
        
        if (segmentCount <= 1) {
            binding.textSplitInfo.setVisibility(View.VISIBLE);
            binding.textSplitInfo.setText(R.string.no_chapters_detected);
            binding.buttonSelectBatch.setVisibility(View.GONE);
        } else {
            binding.textSplitInfo.setVisibility(View.VISIBLE);
            binding.textSplitInfo.setText(fileContentManager.getSplitInfo());
            binding.buttonSelectBatch.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Remove file attachment
     */
    private void removeFileAttachment() {
        fileContentManager.clearFile();
        selectedBatchIndex = -1;
        currentBatchIndex = 0;
        hasPerformedSplit = false;
        binding.fileAttachmentLayout.setVisibility(View.GONE);
        Toast.makeText(this, R.string.file_removed, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show split options dialog
     */
    private void showSplitOptionsDialog() {
        FileSplitter splitter = fileContentManager.getSplitter();
        if (splitter == null) {
            Toast.makeText(this, R.string.no_file_attached, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] options = {
            getString(R.string.split_by_chapter),
            getString(R.string.split_by_lines),
            getString(R.string.split_by_chars),
            getString(R.string.split_by_regex)
        };
        
        // Get last used mode
        int lastMode = splitPrefs.getInt(KEY_LAST_SPLIT_MODE, 0);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_options)
            .setSingleChoiceItems(options, lastMode, (dialog, which) -> {
                // Save selected mode
                splitPrefs.edit().putInt(KEY_LAST_SPLIT_MODE, which).apply();
            })
            .setPositiveButton(R.string.apply_split, (dialog, which) -> {
                int selectedMode = splitPrefs.getInt(KEY_LAST_SPLIT_MODE, 0);
                switch (selectedMode) {
                    case 0:
                        showChapterSplitOptions();
                        break;
                    case 1:
                        showLineSplitOptions();
                        break;
                    case 2:
                        showCharSplitOptions();
                        break;
                    case 3:
                        showRegexSplitOptions();
                        break;
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show chapter split options
     */
    private void showChapterSplitOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        TextInputEditText input = dialogView.findViewById(R.id.input_message_content);
        input.setHint(getString(R.string.batch_size) + " (default: 5)");
        
        // Use last saved batch size
        int lastBatchSize = splitPrefs.getInt(KEY_LAST_BATCH_SIZE, 5);
        input.setText(String.valueOf(lastBatchSize));
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_by_chapter)
            .setView(dialogView)
            .setPositiveButton(R.string.apply_split, (dialog, which) -> {
                String text = input.getText() != null ? input.getText().toString().trim() : "5";
                int batchSize;
                try {
                    batchSize = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    batchSize = 5;
                }
                
                // Save batch size
                splitPrefs.edit().putInt(KEY_LAST_BATCH_SIZE, batchSize).apply();
                
                FileSplitter splitter = fileContentManager.getSplitter();
                if (splitter != null) {
                    splitter.setBatchSize(batchSize);
                    // Reset pattern to default chapter pattern
                    splitter.setSplitPattern("第[零一二三四五六七八九十百千万\\d]+[章节回][^\n]*");
                    splitPrefs.edit().putString(KEY_LAST_REGEX, "").apply();
                    performSplit();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show line split options
     */
    private void showLineSplitOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        TextInputEditText input = dialogView.findViewById(R.id.input_message_content);
        input.setHint(getString(R.string.lines_per_segment) + " (default: 100)");
        input.setText("100");
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_by_lines)
            .setView(dialogView)
            .setPositiveButton(R.string.apply_split, (dialog, which) -> {
                String text = input.getText() != null ? input.getText().toString().trim() : "100";
                int lines;
                try {
                    lines = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    lines = 100;
                }
                
                FileContentManager.FileAttachment file = fileContentManager.getCurrentFile();
                if (file != null) {
                    // Create custom splitter for line-based split
                    FileSplitter lineSplitter = new FileSplitter(file.getContent());
                    lineSplitter.setBatchSize(1);
                    fileContentManager.setCustomSplitter(lineSplitter, lines);
                    hasPerformedSplit = true;
                    
                    int segmentCount = lineSplitter.split();
                    binding.textSplitInfo.setVisibility(View.VISIBLE);
                    binding.textSplitInfo.setText("按行分割: " + segmentCount + " 段");
                    binding.buttonSelectBatch.setVisibility(segmentCount > 1 ? View.VISIBLE : View.GONE);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show character split options
     */
    private void showCharSplitOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        TextInputEditText input = dialogView.findViewById(R.id.input_message_content);
        input.setHint(getString(R.string.chars_per_segment) + " (default: 5000)");
        input.setText("5000");
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_by_chars)
            .setView(dialogView)
            .setPositiveButton(R.string.apply_split, (dialog, which) -> {
                String text = input.getText() != null ? input.getText().toString().trim() : "5000";
                int chars;
                try {
                    chars = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    chars = 5000;
                }
                
                FileContentManager.FileAttachment file = fileContentManager.getCurrentFile();
                if (file != null) {
                    // Create custom splitter for char-based split
                    FileSplitter charSplitter = new FileSplitter(file.getContent());
                    charSplitter.setBatchSize(1);
                    fileContentManager.setCustomSplitter(charSplitter, chars);
                    hasPerformedSplit = true;
                    
                    int segmentCount = charSplitter.split();
                    binding.textSplitInfo.setVisibility(View.VISIBLE);
                    binding.textSplitInfo.setText("按字符分割: " + segmentCount + " 段");
                    binding.buttonSelectBatch.setVisibility(segmentCount > 1 ? View.VISIBLE : View.GONE);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show regex split options - with persistence
     */
    private void showRegexSplitOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        TextInputEditText input = dialogView.findViewById(R.id.input_message_content);
        input.setHint(getString(R.string.regex_pattern));
        
        // Use last saved regex or default
        String lastRegex = splitPrefs.getString(KEY_LAST_REGEX, "第[零一二三四五六七八九十百千万\\d]+[章节回]");
        input.setText(lastRegex);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_by_regex)
            .setView(dialogView)
            .setPositiveButton(R.string.apply_split, (dialog, which) -> {
                String regex = input.getText() != null ? input.getText().toString().trim() : "";
                if (!regex.isEmpty()) {
                    // Save regex
                    splitPrefs.edit().putString(KEY_LAST_REGEX, regex).apply();
                    
                    FileSplitter splitter = fileContentManager.getSplitter();
                    if (splitter != null) {
                        splitter.setSplitPattern(regex);
                        performSplit();
                        Toast.makeText(this, R.string.regex_saved, Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show batch selection dialog
     */
    private void showBatchSelectionDialog() {
        if (!hasPerformedSplit) {
            Toast.makeText(this, R.string.please_split_first, Toast.LENGTH_SHORT).show();
            return;
        }
        
        FileSplitter splitter = fileContentManager.getSplitter();
        if (splitter == null) {
            Toast.makeText(this, R.string.no_file_attached, Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<FileSplitter.Batch> batches = splitter.getBatches();
        if (batches.isEmpty()) {
            Toast.makeText(this, R.string.no_chapters_detected, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Build batch names - only show chapter titles, no prefix
        String[] batchNames = new String[batches.size() + 1];
        batchNames[0] = getString(R.string.send_all_content);
        for (int i = 0; i < batches.size(); i++) {
            batchNames[i + 1] = batches.get(i).getTitle();
        }
        
        int currentIndex = selectedBatchIndex + 1;
        if (currentIndex < 0 || currentIndex >= batchNames.length) {
            currentIndex = 0;
        }
        
        int[] selectedIndex = {currentIndex};
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_batch)
            .setSingleChoiceItems(batchNames, currentIndex, (dialog, which) -> {
                selectedIndex[0] = which;
            })
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                selectedBatchIndex = selectedIndex[0] - 1;
                currentBatchIndex = Math.max(0, selectedBatchIndex);
                
                // Update UI to show selected batch title only
                if (selectedBatchIndex >= 0) {
                    FileSplitter.Batch batch = splitter.getBatch(selectedBatchIndex);
                    if (batch != null) {
                        binding.textSplitInfo.setText(batch.getTitle());
                    }
                } else {
                    binding.textSplitInfo.setText(fileContentManager.getSplitInfo());
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    private void sendMessage() {
        String userContent = binding.editMessage.getText().toString().trim();
        
        // Check if there's a file attached
        String fileContent = null;
        if (fileContentManager.hasFile()) {
            fileContent = fileContentManager.getContentForSending(selectedBatchIndex);
        }
        
        // User content is empty and no file
        if (userContent.isEmpty() && fileContent == null) {
            return;
        }
        
        binding.editMessage.setText("");
        
        // Build message: user content first, then file content
        // File content is passed to API but not shown in conversation
        String displayContent = userContent;
        String apiContent = userContent;
        
        if (fileContent != null && !fileContent.isEmpty()) {
            // Append file content after user content for API
            apiContent = userContent + "\n\n" + fileContent;
        }
        
        // Send message - displayContent shows in UI, apiContent goes to API
        viewModel.sendMessageWithFile(displayContent, apiContent, new ChatViewModel.SendCallback() {
            @Override
            public void onChunk(String chunk) {}
            
            @Override
            public void onComplete() {
                // After sending, move to next batch if in batch mode
                if (selectedBatchIndex >= 0) {
                    FileSplitter splitter = fileContentManager.getSplitter();
                    if (splitter != null && currentBatchIndex < splitter.getBatchCount() - 1) {
                        currentBatchIndex++;
                        selectedBatchIndex = currentBatchIndex;
                        
                        // Update UI
                        runOnUiThread(() -> {
                            FileSplitter.Batch batch = splitter.getBatch(currentBatchIndex);
                            if (batch != null) {
                                binding.textSplitInfo.setText(batch.getTitle());
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onError(String error) {
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
        
        List<String> models = viewModel.getModelsForProvider(session.getProvider());
        if (models.isEmpty()) {
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
                
                List<String> models = viewModel.getModelsForProvider(newProviderId);
                String defaultModel = models.isEmpty() ? "gpt-4o-mini" : models.get(0);
                
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

package com.chatbox.app.ui.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.chatbox.app.utils.SkillManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatActivity - Activity for chatting with AI
 * 
 * Supports file attachment, content splitting, and skill management features.
 */
public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnMessageClickListener {
    
    private static final String TAG = "ChatActivity";
    public static final String EXTRA_SESSION_ID = "session_id";
    private static final int PERMISSION_REQUEST_STORAGE = 1001;
    
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
    private int selectedBatchIndex = -1;
    
    // Split settings
    private SharedPreferences splitPrefs;
    private boolean hasPerformedSplit = false;
    
    // Skill management
    private SkillManager skillManager;
    private ActivityResultLauncher<Uri> skillFolderPickerLauncher;
    
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
        
        // Initialize skill manager
        skillManager = new SkillManager(this);
        
        // Initialize file content manager
        fileContentManager = new FileContentManager(this);
        
        // Initialize file picker
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::handleFilePicked
        );
        
        // Initialize skill folder picker
        skillFolderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            this::handleSkillFolderPicked
        );
        
        setupRecyclerView();
        setupInput();
        observeData();
        updateSkillUI();
        
        // Request storage permission for skill management
        checkStoragePermission();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh skill list when returning to activity
        updateSkillUI();
    }
    
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses scoped storage, no permission needed for app-specific dirs
            // But for public Downloads folder, we need to check
            if (!Environment.isExternalStorageManager()) {
                // For Android 11+, we can still access Downloads via MediaStore API
                // No special permission needed for our use case
            }
        } else {
            // Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, 
                                 Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_STORAGE);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission granted");
            } else {
                Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
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
        
        // Skill selection button
        binding.buttonSelectSkills.setOnClickListener(v -> showSkillSelectionDialog());
        
        // Remove skills button
        binding.buttonRemoveSkills.setOnClickListener(v -> clearSelectedSkills());
        
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
    
    // ==================== Skill Management ====================
    
    /**
     * Update skill UI display
     */
    private void updateSkillUI() {
        List<SkillManager.SkillFile> selectedSkills = skillManager.getSelectedSkills();
        
        if (selectedSkills.isEmpty()) {
            binding.skillLayout.setVisibility(View.GONE);
        } else {
            binding.skillLayout.setVisibility(View.VISIBLE);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Skills: ");
            for (int i = 0; i < selectedSkills.size(); i++) {
                if (i > 0) sb.append(" → ");
                sb.append(selectedSkills.get(i).getName());
            }
            binding.textSelectedSkills.setText(sb.toString());
        }
    }
    
    /**
     * Clear selected skills
     */
    private void clearSelectedSkills() {
        skillManager.setSelectedSkillPaths(new ArrayList<>());
        updateSkillUI();
        Toast.makeText(this, R.string.skills_cleared, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show skill selection dialog
     */
    private void showSkillSelectionDialog() {
        String[] options = {
            getString(R.string.select_skills),
            getString(R.string.manage_skills),
            getString(R.string.set_skill_folder),
            getString(R.string.create_new_skill)
        };
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.skill_options)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showSkillMultiSelectDialog();
                        break;
                    case 1:
                        showManageSkillsDialog();
                        break;
                    case 2:
                        openSkillFolderPicker();
                        break;
                    case 3:
                        showCreateSkillDialog();
                        break;
                }
            })
            .show();
    }
    
    /**
     * Open skill folder picker
     */
    private void openSkillFolderPicker() {
        skillFolderPickerLauncher.launch(null);
    }
    
    /**
     * Handle skill folder picked
     */
    private void handleSkillFolderPicked(Uri uri) {
        if (uri == null) {
            return;
        }
        
        // Convert URI to path
        String path = uri.getPath();
        if (path != null) {
            // Remove the primary: prefix if present
            if (path.contains(":")) {
                String[] parts = path.split(":");
                if (parts.length > 1) {
                    path = "/storage/emulated/0/" + parts[1];
                } else {
                    path = "/storage/emulated/0/";
                }
            }
            
            if (skillManager.setSkillFolder(path)) {
                Toast.makeText(this, getString(R.string.skill_folder_set, path), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.skill_folder_error, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Show skill multi-select dialog with order adjustment
     */
    private void showSkillMultiSelectDialog() {
        List<SkillManager.SkillFile> allSkills = skillManager.getAllSkills();
        List<String> selectedPaths = skillManager.getSelectedSkillPaths();
        
        if (allSkills.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.no_skills_found)
                .setMessage(R.string.no_skills_message)
                .setPositiveButton(R.string.create_new_skill, (d, w) -> showCreateSkillDialog())
                .setNegativeButton(R.string.cancel, null)
                .show();
            return;
        }
        
        // Build skill names array
        String[] skillNames = new String[allSkills.size()];
        boolean[] checkedItems = new boolean[allSkills.size()];
        
        for (int i = 0; i < allSkills.size(); i++) {
            skillNames[i] = allSkills.get(i).getName();
            checkedItems[i] = selectedPaths.contains(allSkills.get(i).getPath());
        }
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_skills)
            .setMultiChoiceItems(skillNames, checkedItems, (dialog, which, isChecked) -> {
                String path = allSkills.get(which).getPath();
                if (isChecked) {
                    skillManager.addSelectedSkill(path);
                } else {
                    skillManager.removeSelectedSkill(path);
                }
            })
            .setPositiveButton(R.string.adjust_order, (dialog, which) -> {
                updateSkillUI();
                showSkillOrderDialog();
            })
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.clear_all, (dialog, which) -> {
                skillManager.setSelectedSkillPaths(new ArrayList<>());
                updateSkillUI();
            })
            .setOnDismissListener(d -> updateSkillUI())
            .show();
    }
    
    /**
     * Show skill order adjustment dialog
     */
    private void showSkillOrderDialog() {
        List<SkillManager.SkillFile> selectedSkills = skillManager.getSelectedSkills();
        
        if (selectedSkills.isEmpty()) {
            Toast.makeText(this, R.string.no_skills_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] skillNames = new String[selectedSkills.size()];
        for (int i = 0; i < selectedSkills.size(); i++) {
            skillNames[i] = (i + 1) + ". " + selectedSkills.get(i).getName();
        }
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.adjust_skill_order)
            .setItems(skillNames, (dialog, which) -> {
                // Show move options
                showMoveSkillDialog(which, selectedSkills.size());
            })
            .setNegativeButton(R.string.done, null)
            .show();
    }
    
    /**
     * Show move skill dialog
     */
    private void showMoveSkillDialog(int currentIndex, int total) {
        if (total <= 1) {
            return;
        }
        
        String[] options = new String[total];
        for (int i = 0; i < total; i++) {
            if (i == currentIndex) {
                options[i] = "→ " + getString(R.string.position, i + 1);
            } else {
                options[i] = getString(R.string.move_to_position, i + 1);
            }
        }
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.move_skill)
            .setItems(options, (dialog, newIndex) -> {
                if (newIndex != currentIndex) {
                    skillManager.moveSkillOrder(currentIndex, newIndex);
                    updateSkillUI();
                    showSkillOrderDialog();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show manage skills dialog
     */
    private void showManageSkillsDialog() {
        List<SkillManager.SkillFile> allSkills = skillManager.getAllSkills();
        
        if (allSkills.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.no_skills_found)
                .setMessage(R.string.no_skills_message)
                .setPositiveButton(R.string.create_new_skill, (d, w) -> showCreateSkillDialog())
                .setNegativeButton(R.string.cancel, null)
                .show();
            return;
        }
        
        String[] skillNames = new String[allSkills.size()];
        for (int i = 0; i < allSkills.size(); i++) {
            skillNames[i] = allSkills.get(i).getName();
        }
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.manage_skills)
            .setItems(skillNames, (dialog, which) -> {
                showSkillActionsDialog(allSkills.get(which));
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show skill actions dialog
     */
    private void showSkillActionsDialog(SkillManager.SkillFile skill) {
        String[] options = {
            getString(R.string.view_skill),
            getString(R.string.edit_skill),
            getString(R.string.delete_skill)
        };
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(skill.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showViewSkillDialog(skill);
                        break;
                    case 1:
                        showEditSkillDialog(skill);
                        break;
                    case 2:
                        showDeleteSkillConfirmDialog(skill);
                        break;
                }
            })
            .show();
    }
    
    /**
     * Show view skill dialog
     */
    private void showViewSkillDialog(SkillManager.SkillFile skill) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(skill.getName())
            .setMessage(skill.getContent())
            .setPositiveButton(R.string.ok, null)
            .show();
    }
    
    /**
     * Show create skill dialog
     */
    private void showCreateSkillDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_skill, null);
        TextInputEditText inputName = dialogView.findViewById(R.id.input_skill_name);
        TextInputEditText inputContent = dialogView.findViewById(R.id.input_skill_content);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_new_skill)
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
                String content = inputContent.getText() != null ? inputContent.getText().toString().trim() : "";
                
                if (name.isEmpty()) {
                    Toast.makeText(this, R.string.skill_name_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (skillManager.createSkill(name, content)) {
                    Toast.makeText(this, R.string.skill_created, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.skill_create_error, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show edit skill dialog
     */
    private void showEditSkillDialog(SkillManager.SkillFile skill) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_skill, null);
        TextInputEditText inputName = dialogView.findViewById(R.id.input_skill_name);
        TextInputEditText inputContent = dialogView.findViewById(R.id.input_skill_content);
        
        inputName.setText(skill.getName());
        inputContent.setText(skill.getContent());
        inputName.setEnabled(false); // Don't allow renaming
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_skill)
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String content = inputContent.getText() != null ? inputContent.getText().toString().trim() : "";
                
                if (skillManager.updateSkill(skill.getPath(), content)) {
                    Toast.makeText(this, R.string.skill_updated, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.skill_update_error, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    /**
     * Show delete skill confirm dialog
     */
    private void showDeleteSkillConfirmDialog(SkillManager.SkillFile skill) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_skill)
            .setMessage(getString(R.string.delete_skill_confirm, skill.getName()))
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                if (skillManager.deleteSkill(skill.getPath())) {
                    Toast.makeText(this, R.string.skill_deleted, Toast.LENGTH_SHORT).show();
                    updateSkillUI();
                } else {
                    Toast.makeText(this, R.string.skill_delete_error, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
    // ==================== File Management ====================
    
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
    
    private void handleFilePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        
        Log.d(TAG, "File picked: " + uri.toString());
        
        FileContentManager.FileAttachment file = fileContentManager.readFile(uri);
        
        if (file.hasError()) {
            Toast.makeText(this, file.getError(), Toast.LENGTH_LONG).show();
            return;
        }
        
        hasPerformedSplit = false;
        selectedBatchIndex = -1;
        currentBatchIndex = 0;
        
        updateFileAttachmentUI(file);
        
        binding.textSplitInfo.setVisibility(View.VISIBLE);
        binding.textSplitInfo.setText(R.string.click_split_to_configure);
        binding.buttonSelectBatch.setVisibility(View.GONE);
        
        Toast.makeText(this, R.string.file_attached, Toast.LENGTH_SHORT).show();
    }
    
    private void updateFileAttachmentUI(FileContentManager.FileAttachment file) {
        if (file == null || file.hasError()) {
            binding.fileAttachmentLayout.setVisibility(View.GONE);
            return;
        }
        
        binding.fileAttachmentLayout.setVisibility(View.VISIBLE);
        binding.textFileName.setText(file.getFileName());
        binding.textFileInfo.setText(fileContentManager.getFileInfo());
    }
    
    private void performSplit() {
        FileSplitter splitter = fileContentManager.getSplitter();
        if (splitter == null) {
            return;
        }
        
        String lastRegex = splitPrefs.getString(KEY_LAST_REGEX, "");
        if (!lastRegex.isEmpty()) {
            splitter.setSplitPattern(lastRegex);
        }
        
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
    
    private void removeFileAttachment() {
        fileContentManager.clearFile();
        selectedBatchIndex = -1;
        currentBatchIndex = 0;
        hasPerformedSplit = false;
        binding.fileAttachmentLayout.setVisibility(View.GONE);
        Toast.makeText(this, R.string.file_removed, Toast.LENGTH_SHORT).show();
    }
    
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
        
        int lastMode = splitPrefs.getInt(KEY_LAST_SPLIT_MODE, 0);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_options)
            .setSingleChoiceItems(options, lastMode, (dialog, which) -> {
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
    
    private void showChapterSplitOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        TextInputEditText input = dialogView.findViewById(R.id.input_message_content);
        input.setHint(getString(R.string.batch_size) + " (default: 5)");
        
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
                
                splitPrefs.edit().putInt(KEY_LAST_BATCH_SIZE, batchSize).apply();
                
                FileSplitter splitter = fileContentManager.getSplitter();
                if (splitter != null) {
                    splitter.setBatchSize(batchSize);
                    splitter.setSplitPattern("第[零一二三四五六七八九十百千万\\d]+[章节回][^\n]*");
                    splitPrefs.edit().putString(KEY_LAST_REGEX, "").apply();
                    performSplit();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
    
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
    
    private void showRegexSplitOptions() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        TextInputEditText input = dialogView.findViewById(R.id.input_message_content);
        input.setHint(getString(R.string.regex_pattern));
        
        String lastRegex = splitPrefs.getString(KEY_LAST_REGEX, "第[零一二三四五六七八九十百千万\\d]+[章节回]");
        input.setText(lastRegex);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.split_by_regex)
            .setView(dialogView)
            .setPositiveButton(R.string.apply_split, (dialog, which) -> {
                String regex = input.getText() != null ? input.getText().toString().trim() : "";
                if (!regex.isEmpty()) {
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
        
        String fileContent = null;
        if (fileContentManager.hasFile()) {
            fileContent = fileContentManager.getContentForSending(selectedBatchIndex);
        }
        
        if (userContent.isEmpty() && fileContent == null) {
            return;
        }
        
        binding.editMessage.setText("");
        
        // Build system prompt from selected skills
        String systemPrompt = skillManager.buildSystemPrompt();
        
        // Build message: user content first, then file content
        String displayContent = userContent;
        String apiContent = userContent;
        
        if (fileContent != null && !fileContent.isEmpty()) {
            apiContent = userContent + "\n\n" + fileContent;
        }
        
        // Send message with system prompt
        viewModel.sendMessageWithSystem(displayContent, apiContent, systemPrompt, new ChatViewModel.SendCallback() {
            @Override
            public void onChunk(String chunk) {}
            
            @Override
            public void onComplete() {
                if (selectedBatchIndex >= 0) {
                    FileSplitter splitter = fileContentManager.getSplitter();
                    if (splitter != null && currentBatchIndex < splitter.getBatchCount() - 1) {
                        currentBatchIndex++;
                        selectedBatchIndex = currentBatchIndex;
                        
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
    public void onMessageClick(Message message) {}
    
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

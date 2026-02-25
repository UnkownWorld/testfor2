package com.chatbox.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SkillManager - 管理System提示词Skill文件
 * 
 * 功能：
 * 1. 从指定文件夹读取skill文件
 * 2. 管理skill的选择顺序
 * 3. 创建和保存新的skill
 * 4. 持久化存储到公共下载目录
 */
public class SkillManager {
    
    private static final String TAG = "SkillManager";
    private static final String PREFS_NAME = "skill_prefs";
    private static final String KEY_SKILL_FOLDER = "skill_folder_path";
    private static final String KEY_SELECTED_SKILLS = "selected_skills";
    private static final String KEY_SKILL_ORDER = "skill_order";
    
    // 默认skill文件夹名称
    private static final String DEFAULT_SKILL_FOLDER = "ChatboxSkills";
    
    private final Context context;
    private final SharedPreferences prefs;
    private File skillFolder;
    
    public SkillManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 初始化skill文件夹
        initSkillFolder();
    }
    
    /**
     * 初始化skill文件夹
     */
    private void initSkillFolder() {
        String savedPath = prefs.getString(KEY_SKILL_FOLDER, "");
        
        if (savedPath.isEmpty()) {
            // 使用默认的公共下载目录
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            skillFolder = new File(downloadsDir, DEFAULT_SKILL_FOLDER);
        } else {
            skillFolder = new File(savedPath);
        }
        
        // 确保文件夹存在
        if (!skillFolder.exists()) {
            boolean created = skillFolder.mkdirs();
            if (created) {
                Log.d(TAG, "Created skill folder: " + skillFolder.getAbsolutePath());
            }
        }
    }
    
    /**
     * 获取当前skill文件夹路径
     */
    public String getSkillFolderPath() {
        return skillFolder.getAbsolutePath();
    }
    
    /**
     * 设置skill文件夹
     */
    public boolean setSkillFolder(String path) {
        File newFolder = new File(path);
        
        if (!newFolder.exists()) {
            boolean created = newFolder.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create folder: " + path);
                return false;
            }
        }
        
        if (!newFolder.isDirectory()) {
            Log.e(TAG, "Path is not a directory: " + path);
            return false;
        }
        
        skillFolder = newFolder;
        prefs.edit().putString(KEY_SKILL_FOLDER, path).apply();
        
        Log.d(TAG, "Skill folder set to: " + path);
        return true;
    }
    
    /**
     * 获取所有skill文件
     */
    public List<SkillFile> getAllSkills() {
        List<SkillFile> skills = new ArrayList<>();
        
        if (skillFolder == null || !skillFolder.exists()) {
            Log.w(TAG, "Skill folder does not exist");
            return skills;
        }
        
        File[] files = skillFolder.listFiles((dir, name) -> {
            // 过滤包含"skill"的文件（不区分大小写）
            String lowerName = name.toLowerCase();
            return lowerName.contains("skill") && 
                   (lowerName.endsWith(".txt") || lowerName.endsWith(".md"));
        });
        
        if (files == null) {
            return skills;
        }
        
        for (File file : files) {
            SkillFile skill = loadSkillFile(file);
            if (skill != null) {
                skills.add(skill);
            }
        }
        
        return skills;
    }
    
    /**
     * 加载单个skill文件
     */
    private SkillFile loadSkillFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            return new SkillFile(
                file.getName(),
                file.getAbsolutePath(),
                content.toString().trim(),
                file.lastModified()
            );
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading skill file: " + file.getName(), e);
            return null;
        }
    }
    
    /**
     * 创建新的skill文件
     */
    public boolean createSkill(String name, String content) {
        // 确保文件名包含"skill"
        String fileName = name;
        if (!fileName.toLowerCase().contains("skill")) {
            fileName = "skill_" + fileName;
        }
        
        // 确保有扩展名
        if (!fileName.endsWith(".txt") && !fileName.endsWith(".md")) {
            fileName += ".txt";
        }
        
        File skillFile = new File(skillFolder, fileName);
        
        try {
            FileWriter writer = new FileWriter(skillFile);
            writer.write(content);
            writer.close();
            
            Log.d(TAG, "Created skill file: " + fileName);
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error creating skill file: " + fileName, e);
            return false;
        }
    }
    
    /**
     * 更新skill文件
     */
    public boolean updateSkill(String filePath, String content) {
        File file = new File(filePath);
        
        if (!file.exists()) {
            Log.e(TAG, "Skill file does not exist: " + filePath);
            return false;
        }
        
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            
            Log.d(TAG, "Updated skill file: " + file.getName());
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error updating skill file: " + filePath, e);
            return false;
        }
    }
    
    /**
     * 删除skill文件
     */
    public boolean deleteSkill(String filePath) {
        File file = new File(filePath);
        
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "Deleted skill file: " + file.getName());
                
                // 从已选择列表中移除
                removeSelectedSkill(filePath);
            }
            return deleted;
        }
        
        return false;
    }
    
    /**
     * 获取已选择的skill列表（按顺序）
     */
    public List<String> getSelectedSkillPaths() {
        String saved = prefs.getString(KEY_SKILL_ORDER, "");
        List<String> paths = new ArrayList<>();
        
        if (!saved.isEmpty()) {
            String[] parts = saved.split("\\|\\|\\|");
            for (String path : parts) {
                if (!path.isEmpty()) {
                    paths.add(path);
                }
            }
        }
        
        return paths;
    }
    
    /**
     * 保存选择的skill列表（按顺序）
     */
    public void setSelectedSkillPaths(List<String> paths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            sb.append(paths.get(i));
            if (i < paths.size() - 1) {
                sb.append("|||");
            }
        }
        
        prefs.edit().putString(KEY_SKILL_ORDER, sb.toString()).apply();
        Log.d(TAG, "Saved " + paths.size() + " selected skills");
    }
    
    /**
     * 添加选择的skill
     */
    public void addSelectedSkill(String path) {
        List<String> paths = getSelectedSkillPaths();
        if (!paths.contains(path)) {
            paths.add(path);
            setSelectedSkillPaths(paths);
        }
    }
    
    /**
     * 移除选择的skill
     */
    public void removeSelectedSkill(String path) {
        List<String> paths = getSelectedSkillPaths();
        paths.remove(path);
        setSelectedSkillPaths(paths);
    }
    
    /**
     * 调整skill顺序
     */
    public void moveSkillOrder(int fromPosition, int toPosition) {
        List<String> paths = getSelectedSkillPaths();
        
        if (fromPosition < 0 || fromPosition >= paths.size() ||
            toPosition < 0 || toPosition >= paths.size()) {
            return;
        }
        
        String moved = paths.remove(fromPosition);
        paths.add(toPosition, moved);
        
        setSelectedSkillPaths(paths);
    }
    
    /**
     * 构建完整的System提示词
     */
    public String buildSystemPrompt() {
        List<String> selectedPaths = getSelectedSkillPaths();
        
        if (selectedPaths.isEmpty()) {
            return "";
        }
        
        StringBuilder systemPrompt = new StringBuilder();
        
        for (String path : selectedPaths) {
            File file = new File(path);
            if (file.exists()) {
                SkillFile skill = loadSkillFile(file);
                if (skill != null && !skill.getContent().isEmpty()) {
                    if (systemPrompt.length() > 0) {
                        systemPrompt.append("\n\n---\n\n");
                    }
                    systemPrompt.append(skill.getContent());
                }
            }
        }
        
        return systemPrompt.toString();
    }
    
    /**
     * 获取已选择的skill文件列表（用于显示）
     */
    public List<SkillFile> getSelectedSkills() {
        List<String> paths = getSelectedSkillPaths();
        List<SkillFile> skills = new ArrayList<>();
        
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                SkillFile skill = loadSkillFile(file);
                if (skill != null) {
                    skills.add(skill);
                }
            }
        }
        
        return skills;
    }
    
    /**
     * Skill文件数据类
     */
    public static class SkillFile {
        private final String name;
        private final String path;
        private final String content;
        private final long lastModified;
        
        public SkillFile(String name, String path, String content, long lastModified) {
            this.name = name;
            this.path = path;
            this.content = content;
            this.lastModified = lastModified;
        }
        
        public String getName() {
            return name;
        }
        
        public String getPath() {
            return path;
        }
        
        public String getContent() {
            return content;
        }
        
        public long getLastModified() {
            return lastModified;
        }
        
        /**
         * 获取内容预览（前100字符）
         */
        public String getPreview() {
            if (content == null || content.isEmpty()) {
                return "";
            }
            
            int len = Math.min(100, content.length());
            return content.substring(0, len) + (content.length() > 100 ? "..." : "");
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}

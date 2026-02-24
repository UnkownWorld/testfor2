package com.chatbox.app.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * FileContentManager - 文件内容管理器
 * 
 * 负责读取、解析和管理文件内容。
 * 支持多种文本文件格式，并提供文件分割功能。
 */
public class FileContentManager {
    
    private static final String TAG = "FileContentManager";
    
    // 支持的文件扩展名
    private static final String[] SUPPORTED_EXTENSIONS = {
        "txt", "md", "json", "xml", "html", "htm", "csv", 
        "java", "kt", "py", "js", "ts", "c", "cpp", "h", "hpp",
        "cs", "go", "rs", "rb", "php", "swift", "scala", "sh", "bat"
    };
    
    private final Context context;
    private FileAttachment currentFile;
    private FileSplitter splitter;
    
    public FileContentManager(Context context) {
        this.context = context;
    }
    
    /**
     * 检查文件扩展名是否支持
     * @param fileName 文件名
     * @return 是否支持
     */
    public static boolean isSupportedFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= fileName.length() - 1) {
            return false;
        }
        
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        for (String supported : SUPPORTED_EXTENSIONS) {
            if (supported.equals(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 读取文件内容
     * @param uri 文件URI
     * @return 文件附件对象
     */
    public FileAttachment readFile(Uri uri) {
        try {
            // 获取文件名
            String fileName = getFileName(uri);
            if (fileName == null) {
                fileName = "unknown_file";
            }
            
            // 检查文件类型
            if (!isSupportedFile(fileName)) {
                return new FileAttachment(null, fileName, 0, "不支持的文件类型");
            }
            
            // 读取文件内容
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return new FileAttachment(null, fileName, 0, "无法打开文件");
            }
            
            // 尝试检测编码并读取内容
            String content = readStream(inputStream);
            
            // 创建文件附件对象
            currentFile = new FileAttachment(content, fileName, content.length(), null);
            
            // 初始化分割器
            splitter = new FileSplitter(content);
            
            return currentFile;
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading file", e);
            return new FileAttachment(null, "error", 0, "读取文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 读取输入流内容
     * @param inputStream 输入流
     * @return 文件内容
     */
    private String readStream(InputStream inputStream) {
        try {
            // 首先尝试UTF-8读取
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            reader.close();
            return content.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading stream", e);
            return "";
        }
    }
    
    /**
     * 获取文件名
     * @param uri 文件URI
     * @return 文件名
     */
    private String getFileName(Uri uri) {
        String fileName = null;
        
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(
                uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name", e);
            }
        }
        
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        
        return fileName;
    }
    
    /**
     * 获取当前文件
     * @return 当前文件附件
     */
    public FileAttachment getCurrentFile() {
        return currentFile;
    }
    
    /**
     * 获取文件分割器
     * @return 分割器
     */
    public FileSplitter getSplitter() {
        return splitter;
    }
    
    /**
     * 设置自定义分割器（用于按行/字符分割）
     * @param customSplitter 自定义分割器
     * @param param 分割参数（行数或字符数）
     */
    public void setCustomSplitter(FileSplitter customSplitter, int param) {
        this.splitter = customSplitter;
    }
    
    /**
     * 清除当前文件
     */
    public void clearFile() {
        currentFile = null;
        splitter = null;
    }
    
    /**
     * 检查是否有当前文件
     * @return 是否有文件
     */
    public boolean hasFile() {
        return currentFile != null && currentFile.getContent() != null;
    }
    
    /**
     * 获取当前文件的内容用于发送（只返回正文，不含批次前缀）
     * @param batchIndex 批次索引（-1表示发送全部）
     * @return 要发送的内容
     */
    public String getContentForSending(int batchIndex) {
        if (!hasFile()) {
            return null;
        }
        
        if (batchIndex < 0) {
            // 发送全部内容
            return currentFile.getContent();
        }
        
        if (splitter != null) {
            FileSplitter.Batch batch = splitter.getBatch(batchIndex);
            if (batch != null) {
                // 只返回正文内容，不含批次前缀
                return batch.getContent();
            }
        }
        
        return null;
    }
    
    /**
     * 获取当前文件信息描述
     * @return 文件信息字符串
     */
    public String getFileInfo() {
        if (!hasFile()) {
            return null;
        }
        
        int charCount = currentFile.getContent().length();
        int lineCount = currentFile.getContent().split("\n").length;
        
        return String.format("字符数: %,d | 行数: %,d", charCount, lineCount);
    }
    
    /**
     * 获取分割信息描述
     * @return 分割信息字符串
     */
    public String getSplitInfo() {
        if (splitter == null) {
            return null;
        }
        
        int segmentCount = splitter.getSegmentCount();
        int batchCount = splitter.getBatchCount();
        
        if (segmentCount <= 1) {
            return "未检测到章节，将作为整体发送";
        }
        
        return String.format("检测到 %d 个章节，分为 %d 批", segmentCount, batchCount);
    }
    
    /**
     * 文件附件数据类
     */
    public static class FileAttachment {
        private final String content;
        private final String fileName;
        private final long fileSize;
        private final String error;
        
        public FileAttachment(String content, String fileName, long fileSize, String error) {
            this.content = content;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.error = error;
        }
        
        public String getContent() {
            return content;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        public String getError() {
            return error;
        }
        
        public boolean hasError() {
            return error != null && !error.isEmpty();
        }
        
        /**
         * 获取格式化的文件大小
         * @return 文件大小字符串
         */
        public String getFormattedSize() {
            if (fileSize < 1024) {
                return fileSize + " B";
            } else if (fileSize < 1024 * 1024) {
                return String.format("%.1f KB", fileSize / 1024.0);
            } else {
                return String.format("%.1f MB", fileSize / (1024.0 * 1024));
            }
        }
    }
}

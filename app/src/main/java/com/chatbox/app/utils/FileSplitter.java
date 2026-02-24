package com.chatbox.app.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FileSplitter - 文件内容分割器
 * 
 * 支持通过正则表达式拆分文件内容，主要用于小说等长文本的分章节处理。
 * 默认支持中文章节格式，如"第一章"、"第1章"等。
 */
public class FileSplitter {
    
    // 默认章节匹配模式：第xxx章/节/回
    private static final String DEFAULT_CHAPTER_PATTERN = 
        "第[零一二三四五六七八九十百千万\\d]+[章节回][^\\n]*";
    
    // 匹配章节标题的模式（用于提取章节名称）
    private static final Pattern CHAPTER_TITLE_PATTERN = 
        Pattern.compile("第[零一二三四五六七八九十百千万\\d]+[章节回][^\\n]*");
    
    private String content;
    private List<String> segments;
    private List<String> segmentTitles;
    private String splitPattern;
    private int batchSize;
    
    public FileSplitter(String content) {
        this.content = content;
        this.segments = new ArrayList<>();
        this.segmentTitles = new ArrayList<>();
        this.splitPattern = DEFAULT_CHAPTER_PATTERN;
        this.batchSize = 5; // 默认每5章一批
    }
    
    /**
     * 设置自定义分割正则表达式
     * @param pattern 正则表达式
     */
    public void setSplitPattern(String pattern) {
        this.splitPattern = pattern;
    }
    
    /**
     * 设置每批章节数
     * @param batchSize 每批章节数
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, batchSize);
    }
    
    /**
     * 执行分割
     * @return 分割后的段落数量
     */
    public int split() {
        segments.clear();
        segmentTitles.clear();
        
        if (content == null || content.isEmpty()) {
            return 0;
        }
        
        // 使用正则表达式查找所有章节标题位置
        Pattern pattern = Pattern.compile(splitPattern);
        Matcher matcher = pattern.matcher(content);
        
        List<Integer> startPositions = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        
        // 找到所有章节开始位置
        while (matcher.find()) {
            startPositions.add(matcher.start());
            titles.add(matcher.group().trim());
        }
        
        // 如果没有找到章节，将整个内容作为一个段落
        if (startPositions.isEmpty()) {
            segments.add(content.trim());
            segmentTitles.add("全部内容");
            return 1;
        }
        
        // 按章节分割内容
        for (int i = 0; i < startPositions.size(); i++) {
            int start = startPositions.get(i);
            int end = (i + 1 < startPositions.size()) ? startPositions.get(i + 1) : content.length();
            
            String segment = content.substring(start, end).trim();
            if (!segment.isEmpty()) {
                segments.add(segment);
                segmentTitles.add(titles.get(i));
            }
        }
        
        // 检查开头是否有未分类内容
        if (startPositions.get(0) > 0) {
            String preamble = content.substring(0, startPositions.get(0)).trim();
            if (!preamble.isEmpty()) {
                segments.add(0, preamble);
                segmentTitles.add(0, "序言/开头");
            }
        }
        
        return segments.size();
    }
    
    /**
     * 获取所有分割后的段落
     * @return 段落列表
     */
    public List<String> getSegments() {
        return new ArrayList<>(segments);
    }
    
    /**
     * 获取所有段落标题
     * @return 标题列表
     */
    public List<String> getSegmentTitles() {
        return new ArrayList<>(segmentTitles);
    }
    
    /**
     * 获取指定段落
     * @param index 段落索引
     * @return 段落内容
     */
    public String getSegment(int index) {
        if (index >= 0 && index < segments.size()) {
            return segments.get(index);
        }
        return null;
    }
    
    /**
     * 获取指定段落的标题
     * @param index 段落索引
     * @return 段落标题
     */
    public String getSegmentTitle(int index) {
        if (index >= 0 && index < segmentTitles.size()) {
            return segmentTitles.get(index);
        }
        return null;
    }
    
    /**
     * 获取段落数量
     * @return 段落数量
     */
    public int getSegmentCount() {
        return segments.size();
    }
    
    /**
     * 获取批次列表（每batchSize个段落为一批）
     * @return 批次列表
     */
    public List<Batch> getBatches() {
        List<Batch> batches = new ArrayList<>();
        
        for (int i = 0; i < segments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, segments.size());
            List<String> batchSegments = segments.subList(i, end);
            List<String> batchTitles = segmentTitles.subList(i, end);
            
            StringBuilder batchContent = new StringBuilder();
            for (String segment : batchSegments) {
                batchContent.append(segment).append("\n\n");
            }
            
            String batchTitle;
            if (end - i == 1) {
                batchTitle = batchTitles.get(0);
            } else {
                batchTitle = batchTitles.get(0) + " ~ " + batchTitles.get(batchTitles.size() - 1);
            }
            
            batches.add(new Batch(i / batchSize, batchTitle, batchContent.toString().trim()));
        }
        
        return batches;
    }
    
    /**
     * 获取指定批次
     * @param batchIndex 批次索引
     * @return 批次内容
     */
    public Batch getBatch(int batchIndex) {
        List<Batch> batches = getBatches();
        if (batchIndex >= 0 && batchIndex < batches.size()) {
            return batches.get(batchIndex);
        }
        return null;
    }
    
    /**
     * 获取批次数量
     * @return 批次数量
     */
    public int getBatchCount() {
        return (int) Math.ceil((double) segments.size() / batchSize);
    }
    
    /**
     * 批次数据类
     */
    public static class Batch {
        private final int index;
        private final String title;
        private final String content;
        
        public Batch(int index, String title, String content) {
            this.index = index;
            this.title = title;
            this.content = content;
        }
        
        public int getIndex() {
            return index;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getContent() {
            return content;
        }
        
        @Override
        public String toString() {
            return "Batch{" + title + ", 内容长度=" + content.length() + "}";
        }
    }
    
    /**
     * 使用自定义正则表达式分割内容
     * @param content 内容
     * @param regex 正则表达式
     * @return 分割后的段落列表
     */
    public static List<String> splitByRegex(String content, String regex) {
        List<String> parts = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return parts;
        }
        
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String part = content.substring(lastEnd, matcher.start()).trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
            }
            lastEnd = matcher.start();
        }
        
        if (lastEnd < content.length()) {
            String part = content.substring(lastEnd).trim();
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        
        return parts;
    }
    
    /**
     * 按固定行数分割内容
     * @param content 内容
     * @param linesPerSegment 每段行数
     * @return 分割后的段落列表
     */
    public static List<String> splitByLines(String content, int linesPerSegment) {
        List<String> parts = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return parts;
        }
        
        String[] lines = content.split("\n");
        StringBuilder currentPart = new StringBuilder();
        int lineCount = 0;
        
        for (String line : lines) {
            currentPart.append(line).append("\n");
            lineCount++;
            
            if (lineCount >= linesPerSegment) {
                parts.add(currentPart.toString().trim());
                currentPart = new StringBuilder();
                lineCount = 0;
            }
        }
        
        if (currentPart.length() > 0) {
            parts.add(currentPart.toString().trim());
        }
        
        return parts;
    }
    
    /**
     * 按固定字符数分割内容
     * @param content 内容
     * @param charsPerSegment 每段字符数
     * @return 分割后的段落列表
     */
    public static List<String> splitByCharacters(String content, int charsPerSegment) {
        List<String> parts = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return parts;
        }
        
        int length = content.length();
        for (int i = 0; i < length; i += charsPerSegment) {
            int end = Math.min(i + charsPerSegment, length);
            parts.add(content.substring(i, end));
        }
        
        return parts;
    }
}

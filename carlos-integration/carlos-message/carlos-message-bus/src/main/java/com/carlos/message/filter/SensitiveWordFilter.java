package com.carlos.message.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 敏感词过滤器（基于 DFA 算法）
 * </p>
 *
 * <p>
 * 支持高效的多模式字符串匹配，时间复杂度近似 O(n)。
 * 可通过配置文件或数据库加载敏感词库。
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    private final Map<Character, Node> root = new HashMap<>();

    public SensitiveWordFilter() {
        // 默认加载一些示例敏感词，实际应从配置中心/数据库加载
        loadDefaultWords();
    }

    private void loadDefaultWords() {
        // 实际项目中应从数据库或配置文件加载
        List<String> words = List.of("诈骗", "赌博", "色情", "暴力", "毒品");
        words.forEach(this::addWord);
        log.info("敏感词过滤器初始化完成, 词库大小: {}", words.size());
    }

    /**
     * 添加敏感词
     *
     * <p>
     * 终止标记必须打在<b>最后一个字符所在的节点</b>上：{@link #checkLength} 是读
     * {@code node.isEnd} 来判断匹配结束的。历史实现把标记打在了末尾字符节点的
     * {@code END_FLAG} 子节点上，而文本里不可能出现 {@code '\0'}，
     * 于是 {@code contains} 恒为 false、{@code findAll} 恒为空、{@code replace} 不做替换。
     * </p>
     *
     * @param word 敏感词
     */
    public synchronized void addWord(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }
        Map<Character, Node> current = root;
        Node last = null;
        for (char c : word.toCharArray()) {
            last = current.computeIfAbsent(c, k -> new Node());
            current = last.children;
        }
        last.isEnd = true;
    }

    /**
     * 检查文本是否包含敏感词
     *
     * @param text 文本
     * @return 是否包含
     */
    public boolean contains(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            int length = checkLength(text, i);
            if (length > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找文本中的所有敏感词
     *
     * @param text 文本
     * @return 敏感词列表
     */
    public List<String> findAll(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        for (int i = 0; i < text.length(); i++) {
            int length = checkLength(text, i);
            if (length > 0) {
                result.add(text.substring(i, i + length));
                i += length - 1;
            }
        }
        return result;
    }

    /**
     * 替换敏感词
     *
     * @param text        原文本
     * @param replacement 替换字符
     * @return 替换后的文本
     */
    public String replace(String text, char replacement) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        for (int i = 0; i < text.length(); i++) {
            int length = checkLength(text, i);
            if (length > 0) {
                for (int j = i; j < i + length; j++) {
                    sb.setCharAt(j, replacement);
                }
                i += length - 1;
            }
        }
        return sb.toString();
    }

    private int checkLength(String text, int start) {
        Map<Character, Node> current = root;
        int length = 0;
        int maxLength = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            Node node = current.get(c);
            if (node == null) {
                break;
            }
            length++;
            if (node.isEnd) {
                maxLength = length;
            }
            current = node.children;
        }
        return maxLength;
    }

    private static class Node {
        boolean isEnd = false;
        Map<Character, Node> children = new HashMap<>();
    }
}

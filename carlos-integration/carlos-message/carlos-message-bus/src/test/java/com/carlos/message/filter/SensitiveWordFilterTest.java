package com.carlos.message.filter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感词过滤器测试
 */
class SensitiveWordFilterTest {

    private final SensitiveWordFilter filter = new SensitiveWordFilter();

    @Test
    void should_detect_sensitive_word() {
        assertTrue(filter.contains("这是一段包含赌博的内容"));
        assertTrue(filter.contains("色情信息"));
    }

    @Test
    void should_not_detect_clean_text() {
        assertFalse(filter.contains("这是一段正常的内容"));
    }

    @Test
    void should_find_all_sensitive_words() {
        List<String> words = filter.findAll("赌博和色情都是违法的");
        assertEquals(2, words.size());
        assertTrue(words.contains("赌博"));
        assertTrue(words.contains("色情"));
    }

    @Test
    void should_replace_sensitive_words() {
        String result = filter.replace("赌博和色情", '*');
        assertEquals("**和**", result);
    }

    @Test
    void should_add_custom_word() {
        filter.addWord("自定义敏感词");
        assertTrue(filter.contains("这里有自定义敏感词出现"));
    }

    @Test
    void should_return_empty_result_for_null_or_empty_text() {
        assertFalse(filter.contains(null));
        assertFalse(filter.contains(""));
        assertTrue(filter.findAll(null).isEmpty());
        assertTrue(filter.findAll("").isEmpty());
        assertNull(filter.replace(null, '*'));
        assertEquals("", filter.replace("", '*'));
    }

    @Test
    void should_detect_word_ending_at_last_char() {
        // 回归：末尾字符所在节点必须带终止标记，否则「结尾命中」永远检测不到
        assertTrue(filter.contains("内容结尾是赌博"));
        assertEquals(List.of("赌博"), filter.findAll("内容结尾是赌博"));
        assertEquals("内容结尾是**", filter.replace("内容结尾是赌博", '*'));
    }

    @Test
    void should_not_match_partial_word() {
        // 词库只有「赌博」，单一个「赌」不应命中
        assertFalse(filter.contains("他只是在赌"));
        assertTrue(filter.findAll("他只是在赌").isEmpty());
    }

    @Test
    void should_prefer_longest_match() {
        filter.addWord("赌");
        assertEquals(List.of("赌博"), filter.findAll("赌博"));
    }

    @Test
    void should_ignore_null_or_blank_word_on_add() {
        assertDoesNotThrow(() -> {
            filter.addWord(null);
            filter.addWord("");
        });
        assertFalse(filter.contains("这是一段正常的内容"));
    }

    @Test
    void should_replace_custom_word() {
        filter.addWord("自定义");
        assertEquals("***词", filter.replace("自定义词", '*'));
    }
}

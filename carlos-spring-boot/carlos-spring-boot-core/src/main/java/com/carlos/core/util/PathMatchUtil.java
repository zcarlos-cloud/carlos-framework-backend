package com.carlos.core.util;

import org.springframework.http.server.PathContainer;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Spring Boot 3 路径匹配静态工具类
 * 基于 PathPatternParser 实现，支持 Ant 风格路径表达式
 *
 * 支持的通配符：
 * ?  匹配单个字符
 * *  匹配零个或多个字符（不包含 /）
 * ** 匹配零个或多个目录
 * {name} 路径变量
 * {name:regex} 带正则的路径变量
 *
 * 使用示例：
 * PathMatchUtil.match("/api/**", "/api/user/list");           // true
 * PathMatchUtil.extractUriVariables("/user/{id}", "/user/123"); // {id=123}
 *
 * @author YourName
 * @since 1.0.0
 */
public final class PathMatchUtil {

    // ==================== 静态常量 ====================

    /** 默认解析器 */
    private static final PathPatternParser DEFAULT_PARSER = new PathPatternParser();

    /** AntPathMatcher 实例（用于传统 Ant 风格匹配） */
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /** 单例实例（延迟加载） */
    private static volatile PathMatchUtil instance;

    /** 实例锁 */
    private static final ReentrantLock LOCK = new ReentrantLock();

    // ==================== 实例成员（非静态） ====================

    /** 解析器实例 */
    private final PathPatternParser parser;

    /** 模式缓存 */
    private final Map<String, PathPattern> patternCache;

    /** 是否启用缓存 */
    private final boolean cacheEnabled;

    // ==================== 私有构造器 ====================

    /**
     * 私有构造器，防止外部实例化
     */
    private PathMatchUtil(PathPatternParser parser, boolean cacheEnabled) {
        this.parser = parser != null ? parser : DEFAULT_PARSER;
        this.cacheEnabled = cacheEnabled;
        this.patternCache = cacheEnabled ? new ConcurrentHashMap<>(256) : null;
    }

    // ==================== 单例获取 ====================

    /**
     * 获取默认实例（启用缓存）
     */
    private static PathMatchUtil getInstance() {
        if (instance == null) {
            LOCK.lock();
            try {
                if (instance == null) {
                    instance = new PathMatchUtil(DEFAULT_PARSER, true);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return instance;
    }

    // ==================== 公共配置方法 ====================

    /**
     * 初始化配置（可选，首次使用前调用）
     * @param parser 自定义解析器，null 则使用默认
     * @param cacheEnabled 是否启用缓存
     */
    public static void init(PathPatternParser parser, boolean cacheEnabled) {
        LOCK.lock();
        try {
            if (instance != null) {
                throw new IllegalStateException("PathMatchUtil 已初始化，请勿重复调用");
            }
            instance = new PathMatchUtil(parser, cacheEnabled);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * 重置实例（用于测试或重新配置）
     */
    public static void reset() {
        LOCK.lock();
        try {
            instance = null;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * 清除模式缓存
     */
    public static void clearCache() {
        PathMatchUtil inst = getInstance();
        if (inst.cacheEnabled && inst.patternCache != null) {
            inst.patternCache.clear();
        }
    }

    /**
     * 获取缓存大小
     */
    public static int getCacheSize() {
        PathMatchUtil inst = getInstance();
        return (inst.cacheEnabled && inst.patternCache != null) ? inst.patternCache.size() : 0;
    }

    // ==================== 核心匹配方法（静态） ====================

    /**
     * 判断路径是否匹配模式（完全匹配）
     *
     * @param pattern Ant风格模式，如 /api/**
     * @param path 待匹配路径，如 /api/user/list
     * @return 是否匹配
     */
    public static boolean match(String pattern, String path) {
        if (!StringUtils.hasText(pattern) || !StringUtils.hasText(path)) {
            return false;
        }
        return getInstance().doMatch(pattern, path);
    }

    /**
     * 判断路径是否匹配模式（支持多个模式，任一匹配即可）
     *
     * @param patterns 模式列表
     * @param path 待匹配路径
     * @return 是否匹配任意一个模式
     */
    public static boolean matchAny(String[] patterns, String path) {
        if (patterns == null || patterns.length == 0) {
            return false;
        }
        for (String pattern : patterns) {
            if (match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断路径是否匹配模式（支持多个模式，任一匹配即可）
     *
     * @param patterns 模式集合
     * @param path 待匹配路径
     * @return 是否匹配任意一个模式
     */
    public static boolean matchAny(Collection<String> patterns, String path) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断路径是否匹配所有模式
     *
     * @param patterns 模式列表
     * @param path 待匹配路径
     * @return 是否匹配所有模式
     */
    public static boolean matchAll(String[] patterns, String path) {
        if (patterns == null || patterns.length == 0) {
            return false;
        }
        for (String pattern : patterns) {
            if (!match(pattern, path)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断路径是否匹配所有模式
     *
     * @param patterns 模式集合
     * @param path 待匹配路径
     * @return 是否匹配所有模式
     */
    public static boolean matchAll(Collection<String> patterns, String path) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (!match(pattern, path)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断路径是否不匹配该模式
     */
    public static boolean notMatch(String pattern, String path) {
        return !match(pattern, path);
    }

    // ==================== AntPathMatcher 兼容方法 ====================

    /**
     * 使用 AntPathMatcher 判断路径是否匹配模式
     * 适用于文件路径匹配、传统 Spring 路径匹配等场景
     *
     * @param pattern Ant风格模式，如 /api/**
     * @param path 待匹配路径，如 /api/user/list
     * @return 是否匹配
     */
    public static boolean antMatch(String pattern, String path) {
        if (!StringUtils.hasText(pattern) || !StringUtils.hasText(path)) {
            return false;
        }
        return ANT_PATH_MATCHER.match(pattern, path);
    }

    /**
     * 使用 AntPathMatcher 判断路径是否匹配任意一个模式
     *
     * @param patterns 模式列表
     * @param path 待匹配路径
     * @return 是否匹配任意一个模式
     */
    public static boolean antMatchAny(String[] patterns, String path) {
        if (patterns == null || patterns.length == 0) {
            return false;
        }
        for (String pattern : patterns) {
            if (ANT_PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 使用 AntPathMatcher 判断路径是否匹配任意一个模式
     *
     * @param patterns 模式集合
     * @param path 待匹配路径
     * @return 是否匹配任意一个模式
     */
    public static boolean antMatchAny(Collection<String> patterns, String path) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (ANT_PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 使用 AntPathMatcher 提取路径变量
     *
     * @param pattern 模式，如 /user/{id}/detail
     * @param path 实际路径，如 /user/123/detail
     * @return 路径变量Map，不匹配返回空Map
     */
    public static Map<String, String> antExtractUriVariables(String pattern, String path) {
        if (!StringUtils.hasText(pattern) || !StringUtils.hasText(path)) {
            return Collections.emptyMap();
        }
        if (!ANT_PATH_MATCHER.match(pattern, path)) {
            return Collections.emptyMap();
        }
        return ANT_PATH_MATCHER.extractUriTemplateVariables(pattern, path);
    }

    /**
     * 使用 AntPathMatcher 判断模式是否是路径的前缀
     *
     * @param pattern 模式
     * @param path 待检查路径
     * @return 是否是前缀
     */
    public static boolean antMatchStart(String pattern, String path) {
        if (!StringUtils.hasText(pattern) || !StringUtils.hasText(path)) {
            return false;
        }
        return ANT_PATH_MATCHER.matchStart(pattern, path);
    }

    /**
     * 使用 AntPathMatcher 组合模式与路径
     *
     * @param pattern 模式，如 /api/**
     * @param path 相对路径，如 /user/list
     * @return 组合后的路径，如 /api/user/list
     */
    public static String antCombine(String pattern, String path) {
        if (!StringUtils.hasText(pattern)) {
            return path;
        }
        if (!StringUtils.hasText(path)) {
            return pattern;
        }
        return ANT_PATH_MATCHER.combine(pattern, path);
    }

    /**
     * 获取 AntPathMatcher 实例（用于高级自定义）
     *
     * @return AntPathMatcher 实例
     */
    public static AntPathMatcher getAntPathMatcher() {
        return ANT_PATH_MATCHER;
    }

    // ==================== 前缀匹配 ====================

    /**
     * 判断模式是否是路径的前缀（路径以模式开头）
     * 例如：模式 /api/**，路径 /api/user/list → true
     *
     * @param pattern 模式
     * @param path 待检查路径
     * @return 是否是前缀
     */
    public static boolean matchStart(String pattern, String path) {
        if (!StringUtils.hasText(pattern) || !StringUtils.hasText(path)) {
            return false;
        }
        return getInstance().doMatchStart(pattern, path);
    }

    // ==================== 路径变量提取 ====================

    /**
     * 提取路径变量
     *
     * @param pattern 模式，如 /user/{id}/detail
     * @param path 实际路径，如 /user/123/detail
     * @return 路径变量Map，如 {id=123}，不匹配返回空Map
     */
    public static Map<String, String> extractUriVariables(String pattern, String path) {
        return getInstance().doExtractUriVariables(pattern, path);
    }

    /**
     * 提取单个路径变量
     *
     * @param pattern 模式
     * @param path 实际路径
     * @param variableName 变量名
     * @return 变量值，不匹配或不存在返回 null
     */
    public static String extractUriVariable(String pattern, String path, String variableName) {
        Map<String, String> variables = extractUriVariables(pattern, path);
        return variables.get(variableName);
    }

    // ==================== 最佳匹配选择 ====================

    /**
     * 从多个模式中选择最匹配的一个
     * 匹配度：完全匹配 > 带变量的匹配 > 通配符匹配
     *
     * @param patterns 模式数组
     * @param path 待匹配路径
     * @return 最匹配的模式，无匹配返回 null
     */
    public static String selectBestMatch(String[] patterns, String path) {
        if (patterns == null || patterns.length == 0) {
            return null;
        }
        return selectBestMatch(Arrays.asList(patterns), path);
    }

    /**
     * 从多个模式中选择最匹配的一个
     *
     * @param patterns 模式集合
     * @param path 待匹配路径
     * @return 最匹配的模式，无匹配返回 null
     */
    public static String selectBestMatch(Collection<String> patterns, String path) {
        if (patterns == null || patterns.isEmpty()) {
            return null;
        }

        String bestPattern = null;
        int bestScore = Integer.MIN_VALUE;

        for (String pattern : patterns) {
            int score = calculateScore(pattern, path);
            if (score > 0 && score > bestScore) {
                bestScore = score;
                bestPattern = pattern;
            }
        }

        return bestPattern;
    }

    // ==================== 批量匹配与过滤 ====================

    /**
     * 过滤匹配的路径列表
     *
     * @param pattern 模式
     * @param paths 路径列表
     * @return 匹配的路径列表
     */
    public static List<String> filterMatches(String pattern, Collection<String> paths) {
        if (paths == null) {
            return Collections.emptyList();
        }
        return paths.stream()
            .filter(path -> match(pattern, path))
            .collect(Collectors.toList());
    }

    /**
     * 过滤不匹配的路径列表
     *
     * @param pattern 模式
     * @param paths 路径列表
     * @return 不匹配的路径列表
     */
    public static List<String> filterNonMatches(String pattern, Collection<String> paths) {
        if (paths == null) {
            return Collections.emptyList();
        }
        return paths.stream()
            .filter(path -> !match(pattern, path))
            .collect(Collectors.toList());
    }

    /**
     * 将路径按模式分组（匹配第一个即停止）
     *
     * @param patterns 模式列表
     * @param paths 路径列表
     * @return 模式 -> 路径列表 的映射
     */
    public static Map<String, List<String>> groupByPattern(List<String> patterns, Collection<String> paths) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (String path : paths) {
            for (String pattern : patterns) {
                if (match(pattern, path)) {
                    result.computeIfAbsent(pattern, k -> new ArrayList<>()).add(path);
                    break;
                }
            }
        }
        return result;
    }

    // ==================== 模式工具方法 ====================

    /**
     * 验证模式语法是否有效
     *
     * @param pattern 模式字符串
     * @return 是否有效
     */
    public static boolean isValidPattern(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return false;
        }
        try {
            DEFAULT_PARSER.parse(pattern);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取模式中的路径变量名
     *
     * @param pattern 如 /user/{id}/order/{orderId}
     * @return 变量名列表 [id, orderId]
     */
    public static List<String> extractVariableNames(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return Collections.emptyList();
        }

        List<String> variables = new ArrayList<>();
        int start = 0;

        while ((start = pattern.indexOf('{', start)) != -1) {
            int end = pattern.indexOf('}', start);
            if (end == -1) {
                break;
            }

            String var = pattern.substring(start + 1, end);
            // 处理正则表达式 {id:\d+}
            if (var.contains(":")) {
                var = var.substring(0, var.indexOf(':'));
            }
            variables.add(var);
            start = end + 1;
        }

        return variables;
    }

    /**
     * 检查模式是否包含路径变量
     */
    public static boolean hasVariable(String pattern) {
        return StringUtils.hasText(pattern) && pattern.contains("{");
    }

    /**
     * 检查模式是否包含通配符
     */
    public static boolean hasWildcard(String pattern) {
        return StringUtils.hasText(pattern) && (pattern.contains("*") || pattern.contains("?"));
    }

    // ==================== 内部实现方法（实例方法） ====================

    /**
     * 执行匹配
     */
    private boolean doMatch(String pattern, String path) {
        PathPattern pathPattern = getPattern(pattern);
        PathContainer pathContainer = PathContainer.parsePath(path);
        return pathPattern.matches(pathContainer);
    }

    /**
     * 执行前缀匹配
     */
    private boolean doMatchStart(String pattern, String path) {
        PathPattern pathPattern = getPattern(pattern);
        PathContainer pathContainer = PathContainer.parsePath(path);
        PathPattern.PathRemainingMatchInfo pathRemainingMatchInfo = pathPattern.matchStartOfPath(pathContainer);
        return pathRemainingMatchInfo != null;
    }

    /**
     * 执行变量提取
     */
    private Map<String, String> doExtractUriVariables(String pattern, String path) {
        PathPattern pathPattern = getPattern(pattern);
        PathContainer pathContainer = PathContainer.parsePath(path);

        PathPattern.PathMatchInfo matchInfo = pathPattern.matchAndExtract(pathContainer);
        if (matchInfo == null) {
            return Collections.emptyMap();
        }

        return matchInfo.getUriVariables();
    }

    /**
     * 获取或解析模式
     */
    private PathPattern getPattern(String pattern) {
        if (cacheEnabled) {
            return patternCache.computeIfAbsent(pattern, p -> parser.parse(p));
        }
        return parser.parse(pattern);
    }

    /**
     * 计算匹配分数
     */
    private static int calculateScore(String pattern, String path) {
        if (!match(pattern, path)) {
            return 0;
        }

        int score = 1000;

        // 包含 ** 扣分（最不精确）
        if (pattern.contains("/**")) {
            score -= 300;
        }
        // 包含 * 扣分
        if (pattern.contains("*") && !pattern.contains("/**")) {
            score -= 200;
        }
        // 包含 ? 扣分
        if (pattern.contains("?")) {
            score -= 100;
        }
        // 包含 {var} 扣分（但比通配符好）
        if (pattern.contains("{")) {
            score -= 50;
        }

        // 长度越接近越精确
        score -= Math.abs(pattern.length() - path.length());

        return score;
    }
}

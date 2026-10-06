package com.carlos.devkit.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

/**
 * <p>
 * 开发工具平台配置属性（前缀 {@code carlos.devkit}）
 * </p>
 * <p>
 * 由 {@link DevkitConfiguration} 的 {@code @EnableConfigurationProperties} 注册。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@ConfigurationProperties(prefix = "carlos.devkit")
public class DevkitProperties {

    /**
     * 渲染沙箱根目录；留空时使用 {@code ${java.io.tmpdir}/carlos-devkit}
     */
    private String tmpRoot;

    /**
     * 单次生成产物总大小上限（字节），默认 50MB
     */
    private long maxOutputSize = 50L * 1024 * 1024;

    /**
     * 单次生成产物文件数上限，默认 2000
     */
    private int maxFileCount = 2000;

    /**
     * 单个渲染文件大小上限（字节），默认 5MB
     */
    private long maxSingleFileSize = 5L * 1024 * 1024;

    /**
     * 单个模板渲染超时（毫秒），默认 10s
     */
    private long renderTimeoutMs = 10_000L;

    /**
     * 目标库元数据读取（连接 / 语句）超时（秒），默认 10s
     */
    private int metaTimeoutSec = 10;

    /**
     * 单个模板保留的历史版本数，默认 50
     */
    private int templateMaxVersions = 50;

    /**
     * 单个模板正文大小上限（字节），默认 1MB（在线编辑器承载上限）
     */
    private int maxTemplateContentSize = 1024 * 1024;

    /**
     * 取渲染沙箱根目录（绝对路径）
     *
     * @return 沙箱根目录
     */
    public File resolveTmpRoot() {
        String root = this.tmpRoot;
        if (StrUtil.isBlank(root)) {
            root = System.getProperty("java.io.tmpdir") + File.separator + "carlos-devkit";
        }
        return new File(root);
    }
}

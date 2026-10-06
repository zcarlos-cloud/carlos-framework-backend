package com.carlos.system.upload.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * <p>
 * minio配置项
 * </p>
 *
 * @author carlos
 * @date 2021/6/10 13:21
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "carlos.system.file")
public class SystemFileProperties implements InitializingBean {


    public static final String DEFAULT_DOWNLOAD_PATH = "/sys/file/load?id=";

    /**
     * 基础地址
     */
    private String baseUrl;

    /**
     * 文件下载接口地址
     */
    private String downloadPath = DEFAULT_DOWNLOAD_PATH;

    /**
     * 支持上传的文件后缀
     */
    private Set<String> supportExt;

    /**
     * 单文件大小限制（字节），默认100MB
     */
    private Long maxFileSize = 100 * 1024 * 1024L;

    /**
     * 禁止的MIME类型
     */
    private Set<String> forbiddenMimeTypes;

    /**
     * 匿名上传是否开启，默认false
     */
    private Boolean anonymousUploadEnabled = false;

    /**
     * 匿名上传允许的文件后缀
     */
    private Set<String> anonymousUploadAllowedExt;

    /**
     * 上传超时时间（秒），默认60
     */
    private Integer uploadTimeoutSeconds = 60;

    @Override
    public void afterPropertiesSet() {
        Assert.hasText(this.baseUrl, "'carlos.system.file' must not be blank.");
        if (StrUtil.endWith(baseUrl, StrUtil.SLASH)) {
            baseUrl = StrUtil.replaceLast(baseUrl, StrUtil.SLASH, StrUtil.EMPTY);
        }

        if (!StrUtil.startWith(downloadPath, StrUtil.SLASH)) {
            downloadPath = StrUtil.SLASH + downloadPath;
        }
        log.debug("Load SystemFileProperties :{}", JSONUtil.toJsonPrettyStr(this));
    }

}

package com.carlos.oss.web;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

/**
 * <p>
 * OSS 对象包装类
 * 用于 Controller 方法返回值，自动处理文件下载
 * </p>
 *
 * @author carlos
 * @date 2026-03-06
 */
@Data
@AllArgsConstructor
public final class OssObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所在桶
     */
    private final String bucket;

    /**
     * 对象名称
     */
    private final String object;

    /**
     * 文件名（下载时使用的文件名）
     */
    private final String attachmentName;

    /**
     * 创建 Builder
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 模式
     */
    public static final class Builder {

        private String bucket;
        private String object;
        private String attachName;

        public Builder bucket(String bucket) {
            this.bucket = Objects.requireNonNull(bucket);
            return this;
        }

        public Builder object(String object) {
            this.object = Objects.requireNonNull(object);
            return this;
        }

        public Builder object(Path object) {
            this.object = Objects.requireNonNull(object).toString();
            return this;
        }

        public Builder attachmentName(String attachName) {
            this.attachName = Objects.requireNonNull(attachName);
            return this;
        }

        public OssObject build() {
            if (this.bucket == null) {
                throw new NullPointerException("bucket must not be null");
            }
            if (this.object == null) {
                throw new NullPointerException("object must not be null");
            }
            if (this.attachName == null) {
                throw new NullPointerException("attachmentName must not be null");
            }

            return new OssObject(
                this.bucket,
                this.object,
                this.attachName
            );
        }
    }
}

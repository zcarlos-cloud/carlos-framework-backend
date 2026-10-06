package com.carlos.devkit.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 单个模板渲染结果 传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
public class TemplateRenderPreviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否渲染成功
     */
    private Boolean success;

    /**
     * 渲染结果（成功时）
     */
    private String content;

    /**
     * 错误信息（失败时）
     */
    private String error;
}

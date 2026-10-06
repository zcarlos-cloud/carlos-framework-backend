package com.carlos.devkit.engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 一次生成的完整产物（内存态）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResult {

    /**
     * 产物根目录名（代码生成=项目名；工程生成=工程名）
     */
    private String rootName;

    /**
     * 产物文件清单（path 为相对 rootName 的路径）
     */
    private List<GeneratedFile> files;

    /**
     * 产物总字节数
     */
    private long totalSize;

    /**
     * 生成耗时（毫秒）
     */
    private long durationMs;
}

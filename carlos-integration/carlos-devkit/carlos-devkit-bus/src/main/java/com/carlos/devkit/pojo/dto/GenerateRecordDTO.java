package com.carlos.devkit.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 生成记录 传输对象（Service ↔ Manager）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
public class GenerateRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 业务类型：1=代码生成 2=工程生成
     */
    private Integer bizType;

    /**
     * 模板组 ID
     */
    private Long templateGroupId;

    /**
     * 模板组编码快照
     */
    private String templateGroupCode;

    /**
     * 模板组名称快照
     */
    private String templateGroupName;

    /**
     * 数据源 ID
     */
    private Long dsId;

    /**
     * 数据源名称快照
     */
    private String dsName;

    /**
     * 项目名
     */
    private String projectName;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 生成参数 JSON 快照
     */
    private String genParams;

    /**
     * 产物文件数
     */
    private Integer fileCount;

    /**
     * 产物总字节数
     */
    private Long totalSize;

    /**
     * 生成耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 状态：1=成功 0=失败
     */
    private Integer status;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 操作人
     */
    private Long createBy;

    /**
     * 生成时间
     */
    private LocalDateTime createTime;
}
